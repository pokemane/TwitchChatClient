
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class HighlightsContextMenu extends ContextMenu {

    public HighlightsContextMenu() {
        addItem("clearHighlights", "Clear");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
    
}
