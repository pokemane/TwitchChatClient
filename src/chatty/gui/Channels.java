
package chatty.gui;

import chatty.gui.components.Channel;
import chatty.gui.components.Tabs;
import chatty.gui.components.menus.ContextMenuListener;
import java.awt.Component;
import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Managing the channel windows, providing a default channel while no other
 * is added.
 * 
 * @author tduva
 */
public class Channels {
    
    private final MainGui gui;
            
    private final Hashtable<String,Channel> channels = new Hashtable<>();
    private final Tabs tabs;
    private Channel defaultChannel;
    private final StyleManager styleManager;
    private final ContextMenuListener contextMenuListener;
    private final MouseClickedListener mouseClickedListener = new MyMouseClickedListener();
    private int defaultUserlistWidth = 140;
    
    /**
     * Save channels whose state is new highlighted messages, so the color
     * doesn't get overwritten by new messages.
     */
    private final Set<Channel> highlighted = new HashSet<>();
    
    public Channels(MainGui gui, StyleManager styleManager,
            ContextMenuListener contextMenuListener) {
        tabs = new Tabs(contextMenuListener);
        this.styleManager = styleManager;
        this.contextMenuListener = contextMenuListener;
        this.gui = gui;
        tabs.addChangeListener(new TabChangeListener());
        //tabs.setOpaque(false);
        //tabs.setBackground(new Color(0,0,0,0));
        addDefaultChannel();
    }
    
    public void addTabChangeListener(ChangeListener listener) {
        tabs.addChangeListener(listener);
    }
    
    /**
     * Set channel to show a new highlight messages has arrived, changes color
     * of the tab. ONly if not currently active tab.
     * 
     * @param channel 
     */
    public void setChannelHighlighted(Channel channel) {
        if (getActiveChannel() != channel) {
            tabs.setForegroundForComponent(channel, MainGui.COLOR_NEW_HIGHLIGHTED_MESSAGE);
            highlighted.add(channel);
        }
    }
    
    /**
     * Set channel to show a new message arrived, changes color of the tab.
     * Only if not currently active tab.
     * 
     * @param channel 
     */
    public void setChannelNewMessage(Channel channel) {
        if (getActiveChannel() != channel && !highlighted.contains(channel)) {
            tabs.setForegroundForComponent(channel, MainGui.COLOR_NEW_MESSAGE);
        }
    }
    
    /**
     * Reset state (color, title suffixes) to default.
     * 
     * @param channel 
     */
    public void resetChannelTab(Channel channel) {
        tabs.setForegroundForComponent(channel, null);
        tabs.setTitleForComponent(channel, channel.getName());
        highlighted.remove(channel);
    }
    
    /**
     * Set channel to new status available, adds a suffix to indicate that.
     * Only if not currently the active tab.
     * 
     * @param channel 
     */
    public void setChannelNewStatus(Channel channel) {
        if (getActiveChannel() != channel) {
            tabs.setTitleForComponent(channel, channel.getName()+"*");
        }
    }
    
    
    /**
     * This is the channel when no channel has been added yet.
     */
    private void addDefaultChannel() {
        defaultChannel = createChannel("");
        tabs.addTab(defaultChannel);
    }
    
    private Channel createChannel(String name) {
        Channel channel = new Channel(name,gui,styleManager, contextMenuListener, defaultUserlistWidth);
        channel.setMouseClickedListener(mouseClickedListener);
        return channel;
    }
    
    public Component getComponent() {
        return tabs;
    }

    public Enumeration keys() {
        return channels.keys();
    }
    
    public Channel get(String key) {
        return channels.get(key);
    }
    
    public Enumeration elements() {
        return channels.elements();
    }
    
    public int getChannelCount() {
        return channels.size();
    }
    
    /**
     * Check if the given channel is added.
     * 
     * @param channel
     * @return 
     */
    public boolean isChannel(String channel) {
        if (channel == null) {
            return false;
        }
        return channels.get(channel) != null;
    }
    
