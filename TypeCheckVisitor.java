package cop5556sp17;

import cop5556sp17.AST.*;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.Type;

import java.util.ArrayList;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.LinePos;
import cop5556sp17.Scanner.Token;

import static cop5556sp17.AST.Type.TypeName.*;
import static cop5556sp17.Scanner.Kind.ARROW;
import static cop5556sp17.Scanner.Kind.KW_HIDE;
import static cop5556sp17.Scanner.Kind.KW_MOVE;
import static cop5556sp17.Scanner.Kind.KW_SHOW;
import static cop5556sp17.Scanner.Kind.KW_XLOC;
import static cop5556sp17.Scanner.Kind.KW_YLOC;
import static cop5556sp17.Scanner.Kind.OP_BLUR;
import static cop5556sp17.Scanner.Kind.OP_CONVOLVE;
import static cop5556sp17.Scanner.Kind.OP_GRAY;
import static cop5556sp17.Scanner.Kind.OP_HEIGHT;
import static cop5556sp17.Scanner.Kind.OP_WIDTH;
import static cop5556sp17.Scanner.Kind.*;

/**
 * @author xiaozhe
 * University of Florida
 * School projects
 */
public class TypeCheckVisitor implements ASTVisitor {

    @SuppressWarnings("serial")
    public static class TypeCheckException extends Exception {
        TypeCheckException(String message) {
            super(message);
        }
    }

    SymbolTable symtab = new SymbolTable();

