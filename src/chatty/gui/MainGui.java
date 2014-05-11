
package chatty.gui;

import chatty.gui.components.UserInfo;
import chatty.gui.components.DebugWindow;
import chatty.gui.components.ChannelInfoDialog;
import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.help.About;
import chatty.gui.components.HighlightedMessages;
import chatty.gui.components.TokenDialog;
import chatty.gui.components.AdminDialog;
import chatty.gui.components.ConnectionDialog;
import chatty.gui.components.Channel;
import chatty.gui.components.TokenGetDialog;
import chatty.gui.components.HotKeyChooserListener;
import chatty.gui.components.FavoritesDialog;
import chatty.gui.components.ChannelsWarning;
import chatty.gui.components.JoinDialog;
import chatty.util.api.Emoticon;
import chatty.util.api.ChatIcons;
import chatty.util.api.StreamInfo;
import chatty.util.api.TokenInfo;
import chatty.util.api.Emoticons;
import chatty.util.api.ChannelInfo;
import chatty.Chatty;
import chatty.TwitchClient;
import chatty.Helper;
import chatty.User;
import chatty.Irc;
import chatty.StatusHistory;
import chatty.UsercolorItem;
import chatty.gui.components.AddressbookDialog;
import chatty.gui.components.ErrorMessage;
import chatty.gui.components.LiveStreamListener;
import chatty.gui.components.LiveStreamsDialog;
import chatty.gui.components.SearchDialog;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.settings.SettingsDialog;
import chatty.gui.notifications.NotificationActionListener;
import chatty.gui.notifications.NotificationManager;
import chatty.util.Sound;
import chatty.util.settings.Setting;
import chatty.util.settings.SettingChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.LogRecord;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The Main Hub for all GUI activity.
 * 
 * @author tduva
 */
public class MainGui extends JFrame implements Runnable { 
    
    public static final Color COLOR_NEW_MESSAGE = new Color(200,0,0);
    public static final Color COLOR_NEW_HIGHLIGHTED_MESSAGE = new Color(255,80,0);
    
    public final Emoticons emoticons = new Emoticons();
    
    // Reference back to the client to give back data etc.
    TwitchClient client = null;
    
    // Parts of the GUI
    private Channels channels;
    private ConnectionDialog connectionDialog;
    private TokenDialog tokenDialog;
    private TokenGetDialog tokenGetDialog;
    private DebugWindow debugWindow;
    private UserInfo userInfoDialog;
    private About aboutDialog;
    private ChannelInfoDialog channelInfoDialog;
    private SettingsDialog settingsDialog;
    private AdminDialog adminDialog;
    private FavoritesDialog favoritesDialog;
    private JoinDialog joinDialog;
    private HighlightedMessages highlightedMessages;
    private MainMenu menu;
    private SearchDialog searchDialog;
    private LiveStreamsDialog liveStreamsDialog;
    private NotificationManager<String> notificationManager;
    private ErrorMessage errorMessage;
    private AddressbookDialog addressbookDialog;
    
    // Helpers
    private final Highlighter highlighter = new Highlighter();
    private StyleManager styleManager;
    private TrayIconManager trayIcon;
    private final StateUpdater state = new StateUpdater();
    private WindowStateManager windowStateManager;

    // Listeners that need to be returned by methods
    private ActionListener actionListener;
    private final WindowListener windowListener = new MyWindowListener();
    private final UserListener userListener = new MyUserListener();
    private final LiveStreamListener liveStreamListener = new MyLiveStreamListener();
    private final LinkLabelListener linkLabelListener = new MyLinkLabelListener();
    private final HotkeyUpdateListener hotkeyUpdateListener = new HotkeyUpdateListener();
    private final ContextMenuListener contextMenuListener = new MyContextMenuListener();
    
    // Remember state
    private boolean showedChannelsWarningThisSession = false;
    
    
    public MainGui(TwitchClient client) {
        this.client = client;
        SwingUtilities.invokeLater(this);
    }
    
    @Override
    public void run() {
        createGui();
    }

    
    private Image createImage(String name) {
        return Toolkit.getDefaultToolkit().createImage(getClass().getResource(name));
    }
    
