
package chatty.gui.components;

import chatty.gui.LinkListener;
import chatty.gui.UrlOpener;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.api.StreamInfo;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class ChannelInfoDialog extends JDialog implements ViewerHistoryListener {

    private static final String STATUS_LABEL_TEXT = "Stream Status:";
    private static final String STATUS_LABEL_TEXT_HISTORY = "Stream Status (History):";
    
    private final JLabel statusLabel = new JLabel("Stream Status:");
    private final ExtendedTextPane title = new ExtendedTextPane();
    
    private final JLabel gameLabel = new JLabel("Playing:");
    private final JTextField game = new JTextField();
    
    private final JLabel historyLabel = new JLabel("Viewers:");
    private final ViewerHistory history = new ViewerHistory();
    
    private StreamInfo currentStreamInfo;
    
    private String statusText = "";
    private String gameText = "";
    
    public ChannelInfoDialog(Frame owner) {
        super(owner);
        setTitle("Channel Info");
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        // Status
        gbc = makeGbc(0,0,1,1);
        add(statusLabel,gbc);
        
        title.setEditable(false);
        //title.setLineWrap(true);
        //title.setWrapStyleWord(true);
        title.setLinkListener(new MyLinkListener());
        JScrollPane scroll = new JScrollPane(title);
        scroll.setPreferredSize(new Dimension(300,70));
        gbc = makeGbc(0,1,2,1);
        gbc.weighty = 0.1;
        gbc.fill = GridBagConstraints.BOTH;
        add(scroll,gbc);

        // Game
        gbc = makeGbc(0,2,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        add(gameLabel,gbc);
        
        game.setEditable(false);
        gbc = makeGbc(0,3,2,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(game,gbc);
 
        // Graph
        gbc = makeGbc(0,4,1,1);
        gbc.insets = new Insets(3,5,3,3);
        gbc.anchor = GridBagConstraints.WEST;
        add(historyLabel,gbc);
        
        history.setListener(this);
        history.setBackgroundColor(getBackground());
        history.setPreferredSize(new Dimension(300,150));
        gbc = makeGbc(0,5,2,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4,4,4,4);
        gbc.weightx = 1;
        gbc.weighty = 0.9;
        add(history,gbc);
        
        pack();
        setMinimumSize(new Dimension(220,290));
    }
    
    /**
     * Sets a new StreamInfo object.
     * 
     * @param streamInfo 
     */
    public void set(StreamInfo streamInfo) {
        this.setTitle("Channel Info: "+streamInfo.getStream()
            +(streamInfo.getFollowed() ? " (followed)" : ""));
        if (streamInfo.isValid() && streamInfo.getOnline()) {
            statusText = streamInfo.getTitle();
            gameText = streamInfo.getGame();
            if (gameText == null) {
                gameText = "No game";
            }
        }
        else if (streamInfo.isValid()) {
            statusText = "Stream offline";
            gameText = "";
        }
        else {
            statusText = "[No Stream Information]";
            gameText = "";
        }
        title.setText(statusText);
        game.setText(gameText);
        history.setHistory(streamInfo.getStream(), streamInfo.getHistory());
        currentStreamInfo = streamInfo;
    }
    
    /**
     * Updates the dialog if the given StreamInfo object is the one already
     * set, or does nothing otherwise.
     * 
     * @param streamInfo 
     */
    public void update(StreamInfo streamInfo) {
        if (streamInfo == currentStreamInfo) {
            set(streamInfo);
        }
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(3,3,3,3);
        return gbc;
    }

    @Override
    public void itemSelected(int viewers, String historyTitle, String historyGame) {
        title.setText(historyTitle);
        game.setText(historyGame);
        statusLabel.setText(STATUS_LABEL_TEXT_HISTORY);
    }
    
    @Override
    public void noItemSelected() {
        this.title.setText(statusText);
        this.game.setText(gameText);
        statusLabel.setText(STATUS_LABEL_TEXT);
    }
    
    public void setHistoryRange(int minutes) {
        history.setRange(minutes*60*1000);
    }
    
    public void addContextMenuListener(ContextMenuListener listener) {
        history.addContextMenuListener(listener);
        title.setContextMenuListener(listener);
    }
    
    private class MyLinkListener implements LinkListener {

        @Override
        public void linkClicked(String url) {
            UrlOpener.openUrlPrompt(ChannelInfoDialog.this, url);
        }
        
    }
    
}