    @Override
    public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
        // TODO Auto-generated method stub
        TypeName chainType = (TypeName) visitChain(binaryChain.getE0(), arg);
        Kind arrowKind = binaryChain.getArrow().kind;
        ChainElem chainElem = binaryChain.getE1();
        TypeName chainElemType = (TypeName) visitChainElem(chainElem, arg);
        Token firstTokenOfCE = chainElem.getFirstToken();
        if (chainType.isType(URL) || chainType.isType(FILE)) {
            if (arrowKind == ARROW && chainElemType == IMAGE) {
                binaryChain.setTypeName(IMAGE);
                binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                return binaryChain.getTypeName();
            } else {
                throw new TypeCheckException("expect ARROR + IMAGE but saw " + arrowKind.getText() + " + " + chainElemType.toString());
            }
        } else if (chainType.isType(FRAME)) {
            if (arrowKind == ARROW && (chainElem instanceof FrameOpChain)) {
                if (firstTokenOfCE.isKind(KW_XLOC, KW_YLOC)) {
                    binaryChain.setTypeName(INTEGER);
                    binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                    return binaryChain.getTypeName();
                } else if (firstTokenOfCE.isKind(KW_SHOW, KW_HIDE, KW_MOVE)) {
                    binaryChain.setTypeName(FRAME);
                    binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                    return binaryChain.getTypeName();
                } else {
                    throw new TypeCheckException("The first token of chainElem should be KW_XLOC/KW_YLOC/KW_SHOW/KW_HIDE/KW_MOVE" + firstTokenOfCE.kind.toString());
                }
            } else {
                if (arrowKind != ARROW) {
                    throw new TypeCheckException("expect ARROR but saw " + arrowKind.getText());
                } else {
                    throw new TypeCheckException("ChainElem should be FrameOpChain");
                }
            }
        } else if (chainType.isType(IMAGE)) {
            if (arrowKind == ARROW) {
                if (chainElem instanceof ImageOpChain) {
                    if (firstTokenOfCE.isKind(OP_WIDTH, OP_HEIGHT)) {
                        binaryChain.setTypeName(INTEGER);
                        binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                        return binaryChain.getTypeName();
                    } else {
                        throw new TypeCheckException("The first token of chainElem should be OP_WIDTH, OP_HEIGHT" + firstTokenOfCE.kind.toString());
                    }
                } else if (chainElemType == FRAME) {
                    binaryChain.setTypeName(FRAME);
                    binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                    return binaryChain.getTypeName();
                } else if (chainElemType == FILE) {
                    binaryChain.setTypeName(NONE);
                    binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                    return binaryChain.getTypeName();
                } else if (chainElem instanceof FilterOpChain) {
                    if (firstTokenOfCE.isKind(OP_GRAY, OP_BLUR, OP_CONVOLVE)) {
                        binaryChain.setTypeName(IMAGE);
                        binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                        return binaryChain.getTypeName();
                    } else {
                        throw new TypeCheckException("The first token of chainElem should be OP_GRAY/OP_BLUR/OP_CONVOLVE" + firstTokenOfCE.kind.toString());
                    }
                } else if (chainElem instanceof ImageOpChain) {
                    if (firstTokenOfCE.isKind(KW_SCALE)) {
                        binaryChain.setTypeName(IMAGE);
                        binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                        return binaryChain.getTypeName();
                    } else {
                        throw new TypeCheckException("The first token of chainElem should be KW_SCALE" + firstTokenOfCE.kind.toString());
                    }
                } else if (chainElem instanceof IdentChain) {
                    binaryChain.setTypeName(IMAGE);
                    binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                    return binaryChain.getTypeName();
                } else {
                    throw new TypeCheckException("Check legal combinations of Binary Chain. Chain = IMAGE, OP = ARROR\nfirstToken " + firstTokenOfCE
                            + "\nchainElem " + chainElem.getClass());
                }
            } else if (arrowKind == BARARROW) {
                if (chainElem instanceof FilterOpChain) {
                    if (firstTokenOfCE.isKind(OP_GRAY, OP_BLUR, OP_CONVOLVE)) {
                        binaryChain.setTypeName(IMAGE);
                        binaryChain.setDec(symtab.lookup(binaryChain.getFirstToken().getText()));
                        return binaryChain.getTypeName();
                    } else {
                        throw new TypeCheckException("The first token of chainElem should be OP_GRAY/OP_BLUR/OP_CONVOLVE" + firstTokenOfCE.kind.toString());
                    }
                } else {
                    throw new TypeCheckException("Expecting chainElem is FilterOpChain but it is " + chainElem.getClass());
                }
            } else {
                throw new TypeCheckException("Expecting operator ARROR/BARRROR");
            }
        } else {
            throw new TypeCheckException("Chain type should be URL/FILE/FRAME/IMAGE but saw " + chainType.toString());
        }
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
        // TODO Auto-generated method stub
        TypeName e0Type = (TypeName) visitExpression(binaryExpression.getE0(), arg);
        TypeName e1Type = (TypeName) visitExpression(binaryExpression.getE1(), arg);
        Token op = binaryExpression.getOp();
        if (op.isKind(PLUS, MINUS)) {
            if (e0Type.isType(INTEGER) && e1Type.isType(INTEGER)) {
                binaryExpression.setTypeName(INTEGER);
                return binaryExpression.getTypeName();
            } else if (e0Type.isType(IMAGE) && e1Type.isType(IMAGE)) {
                binaryExpression.setTypeName(IMAGE);
                return binaryExpression.getTypeName();
            } else {
                throw new TypeCheckException("The type of both E0 and E1 should be INTEGER/IMAGE and same as each other but saw " + e0Type.toString() + e1Type.toString());
            }
        } else if (op.isKind(TIMES)) {
            if (e0Type.isType(INTEGER) && e1Type.isType(INTEGER)) {
                binaryExpression.setTypeName(INTEGER);
                return binaryExpression.getTypeName();
            } else if (e0Type.isType(INTEGER) && e1Type.isType(IMAGE)) {
                binaryExpression.setTypeName(IMAGE);
                return binaryExpression.getTypeName();
            } else if (e0Type.isType(IMAGE) && e1Type.isType(INTEGER)) {
                binaryExpression.setTypeName(IMAGE);
                return binaryExpression.getTypeName();
            } else {
                throw new TypeCheckException("The type of both E0 and E1 should be INTEGER/IMAGE but saw " + e0Type.toString() + e1Type.toString());
            }
        } else if (op.isKind(DIV)) {
            if (e0Type.isType(INTEGER) && e1Type.isType(INTEGER)) {
                binaryExpression.setTypeName(INTEGER);
                return binaryExpression.getTypeName();
            } else {
                throw new TypeCheckException("The type of both E0 and E1 should be INTEGER but saw " + e0Type.toString() + e1Type.toString());
            }
        } else if (op.isKind(LT, LE, GT, GE)) {
            if (e0Type.isType(INTEGER) && e1Type.isType(INTEGER)) {
                binaryExpression.setTypeName(BOOLEAN);
                return binaryExpression.getTypeName();
            } else if (e0Type.isType(BOOLEAN) && e1Type.isType(BOOLEAN)) {
                binaryExpression.setTypeName(BOOLEAN);
                return binaryExpression.getTypeName();
            } else {
                throw new TypeCheckException("The type of both E0 and E1 should be INTEGER/BOOLEAN and same as each other but saw " + e0Type.toString() + e1Type.toString());
            }
        } else if (op.isKind(EQUAL, NOTEQUAL)) {
            if (e0Type.isType(e1Type)) {
                binaryExpression.setTypeName(BOOLEAN);
                return binaryExpression.getTypeName();
            } else {
                throw new TypeCheckException("The type of both E0 and E1 should be as same as each other but saw " + e0Type.toString() + e1Type.toString());
            }
        } else {
            throw new TypeCheckException("Operator should be one of PLUS/MINUS/TIMES/DIV/LT/LE/GT/GE/EQUAL/NOTEQUAL. But saw " + op.kind.getText());
        }
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws Exception {
        // TODO Auto-generated method stub
        symtab.enterScope();
        for (Dec dec : block.getDecs()) {
            visitDec(dec, arg);
        }
        for (Statement statement : block.getStatements()) {
            visitStatement(statement, arg);
        }
        symtab.leaveScope();
        return null;
    }

