/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.scenariosimulation.backend.expression;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.drools.scenariosimulation.api.utils.ConstantsHolder;
import org.drools.scenariosimulation.api.utils.ScenarioSimulationSharedUtils;
import org.drools.scenariosimulation.backend.util.JsonUtils;

import static org.drools.scenariosimulation.api.utils.ConstantsHolder.VALUE;

public abstract class AbstractExpressionEvaluator implements ExpressionEvaluator {

    @Override
    public Object evaluateLiteralExpression(String rawExpression, String className, List<String> genericClasses) {
        if (isStructuredInput(className)) {
            return convertResult(rawExpression, className, genericClasses);
        } else {
            return internalLiteralEvaluation(rawExpression, className);
        }
    }

    @Override
    public ExpressionEvaluatorResult evaluateUnaryExpression(String rawExpression, Object resultValue, Class<?> resultClass) {
        if (isStructuredResult(resultClass)) {
            return verifyResult(rawExpression, resultValue, resultClass);
        } else {
            return ExpressionEvaluatorResult.of(internalUnaryEvaluation(rawExpression, resultValue, resultClass, false));
        }
    }

    /**
     * Check if resultClass represents a structured result
     * @param resultClass
     * @return
     */
    protected boolean isStructuredResult(Class<?> resultClass) {
        return resultClass != null && ScenarioSimulationSharedUtils.isCollectionOrMap(resultClass.getCanonicalName());
    }

    /**
     * Check if className represents a structured input
     * @param className
     * @return
     */
    protected boolean isStructuredInput(String className) {
        return ScenarioSimulationSharedUtils.isCollectionOrMap(className);
    }

    protected Object convertResult(String rawString, String className, List<String> genericClasses) {
        if (rawString == null) {
            return null;
        }

        Optional<JsonNode> optionalJsonNode = JsonUtils.convertFromStringToJSONNode(rawString);
        JsonNode jsonNode = optionalJsonNode.orElseThrow(() -> new IllegalArgumentException(ConstantsHolder.MALFORMED_RAW_DATA_MESSAGE));

        if (jsonNode.isTextual()) {
            /* JSON Text: expression manually written by the user to build a list/map */
            return internalLiteralEvaluation(jsonNode.asText(), className);
        } else if (jsonNode.isArray()) {
            /* JSON Array: list of expressions created using List collection editor */
            return createAndFillList((ArrayNode) jsonNode, new ArrayList<>(), className, genericClasses);
        } else if (jsonNode.isObject()) {
            /* JSON Map: map of expressions created using Map collection editor */
            return createAndFillObject((ObjectNode) jsonNode,
                                       createObject(className, genericClasses),
                                       className,
                                       genericClasses);
        }
        throw new IllegalArgumentException(ConstantsHolder.MALFORMED_RAW_DATA_MESSAGE);
    }

    protected List<Object> createAndFillList(ArrayNode json, List<Object> toReturn, String className, List<String> genericClasses) {
        for (JsonNode node : json) {
            if (isSimpleTypeNode(node)) {
                String generic = genericClasses.get(genericClasses.size() - 1);
                Object value = internalLiteralEvaluation(getSimpleTypeNodeTextValue(node), generic);
                toReturn.add(value);
            } else {
                String genericClassName = ScenarioSimulationSharedUtils.isMap(className) ? className : genericClasses.get(genericClasses.size() - 1);
                Object listElement = createObject(genericClassName, genericClasses);
                Object returnedObject = createAndFillObject((ObjectNode) node, listElement, genericClassName, genericClasses);
                toReturn.add(returnedObject);
            }
        }
        return toReturn;
    }

