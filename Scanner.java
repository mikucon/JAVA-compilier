package cop5556sp17;

import java.util.ArrayList;

//import javax.xml.stream.events.Characters;

//import cop5556sp17.Scanner.Kind;
//import cop5556sp17.Scanner.Token;

/**
 * @author xiaozhe
 * University of Florida
 * School projects
 */
public class Scanner {
    /**
     * Kind enum
     */

    public static enum Kind {
        IDENT(""), INT_LIT(""), KW_INTEGER("integer"), KW_BOOLEAN("boolean"),
        KW_IMAGE("image"), KW_URL("url"), KW_FILE("file"), KW_FRAME("frame"),
        KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"),
        SEMI(";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"),
        RBRACE("}"), ARROW("->"), BARARROW("|->"), OR("|"), AND("&"),
        EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), LE("<="), GE(">="),
        PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"),
        ASSIGN("<-"), OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE("convolve"),
        KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH("screenwidth"),
        OP_WIDTH("width"), OP_HEIGHT("height"), KW_XLOC("xloc"), KW_YLOC("yloc"),
        KW_HIDE("hide"), KW_SHOW("show"), KW_MOVE("move"), OP_SLEEP("sleep"),
        KW_SCALE("scale"), EOF("eof");

        Kind(String text) {
            this.text = text;
        }

        final String text;

        String getText() {
            return text;
        }

