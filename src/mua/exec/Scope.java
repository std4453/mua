package mua.exec;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Scope implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -4140625326865167847L;

    public static class Entry implements Serializable {
        /**
         *
         */
        private static final long serialVersionUID = -9168952513947484387L;

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

    // generate a temporary scope with all local values removed
    public Scope removeInternals() {
        Scope ret = new Scope(this.inFunction);
        for (Map.Entry<String, Entry> entry : this.variables.entrySet()) {
            if (!entry.getValue().modifiable) continue;
            ret.variables.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }

    // merge given scope into current, throwing an exception on overwriting an unmodifiable variable
    public void mergeScope(Scope scope) throws MuaException {
        for (Map.Entry<String, Entry> entry : scope.variables.entrySet()) {
            if (!entry.getValue().modifiable) continue;
            String key = entry.getKey();
            if (this.variables.containsKey(key) && !this.variables.get(key).modifiable) {
                throw new MuaException(String.format("Trying to overwrite unmodifiable variable \"%s\"", key));
            }
            this.variables.put(key, entry.getValue());
        }
    }
}
