package main.visitor.typeChecker;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
import main.ast.types.Type;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;

public class ExpressionTypeChecker extends Visitor<Type> {
    private final Graph<String> classHierarchy;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
//        Type type1 = binaryExpression.getFirstOperand().accept(this);
//        Type type2 = binaryExpression.getSecondOperand().accept(this);
//
//        if (!type1.toString().equals("IntType") || !type2.toString().equals("IntType")){
//
//        }
        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        unaryExpression.getOperand().accept(this);
        return null;
    }

    @Override
    public Type visit(ObjectOrListMemberAccess objectOrListMemberAccess) {
        objectOrListMemberAccess.getInstance().accept(this);
        objectOrListMemberAccess.getMemberName().accept(this);
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        //TODO
        return null;
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        listAccessByIndex.getInstance().accept(this);
        listAccessByIndex.getIndex().accept(this);
        return null;
    }

    @Override
    public Type visit(MethodCall methodCall) {
        ArrayList<Expression> args = methodCall.getArgs();
        methodCall.getInstance().accept(this);
        for (Expression arg : args)
            arg.accept(this);
        return null;
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        ArrayList<Expression> args = newClassInstance.getArgs();
        for (Expression arg : args)
            arg.accept(this);
        return null;
    }

    @Override
    public Type visit(ThisClass thisClass) {
        //TODO
        return null;
    }

    @Override
    public Type visit(ListValue listValue) {
        ArrayList<Expression>  elements = listValue.getElements();
        for (Expression element : elements)
            element.accept(this);
        return null;
    }

    @Override
    public Type visit(NullValue nullValue) {
        //TODO
        return null;
    }

    @Override
    public Type visit(IntValue intValue) {
        //TODO
        return null;
    }

    @Override
    public Type visit(BoolValue boolValue) {
        //TODO
        return null;
    }

    @Override
    public Type visit(StringValue stringValue) {
        //TODO
        return null;
    }
}
