package mua.exec;

import java.io.Serializable;

public class Value implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 5790626257369920438L;

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

    public boolean isBooleanVal() {
        try {
            this.asBooleanVal();
            return true;
        } catch (MuaException e) {
            return false;
        }
    }

    public boolean isNumberVal() {
        try {
            this.asNumberVal();
            return true;
        } catch (MuaException e) {
            return false;
        }
    }

    public boolean isListVal() {
        try {
            this.asListVal();
            return true;
        } catch (MuaException e) {
            return false;
        }
    }

    public boolean isLiteralVal() {
        try {
            this.asLiteralVal();
            return true;
        } catch (MuaException e) {
            return false;
        }
    }

    public boolean isFunctionVal() {
        try {
            this.asFunctionVal();
            return true;
        } catch (MuaException e) {
            return false;
        }
    }

    public String toMakableString() throws MuaException {
        return this.toString();
    }
}
