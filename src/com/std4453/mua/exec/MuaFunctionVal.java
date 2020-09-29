package com.std4453.mua.exec;

import java.util.List;

public class MuaFunctionVal extends FunctionVal {
    private final ListVal paramList;
    private final ListVal code;

    public MuaFunctionVal(ListVal definition) throws MuaException {
        this.paramList = definition.elements.get(0).asListVal();
        this.code = definition.elements.get(1).asListVal();
    }

    // MuaFunctionVal instances are only constructed by ListVal.asFunctionVal() when
    // calling a function, the result won't be stored anywhere so it don't need convertions

    @Override
    public int paramsCount() {
        return this.paramList.elements.size(); 
    }

    @Override
    public Value run(Scope globalScope, Scope outerScope, List<Value> params) throws MuaException {
        Scope localScope = new Scope(true);
        for (int i = 0; i < this.paramsCount(); ++i) {
            var key = this.paramList.elements.get(i).asLiteralVal().content;
            var value = params.get(i);
            localScope.variables.put(key, new Scope.Entry(true, value));
        }
        return Runner.execList(globalScope, localScope, this.code);
    }

    @Override
    public String toString() {
        var buf = new StringBuffer();
        buf.append('(');
        for (int i = 0 ; i < this.paramList.elements.size(); ++i) {
            if (i != 0) buf.append(' ');
            buf.append(this.paramList.elements.get(i));
        }
        buf.append(") -> [");
        for (int i = 0; i < this.code.elements.size(); ++i) {
            if (i != 0)
                buf.append(' ');
            buf.append(this.code.elements.get(i));
        }
        buf.append(']');
        return buf.toString();
    }
}