    /**
     * Sets different sizes of the window icon.
     */
    private void setWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_16.png"));
        windowIcons.add(createImage("app_64.png"));
        this.setIconImages(windowIcons);
    }
    
    private void setLiveStreamsWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_live_16.png"));
        windowIcons.add(createImage("app_live_64.png"));
        liveStreamsDialog.setIconImages(windowIcons);
    }
    
    private void setHelpWindowIcons() {
        ArrayList<Image> windowIcons = new ArrayList<>();
        windowIcons.add(createImage("app_help_16.png"));
        windowIcons.add(createImage("app_help_64.png"));
        aboutDialog.setIconImages(windowIcons);
    }
    
    /**
     * Creates the gui, run in the EDT.
     */
    private void createGui() {

        setWindowIcons();
        
        actionListener = new MyActionListener();
        
        debugWindow = new DebugWindow(new DebugCheckboxListener());
        connectionDialog = new ConnectionDialog(this);
        tokenDialog = new TokenDialog(this);
        tokenGetDialog = new TokenGetDialog(this);
        userInfoDialog = new UserInfo(this, contextMenuListener);
        aboutDialog = new About();
        setHelpWindowIcons();
        channelInfoDialog = new ChannelInfoDialog(this);
        channelInfoDialog.addContextMenuListener(contextMenuListener);
        settingsDialog = new SettingsDialog(this,client.settings);
        adminDialog = new AdminDialog(this);
        favoritesDialog = new FavoritesDialog(this);
        joinDialog = new JoinDialog(this);
        searchDialog = new SearchDialog(this);
        GuiUtil.installEscapeCloseOperation(searchDialog);
        liveStreamsDialog = new LiveStreamsDialog(contextMenuListener);
        setLiveStreamsWindowIcons();
        //GuiUtil.installEscapeCloseOperation(liveStreamsDialog);
        errorMessage = new ErrorMessage(this, linkLabelListener);
        
        trayIcon = new TrayIconManager(createImage("app_16.png"));
        trayIcon.addActionListener(new TrayMenuListener());
        notificationManager = new NotificationManager<>(this);
        notificationManager.setNotificationActionListener(new MyNotificationActionListener());

        styleManager = new StyleManager(client.settings);
        
        highlightedMessages = new HighlightedMessages(this, styleManager);
        highlightedMessages.setContextMenuListener(contextMenuListener);
        
        channels = new Channels(this,styleManager, contextMenuListener);
        channels.getComponent().setPreferredSize(new Dimension(600,300));
        add(channels.getComponent(), BorderLayout.CENTER);
        channels.addTabChangeListener(new TabChangeListener());
        
        addressbookDialog = new AddressbookDialog(this, client.addressbook);

        //dialogs = new DialogManager(this, channels);
        
        client.settings.setSettingChangeListener(new MySettingChangeListener());
        
        this.getContentPane().setBackground(new Color(0,0,0,0));

        MenuListener menuListener = new MenuListener();
        menu = new MainMenu(menuListener,menuListener);
        setJMenuBar(menu);

        state.update();
        
        addListeners();
        
        
        pack();
        setLocationByPlatform(true);
        
        
        client.api.requestEmoticons();
        
        windowStateManager = new WindowStateManager(this, client.settings);
        windowStateManager.addWindow(this, "main", true, true);
        windowStateManager.setPrimaryWindow(this);
        windowStateManager.addWindow(highlightedMessages, "highlights", true, true);
        windowStateManager.addWindow(channelInfoDialog, "channelInfo", true, true);
        windowStateManager.addWindow(liveStreamsDialog, "liveStreams", true, true);
        windowStateManager.addWindow(adminDialog, "admin", true, true);
        windowStateManager.addWindow(addressbookDialog, "addressbook", true, true);
    }
    
    private void addListeners() {
        
        MainWindowListener mainWindowListener = new MainWindowListener();
        addWindowStateListener(mainWindowListener);
        addWindowListener(mainWindowListener);
        //highlightedMessages.addComponentListener(mainWindowListener);
        //liveStreamsDialog.addComponentListener(mainWindowListener);
        
        String switchTab = "switchTab";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ctrl TAB"), switchTab);
        getRootPane().getActionMap().put(switchTab, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToNextChannel();
            }
        });
        String switchTabBack = "switchTabBack";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ctrl shift TAB"), switchTabBack);
        getRootPane().getActionMap().put(switchTabBack, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                channels.switchToPreviousChannel();
            }
        });
    }
    
    public void showGui() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                setVisible(true);
                // Should be done when the main window is already visible, so
                // it can be centered on it correctly, if that is necessary
                reopenWindows();
                
            }
        });
    }
    
    private void makeVisible() {
        toFront();
        setState(Frame.NORMAL);
    }
    
    /**
     * Loads settings
     */
    public void loadSettings() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                loadSettingsInternal();
            }
        });
    }

    /**
     * Initiates the GUI with settings
     */
    private void loadSettingsInternal() {
        setAlwaysOnTop(client.settings.getBoolean("ontop"));

        loadMenuSettings();
        updateConnectionDialog(null);
        userInfoDialog.setTimeoutButtonsDef(client.settings.getString("timeoutButtons"));
        debugWindow.getLogIrcCheckBox().setSelected(client.settings.getBoolean("debugLogIrc"));
        updateLiveStreamsDialog();
        
//        // Set window location
//        int x = (int)client.settings.getLong("x");
//        int y = (int)client.settings.getLong("y");
//        if (x > -1 && y > -1) {
//            setLocation(x, y);
//        }
//        // Set window size
//        int width = (int)client.settings.getLong("width");
//        int height = (int)client.settings.getLong("height");
//        if (width > -1 && height > -1) {
//            setSize(width, height);
//        }
        windowStateManager.loadWindowStates();
        
        // Set window maximized state
        if (client.settings.getBoolean("maximized")) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        updateHighlight();
        updateHistoryRange();
        updateNotificationSettings();
        updateChannelsSettings();
        updateHighlightNextMessages();
        loadCommercialDelaySettings();
        UrlOpener.setPrompt(client.settings.getBoolean("urlPrompt"));
        channels.setTabOrder(client.settings.getString("tabOrder"));
    }
    
    /**
     * Initiates the Main Menu with settings
     */
    private void loadMenuSettings() {
        loadMenuSetting("showJoinsParts");
        loadMenuSetting("ignoreJoinsParts");
        loadMenuSetting("ontop");
    }
    
    /**
     * Initiates a single setting in the Main Menu
     * @param name The name of the setting
     */
    private void loadMenuSetting(String name) {
        menu.setItemState(name,client.settings.getBoolean(name));
    }
    
    /**
     * Tells the highlighter the current list of highlight-items from the settings.
     */
    private void updateHighlight() {
        highlighter.update(Helper.getStringList(client.settings.getList("highlight")));
    }
    
    private void updateChannelsSettings() {
        channels.setDefaultUserlistWidth((int)client.settings.getLong("userlistWidth"));
    }
    
    /**
     * Tells the highlighter the current username and whether it should be used
     * for highlight. Used to initialize on connect, when the username is fixed
     * for the duration of the connection.
     * 
     * @param username The current username.
     */
    public void updateHighlightSetUsername(String username) {
        highlighter.setUsername(username);
        highlighter.setHighlightUsername(client.settings.getBoolean("highlightUsername"));
    }
    
    /**
     * Tells the highlighter whether the current username should be used for
     * highlight. Used to set the setting when the setting is changed.
     * 
     * @param highlight 
     */
    private void updateHighlightSetUsernameHighlighted(boolean highlight) {
        highlighter.setHighlightUsername(highlight);
    }
    
    private void updateHighlightNextMessages() {
        highlighter.setHighlightNextMessages(client.settings.getBoolean("highlightNextMessages"));
    }
    
    private void updateNotificationSettings() {
        notificationManager.setDisplayTime((int)client.settings.getLong("nDisplayTime"));
        notificationManager.setMaxDisplayTime((int)client.settings.getLong("nMaxDisplayTime"));
        notificationManager.setMaxDisplayItems((int)client.settings.getLong("nMaxDisplayed"));
        notificationManager.setMaxQueueSize((int)client.settings.getLong("nMaxQueueSize"));
        int activityTime = client.settings.getBoolean("nActivity")
                ? (int)client.settings.getLong("nActivityTime") : -1;
        notificationManager.setActivityTime(activityTime);
        notificationManager.clearAll();
        notificationManager.setScreen((int)client.settings.getLong("nScreen"));
        notificationManager.setPosition((int)client.settings.getLong(("nPosition")));
    }
    
    /**
     * Saves location/size for windows/dialogs and whether it was open.
     */
    public void saveWindowStates() {
        windowStateManager.saveWindowStates();
    }
    
    /**
     * Reopen some windows if enabled.
     */
    private void reopenWindows() {
        reopenWindow(liveStreamsDialog);
        reopenWindow(highlightedMessages);
        reopenWindow(channelInfoDialog);
        reopenWindow(addressbookDialog);
        reopenWindow(adminDialog);
    }
    
    /**
     * Open the given Component if enabled and if it was open before.
     * 
     * @param window 
     */
    private void reopenWindow(Window window) {
        if (windowStateManager.shouldReopen(window)) {
            if (window == liveStreamsDialog) {
                openLiveStreamsDialog();
            } else if (window == highlightedMessages) {
                openHighlightedMessages();
            } else if (window == channelInfoDialog) {
                openChannelInfoDialog();
            } else if (window == addressbookDialog) {
                openAddressbook(null);
            } else if (window == adminDialog) {
                openChannelAdminDialog();
            }
        }
    }
    
    /**
     * Saves whether the window is currently maximized.
     */
    private void saveState(Component c) {
        if (c == this) {
            client.settings.setBoolean("maximized", isMaximized());
        }
    }
    
    /**
     * Returns if the window is currently maximized.
     * 
     * @return true if the window is maximized, false otherwise
     */
    private boolean isMaximized() {
        return (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
    }
    
    /**
     * Updates the connection dialog with current settings
     */
    private void updateConnectionDialog(String channelPreset) {
        connectionDialog.setUsername(client.settings.getString("username"));
        if (channelPreset != null) {
            connectionDialog.setChannel(channelPreset);
        } else {
            connectionDialog.setChannel(client.settings.getString("channel"));
        }

        String password = client.settings.getString("password");
        String token = client.settings.getString("token");
        boolean usePassword = client.settings.getBoolean("usePassword");
        connectionDialog.update(password, token, usePassword);
        connectionDialog.setAreChannelsOpen(channels.getChannelCount() > 0);
    }
    
    private void updateChannelInfoDialog() {
        String stream = channels.getActiveChannel().getStreamName();
        StreamInfo streamInfo = getStreamInfo(stream);
        channelInfoDialog.set(streamInfo);
    }
    
    private void updateTokenDialog() {
        String username = client.settings.getString("username");
        String token = client.settings.getString("token");
        tokenDialog.update(username, token);
    }
    
    private void updateFavoritesDialog() {
        Set<String> favorites = client.channelFavorites.getFavorites();
        Map<String, Long> history = client.channelFavorites.getHistory();
        favoritesDialog.setData(favorites, history);
    }
    
    private void updateFavoritesDialogWhenVisible() {
        if (favoritesDialog.isVisible()) {
            updateFavoritesDialog();
        }
    }
    
    public void userUpdated(final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateUserInfoDialog(user);
            }
        });
    }
    
    private void updateUserInfoDialog(User user) {
        userInfoDialog.update(user, client.getUsername());
    }
    
    private void updateLiveStreamsDialog() {
        liveStreamsDialog.setSorting(client.settings.getString("liveStreamsSorting"));
    }
    
    private void updateHistoryRange() {
        int range = (int)client.settings.getLong("historyRange");
        channelInfoDialog.setHistoryRange(range);
        liveStreamsDialog.setHistoryRange(range);
    }
    
    private void openTokenDialog() {
        updateTokenDialog();
        updateTokenScopes();
        tokenDialog.setLocationRelativeTo(connectionDialog);
        tokenDialog.setVisible(true);
    }
    
    public void addStreamInfo(final StreamInfo streamInfo) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                liveStreamsDialog.addStream(streamInfo);
            }
        });
    }
    
    public ActionListener getActionListener() {
        return actionListener;
    }
    
    public StatusHistory getStatusHistory() {
        return client.statusHistory;
    }
    
    public boolean saveStatusHistory() {
        return client.settings.getBoolean("saveStatusHistory");
    }
    
    class MyActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            // text input
            Channel chan = channels.getChannelFromInput(event.getSource());
            if (chan != null) {
                client.textInput(chan.getName(), chan.getInputText());
            }

            Object source = event.getSource();
            //---------------------------
            // Connection Dialog actions
            //---------------------------

            if (source == connectionDialog.getCancelButton()) {
                connectionDialog.setVisible(false);
                channels.setInitialFocus();
            } else if (source == connectionDialog.getConnectButton()
                    || source == connectionDialog.getChannelInput()) {
                String password = connectionDialog.getPassword();
                String channel = connectionDialog.getChannel();
                //client.settings.setString("username",name);
                client.settings.setString("password", password);
                client.settings.setString("channel", channel);
                if (client.prepareConnection(connectionDialog.rejoinOpenChannels())) {
                    connectionDialog.setVisible(false);
                    channels.setInitialFocus();
                }
            } else if (event.getSource() == connectionDialog.getGetTokenButton()) {
                openTokenDialog();
            } else if (event.getSource() == connectionDialog.getFavoritesButton()) {
                openFavoritesDialogFromConnectionDialog(connectionDialog.getChannel());
            } //---------------------------
            // Token Dialog actions
            //---------------------------
            else if (event.getSource() == tokenDialog.getDeleteTokenButton()) {
                client.settings.setString("token", "");
                client.settings.setString("username", "");
                resetTokenScopes();
                updateConnectionDialog(null);
                tokenDialog.update("", "");
                updateTokenScopes();
            } else if (event.getSource() == tokenDialog.getRequestTokenButton()) {
                tokenGetDialog.setLocationRelativeTo(tokenDialog);
                tokenGetDialog.reset();
                client.startWebserver();
                tokenGetDialog.setVisible(true);

            } else if (event.getSource() == tokenDialog.getDoneButton()) {
                tokenDialog.setVisible(false);
            } else if (event.getSource() == tokenDialog.getVerifyTokenButton()) {
                verifyToken(client.settings.getString("token"));
            } // Get token Dialog
            else if (event.getSource() == tokenGetDialog.getCloseButton()) {
                tokenGetDialogClosed();
            } //-----------------
            // Userinfo Dialog
            //-----------------
            else if (userInfoDialog.getAction(event.getSource()) != UserInfo.ACTION_NONE) {
                int action = userInfoDialog.getAction(event.getSource());
                User user = userInfoDialog.getUser();
                String nick = user.getNick();
                String channel = userInfoDialog.getChannel();
                if (action == UserInfo.ACTION_BAN) {
                    client.ban(channel, nick);
                } else if (action == UserInfo.ACTION_UNBAN) {
                    client.unban(channel, nick);
                } else if (action == UserInfo.ACTION_TIMEOUT) {
                    int time = userInfoDialog.getTimeoutButtonTime(event.getSource());
                    client.timeout(channel, nick, time);
                } else if (action == UserInfo.ACTION_MOD) {
                    client.mod(channel, nick);
                } else if (action == UserInfo.ACTION_UNMOD) {
                    client.unmod(channel, nick);
                }
            // Favorites Dialog
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_ADD_FAVORITES) {
                Set<String> channels = favoritesDialog.getChannels();
                client.channelFavorites.addChannelsToFavorites(channels);
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_REMOVE_FAVORITES) {
                Set<String> channels = favoritesDialog.getSelectedChannels();
                client.channelFavorites.removeChannelsFromFavorites(channels);
            } else if (favoritesDialog.getAction(source) == FavoritesDialog.BUTTON_REMOVE) {
                Set<String> channels = favoritesDialog.getSelectedChannels();
                client.channelFavorites.removeChannels(channels);
            }
        }
        
    }

    private class DebugCheckboxListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            boolean state = e.getStateChange() == ItemEvent.SELECTED;
            if (e.getSource() == debugWindow.getLogIrcCheckBox()) {
                client.settings.setBoolean("debugLogIrc", state);
            }
        }
    }
    

    private class MyLinkLabelListener implements LinkLabelListener {
        @Override
        public void linkClicked(String type, String ref) {
            if (type.equals("help")) {
                openHelp(ref);
            } else if (type.equals("url")) {
                UrlOpener.openUrlPrompt(MainGui.this, ref);
            }
        }
    }
    
    public LinkLabelListener getLinkLabelListener() {
        return linkLabelListener;
    }
    
    public void clearHistory() {
        client.channelFavorites.clearHistory();
    }
    
    private class TrayMenuListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd == null) {
                makeVisible();
            }
            else if (cmd.equals("exit")) {
                exit();
            }
        }
        
    }
    
    private class MyLiveStreamListener implements LiveStreamListener {

        @Override
        public void liveStreamClicked(StreamInfo stream) {
            makeVisible();
            client.joinChannel(stream.getStream());
        }
        
    }
    
    /**
     * Listener for the Main Menu
     */
    private class MenuListener implements ItemListener, ActionListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            String setting = menu.getSettingByMenuItem(e.getSource());
            boolean state = e.getStateChange() == ItemEvent.SELECTED;

            if (setting != null) {
                client.settings.setBoolean(setting, state);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("debug")) {
                if (!debugWindow.isShowing()) {
                    debugWindow.setLocationByPlatform(true);
                    debugWindow.setPreferredSize(new Dimension(500, 400));
                }
                debugWindow.setVisible(true);
            } else if (cmd.equals("connect")) {
                openConnectDialogInternal(null);
            } else if (cmd.equals("disconnect")) {
                client.disconnect();
            } else if (cmd.equals("exit")) {
                exit();
            } else if (cmd.equals("about")) {
                openHelp("");
            } else if (cmd.equals("channelInfoDialog")) {
                openChannelInfoDialog();
            } else if (cmd.equals("settings")) {
                settingsDialog.showSettings();
            } else if (cmd.equals("website")) {
                UrlOpener.openUrlPrompt(MainGui.this, Chatty.WEBSITE, true);
            } else if (cmd.equals("channelAdminDialog")) {
                openChannelAdminDialog();
            } else if (cmd.equals("favoritesDialog")) {
                openFavoritesDialogToJoin("");
            } else if (cmd.equals("unhandledException")) {
                String[] array = new String[0];
                String a = array[1];
            } else if (cmd.equals("joinChannel")) {
                openJoinDialog();
            } else if (cmd.equals("highlightedMessages")) {
                openHighlightedMessages();
            } else if (cmd.equals("search")) {
                openSearchDialog();
            } else if (cmd.equals("onlineChannels")) {
                openLiveStreamsDialog();
            } else if (cmd.equals("addressbook")) {
                openAddressbook(null);
            }
        }
    
    }
    
    /**
     * Listener for context menu events in the channel pane
     */
    class MyContextMenuListener implements ContextMenuListener {

        @Override
        public void userMenuItemClicked(ActionEvent e, User user) {
            String cmd = e.getActionCommand();
            if (cmd.equals("userinfo")) {
                openUserInfoDialog(user);
            }
            else if (cmd.equals("profile")) {
                TwitchUrl.openTwitchProfile(user.getNick(), MainGui.this);
            }
            else if (cmd.equals("stream")) {
                TwitchUrl.openTwitchStream(user.getNick(), MainGui.this);
            }
            else if (cmd.equals("streamPopout")) {
                TwitchUrl.openTwitchStream(user.getNick(), true, MainGui.this);
            }
            else if (cmd.equals("joinChannel")) {
                client.commandJoinChannel("#"+user.getNick());
            }
            else if (cmd.equals("setColor")) {
                settingsDialog.showSettings("editUsercolorItem", user.getNick());
            }
            else if (cmd.equals("addressbookEdit")) {
                openAddressbook(user.getNick());
            }
            else if (cmd.equals("addressbookRemove")) {
                client.addressbook.remove(user.getNick());
                updateUserInfoDialog(user);
            }
            else if (cmd.startsWith("cat")) {
                if (e.getSource() instanceof JCheckBoxMenuItem) {
                    boolean selected = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    String catName = cmd.substring(3);
                    if (selected) {
                        client.addressbook.add(user.getNick(), catName);
                    } else {
                        client.addressbook.remove(user.getNick(), catName);
                    }
                }
                updateUserInfoDialog(user);
            }
        }

        @Override
        public void urlMenuItemClicked(ActionEvent e, String url) {
            String cmd = e.getActionCommand();
            if (cmd.equals("open")) {
                UrlOpener.openUrlPrompt(MainGui.this, url);
            }
            else if (cmd.equals("copy")) {
                Helper.copyToClipboard(url);
            }
            else if (cmd.equals("joinChannel")) {
                client.commandJoinChannel(url);
            }
        }

        @Override
        public void menuItemClicked(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals("channelInfo")) {
                openChannelInfoDialog();
            }
            else if (cmd.equals("channelAdmin")) {
                openChannelAdminDialog();
            }
            else if (cmd.equals("closeChannel")) {
                client.closeChannel(channels.getActiveChannel().getName());
            }
            else if (cmd.equals("profile")) {
                TwitchUrl.openTwitchProfile(channels.getActiveChannel().getStreamName(), MainGui.this);
            }
            else if (cmd.equals("stream")) {
                TwitchUrl.openTwitchStream(channels.getActiveChannel().getStreamName(), MainGui.this);
            }
            else if (cmd.equals("streamPopout")) {
                TwitchUrl.openTwitchStream(channels.getActiveChannel().getStreamName(), true, MainGui.this);
            }
            else if (cmd.equals("clearHighlights")) {
                highlightedMessages.clear();
            }
            else if (cmd.startsWith("range")) {
                int range = -1;
                switch (cmd) {
                    case "range1h":
                        range = 60;
                        break;
                    case "range2h":
                        range = 120;
                        break;
                    case "range4h":
                        range = 240;
                        break;
                    case "range8h":
                        range = 480;
                        break;
                    case "range12h":
                        range = 720;
                        break;
                }
                // Change here as well, because even if it's the same value,
                // update may be needed. This will make it update twice often.
                updateHistoryRange();
                client.settings.setLong("historyRange", range);
            }
        }

        @Override
        public void streamsMenuItemClicked(ActionEvent e, java.util.List<StreamInfo> streams) {
            String cmd = e.getActionCommand();
            if (cmd.equals("stream") || cmd.equals("streamPopout") ||
                    cmd.equals("profile")) {
                java.util.List<String> urls = new ArrayList<>();
                for (StreamInfo info : streams) {
                    String url;
                    if (cmd.equals("stream")) {
                        url = TwitchUrl.makeTwitchStreamUrl(info.getStream(), false);
                    } else if (cmd.equals("profile")) {
                        url = TwitchUrl.makeTwitchProfileUrl(info.getStream());
                    } else {
                        url = TwitchUrl.makeTwitchStreamUrl(info.getStream(), true);
                    }
                    urls.add(url);
                }
                UrlOpener.openUrlsPrompt(liveStreamsDialog, urls, true);
            }
            else if (cmd.equals("join")) {
                Set<String> channels = new HashSet<>();
                for (StreamInfo info : streams) {
                    channels.add(info.getStream());
                }
                makeVisible();
                client.joinChannels(channels);
            }
            else if (cmd.startsWith("streams")) {
                ArrayList<String> streams2 = new ArrayList<>();
                for (StreamInfo info : streams) {
                    streams2.add(info.getStream());
                }
                String type = TwitchUrl.MULTITWITCH;
                switch (cmd) {
                    case "streamsSpeedruntv": type = TwitchUrl.SPEEDRUNTV;
                }
                TwitchUrl.openMultitwitch(streams2, liveStreamsDialog, type);
            }
            String sorting = null;
            if (cmd.equals("sortName")) {
                sorting = "name";
            } else if (cmd.equals("sortGame")) {
                sorting = "game";
            } else if (cmd.equals("sortRecent")) {
                sorting = "recent";
            }
            if (sorting != null) {
                client.settings.setString("liveStreamsSorting", sorting);
            }
        }
    }

    private class TabChangeListener implements ChangeListener {
        /**
         * When a TAB is changed.
         *
         * @param e
         */
        @Override
        public void stateChanged(ChangeEvent e) {
            state.update();
            updateChannelInfoDialog();
        }
    }
    
    private class MyUserListener implements UserListener {
        @Override
        public void userClicked(User user) {
            openUserInfoDialog(user);
        }
    }
    
    private class MyNotificationActionListener implements NotificationActionListener<String> {

        @Override
        public void notificationAction(String data) {
            if (data != null) {
                makeVisible();
                client.joinChannel(data);
            }
        }
        
    }
    
    public UserListener getUserListener() {
        return userListener;
    }
    
    
    private class HotkeyUpdateListener implements HotKeyChooserListener {
        @Override
        public void hotkeyUpdated(String id, String hotkey) {
            if (id.equals("commercialHotkey")) {
                setCommercialHotkey(hotkey);
            }
        }
    }
    
    public HotKeyChooserListener getHotkeyUpdateListener() {
        return hotkeyUpdateListener;
    }
    
    public java.util.List<UsercolorItem> getUsercolorData() {
        return client.usercolorManager.getData();
    }
    
    public void setUsercolorData(java.util.List<UsercolorItem> data) {
        client.usercolorManager.setData(data);
    }
    
    private void openLiveStreamsDialog() {
        windowStateManager.setWindowPosition(liveStreamsDialog);
        liveStreamsDialog.setAlwaysOnTop(client.settings.getBoolean("ontop"));
        liveStreamsDialog.setState(JFrame.NORMAL);
        liveStreamsDialog.setVisible(true);
    }
    
    private void openUserInfoDialog(User user) {
        userInfoDialog.show(this,user, client.getUsername());
    }
    
    private void openChannelInfoDialog() {
        windowStateManager.setWindowPosition(channelInfoDialog);
        channelInfoDialog.setVisible(true);
    }
    
    private void openChannelAdminDialog() {
        windowStateManager.setWindowPosition(adminDialog);
        updateTokenScopes();
        adminDialog.loadCommercialHotkey(client.settings.getString("commercialHotkey"));
        String stream = channels.getActiveChannel().getStreamName();
        if (stream == null) {
            stream = client.settings.getString("username");
        }
        adminDialog.open(stream);
    }
    
    private void openHelp(String ref) {
        openHelp(null, ref);
    }
    
    public void openHelp(String page, String ref) {
        if (!aboutDialog.isVisible()) {
            aboutDialog.setLocationRelativeTo(this);
        }
        aboutDialog.open(page, ref);
        // Set ontop setting, so it won't be hidden behind the main window
        aboutDialog.setAlwaysOnTop(client.settings.getBoolean("ontop"));
        aboutDialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        aboutDialog.toFront();
        aboutDialog.setState(NORMAL);
        aboutDialog.setVisible(true);
    }
    
    public void openReleaseInfo() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (!client.settings.getString("currentVersion").equals(Chatty.VERSION)) {
                    client.settings.setString("currentVersion", Chatty.VERSION);
                    openHelp("help-releases.html", null);
                }
            }
        });
    }
    
    private void openSearchDialog() {
        searchDialog.setLocationRelativeTo(this);
        searchDialog.setVisible(true);
    }
    
    private void openFavoritesDialogFromConnectionDialog(String channel) {
        Set<String> channels = chooseFavorites(this, channel);
        if (!channels.isEmpty()) {
            connectionDialog.setChannel(Helper.buildStreamsString(channels));
        }
    }
    
    public Set<String> chooseFavorites(Component owner, String channel) {
        updateFavoritesDialog();
        favoritesDialog.setLocationRelativeTo(owner);
        int result = favoritesDialog.showDialog(channel, "Use chosen channels",
                "Use chosen channel");
        if (result == FavoritesDialog.ACTION_DONE) {
            return favoritesDialog.getChannels();
        }
        return new HashSet<>();
    }
    
    private void openFavoritesDialogToJoin(String channel) {
        updateFavoritesDialog();
        favoritesDialog.setLocationRelativeTo(this);
        int result = favoritesDialog.showDialog(channel, "Join chosen channels",
                "Join chosen channel");
        if (result == FavoritesDialog.ACTION_DONE) {
            Set<String> selectedChannels = favoritesDialog.getChannels();
            client.joinChannels(selectedChannels);
        }
    }
    
    private void openJoinDialog() {
        joinDialog.setLocationRelativeTo(this);
        Set<String> chans = joinDialog.showDialog();
        client.joinChannels(chans);
    }
    
    private void openHighlightedMessages() {
        windowStateManager.setWindowPosition(highlightedMessages);
        highlightedMessages.setVisible(true);
    }
    
    /**
     * Opens the addressbook, opening an edit dialog for the given name if it
     * is non-null.
     * 
     * @param name The name to edit or null.
     */
    private void openAddressbook(String name) {
        if (!addressbookDialog.isVisible()) {
            windowStateManager.setWindowPosition(addressbookDialog);
        }
        addressbookDialog.showDialog(name);
    }

    
    /*
     * Channel Management
     */
    
    /**
     * Add the channel with the given name.
     * 
     * @param channel 
     */
    public void addChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    channels.addChannel(channel);
                    state.update();
                }
            });
    }
    
    public void removeChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.removeChannel(channel);
                state.update();
            }
        });
    }
    
    public void switchToChannel(final String channel) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.switchToChannel(channel);
            }
        });
    }
    
    private void messageSound(String channel) {
        playSound("message", channel);
    }
    
    private void playHighlightSound(String channel) {
        playSound("highlight", channel);
    }
    
    /**
     * Plays the sound for the given sound identifier (highlight, status, ..),
     * if the requirements are met.
     * 
     * @param id The id of the sound
     * @param channel The channel this event originated from, to check
     *  requirements
     */
    public void playSound(final String id, final String channel) {
        if (SwingUtilities.isEventDispatchThread()) {
            playSoundInternal(id, channel);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    playSoundInternal(id, channel);
                }
            });
        }
    }
    
    /**
     * Plays the sound for the given sound identifier (highlight, status, ..),
     * if the requirements are met. For use in EDT, because it may have to check
     * which channel is currently selected, but not sure if that has to be in
     * the EDT.
     * 
     * @param id The id of the sound
     * @param channel The channel this event originated from, to check
     *  requirements
     */
    private void playSoundInternal(String id, String channel) {
        if (client.settings.getBoolean("sounds")
                && checkRequirements(client.settings.getString(id + "Sound"), channel)) {
            playSound(id);
        }
    }
    
    /**
     * Plays the sound for the given sound identifier (highlight, status, ..).
     * 
     * @param id 
     */
    private void playSound(String id) {
        String fileName = client.settings.getString(id + "SoundFile");
        long volume = client.settings.getLong(id + "SoundVolume");
        int delay = ((Long) client.settings.getLong(id + "SoundDelay")).intValue();
        Sound.play(fileName, volume, id, delay);
    }
    
    private void showHighlightNotification(String channel, User user, String text) {
        String setting = client.settings.getString("highlightNotification");
        if (checkRequirements(setting, channel)) {
            showNotification("[Highlight] " + user.getNick() + " in " + channel, text, null);
        }
    }
    
    public void setChannelNewStatus(final String channel, final String newStatus) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.setChannelNewStatus(channels.getChannel(channel));
            }
        });
    }
    
    public void statusNotification(final String channel, final String status) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (checkRequirements(client.settings.getString("statusNotification"), channel)) {
                    showNotification("[Status] " + channel, status, channel);
                }
                playSound("status", channel);
            }
        });
    }
    
    private void showNotification(String title, String message, String channel) {
        if (client.settings.getBoolean("useCustomNotifications")) {
            notificationManager.showMessage(title, message, channel);
        } else {
            trayIcon.displayInfo(title, message);
        }
    }
    
    public void showTestNotification() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getString("username").equalsIgnoreCase("joshimuz")) {
                    showNotification("[Test] It works!", "Now you have your notifications Josh.. Kappa", null);
                } else {
                    showNotification("[Test] It works!", "This is where the text goes.", null);
                }
            }
        });
    }
    
    /**
     * Checks the requirements that depend on whether the app and/or the given
     * channel is active.
     * 
     * @param setting What the requirements are
     * @param channel The channel to check the requirement against
     * @return true if the requirements are met, false otherwise
     */
    private boolean checkRequirements(String setting, String channel) {
        boolean channelActive = channels.getActiveChannel().getName().equals(channel);
        boolean appActive = isAppActive();
        // These conditions check when the requirements are NOT met
        if (setting.equals("off")) {
            return false;
        }
        if (setting.equals("both") && (channelActive || appActive)) {
            return false;
        }
        if (setting.equals("channel") && channelActive) {
            return false;
        }
        if (setting.equals("app") && appActive) {
            return false;
        }
        if (setting.equals("either") && (channelActive && appActive)) {
            return false;
        }
        if (setting.equals("channelActive") && !channelActive) {
            return false;
        }
        return true;
    }
    
    private boolean isAppActive() {
        for (Window frame : Window.getWindows()) {
            if (frame.isActive()) {
                return true;
            }
        }
        return false;
    }
    
    /* ############
     * # Messages #
     */
    
    public void printMessage(final String channel, final User user,
            final String text, final boolean action) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                client.chatLog.message(channel, user, text);
                Channel chan = channels.getChannel(channel);
                boolean highlighted = false;
                if (client.settings.getBoolean("highlightEnabled")) {
                    String ownUsername = client.getUsername();
                    if (client.settings.getBoolean("highlightOwnText")
                            || ownUsername == null
                            || !ownUsername.equalsIgnoreCase(user.getNick())) {
                        highlighted = highlighter.check(user, text);
                    }
                }
                if (highlighted) {
                    highlightedMessages.addMessage(channel, user, text, action);
                    playHighlightSound(channel);
                    showHighlightNotification(channel, user, text);
                } else {
                    messageSound(channel);
                }
                chan.printMessage(user, text, action, highlighted);
                user.addMessage(text);
                updateUserInfoDialog(user);
                
                if (highlighted) {
                    channels.setChannelHighlighted(chan);
                } else {
                    channels.setChannelNewMessage(chan);
                }
            }
        });
    }
    
    public void userBanned(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                channels.getChannel(channel).userBanned(user);
                user.addBan();
                updateUserInfoDialog(user);
            }
        });
    }
    

    
    /**
     * Shows a warning about joining several channels, depending on the settings.
     */
    public void channelsWarning() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (client.settings.getBoolean("tc3")) {
                    return;
                }
                boolean show = client.settings.getBoolean("channelsWarning");
                if (showedChannelsWarningThisSession) {
                    show = false;
                }
                if (show) {
                    int result = ChannelsWarning.showWarning(MainGui.this, linkLabelListener);
                    boolean showAgain = true;
                    if (result == ChannelsWarning.DONT_SHOW_AGAIN) {
                        showAgain = false;
                    }
                    client.settings.setBoolean("channelsWarning", showAgain);
                    showedChannelsWarningThisSession = true;
                }
            }
        });
    }

    public void printLine(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getActiveChannel();
                if (panel != null) {
                    panel.printLine(line);
                    client.chatLog.info(panel.getName(), line);
                }
            }
        });
    }
    
    public void printSystem(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel panel = channels.getActiveChannel();
                if (panel != null) {
                    panel.printLine(line);
                    client.chatLog.system(panel.getName(), line);
                }
            }
        });
    }

    public void printLine(final String channel, final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (channel == null) {
                    printLine(line);
                } else {
                    channels.getChannel(channel).printLine(line);
                    client.chatLog.info(channel, line);
                }
            }
        });
    }
    
    public void printLineAll(final String line) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //client.chatLog.info(null, line);
                if (channels.getChannelCount() == 0) {
                    Channel panel = channels.getActiveChannel();
                    if (panel != null) {
                        panel.printLine(line);
                    }
                    return;
                }
                Enumeration e = channels.elements();
                while (e.hasMoreElements()) {
                    Channel panel = (Channel)e.nextElement();
                    panel.printLine(line);
                    client.chatLog.info(panel.getName(), line);
                }
            }
        });
    }
    
    
    /**
     * Calls the appropriate method from the given channel
     * 
     * @param channel The channel this even has happened in.
     * @param type The type of event.
     * @param user The User object of who was the target of this event (mod/..).
     */
    public void printCompact(final String channel, final String type, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).printCompact(type, user);
            }
        });
    }
    
    /**
     * Perform search in the currently selected channel. Should only be called
     * from the EDT.
     * 
     * @param searchText 
     */
    public boolean search(final String searchText) {
        return channels.getActiveChannel().search(searchText);
    }
    
    public void resetSearch() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getActiveChannel().resetSearch();
            }
        });
    }
    
    public void showMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (connectionDialog.isVisible()) {
                    JOptionPane.showMessageDialog(connectionDialog, message);
                }
                else {
                    printLine(message);
                }
            }
        });
    }
    
    /**
     * Outputs a line to the debug window
     * 
     * @param line 
     */
    public void printDebug(final String line) {
        if (SwingUtilities.isEventDispatchThread()) {
            debugWindow.printLine(line);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    debugWindow.printLine(line);
                }
            });
        }
    }
    
    /**
     * Outputs a line to the debug window
     * 
     * @param line 
     */
    public void printDebugIrc(final String line) {
        if (SwingUtilities.isEventDispatchThread()) {
            debugWindow.printLineIrc(line);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    debugWindow.printLineIrc(line);
                }
            });
        }
    }
    
    // User stuff
    
    /**
     * Adds a user to a channel, adding to the userlist
     * 
     * @param channel
     * @param user 
     */
    public void addUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel c = channels.getChannel(channel);
                c.addUser(user);
                if (channels.getActiveChannel() == c) {
                    state.update();
                }
            }
        });
    }
    
    /**
     * Removes a user from a channel, removing from the userlist
     * 
     * @param channel
     * @param user 
     */
    public void removeUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Channel c = channels.getChannel(channel);
                c.removeUser(user);
                if (channels.getActiveChannel() == c) {
                    state.update();
                }
            }
        });
    }
    
    /**
     * Updates a user on the given channel.
     * 
     * @param channel
     * @param user 
     */
    public void updateUser(final String channel, final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                channels.getChannel(channel).updateUser(user);
                state.update();
            }
        });
    }
    
    /**
     * Clears the userlist on all channels
     */
    public void clearUsers() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Enumeration e = channels.elements();
                while (e.hasMoreElements()) {
                    Channel panel = (Channel)e.nextElement();
                    panel.clearUsers();
                }
            }
        });
    }

    public void updateChannelInfo() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateChannelInfoDialog();
           }
        });
    }
    
    public void updateState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                state.update();
                //client.testHotkey();
            }
        });
    }
    
    private class StateUpdater {
        /**
         * Saves when the state was last setd, so the delay can be measured.
         */
        private long stateLastUpdated = 0;
        /**
         * Update state no faster than this amount of milliseconds.
         */
        private static final int UPDATE_STATE_DELAY = 100;

        /**
         * Updates the title and other things based on the current state
         */
        protected void update() {
            /*
             * Only set at most once every UPDATE_STATE_DELAY milliseconds.
             * This prevents flickering of the titlebar when a lot of updates
             * happen, for example when a lot of joins/parts happen at once.
             *
             * Of course the info might not be up-to-date this way, when the
             * last set is skipped because it came to close to the previous.
             * The UpdateTimer updates every 10 seconds so it shouldn't take too
             * long to be corrected (most of the time less than 5 seconds). This
             * also only affects the chatter count because it's the only one
             * that can be displayed somewhat accurately, but it's still not
             * very up-to-date anyway (from Twitch side).
             */
            if (System.currentTimeMillis() - stateLastUpdated < UPDATE_STATE_DELAY) {
                return;
            }
            stateLastUpdated = System.currentTimeMillis();

            int state = client.getState();

            requestFollowedStreams();
            updateMenuState(state);
            setTitle(makeTitle(state));
        }

        /**
         * Disables/enables menu items based on the current state.
         *
         * @param state
         */
        private void updateMenuState(int state) {
            if (state > Irc.STATE_OFFLINE || state == Irc.STATE_RECONNECTING) {
                menu.getMenuItem("connect").setEnabled(false);
            } else {
                menu.getMenuItem("connect").setEnabled(true);
            }

            if (state > Irc.STATE_CONNECTING || state == Irc.STATE_RECONNECTING) {
                menu.getMenuItem("disconnect").setEnabled(true);
            } else {
                menu.getMenuItem("disconnect").setEnabled(false);
            }
        }

        /**
         * Assembles the title of the window based on the current state and chat
         * and stream info.
         *
         * @param state
         * @return
         */
        private String makeTitle(int state) {
            Channel activeChannel = channels.getActiveChannel();
            String channel = activeChannel.getName();

            // Current state
            String stateText = "";

            if (state == Irc.STATE_CONNECTING) {
                stateText = "Connecting..";
            } else if (state == Irc.STATE_CONNECTED) {
                stateText = "Connecting...";
            } else if (state == Irc.STATE_REGISTERED) {
                if (channel.isEmpty()) {
                    stateText = "Connected";
                }
            } else if (state == Irc.STATE_OFFLINE) {
                stateText = "Not connected";
            } else if (state == Irc.STATE_RECONNECTING) {
                stateText = "Reconnecting..";
            }

            String title = stateText;

            // Stream Info
            if (!channel.isEmpty()) {
                if (!title.isEmpty()) {
                    title += " - ";
                }
                String numUsers = Helper.formatViewerCount(activeChannel.getNumUsers());
                StreamInfo streamInfo = getStreamInfo(activeChannel.getStreamName());
                if (streamInfo.isValid()) {
                    if (streamInfo.getOnline()) {
                        String numViewers = Helper.formatViewerCount(streamInfo.getViewers());
                        title += channel + " [" + numUsers + "|" + numViewers + "]";
                    } else {
                        title += channel + " [" + numUsers + "]";
                    }
                    title += " - " + streamInfo.getFullStatus();
                } else {
                    title += channel + " [" + numUsers + "]";
                }
            }

            title += " - Chatty";
            return title;
        }
    }
    
