/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.mvel.asm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.base.base.ValueResolver;
import org.drools.base.reteoo.BaseTuple;
import org.drools.kiesession.rulebase.InternalKnowledgeBase;
import org.drools.util.ClassTypeResolver;
import org.drools.util.TypeResolver;
import org.drools.core.common.InternalFactHandle;
import org.drools.base.definitions.InternalKnowledgePackage;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.accessor.CompiledInvoker;
import org.mvel2.asm.Label;
import org.mvel2.asm.MethodVisitor;

import static org.drools.util.ClassUtils.convertFromPrimitiveType;
import static org.drools.util.ClassUtils.convertPrimitiveNameToType;
import static org.mvel2.asm.Opcodes.AALOAD;
import static org.mvel2.asm.Opcodes.ACC_FINAL;
import static org.mvel2.asm.Opcodes.ACC_PRIVATE;
import static org.mvel2.asm.Opcodes.ACC_PUBLIC;
import static org.mvel2.asm.Opcodes.ALOAD;
import static org.mvel2.asm.Opcodes.ARETURN;
import static org.mvel2.asm.Opcodes.ASTORE;
import static org.mvel2.asm.Opcodes.CHECKCAST;
import static org.mvel2.asm.Opcodes.GOTO;
import static org.mvel2.asm.Opcodes.ICONST_0;
import static org.mvel2.asm.Opcodes.IFNE;
import static org.mvel2.asm.Opcodes.IFNULL;
import static org.mvel2.asm.Opcodes.IF_ICMPLE;
import static org.mvel2.asm.Opcodes.ILOAD;
import static org.mvel2.asm.Opcodes.INVOKEVIRTUAL;
import static org.mvel2.asm.Opcodes.IRETURN;
import static org.mvel2.asm.Opcodes.ISTORE;

public final class GeneratorHelper {

    public static final Long INVOKER_SERIAL_UID = Long.valueOf(510L);

    // DeclarationMatcher

    public static List<DeclarationMatcher> matchDeclarationsToTuple(Declaration[] declarations) {
        List<DeclarationMatcher> matchers = new ArrayList<>();
        for (int i = 0; i < declarations.length; i++) {
            matchers.add(new DeclarationMatcher(i, declarations[i]));
        }
        Collections.sort(matchers);
        return matchers;
    }

    public static class DeclarationMatcher implements Comparable {
        private final int         matcherIndex;
        private final Declaration declaration;
        private final int         tupleIndex;

        public DeclarationMatcher(int matchersIndex, Declaration declaration) {
            this.matcherIndex = matchersIndex;
            this.declaration = declaration;
            this.tupleIndex = declaration.getTupleIndex();
        }

        public int getMatcherIndex() {
            return matcherIndex;
        }

        public int getTupleIndex() {
            return tupleIndex;
        }

        public Declaration getDeclaration() {
            return declaration;
        }

        public int compareTo(Object obj) {
            return ((DeclarationMatcher) obj).tupleIndex - tupleIndex;
        }
    }

    private static ClassLoader getClassLoader(final Object obj, final ValueResolver valueResolver) {
        // use the same ClassLoader used for the stub
        return obj.getClass().getClassLoader();
    }

    static ClassGenerator createInvokerClassGenerator( InvokerStub stub, ValueResolver valueResolver) {
        return createInvokerClassGenerator(stub, "", valueResolver);
    }

    static ClassGenerator createInvokerClassGenerator(InvokerStub stub, String classSuffix, ValueResolver valueResolver) {
        String className = stub.getPackageName() + "." + stub.getGeneratedInvokerClassName() + classSuffix;
        ClassLoader classLoader = getClassLoader(stub, valueResolver);
        return createInvokerClassGenerator(className, stub, classLoader, getTypeResolver(stub, valueResolver, classLoader));
    }

    public static ClassGenerator createInvokerClassGenerator(final String className,
                                                             final InvokerDataProvider data,
                                                             final ClassLoader classLoader,
                                                             final TypeResolver typeResolver) {
        final ClassGenerator generator = new ClassGenerator(className, classLoader, typeResolver)
                .addStaticField(ACC_PRIVATE + ACC_FINAL, "serialVersionUID", Long.TYPE, INVOKER_SERIAL_UID)
                .addDefaultConstructor();

        generator.addMethod(ACC_PUBLIC, "hashCode", generator.methodDescr(Integer.TYPE), new ClassGenerator.MethodBody() {
            public void body(MethodVisitor mv) {
                push(data.hashCode());
                mv.visitInsn(IRETURN);
            }
        })
                .addMethod(ACC_PUBLIC, "getMethodBytecode", generator.methodDescr(String.class), new GetMethodBytecodeMethod(data))
                .addMethod(ACC_PUBLIC, "equals", generator.methodDescr(Boolean.TYPE, Object.class), new EqualsMethod());

        return generator;
    }

