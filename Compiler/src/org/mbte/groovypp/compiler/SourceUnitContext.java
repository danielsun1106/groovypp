/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.compiler;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SourceUnitContext {
    private int syntheticAccessorNumber = 1979;
    private int tempVarNumber = 1979;
    public Map<FieldNode, MethodNode> generatedFieldGetters = new HashMap<FieldNode, MethodNode>();
    public Map<FieldNode, MethodNode> generatedFieldSetters = new HashMap<FieldNode, MethodNode>();
    public Map<MethodNode, MethodNode> generatedMethodDelegates = new HashMap<MethodNode, MethodNode>();
    private Map<MethodNode, Integer> generatedSuperMethodAccessorNumbers = new HashMap<MethodNode, Integer>();

    private Set<ClassNode> outerClassInstanceUsers = new HashSet<ClassNode>();

    public MethodNode getFieldGetter(FieldNode field) {
        MethodNode getter = generatedFieldGetters.get(field);
        if (getter == null) {
            initAccessors(field);
            getter = generatedFieldGetters.get(field);
        }
        return getter;
    }

    public MethodNode getFieldSetter(FieldNode field) {
        MethodNode setter = generatedFieldSetters.get(field);
        if (setter == null) {
            initAccessors(field);
            setter = generatedFieldSetters.get(field);
        }
        return setter;
    }

    private void initAccessors(FieldNode field) {
        int num = syntheticAccessorNumber++;
        String getterName = "getField" + num;
        int modifiers = field.getModifiers() & Opcodes.ACC_STATIC;
        MethodNode getter = new MethodNode(getterName, modifiers, field.getType(), new Parameter[0], new ClassNode[0],
                new ReturnStatement(new VariableExpression(field)));
        ClassNode clazz = field.getDeclaringClass();
        getter.setDeclaringClass(clazz);
        clazz.addMethod(getter);
        generatedFieldGetters.put(field, getter);
        if (!field.isFinal()) {
            String setterName = "setField" + num;
            Parameter[] setterParams = {new Parameter(field.getType(), "p")};
            ExpressionStatement code = new ExpressionStatement(new BinaryExpression(new VariableExpression(field),
                                                               Token.newSymbol(Types.ASSIGN, -1, -1),
                                                               new VariableExpression("p")));
            MethodNode setter = new MethodNode(setterName, modifiers, ClassHelper.VOID_TYPE, setterParams, new ClassNode[0],
                    code);
            setter.setDeclaringClass(clazz);
            clazz.addMethod(setter);
            generatedFieldSetters.put(field, setter);
        }
        ClassNodeCache.clearCache(clazz);
    }

    public MethodNode getMethodDelegate(MethodNode method) {
        MethodNode delegate = generatedMethodDelegates.get(method);
        if (delegate == null) {
            int num = syntheticAccessorNumber++;
            String name = "delegate" + num;
            int modifiers = method.getModifiers() & Opcodes.ACC_STATIC;

            Expression[] exprs = new Expression[method.getParameters().length];
            for (int i = 0; i < exprs.length; i++) {
                exprs[i] = new VariableExpression(method.getParameters()[i]);
            }
            Expression argList = new ArgumentListExpression(exprs);
            delegate = new MethodNode(name, modifiers, method.getReturnType(), method.getParameters(), new ClassNode[0],
                    new ReturnStatement(new MethodCallExpression(new VariableExpression("this", ClassHelper.DYNAMIC_TYPE),
                            method.getName(), argList)));
            delegate.setGenericsTypes(method.getGenericsTypes());
            generatedMethodDelegates.put(method, delegate);
            ClassNode clazz = method.getDeclaringClass();
            delegate.setDeclaringClass(clazz);
            clazz.addMethod(delegate);
            ClassNodeCache.clearCache(clazz);
        }
        return delegate;
    }
    public MethodNode getConstructorDelegate(MethodNode constructor) {
        MethodNode delegate = generatedMethodDelegates.get(constructor);
        if (delegate == null) {
            int num = syntheticAccessorNumber++;
            String name = "delegate" + num;
            int modifiers = Opcodes.ACC_STATIC;

            Expression[] exprs = new Expression[constructor.getParameters().length];
            for (int i = 0; i < exprs.length; i++) {
                exprs[i] = new VariableExpression(constructor.getParameters()[i]);
            }
            Expression argList = new ArgumentListExpression(exprs);
            delegate = new MethodNode(name, modifiers, constructor.getDeclaringClass(), constructor.getParameters(), new ClassNode[0],
                    new ReturnStatement(new ConstructorCallExpression(constructor.getDeclaringClass(), argList)));
            generatedMethodDelegates.put(constructor, delegate);
            ClassNode clazz = constructor.getDeclaringClass();
            delegate.setDeclaringClass(clazz);
            clazz.addMethod(delegate);
            ClassNodeCache.clearCache(clazz);
        }
        return delegate;
    }

    public MethodNode getSuperMethodDelegate(MethodNode superMethod, ClassNode placeClass) {
        Integer num = generatedSuperMethodAccessorNumbers.get(superMethod);
        if (num == null) {
            num = syntheticAccessorNumber++;
            generatedSuperMethodAccessorNumbers.put(superMethod, num);
        }
        String name = "delegate" + num;
        final ClassNode declaringClass = superMethod.getDeclaringClass();
        final Parameter[] superParams = superMethod.getParameters();
        Parameter[] params = new Parameter[superParams.length];
        for (int i = 0; i < params.length; i++) {
            ClassNode type = TypeUtil.mapTypeFromSuper(superParams[i].getType(), declaringClass, placeClass);
            params[i] = new Parameter(type, superParams[i].getName());
        }
        MethodNode delegate = placeClass.getMethod(name, superParams);
        if (delegate == null) {

            Expression[] exprs = new Expression[superParams.length];
            for (int i = 0; i < exprs.length; i++) {
                exprs[i] = new VariableExpression(params[i]);
            }
            Expression argList = new ArgumentListExpression(exprs);
            ClassNode ret = TypeUtil.mapTypeFromSuper(superMethod.getReturnType(), declaringClass, placeClass);
            final MethodCallExpression call = new MethodCallExpression(new VariableExpression("super", ClassHelper.DYNAMIC_TYPE),
                    superMethod.getName(), argList);
            final Statement statement = ret != ClassHelper.VOID_TYPE ? new ReturnStatement(call) : new ExpressionStatement(call);
            final int modifiers = superMethod.getModifiers() & ~Opcodes.ACC_ABSTRACT;
            delegate = new MethodNode(name, modifiers, ret, params, new ClassNode[0], statement);
            delegate.setDeclaringClass(placeClass);
            placeClass.addMethod(delegate);
            ClassNodeCache.clearCache(placeClass);
        }
        return delegate;
    }


    public void setOuterClassInstanceUsed(ClassNode node) {
        outerClassInstanceUsers.add(node);
    }

    public boolean isOuterClassInstanceUsed(ClassNode node) {
        return outerClassInstanceUsers.contains(node);
    }

    public String getNextTempVarName() {
        return "$temp" + (tempVarNumber++);
    }
}
