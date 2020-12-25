package main.visitor.typeChecker;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.symbolTable.utils.graph.Graph;

import main.visitor.Visitor;
import main.ast.types.Type;
import main.ast.types.list.ListType;
import main.ast.types.list.ListNameType;

import main.compileErrorException.typeErrors.UnsupportedTypeForPrint;
import main.compileErrorException.typeErrors.ConditionNotBool;
import main.compileErrorException.typeErrors.CannotHaveEmptyList;
import main.compileErrorException.typeErrors.DuplicateListId;

import java.util.ArrayList;

public class TypeChecker extends Visitor<Void> {
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;

    public TypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
    }

    @Override
    public Void visit(Program program) {
        for(ClassDeclaration classDeclaration : program.getClasses()) {
            classDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        for(FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            fieldDeclaration.accept(this);
        }
        if(classDeclaration.getConstructor() != null) {
            classDeclaration.getConstructor().accept(this);
        }
        for(MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            methodDeclaration.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {

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
        for(VarDeclaration varDeclaration : methodDeclaration.getArgs()) {
            varDeclaration.accept(this);
        }
        for(VarDeclaration varDeclaration : methodDeclaration.getLocalVars()) {
            varDeclaration.accept(this);
        }
        ArrayList<Statement> body = methodDeclaration.getBody();
        for (Statement s : body)
            s.accept(this);
        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) {
        fieldDeclaration.getVarDeclaration().accept(this);
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration) {
        Type type = varDeclaration.getVarName().accept(this.expressionTypeChecker);

        if (varDeclaration.getType().toString().equals("ListType")){
            ListType list_type = (ListType) varDeclaration.getType();
            if (list_type.getElementsTypes().size() == 0){
                CannotHaveEmptyList exception = new CannotHaveEmptyList(varDeclaration.getLine());
                varDeclaration.addError(exception);
            }
            boolean has_duplicate_name = false;
            for (ListNameType list_name1 : list_type.getElementsTypes()){
                for (ListNameType list_name2 : list_type.getElementsTypes()){
                    if (list_name1.getName().getName().equals(list_name2.getName().getName())){
                        DuplicateListId exception = new DuplicateListId(varDeclaration.getLine());
                        varDeclaration.addError(exception);
                        has_duplicate_name = true;
                        break;
                    }
                }
                if (has_duplicate_name) break;
            }
        }

        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        assignmentStmt.getlValue().accept(this.expressionTypeChecker);
        assignmentStmt.getrValue().accept(this.expressionTypeChecker);
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
        if (!type1.toString().equals("BoolType")){
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
        methodCallStmt.getMethodCall().accept(this.expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type type = print.getArg().accept(this.expressionTypeChecker);

        if (!((type.toString().equals("BoolType")) || (type.toString().equals("IntType")) || (type.toString().equals("StringType")))) {
            UnsupportedTypeForPrint exception = new UnsupportedTypeForPrint(print.getLine());
            print.addError(exception);
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        returnStmt.getReturnedExpr().accept(this.expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(BreakStmt breakStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        //TODO
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        foreachStmt.getVariable().accept(this.expressionTypeChecker);
        foreachStmt.getList().accept(this.expressionTypeChecker);
        foreachStmt.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        forStmt.getInitialize().accept(this);
        Type type1 = forStmt.getCondition().accept(this.expressionTypeChecker);
        if (!type1.toString().equals("BoolType")){
            ConditionNotBool exception = new ConditionNotBool(forStmt.getLine());
            forStmt.addError(exception);
        }

        forStmt.getUpdate().accept(this);
        forStmt.getBody().accept(this);
        return null;
    }

}
