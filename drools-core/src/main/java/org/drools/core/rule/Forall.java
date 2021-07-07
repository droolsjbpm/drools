/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.rule;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The forall conditional element.
 */
public class Forall extends ConditionalElement {

    private static final long serialVersionUID = 510l;

    // forall base pattern
    private Pattern            basePattern;
    // foral remaining patterns
    private List<Pattern>      remainingPatterns;
    
    private boolean           emptyBetaConstraints;

    public Forall() {
        this( null,
              new ArrayList( 1 ) );
    }

    public Forall(final Pattern basePattern) {
        this( basePattern,
              new ArrayList( 1 ) );
    }

    public Forall(final Pattern basePattern,
                  final List<Pattern> remainingPatterns) {
        this.basePattern = basePattern;
        this.remainingPatterns = remainingPatterns;
        this.emptyBetaConstraints = false;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        basePattern = (Pattern)in.readObject();
        remainingPatterns = (List<Pattern>)in.readObject();
        emptyBetaConstraints = in.readBoolean();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(basePattern);
        out.writeObject(remainingPatterns);
        out.writeBoolean( emptyBetaConstraints );
    }
    /* (non-Javadoc)
     * @see org.kie.rule.ConditionalElement#clone()
     */
    public Forall clone() {
        List<Pattern> clonedPatterns = new ArrayList<>();
        for (Pattern remainingPattern : remainingPatterns) {
            clonedPatterns.add(remainingPattern.clone());
        }
        return new Forall( basePattern.clone(), clonedPatterns );
    }

    /**
     * Forall inner declarations are only provided by the base patterns
     * since it negates the remaining patterns
     */
    public Map<String, Declaration> getInnerDeclarations() {
        final Map inner = new HashMap( this.basePattern.getOuterDeclarations() );
        for ( Pattern pattern : remainingPatterns ) {
            inner.putAll( pattern.getOuterDeclarations() );
        }
        return inner;
    }

    /**
     * Forall does not export any declarations
     */
    public Map<String, Declaration> getOuterDeclarations() {
        return Collections.EMPTY_MAP;
    }

    /**
     * Forall can only resolve declarations from its base pattern
     */
    public Declaration resolveDeclaration(final String identifier) {
        return this.getInnerDeclarations().get( identifier );
    }

    /**
     * @return the basePattern
     */
    public Pattern getBasePattern() {
        return this.basePattern;
    }

    /**
     * @param basePattern the basePattern to set
     */
    public void setBasePattern(final Pattern basePattern) {
        this.basePattern = basePattern;
    }

    /**
     * @return the remainingPatterns
     */
    public List<Pattern> getRemainingPatterns() {
        return this.remainingPatterns;
    }

    /**
     * @param remainingPatterns the remainingPatterns to set
     */
    public void setRemainingPatterns(final List<Pattern> remainingPatterns) {
        this.remainingPatterns = remainingPatterns;
    }

    /**
     * Adds one more pattern to the list of remaining patterns
     */
    public void addRemainingPattern(final Pattern pattern) {
        this.remainingPatterns.add( pattern );
    }

    public List<Pattern> getNestedElements() {
        List<Pattern> elements = new ArrayList<Pattern>( 1 + this.remainingPatterns.size() );
        elements.add( this.basePattern );
        elements.addAll( this.remainingPatterns );
        return elements;
    }

    public boolean isPatternScopeDelimiter() {
        return true;
    }    
    
    public boolean isEmptyBetaConstraints() {
        return emptyBetaConstraints;
    }

    public void setEmptyBetaConstraints(boolean emptyBetaConstraints) {
        this.emptyBetaConstraints = emptyBetaConstraints;
    }

    @Override
    public String toString() {
        return "forall( "+basePattern+" "+remainingPatterns+" )";
    }

}