//    private class ViewerStats {
//        private long lastTime;
//        private static final long DELAY = 10*60*1000;
//        
//        public void makeViewerStats(String channel) {
//            long timePassed = System.currentTimeMillis() - lastTime;
//            if (timePassed > DELAY) {
//                StreamInfo info = getStreamInfo();
//                lastTime = System.currentTimeMillis();
//            }
//        }
//    }

    

    
    public void openConnectDialog(final String channelPreset) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                openConnectDialogInternal(channelPreset);
            }
        });
    }
    
    private void openConnectDialogInternal(String channelPreset) {
        updateConnectionDialog(channelPreset);
        connectionDialog.setLocationRelativeTo(this);
        connectionDialog.setVisible(true);
    }
    
    public void setEmoticons(final HashMap<Integer,HashSet<Emoticon>> newEmoticons) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.addEmoticons(newEmoticons);
            }
        });
    }
    
    public void setEmoticons2(final HashMap<String,HashSet<Emoticon>> newEmoticons) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                emoticons.addOtherEmoticons(newEmoticons);
            }
        });
    }
    
    /**
     * Chat icons received from the API.
     * 
     * @param channel
     * @param icons 
     */
    public void setChatIcons(final String channel, final ChatIcons icons) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (channels.isChannel(channel)) {
                    channels.getChannel(channel).setChatIcons(icons);
                    if (icons.type == ChatIcons.TWITCH) {
                        styleManager.setChatIcons(icons);
                        highlightedMessages.setChatIcons(icons);
                    }
                }
            }
        });
    }

    /* ###############
     * Get token stuff
     */
    
    public void webserverStarted() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tokenGetDialog.isVisible()) {
                    tokenGetDialog.ready();
                }
            }
        });
    }
    
    public void webserverError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (tokenGetDialog.isVisible()) {
                    tokenGetDialog.error(error);
                }
            }
        });
    }
    
    public void webserverTokenReceived(final String token) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tokenReceived(token);
            }
        });
    }
    
    private void tokenGetDialogClosed() {
        tokenGetDialog.setVisible(false);
        client.stopWebserver();
    }
    
    /**
     * Token received from the webserver.
     * 
     * @param token 
     */
    private void tokenReceived(String token) {
        client.settings.setString("token", token);
        if (tokenGetDialog.isVisible()) {
            tokenGetDialog.tokenReceived();
        }
        tokenDialog.update("",token);
        updateConnectionDialog(null);
        verifyToken(token);
    }
    
    /**
     * Verify the given Token. This sends a request to the TwitchAPI.
     * 
     * @param token 
     */
    private void verifyToken(String token) {
        client.api.verifyToken(token);
        tokenDialog.verifyingToken();
    }
    
    public void tokenVerified(final String token, final TokenInfo tokenInfo) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tokenVerifiedInternal(token, tokenInfo);
            }
        });
    }
    
    private String manuallyChangedToken = null;
    
    public void changeToken(final String token) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (token == null || token.isEmpty()) {
                    printSystem("You have to supply a token.");
                } else if (manuallyChangedToken != null) {
                    printSystem("You already have changed the token, please wait..");
                } else if (token.equals(client.settings.getString("token"))) {
                    printSystem("The token you entered is already set.");
                } else {
                    printSystem("Setting new token. Please wait..");
                    client.settings.setString("username", null);
                    manuallyChangedToken = token;
                    tokenReceived(token);
                }
            }
        });
    }
    
    /**
     * This does the main work when a response for verifying the token is
     * received from the Twitch API.
     * 
     * A Token can be verified manually by pressing the button or automatically
     * when a new Token was received by the webserver. So when this is called
     * the original source can be both.
     * 
     * The tokenGetDialog is closed if necessary.
     * 
     * @param token The token that was verified
     * @param username The usernamed that was received for this token. If this
     *      is null then an error occured, if it is empty then the token was
     *      invalid.
     */
    private void tokenVerifiedInternal(String token, TokenInfo tokenInfo) {
        // Stopping the webserver here, because it allows the /tokenreceived/
        // page to be delievered, because of the delay of verifying the token.
        // This should probably be solved better.
        client.stopWebserver();
        
        String result;
        String currentUsername = client.settings.getString("username");
        // Check if a new token was requested (the get token dialog should still
        // be open at this point) If this is wrong, it just displays the wrong
        // text, this shouldn't be used for something critical.
        boolean getNewLogin = tokenGetDialog.isVisible();
        boolean showInDialog = tokenDialog.isVisible();
        boolean changedTokenResponse = token == null
                ? manuallyChangedToken == null : token.equals(manuallyChangedToken);
        boolean valid = false;
        if (tokenInfo == null) {
            // An error occured when verifying the token
            if (getNewLogin) {
                result = "An error occured completing getting login data.";
            }
            else {
                result = "An error occured verifying login data.";
            }
        }
        else if (!tokenInfo.isTokenValid()) {
            // There was an answer when verifying the token, but it was invalid
            if (getNewLogin) {
                result = "Invalid token received when getting login data. Please "
                    + "try again.";
            }
            else if (changedTokenResponse) {
                result = "Invalid token entered. Please try again.";
            }
            else {
                result = "Login data invalid. Should probably remove it and request "
                        + "it again.";
            }
            if (!showInDialog && !changedTokenResponse) {
                showTokenWarning();
            }
            client.settings.setString("token", "");
        }
        else if (!tokenInfo.chat_access()) {
            result = "No chat access (required) with token.";
        }
        else {
            // Everything is fine, so save username and token
            valid = true;
            String username = tokenInfo.getUsername();
            client.settings.setString("username", username);
            client.settings.setString("token", token);
            tokenDialog.update(username, token);
            updateConnectionDialog(null);
            if (!currentUsername.isEmpty() && !username.equals(currentUsername)) {
                result = "Login verified and ready to connect (replaced '" +
                        currentUsername + "' with '" + username + "').";
            }
            else {
                result = "Login verified and ready to connect.";
            }
        }
        if (changedTokenResponse) {
            printLine(result);
            manuallyChangedToken = null;
        }
        setTokenScopes(tokenInfo);
        // Always close the get token dialog, if it's not open, nevermind ;)
        tokenGetDialog.setVisible(false);
        // Show result in the token dialog
        tokenDialog.tokenVerified(valid, result);
    }
    
    /**
     * Sets the token scopes in the settings based on the given TokenInfo.
     * 
     * @param info 
     */
    private void setTokenScopes(TokenInfo info) {
        if (info == null) {
            return;
        }
        if (info.isTokenValid()) {
            client.settings.setBoolean("token_editor", info.channel_editor());
            client.settings.setBoolean("token_commercials", info.channel_commercials());
            client.settings.setBoolean("token_user", info.user_read());
        }
        else {
            client.settings.setBoolean("token_editor", false);
            client.settings.setBoolean("token_commercials", false);
            client.settings.setBoolean("token_user", false);
        }
        updateTokenScopes();
    }
    
    /**
     * Updates the token scopes in the GUI based on the settings.
     */
    private void updateTokenScopes() {
        boolean commercials = client.settings.getBoolean("token_commercials");
        boolean editor = client.settings.getBoolean("token_editor");
        boolean user = client.settings.getBoolean("token_user");
        tokenDialog.updateAccess(editor, commercials, user);
        adminDialog.updateAccess(editor, commercials);
    }
    
    private void resetTokenScopes() {
        client.settings.setBoolean("token_commercials", false);
        client.settings.setBoolean("token_editor", false);
        client.settings.setBoolean("token_user", false);
    }
    
    public void showTokenWarning() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String message = "Login data (access token) was determined "
                        + "invalid and was removed.";
                String[] options = new String[]{"Close / Show Help","Just Close"};
                int result = GuiUtil.showNonAutoFocusOptionPane(MainGui.this, "Error",
                        message, JOptionPane.ERROR_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, options);
                if (result == 0) {
                    openHelp("help-troubleshooting.html","tokenDeleted");
                }
            }
        });
    }
    
    public void setChannelInfo(final String channel, final ChannelInfo info, final int result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setChannelInfo(channel, info, result);
            }
        });
    }
    
    public void putChannelInfo(String stream, ChannelInfo info) {
        client.api.putChannelInfo(stream, info, client.settings.getString("token"));
    }
    
    public void getChannelInfo(String channel) {
        client.api.getChannelInfo(channel);
    }
    
    public void performGameSearch(String search) {
        client.api.getGameSearch(search);
    }
    
    public String getActiveStream() {
        return channels.getActiveChannel().getStreamName();
    }
    
    /**
     * Saves the Set game favorites to the settings.
     * 
     * @param favorites 
     */
    public void setGameFavorites(Set<String> favorites) {
//        java.util.List<String> list = client.settings.getList("gamesFavorites");
//        list.clear();
//        list.addAll(favorites);
        client.settings.putList("gamesFavorites", new ArrayList(favorites));
    }
    
    /**
     * Returns a Set of game favorites retrieved from the settings.
     * 
     * @return 
     */
    public Set<String> getGameFavorites() {
        return new HashSet<>(client.settings.getList("gamesFavorites"));
    }
    
    public void gameSearchResult(final Set<String> games) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.gameSearchResult(games);
            }
        });
    }
    
    public void putChannelInfoResult(final int result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.setPutResult(result);
            }
        });
    }
    
    public void saveCommercialDelaySettings(boolean enabled, long delay) {
        client.settings.setBoolean("adDelay", enabled);
        client.settings.setLong("adDelayLength", delay);
    }
    
    private void loadCommercialDelaySettings() {
        boolean enabled = client.settings.getBoolean("adDelay");
        long length = client.settings.getLong("adDelayLength");
        adminDialog.updateCommercialDelaySettings(enabled, length);
    }
    
    public void runCommercial(String stream, int length) {
        client.runCommercial(stream, length);
    }
    
    public void commercialResult(final String stream, final String text, final int result) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                adminDialog.commercialResult(stream, text, result);
            }
        });
    }
    
    public void setCommercialHotkey(String hotkey) {
        client.setCommercialHotkey(hotkey);
    }

    /**
     * Get StreamInfo for the given stream, but also request it for all open
     * channels.
     * 
     * @param stream
     * @return 
     */
    public StreamInfo getStreamInfo(String stream) {
        Set<String> streams = new HashSet<>();
        for (Channel chan : channels.getChannels()) {
            streams.add(chan.getStreamName());
        }
        return client.api.getStreamInfo(stream, streams);
    }
    
    /**
     * Outputs the full title if the StreamInfo for this channel is valid.
     * 
     * @param channel 
     */
    public void printStreamInfo(final String channel) {
        final String stream = channel.replace("#", "");
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (client.settings.getBoolean("printStreamStatus")) {
                    StreamInfo info = getStreamInfo(stream);
                    if (info.isValid()) {
                        printLine(channel, "~" + info.getFullStatus() + "~");
                    }
                }
            }
        });
    }
    
    /**
     * Possibly request followed streams from the API, if enabled and access
     * was granted.
     */
    private void requestFollowedStreams() {
        if (client.settings.getBoolean("requestFollowedStreams") &&
                client.settings.getBoolean("token_user")) {
            client.api.getFollowedStreams(client.settings.getString("token"));
        }
    }

    private class MySettingChangeListener implements SettingChangeListener {
        /**
         * Since this can also be called from other threads, run in EDT if
         * necessary.
         *
         * @param setting
         * @param type
         * @param value
         */
        @Override
        public void settingChanged(final String setting, final int type, final Object value) {
            if (SwingUtilities.isEventDispatchThread()) {
                settingChangedInternal(setting, type, value);
            } else {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        settingChangedInternal(setting, type, value);
                    }
                });
            }
        }
        
        private void settingChangedInternal(String setting, int type, Object value) {
            if (type == Setting.BOOLEAN) {
                if (setting.equals("ontop")) {
                    setAlwaysOnTop((Boolean) value);
                } else if (setting.equals("ignoreJoinsParts")) {
                    if ((Boolean) value) {
                        client.clearUserList();
                    }
                } else if (setting.equals("highlightUsername")) {
                    updateHighlightSetUsernameHighlighted((Boolean) value);
                } else if (setting.equals("highlightNextMessages")) {
                    updateHighlightNextMessages();
                }
                loadMenuSetting(setting);
            }

            if (StyleManager.settingNames.contains(setting)) {
                styleManager.refresh();
                channels.refreshStyles();
                highlightedMessages.refreshStyles();
                //menu.setForeground(styleManager.getColor("foreground"));
                //menu.setBackground(styleManager.getColor("background"));
            }
            if (type == Setting.STRING) {

                if (setting.equals("timeoutButtons")) {
                    userInfoDialog.setTimeoutButtonsDef((String) value);
                }
            }
            if (type == Setting.LIST) {
                if (setting.equals("highlight")) {
                    updateHighlight();
                }
            }
            if (setting.equals("channelFavorites") || setting.equals("channelHistory")) {
                // TOCONSIDER: This means that it is updated twice in a row when an action
                // requires both settings to be changed
                updateFavoritesDialogWhenVisible();
            }
            if (setting.equals("liveStreamsSorting")) {
                updateLiveStreamsDialog();
            }
            if (setting.equals("historyRange")) {
                updateHistoryRange();
            }
            Set<String> notificationSettings = new HashSet<>(Arrays.asList(
                "nScreen", "nPosition", "nDisplayTime", "nMaxDisplayTime",
                "nMaxDisplayed", "nMaxQueueSize", "nActivity", "nActivityTime"));
            if (notificationSettings.contains(setting)) {
                updateNotificationSettings();
            }
            if (setting.equals("spamProtection")) {
                client.setLinesPerSeconds((String)value);
            }
            if (setting.equals("urlPrompt")) {
                UrlOpener.setPrompt((Boolean)value);
            }
        }
    }

    public WindowListener getWindowListener() {
        return windowListener;
    }
    
    private class MyWindowListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (e.getSource() == tokenGetDialog) {
                tokenGetDialogClosed();
            }
        }
    }
    
    private class MainWindowListener extends WindowAdapter {
        
        @Override
        public void windowStateChanged(WindowEvent e) {
            if (e.getComponent() == MainGui.this) {
                saveState(e.getComponent());
            }
        }

        @Override
        public void windowClosing(WindowEvent evt) {
            if (evt.getComponent() == MainGui.this) {
                exit();
            }
        }
    }
    
    /**
     * Display an error dialog with the option to quit or continue the program
     * and to report the error.
     *
     * @param error The error as a LogRecord
     * @param previous Some previous debug messages as LogRecord, to provide
     * context
     */
    public void error(final LogRecord error, final LinkedList<LogRecord> previous) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int result = errorMessage.show(error, previous);
                if (result == ErrorMessage.QUIT) {
                    exit();
                }
            }
        });
    }
    
    /**
     * Exit the program.
     */
    private void exit() {
        client.exit();
    }
    
    public void cleanUp() {
        if (SwingUtilities.isEventDispatchThread()) {
            setVisible(false);
            dispose();
        }
    }
    
}
