package mua;

import mua.exec.Environment;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var env = new Environment()) {
            while (env.scanner.hasNextLine()) {
                env.execLine();
            }
        }
    }
}
