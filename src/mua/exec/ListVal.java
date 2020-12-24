package mua.exec;

import java.util.List;
import java.util.Vector;

public class ListVal extends Value {
    public final Vector<Value> elements;

    public ListVal() {
        this.elements = new Vector<>();
    }

    public ListVal(List<Value> elements) {
        this.elements = new Vector<>(elements);
    }

    @Override
    public ListVal asListVal() throws MuaException {
        return this;
    }

    @Override
    public FunctionVal asFunctionVal() throws MuaException {
        if (this.elements.size() != 2) throw new MuaException("Cannot convert to function");
        return new MuaFunctionVal(this);
    }

    @Override
    public LiteralVal asLiteralVal() throws MuaException {
        return new LiteralVal(this.toString());
    }

    public String toString(boolean noBrackets) {
        StringBuffer buf = new StringBuffer();
        if (!noBrackets) buf.append('[');
        for (int i = 0; i < this.elements.size(); ++i) {
            if (i != 0)
                buf.append(' ');
            buf.append(this.elements.get(i));
        }
        if (!noBrackets) buf.append(']');
        return buf.toString();
    }

    @Override
    public String toString() {
        return this.toString(false);
    }
}
