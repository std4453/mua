package mua.exec;

import java.util.HashMap;

public class Scope {
    public static class Entry {
        public final boolean modifiable;
        public final Value value;

        public Entry(boolean modifiable, Value value) {
            this.modifiable = modifiable;
            this.value = value;
        }
    }

    public final HashMap<String, Entry> variables;
    public final boolean inFunction;

    public Scope(boolean inFunction) {
        variables = new HashMap<>();
        this.inFunction = inFunction;
    }

    public static Value getValue(Scope globalScope, Scope localScope, String name) throws MuaException {
        if (localScope.variables.containsKey(name)) return localScope.variables.get(name).value;
        if (globalScope.variables.containsKey(name)) return globalScope.variables.get(name).value;
        throw new MuaException(String.format("Variable \"%s\" not found in scope", name));
    }
}
