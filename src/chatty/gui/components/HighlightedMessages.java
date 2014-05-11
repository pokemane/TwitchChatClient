
package chatty.gui.components;

import chatty.User;
import chatty.gui.MainGui;
import chatty.gui.StyleServer;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HighlightsContextMenu;
import chatty.util.api.ChatIcons;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.text.MutableAttributeSet;

/**
 * Window showing all highlighted messages.
 * 
 * @author tduva
 */
public class HighlightedMessages extends JDialog {
    
    private final TextPane messages;
    private String currentChannel;
    private int currentChannelMessageCount = 0;
    private boolean setChatIconsYet = false;
    
    public HighlightedMessages(MainGui owner, StyleServer styleServer) {
        super(owner);
        setTitle("Highlighted Messages");
        
        messages = new TextPane(owner, styleServer);
        //messages.setLineWrap(true);
        //messages.setWrapStyleWord(true);
        //messages.setEditable(false);
        
        JScrollPane scroll = new JScrollPane(messages);
        messages.setScrollPane(scroll);
        
        add(scroll);
        
        setPreferredSize(new Dimension(400,300));
        
        pack();
    }
    
    public void addMessage(String channel, User user, String text, boolean action) {
        if (currentChannel == null || !currentChannel.equals(channel)
                || currentChannelMessageCount > 10) {
            messages.printLine("Highlights in " + channel + ":");
            currentChannel = channel;
            currentChannelMessageCount = 0;
        }
        currentChannelMessageCount++;
        messages.printMessage(user, text, action, false);
    }
    
    public void refreshStyles() {
        messages.refreshStyles();
    }
    
    /**
     * Set the chat icons once, so they are changed once they were loaded from
     * the API. Remove the subscriber icon because the ChannelTextPane doesn't
     * have support for several icon sets (it's only really designed to provide
     * for one single channel). Will use the default sub-star instead.
     * 
     * @param icons The icons to set.
     */
    public void setChatIcons(ChatIcons icons) {
        if (!setChatIconsYet) {
            ChatIcons newIcons = new ChatIcons(icons);
            newIcons.setSubscriberImage(null);
            messages.setChatIcons(newIcons);
            setChatIconsYet = true;
        }
    }
    
    public void setContextMenuListener(ContextMenuListener listener) {
        messages.setContextMenuListener(listener);
    }
    
    /**
     * Removes all text from the window.
     */
    public void clear() {
        messages.clear();
        currentChannel = null;
        currentChannelMessageCount = 0;
    }
    
    static class TextPane extends ChannelTextPane {
        
        public TextPane(MainGui main, StyleServer styleServer) {
            super(main, styleServer);
            linkController.setDefaultContextMenu(new HighlightsContextMenu());
        }

        public void printMessage(String channel, User user, String text, boolean action) {
            closeCompactMode();

            MutableAttributeSet style = styles.standard();
            print(getTimePrefix(), style);
            print(channel);
            printUser(user, action);
            printSpecials(text, user, style);
            printNewline();
        }
        
        public void clear() {
            setText("");
        }
        
    }
    
}