    public Object visitStatement(Statement statement, Object arg) throws Exception {
        //TODO
        if (statement instanceof SleepStatement) {
            return visitSleepStatement((SleepStatement) statement, arg);
        } else if (statement instanceof WhileStatement) {
            return visitWhileStatement((WhileStatement) statement, arg);
        } else if (statement instanceof IfStatement) {
            return visitIfStatement((IfStatement) statement, arg);
        } else if (statement instanceof AssignmentStatement) {
            return visitAssignmentStatement((AssignmentStatement) statement, arg);
        } else if (statement instanceof Chain) {
            return visitChain((Chain) statement, arg);
        } else {
            throw new TypeCheckException("expecting sleep/while/if/chain/assignment statement");
        }
    }

    public Object visitChain(Chain chain, Object arg) throws Exception {
        //TODO
        if (chain instanceof ChainElem) {
            return visitChainElem((ChainElem) chain, arg);
        } else if (chain instanceof BinaryChain) {
            return visitBinaryChain((BinaryChain) chain, arg);
        } else {
            throw new TypeCheckException("expecting ChainElem or BinaryChain");
        }
    }

    public Object visitChainElem(ChainElem chain, Object arg) throws Exception {
        //TODO
        if (chain instanceof IdentChain) {
            return visitIdentChain((IdentChain) chain, arg);
        } else if (chain instanceof FilterOpChain) {
            return visitFilterOpChain((FilterOpChain) chain, arg);
        } else if (chain instanceof FrameOpChain) {
            return visitFrameOpChain((FrameOpChain) chain, arg);
        } else if (chain instanceof ImageOpChain) {
            return visitImageOpChain((ImageOpChain) chain, arg);
        } else {
            throw new TypeCheckException("expecting Ident/FilterOp/FrameOp/ImageOp chain");
        }
    }