    protected Object createAndFillObject(ObjectNode json, Object toReturn, String className, List<String> genericClasses) {
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> element = fields.next();
            String key = element.getKey();
            JsonNode jsonNode = element.getValue();

            if (isSimpleTypeNode(jsonNode)) {
                Map.Entry<String, List<String>> fieldDescriptor = getFieldClassNameAndGenerics(toReturn, key, className, genericClasses);
                setField(toReturn, key, internalLiteralEvaluation(getSimpleTypeNodeTextValue(jsonNode), fieldDescriptor.getKey()));
            } else if (jsonNode.isArray()) {
                List<Object> nestedList = new ArrayList<>();
                Map.Entry<String, List<String>> fieldDescriptor = getFieldClassNameAndGenerics(toReturn, key, className, genericClasses);
                List<Object> returnedList = createAndFillList((ArrayNode) jsonNode, nestedList, fieldDescriptor.getKey(), fieldDescriptor.getValue());
                setField(toReturn, key, returnedList);
            } else if (jsonNode.isObject()) {
                Map.Entry<String, List<String>> fieldDescriptor = getFieldClassNameAndGenerics(toReturn, key, className, genericClasses);
                Object nestedObject = createObject(fieldDescriptor.getKey(), fieldDescriptor.getValue());
                Object returnedObject = createAndFillObject((ObjectNode) jsonNode, nestedObject, fieldDescriptor.getKey(), fieldDescriptor.getValue());
                setField(toReturn, key, returnedObject);
            } else if (!isEmptyText(jsonNode)) {
                Map.Entry<String, List<String>> fieldDescriptor = getFieldClassNameAndGenerics(toReturn, key, className, genericClasses);
                setField(toReturn, key, internalLiteralEvaluation(jsonNode.textValue(), fieldDescriptor.getKey()));
            } else {
                // empty strings are skipped
            }
        }
        return toReturn;
    }

    protected ExpressionEvaluatorResult verifyResult(String rawExpression, Object resultRaw, Class<?> resultClass) {
        if (rawExpression == null) {
            return ExpressionEvaluatorResult.of(resultRaw == null);
        }
        if (resultRaw != null && !(resultRaw instanceof List) && !(resultRaw instanceof Map)) {
            throw new IllegalArgumentException("A list or map was expected");
        }
        Optional<JsonNode> optionalJsonNode = JsonUtils.convertFromStringToJSONNode(rawExpression);
        JsonNode jsonNode = optionalJsonNode.orElseThrow(() -> new IllegalArgumentException(ConstantsHolder.MALFORMED_RAW_DATA_MESSAGE));

        if (jsonNode.isTextual()) {
            /* JSON Text: expression manually written by the user to build a list/map */
            return ExpressionEvaluatorResult.of(internalUnaryEvaluation(jsonNode.asText(), resultRaw, resultClass, false));
        } else if (jsonNode.isArray()) {
            /* JSON Array: list of expressions created using List collection editor */
            return verifyList((ArrayNode) jsonNode, (List<Object>) resultRaw);
        } else if (jsonNode.isObject()) {
            /* JSON Map: map of expressions created using Map collection editor */
            return verifyObject((ObjectNode) jsonNode, resultRaw);
        }
        throw new IllegalArgumentException(ConstantsHolder.MALFORMED_RAW_DATA_MESSAGE);
    }

    protected ExpressionEvaluatorResult verifyList(ArrayNode json, List<Object> resultRaw) {
        if (resultRaw == null) {
            return ExpressionEvaluatorResult.of(isListEmpty(json));
        }
        int elementNumber = 0;
        for (JsonNode node : json) {
            elementNumber++;
            boolean success = false;
            boolean simpleTypeNode = isSimpleTypeNode(node);

            for (Object result : resultRaw) {
                if (simpleTypeNode && internalUnaryEvaluation(getSimpleTypeNodeTextValue(node), result, result.getClass(), true)) {
                    success = true;
                } else if (!simpleTypeNode && verifyObject((ObjectNode) node, result).isSuccessful()) {
                    success = true;
                }

                if (success) {
                    break;
                }
            }

            if (!success) {
                ExpressionEvaluatorResult evaluatorResult = ExpressionEvaluatorResult.ofFailed();
                if (simpleTypeNode) {
                    evaluatorResult.setWrongValue(getSimpleTypeNodeTextValue(node));
                }
                evaluatorResult.addListItemStepToPath(elementNumber);
                return evaluatorResult;
            }
        }
        return ExpressionEvaluatorResult.ofSuccessful();
    }

    protected ExpressionEvaluatorResult verifyObject(ObjectNode json, Object resultRaw) {
        if (resultRaw == null) {
            return ExpressionEvaluatorResult.of(isObjectEmpty(json));
        }
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> element = fields.next();
            String key = element.getKey();
            JsonNode jsonNode = element.getValue();
            Object fieldValue = extractFieldValue(resultRaw, key);
            Class<?> fieldClass = fieldValue != null ? fieldValue.getClass() : null;
            ExpressionEvaluatorResult evaluatorResult = ExpressionEvaluatorResult.ofFailed();

            if (isSimpleTypeNode(jsonNode)) {
                String nodeValue = getSimpleTypeNodeTextValue(jsonNode);
                if (!internalUnaryEvaluation(nodeValue, fieldValue, fieldClass, true)) {
                    evaluatorResult.setWrongValue(nodeValue);
                    evaluatorResult.addMapItemStepToPath(key);
                    return evaluatorResult;
                }
            } else if (jsonNode.isArray()) {
                evaluatorResult = verifyList((ArrayNode) jsonNode, (List) fieldValue);
                if (!evaluatorResult.isSuccessful()) {
                    evaluatorResult.addMapItemStepToPath(key);
                    return evaluatorResult;
                }
            } else if (jsonNode.isObject()) {
                evaluatorResult = verifyObject((ObjectNode) jsonNode, fieldValue);
                if (!evaluatorResult.isSuccessful()) {
                    if (resultRaw instanceof Map) {
                        evaluatorResult.addMapItemStepToPath(key);
                    } else {
                        evaluatorResult.addFieldItemStepToPath(key);
                    }
                    return evaluatorResult;
                }
            } else {
                if (!internalUnaryEvaluation(jsonNode.textValue(), fieldValue, fieldClass, true)) {
                    return ExpressionEvaluatorResult.ofFailed(jsonNode.textValue(), key);
                }
            }
        }
        return ExpressionEvaluatorResult.ofSuccessful();
    }

    /**
     * Verify if given json node has all final values as empty strings
     * @param json
     * @return
     */
    protected boolean isNodeEmpty(JsonNode json) {
        if (json.isArray()) {
            return isListEmpty((ArrayNode) json);
        } else if (json.isObject()) {
            return isObjectEmpty((ObjectNode) json);
        } else {
            return isEmptyText(json);
        }
    }

    /**
     * Verify if all elements of given json array are empty
     * @param json
     * @return
     */
    protected boolean isListEmpty(ArrayNode json) {
        for (JsonNode node : json) {
            if (!isNodeEmpty(node)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verify if all fields of given json object are empty
     * @param json
     * @return
     */
    protected boolean isObjectEmpty(ObjectNode json) {
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        while (fields.hasNext()) {
            JsonNode element = fields.next().getValue();
            if (!isNodeEmpty(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verify if given json node text is empty
     * @param jsonNode
     * @return
     */
    protected boolean isEmptyText(JsonNode jsonNode) {
        return jsonNode.textValue() == null || jsonNode.textValue().isEmpty();
    }

    /**
     * A node represent a simple type if it is an object with only one field named "value"
     * @param jsonNode
     * @return
     */
    protected boolean isSimpleTypeNode(JsonNode jsonNode) {
        if (!jsonNode.isObject()) {
            return false;
        }
        ObjectNode objectNode = (ObjectNode) jsonNode;
        int numberOfFields = objectNode.size();
        return numberOfFields == 1 && objectNode.has(VALUE);
    }

    /**
     * Return text value of a simple type node
     * @param jsonNode
     * @return
     */
    protected String getSimpleTypeNodeTextValue(JsonNode jsonNode) {
        if (!isSimpleTypeNode(jsonNode)) {
            throw new IllegalArgumentException("Parameter does not contains a simple type");
        }
        return jsonNode.get(VALUE).textValue();
    }

    protected abstract boolean internalUnaryEvaluation(String rawExpression, Object resultValue, Class<?> resultClass, boolean skipEmptyString);

    protected abstract Object internalLiteralEvaluation(String raw, String className);

    protected abstract  Object extractFieldValue(Object result, String fieldName);

    protected abstract Object createObject(String className, List<String> genericClasses);

    protected abstract void setField(Object toReturn, String fieldName, Object fieldValue);

    /**
     * Return a pair with field className as key and list of generics as value
     * @param element : instance to be populated
     * @param fieldName : field to analyze
     * @param className : canonical class name of instance
     * @param genericClasses : list of generics related to this field
     * @return
     */
    protected abstract Map.Entry<String, List<String>> getFieldClassNameAndGenerics(Object element, String fieldName, String className, List<String> genericClasses);
}
