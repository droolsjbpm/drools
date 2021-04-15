package org.kie.dmn.api.core;

import java.util.List;

/**
 * An interface for message container objects like
 * DMNResults and DMNModel
 */
public interface DMNMessageContainer {
    /**
     * Returns a list of all the messages produced
     * during the DMN service invocation.
     *
     * @return list of messages
     */
    List<DMNMessage> getMessages();

    /**
     * Returns a list of all the messages produced
     * during the DMN service invocation, filtered
     * by the list of severities given.
     *
     * @param sevs the list of severities to filter
     *             the messages by
     *
     * @return filtered list of messages
     */
    List<DMNMessage> getMessages(DMNMessage.Severity... sevs);

    /**
     * A helper method to quick check for the presence
     * of error messages. The actual error messages can
     * be retrieved by invoking <code>#getMessages()</code>
     *
     * @return true if there are any error messages,
     *         false otherwise.
     */
    boolean hasErrors();
}
