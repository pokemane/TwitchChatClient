
package chatty.gui.components;

import chatty.gui.MainGui;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

/**
 *
 * @author tduva
 */
public class TokenDialog extends JDialog {
    
    JLabel nameLabel = new JLabel("Account name:");
    JLabel name = new JLabel("<no account>");
    LinkLabel accessLabel;
    JLabel access = new JLabel("<none>");
    
    JLabel info = new JLabel("<html><body style='width:200px'>");
    JButton deleteToken = new JButton("Remove login");
    JButton requestToken = new JButton("Request login data");
    JButton verifyToken = new JButton("Verify login");
    JLabel tokenInfo = new JLabel("");
    JButton done = new JButton("Done");
    
    String currentUsername = "";
    String currentToken = "";
    
    public TokenDialog(MainGui owner) {
        super(owner,"Login configuration",true);
        this.setResizable(false);
       
        this.setLayout(new GridBagLayout());
        
        accessLabel = new LinkLabel("Access [help:login (?)]:", owner.getLinkLabelListener());
        
        add(nameLabel, makeGridBagConstraints(0,0,1,1,GridBagConstraints.WEST));
        add(name, makeGridBagConstraints(0,1,2,1,GridBagConstraints.CENTER,new Insets(0,5,5,5)));
        
        add(accessLabel, makeGridBagConstraints(0,2,1,1,GridBagConstraints.WEST));
        add(access, makeGridBagConstraints(0,3,2,1,GridBagConstraints.CENTER,new Insets(0,5,5,5)));
        
        add(tokenInfo, makeGridBagConstraints(0,4,2,1,GridBagConstraints.WEST));
        add(deleteToken, makeGridBagConstraints(0,6,1,1,GridBagConstraints.WEST));
        add(requestToken, makeGridBagConstraints(0,6,2,1,GridBagConstraints.CENTER));
        add(verifyToken, makeGridBagConstraints(1,6,1,1,GridBagConstraints.WEST));
        add(done, makeGridBagConstraints(1,7,1,1,GridBagConstraints.EAST));
        
        ActionListener actionListener = owner.getActionListener();
        requestToken.addActionListener(actionListener);
        deleteToken.addActionListener(actionListener);
        verifyToken.addActionListener(actionListener);
        done.addActionListener(actionListener);

        pack();
    }
    
    public JButton getRequestTokenButton() {
        return requestToken;
    }
    
    public JButton getDeleteTokenButton() {
        return deleteToken;
    }
    
    public JButton getVerifyTokenButton() {
        return verifyToken;
    }
    
    public JButton getDoneButton() {
        return done;
    }
    
    public void update() {
        boolean empty = currentUsername.isEmpty() || currentToken.isEmpty();
        deleteToken.setVisible(!empty);
        requestToken.setVisible(empty);
        verifyToken.setVisible(!empty);
        pack();
    }
    
    public void update(String username, String currentToken) {
        this.currentUsername = username;
        this.currentToken = currentToken;
        if (currentUsername.isEmpty() || currentToken.isEmpty()) {
            name.setText("<click below to create a login>");
        }
        else {
            name.setText(currentUsername);
        }
        setTokenInfo("");
        update();
    }
    
    public void updateAccess(boolean editor, boolean commercial, boolean user) {
        boolean empty = currentUsername.isEmpty() || currentToken.isEmpty();
        access.setVisible(!empty);
        accessLabel.setVisible(!empty);
        
        String text = "<html><body>Chat access";
        if (user) {
            text += "<br />Read user info";
        }
        if (editor) {
            text += "<br />Editor access";
        }
        if (commercial) {
            text += "<br />Run commercials";
        }
        access.setText(text);
        update();
    }
    
    public void verifyingToken() {
        setTokenInfo("Verifying login..");
        verifyToken.setEnabled(false);
    }
    
    public void tokenVerified(boolean valid, String result) {
        setTokenInfo(result);
        verifyToken.setEnabled(true);
        if (!valid) {
            access.setText("<none>");
        }
        update();
    }
    
    private void setTokenInfo(String info) {
        tokenInfo.setText("<html><body style='width:170px'>"+info);
        pack();
    }
    
    private GridBagConstraints makeGridBagConstraints(int x, int y,int w, int h, int anchor, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.insets = insets;
        constraints.anchor = anchor;
        return constraints;
    }
    
    private GridBagConstraints makeGridBagConstraints(int x, int y,int w, int h, int anchor) {
        return makeGridBagConstraints(x,y,w,h,anchor,new Insets(5,5,5,5));
        
    }
    
}
