
package chatty.gui.components.menus;

import chatty.User;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

/**
 * The Context Menu for a single User. E.g. opened when right-clicking on a user
 * in chat, on the userlist or in the user info dialog.
 * 
 * @author tduva
 */
public class UserContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    private final User user;
    
    public UserContextMenu(User user, ContextMenuListener listener) {
        this.listener = listener;
        this.user = user;
        
        addItem("userinfo", "User: "+user.getNick());
        addSeparator();
        addItem("stream", "Normal","Twitch Stream");
        addItem("streamPopout", "Popout","Twitch Stream");
        addItem("profile","Twitch Profile");
        addSeparator();
        addItem("joinChannel","Join #"+user.getNick());
        addSeparator();
        addItem("setColor", "Set color");
        
        // Get the preset categories from the addressbook, which may be empty
        // if not addressbook is set to this user
        List<String> presetCategories = user.getPresetCategories();
        if (presetCategories != null) {
            final String submenu = "Addressbook";
            
            // Get this user's categories. If this is null, then the user isn't
            // in the addressbook
            Set<String> userCategories = user.getCategories();
            
            // Add all preset categories and select them if the user has them
            for (String presetCategory : presetCategories) {
                boolean selected = userCategories != null
                        ? userCategories.contains(presetCategory) : false;
                addCheckboxItem("cat" + presetCategory, presetCategory,
                        submenu, selected);
            }
            
            // Add seperator only if any preset categories exist
            if (!presetCategories.isEmpty()) {
                addSeparator(submenu);
            }
            
            // Add "add" or "edit" buttons depending on whether the user is
            // already in the addressbook
            if (userCategories != null) {
                addItem("addressbookEdit", "Edit", submenu);
                addItem("addressbookRemove", "Remove", submenu);
            } else {
                addItem("addressbookEdit", "Add", submenu);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.userMenuItemClicked(e, user);
        }
    }
}