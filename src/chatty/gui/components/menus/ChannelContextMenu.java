
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;


/**
 * The default Context Menu for the Channel
 * 
 * @author tduva
 */
public class ChannelContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    
    public ChannelContextMenu(ContextMenuListener listener) {
        this.listener = listener;
        
        addItem("channelInfo", "Channel Info");
        addItem("channelAdmin", "Channel Admin");
        addSeparator();
        addItem("stream", "Normal","Twitch Stream");
        addItem("streamPopout", "Popout","Twitch Stream");
        addItem("profile", "Twitch Profile");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.menuItemClicked(e);
        }
    }
    
}
