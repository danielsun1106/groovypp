package org.mbte.groovypp.compiler

import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.mbte.groovypp.compiler.TypeUtil
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.syntax.ASTHelper
import org.codehaus.groovy.classgen.BytecodeSequence
import org.codehaus.groovy.classgen.BytecodeInstruction
import org.codehaus.groovy.classgen.Verifier

@Typed
@GroovyASTTransformation (phase = CompilePhase.CANONICALIZATION)
class StructASTTransform implements ASTTransformation, Opcodes {

    static final ClassNode ASTRUCT = make(AbstractStruct)
    static final ClassNode ABUILDER = make(AbstractStruct.Builder)
    static final ClassNode STRING_BUILDER = make(StringBuilder)

    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode module = nodes[0]
        for (ClassNode classNode: new ArrayList(module.classes)) {
            processClass classNode, source
        }
    }

    private void processClass (ClassNode classNode, SourceUnit source) {
        def typed = false, process = false
        for (AnnotationNode ann : classNode.getAnnotations()) {
            final String withoutPackage = ann.getClassNode().getNameWithoutPackage();
            if (withoutPackage.equals("Struct")) {
                process = true;
            }
            if (withoutPackage.equals("Typed")) {
                typed = true;
            }
        }

        if (process) {
            String name = classNode.getNameWithoutPackage() + "\$Builder";
            String fullName = ASTHelper.dot(classNode.getPackageName(), name);

            for(c in classNode.module.classes)
                if (c.name == fullName)
                    return

            if (!typed)
                classNode.addAnnotation(new AnnotationNode(TypeUtil.TYPED))

            ClassNode superClass = classNode.getSuperClass();
            if (ClassHelper.OBJECT_TYPE.equals(superClass)) {
                classNode.setSuperClass(ASTRUCT)
                superClass = ASTRUCT
            }

            ClassNode superBuilder
            for(ClassNode bc : superClass.getInnerClasses()) {
                if (bc.getName().endsWith("\$Builder")) {
                    superBuilder = bc
                    break;
                }
            }

            if (!superBuilder) {
                if(superClass.module) {
                    processClass(superClass, superClass.module.getContext())
                }
                else {
                    superBuilder = ABUILDER
                }
            }

            InnerClassNode builderClassNode = new InnerClassNode(classNode, fullName, ACC_PUBLIC|ACC_STATIC, superBuilder, ClassNode.EMPTY_ARRAY, null)
            AnnotationNode typedAnn = new AnnotationNode(TypeUtil.TYPED)
            final Expression member = classNode.getAnnotations(TypeUtil.TYPED).get(0).getMember("debug")
            if (member != null && member instanceof ConstantExpression && ((ConstantExpression)member).value.equals(Boolean.TRUE))
                typedAnn.addMember("debug", ConstantExpression.TRUE)
            builderClassNode.addAnnotation(typedAnn)

            builderClassNode.genericsTypes = builderGenericTypes(classNode)
            builderClassNode.superClass = TypeUtil.withGenericTypes(superBuilder, builderSuperclassTypes(classNode, builderClassNode))

            classNode.getModule().addClass(builderClassNode);

            SerialASTTransform.processClass(classNode, source)

            addFactoryMethods (classNode, builderClassNode)

            classNode.properties.clear ()

            for (f in classNode.fields) {
                if (!f.static && !(f.name == "metaClass")) {
                    f.modifiers = (f.modifiers & ~(Opcodes.ACC_PUBLIC & Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE
                    addFieldMethods(f, classNode, builderClassNode)
                }
            }

            addToString (classNode)
        }

        for( c in classNode.innerClasses)
            processClass(c, source)
    }

    GenericsType [] builderGenericTypes(ClassNode classNode) {
        def gt = classNode.getGenericsTypes()
        if (!gt)
            gt = new GenericsType[0]

        def builderGt = new GenericsType[gt.length + 1]
        ClassNode varT = ClassHelper.make("T")
        varT.setRedirect(classNode)
        varT.genericsPlaceHolder = true
        builderGt[0] = new GenericsType(varT, [classNode], null)
        for (i in 0..<gt.length)
            builderGt[i + 1] = gt[i]
        return builderGt
    }

    GenericsType [] builderSuperclassTypes(ClassNode classNode, ClassNode builderClassNode) {
        def gt = classNode.getUnresolvedSuperClass(false).genericsTypes
        if (!gt)
            gt = new GenericsType[0]

        def builderGt = new GenericsType[gt.length+1]
        builderGt[0] = new GenericsType(classNode)
        for (i in 0..<gt.length)
            builderGt[i+1] = gt[i]
        return builderGt
    }

    def addFactoryMethods(ClassNode classNode, ClassNode innerClass) {
//    static Builder newBuilder(StructTest<A> self = null) {
//        new Builder(self)
//    }
        classNode.addMethod(
                "newBuilder",
                Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,
                innerClass,
                [[classNode, "obj", ConstantExpression.NULL]],
                [], new ExpressionStatement(
                        new ConstructorCallExpression(
                                innerClass,
                                new VariableExpression("obj")
                        )
                )
        )

//    static class Builder<T extends StructTest,A> extends AbstractStruct.Builder<T> {
//        Builder (T obj = null) {
//            super(obj != null ? (T)obj.clone() : new StructTest())
//        }
        innerClass.addConstructor(
            Opcodes.ACC_PUBLIC,
            [[classNode, "obj", ConstantExpression.NULL]],
            [],
            new ExpressionStatement(
                new ConstructorCallExpression(
                    ClassNode.SUPER,
                    new ArgumentListExpression(
                        new TernaryExpression(
                            new BooleanExpression(new VariableExpression("obj")),
                            new CastExpression(
                                classNode,
                                new MethodCallExpression(
                                    new VariableExpression("obj"),
                                    "clone",
                                    new ArgumentListExpression()
                                )
                            ),
                            new ConstructorCallExpression(
                                classNode,
                                new ArgumentListExpression()
                            )
                        )
                    )
                )
            )
        )
    }

    void addFieldMethods(FieldNode fieldNode, ClassNode classNode, InnerClassNode innerClassNode) {
        innerClassNode.addMethod(
            "get" + Verifier.capitalize(fieldNode.name),
            Opcodes.ACC_PUBLIC,
            fieldNode.type,
            [],
            [], new ExpressionStatement(
                new PropertyExpression(
                    new PropertyExpression(VariableExpression.THIS_EXPRESSION,"obj"),
                    fieldNode.name
                )
            )
        )

        if (!fieldNode.final) {
            innerClassNode.addMethod(
                "set" + Verifier.capitalize(fieldNode.name),
                Opcodes.ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                [[fieldNode.type, "value"]],
                [], new ExpressionStatement(
                    new BinaryExpression(
                        new PropertyExpression(
                            new PropertyExpression(VariableExpression.THIS_EXPRESSION,"obj"),
                            fieldNode.name
                        ),
                        Token.newSymbol(Types.ASSIGN,-1,-1),
                        new VariableExpression("value")
                    )
                )
            )
        }
    }

    void addToString(ClassNode classNode) {
        def code = new BlockStatement ()

        code.addStatement(
            new ExpressionStatement(
                new MethodCallExpression(
                    VariableExpression.SUPER_EXPRESSION,
                    "toString",
                    new ArgumentListExpression(new VariableExpression("sb"))
                )
            )
        )

        boolean nonFirst = false
        for(def cn = classNode.superClass; !nonFirst && cn; cn = cn.superClass) {
            for(ff in cn.fields) {
                if (!ff.static && !(ff.name == "metaClass")) {
                    nonFirst = true
                    break
                }
            }
        }

        for (f in classNode.fields) {
            if (!f.static && !(f.name == "metaClass")) {
                if(nonFirst) {
                    code.addStatement(
                        new ExpressionStatement(
                            new MethodCallExpression(
                                new VariableExpression("sb"),
                                "append",
                                new ArgumentListExpression(new ConstantExpression(", "))
                            )
                        )
                    )
                }
                nonFirst = true

                code.addStatement(
                    new ExpressionStatement(
                        new MethodCallExpression(
                            new MethodCallExpression(
                                new VariableExpression("sb"),
                                "append",
                                new ArgumentListExpression(new ConstantExpression(f.name + ": "))
                            ),
                            "append",
                            new ArgumentListExpression(new FieldExpression(f))
                        )
                    )
                )
            }
        }

        classNode.addMethod(
                "toString",
                Opcodes.ACC_PUBLIC,
                ClassHelper.VOID_TYPE,
                [[STRING_BUILDER, "sb"]],
                [],
                code
        )
    }
}
