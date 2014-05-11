
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ImageSettings extends SettingsPanel {
    
    public ImageSettings(SettingsDialog d) {
        
        JPanel main = addTitledPanel("Global", 0);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        main.add(
                d.addBooleanSetting("emoticonsEnabled", "Show emoticons",
                        "Whether to show emotes as icons.\n"
                        + "Changing this only affects new lines."),
                gbc);
        
        
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        main.add(d.addBooleanSetting("usericonsEnabled","Show user icons",
                "Show mod/turbo/.. as icons. Changing this only affects new lines."),
                gbc);
        
        JPanel ffz = addTitledPanel("FrankerFaceZ", 1);
        
        gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        ffz.add(d.addBooleanSetting("ffz","Enable FrankerFaceZ",
                "Retrieve custom emotes and possibly mod icon."),
                gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        ffz.add(d.addBooleanSetting("ffzModIcon","Enable Mod Icon",
                "Show custom mod icon for some channels."),
                gbc);
        
        gbc = d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST);
        ffz.add(new JLabel("These settings require a restart of Chatty to "
                + "have full effect."),
                gbc);
        
    }
    
}
