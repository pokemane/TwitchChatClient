
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.MainGui;
import chatty.util.DateTime;
import chatty.util.MapUtil;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author tduva
 */
public class FavoritesDialog extends JDialog {
    
    private final JTable table;
    private final MyTableModel data;
    
    private final JTextField input = new JTextField(30);
    
    private final JButton addToFavoritesButton = new JButton("Add to favorites");
    private final JButton removeFromFavoritesButton = new JButton("Remove selected from favorites");
    private final JButton removeButton = new JButton("Remove selected");
    private final JButton doneButton = new JButton("Use chosen channels");
    private final JButton cancelButton = new JButton("Cancel");
    
    private String doneButtonText = "";
    private String doneButtonTextOneChannel = "";
    
    public static final int ACTION_CANCEL = 0;
    public static final int ACTION_DONE = 1;
    public static final int BUTTON_ADD_FAVORITES = 2;
    public static final int BUTTON_REMOVE_FAVORITES = 3;
    public static final int BUTTON_REMOVE = 4;
    
    private static final int FAV_COLUMN_WIDTH = 50;
    private static final int TIME_COLUMN_WIDTH = 100;
    
    private int result = -1;
    
    public FavoritesDialog(MainGui main) {
        super(main);
        setTitle("Favorites / History");
        setModal(true);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        
        input.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                channelsChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                channelsChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                channelsChanged();
            }
        });
        
        data = new MyTableModel();
        table = new JTable();
        table.setModel(data);
        table.setDefaultRenderer(Boolean.class, new TestRenderer());
        table.setDefaultRenderer(Long.class, new TimeRenderer());
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                selectionChanged();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doneButton.doClick();
                }
            }
        
        });
        
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_J, 0), "done");
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "done");
        table.getActionMap().put("done", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doneButton.doClick();
            }
        });
        
        TableColumn favColumn = table.getColumnModel().getColumn(0);
        favColumn.setMaxWidth(FAV_COLUMN_WIDTH);
        favColumn.setMinWidth(FAV_COLUMN_WIDTH);
        TableColumn timeColumn = table.getColumnModel().getColumn(2);
        timeColumn.setMaxWidth(TIME_COLUMN_WIDTH);
        timeColumn.setMinWidth(TIME_COLUMN_WIDTH);
        
        //table.setAutoCreateRowSorter(true);
        
        gbc = makeGbc(0,0,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(input, gbc);
        
        gbc = makeGbc(2,0,1,1);
        add(addToFavoritesButton, gbc);
        
        gbc = makeGbc(0,2,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        add(new JScrollPane(table), gbc);
        
        gbc = makeGbc(0,3,1,1);
        add(removeFromFavoritesButton, gbc);
        
        gbc = makeGbc(1,3,1,1);
        add(removeButton, gbc);

        doneButton.setMnemonic(KeyEvent.VK_J);
        gbc = makeGbc(0,4,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(doneButton, gbc);
        
        cancelButton.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(2,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cancelButton, gbc);
        
        table.setShowGrid(false);
        
        ActionListener listener = new FavoritesActionListener();
        
        removeFromFavoritesButton.setToolTipText("Remove selected channel(s) "
                + "from favorites only");
        removeButton.setToolTipText("Remove selected channel(s) from favorites "
                + "and history");
        
        ActionListener actionListener = main.getActionListener();
        addToFavoritesButton.addActionListener(actionListener);
        cancelButton.addActionListener(listener);
        doneButton.addActionListener(listener);
        removeFromFavoritesButton.addActionListener(actionListener);
        removeButton.addActionListener(actionListener);
        input.addActionListener(actionListener);
        
        channelsChanged();
        
        pack();
        setMinimumSize(getSize());
    }
    
    /**
     * Shows the dialog with the given channel as preset and the given action
     * as button description.
     * 
     * @param channelPreset
     * @param action The description of the done-button
     * @param actionOneChannel The secondary description, to be used for one channel
     * @return 
     */
    public int showDialog(String channelPreset, String action, String actionOneChannel) {
        doneButton.setText(action);
        doneButtonText = action;
        doneButtonTextOneChannel = actionOneChannel;
        setChannel(channelPreset);
        result = -1;
        updateEditButtons();
        setVisible(true);
        return result;
    }
    
    public int showDialog(String channelPreset, String action) {
        return showDialog(channelPreset, action, null);
    }
    

    /**
     * Gets the action associated with this Object (should be JButton).
     * 
     * @param source
     * @return 
     */
    public int getAction(Object source) {
        if (source == addToFavoritesButton || source == input) {
            return BUTTON_ADD_FAVORITES;
        }
        else if (source == removeFromFavoritesButton) {
            return BUTTON_REMOVE_FAVORITES;
        }
        else if (source == cancelButton) {
            return ACTION_CANCEL;
        }
        else if (source == doneButton) {
            return ACTION_DONE;
        }
        else if (source == removeButton) {
            return BUTTON_REMOVE;
        }
        return -1;
    }
    
    /**
     * Sets the chosen channel(s) to the input box.
     * 
     * @param channel 
     */
    public void setChannel(String channel) {
        if (channel != null) {
            input.setText(channel);
        }
    }
    
    /**
     * When the chosen channels changed. Update the done button.
     */
    private void channelsChanged() {
        boolean channelsPresent = !getChannels().isEmpty();
        doneButton.setEnabled(channelsPresent);
        int count = getChannels().size();
        if (count == 1 && doneButtonTextOneChannel != null) {
            doneButton.setText(doneButtonTextOneChannel);
        } else {
            doneButton.setText(doneButtonText);
        }
    }
    
    /**
     * Sets input to the currently selected channels.
     */
    private void selectionChanged() {
        updateChosenFromSelected();
        updateEditButtons();
    }
    
    private void updateChosenFromSelected() {
        String selectedChannels = Helper.buildStreamsString(getSelectedChannels());
        input.setText(selectedChannels);
    }
    
    private void updateEditButtons() {
        boolean selected = !getSelectedChannels().isEmpty();
        removeButton.setEnabled(selected);
        removeFromFavoritesButton.setEnabled(selected);
    }
    
    public Set<String> getSelectedChannels() {
        Set<String> selectedChannels = new HashSet<>();
        int[] selected = table.getSelectedRows();
        for (int i=0;i<selected.length;i++) {
            int row = selected[i];
            Object channel = table.getValueAt(row, 1);
            if (channel != null) {
                selectedChannels.add((String)channel);
            }
        }
        return selectedChannels;
    }
    
    /**
     * Gets the current chosen channels. This means the channels in the input
     * box, either entered manually or preset by the caller or by selecting
     * channels in the list.
     * 
     * @return 
     */
    public Set<String> getChannels() {
        String channels = input.getText();
        String[] chans = Helper.parseChannels(channels, false);
        Set<String> result = new HashSet<>();
        for (String channel : chans) {
            result.add(channel);
        }
        return result;
    }
    
    public void setData(Set<String> favorites, Map<String, Long> history) {
        Map<String, Long> favoritesWithHistory = new HashMap<>();
        for (String channel : favorites) {
            favoritesWithHistory.put(channel, history.get(channel));
        }
        favoritesWithHistory = MapUtil.sortByValue(favoritesWithHistory);
        history = MapUtil.sortByValue(history);
        data.setData(favoritesWithHistory, history);
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(5,5,5,5);
        return gbc;
    }
    
    static class TestRenderer extends DefaultTableCellRenderer {
        
        ImageIcon icon = new ImageIcon(getClass().getResource("/chatty/gui/star.png"));
        
        @Override
        public void setValue(Object value) {
            if (value instanceof Boolean) {
                if ((Boolean)value) {
                    this.setIcon(icon);
                }
                else {
                    this.setIcon(null);
                }
                JLabel label = (JLabel)this;
                label.setHorizontalAlignment(JLabel.CENTER);
                //this.setText(value.toString());
            }
        }
        
    }
    
    static class TimeRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            long time = (Long)value;
            if (time == -1) {
                setText("-");
            }
            else {
                setText(DateTime.ago(time));
            }
            JLabel label = (JLabel)this;
            label.setHorizontalAlignment(JLabel.CENTER);
        }
        
    }
    
    class MyTableModel extends AbstractTableModel {

        Object[][] data = {
            {true,"joshimuz",new Integer(1000)},
            {true,"ninkortek",new Integer(1234)},
            {false,"abc",new Integer(1)}
        };
        private String[] columns = {"Fav","Channel","Last joined"};
        
        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data[rowIndex][columnIndex];
        }
        
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        
        @Override
        public String getColumnName(int col) {
            return columns[col];
        }
        
        
        public void setData(Map<String, Long> favorites, Map<String, Long> history) {
            int count = 0;
            for (String entry : favorites.keySet()) {
                if (!history.containsKey(entry)) {
                    count++;
                }
            }
            count += history.size();
            
            data = new Object[count][columns.length];
            int i = 0;
            
            for (String entry : favorites.keySet()) {
                
                addEntry(i, true, entry, history.get(entry));
                i++;
            }
            
            for (String channel : history.keySet()) {
                Long time = history.get(channel);
                if (!favorites.containsKey(channel)) {
                    
                    addEntry(i, false, channel, time);
                    i++;
                }
            }
            fireTableDataChanged();
        }
        
        private void addEntry(int i, boolean favorite, String channel, Long time) {
            if (time == null) {
                time = -1l;
            }
            //System.out.println(i+" "+channel);
            data[i] = new Object[3];
            data[i][0] = favorite;
            data[i][1] = channel;
            data[i][2] = time;
        }
    }
    
    private class FavoritesActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == doneButton) {
                result = ACTION_DONE;
                setVisible(false);
            }
            else if (e.getSource() == cancelButton) {
                result = ACTION_CANCEL;
                setVisible(false);
            }
        }
        
    }
    
}
