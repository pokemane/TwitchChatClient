
package chatty.gui.components.settings;

import chatty.Chatty;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * More settings..
 * 
 * @author tduva
 */
public class OtherSettings extends SettingsPanel {
    
    private static final String INFO = "<html><body style='width:300px'>"
            + "The following settings disable Hardware Acceleration for Chatty, which "
            + "may resolve display issues. You have to restart Chatty for "
            + "changes to take effect.";
    
    public OtherSettings(SettingsDialog d) {
        
        JPanel graphics = addTitledPanel("Graphic Settings", 0);
        JPanel other = addTitledPanel("Other", 1);
        
        GridBagConstraints gbc;

        // Graphics settings
        gbc = d.makeGbc(0, 0, 2, 1);
        graphics.add(new JLabel(INFO), gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1);
        graphics.add(d.addBooleanSetting("nod3d", "Disable Direct3D", ""), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1);
        graphics.add(d.addBooleanSetting("noddraw", "Disable DirectDraw", ""), gbc);
        
        // Other settings
        gbc = d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(4,5,0,5);
        other.add(d.addBooleanSetting("autoScroll", "Scroll down after 30s of inactiviy",
                "After 30s of not changing position of the scrollbar, automatically scroll down."), gbc);
 
        gbc = d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST);
        other.add(new JLabel("Chat buffer size:"), gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST);
        other.add(d.addSimpleLongSetting("bufferSize", 3, true), gbc);
        
        gbc = d.makeGbc(2, 2, 1, 1, GridBagConstraints.WEST);
        other.add(new JLabel("(too high values can lower performance)"), gbc);



        gbc = d.makeGbc(0, 4, 3, 1, GridBagConstraints.WEST);
        JCheckBox versionCheck = d.addBooleanSetting("checkNewVersion", "Inform me about new versions",
                "Automatically check for a new version every few days and output a message "
                + "if a new one is available.");
        other.add(versionCheck, gbc);
        if (!Chatty.VERSION_CHECK_ENABLED) {
            versionCheck.setEnabled(false);
            versionCheck.setToolTipText("Feature disabled in this distributed version.");
        }
        
        gbc = d.makeGbc(0, 5, 1, 1, GridBagConstraints.WEST);
        other.add(new JLabel("Timeout buttons:"), gbc);
        
        gbc = d.makeGbc(1, 5, 2, 1, GridBagConstraints.WEST);
        other.add(d.addSimpleStringSetting("timeoutButtons", 20, true), gbc);
    }
    
}
