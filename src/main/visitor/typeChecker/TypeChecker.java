package main.visitor.typeChecker;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.*;
import main.ast.types.NoType;
import main.ast.types.NullType;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.single.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.FieldSymbolTableItem;
import main.symbolTable.utils.graph.Graph;

import main.visitor.Visitor;
import main.ast.types.Type;
import main.ast.types.list.ListType;
import main.ast.types.list.ListNameType;

import main.compileErrorException.typeErrors.*;


import java.util.ArrayList;

public class TypeChecker extends Visitor<Void> {
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;

    private MethodDeclaration currMethodDeclaration;

    private boolean isInLoop;

    public TypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
        this.isInLoop = false;
    }

    boolean firstIsSubTypeOfSecond(Type first, Type second){
        if (first instanceof NoType) {
            return true;
        }

        if (second instanceof NoType){
            return false;
        }

        if (first instanceof IntType){
            if (second instanceof IntType)
                return true;
        }

        if (first instanceof BoolType){
            if (second instanceof BoolType)
                return true;
        }

        if (first instanceof StringType){
            if (second instanceof StringType)
                return true;
        }

        if (first instanceof ClassType){
            if (second instanceof ClassType){
                return classHierarchy.isSecondNodeAncestorOf(first.toString(), second.toString()) ||
                        ((ClassType) first).getClassName().getName().equals(((ClassType) second).getClassName().getName());
            }
        }

        if (first instanceof FptrType){
            if (second instanceof FptrType){
                if (((FptrType) first).getArgumentsTypes().size() == ((FptrType) second).getArgumentsTypes().size() &&
                        firstIsSubTypeOfSecond(((FptrType) first).getReturnType(), ((FptrType) second).getReturnType())){
                    for (int i = 0; i < ((FptrType) first).getArgumentsTypes().size(); i++){
                        if (!firstIsSubTypeOfSecond(((FptrType) first).getArgumentsTypes().get(i), ((FptrType) second).getArgumentsTypes().get(i))){
                            return false;
                        }
                    }
                    return true;
                }
            }
        }

        if (first instanceof ListType){
            if (second instanceof ListType && ((ListType) first).getElementsTypes().size() == ((ListType) second).getElementsTypes().size()){
                for (int i = 0; i < ((ListType) first).getElementsTypes().size(); i++){
                    if (!firstIsSubTypeOfSecond(((ListType) first).getElementsTypes().get(i).getType(), ((ListType) second).getElementsTypes().get(i).getType())){
                        return false;
                    }
                }
                return true;
            }
        }

        if (first instanceof NullType){
            if (second instanceof NullType){
                return true;
            }
        }

        return false;
    }

    void getDeclarationTypeErrors(Type type, VarDeclaration varDeclaration){
        if (type instanceof ListType){
            ListType list_type = (ListType) type;
            if (list_type.getElementsTypes().size() == 0){
                CannotHaveEmptyList exception = new CannotHaveEmptyList(varDeclaration.getLine());
                varDeclaration.addError(exception);
            }
            boolean has_duplicate_name = false;
            ArrayList<ListNameType> elementTypes = list_type.getElementsTypes();
            for (int i = 0; i < elementTypes.size(); i++){
                for (int j = 0; j < elementTypes.size(); j++){
                    if ((i != j) && !elementTypes.get(i).getName().getName().equals("") &&
                            !elementTypes.get(j).getName().getName().equals("") &&
                            elementTypes.get(i).getName().getName().equals(elementTypes.get(j).getName().getName())){
                        DuplicateListId exception = new DuplicateListId(varDeclaration.getLine());
                        varDeclaration.addError(exception);
                        has_duplicate_name = true;
                        break;
                    }
                }
                if (has_duplicate_name) break;
            }

            for (int i = 0; i < elementTypes.size(); i++)
                getDeclarationTypeErrors(elementTypes.get(i).getType(), varDeclaration);

        }

        if (type instanceof ClassType){
            try {
                String classKey = "Class_" + ((ClassType) type).getClassName().getName();
                ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.root.getItem(classKey, true);
            }
            catch (ItemNotFoundException e){
                ClassNotDeclared exception = new ClassNotDeclared(varDeclaration.getLine(), ((ClassType) type).getClassName().getName());
                varDeclaration.addError(exception);
            }
        }

        if (type instanceof FptrType){
            ArrayList<Type> args = ((FptrType) type).getArgumentsTypes();
            for (Type argType : args){
                getDeclarationTypeErrors(argType, varDeclaration);
            }
            getDeclarationTypeErrors(((FptrType) type).getReturnType(), varDeclaration);
        }
    }

    @Override
    public Void visit(Program program) {
        boolean hasMain = false;
        for(ClassDeclaration classDeclaration : program.getClasses()) {
            if (classDeclaration.getClassName().getName().equals("Main")) {
                if (classDeclaration.getConstructor() == null){
                    NoConstructorInMainClass exception = new NoConstructorInMainClass(classDeclaration);
                    program.addError(exception);
                }
                else {
                    if (classDeclaration.getConstructor().getArgs().size() != 0){
                        MainConstructorCantHaveArgs exception = new MainConstructorCantHaveArgs(classDeclaration.getConstructor().getLine());
                        program.addError(exception);
                    }
                }
                if (classDeclaration.getParentClassName() != null){
                    MainClassCantExtend exception = new MainClassCantExtend(classDeclaration.getParentClassName().getLine());
                    program.addError(exception);
                }
                hasMain = true;
            }
            else {
                if (classDeclaration.getParentClassName() != null){
                    if (classDeclaration.getParentClassName().getName().equals("Main")){
                        CannotExtendFromMainClass exception = new CannotExtendFromMainClass(classDeclaration.getParentClassName().getLine());
                        program.addError(exception);
                    }
                    else{
                        try {
                            String classKey = "Class_" + classDeclaration.getParentClassName().getName();
                            ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem) SymbolTable.root.getItem(classKey, true);
                        }
                        catch (ItemNotFoundException e){
                            ClassNotDeclared exception = new ClassNotDeclared(classDeclaration.getParentClassName().getLine(), classDeclaration.getParentClassName().getName());
                            classDeclaration.addError(exception);
                        }
                    }
                }
            }

            classDeclaration.accept(this);
        }

        if (!hasMain) {
            NoMainClass excpetion = new NoMainClass();
            program.addError(excpetion);
        }
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        expressionTypeChecker.setCurrentClassDeclaration(classDeclaration);

        for(FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            fieldDeclaration.accept(this);
        }
        if(classDeclaration.getConstructor() != null) {
            if (!classDeclaration.getConstructor().getMethodName().getName().equals(classDeclaration.getClassName().getName())){
                ConstructorNotSameNameAsClass exception = new ConstructorNotSameNameAsClass(classDeclaration.getConstructor().getLine());
                classDeclaration.getConstructor().addError(exception);
            }
            classDeclaration.getConstructor().accept(this);
        }
        for(MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            methodDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {
        expressionTypeChecker.setCurrentMethodDeclaration(constructorDeclaration);
        currMethodDeclaration = constructorDeclaration;

        for(VarDeclaration varDeclaration : constructorDeclaration.getArgs()) {
            varDeclaration.accept(this);
        }
        for(VarDeclaration varDeclaration : constructorDeclaration.getLocalVars()) {
            varDeclaration.accept(this);
        }
        ArrayList<Statement> body = constructorDeclaration.getBody();
        for (Statement s : body)
            s.accept(this);
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        expressionTypeChecker.setCurrentMethodDeclaration(methodDeclaration);
        currMethodDeclaration = methodDeclaration;

        for(VarDeclaration varDeclaration : methodDeclaration.getArgs()) {
            varDeclaration.accept(this);
        }
        for(VarDeclaration varDeclaration : methodDeclaration.getLocalVars()) {
            varDeclaration.accept(this);
        }
        ArrayList<Statement> body = methodDeclaration.getBody();

        boolean hasReturn = false;
        for (Statement s : body) {
            if (s instanceof ReturnStmt){
                hasReturn = true;
            }
            s.accept(this);
        }
        if (!(methodDeclaration.getReturnType() instanceof NullType) && !(hasReturn)){
            MissingReturnStatement exception = new MissingReturnStatement(methodDeclaration);
            methodDeclaration.addError(exception);
        }

        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        fieldDeclaration.getVarDeclaration().accept(this);
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration) {
        getDeclarationTypeErrors(varDeclaration.getType(), varDeclaration);

        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        expressionTypeChecker.assignStmtIsLValue = true;
        Type ltype = assignmentStmt.getlValue().accept(this.expressionTypeChecker);
        if (!expressionTypeChecker.assignStmtIsLValue){
            LeftSideNotLvalue exception = new LeftSideNotLvalue(assignmentStmt.getlValue().getLine());
            assignmentStmt.addError(exception);
        }

        Type rtype = assignmentStmt.getrValue().accept(this.expressionTypeChecker);

        if (rtype instanceof NoType || ltype instanceof NoType){
            return null;
        }

        if (!firstIsSubTypeOfSecond(rtype, ltype)){
            UnsupportedOperandType exception = new UnsupportedOperandType(assignmentStmt.getLine(), BinaryOperator.assign.toString());
            assignmentStmt.addError(exception);
        }

        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        ArrayList<Statement> block = blockStmt.getStatements();
        for (Statement s : block)
            s.accept(this);
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        Type type1 = conditionalStmt.getCondition().accept(this.expressionTypeChecker);
        if (!(type1 instanceof BoolType)){
            ConditionNotBool exception = new ConditionNotBool(conditionalStmt.getLine());
            conditionalStmt.addError(exception);
        }

        conditionalStmt.getThenBody().accept(this);
        if (conditionalStmt.getElseBody() != null)
            conditionalStmt.getElseBody().accept(this);
        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) {
        expressionTypeChecker.inMethodCallStmt = true;
        methodCallStmt.getMethodCall().accept(this.expressionTypeChecker);
        expressionTypeChecker.inMethodCallStmt = false;
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type type = print.getArg().accept(this.expressionTypeChecker);

        if (!(type instanceof BoolType) && !(type instanceof IntType) && !(type instanceof StringType) && !(type instanceof NoType)) {
            UnsupportedTypeForPrint exception = new UnsupportedTypeForPrint(print.getLine());
            print.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        Type type = returnStmt.getReturnedExpr().accept(this.expressionTypeChecker);

        if (!firstIsSubTypeOfSecond(type, currMethodDeclaration.getReturnType())){
            ReturnValueNotMatchMethodReturnType exception = new ReturnValueNotMatchMethodReturnType(returnStmt);
            returnStmt.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(BreakStmt breakStmt) {
        if (!isInLoop){
            ContinueBreakNotInLoop exception = new ContinueBreakNotInLoop(breakStmt.getLine(), 0);
            breakStmt.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        if (!isInLoop){
            ContinueBreakNotInLoop exception = new ContinueBreakNotInLoop(continueStmt.getLine(), 1);
            continueStmt.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        isInLoop = true;
        foreachStmt.getBody().accept(this);

        Type type1 = foreachStmt.getVariable().accept(this.expressionTypeChecker);
        Type type2 = foreachStmt.getList().accept(this.expressionTypeChecker);
        if (type2 instanceof NoType)
            return null;
        if (!(type2 instanceof ListType)){
            ForeachCantIterateNoneList exception = new ForeachCantIterateNoneList(foreachStmt.getLine());
            foreachStmt.addError(exception);
        }
        else{
            boolean sameElements = true;
            Type firstType = ((ListType) type2).getElementsTypes().get(0).getType();
            for (int i = 1; i < ((ListType) type2).getElementsTypes().size(); i++){ // check later: a[3] (a is (IntType, IntType, NoType))
                if (!(firstIsSubTypeOfSecond(firstType, ((ListType) type2).getElementsTypes().get(i).getType()) &&
                        firstIsSubTypeOfSecond(((ListType) type2).getElementsTypes().get(i).getType(), firstType)) &&
                        (((ListType) type2).getElementsTypes().get(i).getType() instanceof NoType))
                    sameElements = false;
            }
            if (!sameElements){
                ForeachListElementsNotSameType exception = new ForeachListElementsNotSameType(foreachStmt.getList().getLine());
                foreachStmt.addError(exception);
            }

            if (!firstIsSubTypeOfSecond(firstType, type1)){
                ForeachVarNotMatchList exception = new ForeachVarNotMatchList(foreachStmt);
                foreachStmt.addError(exception);
            }
        }

        isInLoop = false;
        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        isInLoop = true;
        forStmt.getInitialize().accept(this);
        Type type = forStmt.getCondition().accept(this.expressionTypeChecker);
        if (!(type instanceof BoolType)){
            ConditionNotBool exception = new ConditionNotBool(forStmt.getLine());
            forStmt.addError(exception);
        }

        forStmt.getUpdate().accept(this);
        forStmt.getBody().accept(this);

        isInLoop = false;
        return null;
    }
}
