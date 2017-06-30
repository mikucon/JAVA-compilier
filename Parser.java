package cop5556sp17;

import cop5556sp17.AST.*;
import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;

import java.util.ArrayList;
import java.util.List;

import static cop5556sp17.Scanner.Kind.*;

/**
 * @author xiaozhe
 * University of Florida
 * School projects
 */
public class Parser {

    /**
     * Exception to be thrown if a syntax error is detected in the input.
     * You will want to provide a useful error message.
     */
    @SuppressWarnings("serial")
    public static class SyntaxException extends Exception {
        public SyntaxException(String message) {
            super(message);
        }
    }

    /**
     * Useful during development to ensure unimplemented routines are
     * not accidentally called during development.  Delete it when
     * the Parser is finished.
     */
    @SuppressWarnings("serial")
    public static class UnimplementedFeatureException extends RuntimeException {
        public UnimplementedFeatureException() {
            super();
        }
    }

    Scanner scanner;
    Token t;

    Parser(Scanner scanner) {
        this.scanner = scanner;
        t = scanner.nextToken();
    }

    /**
     * parse the input using tokens from the scanner.
     * Check for EOF (i.e. no trailing junk) when finished
     *
     * @throws SyntaxException
     */
    ASTNode parse() throws SyntaxException {
        Program program = program();
        matchEOF();
        return program;
    }

    Expression expression() throws SyntaxException {
        //TODO
        Expression e0;
        Expression e1;
        Token firstToken = t;
        e0 = term();
        while (t.isKind(LT, LE, GT, GE, EQUAL, NOTEQUAL)) {
            Token op = t;
            consume();
            e1 = term();
            e0 = new BinaryExpression(firstToken, e0, op, e1);
        }
        return e0;
    }

    Expression term() throws SyntaxException {
        //TODO
        Expression e0;
        Expression e1;
        Token firstToken = t;
        e0 = elem();
        while (t.isKind(PLUS, MINUS, OR)) {
            Token op = t;
            consume();
            e1 = elem();
            e0 = new BinaryExpression(firstToken, e0, op, e1);
        }
        return e0;
    }

    Expression elem() throws SyntaxException {
        //TODO
        Expression e0;
        Expression e1;
        Token firstToken = t;
        e0 = factor();
        while (t.isKind(TIMES, DIV, AND, MOD)) {
            Token op = t;
            consume();
            e1 = factor();
            e0 = new BinaryExpression(firstToken, e0, op, e1);
        }
        return e0;
    }

    Expression factor() throws SyntaxException {
        Expression e;
        Token firstToken = t;
        Kind kind = t.kind;
        switch (kind) {
            case IDENT: {
                e = new IdentExpression(t);
                consume();
            }
            break;
            case INT_LIT: {
                e = new IntLitExpression(t);
                consume();
            }
            break;
            case KW_TRUE:
            case KW_FALSE: {
                e = new BooleanLitExpression(t);
                consume();
            }
            break;
            case KW_SCREENWIDTH:
            case KW_SCREENHEIGHT: {
                e = new ConstantExpression(t);
                consume();
            }
            break;
            case LPAREN: {
                consume();
                e = expression();
                match(RPAREN);
            }
            break;
            default:
                //you will want to provide a more useful error message
                throw new SyntaxException("illegal factor");
        }
        return e;
    }

