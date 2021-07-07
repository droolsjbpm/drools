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

package org.drools.core.util.debug;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.drools.core.common.NetworkNode;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.core.impl.StatefulKnowledgeSessionImpl;

public class StatefulKnowledgeSessionInfo {
    private StatefulKnowledgeSessionImpl session;
    private Map<NetworkNode, DefaultNodeInfo> nodesInfo;
    private List<String>                      log;

    private SimpleDateFormat                  df;

    public StatefulKnowledgeSessionInfo() {
        nodesInfo = new TreeMap<NetworkNode, DefaultNodeInfo>( new Comparator<NetworkNode>() {
            public int compare(NetworkNode o1,
                               NetworkNode o2) {
                return o1.getId() - o2.getId();
            }
        } );
        log = new ArrayList<String>();
        df = new SimpleDateFormat( "HH:mm:ss.SSS" );
    }

    public InternalKnowledgePackage[] getPackages() {
        return ((KnowledgeBaseImpl) this.session.getKnowledgeBase()).getPackages();
    }

    public int getNodeCount() {
        return ((KnowledgeBaseImpl) this.session.getKnowledgeBase()).getNodeCount();
    }

    public int getExternalFactCount() {
        return this.session.getObjectStore().size();
    }

    public void assign(NetworkNode snode,
                       RuleImpl rule) {
        DefaultNodeInfo info = this.nodesInfo.get( snode );
        info.assign( rule );
    }

    public boolean visited(NetworkNode node) {
        return this.nodesInfo.containsKey( node );
    }

    public DefaultNodeInfo getNodeInfo(NetworkNode node) {
        return this.nodesInfo.get( node );
    }

    public void addNodeInfo(NetworkNode node,
                            DefaultNodeInfo dni) {
        this.nodesInfo.put( node,
                            dni );
    }

    public void info(String msg) {
        log( msg,
             " [INFO] - " );
    }

    public void warn(String msg) {
        log( msg,
             " [WARN] - " );
    }

    public void error(String msg) {
        log( msg,
             " [ERRO] - " );
    }

    private void log(String msg,
                     String lvl) {
        log.add( df.format( System.currentTimeMillis() ) + lvl + msg );
    }

    public List<String> getLog() {
        return log;
    }

    public Collection< ? extends NodeInfo> getNodeInfos() {
        return this.nodesInfo.values();
    }

    public StatefulKnowledgeSessionImpl getSession() {
        return session;
    }

    public void setSession(StatefulKnowledgeSessionImpl session) {
        this.session = session;
    }

}
