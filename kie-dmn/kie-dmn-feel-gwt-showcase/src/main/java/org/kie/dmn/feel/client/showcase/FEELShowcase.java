/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.dmn.feel.client.showcase;

import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.ui.nav.client.local.DefaultPage;
import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.kie.dmn.feel.lang.ast.ASTNode;
import org.kie.dmn.feel.lang.ast.BaseNode;
import org.kie.dmn.feel.lang.ast.visitor.DMNDTAnalyserValueFromNodeVisitor;
import org.kie.dmn.feel.parser.feel11.ASTBuilderVisitor;
import org.kie.dmn.feel.parser.feel11.FEELParser;
import org.kie.dmn.feel.parser.feel11.FEEL_1_1Parser;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@EntryPoint
@ApplicationScoped
@Templated("#root")
@Page(role = DefaultPage.class)
public class FEELShowcase extends Composite {

    @DataField("text")
    private final TextArea text;

    @DataField("nodes")
    private final TextArea nodes;

    @DataField("evaluation")
    private final TextArea evaluation;

    @Inject
    public FEELShowcase(final TextArea text,
                        final TextArea nodes,
                        final TextArea evaluation) {
        this.text = text;
        this.nodes = nodes;
        this.evaluation = evaluation;
    }

    @PostConstruct
    public void init() {
        RootPanel.get().add(this);
    }

    @EventHandler("text")
    public void onTextChange(final KeyUpEvent e) {
        final FEEL_1_1Parser parser = FEELParser.parse(null,
                                                       text.getValue(),
                                                       Collections.emptyMap(),
                                                       Collections.emptyMap(),
                                                       Collections.emptyList(),
                                                       Collections.emptyList(),
                                                       null);
        final ParseTree tree = parser.expression();
        final ASTBuilderVisitor astBuilderVisitor = new ASTBuilderVisitor(emptyMap(), null);
        final BaseNode baseNode = astBuilderVisitor.visit(tree);

        nodes.setValue(getNodesString(baseNode));
        evaluation.setValue(getEvaluation(baseNode));
    }

    private String getEvaluation(final BaseNode baseNode) {
        try {
            return baseNode.accept(new DMNDTAnalyserValueFromNodeVisitor(emptyList())).toString();
        } catch (final Exception e) {
            return "Eval error.";
        }
    }

    private String getNodesString(final ASTNode expr) {
        final StringBuilder str = new StringBuilder();
        getNodesString(str, expr, 0);
        return str.toString();
    }

    private void getNodesString(final StringBuilder str,
                                final ASTNode expr,
                                final int level) {
        if (expr == null) {
            return;
        }

        str.append(repeat(level));
        str.append(String.join("", expr.getText().split("\n")).trim().replaceAll(" +", " "));
        str.append(": ");
        str.append(expr.getResultType().getName());
        str.append("\n");

        for (final ASTNode astNode : expr.getChildrenNode()) {
            getNodesString(str, astNode, level + 1);
        }
    }

    private String repeat(final int n) {
        final StringBuilder append = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            append.append("  ");
        }
        return append.toString();
    }
}
