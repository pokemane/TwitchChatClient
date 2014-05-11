
package chatty.gui.components;

import chatty.Helper;
import chatty.TwitchClient;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class TokenGetDialog extends JDialog implements ItemListener, ActionListener {
    
    private static final String INFO = "<html><body>Request new login data ([help:login ?]):<br />"
            + "1. Open the link below<br />"
            + "2. Grant chat access for Chatty<br />"
            + "3. Get redirected";
    private final LinkLabel info;
    private final JTextField urlField = new JTextField(20);
    private final JLabel status = new JLabel();
    private final JButton copyUrl = new JButton("Copy URL");
    private final JButton openUrl = new JButton("Open (default browser)");
    private final JButton close = new JButton("Close");

    private final JCheckBox includeReadUserAccess = new JCheckBox("Read user info (followed streams)");
    private final JCheckBox includeEditorAccess = new JCheckBox("Editor access (edit stream title/game)");
    private final JCheckBox includeCommercialAccess = new JCheckBox("Allow running ads");
    
    private String currentUrl = TwitchClient.REQUEST_TOKEN_URL;
    
    public TokenGetDialog(MainGui owner) {
        super(owner,"Get login data",true);
        this.setResizable(false);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(owner.getWindowListener());
        
        info = new LinkLabel(INFO, owner.getLinkLabelListener());
        
        setLayout(new GridBagLayout());
        
        updateUrl();
        GridBagConstraints gbc;
        add(info,makeGridBagConstraints(0,0,2,1,GridBagConstraints.CENTER));
        
        gbc = makeGridBagConstraints(0, 1, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5,5,0,5);
        includeReadUserAccess.setSelected(true);
        includeReadUserAccess.setToolTipText("To get notified when streams you "
                + "follow go online.");
        add(includeReadUserAccess, gbc);
        gbc = makeGridBagConstraints(0, 2, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0,5,0,5);
        includeEditorAccess.setToolTipText("To be able to edit your channel's title and game.");
        add(includeEditorAccess,gbc);
        gbc = makeGridBagConstraints(0,3,2,1,GridBagConstraints.WEST);
        gbc.insets = new Insets(0,5,5,5);
        includeCommercialAccess.setToolTipText("To be able to run commercials on your stream.");
        add(includeCommercialAccess,gbc);
        gbc = makeGridBagConstraints(0,4,2,1,GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        urlField.setEditable(false);
        add(urlField, gbc);
        gbc = makeGridBagConstraints(0,5,1,1,GridBagConstraints.EAST);
        gbc.insets = new Insets(0,5,10,5);
        add(copyUrl,gbc);
        gbc = makeGridBagConstraints(1,5,1,1,GridBagConstraints.EAST);
        gbc.insets = new Insets(0,0,10,5);
        add(openUrl,gbc);
        add(status,makeGridBagConstraints(0,6,2,1,GridBagConstraints.CENTER));
        add(close,makeGridBagConstraints(1,7,1,1,GridBagConstraints.EAST));
        
        openUrl.addActionListener(this);
        copyUrl.addActionListener(this);
        close.addActionListener(owner.getActionListener());
        
        includeEditorAccess.addItemListener(this);
        includeCommercialAccess.addItemListener(this);
        includeReadUserAccess.addItemListener(this);
        
        reset();
        updateUrl();
        
        pack();
    }
    
    public JButton getCloseButton() {
        return close;
    }
    
    public void reset() {
        openUrl.setEnabled(false);
        copyUrl.setEnabled(false);
        urlField.setEnabled(false);
        setStatus("Please wait..");
    }
    
    public void ready() {
        openUrl.setEnabled(true);
        copyUrl.setEnabled(true);
        urlField.setEnabled(true);
        setStatus("Ready.");
    }
    
    public void error(String errorMessage) {
        setStatus("Error: "+errorMessage);
    }
    
    public void tokenReceived() {
        setStatus("Token received.. completing..");
    }
    
    private void setStatus(String text) {
        status.setText("<html><body style='width:150px;text-align:center'>"+text);
        pack();
    }
    
    private GridBagConstraints makeGridBagConstraints(int x, int y,int w, int h, int anchor) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.insets = new Insets(5,5,5,5);
        constraints.anchor = anchor;
        return constraints;
    }
    
    private void updateUrl() {
        String url = TwitchClient.REQUEST_TOKEN_URL;
        if (includeEditorAccess.isSelected()) {
            url += "+channel_editor";
        }
        if (includeCommercialAccess.isSelected()) {
            url += "+channel_commercial";
        }
        if (includeReadUserAccess.isSelected()) {
            url += "+user_read";
        }
        currentUrl = url;
        urlField.setText(url);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        updateUrl();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openUrl) {
            UrlOpener.openUrl(currentUrl);
        }
        else if (e.getSource() == copyUrl) {
            Helper.copyToClipboard(currentUrl);
        }
    } 
   
}
