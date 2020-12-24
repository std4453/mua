package mua.exec;

import java.util.ArrayList;
import java.util.List;

public class LiteralVal extends Value {
    public final String content;

    public LiteralVal(String content) {
        this.content = content;
    }

    @Override
    public LiteralVal asLiteralVal() throws MuaException {
        return this;
    }

    @Override
    public BooleanVal asBooleanVal() throws MuaException {
        if ("true".equals(this.content)) return new BooleanVal(true);
        if ("false".equals(this.content)) return new BooleanVal(false);
        throw new MuaException(String.format("Literal \"%s\" is not a boolean", this.content));
    }

    @Override
    public NumberVal asNumberVal() throws MuaException {
        try {
            return new NumberVal(Double.parseDouble(this.content));
        } catch (NumberFormatException e) {
            throw new MuaException(String.format("Literal \"%s\" is not a number", this.content));
        }
    }

    @Override
    public ListVal asListVal() throws MuaException {
        List<Value> elements = new ArrayList<>();
        elements.add(this);
        return new ListVal(elements);
    }

    @Override
    public String toString() {
        return this.content;
    }

    @Override
    public String toMakableString() throws MuaException {
        return "\"" + this.toString();
    }
}
