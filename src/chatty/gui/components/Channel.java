
package chatty.gui.components;

import chatty.gui.MouseClickedListener;
import chatty.gui.CompletionServer;
import chatty.gui.StyleManager;
import chatty.gui.StyleServer;
import chatty.gui.MainGui;
import chatty.User;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.api.ChatIcons;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * A single channel window, combining styled text pane, userlist and input box.
 * 
 * @author tduva
 */
public class Channel extends JPanel implements CompletionServer {
    // Main text and userlist
    private static final int USERLIST_INITIAL_WIDTH = 120;
    
    private final ChannelEditBox input;
    private final ChannelTextPane text;
    private final UserList users;
    private String name;
    private final JSplitPane mainPane;
    private JScrollPane userlist;
    
    private final StyleServer styleManager;

    public Channel(String name, MainGui main, StyleManager styleManager,
            ContextMenuListener contextMenuListener, int userlistSize) {
        this.setLayout(new BorderLayout());
        this.styleManager = styleManager;
        // Text Pane
        text = new ChannelTextPane(main,styleManager);
        text.setContextMenuListener(contextMenuListener);
        final JScrollPane west = new JScrollPane(text);
        text.setScrollPane(west);
        InputMap westScrollInputMap = west.getInputMap(WHEN_IN_FOCUSED_WINDOW);
        westScrollInputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "pageUp");
        west.getActionMap().put("pageUp", new ScrollAction("pageUp", west.getVerticalScrollBar()));
        westScrollInputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "pageDown");
        west.getActionMap().put("pageDown", new ScrollAction("pageDown", west.getVerticalScrollBar()));

        // User list
        users = new UserList(contextMenuListener, main.getUserListener());
        
        userlist = new JScrollPane(users);
        setUserlistWidth(userlistSize);
        
        // Split Pane
        mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,west,userlist);
        mainPane.setResizeWeight(1);
        mainPane.setDividerSize(5);

        //updateDividerLocation();

        add(mainPane, BorderLayout.CENTER);
        
        // Text input
        JPanel inputPanel = new JPanel();
        input = new ChannelEditBox(40);
        inputPanel.add(input);
        input.addActionListener(main.getActionListener());
        input.setCompletionServer(this);
        add(input, BorderLayout.SOUTH);
        
        input.requestFocusInWindow();
        
        this.name = name;
        
        setStyles();
    }
    

    public void setMouseClickedListener(MouseClickedListener listener) {
        text.setMouseClickedListener(listener);
    }

    public void updateDividerLocation() {
        // Set the divider location once the windows is properly displayed
        // (hopefully)
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                mainPane.setDividerLocation(getWidth() - USERLIST_INITIAL_WIDTH);
            }
        });
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Gets the name of the stream (without leading #) if it is a stream channel
     * (and thus has a leading #) ;)
     * @return 
     */
    public String getStreamName() {
        if (name.startsWith("#")) {
            return name.substring(1);
        }
        return null;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void addUser(User user) {
        users.addUser(user);
    }
    
    public void removeUser(User user) {
        users.removeUser(user);
    }
    
    public void updateUser(User user) {
        users.updateUser(user);
    }
    
    public void clearUsers() {
        users.clearUsers();
    }
    
    public int getNumUsers() {
        return users.getNumUsers();
    }

    @Override
    public List<String> getCompletionItems(String search) {
        search = search.toLowerCase();
        ArrayList<User> data = users.getData();
        List<String> nicks = new ArrayList<>();
        Iterator<User> it = data.iterator();
        int i = 0;
        while (it.hasNext()) {
            String nick = it.next().getNick();
            if (nick.toLowerCase().startsWith(search)) {
                nicks.add(nick);
                i++;
            }
        }
        return nicks;
    }
    
    public ChannelEditBox getInput() {
        return input;
    }
    
    public String getInputText() {
        return input.getText();
    }

    @Override
    public boolean requestFocusInWindow() {
        // Invoke later, because otherwise it wouldn't get focus for some
        // reason.
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                //System.out.println("requesting focus for " + name);
                input.requestFocusInWindow();
            }
        });
        return input.requestFocusInWindow();
        
    }
    
    
    // Messages
    
    public boolean search(String searchText) {
        return text.search(searchText);
    }
    
    public void resetSearch() {
        text.resetSearch();
    }
    
    public void printLine(String line) {
        text.printLine(line);
    }
    
    public void userBanned(User user) {
        text.userBanned(user);
    }
    
    public void printCompact(String type, User user) {
        text.printCompact(type, user);
    }
    
    public void printMessage(User user, String message, boolean action,
            boolean highlighted) {
        text.printMessage(user, message, action, highlighted);
    }
    
    
    // Style
    
    public void refreshStyles() {
        text.refreshStyles();
        setStyles();
    }
    
    private void setStyles() {
        input.setFont(styleManager.getFont());
        input.setBackground(styleManager.getColor("inputBackground"));
        input.setCaretColor(styleManager.getColor("inputForeground"));
        input.setForeground(styleManager.getColor("inputForeground"));
        users.setBackground(styleManager.getColor("background"));
        users.setForeground(styleManager.getColor("foreground"));
    }
    
    public void setChatIcons(ChatIcons icons) {
        text.setChatIcons(icons);
    }
    
    private static class ScrollAction extends AbstractAction {
        
        private final String action;
        private final JScrollBar scrollbar;
        
        ScrollAction(String action, JScrollBar scrollbar) {
            this.scrollbar = scrollbar;
            this.action = action;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            int now = scrollbar.getValue();
            int height = scrollbar.getVisibleAmount();
            height = height - height / 10;
            int newValue = 0;
            switch (action) {
                case "pageUp": newValue = now - height; break;
                case "pageDown": newValue = now + height; break;
            }
            scrollbar.setValue(newValue);
        }
    }
    
    public void setUserlistWidth(int width) {
        userlist.setPreferredSize(new Dimension(width, 10));
        userlist.setMinimumSize(new Dimension(0, 0));
    }
    
}
