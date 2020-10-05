package mua.exec;

public class NumberVal extends Value {
    public final double content;

    public NumberVal(double content) {
        this.content = content;
    }

    @Override
    public NumberVal asNumberVal() throws MuaException {
        return this;
    }

    @Override
    public String toString() {
        return Double.toString(this.content);
    }
}