    Block block() throws SyntaxException {
        //TODO
        Token firstToken = t;
        ArrayList<Dec> decs = new ArrayList<>();
        ArrayList<Statement> statements = new ArrayList<>();
        match(LBRACE);
        while (!t.isKind(RBRACE)) {
            if (t.isKind(KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME)) {
                int slotNumber = 0;
                Dec dec = dec();
                decs.add(dec);
            } else if (t.isKind(OP_SLEEP, KW_WHILE, KW_IF, IDENT, INT_LIT, KW_TRUE, KW_FALSE, KW_SCREENHEIGHT, KW_SCREENWIDTH, LPAREN,
                    OP_BLUR, OP_CONVOLVE, OP_GRAY, OP_HEIGHT, OP_WIDTH, KW_SCALE, KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC)) {
                statements.add(statement());
            } else {
                match(KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME, OP_SLEEP, KW_WHILE, KW_IF, IDENT, INT_LIT, KW_TRUE, KW_FALSE, KW_SCREENHEIGHT, KW_SCREENWIDTH, LPAREN,
                        OP_BLUR, OP_CONVOLVE, OP_GRAY, OP_HEIGHT, OP_WIDTH, KW_SCALE, KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC);
            }
        }
        consume();
        return new Block(firstToken, decs, statements);
    }

    Program program() throws SyntaxException {
        //TODO
        Token firstToken = t;
        ArrayList<ParamDec> paramDecs = new ArrayList<>();
        Block block = null;
        Kind kind = t.kind;
        switch (kind) {
            case IDENT: {
                consume();
                kind = t.kind;
                switch (kind) {
                    //block or prarmDec
                    case LBRACE:
                        block = block();
                        break;
                    case KW_URL:
                    case KW_FILE:
                    case KW_INTEGER:
                    case KW_BOOLEAN:
                        int slotNumber = 0;
                        ParamDec paramDec = paramDec();
                        paramDec.setSlotNumber(slotNumber++);
                        paramDecs.add(paramDec);
                        while (t.isKind(COMMA)) { //loop
                            consume();
                            ParamDec paramDec1 = paramDec();
                            paramDec1.setSlotNumber(slotNumber++);
                            paramDecs.add(paramDec1);
                        }
                        block = block();
                        break;
                    default:
                        match(LBRACE, KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN);
                }
            } break;
            default: match(IDENT);
        }
        return new Program(firstToken, paramDecs, block);
    }

    ParamDec paramDec() throws SyntaxException {
        //TODO
        Token firstToken = t;
        match(KW_URL, KW_FILE, KW_INTEGER, KW_BOOLEAN);
        Token ident = t;
        match(IDENT);
        return new ParamDec(firstToken, ident);
    }

    Dec dec() throws SyntaxException {
        //TODO
        Token firstToken = t;
        match(KW_INTEGER, KW_BOOLEAN, KW_IMAGE, KW_FRAME);
        Token ident = t;
        match(IDENT);
        return new Dec(firstToken, ident);
    }

    Statement statement() throws SyntaxException {
        //TODO
        //expression || while || if || chain || assign
        //expression --> term --> elem --> factor
        Token firstToken = t;
        if (t.isKind(OP_SLEEP)) {
            consume();
            Expression expression = expression();
            match(SEMI);
            return new SleepStatement(firstToken, expression);
        } else if (t.isKind(IDENT)) {//IDENT could be chainElem or assign
            Token temToken = scanner.tokens.get(scanner.tokenNum);
            if (temToken.isKind(ARROW, BARARROW)) {
                Chain chain = chain();
                match(SEMI);
                return chain;
            } else if (temToken.isKind(ASSIGN)) {
                AssignmentStatement assignmentStatement = assign();
                match(SEMI);
                return assignmentStatement;
            } else {
                throw new SyntaxException("saw " + temToken.kind + " expected one of the relOp,weakOp,strongOp,ArrowOp or Assign token kind");
            }
        } else if (t.isKind(KW_IF, KW_WHILE)) {
            return ifOrWhile();
        } else if (t.isKind(OP_BLUR, OP_CONVOLVE, OP_GRAY, OP_HEIGHT, OP_WIDTH, KW_SCALE, KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC)) {
            Chain chain = chain();
            match(SEMI);
            return chain;
        } else {
            throw new SyntaxException("saw " + t.kind + " expected one of the OP_SLEEP,while,if,chain,assign");
        }
    }


    AssignmentStatement assign() throws SyntaxException {
        Token firstToken = t;
        match(IDENT);
        match(ASSIGN);
        return new AssignmentStatement(firstToken, new IdentLValue(firstToken), expression());
    }