    static TypeResolver getTypeResolver(final InvokerStub stub, final ValueResolver valueResolver, final ClassLoader classLoader) {
        InternalKnowledgePackage pkg = ((InternalKnowledgeBase)valueResolver.getRuleBase()).getPackage(stub.getPackageName());
        TypeResolver typeResolver = pkg == null ? null : pkg.getTypeResolver();
        if (typeResolver == null) {
            Set<String> imports = new HashSet<>();
            Collections.addAll(imports, stub.getPackageImports());
            typeResolver = new ClassTypeResolver(imports, classLoader, stub.getPackageName());
        }
        return typeResolver;
    }

    // Reusable ASM generated methods

    public static class GetMethodBytecodeMethod extends ClassGenerator.MethodBody {

        private InvokerDataProvider data;

        public GetMethodBytecodeMethod(InvokerDataProvider data) {
            this.data = data;
        }

        @Override
        public void body(MethodVisitor mv) {
            mv.visitVarInsn(ALOAD, 0);
            invokeVirtual(Object.class, "getClass", Class.class);
            push(data.getRuleClassName());
            push(data.getPackageName());
            push(data.getMethodName());
            push(data.getInternalRuleClassName() + ".class");
            invokeStatic(MethodComparator.class, "getMethodBytecode", String.class, Class.class, String.class, String.class, String.class, String.class);
            mv.visitInsn(ARETURN);
        }
    }

    public static class EqualsMethod extends ClassGenerator.MethodBody {

        @Override
        public void body(MethodVisitor mv) {
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitVarInsn(ALOAD, 1); // if (object == null)
            mv.visitJumpInsn(IFNULL, l1);
            mv.visitVarInsn(ALOAD, 1);
            instanceOf(CompiledInvoker.class);
            mv.visitJumpInsn(IFNE, l2); // if (!(object instanceof  org.kie.spi.CompiledInvoker))
            mv.visitLabel(l1);
            mv.visitInsn(ICONST_0); // return false
            mv.visitInsn(IRETURN);
            mv.visitLabel(l2);
            mv.visitVarInsn(ALOAD, 0);
            invokeThis("getMethodBytecode", String.class);
            mv.visitVarInsn(ALOAD, 1);
            cast(CompiledInvoker.class);
            invokeInterface(CompiledInvoker.class, "getMethodBytecode", String.class);
            invokeStatic(MethodComparator.class, "compareBytecode", Boolean.TYPE, String.class, String.class);
            // return MethodComparator.compareBytecode(getMethodBytecode(), ((CompiledInvoker)object).getMethodBytecode());
            mv.visitInsn(IRETURN);
        }
    }

    public static abstract class DeclarationAccessorMethod extends ClassGenerator.MethodBody {

        protected int storeObjectFromDeclaration(Declaration declaration, int registry) {
            return storeObjectFromDeclaration(declaration, declaration.getTypeName(), registry);
        }

        protected int storeObjectFromDeclaration(Declaration declaration, String declarationType, int registry) {
            String readMethod = declaration.getNativeReadMethodName();
            boolean isObject = readMethod.equals("getValue");
            String expectedTypeDescr = typeDescr(declarationType);
            boolean needsPrimitive = !(expectedTypeDescr.startsWith("L") || expectedTypeDescr.startsWith("["));
            String returnedType = isObject ? "Ljava/lang/Object;" : typeDescr(declaration.getTypeName());
            mv.visitMethodInsn(INVOKEVIRTUAL, Declaration.class.getName().replace('.', '/'), readMethod,
                               "(L" + ValueResolver.class.getName().replace('.', '/') + ";Ljava/lang/Object;)" + returnedType);
            if (isObject) {
                Class<?> declarationClass = declaration.getDeclarationClass();
                if (declarationClass != null) {
                    cast(declarationClass);
                }
            }

            if (needsPrimitive && isObject) {
                castToPrimitive(convertPrimitiveNameToType(declarationType));
            } else if (!needsPrimitive && !isObject) {
                castFromPrimitive(convertPrimitiveNameToType(declaration.getExtractor().getExtractToClassName()));
            } else if (needsPrimitive && !isObject && !returnedType.equals(declarationType)) {
                castPrimitiveToPrimitive(declaration.getExtractor().getExtractToClass(), convertPrimitiveNameToType(declarationType));
            }

            return store(registry, declarationType);
        }

        protected BaseTuple traverseTuplesUntilDeclaration(BaseTuple currentTuple, int tupleIndex, int tupleReg) {
            // do not use currentTuple.skipEmptyHandles(), as we use else where, because we need the getParent as part of the generated code.
            while (currentTuple.getIndex() != tupleIndex) {
                // FactHandle is null for eval, not and join nodes as it has no right input
                mv.visitVarInsn(ALOAD, tupleReg);
                invokeInterface(BaseTuple.class, "getParent", BaseTuple.class);
                mv.visitVarInsn(ASTORE, tupleReg); // tuple = tuple.getParent()
                currentTuple = currentTuple.getParent();
            }
            return currentTuple;
        }

