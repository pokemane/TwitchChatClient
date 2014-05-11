
package chatty.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class GuiUtil {
    
    
    private static final String CLOSE_DIALOG_ACTION_MAP_KEY = "CLOSE_DIALOG_ACTION_MAP_KEY";
    private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    
    
    public static void installEscapeCloseOperation(final JDialog dialog) {
    Action closingAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispatchEvent(
                        new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING
                        ));
            }
        };
        
        JRootPane root = dialog.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                ESCAPE_STROKE,
                CLOSE_DIALOG_ACTION_MAP_KEY);
        root.getActionMap().put(CLOSE_DIALOG_ACTION_MAP_KEY, closingAction);
    }
    
    /**
     * Shows a JOptionPane that doesn't steal focus when opened, but is
     * focusable afterwards.
     * 
     * @param parent The parent Component
     * @param title The title
     * @param message The message
     * @param messageType The type of message as in JOptionPane
     * @param optionType The option type as in JOptionPane
     * @param options The options as in JOptionPane
     * @return The selected option or -1 if none was selected
     */
    public static int showNonAutoFocusOptionPane(Component parent, String title, String message,
            int messageType, int optionType, Object[] options) {
        JOptionPane p = new JOptionPane(message, messageType, optionType);
        p.setOptions(options);
        final JDialog d = p.createDialog(parent, title);
        d.setAutoRequestFocus(false);
        d.setFocusableWindowState(false);
        // Make focusable after showing the dialog, so that it can be focused
        // by the user, but doesn't steal focus from the user when it opens.
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                d.setFocusableWindowState(true);
            }
        });
        d.setVisible(true);
        // Find index of result
        Object value = p.getValue();
        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
