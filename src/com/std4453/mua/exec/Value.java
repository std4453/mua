package com.std4453.mua.exec;

public class Value {
    public BooleanVal asBooleanVal() throws MuaException {
        throw new MuaException("Cannot convert to boolean");
    }

    public FunctionVal asFunctionVal() throws MuaException {
        throw new MuaException("Cannot convert to function");
    }

    public ListVal asListVal() throws MuaException {
        throw new MuaException("Cannot convert to list");
    }

    public LiteralVal asLiteralVal() throws MuaException {
        throw new MuaException("Cannot convert to literal");
    }

    public NumberVal asNumberVal() throws MuaException {
        throw new MuaException("Cannot convert to number");
    }
}
