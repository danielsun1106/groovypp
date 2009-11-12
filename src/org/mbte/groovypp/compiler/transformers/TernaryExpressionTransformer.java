package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.StaticCompiler;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public class TernaryExpressionTransformer extends ExprTransformer<TernaryExpression>{
    public Expression transform(TernaryExpression exp, CompilerTransformer compiler) {
        if (exp instanceof ElvisOperatorExpression) {
            return transfromElvis((ElvisOperatorExpression)exp, compiler);
        }
        else {
            return transfromTernary(exp, compiler);
        }
    }
    private Expression transfromTernary(TernaryExpression te, CompilerTransformer compiler) {
        final Label elseLabel = new Label();
        final BytecodeExpr condition = compiler.transformLogical(te.getBooleanExpression().getExpression(), elseLabel, false);

        final BytecodeExpr trueE  = (BytecodeExpr) compiler.transform(te.getTrueExpression());
        final BytecodeExpr falseE = (BytecodeExpr) compiler.transform(te.getFalseExpression());

        final ClassNode type = TypeUtil.commonType(trueE.getType(), falseE.getType());

        final BytecodeExpr finalTrueE = compiler.cast(trueE, type);
        final BytecodeExpr finalFalseE = compiler.cast(falseE, type);

        return new BytecodeExpr(te, type) {
            protected void compile(MethodVisitor mv) {
                condition.visit(mv);

                finalTrueE.visit(mv);

                Label endLabel = new Label();
                mv.visitJumpInsn(GOTO, endLabel);

                mv.visitLabel(elseLabel);

                finalFalseE.visit(mv);

                mv.visitLabel(endLabel);
            }
        };
    }

    private Expression transfromElvis(ElvisOperatorExpression ee, CompilerTransformer compiler) {
        ee = (ElvisOperatorExpression) ee.transformExpression(compiler);
        final ElvisOperatorExpression eee = ee;
        return new Elvis(ee, eee);
    }

    private static class Elvis extends BytecodeExpr {
        private final ElvisOperatorExpression eee;

        public Elvis(ElvisOperatorExpression ee, ElvisOperatorExpression eee) {
            super(ee, TypeUtil.commonType(ee.getBooleanExpression().getExpression().getType(), ee.getFalseExpression().getType()));
            this.eee = eee;
        }

        protected void compile(MethodVisitor mv) {
            final BytecodeExpr be = (BytecodeExpr) eee.getBooleanExpression().getExpression();
            be.visit(mv);
            dup(be.getType(), mv);

            Label elseLabel = new Label();
            StaticCompiler.branch(be, IFEQ, elseLabel, mv);

            box (be.getType(), mv);
            cast(TypeUtil.wrapSafely(be.getType()), TypeUtil.wrapSafely(getType()), mv);
            unbox(getType(), mv);
            Label endLabel = new Label();
            mv.visitJumpInsn(GOTO, endLabel);

            mv.visitLabel(elseLabel);
            final BytecodeExpr falseExp = (BytecodeExpr) eee.getFalseExpression();
            pop(be.getType(), mv);
            falseExp.visit(mv);
            box (falseExp.getType(), mv);
            cast(TypeUtil.wrapSafely(falseExp.getType()), TypeUtil.wrapSafely(getType()), mv);
            unbox(getType(), mv);

            mv.visitLabel(endLabel);
        }
    }
}
