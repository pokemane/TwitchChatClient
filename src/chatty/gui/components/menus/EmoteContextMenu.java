
package chatty.gui.components.menus;

import chatty.util.api.Emoticon;
import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class EmoteContextMenu extends ContextMenu {
    
    public EmoteContextMenu(Emoticon emote) {
        addItem("", emote.toString());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
    
}
