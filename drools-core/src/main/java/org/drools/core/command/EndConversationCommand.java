package org.drools.core.command;

import org.drools.core.command.impl.ExecutableCommand;
import org.kie.api.runtime.Context;


public class EndConversationCommand<Void> implements ExecutableCommand<Void> {
    private long conversationId;

    public EndConversationCommand() {
    }

    public EndConversationCommand(long conversationId) {
        this.conversationId = conversationId;
    }

    @Override
    public Void execute(Context context) {
        RequestContextImpl         reqContext = (RequestContextImpl)context;
        ConversationContextManager cvnManager = reqContext.getConversationManager();
        cvnManager.endConversation(reqContext, conversationId);

        return (Void) null;
    }
}
