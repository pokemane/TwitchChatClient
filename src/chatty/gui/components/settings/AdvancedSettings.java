
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Settings that should only be changed if you know what you're doing, includes
 * a warning about that.
 * 
 * @author tduva
 */
public class AdvancedSettings extends SettingsPanel {
    
    public AdvancedSettings(SettingsDialog d) {

        JPanel warning = new JPanel();
        
        warning.add(new JLabel("<html><body style='width:300px'>"
                + "These settings can break Chatty if you change them, "
                + "so you should only change these settings if you "
                + "know what you are doing."));
        
        addPanel(warning, getGbc(0));
        
        JPanel connection = addTitledPanel("Connection", 1);

        connection.add(new JLabel("Server:"), d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("serverDefault", 20, true), d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("Port:"), d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("portDefault", 10, true), d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("(These might be overridden by commandline parameters.)"), d.makeGbc(0, 2, 2, 1));
        
        JPanel other = addTitledPanel("Other", 2);
        
        other.add(d.addBooleanSetting("ignoreJoinsParts", "Ignore joins/parts", "Only users who talked appear in the userlist, no joins/parts shown."),
            d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        other.add(d.addBooleanSetting("tc3", "Chat Client Version 3", "Experimental, read help (requires you to reconnect for changes to take effect)"),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
    }
    
}
