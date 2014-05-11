
package chatty.gui.components.menus;

import chatty.User;
import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 *
 * @author tduva
 */
public interface ContextMenuListener {
    public void userMenuItemClicked(ActionEvent e, User user);
    public void urlMenuItemClicked(ActionEvent e, String url);
    public void menuItemClicked(ActionEvent e);
    public void streamsMenuItemClicked(ActionEvent e, List<StreamInfo> streams);
}