        protected void traverseTuplesUntilDeclarationWithOr(int declarIndex, int declarReg, int tupleReg, int declarOffsetReg) {
            mv.visitVarInsn(ALOAD, declarReg);
            push(declarIndex);
            mv.visitInsn(AALOAD); // declarations[i]
            invokeVirtual(Declaration.class, "getPattern", Pattern.class);
            invokeVirtual(Pattern.class, "getOffset", Integer.TYPE); // declarations[i].getPattern().getOffset()
            mv.visitVarInsn(ISTORE, declarOffsetReg); // declarations[i].getPattern().getOffset()

            // while (tuple.getQueueIndex() > declaration[i].getPattern().getOffset()) tuple = tuple.getParent()
            Label whileStart = new Label();
            Label whileExit = new Label();
            mv.visitLabel(whileStart);
            mv.visitVarInsn(ALOAD, tupleReg);
            invokeInterface(BaseTuple.class, "getIndex", Integer.TYPE); // tuple.getIndex()
            mv.visitVarInsn(ILOAD, declarOffsetReg); // declarations[i].getPattern().getOffset()
            mv.visitJumpInsn(IF_ICMPLE, whileExit); // if tuple.getQueueIndex() <= declarations[i].getPattern().getOffset() jump to whileExit
            mv.visitVarInsn(ALOAD, tupleReg);
            invokeInterface(BaseTuple.class, "getParent", BaseTuple.class);
            mv.visitVarInsn(ASTORE, tupleReg); // tuple = tuple.getParent()
            mv.visitJumpInsn(GOTO, whileStart);
            mv.visitLabel(whileExit);
        }
    }

    public static abstract class EvaluateMethod extends DeclarationAccessorMethod {

        protected int objAstorePos;

        protected int[] parseDeclarations(Declaration[] declarations, int declarReg, int tupleReg, int wmReg, boolean readLocalsFromTuple) {
            int[] declarationsParamsPos = new int[declarations.length];
            // DeclarationTypes[i] value[i] = (DeclarationTypes[i])localDeclarations[i].getValue(reteEvaluator, object);
            for (int i = 0; i < declarations.length; i++) {
                declarationsParamsPos[i] = objAstorePos;
                mv.visitVarInsn(ALOAD, declarReg); // declarations
                push(i);
                mv.visitInsn(AALOAD);  // declarations[i]
                mv.visitVarInsn(ALOAD, wmReg); // workingMemory
                if (readLocalsFromTuple) {
                    // tuple.get(declarations[i])).getObject()
                    mv.visitVarInsn(ALOAD, tupleReg); // tuple
                    mv.visitVarInsn(ALOAD, declarReg);
                    push(i);
                    mv.visitInsn(AALOAD);  // declarations[i]
                    invokeInterface(BaseTuple.class, "get", InternalFactHandle.class, Declaration.class);
                    invokeInterface(InternalFactHandle.class, "getObject", Object.class);
                } else {
                    mv.visitVarInsn(ALOAD, 1); // object
                }

                String readMethod = declarations[i].getNativeReadMethodName();
                boolean isObject = readMethod.equals("getValue");
                String declarationType = declarations[i].getTypeName();
                String returnedType = isObject ? "Ljava/lang/Object;" : typeDescr(declarationType);
                mv.visitMethodInsn(INVOKEVIRTUAL, Declaration.class.getName().replace('.', '/'), readMethod, "(L" + ValueResolver.class.getName().replace('.', '/') + ";Ljava/lang/Object;)" + returnedType);
                if (isObject) {
                    mv.visitTypeInsn(CHECKCAST, internalName(declarationType));
                }
                objAstorePos += store(objAstorePos, declarationType); // obj[i]
            }
            return declarationsParamsPos;
        }

        protected void parseGlobals(String[] globals, String[] globalTypes, int wmReg, StringBuilder methodDescr) {
            for (int i = 0; i < globals.length; i++) {
                String globalType = globalTypes[i];
                if (globalType.indexOf('<') > 0) {
                    globalType = globalType.substring(0, globalType.indexOf('<')).trim();
                }

                mv.visitVarInsn(ALOAD, wmReg); // workingMemory
                push(globals[i]);
                invokeInterface(ValueResolver.class, "getGlobal", Object.class, String.class);
                Class<?> primitiveType = convertPrimitiveNameToType(globalType);
                if (primitiveType != null) {
                    cast(convertFromPrimitiveType(primitiveType), primitiveType);
                } else {
                    mv.visitTypeInsn(CHECKCAST, internalName(globalType));
                }
                methodDescr.append(typeDescr(globalType));
            }
        }

        protected void storeObjectFromDeclaration(Declaration declaration, String declarationType) {
            objAstorePos += storeObjectFromDeclaration(declaration, declarationType, objAstorePos);
        }
    }
}
