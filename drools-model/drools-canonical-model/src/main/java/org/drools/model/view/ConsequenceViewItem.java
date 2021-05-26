/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.model.view;

import java.util.stream.Stream;

import org.drools.model.Condition.Type;
import org.drools.model.Consequence;
import org.drools.model.Variable;

/**
 * ViewItem to combine usual ViewItem and NamedConsequence in "and"
 * 
 */
public class ConsequenceViewItem<T> extends AbstractExprViewItem<T> {

    private final ViewItem expression;
    private final Consequence namedConsequence;

    public ConsequenceViewItem(ViewItem expression, Consequence namedConsequence) {
        super(expression.getFirstVariable());
        this.expression = expression;
        this.namedConsequence = namedConsequence;
    }

    @Override
    public Variable<?>[] getVariables() {
        return Stream.of(expression.getVariables())
                     .distinct()
                     .toArray(Variable[]::new);
    }

    @Override
    public Type getType() {
        return Type.CONSEQUENCE;
    }

    @Override
    public void setQueryExpression(boolean queryExpression) {
        super.setQueryExpression(queryExpression);
        if (expression instanceof AbstractExprViewItem) {
            ((AbstractExprViewItem) expression).setQueryExpression(queryExpression);
        }
    }

    public ViewItem getExpression() {
        return expression;
    }

    public Consequence getNamedConsequence() {
        return namedConsequence;
    }

}
