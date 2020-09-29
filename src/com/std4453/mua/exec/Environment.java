package com.std4453.mua.exec;

import java.util.Scanner;

import com.std4453.mua.token.Tokenizer;
import com.std4453.mua.token.TokenizerException;

public class Environment {
    public Scope globalScope;

    // convenient method for defining global function
    private void define(String name, boolean modifiable, int paramsCount, FunctionVal.InternalFunction fn) {
        this.globalScope.variables.put(name, new Scope.Entry(modifiable, FunctionVal.makeInternalFunction(paramsCount, fn)));
    }

    public Environment() {
        this.globalScope = new Scope(false);

        // basic operations
        define("make", false, 2, (globalScope, outerScope, params) -> {
            var name = params.get(0).asLiteralVal().content;
            var val = params.get(1);
            var entry = outerScope.variables.get(name);
            if (entry != null && !entry.modifiable) {
                throw new MuaException(String.format("Cannot overrite variable \"%s\"", name));
            }
            outerScope.variables.put(name, new Scope.Entry(true, val));
            return val;
        });
        define("thing", false, 1, (globalScope, outerScope, params) -> {
            var name = params.get(0).asLiteralVal().content;
            return Scope.getValue(globalScope, outerScope, name);
        });
        define("export", false, 1, (globalScope, outerScope, params) -> {
            // not in function
            if (!outerScope.inFunction) throw new MuaException("Export can be called in function only"); 
            var name = params.get(0).asLiteralVal().content;
            if (!outerScope.variables.containsKey(name)) {
                throw new MuaException(String.format("Variable %s not in local scope", name));
            }
            var value = outerScope.variables.get(name).value;
            if (globalScope.variables.containsKey(name) && !globalScope.variables.get(name).modifiable) {
                throw new MuaException(String.format("Variable %s in global scope is not modifiable", name));
            }
            globalScope.variables.put(name, new Scope.Entry(true, value));
            return value;
        });
        define("print", false, 1, (globalScope, outerScope, params) -> {
            var value = params.get(0);
            System.out.println(value.toString());
            return value;
        });
        // TODO: read

        define("add", false, 2, (globalScope, outerScope, params) -> {
            double a = params.get(0).asNumberVal().content;
            double b = params.get(1).asNumberVal().content;
            return new NumberVal(a + b);
        });
        define("sub", false, 2, (globalScope, outerScope, params) -> {
            double a = params.get(0).asNumberVal().content;
            double b = params.get(1).asNumberVal().content;
            return new NumberVal(a - b);
        });
        define("mul", false, 2, (globalScope, outerScope, params) -> {
            double a = params.get(0).asNumberVal().content;
            double b = params.get(1).asNumberVal().content;
            return new NumberVal(a * b);
        });
        define("div", false, 2, (globalScope, outerScope, params) -> {
            double a = params.get(0).asNumberVal().content;
            double b = params.get(1).asNumberVal().content;
            return new NumberVal(a / b);
        });
        define("mod", false, 2, (globalScope, outerScope, params) -> {
            double a = params.get(0).asNumberVal().content;
            double b = params.get(1).asNumberVal().content;
            return new NumberVal(a % b); // works for floating points as well
        });

        define("erase", false, 1, (globalScope, outerScope, params) -> {
            var name = params.get(0).asLiteralVal().content;
            if (outerScope.variables.containsKey(name)) {
                var entry = outerScope.variables.get(name);
                if (!entry.modifiable) throw new MuaException(String.format("Cannot erase variable %s", name));
                outerScope.variables.remove(name);
                return entry.value;
            } else if (globalScope.variables.containsKey(name)) {
                var entry = globalScope.variables.get(name);
                if (!entry.modifiable) throw new MuaException(String.format("Cannot erase variable %s", name));
                globalScope.variables.remove(name);
                return entry.value;
            } else throw new MuaException(String.format("Variable %s not in scope", name));
        });
        define("isname", false, 1, (globalScope, outerScope, params) -> {
            var name = params.get(0).asLiteralVal().content;
            var isName = outerScope.variables.containsKey(name) || globalScope.variables.containsKey(name);
            return new BooleanVal(isName);
        });
        // TODO: readlist
        define("repeat", false, 2, (globalScope, outerScope, params) -> {
            var number = params.get(0).asNumberVal().content;
            var list = params.get(1).asListVal();
            Value retVal = null;
            for (int i = 0; i < number; ++i) {
                retVal = Runner.execList(globalScope, outerScope, list);
            }
            return retVal;
        });

        // if both numbers, compare them, otherwise compare words in lexicographical order
        // TODO: auto type convertion
        define("eq", false, 2, (globalScope, outerScope, params) -> {
            if (params.get(0) instanceof NumberVal && params.get(1) instanceof NumberVal) {
                var a = params.get(0).asNumberVal().content;
                var b = params.get(1).asNumberVal().content;
                return new BooleanVal(a == b);
            } else {
                var a = params.get(0).asLiteralVal().content;
                var b = params.get(1).asLiteralVal().content;
                return new BooleanVal(a.compareTo(b) == 0);
            }
        });
        define("lt", false, 2, (globalScope, outerScope, params) -> {
            if (params.get(0) instanceof NumberVal && params.get(1) instanceof NumberVal) {
                var a = params.get(0).asNumberVal().content;
                var b = params.get(1).asNumberVal().content;
                return new BooleanVal(a < b);
            } else {
                var a = params.get(0).asLiteralVal().content;
                var b = params.get(1).asLiteralVal().content;
                return new BooleanVal(a.compareTo(b) < 0);
            }
        });
        define("gt", false, 2, (globalScope, outerScope, params) -> {
            if (params.get(0) instanceof NumberVal && params.get(1) instanceof NumberVal) {
                var a = params.get(0).asNumberVal().content;
                var b = params.get(1).asNumberVal().content;
                return new BooleanVal(a > b);
            } else {
                var a = params.get(0).asLiteralVal().content;
                var b = params.get(1).asLiteralVal().content;
                return new BooleanVal(a.compareTo(b) > 0);
            }
        });
        // no short circuit
        define("and", false, 2, (globalScope, outerScope, params) -> {
            var a = params.get(0).asBooleanVal().content;
            var b = params.get(1).asBooleanVal().content;
            return new BooleanVal(a && b);
        });
        define("or", false, 2, (globalScope, outerScope, params) -> {
            var a = params.get(0).asBooleanVal().content;
            var b = params.get(1).asBooleanVal().content;
            return new BooleanVal(a || b);
        });
        define("not", false, 1, (globalScope, outerScope, params) -> {
            var val = params.get(0).asBooleanVal().content;
            return new BooleanVal(!val);
        });

        define("if", false, 3, (globalScope, outerScope, params) -> {
            var cond = params.get(0).asBooleanVal().content;
            var list1 = params.get(1).asListVal();
            var list2 = params.get(2).asListVal();
            if (cond) {
                return Runner.execList(globalScope, outerScope, list1);
            } else {
                return Runner.execList(globalScope, outerScope, list2);
            } 
        });
    }

    public static void main(String[] args) {
        try (var scanner = new Scanner(System.in)) {
            var env = new Environment();
            do {
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                var line = scanner.nextLine();
                try {
                    var tokens = Tokenizer.tokenize(line + "\n"); 
                    var value = Runner.execTokens(env.globalScope, env.globalScope, tokens);
                    System.out.println(value);
                } catch (TokenizerException e) {
                    // System.out.println(e.getMessage());
                    e.printStackTrace();
                } catch (MuaException e) {
                    // System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            } while (true);
        }
    }
}