        int eval(int a, int b) {
            assert false: "illegal application of eval";
            return 0;
        }

    }

    public enum State {
        START, IN_DIGIT, IN_IDENT, AFTER_EQ;
    }

    /**
     * Thrown by Scanner when an illegal character is encountered
     */
    @SuppressWarnings("serial")
    public static class IllegalCharException extends Exception {
        public IllegalCharException(String message) {
            super(message);
        }
    }

    /**
     * Thrown by Scanner when an int literal is not a value that can be represented by an int.
     */
    @SuppressWarnings("serial")
    public static class IllegalNumberException extends Exception {
        public IllegalNumberException(String message) {
            super(message);
        }
    }


    /**
     * Holds the line and position in the line of a token.
     */
    static class LinePos {
        public final int line;
        public final int posInLine;

        public LinePos(int line, int posInLine) {
            super();
            this.line = line;
            this.posInLine = posInLine;
        }

        @Override
        public String toString() {
            return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
        }
    }

    int line;
    int newLinePos;
    int startLine;
    int startPosInLine;

    public class Token {
        public final Kind kind;
        public final int pos;  //position in input array
        public final int length;
        private final LinePos linePos;

        //returns the text of this Token
        public String getText() {
            //TODO IMPLEMENT THIS
            return chars.substring(pos, pos + length);
        }

        //returns a LinePos object representing the line and column of this Token
        LinePos getLinePos() {
            //TODO IMPLEMENT THIS
            return linePos;
        }

        Token(Kind kind, int pos, int length) {
            this.kind = kind;
            this.pos = pos;
            this.length = length;
            linePos = new LinePos(startLine, startPosInLine);
        }

        public boolean isKind(Kind... kinds) {
            for (Kind k : kinds) {
                if (k == kind) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((kind == null) ? 0 : kind.hashCode());
            result = prime * result + length;
            result = prime * result + pos;
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Token)) {
                return false;
            }
            Token other = (Token) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (kind != other.kind) {
                return false;
            }
            if (length != other.length) {
                return false;
            }
            if (pos != other.pos) {
                return false;
            }
            return true;
        }

        private Scanner getOuterType() {
            return Scanner.this;
        }


        /**
         * Precondition:  kind = Kind.INT_LIT,  the text can be represented with a Java int.
         * Note that the validity of the input should have been checked when the Token was created.
         * So the exception should never be thrown.
         *
         * @return int value of this token, which should represent an INT_LIT
         * @throws NumberFormatException
         */
        public int intVal() throws NumberFormatException {
            //TODO IMPLEMENT THIS
            return Integer.valueOf(this.getText());
        }

    }


    Scanner(String chars) {
        this.chars = chars;
        tokens = new ArrayList<Token>();


    }


    /**
     * Initializes Scanner object by traversing chars and adding tokens to tokens list.
     *
     * @return this scanner
     * @throws IllegalCharException
     * @throws IllegalNumberException
     */
    public Scanner scan() throws IllegalCharException, IllegalNumberException {
        int pos = 0;
        //TODO IMPLEMENT THIS!!!!
        int length = chars.length();
        State state = State.START;
        int startPos = 0;
        int ch;
        while (pos <= length) {
            ch = pos < length ? chars.charAt(pos) : -1;
            switch (state) {
                case START: {
                    pos = skipWhiteSpace(pos);
                    ch = pos < length ? chars.charAt(pos) : -1;
                    startPos = pos;
                    startPosInLine = pos - newLinePos;
                    startLine = line;
                    switch (ch) {
                        case -1: {
                            tokens.add(new Token(Kind.EOF, pos, 0));
                            pos++;
                        }
                        break;
                        // for one character
                        case '+': {
                            tokens.add(new Token(Kind.PLUS, pos, 1));
                            pos++;
                        }
                        break;
                        case '*': {
                            tokens.add(new Token(Kind.TIMES, pos, 1));
                            pos++;
                        }
                        break;
                        case '/': {
                            if (pos + 1 < length && chars.charAt(pos + 1) == '*') {
                                pos = skipComments(pos);
                            } else {
                                tokens.add(new Token(Kind.DIV, pos, 1));
                                pos++;
                            }
                        }
                        break;
                        case '%': {
                            tokens.add(new Token(Kind.MOD, pos, 1));
                            pos++;
                        }
                        break;
                        case '&': {
                            tokens.add(new Token(Kind.AND, pos, 1));
                            pos++;
                        }
                        break;
                        case '0': {
                            tokens.add(new Token(Kind.INT_LIT, startPos, 1));
                            pos++;
                        }
                        break;
                        case '{': {
                            tokens.add(new Token(Kind.LBRACE, pos, 1));
                            pos++;
                        }
                        break;
                        case '}': {
                            tokens.add(new Token(Kind.RBRACE, pos, 1));
                            pos++;
                        }
                        break;
                        case '(': {
                            tokens.add(new Token(Kind.LPAREN, pos, 1));
                            pos++;
                        }
                        break;
                        case ')': {
                            tokens.add(new Token(Kind.RPAREN, pos, 1));
                            pos++;
                        }
                        break;
                        case ',': {
                            tokens.add(new Token(Kind.COMMA, pos, 1));
                            pos++;
                        }
                        break;
                        case ';': {
                            tokens.add(new Token(Kind.SEMI, pos, 1));
                            pos++;
                        }
                        break;
                        //check the next character, use after
                        case '-': {
                            if (pos == length - 1) {
                                tokens.add(new Token(Kind.MINUS, startPos, 1));
                            } else state = State.AFTER_EQ;
                            pos++;
                        }
                        break;
                        case '|': {
                            if (pos == length - 1) {
                                tokens.add(new Token(Kind.OR, startPos, 1));
                            } else state = State.AFTER_EQ;
                            pos++;
                        }
                        break;
                        case '=': {
                            if (pos == length - 1) {
                                throw new IllegalCharException("illegal char");
                            }
                            state = State.AFTER_EQ;
                            pos++;
                        }
                        break;
                        case '>': {
                            if (pos == length - 1) {
                                tokens.add(new Token(Kind.GT, startPos, 1));
                            } else state = State.AFTER_EQ;
                            pos++;
                        }
                        break;
                        case '<': {
                            if (pos == length - 1) {
                                tokens.add(new Token(Kind.LT, startPos, 1));
                            } else state = State.AFTER_EQ;
                            pos++;
                        }
                        break;
                        case '!': {
                            if (pos == length - 1) {
                                tokens.add(new Token(Kind.NOT, startPos, 1));
                            } else state = State.AFTER_EQ;
                            pos++;
                        }
                        break;
                        default: {
                            if (Character.isDigit(ch)) {
                                state = State.IN_DIGIT;
                                pos++;
                            } else if (Character.isJavaIdentifierStart(ch)) {
                                state = State.IN_IDENT;
                                pos++;
                            } else {
                                throw new IllegalCharException("illegal char" + ch + "at pos" + pos);
                            }
                        }
                    }
                }
                break;
                case IN_DIGIT: {
                    if (Character.isDigit(ch)) {
                        pos++;
                    } else {
                        long value = Long.valueOf(chars.substring(startPos, pos));
                        if (value > Integer.MAX_VALUE) {
                            throw new IllegalNumberException("");
                        }
                        tokens.add(new Token(Kind.INT_LIT, startPos, pos - startPos));
                        state = State.START;
                    }
                }
                break;
                case IN_IDENT: {
                    if (Character.isJavaIdentifierPart(ch)) {
                        pos++;
                    } else {
                        AddKeywordToken(startPos, pos);
                        state = State.START;
                    }
                }
                break;
                case AFTER_EQ: {
                    switch (chars.charAt(startPos)) {
                        case ('='): {
                            if (chars.charAt(pos) == '=') {
                                tokens.add(new Token(Kind.EQUAL, startPos, 2));
                                state = State.START;
                                pos++;
                            } else {
                                throw new IllegalCharException("illegal char");
                            }
                        }
                        break;
                        case ('|'): {
                            if (chars.charAt(pos) == '-' && pos + 1 < length && chars.charAt(pos + 1) == '>') {
                                tokens.add(new Token(Kind.BARARROW, startPos, 3));
                                pos = pos + 2;
                            } else {
                                tokens.add(new Token(Kind.OR, startPos, 1));
                            }
                            state = State.START;
                        }
                        break;

                        case ('!'): {
                            if (chars.charAt(pos) == '=') {
                                tokens.add(new Token(Kind.NOTEQUAL, startPos, 2));
                                pos++;
                            } else {
                                tokens.add(new Token(Kind.NOT, startPos, 1));
                            }
                            state = State.START;
                        }
                        break;

                        case ('-'): {
                            if (chars.charAt(pos) == '>') {
                                tokens.add(new Token(Kind.ARROW, startPos, 2));
                                pos++;
                            } else {
                                tokens.add(new Token(Kind.MINUS, startPos, 1));
                            }
                            state = State.START;
                        }
                        break;

                        case ('>'): {
                            if (chars.charAt(pos) == '=') {
                                tokens.add(new Token(Kind.GE, startPos, 2));
                                pos++;
                            } else {
                                tokens.add(new Token(Kind.GT, startPos, 1));
                            }
                            state = State.START;
                        }
                        break;

                        case ('<'): {
                            if (chars.charAt(pos) == '=') {
                                tokens.add(new Token(Kind.LE, startPos, 2));
                                pos++;
                            } else if (chars.charAt(pos) == '-') {
                                tokens.add(new Token(Kind.ASSIGN, startPos, 2));
                                pos++;
                            } else {
                                tokens.add(new Token(Kind.LT, startPos, 1));
                            }
                            state = State.START;
                        }
                        break;
                        default:
                            throw new IllegalCharException("illegal char");
                    }
                }
                break;
                default:
                    throw new IllegalCharException("illegal char");
            }
        }
        switch (state) {
            case IN_DIGIT: {
                tokens.add(new Token(Kind.INT_LIT, length, 1));
            }
            break;
            case IN_IDENT: {
                tokens.add(new Token(Kind.IDENT, length, 1));
            }
            break;
        }
        return this;
    }

    private void AddKeywordToken(int startPos, int pos) {
        switch (chars.substring(startPos, pos)) {
            case ("image"): {
                tokens.add(new Token(Kind.KW_IMAGE, startPos, pos - startPos));
            }
            break;

            case ("boolean"): {
                tokens.add(new Token(Kind.KW_BOOLEAN, startPos, pos - startPos));
            }
            break;

            case ("integer"): {
                tokens.add(new Token(Kind.KW_INTEGER, startPos, pos - startPos));
            }
            break;

            case ("url"): {
                tokens.add(new Token(Kind.KW_URL, startPos, pos - startPos));
            }
            break;

            case ("file"): {
                tokens.add(new Token(Kind.KW_FILE, startPos, pos - startPos));
            }
            break;

            case ("frame"): {
                tokens.add(new Token(Kind.KW_FRAME, startPos, pos - startPos));
            }
            break;

            case ("while"): {
                tokens.add(new Token(Kind.KW_WHILE, startPos, pos - startPos));
            }
            break;

            case ("if"): {
                tokens.add(new Token(Kind.KW_IF, startPos, pos - startPos));
            }
            break;

            case ("true"): {
                tokens.add(new Token(Kind.KW_TRUE, startPos, pos - startPos));
            }
            break;

            case ("false"): {
                tokens.add(new Token(Kind.KW_FALSE, startPos, pos - startPos));
            }
            break;

            case ("blur"): {
                tokens.add(new Token(Kind.OP_BLUR, startPos, pos - startPos));
            }
            break;

            case ("gray"): {
                tokens.add(new Token(Kind.OP_GRAY, startPos, pos - startPos));
            }
            break;

            case ("convolve"): {
                tokens.add(new Token(Kind.OP_CONVOLVE, startPos, pos - startPos));
            }
            break;

            case ("screenheight"): {
                tokens.add(new Token(Kind.KW_SCREENHEIGHT, startPos, pos - startPos));
            }
            break;

            case ("screenwidth"): {
                tokens.add(new Token(Kind.KW_SCREENWIDTH, startPos, pos - startPos));
            }
            break;

            case ("width"): {
                tokens.add(new Token(Kind.OP_WIDTH, startPos, pos - startPos));
            }
            break;

            case ("height"): {
                tokens.add(new Token(Kind.OP_HEIGHT, startPos, pos - startPos));
            }
            break;

            case ("xloc"): {
                tokens.add(new Token(Kind.KW_XLOC, startPos, pos - startPos));
            }
            break;

            case ("yloc"): {
                tokens.add(new Token(Kind.KW_YLOC, startPos, pos - startPos));
            }
            break;

            case ("hide"): {
                tokens.add(new Token(Kind.KW_HIDE, startPos, pos - startPos));
            }
            break;

            case ("show"): {
                tokens.add(new Token(Kind.KW_SHOW, startPos, pos - startPos));
            }
            break;

            case ("move"): {
                tokens.add(new Token(Kind.KW_MOVE, startPos, pos - startPos));
            }
            break;

            case ("sleep"): {
                tokens.add(new Token(Kind.OP_SLEEP, startPos, pos - startPos));
            }
            break;

            case ("scale"): {
                tokens.add(new Token(Kind.KW_SCALE, startPos, pos - startPos));
            }
            break;

            case ("eof"): {
                tokens.add(new Token(Kind.EOF, startPos, pos - startPos));
            }
            break;

            default: {
                tokens.add(new Token(Kind.IDENT, startPos, pos - startPos));
            }
        }
    }

    private int skipWhiteSpace(int pos) {
        while (pos < chars.length() && Character.isWhitespace(chars.charAt(pos))) {
            if (chars.charAt(pos) == 10) {
                line++;
                newLinePos = pos + 1;
            }
            pos++;
        }
        return pos;
    }

    private int skipComments(int pos) {
        while (pos < chars.length() - 1 && (chars.charAt(pos) != '*' || chars.charAt(pos + 1) != '/')) {
            pos++;
            if (pos == chars.length() - 2){
                return chars.length();
            }
        }
        return pos + 2;
    }

    final ArrayList<Token> tokens;
    final String chars;
    int tokenNum;

    /*
     * Return the next token in the token list and update the state so that
     * the next call will return the Token..
     */
    public Token nextToken() {
        if (tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum++);
    }

    /*
     * Return the next token in the token list without updating the state.
     * (So the following call to next will return the same token.)
     */
    public Token peek() {
        if (tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum);
    }


    /**
     * Returns a LinePos object containing the line and position in line of the
     * given token.
     * <p>
     * Line numbers start counting at 0
     *
     * @param t
     * @return
     */
    public LinePos getLinePos(Token t) {
        //TODO IMPLEMENT THIS
        //return null;
        return t.getLinePos();
    }
}
