package mua;

import java.util.Scanner;

import mua.exec.Environment;
import mua.exec.Runner;
import mua.token.Tokenizer;

public class Main {
    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            var env = new Environment();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                var tokens = Tokenizer.tokenize(line + "\n");
                Runner.execTokens(env.globalScope, env.globalScope, tokens);
            }
        }
    }
}
