package com.std4453.mua.token;

public class OpToken extends Token {
    public final String name;

    public OpToken(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("OpToken{name=%s}", this.name);
    }
}
