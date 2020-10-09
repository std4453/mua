package mua.token;

public class MathToken extends Token {
    public final String op;

    public MathToken(String op) {
        this.op = op;
    }

    @Override
    public String toString() {
        return String.format("MathToken{op=%s}", this.op);
    }
}
