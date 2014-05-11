
package chatty.gui.components.settings;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class HistorySettings extends SettingsPanel implements ActionListener {
    
    private final JButton clearHistory = new JButton("Clear history");
    private final SettingsDialog d;
    //private final JButton removeOld = new JButton("Remove old entries");
    
    public HistorySettings(SettingsDialog d) {
        this.d = d;

        // History group
        JPanel main = addTitledPanel("Channel History", 0);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(d.addBooleanSetting("saveChannelHistory", "Enable History", "If enabled, automatically add joined channels to the history"), gbc);
        
        JPanel days = new JPanel();
        ((FlowLayout)days.getLayout()).setVgap(0);
        days.add(d.addBooleanSetting("historyClear", "Only keep channels joined in the last ", ""));
        days.add(d.addSimpleLongSetting("channelHistoryKeepDays", 3, true));
        days.add(new JLabel("days"));
        
        gbc = d.makeGbc(0, 2, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0,20,5,5);
        main.add(days, gbc);
        
        gbc = d.makeGbc(0, 3, 1, 1);
        gbc.insets = new Insets(5, 20, 10, 5);
        main.add(new JLabel("<html><body style='width: 280px'>"
                + "Expired entries (defined as per the setting above) "
                + "are automatically deleted from the history "
                + "when you start Chatty."), gbc);
        
        gbc = d.makeGbc(0, 4, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(clearHistory, gbc);

        clearHistory.addActionListener(this);
        
        JPanel presets = addTitledPanel("Status Presets", 1);
        
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        presets.add(d.addBooleanSetting("saveStatusHistory", "Enable History",
                "If enabled, automatically add used status (title/game) to the history"), gbc);
        
        JPanel daysPresets = new JPanel();
        ((FlowLayout)daysPresets.getLayout()).setVgap(0);
        daysPresets.add(d.addBooleanSetting("statusHistoryClear",
                "Only keep entries used in the last ",
                "Whether to remove old status history entries."));
        daysPresets.add(d.addSimpleLongSetting("statusHistoryKeepDays", 3, true));
        daysPresets.add(new JLabel("days"));
        
        gbc = d.makeGbc(0, 2, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0,20,5,5);
        presets.add(daysPresets, gbc);
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == clearHistory) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Do you want to delete all history entries?",
                    "Clear history",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                d.clearHistory();
            }
        }
    }
    
}
