package mua.token;

import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mua.token.BracketToken.Type;

public class Tokenizer {
    private enum State {
        INIT, WORD, NUMBER, COLON, LEFT_BRACKET, LIST, LIST_ITEM, OP, ERROR, FINISH, MINUS
    }

    private static final String WHITESPACES = " \t\n";
    private static final String TOKEN_BOUNDS = WHITESPACES + "()+-*/%";
    private static final String WORD_BOUNDS = WHITESPACES;
    private static final String TOKEN_DELIMITER = " \t\n)";

    // global states
    private State state;
    private String input;
    private int index;
    private Vector<Token> tokens;

    // state-specific states
    private StringBuffer buf = null;
    private int listLevel = 0;

    public Tokenizer() {
        this.state = State.INIT;
        this.tokens = new Vector<>();
    }

    // init state, state before a new token, usually at start of input or after whitespace, not when in list
    private void initState() throws TokenizerException {
        final char ch = this.input.charAt(this.index);
        switch (ch) {
            // whitespaces, igonre
            case ' ': case '\t': case '\n':
                this.state = State.INIT;
                ++this.index;
                break;
            // start of word, skip '"'
            case '"':
                this.state = State.WORD;
                this.buf = new StringBuffer();
                ++this.index;
                break;
            // numbers, don't skip
            case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                this.state = State.NUMBER;
                break;
            // left bracket, don't skip
            case '[':
                this.state = State.LEFT_BRACKET;
                break;
            // init state is not in list, so error
            case ']':
                throw new TokenizerException("Unexpected token ']'!");
            // skip, enter colon state
            case ':':
                ++this.index;
                this.buf = new StringBuffer();
                this.tokens.add(new OpToken("thing"));
                this.state = State.COLON;
                break;
            // math tokens are considered separate
            // '-' can either be a operator or beginning of a negative number, however this cannot be
            // distinguished at tokenize pass, so '-' must go as a separate token type.
            // consider the code (foo 1 -2), we don't know whether '-' here is negative of minus
            // until runtime since foo can take one or two parameter(s).
            // so at tokenize pass, we treat '-' as math token always
            case '(': case ')': case '+': case '*': case '/': case '%': case '-':
                this.state = State.INIT;
                this.tokens.add(new MathToken(Character.toString(ch)));
                ++this.index;
                break;
            // otherwise
            default:
                // alpha, begin of op or bool
                if ("abcdefghijklmnopqrstuvwxyz".indexOf(ch) != -1) {
                    this.state = State.OP;
                    this.buf = new StringBuffer();
                // otherwise, error
                } else {
                    throw new TokenizerException(String.format("Unexcepted character '%c'", ch));
                }
                break;
        }
    }

    private void wordState() {
        final char ch = this.input.charAt(this.index);
        // ')' is not end of word
        if (WORD_BOUNDS.indexOf(ch) != -1) {
            this.state = State.INIT;
            this.tokens.add(new WordToken(buf.toString()));
            this.buf = null;
            ++this.index;
        // else, append char to buf and proceed
        } else {
            this.state = State.WORD;
            this.buf.append(ch);
            ++this.index;
        }
    }

    private void numberState() throws TokenizerException {
        // here, we simply let Scanner do the parsing for us, skip necessary characters and append the number
        try (Scanner scanner = new Scanner(this.input.substring(this.index))) {
            // stop at end of token
            scanner.useDelimiter(TOKEN_BOUNDS
                .chars()
                .mapToObj((int ch) -> Character.toString((char)ch))
                .map(Pattern::quote)
                .collect(Collectors.joining("|")));
            if (!scanner.hasNextDouble()) {
                throw new TokenizerException("Unable to parse number");
            } else {
                // numbers are converted on demand, stored as string, so next() instead of nextDouble()
                final String token = scanner.next();
                this.tokens.add(new WordToken(token));
                this.state = State.INIT;
                this.index += token.length();
            }
        }
    }

    // almost identical to op state
    private void colonState() throws TokenizerException {
        char ch = this.input.charAt(this.index);
        // only alphanumeric and underscore allowed in colon, append to buf
        if ("0123456789abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(ch) != -1) {
            this.state = State.COLON;
            this.buf.append(ch);
            ++this.index;
        // token end, insert, don't skip
        } else if (TOKEN_BOUNDS.indexOf(ch) != -1) {
            this.tokens.add(new WordToken(this.buf.toString()));
            this.buf = null;
            this.state = State.INIT;
        } else {
            throw new TokenizerException(String.format("Unexcepted character '%c'", ch));
        }
    }

