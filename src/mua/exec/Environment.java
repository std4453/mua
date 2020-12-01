package mua.exec;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mua.Debug;
import mua.token.Token;
import mua.token.Tokenizer;
import mua.token.TokenizerException;

public class Environment implements AutoCloseable {
    public Scope globalScope;
    public Scanner scanner;

    private Random random;

    // convenient method for defining global function
    private void define(String name, boolean modifiable, int paramsCount, FunctionVal.InternalFunction fn) {
        this.globalScope.variables.put(name, new Scope.Entry(modifiable, FunctionVal.makeInternalFunction(paramsCount, fn)));
    }

    public Environment() {
        this.globalScope = new Scope(false);
        this.scanner = new Scanner(System.in);
        this.random = new Random();

        // boolean values are functions
        define("true", false, 0, (globalScope, outerScope, params) -> {
            return new BooleanVal(true);
        });
        define("false", false, 0, (globalScope, outerScope, params) -> {
            return new BooleanVal(false);
        });

        // basic operations
        define("make", false, 2, (globalScope, outerScope, params) -> {
            String name = params.get(0).asLiteralVal().content;
            Value val = params.get(1);
            Scope.Entry entry = outerScope.variables.get(name);
            if (entry != null && !entry.modifiable) {
                throw new MuaException(String.format("Cannot overrite variable \"%s\"", name));
            }
            outerScope.variables.put(name, new Scope.Entry(true, val));
            return val;
        });
        define("thing", false, 1, (globalScope, outerScope, params) -> {
            String name = params.get(0).asLiteralVal().content;
            return Scope.getValue(globalScope, outerScope, name);
        });
        define("export", false, 1, (globalScope, outerScope, params) -> {
            // not in function
            if (!outerScope.inFunction) throw new MuaException("Export can be called in function only"); 
            String name = params.get(0).asLiteralVal().content;
            if (!outerScope.variables.containsKey(name)) {
                throw new MuaException(String.format("Variable %s not in local scope", name));
            }
            Value value = outerScope.variables.get(name).value;
            if (globalScope.variables.containsKey(name) && !globalScope.variables.get(name).modifiable) {
                throw new MuaException(String.format("Variable %s in global scope is not modifiable", name));
            }
            globalScope.variables.put(name, new Scope.Entry(true, value));
            return value;
        });
        define("print", false, 1, (globalScope, outerScope, params) -> {
            Value value = params.get(0);
            System.out.println(value.toString());
            return value;
        });
        define("read", false, 0, (globalScope, outerScope, params) -> {
            if (!this.scanner.hasNext()) throw new MuaException("Unable to read word.");
            return new LiteralVal(this.scanner.next());
        });

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
            String name = params.get(0).asLiteralVal().content;
            if (outerScope.variables.containsKey(name)) {
                Scope.Entry entry = outerScope.variables.get(name);
                if (!entry.modifiable) throw new MuaException(String.format("Cannot erase variable %s", name));
                outerScope.variables.remove(name);
                return entry.value;
            } else if (globalScope.variables.containsKey(name)) {
                Scope.Entry entry = globalScope.variables.get(name);
                if (!entry.modifiable) throw new MuaException(String.format("Cannot erase variable %s", name));
                globalScope.variables.remove(name);
                return entry.value;
            } else throw new MuaException(String.format("Variable %s not in scope", name));
        });
        define("isname", false, 1, (globalScope, outerScope, params) -> {
            String name = params.get(0).asLiteralVal().content;
            boolean isName = outerScope.variables.containsKey(name) || globalScope.variables.containsKey(name);
            return new BooleanVal(isName);
        });
        define("readlist", false, 0, (globalScope, outerScope, params) -> {
            if (!this.scanner.hasNextLine())
                throw new MuaException("Unable to read line");
            String line = this.scanner.nextLine();
            List<Value> values = Stream
                .of(line.split("\\s+"))
                .map((String word) -> new LiteralVal(word))
                .collect(Collectors.toList());
            return new ListVal(values);
        });
        // MUA P3: disabled temporarily
        // define("repeat", false, 2, (globalScope, outerScope, params) -> {
        //     double number = params.get(0).asNumberVal().content;
        //     ListVal list = params.get(1).asListVal();
        //     Value retVal = null;
        //     for (int i = 0; i < number; ++i) {
        //         retVal = Runner.execList(globalScope, outerScope, list);
        //     }
        //     return retVal;
        // });

        // if both numbers, compare them, otherwise compare words in lexicographical order
        define("eq", false, 2, (globalScope, outerScope, params) -> {
            if (params.get(0).isNumberVal() && params.get(1).isNumberVal()) {
                double a = params.get(0).asNumberVal().content;
                double b = params.get(1).asNumberVal().content;
                return new BooleanVal(a == b);
            } else {
                String a = params.get(0).asLiteralVal().content;
                String b = params.get(1).asLiteralVal().content;
                return new BooleanVal(a.compareTo(b) == 0);
            }
        });
        define("lt", false, 2, (globalScope, outerScope, params) -> {
            if (params.get(0).isNumberVal() && params.get(1).isNumberVal()) {
                double a = params.get(0).asNumberVal().content;
                double b = params.get(1).asNumberVal().content;
                return new BooleanVal(a < b);
            } else {
                String a = params.get(0).asLiteralVal().content;
                String b = params.get(1).asLiteralVal().content;
                return new BooleanVal(a.compareTo(b) < 0);
            }
        });
        define("gt", false, 2, (globalScope, outerScope, params) -> {
            if (params.get(0).isNumberVal() && params.get(1).isNumberVal()) {
                double a = params.get(0).asNumberVal().content;
                double b = params.get(1).asNumberVal().content;
                return new BooleanVal(a > b);
            } else {
                String a = params.get(0).asLiteralVal().content;
                String b = params.get(1).asLiteralVal().content;
                return new BooleanVal(a.compareTo(b) > 0);
            }
        });
        // no short circuit
        define("and", false, 2, (globalScope, outerScope, params) -> {
            boolean a = params.get(0).asBooleanVal().content;
            boolean b = params.get(1).asBooleanVal().content;
            return new BooleanVal(a && b);
        });
        define("or", false, 2, (globalScope, outerScope, params) -> {
            boolean a = params.get(0).asBooleanVal().content;
            boolean b = params.get(1).asBooleanVal().content;
            return new BooleanVal(a || b);
        });
        define("not", false, 1, (globalScope, outerScope, params) -> {
            boolean val = params.get(0).asBooleanVal().content;
            return new BooleanVal(!val);
        });

        // given mua's lazy evaluation nature, if can be implemented as an
        // internal function
        define("if", false, 3, (globalScope, outerScope, params) -> {
            boolean cond = params.get(0).asBooleanVal().content;
            ListVal list1 = params.get(1).asListVal();
            ListVal list2 = params.get(2).asListVal();
            if (cond) {
                return Runner.execList(globalScope, outerScope, list1);
            } else {
                return Runner.execList(globalScope, outerScope, list2);
            } 
        });
        define("isnumber", false, 1, (globalScope, outerScope, params) -> {
            return new BooleanVal(params.get(0).isNumberVal());
        });
        define("isword", false, 1, (globalScope, outerScope, params) -> {
            return new BooleanVal(params.get(0).isLiteralVal());
        });
        define("islist", false, 1, (globalScope, outerScope, params) -> {
            return new BooleanVal(params.get(0).isListVal());
        });
        define("isbool", false, 1, (globalScope, outerScope, params) -> {
            return new BooleanVal(params.get(0).isBooleanVal());
        });
        // list and words don't convert, so it's fine
        define("isempty", false, 1, (globalScope, outerScope, params) -> {
            if (params.get(0).isLiteralVal()) {
                return new BooleanVal(params.get(0).asLiteralVal().content.isEmpty());
            } else if (params.get(0).isListVal()) {
                return new BooleanVal(params.get(0).asListVal().elements.isEmpty());
            } else throw new MuaException("Value is not word or list");
        });

        // type convertion is done automatically by asLiteralVal()
        define("word", false, 2, (globalScope, outerScope, params) -> {
            String pre = params.get(0).asLiteralVal().content;
            String post = params.get(1).asLiteralVal().content;
            return new LiteralVal(pre + post);
        });
        define("sentence", false, 2, (globalScope, outerScope, params) -> {
            return new ListVal(Stream
                .concat(
                    params.get(0).asListVal().elements.stream(),
                    params.get(1).asListVal().elements.stream())
                .collect(Collectors.toList()));
        });
        define("list", false, 2, (globalScope, outerScope, params) -> {
            List<Value> list = new Vector<>();
            list.add(params.get(0));
            list.add(params.get(1));
            return new ListVal(list);
        });
        define("join", false, 2, (globalScope, outerScope, params) -> {
            return new ListVal(Stream
                .concat(
                    params.get(0).asListVal().elements.stream(),
                    Stream.of(params.get(1)))
                .collect(Collectors.toList()));
        });
        define("first", false, 1, (globalScope, outerScope, params) -> {
            Value val = params.get(0);
            if (val.isLiteralVal()) {
                return new LiteralVal(Character.toString(
                    val.asLiteralVal().content.charAt(0)
                ));
            } else {
                return val.asListVal().elements.firstElement();
            }
        });
        define("last", false, 1, (globalScope, outerScope, params) -> {
            Value val = params.get(0);
            if (val.isLiteralVal()) {
                String str = val.asLiteralVal().content;
                return new LiteralVal(Character.toString(
                    str.charAt(str.length() - 1)
                ));
            } else {
                return val.asListVal().elements.lastElement();
            }
        });
        define("butfirst", false, 1, (globalScope, outerScope, params) -> {
            Value val = params.get(0);
            if (val.isLiteralVal()) {
                String str = val.asLiteralVal().content;
                return new LiteralVal(str.substring(1));
            } else {
                List<Value> values = val.asListVal().elements;
                return new ListVal(values.subList(1, values.size()));
            }
        });
        define("butlast", false, 1, (globalScope, outerScope, params) -> {
            Value val = params.get(0);
            if (val.isLiteralVal()) {
                String str = val.asLiteralVal().content;
                return new LiteralVal(str.substring(0, str.length() - 1));
            } else {
                List<Value> values = val.asListVal().elements;
                return new ListVal(values.subList(0, values.size() - 1));
            }
        });

        define("random", false, 1, (globalScope, outerScope, params) -> {
            // this allows non-positive bounds, which does not exactly match
            // the [0, number) requirement
            double bound = params.get(0).asNumberVal().content;
            return new NumberVal(this.random.nextDouble() * bound);
        });
        define("int", false, 1, (globalScope, outerScope, params) -> {
            double value = params.get(0).asNumberVal().content;
            return new NumberVal(Math.floor(value));
        });
        define("sqrt", false, 1, (globalScope, outerScope, params) -> {
            double value = params.get(0).asNumberVal().content;
            // if value is negative, will return NaN
            return new NumberVal(Math.sqrt(value));
        });

        define("save", false, 1, (globalScope, outerScope, params) -> {
            String filename = params.get(0).asLiteralVal().content;
            try(FileOutputStream fos = new FileOutputStream(filename)) {
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                // save scope simply by writeObject(), since Scope and Value are serializable
                // and immutable
                oos.writeObject(outerScope.removeInternals());
            } catch (IOException e) {
                throw new MuaException(String.format("Cannot save file: %s", e.getMessage()));
            }
            return new LiteralVal(filename);
        });
        define("load", false, 1, (globalScope, outerScope, params) -> {
            String filename = params.get(0).asLiteralVal().content;
            try (FileInputStream fis = new FileInputStream(filename)) {
                ObjectInputStream ois = new ObjectInputStream(fis);
                Object obj = ois.readObject();
                if (!(obj instanceof Scope)) {
                    throw new MuaException(String.format("Invalid file \"%s\"", filename));
                }
                outerScope.mergeScope((Scope)obj);
            } catch (IOException | ClassNotFoundException e) {
                throw new MuaException(String.format("Cannot load file: %s", e.getMessage()));
            }
            return new BooleanVal(true);
        });
        define("erall", false, 0, (globalScope, outerScope, params) -> {
            List<String> erases = new Vector<>();
            for (Map.Entry<String, Scope.Entry> entry : outerScope.variables.entrySet()) {
                if (entry.getValue().modifiable) {
                    erases.add(entry.getKey());
                }
            }
            for (String key : erases) {
                outerScope.variables.remove(key);
            }
            return new BooleanVal(true);
        });
        define("poall", false, 0, (globalScope, outerScope, params) -> {
            List<Value> names = new Vector<>();
            for (Map.Entry<String, Scope.Entry> entry : outerScope.variables.entrySet()) {
                // non-modifiable values are considered internal and
                // not displayed by poall
                if (entry.getValue().modifiable) {
                    names.add(new LiteralVal(entry.getKey()));
                }
            }
            return new ListVal(names);
        });

        this.globalScope.variables.put("pi", new Scope.Entry(true, new NumberVal(Math.PI)));
        // run is now internal
        define("run", false, 1, (globalScope, outerScope, params) -> {
            ListVal list = params.get(0).asListVal();
            return Runner.execList(globalScope, outerScope, list);
        });
    }

    public Value execLine() throws MuaException, TokenizerException {
        Debug.log("execLine {\n");
        Debug.increaseLevel();
        String line = scanner.nextLine();
        Debug.log("input: ", line, "\n");
        List<Token> tokens = Tokenizer.tokenize(line + "\n");
        Value value = Runner.execTokens(this.globalScope, this.globalScope, tokens);
        Debug.decreaseLevel();
        Debug.log("}\n");
        return value;
    }

    // REPL
    public static void main(String[] args) {
        try (Environment env = new Environment()) {
            do {
                System.out.print("> ");
                if (!env.scanner.hasNextLine()) break;
                try {
                    Value value = env.execLine();
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

    @Override
    public void close() {
        if (this.scanner != null) this.scanner.close();
    }
}
