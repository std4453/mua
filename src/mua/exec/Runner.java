package mua.exec;

import java.util.Map;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.function.BiFunction;

import mua.token.BracketToken;
import mua.token.MathToken;
import mua.token.OpToken;
import mua.token.Token;
import mua.token.Tokenizer;
import mua.token.TokenizerException;
import mua.token.WordToken;
import mua.token.BracketToken.Type;

public class Runner {
    @FunctionalInterface
    private interface MathOpInterface {
        public void calc(Stack<Value> values) throws MuaException;
    }

    private static class MathOp {
        private enum Associativity {
            LEFT, RIGHT
        }

        public final int priority;
        public final Associativity associativity;
        public final MathOpInterface fn;

        public MathOp(int priority, Associativity associativity, MathOpInterface fn) {
            this.priority = priority;
            this.associativity = associativity;
            this.fn = fn;
        }
      
        public void calc(Stack<Value> values) throws MuaException {
            try {
                this.fn.calc(values);
            } catch (EmptyStackException e) {
                throw new MuaException("Not enough operands");
            }
        }

        public static MathOp create(int priority, Associativity associativity, BiFunction<Double, Double, Double> fn) {
            return new MathOp(priority, associativity, (values) -> {
                Double op2 = values.pop().asNumberVal().content;
                Double op1 = values.pop().asNumberVal().content;
                Double result = fn.apply(op1, op2);
                values.push(new NumberVal(result));
            });
        }
    }

    private static final Map<String, MathOp> ops = new HashMap<>() {{
        put("+", MathOp.create(1, MathOp.Associativity.LEFT, (a, b) -> a + b));
        put("-", MathOp.create(1, MathOp.Associativity.LEFT, (a, b) -> a - b));
        put("*", MathOp.create(2, MathOp.Associativity.LEFT, (a, b) -> a * b));
        put("/", MathOp.create(2, MathOp.Associativity.LEFT, (a, b) -> a / b));
        put("%", MathOp.create(2, MathOp.Associativity.LEFT, (a, b) -> a % b));
    }};

    private Scope globalScope;
    private Scope localScope;

    private List<Token> tokens;
    private int index;
    
    private boolean shouldReturn;
    private Value retVal;
    private Stack<Stack<Value>> valuesStacks;
    private Stack<Stack<String>> opsStacks; 

    public Runner(Scope globalScope, Scope localScope) {
        this.globalScope = globalScope;
        this.localScope = localScope;

        this.tokens = new Vector<>();
        this.index = 0;

        this.shouldReturn = false;
        this.retVal = null;
        this.valuesStacks = new Stack<>();
        this.opsStacks = new Stack<>();
    }

    // index before left bracket -> index before right bracket
    private Value execValueBracket() throws MuaException {
        List<Value> elements = new Vector<>();
        // skip bracket
        ++this.index;
        for (; this.index < this.tokens.size(); ++this.index) {
            Token token = this.tokens.get(this.index);
            if (token instanceof WordToken) {
                elements.add(new LiteralVal(((WordToken)token).value));
            } else if (token instanceof BracketToken) {
                BracketToken bracketToken = (BracketToken)token;
                if (bracketToken.type == Type.LEFT) {
                    elements.add(execValueBracket());
                } else break;
            } else throw new MuaException(String.format("Unexpected token: %s", token));
        }
        return new ListVal(elements);
    }

    // after left parentheses, return after right parentheses, stack unchanged
    private Value execValueLeftParen() throws MuaException {
        int startDepth = this.valuesStacks.size();
        this.valuesStacks.push(new Stack<>());
        this.opsStacks.push(new Stack<>());
        // loop until reaches right paren
        Value lastVal = null;
        while (true) {
            // exec value, whether function, pure value, list or expression
            lastVal = this.execValue();
            // right paren, exit loop
            if (this.valuesStacks.size() <= startDepth) break;
            // push value to stack, op is pushed to stack by execValueMath()
            if (lastVal != null) {
                this.valuesStacks.peek().push(lastVal);
            }
        }
        // last lastVal should be from execValueRightParen(), which is the final value 
        return lastVal;
    }

    // collapse current expression by evaluating all operators with
    // priority >= priorityThres 
    private void collapseMathExp(int priorityThres) throws MuaException {
        Stack<String> opsStack = this.opsStacks.peek();
        Stack<Value> valuesStack = this.valuesStacks.peek();
        while (!opsStack.isEmpty() && ops.get(opsStack.peek()).priority >= priorityThres) {
            String op = opsStack.pop();
            ops.get(op).calc(valuesStack);
        }
    } 

