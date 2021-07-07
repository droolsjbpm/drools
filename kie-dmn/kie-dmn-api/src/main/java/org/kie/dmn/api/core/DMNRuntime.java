/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.api.core;

import java.util.List;

import org.kie.dmn.api.core.event.DMNRuntimeEventManager;

public interface DMNRuntime extends DMNRuntimeEventManager {

    /**
     * Returns a list of all models available to this runtime
     *
     * @return the list of available models. An empty list in
     *         case no model is available.
     */
    List<DMNModel> getModels();

    /**
     * Returns the model registered with the given namespace and
     * model name.
     *
     * @param namespace the name space for the model
     * @param modelName the name of the model
     *
     * @return the corresponding DMN model, or null if none is
     *         registered with the given name and namespace.
     */
    DMNModel getModel( String namespace, String modelName );
    
    /**
     * Returns the model registered with the given namespace and
     * model id.
     *
     * @param namespace the name space for the model
     * @param modelId the identifier of the model
     *
     * @return the corresponding DMN model, or null if none is
     *         registered with the given id and namespace.
     */
    DMNModel getModelById( String namespace, String modelId );

    /**
     * Evaluate all decisions for the model, given the context
     *
     * @param model the model to evaluate
     * @param context the context with all the input variables
     *
     * @return the result of the evaluation
     */
    DMNResult evaluateAll( DMNModel model, DMNContext context );

    /**
     * Evaluate the decision identified by the given name and
     * all dependent decisions, given the context
     *
     * @deprecated consider using {@link #evaluateByName(DMNModel, DMNContext, String...)} instead
     *
     * @param model the model to evaluate
     * @param decisionName the root decision to evaluate, identified
     *                     by name
     * @param context the context with all the input variables
     *
     * @return the result of the evaluation
     */
    @Deprecated
    DMNResult evaluateDecisionByName(DMNModel model, String decisionName, DMNContext context );

    /**
     * Evaluate the decision identified by the given ID and
     * all dependent decisions, given the context
     *
     * @deprecated consider using {@link #evaluateById(DMNModel, DMNContext, String...)} instead
     *
     * @param model the model to evaluate
     * @param decisionId the root decision to evaluate, identified
     *                   by ID
     * @param context the context with all the input variables
     *
     * @return the result of the evaluation
     */
    @Deprecated
    DMNResult evaluateDecisionById(DMNModel model, String decisionId, DMNContext context );

    /**
     * Evaluate all decisions identified by the given names and
     * all dependent decisions, given the context
     *
     * @param model the model to evaluate
     * @param decisionNames list of root decisions to evaluate, identified
     *                     by name
     * @param context the context with all the input variables
     *
     * @return the result of the evaluation
     */
    DMNResult evaluateByName( DMNModel model, DMNContext context, String... decisionNames );

    /**
     * Evaluate all decision identified by the given IDs and
     * all dependent decisions, given the context
     *
     * @param model the model to evaluate
     * @param decisionIds list of root decisions to evaluate, identified
     *                   by ID
     * @param context the context with all the input variables
     *
     * @return the result of the evaluation
     */
    DMNResult evaluateById( DMNModel model, DMNContext context, String... decisionIds );

    /**
     * Creates a new empty DMNContext
     *
     * @return a new empty DMNContext
     */
    DMNContext newContext();

    /**
     * Returns the ClassLoader used by this DMNRuntime
     */
    ClassLoader getRootClassLoader();

    /**
     * Evaluate the Decision Service identified by the given name, given the context
     *
     * @param model the model to evaluate
     * @param decisionServiceName the Decision Service name
     * @param context the context with all the required inputs (inputData and inputDecision) required by the Decision Service
     *
     * @return the result of the evaluation
     */
    DMNResult evaluateDecisionService(DMNModel model, DMNContext context, String decisionServiceName);

}
