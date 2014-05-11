
package chatty.gui.components.menus;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * A Popup Menu with convenience methods to add items as well as items in
 * submenus.
 * 
 * @author tduva
 */
public abstract class ContextMenu extends JPopupMenu implements ActionListener {
    
    private final Map<String, JMenu> subMenus = new HashMap<>();
    private final Set<ContextMenuListener> listeners = new HashSet<>();
    
    private JMenuItem makeItem(String action, String text) {
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(action);
        item.addActionListener(this);
        return item;
    }
    
    private JMenuItem makeCheckboxItem(String action, String text, boolean selected) {
        JMenuItem item = new JCheckBoxMenuItem(text, selected);
        item.setActionCommand(action);
        item.addActionListener(this);
        return item;
    }
    
    protected void addItem(String action, String text) {
        add(makeItem(action, text));
    }
    
    protected void addItem(String action, String text, String parent) {
        getSubmenu(parent).add(makeItem(action, text));
    }
    
    protected void addCheckboxItem(String action, String text, boolean selected) {
        add(makeCheckboxItem(action, text, selected));
    }
    
    protected void addCheckboxItem(String action, String text, String parent, boolean selected) {
        getSubmenu(parent).add(makeCheckboxItem(action, text, selected));
    }
    
    protected void addSeparator(String parent) {
        getSubmenu(parent).addSeparator();
    }
    
    public void addContextMenuListener(ContextMenuListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    protected Set<ContextMenuListener> getContextMenuListeners() {
        return listeners;
    }
    
    private JMenu getSubmenu(String name) {
        if (subMenus.get(name) == null) {
            JMenu menu = new JMenu(name);
            add(menu);
            subMenus.put(name, menu);
        }
        return subMenus.get(name);
    }
    
}