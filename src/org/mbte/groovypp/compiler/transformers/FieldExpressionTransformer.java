package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;

public class FieldExpressionTransformer extends ExprTransformer<FieldExpression> {

    public Expression transform(FieldExpression exp, CompilerTransformer compiler) {
        return new ResolvedFieldBytecodeExpr(
                exp,
                exp.getField(),
                new BytecodeExpr(exp, compiler.classNode) {
                    protected void compile() {
                        mv.visitVarInsn(ALOAD, 0);
                    }
                },
                null
        );
    }
}