package mua.token;

public class WordToken extends Token {
    public final String value;

    public WordToken(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("WordToken{value=\"%s\"}", this.value);
    }
}
