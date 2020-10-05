package mua.exec;

import java.util.List;
import java.util.Vector;

import mua.token.BracketToken;
import mua.token.OpToken;
import mua.token.Token;
import mua.token.Tokenizer;
import mua.token.TokenizerException;
import mua.token.WordToken;
import mua.token.BracketToken.Type;

public class Runner {
    private Scope globalScope;
    private Scope localScope;

    private List<Token> tokens;
    private int index;
    
    private boolean shouldReturn;
    private Value retVal;

    public Runner(Scope globalScope, Scope localScope) {
        this.globalScope = globalScope;
        this.localScope = localScope;

        this.tokens = new Vector<>();
        this.index = 0;

        this.shouldReturn = false;
        this.retVal = null;
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
