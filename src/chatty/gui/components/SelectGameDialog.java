
package chatty.gui.components;

import chatty.gui.MainGui;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author tduva
 */
public class SelectGameDialog extends JDialog implements ActionListener,
        ListSelectionListener, MouseListener {

    private static final String INFO = "<html><body style='width:280px'>"
            + "The game name has to be exact so your game shows up in the right "
            + "category on Twitch, so best use the search.<br />"
            + "Enter the beginning of your game and press enter or 'Search'.";
    private final MainGui main;
    private final JTextField search = new JTextField(30);
    private final JList<String> searchResult = new JList<>();
    private final DefaultListModel<String> searchResultData = new DefaultListModel<>();
    
    private final JButton searchButton = new JButton("Search");
    
    private final JLabel searchResultInfo = new JLabel("No search performed yet.");
    
    private final JButton ok = new JButton("Ok");
    private final JButton cancel = new JButton("Cancel");
    
    private final JList<String> favorites = new JList<>();
    private final DefaultListModel<String> favoritesData = new DefaultListModel<>();
    private final JButton addToFavorites = new JButton("Add to favorites");
    private final JButton removeFromFavorites = new JButton("Remove from favorites");
    
    private String game = null;
    
    public SelectGameDialog(MainGui main) {
        super(main, "Select game", true);
        setResizable(false);
        
        this.main = main;
        
        setLayout(new GridBagLayout());
        searchResult.setModel(searchResultData);
        searchResult.setVisibleRowCount(10);
        favorites.setModel(favoritesData);
        GridBagConstraints gbc;
        
        Action doneAction = new DoneAction();
        favorites.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "useSelectedGame");
        favorites.getActionMap().put("useSelectedGame", doneAction);
        searchResult.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "useSelectedGame");
        searchResult.getActionMap().put("useSelectedGame", doneAction);
        
        gbc = makeGbc(0,0,4,1);
        add(new JLabel(INFO), gbc);
        
        gbc = makeGbc(0,1,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(search, gbc);
        
        searchButton.setPreferredSize(new Dimension(100,20));
        gbc = makeGbc(2,1,1,1);
        add(searchButton, gbc);
        
        gbc = makeGbc(0,2,4,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(searchResult), gbc);
        
        gbc = makeGbc(0,3,4,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0,4,4,4);
        add(searchResultInfo, gbc);
        
        addToFavorites.setPreferredSize(new Dimension(100,20));
        gbc = makeGbc(0,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(addToFavorites, gbc);
        
        removeFromFavorites.setPreferredSize(new Dimension(100,20));
        gbc = makeGbc(1,4,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(removeFromFavorites, gbc);
        
        favorites.setVisibleRowCount(4);
        gbc = makeGbc(0,5,4,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.5;
        add(new JScrollPane(favorites), gbc);
        
        ok.setMnemonic(KeyEvent.VK_ENTER);
        gbc = makeGbc(0,6,2,1);
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(ok, gbc);
        
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(2,6,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cancel, gbc);
        
        
        
        searchButton.addActionListener(this);
        search.addActionListener(this);
        ok.addActionListener(this);
        cancel.addActionListener(this);
        searchResult.addListSelectionListener(this);
        searchResult.addMouseListener(this);
        favorites.addListSelectionListener(this);
        favorites.addMouseListener(this);
        addToFavorites.addActionListener(this);
        removeFromFavorites.addActionListener(this);
        
        pack();
        
        setMinimumSize(getSize());
        
    }
    
    public String open(String gamePreset) {
        //System.out.println(gamePreset);
        loadFavorites();
        pack();
        search.setText(gamePreset);
        setVisible(true);
        return game;
    }
    
    public void searchResult(Set<String> games) {
        searchResultData.clear();
        searchResultInfo.setText(games.size()+" game"+(games.size() == 1 ? "" : "s")+" found");
        games = new TreeSet<>(games);
        for (String game : games) {
            searchResultData.addElement(game);
        }
        pack();
    }
    
    private void doSearch() {
        String searchString = search.getText().trim();
        if (searchString.isEmpty()) {
            searchResultInfo.setText("Enter something to search.");
            return;
        }
        main.performGameSearch(searchString);
        searchResultInfo.setText("Searching..");
    }
    
    private void addToFavorites() {
        for (String game : searchResult.getSelectedValuesList()) {
            if (!favoritesData.contains(game)) {
                favoritesData.addElement(game);
            }
        }
        saveFavorites();
    }

    private void removeFromFavorites() {
        for (String game : favorites.getSelectedValuesList()) {
            favoritesData.removeElement(game);
        }
        saveFavorites();
    }
    
    private void saveFavorites() {
        main.setGameFavorites(getFavorites());
    }
    
    private void loadFavorites() {
        favoritesData.clear();
        for (String game : main.getGameFavorites()) {
            favoritesData.addElement(game);
        }
    }
    
    public Set<String> getFavorites() {
        Set<String> result = new HashSet<>();
        Enumeration<String> e = favoritesData.elements();
        while (e.hasMoreElements()) {
            result.add(e.nextElement());
        }
        return result;
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(4,4,4,4);
        return gbc;
    }
    
    /**
     * Change the game to be used to the one currently selected in the given
     * JList.
     * 
     * @param list 
     */
    private void updateGameFromSelection(JList<String> list) {
        search.setText(list.getSelectedValue());
    }
    
    /**
     * Called when an item is selected either by changing the selected item
     * or clicked an already selected item.
     * 
     * @param source 
     */
    private void itemSelected(Object source) {
        if (source == searchResult) {
            updateGameFromSelection(searchResult);
        }
        else if (source == favorites) {
            updateGameFromSelection(favorites);
        }
    }
    
    private void useSelectedGame() {
        game = search.getText().trim();
        setVisible(false);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == search || e.getSource() == searchButton) {
            doSearch();
        }
        if (e.getSource() == ok) {
            useSelectedGame();
        }
        if (e.getSource() == cancel) {
            game = null;
            setVisible(false);
        }
        if (e.getSource() == addToFavorites) {
            addToFavorites();
        }
        if (e.getSource() == removeFromFavorites) {
            removeFromFavorites();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        itemSelected(e.getSource());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        itemSelected(e.getSource());
        if (e.getClickCount() == 2) {
            useSelectedGame();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        
    }
    
    private class DoneAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            useSelectedGame();
        }
        
    }
    
}