    /**
     * Gets the Channel object for the given channel name. If none exists, the
     * channel is automatically added.
     * 
     * @param channel
     * @return 
     */
    public Channel getChannel(String channel) {
        Channel panel = channels.get(channel);
        if (panel == null) {
            panel = addChannel(channel);
        }
        return panel;
    }
    
    public String getChannelFromPanel(Channel panel) {
        Enumeration e = channels.keys();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            if (channels.get(key) == panel) {
                return key;
            }
        }
        return null;
    }
    
    
    /**
     * Adds a channel with the given name. If the default channel is still there
     * it is used for this channel and renamed.
     * 
     * @param channelName
     * @return 
     */
    public Channel addChannel(String channelName) {
        if (channels.get(channelName) != null) {
            return null;
        }
        Channel panel;
        if (defaultChannel != null) {
            // Reuse default channel
            panel = defaultChannel;
            defaultChannel = null;
            panel.setName(channelName);
        }
        else {
            // No default channel, so create a new one
            panel = createChannel(channelName);
            tabs.addTab(panel);
            tabs.setSelectedComponent(panel);
        }
        channels.put(channelName, panel);
        return panel;
    }
    
    public void removeChannel(final String channel) {
        // TODO: Why invokeLater?
//        SwingUtilities.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//                Channel panel = channels.get(channel);
//                if (panel == null) {
//                    return;
//                }
//                channels.remove(channel);
//                tabs.removeTab(panel);
//                if (tabs.getTabCount() == 0) {
//                    addDefaultChannel();
//                    gui.updateState();
//                }
//            }
//        });
        Channel panel = channels.get(channel);
        if (panel == null) {
            return;
        }
        channels.remove(channel);
        tabs.removeTab(panel);
        if (tabs.getTabCount() == 0) {
            addDefaultChannel();
            gui.updateState();
        }
    }
    
    /**
     * Return the currently selected channel.
     * 
     * @return The Channel object which is currently selected.
     */
    public Channel getActiveChannel() {
        Component c = tabs.getSelectedComponent();
        if (c instanceof Channel) {
            return (Channel)c;
        }
        return null;
    }
    
    /**
     * Return the channel from the given input box.
     * 
     * @param input The reference to the input box.
     * @return The Channel object, or null if the given reference isn't an
     *  input box
     */
    public Channel getChannelFromInput(Object input) {
        if (defaultChannel != null && input == defaultChannel.getInput()) {
            return defaultChannel;
        }
        Enumeration e = channels.keys();
        while (e.hasMoreElements()) {
            String key = (String)e.nextElement();
            Channel value = channels.get(key);
            if (input == value.getInput()) {
                return value;
            }
        }
        return null;
    }

    private class TabChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            setInitialFocus();
            resetChannelTab(getActiveChannel());
        }
    }
    
    private class MyMouseClickedListener implements MouseClickedListener {

        @Override
        public void mouseClicked() {
            setInitialFocus();
        }
    }
    
    public List<Channel> getChannels() {
        List<Channel> result = new ArrayList<>();
        if (defaultChannel != null) {
            result.add(defaultChannel);
        }
        result.addAll(channels.values());
        return result;
    }
    
    public void setInitialFocus() {
        getActiveChannel().requestFocusInWindow();
    }
    
    public void refreshStyles() {
        for (Channel channel : getChannels()) {
            channel.refreshStyles();
        }
    }
    
    public void switchToChannel(String channel) {
        if (isChannel(channel)) {
            tabs.setSelectedComponent(get(channel));
        }
    }
    
    public void switchToNextChannel() {
        tabs.setSelectedNext();
    }
    
    public void switchToPreviousChannel() {
        tabs.setSelectedPrevious();
    }
    
    public void setDefaultUserlistWidth(int width) {
        defaultUserlistWidth = width;
        if (defaultChannel != null) {
            // Set the width of the default channel because it's created before
            // the width is loaded from the settings
            defaultChannel.setUserlistWidth(width);
        }
    }
    
    public void setTabOrder(String order) {
        int setting = Tabs.ADD_ORDER;
        switch (order) {
            case "alphabetical": setting = Tabs.ALPHABETIC_ORDER; break;
        }
        tabs.setOrder(setting);
    }
    
}