    private void leftBracketState() {
        // skip bracket, increase list level, and enter list state
        this.state = State.LIST;
        ++this.listLevel;
        this.tokens.add(new BracketToken(Type.LEFT));
        ++this.index;
    }

    // begin of list item, or before right bracket
    private void listState() throws TokenizerException {
        char ch = this.input.charAt(this.index);
        // stop at word bounds since list items are words without preceding '"'
        if (WORD_BOUNDS.indexOf(ch) != -1) {
            this.state = State.LIST;
            ++this.index;
        // left bracket, begin of sublist,
        } else if (ch == '[') {
            this.state = State.LIST;
            ++this.listLevel;
            this.tokens.add(new BracketToken(Type.LEFT));
            ++this.index;
        // right bracket, end of list, decrease list level, insert right bracket, skip
        } else if (ch == ']') {
            --this.listLevel;
            if (this.listLevel < 0) throw new TokenizerException("Unmatched brackets!");
            this.state = this.listLevel == 0 ? State.INIT : State.LIST;
            this.tokens.add(new BracketToken(Type.RIGHT));
            ++this.index;
        // everything else can be begin of word, since list elements don't need preceeding '"'
        } else {
            this.state = State.LIST_ITEM;
            this.buf = new StringBuffer();
        } 
    }

    // inside list item, including before last char
    private void listItemState() {
        char ch = this.input.charAt(this.index);
        // right bracket or whitespace, don't skip, let list state handle it
        // TODO: this won't allow ']' to be in a word in a list, is this the desired behavior?
        if (ch == ']' || WHITESPACES.indexOf(ch) != -1) {
            this.state = State.LIST;
            this.tokens.add(new WordToken(this.buf.toString()));
            this.buf = null;
        } else {
            this.state = State.LIST_ITEM;
            this.buf.append(ch);
            ++this.index;
        }
    }

    private void opState() throws TokenizerException {
        char ch = this.input.charAt(this.index);
        // only alphanumeric and underscore allowed in op, append to buf
        if ("0123456789abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(ch) != -1) {
            this.state = State.OP;
            this.buf.append(ch);
            ++this.index;
        // token end, insert, don't skip
        } else if (TOKEN_BOUNDS.indexOf(ch) != -1) {
            this.tokens.add(new OpToken(this.buf.toString()));
            this.buf = null;
            this.state = State.INIT;
        } else {
            throw new TokenizerException(String.format("Unexcepted character '%c'", ch));
        }
    }

    private void minusState() throws TokenizerException {
        // TODO: look-forward, possible defects
        char next = this.input.charAt(this.index + 1);
        // token bound, append token and skip
        if (TOKEN_DELIMITER.indexOf(next) != -1) {
            this.state = State.INIT;
            this.tokens.add(new MathToken("-"));
            ++this.index;
        // digits, part of number
        } else if ("1234567890".indexOf(next) != -1) {
            this.state = State.NUMBER;
        } else throw new TokenizerException(String.format("Unexpected character '%c'", next));
    }

    public void feed(String input) throws TokenizerException {
        try {
            this.input = input;
            this.index = 0;
            while (this.index < this.input.length()) {
                switch (this.state) {
                    case INIT: this.initState(); break;
                    case WORD: this.wordState(); break;
                    case NUMBER: this.numberState(); break;
                    case COLON: this.colonState(); break;
                    case LEFT_BRACKET: this.leftBracketState(); break;
                    case LIST: this.listState(); break;
                    case LIST_ITEM: this.listItemState(); break;
                    case OP: this.opState(); break;
                    case MINUS: this.minusState(); break;
                    default: throw new TokenizerException("Invalid tokenizer state!");
                }
            }
        } catch (TokenizerException e) {
            this.state = State.ERROR;
            throw e;
        }
    }

    public List<Token> finish() throws TokenizerException {
        if (this.state != State.INIT) {
            throw new TokenizerException("Malformed input!");
        }
        this.buf = null;
        this.input = null;
        this.state = State.FINISH;
        return this.tokens;
    }

    public static List<Token> tokenize(String input) throws TokenizerException {
        Tokenizer tokenizer = new Tokenizer();
        tokenizer.feed(input);
        return tokenizer.finish();
    }

    // test
    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            do {
                System.out.print("> ");
                if (!scanner.hasNextLine())
                    break;
                String line = scanner.nextLine();
                try {
                    List<Token> tokens = Tokenizer.tokenize(line + ' ');
                    for (Token token : tokens) {
                        System.out.print(token);
                        System.out.print(' ');
                    }
                    System.out.println();
                } catch (TokenizerException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }
}
