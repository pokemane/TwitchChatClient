
package chatty.gui.components;

import chatty.gui.CompletionServer;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A TextField that supports auto-completion and a history of entered text.
 * 
 * @author tduva
 */
public class ChannelEditBox extends JTextField implements KeyListener,
        ActionListener, DocumentListener {
    
    // History
    Vector<String> history = new Vector<>();
    int historyPosition = 0;
    boolean historyTextEdited = false;
    
    // Auto completion
    CompletionServer server;
    String prevCompletion = null;
    int prevCompletionIndex = 0;
    String prevCompletionText = null;
    
    public ChannelEditBox(int size) {
        super(size);
        this.addKeyListener(this);
        this.addActionListener(this);
        this.setFocusTraversalKeysEnabled(false);
        this.setFont(new Font("Consolas",Font.PLAIN,14));
        getDocument().addDocumentListener(this);
    }
    

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            historyBack();
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            historyForward();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            doAutoCompletion();
        }
    }
    
    //###############
    //### History ###
    //###############
    
    /**
     * Move back in the history, changing the text.
     */
    private void historyBack() { 
        if (historyPosition > 0 && historyPosition <= history.size()) {
            historyPosition--;
            String text = history.get(historyPosition);
            if (text != null) {
                setText(text);
                historyTextEdited = false;
            }
        }
    }
    
    private void historyForward() {
        
        historyAddChanged();
        
        historyPosition++;
        if (historyPosition >= history.size()) {
            // If further than the latest history entry, clear input
            setText("");
            historyTextEdited = false;
            historyPosition = history.size();
        }
        else if (historyPosition >= 0) {
            // If still in the history range set to next history position
            String text = history.get(historyPosition);
            if (text != null) {
                setText(text);
                historyTextEdited = false;
            }
        }
    }
    
    /**
     * Adds the current text to the history, if it was changed.
     */
    private void historyAddChanged() {
        if (historyTextEdited) {
            historyAdd(getText());
            historyTextEdited = false;
            historyPosition = history.size();
        }
    }
    
    /**
     * Adds the given text to the history. Mainly used when text is send. The
     * text is removed first, so it only occurs once. Only text that is not
     * empty is added.
     * 
     * @param text 
     */
    private void historyAdd(String text) {
        if (!text.isEmpty()) {
            history.remove(text);
            history.add(text);
        }
    }
    
    /**
     * Adds sent text to the history, sets history position and clears the
     * input field.
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        historyAdd(getText());
        historyPosition = history.size();
        setText("");
        historyTextEdited = false;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        historyTextEdited = true;
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        historyTextEdited = true;
    }

    @Override
    public void changedUpdate(DocumentEvent e) { }
    
    //#######################
    //### Auto Completion ###
    //#######################
    
    
    public void setCompletionServer(CompletionServer server) {
        this.server = server;
    }
    
    public void addCompletionType(String type, String prefix) {
        
    }


    
    private void doAutoCompletion() {
        if (server == null) {
            return;
        }
        
        // Get current state
        int pos = getCaretPosition();
        String text = getText();

        // Find start and end of the word where the caret is
        int end = findWordEnd(text,pos);
        int start = findWordStart(text,pos);

        // Get the word
        String word = text.substring(start, end);
        if (word.isEmpty()) {
            return;
        }
        String actualWord = word;

        // If text was manually edited after the previous completion, start
        // fresh
        if (!text.equals(prevCompletionText)) {
            prevCompletion = null;
            prevCompletionIndex = 0;
        }
        if (prevCompletion != null) {
            word = prevCompletion;
        }

        List<String> searchResult = findResults(word);


        // If no matches were found, quit now
        if (searchResult.isEmpty()) {
            return;
        }

        // If previous completion reached the end, start from the beginning
        if (prevCompletionIndex >= searchResult.size()) {
            prevCompletionIndex = 0;
        }

        // Get the value for this completion and replace the right
        // word with it
        int index = prevCompletionIndex;
        String nick = searchResult.get(index);

        // Create new text and set it
        String newText = text.substring(0, start) + nick + text.substring(end);
        setText(newText);

        // Set caret at the end of the new word
        int newEnd = end + (nick.length() - actualWord.length());
        setCaretPosition(newEnd);

        // Set variables for next completion
        prevCompletion = word;
        prevCompletionIndex = index + 1;
        prevCompletionText = newText;
    }

    /**
     * Create search result for the given search. Finds all words that start
     * with the given search.
     * 
     * @param nicks An Array of nicknames
     * @param search The String to search for
     * @return A List of search results
     */
    private List<String> findResults(String search) {
        
        List<String> nicks = server.getCompletionItems(search);
        java.util.Collections.sort(nicks);
        
        // Create search result for the current word
        //java.util.Arrays.sort(nicks);
//        ArrayList<String> searchResult = new ArrayList<>();
//        search = search.toLowerCase();
//        for (int i = 0; i < nicks.length; i++) {
//            String nick = nicks[i];
//            if (nick.toLowerCase().startsWith(search)) {
//                searchResult.add(nick);
//            }
//        }
        return nicks;
    }
    
    /**
     * Find the end of the word at the given position.
     * 
     * @param text The String to work with
     * @param pos The position where the word is
     * @return 
     */
    private int findWordEnd(String text, int pos) {
        int end = text.indexOf(32, pos);
        if (text.length() == pos) {
            end = text.length();
        }
        if (end == -1) {
            end = text.length();
        }
        return end;
    }
    
    /**
     * Find the beginning of the word at the given position.
     * 
     * Find the first whitespace before the position
     * 
     * @param text
     * @param pos
     * @return 
     */
    private int findWordStart(String text, int pos) {
        int start = text.lastIndexOf(32, pos - 1) + 1;
        if (start == -1) {
            start = 0;
        }
        return start;
    }
    
}
