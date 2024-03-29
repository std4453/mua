package mua.exec;

import java.util.List;

public abstract class FunctionVal extends Value {
    public abstract int paramsCount();
    public abstract Value run(Scope globalScope, Scope outerScope, List<Value> params) throws MuaException;

    @FunctionalInterface
    public interface InternalFunction {
        Value run(Scope globalScope, Scope outerScope, List<Value> params) throws MuaException;
    }

    @Override
    public FunctionVal asFunctionVal() throws MuaException {
        return this;
    }

    public static FunctionVal makeInternalFunction(int paramsCount, InternalFunction fn) {
        return new FunctionVal(){
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public int paramsCount() {
                return paramsCount;
            }

            @Override
            public Value run(Scope globalScope, Scope outerScope, List<Value> params) throws MuaException {
                return fn.run(globalScope, outerScope, params);
            }
        };
    }

    @Override
    public String toMakableString() throws MuaException {
        throw new MuaException("function cannot be converted to makeable string");
    }
}
