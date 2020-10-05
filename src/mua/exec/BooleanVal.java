package mua.exec;

public class BooleanVal extends Value {
    public final boolean content;

    public BooleanVal(boolean content) {
        this.content = content;
    }

    @Override
    public BooleanVal asBooleanVal() throws MuaException {
        return this;
    }
    
    @Override
    public LiteralVal asLiteralVal() throws MuaException {
        return new LiteralVal(this.content ? "true" : "false");
    }

    @Override
    public String toString() {
        return Boolean.toString(this.content);
    }
}
