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
package org.kie.dmn.feel;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.dmn.api.feel.runtime.events.FEELEventListener;
import org.kie.dmn.feel.codegen.feel11.ProcessedExpression;
import org.kie.dmn.feel.lang.CompiledExpression;
import org.kie.dmn.feel.lang.CompilerContext;
import org.kie.dmn.feel.lang.EvaluationContext;
import org.kie.dmn.feel.lang.FEELProfile;
import org.kie.dmn.feel.lang.Type;
import org.kie.dmn.feel.lang.impl.FEELBuilder;
import org.kie.dmn.feel.runtime.UnaryTest;

/**
 * FEEL expression language engine interface
 *
 * This class is the entry point for the engine use
 */
public interface FEEL {
    
    /**
     * Factory method to create a new FEEL engine instance using custom FEELProfile(s), using the specified classloader.
     *
     * @return a newly instantiated FEEL engine instance
     */
    static FEEL newInstance(ClassLoader cl, List<FEELProfile> profiles) {
        return FEELBuilder.builder().withClassloader(cl).withProfiles(profiles).build();
    }

    /**
     * Factory method to create a new compiler context
     *
     * @return compiler context with default options set
     */
    CompilerContext newCompilerContext();

    /**
     * Compiles the string expression using the given
     * compiler context.
     *
     * @param expression a FEEL expression
     * @param ctx a compiler context
     * @return the compiled expression
     */
    CompiledExpression compile(String expression, CompilerContext ctx);

    /**
     * Process the string expression using the given
     * compiler context. The returned <code>ProcessedExpression</code>
     * could be used to retrieve the actual <code>CompiledExpression</code>
     *
     * @param expression a FEEL expression
     * @param ctx a compiler context
     * @return the processed expression
     */
    ProcessedExpression processExpression(String expression, CompilerContext ctx);

    /**
     * Compiles the string expression using the given
     * compiler context.
     *
     * @param expression a FEEL expression for unary tests
     * @param ctx a compiler context
     * @return the compiled unary tests
     */
    CompiledExpression processUnaryTests(String expression, CompilerContext ctx);

    /**
     * Evaluates the given FEEL expression and returns
     * the result
     *
     * @param expression a FEEL expression
     * @return the result of the evaluation of the expression
     */
    Object evaluate(String expression);
    
    /**
     * Evaluates the given FEEL expression using the
     * given EvaluationContext, and returns the result
     *
     * @param expression a FEEL expression
     * @param ctx the EvaluationContext to be used for defining
     *            input variables and additional feel event listeners
     *            contextual to this method call
     *
     * @return the result of the evaluation of the expression.
     */
    Object evaluate(String expression, EvaluationContext ctx);

    /**
     * Evaluates the given FEEL expression using the
     * given input variables, and returns the result
     *
     * @param expression a FEEL expression
     * @param inputVariables a map of input Variables. The keys
     *                       on the map are the variable names,
     *                       that need to follow the naming rules
     *                       for the FEEL language. The values on
     *                       the map are the corresponding values
     *                       for the variables. It is completely
     *                       fine to use a previously returned FEEL
     *                       context as inputVariables.
     *
     * @return the result of the evaluation of the expression.
     */
    Object evaluate(String expression, Map<String, Object> inputVariables);

    /**
     * Evaluates the given compiled FEEL expression using the
     * given input variables, and returns the result
     *
     * @param expression a FEEL expression
     * @param inputVariables a map of input Variables. The keys
     *                       on the map are the variable names,
     *                       that need to follow the naming rules
     *                       for the FEEL language. The values on
     *                       the map are the corresponding values
     *                       for the variables. It is completely
     *                       fine to use a previously returned FEEL
     *                       context as inputVariables.
     *
     * @return the result of the evaluation of the expression.
     */
    Object evaluate(CompiledExpression expression, Map<String, Object> inputVariables);
    
    /**
     * Evaluates the given compiled FEEL expression using the
     * given EvaluationContext, and returns the result
     *
     * @param expr a FEEL expression
     * @param ctx the EvaluationContext to be used for defining
     *            input variables and additional feel event listeners
     *            contextual to this method call
     *
     * @return the result of the evaluation of the expression.
     */
    Object evaluate(CompiledExpression expr, EvaluationContext ctx);

    /**
     * Evaluates the given expression as a list of of unary tests.
     * The syntax for this is defined in the FEEL grammar rule #17,
     * i.e., a list of unary tests separated by commas.
     *
     * @param expression a unary test list expression
     *
     * @return a List of compiled UnaryTests
     */
    List<UnaryTest> evaluateUnaryTests( String expression );

    /**
     * Evaluates the given expression as a list of of unary tests.
     * The syntax for this is defined in the FEEL grammar rule #17,
     * i.e., a list of unary tests separated by commas.
     *
     * @param expression a unary test list expression
     * @param variableTypes map of variable names and corresponding types,
     *                      necessary to compile the unary tests
     *
     * @return a List of compiled UnaryTests
     */
    List<UnaryTest> evaluateUnaryTests(String expression, Map<String, Type> variableTypes);

    /**
     * Registers a new event listener into this FEEL instance.
     * The event listeners are notified about signitificative
     * events during compilation or evaluation of expressions.
     *
     * @param listener the listener to register
     */
    void addListener( FEELEventListener listener );

    /**
     * Removes a listener from the list of event listeners.
     *
     * @param listener the listener to remove
     */
    void removeListener( FEELEventListener listener );

    /**
     * Retrieves the set of registered event listeners
     *
     * @return the set of listeners
     */
    Set<FEELEventListener> getListeners();
}
