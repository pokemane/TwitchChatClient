
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class MainSettings extends SettingsPanel implements ActionListener {
    
    private final JButton selectFontButton = new JButton("Select font");
    private final FontChooser fontChooser;
    private final SettingsDialog d;
    
    public MainSettings(SettingsDialog d) {
        
        fontChooser = new FontChooser(d);
        this.d = d;
        
        GridBagConstraints gbc;
        
        JPanel fontSettingsPanel = addTitledPanel("Font", 0);
        JPanel otherSettingsPanel = addTitledPanel("Other", 2);
        JPanel timeoutSettingsPanel = addTitledPanel("Timeouts/Bans", 1);
        
        /*
         * Font settings (Panel)
         */
        // Font Name
        gbc = d.makeGbc(0,0,1,1);
        fontSettingsPanel.add(new JLabel("Font Name:"),gbc);
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.EAST;
        fontSettingsPanel.add(new JLabel("Font Size:"),gbc);
        
        // Font Size
        gbc = d.makeGbc(1,0,1,1);
        SimpleStringSetting fontSetting = new SimpleStringSetting(15, false);
        d.addStringSetting("font", fontSetting);
        fontSettingsPanel.add(fontSetting ,gbc);
        gbc = d.makeGbc(1,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        fontSettingsPanel.add(d.addSimpleLongSetting("fontSize",7,false),gbc);
        
        // Select Font button
        selectFontButton.addActionListener(this);
        gbc = d.makeGbc(2,0,1,1);
        fontSettingsPanel.add(selectFontButton,gbc);
        
        /*
         * Other settings (Panel)
         */
        
        // Timestamp
        gbc = d.makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(new JLabel("Timestamp: "),gbc);
        
        gbc = d.makeGbc(1,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        String[] options = new String[]{"off","[HH:mm:ss]","[HH:mm]"};
        otherSettingsPanel.add(
                d.addComboStringSetting("timestamp",15,false,options),
                gbc);
        
        gbc = d.makeGbc(2,0,2,1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(
                d.addBooleanSetting("capitalizedNames","Capitalized Names",
                "Requires a restart of Chatty to have any effect."),
                gbc);
        
        gbc = d.makeGbc(0,1,2,1);
        gbc.anchor = GridBagConstraints.WEST;
        otherSettingsPanel.add(
                d.addBooleanSetting("showModMessages","Show mod/unmod messages",
                "Whether to show when someone was modded/unmodded."),
                gbc);
        
                
        gbc = d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST);
        otherSettingsPanel.add(d.addBooleanSetting("printStreamStatus", "Show stream status in chat",
                "Output stream status when you join a channel and when it changes"), gbc);
        
        
        
        /**
         * Timeout settings
         */
        gbc = d.makeGbc(0,0,1,1);
        DeletedMessagesModeSetting deletedMessagesModeSetting = new DeletedMessagesModeSetting(d);
        timeoutSettingsPanel.add(deletedMessagesModeSetting, gbc);
        
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        timeoutSettingsPanel.add(
                d.addBooleanSetting("showBanMessages","Show ban messages",
                "If enabled, shows '<user> has been banned from talking' for bans/timeouts"),
                gbc);
        
        
        /*
         * Add to the dialog
         */
//        setLayout(new GridBagLayout());
//        gbc = d.makeGbc(0,0,2,1);
//        gbc.fill = GridBagConstraints.BOTH;
//        gbc.weightx = 0.5;
//        gbc.weighty = 0.5;
//        gbc.insets = new Insets(10, 10, 5, 10);
//        add(fontSettingsPanel,gbc);
//        gbc = d.makeGbc(0,2,2,1);
//        gbc.fill = GridBagConstraints.BOTH;
//        gbc.insets = new Insets(5, 10, 10, 10);
//        add(otherSettingsPanel,gbc);
//        gbc = d.makeGbc(0,1,2,1);
//        gbc.fill = GridBagConstraints.BOTH;
//        gbc.insets = new Insets(5, 10, 5, 10);
//        add(timeoutSettingsPanel,gbc);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectFontButton) {
            String font = d.getStringSetting("font");
            int fontSize = d.getLongSetting("fontSize").intValue();
            int result = fontChooser.showDialog(font, fontSize);
            if (result == FontChooser.ACTION_OK) {
                d.setStringSetting("font", fontChooser.getFontName());
                d.setLongSetting("fontSize", fontChooser.getFontSize().longValue());
            }
        }
    }
    
}