    @Override
    public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
        // TODO Auto-generated method stub
        booleanLitExpression.setTypeName(BOOLEAN);
        return booleanLitExpression.getTypeName();
    }

    @Override
    public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
        // TODO Auto-generated method stub
        visitTuple(filterOpChain.getArg(), arg);
        if (filterOpChain.getArg().getExprList().size() == 0) {
            filterOpChain.setTypeName(IMAGE);
            return filterOpChain.getTypeName();
        } else {
            throw new TypeCheckException("Tuple size should be 0 but it is " + filterOpChain.getArg().getExprList().size());
        }
    }

    @Override
    public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
        // TODO Auto-generated method stub
        visitTuple(frameOpChain.getArg(), arg);
        if (frameOpChain.getFirstToken().isKind(KW_SHOW, KW_HIDE)) {
            if (frameOpChain.getArg().getExprList().size() == 0) {
                frameOpChain.setTypeName(NONE);
                return frameOpChain.getTypeName();
            } else {
                throw new TypeCheckException("Tuple size should be 0 but it is " + frameOpChain.getArg().getExprList().size());
            }
        } else if (frameOpChain.getFirstToken().isKind(KW_XLOC, KW_YLOC)) {
            if (frameOpChain.getArg().getExprList().size() == 0) {
                frameOpChain.setTypeName(INTEGER);
                return frameOpChain.getTypeName();
            } else {
                throw new TypeCheckException("Tuple size should be 0 but it is " + frameOpChain.getArg().getExprList().size());
            }
        } else if (frameOpChain.getFirstToken().isKind(KW_MOVE)) {
            if (frameOpChain.getArg().getExprList().size() == 2) {
                frameOpChain.setTypeName(NONE);
                return frameOpChain.getTypeName();
            } else {
                throw new TypeCheckException("Tuple size should be 2 but it is " + frameOpChain.getArg().getExprList().size());
            }
        } else {
            throw new TypeCheckException("Expecting the kind of token is KW_SHOW/KW_HIDE/KW_XLOC/KW_YLOC/KW_MOVE" + frameOpChain.getFirstToken().kind);
        }
    }

    @Override
    public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
        // TODO Auto-generated method stub
        Dec firstTokenDec = symtab.lookup(identChain.getFirstToken().getText());
        if (!identChain.getFirstToken().getText().isEmpty() && firstTokenDec != null) {
            identChain.setTypeName(Type.getTypeName(firstTokenDec.getType()));
            return identChain.getTypeName();
        } else {
            throw new TypeCheckException("This identChain: " + identChain.getFirstToken().getText() + " does not exist in the current scope");
        }
    }

    @Override
    public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
        // TODO Auto-generated method stub
        if (identExpression.getFirstToken() != null && symtab.lookup(identExpression.getFirstToken().getText()) != null) {
            identExpression.setTypeName(Type.getTypeName(symtab.lookup(identExpression.getFirstToken().getText()).getFirstToken()));
            identExpression.setDec(symtab.lookup(identExpression.getFirstToken().getText()));
            return identExpression.getTypeName();
        } else {
            throw new TypeCheckException("This ident " + identExpression.getFirstToken().getText() + "is null or does not exist on the current scope");
        }
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
        // TODO Auto-generated method stub
        TypeName expressionType = (TypeName) visitExpression(ifStatement.getE(), arg);
        if (expressionType.isType(BOOLEAN)) {
            visitBlock(ifStatement.getB(), arg);
        } else {
            throw new TypeCheckException("The expression type should be BOOLEAN but saw " + expressionType.toString());
        }
        return null;
    }

    @Override
    public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
        // TODO Auto-generated method stub
        intLitExpression.setTypeName(INTEGER);
        return intLitExpression.getTypeName();
    }

    @Override
    public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
        // TODO Auto-generated method stub
        TypeName typeName = (TypeName) visitExpression(sleepStatement.getE(), arg);
        if (!INTEGER.isType(typeName)) {
            throw new Exception("expect INTEGER saw: " + typeName.toString());
        }
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
        // TODO Auto-generated method stub
        TypeName expressionType = (TypeName) visitExpression(whileStatement.getE(), arg);
        if (expressionType.isType(BOOLEAN)) {
            visitBlock(whileStatement.getB(), arg);
        } else {
            throw new TypeCheckException("The expression type should be BOOLEAN but saw " + expressionType.toString());
        }
        return null;
    }

    @Override
    public Object visitDec(Dec declaration, Object arg) throws Exception {
        // TODO Auto-generated method stub
        symtab.insert(declaration.getIdent().getText(), declaration);
        return null;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        // TODO Auto-generated method stub
        for (ParamDec paramDec : program.getParams()) {
            visitParamDec(paramDec, arg);
        }
        visitBlock(program.getB(), arg);
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
        // TODO Auto-generated method stub
        TypeName identLType = (TypeName) visitIdentLValue(assignStatement.getVar(), arg);
        TypeName expressionType = (TypeName) visitExpression(assignStatement.getE(), arg);
        if (identLType != expressionType) {
            throw new TypeCheckException("IdentL typeName is " + identLType.toString() + ", is not as same as expression typeName which is " + expressionType.toString());
        }
        return null;
    }

    public Object visitExpression(Expression e, Object arg) throws Exception {
        //TODO
        if (e instanceof IdentExpression) {
            return visitIdentExpression((IdentExpression) e, arg);
        } else if (e instanceof IntLitExpression) {
            return visitIntLitExpression((IntLitExpression) e, arg);
        } else if (e instanceof BooleanLitExpression) {
            return visitBooleanLitExpression((BooleanLitExpression) e, arg);
        } else if (e instanceof ConstantExpression) {
            return visitConstantExpression((ConstantExpression) e, arg);
        } else if (e instanceof BinaryExpression) {
            return visitBinaryExpression((BinaryExpression) e, arg);
        } else {
            throw new TypeCheckException("Expecting Ident/IntLit/BooleanLit/Constant/Binary Expression");
        }
    }

    @Override
    public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
        // TODO Auto-generated method stub
        if (identX.getText() != null && symtab.lookup(identX.getText()) != null) {
            identX.setTypeName(Type.getTypeName(symtab.lookup(identX.getText()).getFirstToken()));
            return identX.getTypeName();
        } else {
            throw new TypeCheckException("This ident " + identX.getText() + "is null or does not exist on the current scope");
        }
    }

    @Override
    public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
        // TODO Auto-generated method stub
        symtab.insert(paramDec.getIdent().getText(), paramDec);
        return null;
    }

    @Override
    public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
        // TODO Auto-generated method stub
        constantExpression.setTypeName(INTEGER);
        return constantExpression.getTypeName();
    }

    @Override
    public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
        // TODO Auto-generated method stub
        visitTuple(imageOpChain.getArg(), arg);
        int tupleSize = imageOpChain.getArg().getExprList().size();
        if (imageOpChain.getFirstToken().isKind(OP_WIDTH, OP_HEIGHT)) {
            if (tupleSize == 0) {
                imageOpChain.setTypeName(INTEGER);
                return imageOpChain.getTypeName();
            } else {
                throw new TypeCheckException("Tuple size should be 0 but it is " + tupleSize);
            }
        } else if (imageOpChain.getFirstToken().isKind(KW_SCALE)) {
            if (tupleSize == 1) {
                imageOpChain.setTypeName(IMAGE);
                return imageOpChain.getTypeName();
            } else {
                throw new TypeCheckException("Tuple size should be 1 but it is " + tupleSize);
            }
        } else {
            throw new TypeCheckException("Expecting kind of token is OP_WIDTH/OP_HEIGHT/KW_SCALE but saw " + imageOpChain.getFirstToken().kind);
        }
    }

    @Override
    public Object visitTuple(Tuple tuple, Object arg) throws Exception {
        // TODO Auto-generated method stub
        for (Expression expression : tuple.getExprList()) {
            TypeName typeName = (TypeName) visitExpression(expression, arg);
            if (!typeName.isType(INTEGER)) {
                throw new TypeCheckException("The expression type should be INTEGER but saw " + typeName.toString());
            }
        }
        return null;
    }


}
