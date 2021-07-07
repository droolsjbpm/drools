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

package org.drools.core.command.runtime;

import org.kie.api.command.ExecutableCommand;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.Context;
import org.kie.api.runtime.KieSession;
import org.kie.internal.command.RegistryContext;

public class RegisterChannelCommand
    implements
    ExecutableCommand<Void> {

    private static final long serialVersionUID = 510l;

    private String  name;
    private Channel channel;
    
    public RegisterChannelCommand() {
    }

    public RegisterChannelCommand(String name,
                                  Channel channel) {
        this.name = name;
        this.channel = channel;
    }

    public Void execute(Context context) {
        KieSession ksession = ((RegistryContext) context).lookup( KieSession.class );

        ksession.registerChannel( name,
                                  channel );

        return null;
    }

    public String toString() {
        return "reteooStatefulSession.registerChannel( " + name + ", " + channel + " );";
    }
}
