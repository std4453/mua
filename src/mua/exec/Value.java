package mua.exec;

public class Value {
    public BooleanVal asBooleanVal() throws MuaException {
        throw new MuaException(String.format("Cannot convert to boolean: %s", this));
    }

    public FunctionVal asFunctionVal() throws MuaException {
        throw new MuaException(String.format("Cannot convert to function: %s", this));
    }

    public ListVal asListVal() throws MuaException {
        throw new MuaException(String.format("Cannot convert to list: %s", this));
    }

    public LiteralVal asLiteralVal() throws MuaException {
        throw new MuaException(String.format("Cannot convert to literal: %s", this));
    }

    public NumberVal asNumberVal() throws MuaException {
        throw new MuaException(String.format("Cannot convert to number: %s", this));
    }
}
