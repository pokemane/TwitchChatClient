
package chatty.gui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Adds a tray icon, so messages can be displayed there.
 * 
 * @author tduva
 */
public class TrayIconManager {
    
    private static final Logger LOGGER = Logger.getLogger(TrayIconManager.class.getName());
    
    private TrayIcon trayIcon;
    private PopupMenu popup;
    
    public TrayIconManager(Image image) {
        trayIcon = null;
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            
            popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.setActionCommand("exit");
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "Chatty");

            try {
                tray.add(trayIcon);
            } catch (AWTException ex) {
                LOGGER.warning("Error adding tray icon: "+ex.getLocalizedMessage());
                return;
            }
            
            trayIcon.setPopupMenu(popup);
        }
        
    }
    
    /**
     * Displays a tray icon info message.
     * 
     * @param title The title to use
     * @param message The main message text to use
     */
    public void displayInfo(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
    
    public void addActionListener(ActionListener listener) {
        if (trayIcon != null) {
            trayIcon.addActionListener(listener);
            popup.addActionListener(listener);
        }
    }
    
}
