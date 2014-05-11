
package chatty.gui.components.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A simple panel to edit (add/remove Strings) a list.
 * 
 * @author tduva
 */
public class ListSelector extends JPanel implements ListSetting<String> {
    
    private static final Dimension BUTTON_SIZE = new Dimension(27,27);
    
    private final JList<String> list = new JList<>();
    private final DefaultListModel<String> data = new DefaultListModel<>();
    private final JButton add = new JButton();
    private final JButton remove = new JButton();
    private final JButton change = new JButton();
    private final JTextField input = new JTextField();
    
    private DataFormatter<String> formatter;

    public ListSelector() {
        
        // Button actions
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == add || e.getSource() == input) {
                    addItem();
                }
                else if (e.getSource() == remove) {
                    removeItem();
                }
                else if (e.getSource() == change) {
                    changeItem();
                }
            }
        };
        
        list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeItems");
        list.getActionMap().put("removeItems", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeItem();
            }
        });
        
        // List double-click
        list.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    changeItem();
                }
            }
            
        });
        
        // List selection changes
        list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateEditButtons();
            }
        });
        
        // Inputbox changes
        input.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateAddButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateAddButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateAddButton();
            }
        });
        
        // Buttons
        configureButton(add, "list-add.png", "Add item (or press enter in inputbox)");
        configureButton(remove, "list-remove.png", "Remove selected item");
        configureButton(change, "edit.png", "Edit selected item (or double-click on item)");
        
        // Listeners
        add.addActionListener(buttonAction);
        remove.addActionListener(buttonAction);
        input.addActionListener(buttonAction);
        change.addActionListener(buttonAction);
        
        list.setModel(data);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(input, gbc);
        
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        add(add, gbc);
        
        gbc.weightx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        add(remove, gbc);
        
        gbc.gridy = 2;
        add(change, gbc);
        
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        add(new JScrollPane(list), gbc);
        
        updateAddButton();
        updateEditButtons();
    }
    
    private void configureButton(JButton button, String icon, String tooltip) {
        button.setIcon(new ImageIcon(ListSelector.class.getResource(icon)));
        button.setToolTipText(tooltip);
        button.setPreferredSize(BUTTON_SIZE);
        button.setSize(BUTTON_SIZE);
        button.setMaximumSize(BUTTON_SIZE);
        button.setMinimumSize(BUTTON_SIZE);
    }
    
    /**
     * Add the item currently in the input box, but not if it's empty or already
     * in the list.
     */
    private void addItem() {
        String item = input.getText();
        item = format(item);
        if (item != null && !item.isEmpty() && !data.contains(item)) {
            data.addElement(item);
            input.setText("");
        }
    }
    
    /**
     * Remove selected items.
     */
    private void removeItem() {
        for (String item : list.getSelectedValuesList()) {
            data.removeElement(item);
        }
    }
    
    private void changeItem() {
        String selectedValue = list.getSelectedValue();
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex > -1) {
            String newValue = JOptionPane.showInputDialog(this, "Change value:", selectedValue);
            newValue = format(newValue);
            if (newValue != null && !newValue.isEmpty()) {
                data.set(selectedIndex, newValue);
            }
        }
    }
    
    /**
     * Checks if the current input isn't empty (after formatting) and enables
     * or diables the "add" button accordingly.
     */
    private void updateAddButton() {
        String currentInput = format(input.getText());
        boolean somethingToAdd = currentInput != null && !currentInput.isEmpty();
        add.setEnabled(somethingToAdd);
    }
    
    /**
     * Checks if an item is selected in the list and enables or disables the
     * edit buttons accordingly.
     */
    private void updateEditButtons() {
        boolean somethingIsSelected = list.getSelectedIndex() != -1;
        remove.setEnabled(somethingIsSelected);
        change.setEnabled(somethingIsSelected);
    }
    
    /**
     * Gets the current data.
     * 
     * @return 
     */
    public List<String> getData() {
        List<String> list = new ArrayList<>();
        Enumeration<String> e = data.elements();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        return list;
    }
    
    /**
     * Fills the list with the given data.
     * 
     * @param list 
     */
    public void setData(List<String> list) {
        data.clear();
        for (String item : list) {
            data.addElement(item);
        }
    }

    @Override
    public List<String> getSettingValue() {
        return getData();
    }

    @Override
    public void setSettingValue(List<String> value) {
        setData(value);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        input.setEnabled(enabled);
        list.setEnabled(enabled);
    }
    
    public void setDataFormatter(DataFormatter<String> formatter) {
        this.formatter = formatter;
    }
    
    /**
     * Formats the given text according to the set {@code DataFormatter}, or
     * just returns the input if no formatter is set.
     * 
     * @param input The text to format
     * @return The input after formatting, or the unchanged input if no
     * formatter is set (may also be {@code null}, depending on the
     * implementation of the formatter)
     */
    private String format(String input) {
        if (formatter != null) {
            return formatter.format(input);
        }
        return input;
    }
    
}
