package mua;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Debug {
    private static PrintStream ps;
    private static int level = 0;

    public static void log(Object ...params) {
        if (!"true".equals(System.getenv("DEBUG"))) return;
        if (ps == null) {
            try {
                OutputStream fos = Files.newOutputStream(Paths.get("debug.txt"));
                ps = new PrintStream(fos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        printTabs();
        for (Object param : params) {
            if (param instanceof String) {
                ps.print((String)param);
            } else if (param instanceof List) {
                ps.print("[\n");
                increaseLevel();
                for (Object obj : ((List<?>)param)) {
                    printTabs();
                    ps.printf("%s,\n", obj);
                }
                decreaseLevel();
                printTabs();
                ps.print("]");
            } else {
                ps.print(param);
            }
        }
    }

    private static void printTabs() {
        for (int i = 0; i < level; ++i) ps.print("\t");
    }

    public static void increaseLevel() {
        ++level;
    }

    public static void decreaseLevel() {
        --level;
    }
}
