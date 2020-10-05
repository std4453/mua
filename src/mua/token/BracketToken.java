package mua.token;

public class BracketToken extends Token {
    public enum Type {
        LEFT, RIGHT
    }

    public final Type type;

    public BracketToken(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("BracketToken{type=%s}", type == Type.LEFT ? "LEFT" : "RIGHT");
    }
}
