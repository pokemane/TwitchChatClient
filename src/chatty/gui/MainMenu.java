
package chatty.gui;

import chatty.Chatty;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * The main menu of the application (actually several menus in a MenuBar).
 * 
 * @author tduva
 */
public class MainMenu extends JMenuBar {
    
    private final JMenu main = new JMenu("Main");
    private final JMenu view = new JMenu("View");
    private final JMenu channels = new JMenu("Channels");
    private final JMenu extra = new JMenu("Expert");
    private final JMenu help = new JMenu("Help");
    
    private final ItemListener itemListener;
    private final ActionListener actionListener;
    
    /**
     * Stores all the menu items associated with a key
     */
    private HashMap<String,JMenuItem> menuItems = new HashMap<>();
    
    public MainMenu(ActionListener actionListener, ItemListener itemListener) {
        this.itemListener = itemListener;
        this.actionListener = actionListener;
        
        //this.setBackground(Color.black);
        //this.setForeground(Color.white);
        
        main.addActionListener(actionListener);
        view.addActionListener(actionListener);
        channels.addActionListener(actionListener);
        extra.addActionListener(actionListener);
        help.addActionListener(actionListener);
        
        main.setMnemonic(KeyEvent.VK_M);
        view.setMnemonic(KeyEvent.VK_V);
        channels.setMnemonic(KeyEvent.VK_C);
        help.setMnemonic(KeyEvent.VK_H);
        
        // Main
        addItem(main,"connect","Connect");
        addItem(main,"disconnect","Disconnect").setEnabled(false);
        main.addSeparator();
        setIcon(addItem(main,"settings","Settings", KeyEvent.VK_S), "preferences-system.png");
        main.addSeparator();
        addItem(main,"exit","Exit");
        
        // View
        addCheckboxItem(view,"ontop","Always on top");
        addCheckboxItem(view,"showJoinsParts","Show joins/parts");
        view.addSeparator();
        addItem(view,"channelInfoDialog","Channel Info", KeyEvent.VK_I);
        addItem(view,"channelAdminDialog","Channel Admin", KeyEvent.VK_A);
        view.addSeparator();
        addItem(view,"highlightedMessages","Highlights", KeyEvent.VK_H);
        JMenuItem searchMenuItem = addItem(view,"search","Find text..", KeyEvent.VK_S);
        searchMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl F"));
        

        // Channels
        addItem(channels,"favoritesDialog","Favorites/History", KeyEvent.VK_F);
        JMenuItem onlineChannels = addItem(channels, "onlineChannels", "Live Channels", KeyEvent.VK_L);
        onlineChannels.setAccelerator(KeyStroke.getKeyStroke("ctrl L"));
        addItem(channels,"addressbook", "Addressbook");
        channels.addSeparator();
        addItem(channels, "joinChannel", "Join Channel", KeyEvent.VK_J);
        
        // Extra
        addItem(extra,"debug","Debug window");
        //addCheckboxItem(extra,"verboseDebug","Verbose debug");
//        JMenu debugOptions = new JMenu("Options");
//        addCheckboxItem(debugOptions,"ignoreJoinsParts","Ignore joins/parts");
//        extra.add(debugOptions);
        if (Chatty.DEBUG) {
            addItem(extra,"unhandledException", "Unhandled Exception");
        }

        // Help
        addItem(help,"website","Website");
        JMenuItem helpItem = addItem(help,"about","About/Help", KeyEvent.VK_H);
        helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        setIcon(helpItem, "help-browser.png");
        
        
        add(main);
        add(view);
        add(channels);
        add(extra);
        add(help);
    }
    

    /**
     * Adds a MenuItem to a menu.
     * @param menu The Menu to which the item is added
     * @param key The key this item is associated with
     * @param label The text of the item
     * @return The created MenuItem
     */
    public final JMenuItem addItem(JMenu menu, String key, String label, int mnemonic) {
        JMenuItem item = new JMenuItem(label);
        if (mnemonic != -1) {
            item.setMnemonic(mnemonic);
        }
        menuItems.put(key,item);
        item.setActionCommand(key);
        menu.add(item);
        item.addActionListener(actionListener);
        return item;
    }
    
    public final JMenuItem addItem(JMenu menu, String key, String label) {
        return addItem(menu, key, label, -1);
    }
    
    /**
     * Adds a CheckboxMenuItem to a menu.
     * @param menu The Menu to which the item is added
     * @param key The key this item is associated with (the setting)
     * @param label The text of the item
     * @return The created MenuItem
     */
    public final JMenuItem addCheckboxItem(JMenu menu, String key, String label) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        menuItems.put(key, item);
        item.setActionCommand(key);
        menu.add(item);
        item.addItemListener(itemListener);
        return item;
    }
    
    /**
     * Gets the MenuItem for the given setting name.
     * @param key
     * @return 
     */
    public JMenuItem getMenuItem(String key) {
        return menuItems.get(key);
    }
    
    /**
     * Gets the setting name for the given menu item.
     * @param item
     * @return 
     */
    public String getSettingByMenuItem(Object item) {
        Iterator<Entry<String,JMenuItem>> it = menuItems.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String,JMenuItem> entry = it.next();
            if (entry.getValue() == item) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Sets the state (selected/unselected) of the CheckboxMenuItem associated
     * with the given setting.
     * @param setting
     * @param state 
     */
    public void setItemState(String setting, boolean state) {
        JMenuItem item = getMenuItem(setting);
        if (item != null && item instanceof JCheckBoxMenuItem) {
            ((JCheckBoxMenuItem)item).setState(state);
        }
    }
    
    private void setIcon(JMenuItem item, String name) {
        item.setIcon(new ImageIcon(MainMenu.class.getResource(name)));
    }
    
}
