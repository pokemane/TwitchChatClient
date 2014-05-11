
package chatty.gui.components;

import chatty.gui.MainGui;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * Dialog to search text in the chat.
 * 
 * @author tduva
 */
public class SearchDialog extends JDialog {
    
    private static final Color COLOR_NORMAL = Color.WHITE;
    private static final Color COLOR_NO_RESULT = new Color(255,165,80);
    
    private static final int NO_RESULT_COLOR_TIME = 1000;
    
    private final Timer timer;
    private final JTextField searchText = new JTextField(20);
    private final JButton searchButton = new JButton("Search");
    //private final JCheckBox highlightAll = new JCheckBox("Highlight all occurences");
    
    public SearchDialog(final MainGui g) {
        super(g);
        setTitle("Search");
        setResizable(false);
        setLayout(new GridBagLayout());
        
        timer = new Timer(NO_RESULT_COLOR_TIME, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                searchText.setBackground(COLOR_NORMAL);
            }
        });
        timer.setRepeats(false);
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!g.search(searchText.getText())) {
                    searchText.setBackground(COLOR_NO_RESULT);
                    timer.restart();
                }
            }
        };
        searchText.addActionListener(listener);
        searchButton.addActionListener(listener);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        
        add(searchText, gbc);
        gbc.gridx = 1;
        add(searchButton, gbc);
        //gbc.gridx = 0;
        //gbc.gridy = 1;
        //gbc.gridwidth = 2;
        //gbc.insets = new Insets(0,4,4,4);
        //add(highlightAll, gbc);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                g.resetSearch();
                searchText.setText(null);
                searchText.setBackground(COLOR_NORMAL);
            }
        });
        
        pack();
    }
    
}
