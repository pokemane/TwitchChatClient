
package chatty.gui.components;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class ChannelsWarning extends JDialog {
    
    private static final String INFO_TEXT = "<html><body style=\"width:300px;\">"
            + "<p>Due to limitations in Twitch Chat joining more than one channel "
            + "on the same connection can cause some problems:</p>"
            + "<ul><li>Subscribers might not be displayed correctly</li>"
            + "<li>Bans/Timeouts might not be displayed in the right channel</li>"
            + "<li>Info messages such as 'This channel is now in..' might not "
            + "be displayed in the right channel</li>"
            + "</ul>"
            + "<p>[help:channels More information..]</p>";
    
    private final JButton closeAndRemind = new JButton("Remind me again");
    private final JButton dontShowAgain = new JButton("Don't show again");
    private final LinkLabel info;
    private int result = REMIND_AGAIN;
    public final static int DONT_SHOW_AGAIN = 0;
    public final static int REMIND_AGAIN = 1;
    
    private ChannelsWarning(Window parent, LinkLabelListener linkLabelListener) {
        super(parent);
        
        // Icon
        Icon icon = UIManager.getIcon("OptionPane.warningIcon");
        Image image = null;
        if (icon != null && icon instanceof ImageIcon) {
            image = ((ImageIcon)icon).getImage();
        }
        setIconImage(image);
        
        setTitle("Warning");
        setModalityType(ModalityType.APPLICATION_MODAL);
        setResizable(false);
        setLayout(new GridBagLayout());
        
        info = new LinkLabel(INFO_TEXT, linkLabelListener);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(5,10,10,10);
        gbc.gridwidth = 2;
        add(info, gbc);
        

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0.5;
        add(closeAndRemind, gbc);
        
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        add(dontShowAgain, gbc);
        
        ActionListener buttonListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == dontShowAgain) {
                    result = DONT_SHOW_AGAIN;
                }
                else if (e.getSource() == closeAndRemind) {
                    result = REMIND_AGAIN;
                }
                setVisible(false);
            }
        };
        
        closeAndRemind.addActionListener(buttonListener);
        dontShowAgain.addActionListener(buttonListener);
        
        pack();
    }
    
    private int showWarning2() {
        setVisible(true);
        return result;
    }
    
    public static int showWarning(Window parent, LinkLabelListener linkLabelListener) {
        ChannelsWarning instance = new ChannelsWarning(parent, linkLabelListener);
        instance.setLocationRelativeTo(parent);
        return instance.showWarning2();
    }
    
}
