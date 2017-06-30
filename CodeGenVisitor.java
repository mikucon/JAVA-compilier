package cop5556sp17;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import cop5556sp17.AST.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.*;

import cop5556sp17.AST.Type.*;

import static cop5556sp17.AST.Type.*;
import static cop5556sp17.Scanner.*;
import static cop5556sp17.Scanner.Kind.KW_SCALE;
import static cop5556sp17.Scanner.Kind.OP_HEIGHT;
import static cop5556sp17.Scanner.Kind.OP_WIDTH;

/**
 * @author xiaozhe
 * University of Florida
 * School projects
 */
public class CodeGenVisitor implements ASTVisitor, Opcodes {

    /**
     * @param DEVEL          used as parameter to genPrint and genPrintTOS
     * @param GRADE          used as parameter to genPrint and genPrintTOS
     * @param sourceFileName name of source file, may be null.
     */
    public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
        super();
        this.DEVEL = DEVEL;
        this.GRADE = GRADE;
        this.sourceFileName = sourceFileName;
    }

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;
	SymbolTable decSymtab = new SymbolTable();
	SymbolTable paramSymtab = new SymbolTable();
	
	MethodVisitor mv; // visitor of method currently under construction
	private static Stack<Integer> slot_stack = new Stack<>();
	//false means the frame is already initial, true means the frame is not initial
	private static HashMap<Dec,Boolean> frameMap = new HashMap<>();

    /**
     * Indicates whether genPrint and genPrintTOS should generate code.
     */
    final boolean DEVEL;
    final boolean GRADE;

    @Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		//put the field into class file
		int paramDecSlotNum = 0;
		for(ParamDec param:program.getParams()){
			param.setSlotNum(paramDecSlotNum);
			paramSymtab.insert(param.getIdent().text, param);
			cw.visitField(ACC_PUBLIC, param.getIdent().text, param.getTypeName().getJVMTypeDesc(), null, null);
			paramDecSlotNum++;
		}

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		for (ParamDec dec : params)
			dec.visit(this, mv);
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		//slot stack add first element
		slot_stack.add(0);

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		Label endRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		mv.visitLabel(endRun);
		//TODO  visit the local variables
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd(); // end of run method


		cw.visitEnd();//end of class

		//generate classfile and return it
		return cw.toByteArray();
	}




    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
        assignStatement.getE().visit(this, arg);
        CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
        CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getTypeName());
        if(assignStatement.getE().getTypeName==Type.TypeName.IMAGE)
        	mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps","copyImage","(Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;", false);
        assignStatement.getVar().visit(this.arg, arg);
        return null;
    }

    @Override
    public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
    	String flag = (String)arg;
		binaryChain.getE0().visit(this,"left");
		if(binaryChain.getE0().getTypeName()==Type.TypeName.URL){
				mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageIO", "readFromURL", "(Ljava/net/URL;)Ljava/awt/image/BufferedImage;", false);
		}
		else if(binaryChain.getE0().getTypeName()==Type.TypeName.FILE){
				mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageIO", "readFromFile", "(Ljava/io/File;)Ljava/awt/image/BufferedImage;", false);
		}
		if(flag!=null){
			if(flag.equals("left"))
				binaryChain.getE1().visit(this,"leftright");
		}else{
			binaryChain.getE1().visit(this, "right");
		}
        return null;
    }

    @Override
    public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
        //TODO  Implement this
        binaryExpression.getE0().visit(this, arg);
        binaryExpression.getE1().visit(this, arg);
        if(binaryExpression.getE0().getTypeName()==Type.TypeName.INTEGER&&binaryExpression.getE1().getTypeName()==Type.TypeName.INTEGER){
	        Label label1 = new Label();
	        Label label2 = new Label();
	        Token op = binaryExpression.getOp();
	        switch (op.kind) {
            	case PLUS:
	                mv.visitInsn(IADD);
	                break;
	            case MINUS:
	                mv.visitInsn(ISUB);
	                break;
	            case TIMES:
	                mv.visitInsn(IMUL);
	                break;
	            case DIV:
	                mv.visitInsn(IDIV);
	                break;
	            case MOD:
	                mv.visitInsn(IREM);
	                break;
	            case OR:
	                mv.visitInsn(IREM);
	                break;
	            case AND:
	                mv.visitInsn(IREM);
	                break;
	            case LT:
	                compareProcess(label1, label2, IF_ICMPGE);
	                break;
	            case LE:
	                compareProcess(label1, label2, IF_ICMPGT);
	                break;
	            case GT:
	                compareProcess(label1, label2, IF_ICMPLE);
	                break;
	            case GE:
	                compareProcess(label1, label2, IF_ICMPLT);
	                break;
	            case EQUAL:
	                compareProcess(label1, label2, IF_ICMPNE);
	                break;
	            case NOTEQUAL:
	                mv.visitJumpInsn(IF_ICMPNE, label1);
	                mv.visitInsn(ICONST_0);
	                mv.visitJumpInsn(GOTO, label2);
	                mv.visitLabel(label1);
	                mv.visitInsn(ICONST_1);
	                mv.visitLabel(label2);
	                break;
	            default:
	                throw new RuntimeException("not yet implemented");
	        	}
	        
	        }
        else if(binaryExpression.getE0().getTypeName()==Type.TypeName.IMAGE||binaryExpression.getE1().getTypeName()==Type.TypeName.IMAGE){
        	 Label label1 = new Label();
        	 Label label2 = new Label();
        	 Token op = binaryExpression.getOp();
        	 switch (op.kind) {
		        case PLUS:
		        	mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "add", "(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;", false);
		            break;
		        case MINUS:
		        	mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "sub", "(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;", false);
		            break;
		        case TIMES:
		        	mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "mul", "(Ljava/awt/image/BufferedImage;I)Ljava/awt/image/BufferedImage;", false);
		            break;
		        case DIV:
		        	mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "div", "(Ljava/awt/image/BufferedImage;I)Ljava/awt/image/BufferedImage;", false);
		            break;
		        case MOD:
		        	mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "mod", "(Ljava/awt/image/BufferedImage;I)Ljava/awt/image/BufferedImage;", false);
		            break;
		        case EQUAL:
		        	Label label1 = new Label();
					Label label2 = new Label();
					mv.visitJumpInsn(IF_ACMPNE, label1);
					mv.visitInsn(ICONST_1);
					mv.visitJumpInsn(GOTO, label2);
					mv.visitLabel(label1);
					mv.visitInsn(ICONST_0);
					mv.visitLabel(label2);
					break;
		        case NOTEQUAL:
		        	Label label1 = new Label();
					Label label2 = new Label();
					mv.visitJumpInsn(IF_ACMPNE, label1);
					mv.visitInsn(ICONST_0);
					mv.visitJumpInsn(GOTO, label2);
					mv.visitLabel(label1);
					mv.visitInsn(ICONST_1);
					mv.visitLabel(label2);
					break;
        	 }
        }
        return null;
    }

    private void compareProcess(Label label1, Label label2, int opcodes) {
        mv.visitJumpInsn(opcodes, label1);
        mv.visitInsn(ICONST_1);
        mv.visitJumpInsn(GOTO, label2);
        mv.visitLabel(label1);
        mv.visitInsn(ICONST_0);
        mv.visitLabel(label2);
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws Exception {
        //TODO  Implement this
        Label startBlock = new Label();
        Label endBlock = new Label();
        mv.visitLabel(startBlock);
        decSymtab.enterScope();
        for (Dec dec : block.getDecs()) {
        	if(dec.getTypeName()==Type.TypeName.FRAME){
				frameMap.put(dec, false);
			}
            dec.visit(this, arg);
        }
        for (Statement statement : block.getStatements()) {
            statement.visit(this, arg);
            if (statement instanceof BinaryChain) {
                slotStack.push(slotStack.peek() + 1);
                decSymtab.insert(statement.getDec().getIdent().getText(), statement.getDec());
            }
        }
        mv.visitLabel(endBlock);
        decSymtab.leaveScope();
        for (Dec dec : block.getDecs()) {
            dec.visit(this, arg);
            mv.visitLocalVariable(dec.getIdent().getText(), dec.getTypeName().getJVMTypeDesc(), null, startBlock, endBlock, dec.getSlotNumber());
        }
        for (int i = 0; i < block.getDecs().size(); i++) {
            slotStack.pop();
        }
        return null;
    }

    @Override
    public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
        //TODO Implement this
        if (booleanLitExpression.getValue()) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }
        return null;
    }

    @Override
    public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
    	if(constantExpression.getFirstToken().isKind(KW_SCREENHEIGHT)){
			mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFrame", "getScreenHeight", "()I", false);
		}else if(constantExpression.getFirstToken().isKind(KW_SCREENWIDTH)){
			mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFrame", "getScreenWidth", "()I", false);
		}
        return null;
    }

    @Override
    public Object visitDec(Dec declaration, Object arg) throws Exception {
        //TODO Implement this
        declaration.setSlotNumber(slotStack.peek() + 1);
        slotStack.push(slotStack.peek() + 1);
        decSymtab.insert(declaration.getIdent().getText(), declaration);

        if (declaration.getTypeName().isType(TypeName.FRAME)) {
            mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFrame", "showImage", "(Lcop5556sp17/PLPRuntimeFrame;)", true);
        } else if (declaration.getTypeName().isType(TypeName.IMAGE)) {
            mv.visitMethodInsn(INVOKESTATIC, "java/awt/image/BufferedImage", "getData", "Ljava/awt/image/BufferedImage;", true);
        }
        return null;
    }

    @Override
    public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
    	String flag = (String)arg;
		filterOpChain.getArg().visit(this, arg);
		mv.visitInsn(ACONST_NULL);
    	Token op = filterOpChain.getFirstToken();
    	
        switch (op.kind) {
        	case OP_BLUR:
        		mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFilterOps", "blurOp", "(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;", false);
        		break;
        	case OP_GRAY:
        		mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFilterOps", "grayOp", "(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;", false);
        		break;
        	case OP_CONVOLVE:
        		mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFilterOps", "convolveOp", "(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;", false);
        		break;   
        }
        
        if(flag.equals("right")){
			mv.visitInsn(POP);
		}
        return null;
    }

    @Override
    public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
    	String flag = (String)arg;
    	Token op = frameOpChain.getFirstToken();
    	switch (op.kind){
    		case KW_SHOW:
    			frameOpChain.getArg().visit(this, arg);
    			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "showImage", "()Lcop5556sp17/PLPRuntimeFrame;", false);
    			break;
    		case KW_HIDE:
    			frameOpChain.getArg().visit(this, arg);
    			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "hideImage", "()Lcop5556sp17/PLPRuntimeFrame;", false);
    			break;
    		case KW_MOVE:
    			frameOpChain.getArg().visit(this, arg);
    			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "moveFrame", "(II)Lcop5556sp17/PLPRuntimeFrame;", false);
    			break;
    		case KW_XLOC:
    			frameOpChain.getArg().visit(this, arg);
    			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "getXVal", "()I", false);
    			break;
    		case KW_YLOC:
    			frameOpChain.getArg().visit(this, arg);
    			mv.visitMethodInsn(INVOKEVIRTUAL, "cop5556sp17/PLPRuntimeFrame", "getYVal", "()I", false);
    			break;
    		}
    	if(flag.equals("right")){
			mv.visitInsn(POP);	
    	}
        return null;
    }

    @Override
    public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		String position = (String)arg;
		if(position.equals("left")){
			if(decSymtab.lookup(identChain.getFirstToken().text)!=null){
				if(identChain.getTypeName()==Type.TypeName.INTEGER||decSymtab.lookup(identChain.getFirstToken().text).getTypeName()==Type.TypeName.BOOLEAN){
					mv.visitVarInsn(ILOAD, decSymtab.lookup(identChain.getFirstToken().text).getSlotNum());
				}else{
					mv.visitVarInsn(ALOAD, decSymtab.lookup(identChain.getFirstToken().text).getSlotNum());
				}
			}else if(paramSymtab.lookup(identChain.getFirstToken().text)!=null){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, identChain.getFirstToken().text, identChain.getTypeName().getJVMTypeDesc());
			}else{
				throw new Exception("can't find ident");
			}


		}else if(position.equals("right")||position.equals("leftright")){
			if(identChain.getTypeName()==Type.TypeName.INTEGER||identChain.getTypeName()==Type.TypeName.IMAGE){
				
				if(position.equals("leftright")){
					mv.visitInsn(DUP);
				}
				if(decSymtab.lookup(identChain.getFirstToken().text)!=null){
					if(identChain.getTypeName()==Type.TypeName.INTEGER){
						mv.visitVarInsn(ISTORE, decSymtab.lookup(identChain.getFirstToken().text).getSlotNum());
					}else{
						mv.visitVarInsn(ASTORE, decSymtab.lookup(identChain.getFirstToken().text).getSlotNum());
					}
				}else if(paramSymtab.lookup(identChain.getFirstToken().text)!=null){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, identChain.getFirstToken().text, identChain.getTypeName().getJVMTypeDesc());
				}else{
					throw new Exception("can't find ident");
				}
			}else if(identChain.getTypeName()==Type.TypeName.FILE){
				
				if(paramSymtab.lookup(identChain.getFirstToken().text)!=null){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, identChain.getFirstToken().text, identChain.getTypeName().getJVMTypeDesc());
				}else{
					throw new Exception("can't find ident");
				}
				mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageIO", "write", "(Ljava/awt/image/BufferedImage;Ljava/io/File;)Ljava/awt/image/BufferedImage;", false);
				mv.visitInsn(POP);
			}else if(identChain.getTypeName()==Type.TypeName.FRAME){
				
				if(decSymtab.lookup(identChain.getFirstToken().text)!=null){
					
					if(frameMap.get(decSymtab.lookup(identChain.getFirstToken().text))){
						mv.visitVarInsn(ALOAD, decSymtab.lookup(identChain.getFirstToken().text).getSlotNum());
					}else{
						mv.visitInsn(ACONST_NULL);
						frameMap.put(decSymtab.lookup(identChain.getFirstToken().text),true);
					}
				}else{
					throw new Exception("can't find ident");
				}
	
				mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeFrame", "createOrSetFrame", "(Ljava/awt/image/BufferedImage;Lcop5556sp17/PLPRuntimeFrame;)Lcop5556sp17/PLPRuntimeFrame;", false);
				
				if(position.equals("leftright")){
					mv.visitInsn(DUP);
				}
				mv.visitVarInsn(ASTORE,decSymtab.lookup(identChain.getFirstToken().text).getSlotNum());
			}
		}
		return null;
	}


    @Override
    public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
        //TODO Implement this
        if (decSymtab.lookup(identExpression.getFirstToken().getText()) != null) {
            mv.visitVarInsn(ILOAD, identExpression.getDec().getSlotNumber());
        } else if (paramSymtab.lookup(identExpression.getFirstToken().getText()) != null) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, identExpression.getDec().getIdent().getText(), identExpression.getTypeName().getJVMTypeDesc());
        } else {
            throw new Exception("can't find ident");
        }
        return null;
    }

    @Override
    public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
        //TODO Implement this
        if(decSymtab.lookup(identX.getText())!=null){
        	if(decSymtab.lookup(identX.getText()).getTypeName().isType(Type.TypeName.INTEGER)||dec_symtab.lookup(identX.getText()).getTypeName().isType(Type.TypeName.BOOLEAN)){
            mv.visitVarInsn(ISTORE, identX.getDec().getSlotNumber());
        		}
        		else{
        			mv.visitVarInsn(ASTORE, identX.getDec().getSlotNum());
        		}
        }
        else if(paramSymtab.lookup(identX.getText())!=null){
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(SWAP);
            mv.visitFieldInsn(PUTFIELD, className, identX.getText(), identX.getTypeName().getJVMTypeDesc());
        }
        else{
            throw new Exception("can't find ident");
        }
        return null;

    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
        //TODO Implement this
        ifStatement.getE().visit(this, arg);
        Label AFTER = new Label();
        mv.visitJumpInsn(IFNE, AFTER);
        ifStatement.getB().visit(this, arg);
        mv.visitLabel(AFTER);
        return null;
    }

    @Override
    public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
    	String flag = (String)arg;
		imageOpChain.getArg().visit(this, arg);
		if(imageOpChain.getFirstToken().isKind(OP_WIDTH)){
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage", "getWidth", "()I", false);
		}
		else if(imageOpChain.getFirstToken().isKind(OP_HEIGHT)){
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage", "getHeight", "()I", false);
		}
		else if(imageOpChain.getFirstToken().isKind(KW_SCALE)){
			mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageOps", "scale", "(Ljava/awt/image/BufferedImage;I)Ljava/awt/image/BufferedImage;", false);
		}
		if(flag.equals("right")){
			mv.visitInsn(POP);
		}
        return null;
    }

    @Override
    public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
        //TODO Implement this
        mv.visitLdcInsn(Integer.parseInt(intLitExpression.getFirstToken().getText()));
        return null;
    }


    @Override
    public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
        //TODO Implement this
        //For assignment 5, only needs to handle integers and booleans
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);

		if(paramDec.getTypeName()==Type.TypeName.FILE){
			mv.visitTypeInsn(NEW, "java/io/File");
			mv.visitInsn(DUP);
		}
		
        mv.visitVarInsn(BIPUSH, paramDec.getSlotNumber());
        mv.visitInsn(ALOAD);
        
        if (paramDec.getTypeName().isType(TypeName.INTEGER)) {
        	mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
        } 
        
        else if(paramDec.getTypeName()==Type.TypeName.BOOLEAN) {
        	mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
        }
		
        else if(paramDec.getTypeName()==Type.TypeName.URL){
			mv.visitMethodInsn(INVOKESTATIC, "cop5556sp17/PLPRuntimeImageIO", "getURL", "([Ljava/lang/String;I)Ljava/net/URL;", false);
		}
        else if(paramDec.getTypeName()==Type.TypeName.FILE){
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
		}
        mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().text, paramDec.getTypeName().getJVMTypeDesc());
        return null;

    }

    @Override
    public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
    	sleepStatement.getE().visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
        mv.visitInsn(I2L);
        return null;
    }

    @Override
    public Object visitTuple(Tuple tuple, Object arg) throws Exception {
    	for(Expression expression:tuple.getExprList()){
			expression.visit(this, arg);
		}
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
        //TODO Implement this
        Label GUARD = new Label();
        Label BODY = new Label();
        mv.visitJumpInsn(GOTO, GUARD);
        mv.visitLabel(BODY);
        whileStatement.getB().visit(this, arg);
        mv.visitLabel(GUARD);
        whileStatement.getE().visit(this, arg);
        mv.visitJumpInsn(IFNE, BODY);
        return null;
    }

}