    Statement ifOrWhile() throws SyntaxException {
        Token firstToken = t;
        consume();
        match(LPAREN);
        Expression expression = expression();
        match(RPAREN);
        Block block = block();
        if (firstToken.isKind(KW_IF)) {
            return new IfStatement(firstToken, expression, block);
        } else {//no option other than KW_WHILE
            return new WhileStatement(firstToken, expression, block);
        }
    }

    Chain chain() throws SyntaxException {
        //TODO
        Token firstToken = t;
        Chain loopChain = chainElem();
        do {
            Token arrowToken = t;
            match(ARROW, BARARROW);
            loopChain = new BinaryChain(firstToken, loopChain, arrowToken, chainElem());
        } while (t.isKind(ARROW, BARARROW));
        return loopChain;
    }

    ChainElem chainElem() throws SyntaxException {
        //TODO
        Token firstToken = t;
        if (t.isKind(IDENT)) {
            match(IDENT);
            return new IdentChain(firstToken);
        } else if (t.isKind(OP_BLUR, OP_CONVOLVE, OP_GRAY)) {//filterOp
            match(OP_BLUR, OP_CONVOLVE, OP_GRAY);
            return new FilterOpChain(firstToken, arg());
        } else if (t.isKind(KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC)) {//frameOp
            match(KW_SHOW, KW_HIDE, KW_MOVE, KW_XLOC, KW_YLOC);
            return new FrameOpChain(firstToken, arg());
        } else if (t.isKind(OP_HEIGHT, OP_WIDTH, KW_SCALE)) {//imageOp
            match(OP_HEIGHT, OP_WIDTH, KW_SCALE);
            return new ImageOpChain(firstToken, arg());
        } else {
            throw new SyntaxException("saw " + t.kind + " expected one of the Ident, filterOp, frameOp, imageOp");
        }
    }

    Tuple arg() throws SyntaxException {
        //TODO
        Token firstToken = t;
        List<Expression> expressions = new ArrayList<>();
        if (t.isKind(LPAREN)) {
            do {
                consume();
                expressions.add(expression());
            } while (t.isKind(COMMA));
            match(RPAREN);
        }
        return new Tuple(firstToken, expressions);
    }

    /**
     * Checks whether the current token is the EOF token. If not, a
     * SyntaxException is thrown.
     *
     * @return
     * @throws SyntaxException
     */
    private Token matchEOF() throws SyntaxException {
        if (t.isKind(EOF)) {
            return t;
        }
        throw new SyntaxException(" expected EOF");
    }

    /**
     * Checks if the current token has the given kind. If so, the current token
     * is consumed and returned. If not, a SyntaxException is thrown.
     * <p>
     * Precondition: kind != EOF
     *
     * @param kind
     * @return
     * @throws SyntaxException
     */
    private Token match(Kind kind) throws SyntaxException {
        if (t.isKind(kind)) {
            return consume();
        }
        throw new SyntaxException("saw " + t.kind + " expected " + kind);
    }

    /**
     * Checks if the current token has one of the given kinds. If so, the
     * current token is consumed and returned. If not, a SyntaxException is
     * thrown.
     * <p>
     * * Precondition: for all given kinds, kind != EOF
     *
     * @param kinds list of kinds, matches any one
     * @return
     * @throws SyntaxException
     */
    private Token match(Kind... kinds) throws SyntaxException {
        // TODO. Optional but handy
        if (kinds.length > 0) {
            for (Kind k : kinds) {
                if (t.isKind(k)) {
                    return consume();
                }
            }
        }
        throw new SyntaxException("saw " + t.kind + " expected " + kinds);
    }

    /**
     * Gets the next token and returns the consumed token.
     * <p>
     * Precondition: t.kind != EOF
     *
     * @return
     */
    private Token consume() throws SyntaxException {
        Token tmp = t;
        t = scanner.nextToken();
        return tmp;
    }

}