    // before right parentheses, pops stack
    private Value execValueRightParen() throws MuaException {
        if (this.valuesStacks.isEmpty()) throw new MuaException("Unexpected token \")\"");
        // proceed
        ++this.index;
        // minimum priority is 1, so collapses all operators, i.e., finish the calculation
        this.collapseMathExp(0);
        Stack<String> opsStack = this.opsStacks.peek();
        Stack<Value> valuesStack = this.valuesStacks.peek();
        if (valuesStack.size() != 1 || opsStack.size() != 0)
            throw new MuaException("Malformed mathematical expression");
        Value result = valuesStack.pop();
        // remove stack so that execValueLeftParen can exit; 
        opsStacks.pop();
        valuesStacks.pop();
        return result;
    }

    // before math op, return null
    private void execValueMath() throws MuaException {
        Token first = this.tokens.get(this.index);
        ++this.index;
        String op = ((MathToken)first).op;
        if (!ops.containsKey(op)) throw new MuaException(String.format("Unknown operator \"%s\"", op));
        MathOp mop = ops.get(op);
        // if left associative, operators with same priority can be evaluated, so don't
        // increase threshold, otherwise increase one to avoid evaluation in advance
        int priorityThres = mop.priority + (
            mop.associativity == MathOp.Associativity.LEFT ? 0 : 1
        );
        collapseMathExp(priorityThres);
        if (this.opsStacks.isEmpty()) throw new MuaException("Math expressions must be within parentheses");
        this.opsStacks.peek().push(op);
    }

    // consume one statement, goes one-way only so a part of a token flow will work
    public Value execValue() throws MuaException {
        Token first = this.tokens.get(this.index);
        if (first instanceof WordToken) {
            ++this.index;
            return new LiteralVal(((WordToken)first).value);
        } else if (first instanceof BracketToken && ((BracketToken)first).type == Type.LEFT) {
            Value value = execValueBracket();
            ++this.index;
            return value;
        } else if (first instanceof OpToken) {
            String op = ((OpToken)first).name;
            switch (op) {
                // keywords
                case "return":
                {
                    // TODO: return should be first one of statement
                    if (!this.localScope.inFunction) throw new MuaException("Cannot return outside of a function");
                    ++this.index;
                    this.shouldReturn = true;
                    return this.retVal;
                }
                // other op, considered function
                default:
                {
                    ++this.index;
                    FunctionVal fn = Scope.getValue(globalScope, localScope, ((OpToken) first).name).asFunctionVal();
                    List<Value> params = new Vector<>();
                    for (int i = 0; i < fn.paramsCount(); ++i) {
                        params.add(this.execValue());
                    }
                    return fn.run(globalScope, localScope, params);
                }
            }
        } else if (first instanceof MathToken) {
            switch (((MathToken)first).op) {
                case "(":
                    ++this.index;
                    return this.execValueLeftParen();
                case ")":
                    return this.execValueRightParen();
                default:
                    this.execValueMath();
                    return null;
            }
        } else throw new MuaException(String.format("Unexpected token: %s", first));
    }

    // consume all tokens or throw exception
    public void execAll() throws MuaException {
        while (!this.shouldReturn && this.index < this.tokens.size()) {
            this.retVal = this.execValue();
        } 
    }

    // feed tokens
    public void feed(List<Token> tokens) {
        this.tokens.addAll(tokens);
    }

    // execute tokens, might return null
    public static Value execTokens(Scope globalScope, Scope localScope, List<Token> tokens) throws MuaException {
        Runner runner = new Runner(globalScope, localScope);
        runner.feed(tokens);
        runner.execAll();
        return runner.retVal;
    }

    // execute list by joining words together with whitespaces, tokenize and execTokens
    public static Value execList(Scope globalScope, Scope localScope, ListVal list) throws MuaException {
        StringBuffer buf = new StringBuffer();
        for (Value value : list.elements) {
            // throws error if any value cannot be converted to a literal
            // i.e., lists and functions
            String str = value.asLiteralVal().content;
            buf.append(str).append(' ');
        } 
        try {
            List<Token> tokens = Tokenizer.tokenize(buf.toString());
            return execTokens(globalScope, localScope, tokens);
        } catch (TokenizerException e) {
            throw new MuaException(String.format("Tokenize error: %s", e.getMessage()));
        }
    }
}
