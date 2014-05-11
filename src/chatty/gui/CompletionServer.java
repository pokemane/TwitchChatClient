
package chatty.gui;

import java.util.List;

/**
 *
 * @author tduva
 */
public interface CompletionServer {
    public List<String> getCompletionItems(String type);
}
