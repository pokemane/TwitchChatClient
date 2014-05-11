
package chatty.gui.components.settings;

import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Main settings dialog class that provides ways to add different kinds of
 * settings which are then automatically loaded and saved.
 * 
 * @author tduva
 */
public class SettingsDialog extends JDialog implements ActionListener {
    
    private final static Logger LOGGER = Logger.getLogger(SettingsDialog.class.getName());
    
    private final JButton ok = new JButton("Save");
    private final JButton cancel = new JButton("Cancel");
    
    private final Set<String> restartRequiredDef = new HashSet<>(Arrays.asList(
            "capitalizedNames", "ffz", "ffzModIcon", "nod3d", "noddraw",
            "userlistWidth", "tabOrder"));
    
    private boolean restartRequired = false;
    
    private static final String RESTART_REQUIRED_INFO = "<html><body style='width: 280px'>One or more settings "
            + "you have changed require a restart of Chatty to take full effect.";
    
    private final HashMap<String,StringSetting> stringSettings = new HashMap<>();
    private final HashMap<String,LongSetting> longSettings = new HashMap<>();
    private final HashMap<String,BooleanSetting> booleanSettings = new HashMap<>();
    private final HashMap<String,ListSetting> listSettings = new HashMap<>();
    
    private final Settings settings;
    private final MainGui owner;
    
    private final NotificationSettings notificationSettings;
    private final UsercolorSettings usercolorSettings;

    private static final String PANEL_MAIN = "Main";
    private static final String PANEL_IMAGES = "Emotes/Icons";
    private static final String PANEL_COLORS = "Colors";
    private static final String PANEL_HIGHLIGHT = "Highlight";
    private static final String PANEL_HISTORY = "History";
    private static final String PANEL_SOUND = "Sounds";
    private static final String PANEL_NOTIFICATIONS = "Notifications";
    private static final String PANEL_USERCOLORS = "Usercolors";
    private static final String PANEL_LOG = "Logging";
    private static final String PANEL_WINDOW = "Window";
    private static final String PANEL_OTHER = "Other";
    private static final String PANEL_ADVANCED = "Advanced";
    
    private String currentlyShown;
    
    final CardLayout cardManager;
    final JPanel cards;
    
    private final LinkLabelListener settingsHelpLinkLabelListener;
    
    private static final String[] MENU = {
        PANEL_MAIN, PANEL_IMAGES, PANEL_COLORS, PANEL_USERCOLORS, PANEL_HIGHLIGHT, PANEL_HISTORY,
        PANEL_SOUND, PANEL_NOTIFICATIONS, PANEL_LOG, PANEL_WINDOW, PANEL_OTHER, PANEL_ADVANCED};

    public SettingsDialog(final MainGui owner, final Settings settings) {
        super(owner,"Settings",true);
        setResizable(false);
        
        settingsHelpLinkLabelListener = new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                owner.openHelp("help-settings.html", ref);
            }
        };
        
        // Save references
        this.owner = owner;
        this.settings = settings;

        /*
         * Layout
         */
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        /*
         * Add to Tabs
         */
        //JTabbedPane tabs = new JTabbedPane();
        final JList<String> selection = new JList<>(MENU);
        selection.setSelectedIndex(0);
        selection.setSize(200, 200);
        Font defaultFont = selection.getFont();
        selection.setFont(new Font(defaultFont.getFontName(), Font.BOLD, 12));
        selection.setFixedCellHeight(20);
        selection.setFixedCellWidth(100);
        selection.setBorder(BorderFactory.createEtchedBorder());
//        selection.setBackground(getBackground());
//        selection.setForeground(getForeground());
        
        gbc = makeGbc(0,0,1,1);
        gbc.insets = new Insets(10,10,10,3);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 1;
        add(selection, gbc);
        
        cardManager = new CardLayout();
        cards = new JPanel(cardManager);
        cards.add(new MainSettings(this), PANEL_MAIN);
        cards.add(new ImageSettings(this), PANEL_IMAGES);
        cards.add(new ColorSettings(this), PANEL_COLORS);
        cards.add(new HighlightSettings(this), PANEL_HIGHLIGHT);
        cards.add(new HistorySettings(this), PANEL_HISTORY);
        cards.add(new SoundSettings(this), PANEL_SOUND);
        notificationSettings = new NotificationSettings(this);
        cards.add(notificationSettings, PANEL_NOTIFICATIONS);
        usercolorSettings = new UsercolorSettings(this);
        cards.add(usercolorSettings, PANEL_USERCOLORS);
        cards.add(new LogSettings(this), PANEL_LOG);
        cards.add(new WindowSettings(this), PANEL_WINDOW);
        cards.add(new OtherSettings(this), PANEL_OTHER);
        cards.add(new AdvancedSettings(this), PANEL_ADVANCED);
        
        currentlyShown = PANEL_MAIN;
        selection.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                showPanel(selection.getSelectedValue());
            }
        });
        
        
        gbc = makeGbc(1,0,2,1);
        add(cards, gbc);
        
        //tabs.setTabPlacement(JTabbedPane.LEFT);
        //tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
//        tabs.add(new MainSettings(this), "Main");
//        tabs.add(new ColorSettings(this), "Colors");
//        tabs.add(new HighlightSettings(this), "Highlight");
//        tabs.add(new HistorySettings(this), "History");
//        tabs.add(new SoundSettings(this), "Sound");
//        tabs.add(new NotificationSettings(this), "Tray");
//        tabs.add(new OtherSettings(this), "Other");
        //tabs.add(null, "Messages");
        //tabs.add(new JLabel("test"), "Logs");
        
        gbc = makeGbc(0,2,1,1);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0,10,0,0);
        add(new LinkLabel("[maeh:muh Help]", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                owner.openHelp("help-settings.html", currentlyShown);
            }
        }), gbc);
        
        // Buttons
        ok.setMnemonic(KeyEvent.VK_S);
        gbc = makeGbc(1,2,1,1);
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(4,3,8,8);
        add(ok,gbc);
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(2,2,1,1);
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4,3,8,8);
        add(cancel,gbc);
        
        // Listeners
        ok.addActionListener(this);
        cancel.addActionListener(this);

        pack();
    }
    
    /**
     * Opens the settings dialog
     */
    public void showSettings() {
        showSettings(null, null);
    }
    
    public void showSettings(String action, String parameter) {
        loadSettings();
        notificationSettings.setUserReadPermission(settings.getBoolean("token_user"));
        setLocationRelativeTo(owner);
        if (action != null) {
            if (action.equals("editUsercolorItem")) {
                editUsercolorItem(parameter);
            }
        }
        setVisible(true);
    }
    
    private void editUsercolorItem(final String item) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                showPanel(PANEL_USERCOLORS);
                usercolorSettings.editItem(item);
            }
        });
    }

    private void showPanel(String showCard) {
        cardManager.show(cards, showCard);
        currentlyShown = showCard;
    }
    
    /**
     * Loads all settings from the settings object
     */
    private void loadSettings() {
        loadStringSettings();
        loadNumericSettings();
        loadBooleanSettings();
        loadListSettings();
        usercolorSettings.setData(owner.getUsercolorData());
    }
    
    /**
     * Loads all settings of type String
     */
    private void loadStringSettings() {
        for (String settingName : stringSettings.keySet()) {
            StringSetting setting = stringSettings.get(settingName);
            String value = settings.getString(settingName);
            setting.setSettingValue(value);
        }
    }
    
    /**
     * Loads all settings of type Integer
     */
    private void loadNumericSettings() {
        for (String settingName : longSettings.keySet()) {
            LongSetting setting = longSettings.get(settingName);
            Long value = settings.getLong(settingName);
            setting.setSettingValue(value);
        }
    }
    
    /**
     * Loads all settings of type Boolean
     */
    private void loadBooleanSettings() {
        for (String settingName : booleanSettings.keySet()) {
            BooleanSetting setting = booleanSettings.get(settingName);
            Boolean value = settings.getBoolean(settingName);
            setting.setSettingValue(value);
        }
    }
    
    private void loadListSettings() {
        for (String settingName : listSettings.keySet()) {
            ListSetting setting = listSettings.get(settingName);
            List data = settings.getList(settingName);
            setting.setSettingValue(data);
        }
    }
    
    /**
     * Saves settings into the settings object
     */
    private void saveSettings() {
        restartRequired = false;
        saveStringSettings();
        saveBooleanSettings();
        saveIntegerSettings();
        saveListSettings();
        owner.setUsercolorData(usercolorSettings.getData());
        if (restartRequired) {
            JOptionPane.showMessageDialog(this, RESTART_REQUIRED_INFO, "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Saves all settings of type String
     */
    private void saveStringSettings() {
        for (String settingName  : stringSettings.keySet()) {
            StringSetting setting = stringSettings.get(settingName);
            String value = setting.getSettingValue();
            if (settings.setString(settingName,value) == Setting.CHANGED) {
                changed(settingName);
            }
        }
    }
    
    /**
     * Saves all settings of type Boolean
     */
    private void saveBooleanSettings() {
        for (String settingName : booleanSettings.keySet()) {
            BooleanSetting setting = booleanSettings.get(settingName);
            if (settings.setBoolean(settingName, setting.getSettingValue()) == Setting.CHANGED) {
                changed(settingName);
            }
        }
    }
    
    /**
     * Saves all settings of type Integer.
     * 
     * Parses the String of the JTextFields into an Integer and only saves if
     * it succeeds
     */
    private void saveIntegerSettings() {
        for (String settingName : longSettings.keySet()) {
            LongSetting setting = longSettings.get(settingName);
            Long value = setting.getSettingValue();
            if (value != null) {
                if (settings.setLong(settingName, setting.getSettingValue()) == Setting.CHANGED) {
                    changed(settingName);
                }
            } else {
                LOGGER.warning("Invalid number format for setting "+settingName);
            }
        }
    }
    
    private void changed(String settingName) {
        if (restartRequiredDef.contains(settingName)) {
            restartRequired = true;
        }
    }
    
    private void saveListSettings() {
        for (String settingName : listSettings.keySet()) {
            ListSetting setting = listSettings.get(settingName);
            settings.putList(settingName, setting.getSettingValue());
//            settingsgetList2t(settingName).clear();
//            settinggetList2st(settingName).addAll(setting.getSettingValue());
            settings.setSettingChanged(settingName);
        }
    }
    
    
    protected GridBagConstraints makeGbc(int x, int y, int w, int h) {
        return makeGbc(x, y, w, h, GridBagConstraints.CENTER);
    }
    
    protected GridBagConstraints makeGbc(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(4,5,4,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected GridBagConstraints makeGbcCloser(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(2,5,2,5);
        gbc.anchor = anchor;
        return gbc;
    }
    
    protected JCheckBox addBooleanSetting(String name, String description, String tooltipText) {
        SimpleBooleanSetting result = new SimpleBooleanSetting(description, tooltipText);
        booleanSettings.put(name,result);
        return result;
    }
    
    protected void setBooleanSetting(String name, Boolean value) {
        if (booleanSettings.containsKey(name)) {
            booleanSettings.get(name).setSettingValue(value);
        }
    }
    
    protected Boolean getBooleanSetting(String name) {
        if (booleanSettings.containsKey(name)) {
            return booleanSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    protected ComboStringSetting addComboStringSetting(String name, int size, boolean editable, String[] choices) {
        ComboStringSetting result = new ComboStringSetting(choices);
        result.setEditable(editable);
        stringSettings.put(name, result);
        return result;
    }
    
//    protected JTextField addStringSetting(String name, int size, boolean editable) {
//        JTextField result = new JTextField(size);
//        result.setEditable(editable);
//        stringSettings.put(name,result);
//        return result;
//   }
    
    protected StringSetting addStringSetting(String settingName, StringSetting setting) {
        stringSettings.put(settingName, setting);
        return setting;
    }
    
    protected JTextField addSimpleStringSetting(String settingName, int size, boolean editable) {
        SimpleStringSetting s = new SimpleStringSetting(size, editable);
        addStringSetting(settingName, s);
        return s;
    }
    
    /**
     * Changes the String setting with the given name to the given value. Does
     * nothing if a setting with this name doesn't exist.
     * 
     * @param name The name of the setting
     * @param value The new value
     */
    protected void setStringSetting(String name, String value) {
        if (stringSettings.containsKey(name)) {
            stringSettings.get(name).setSettingValue(value);
        }
    }
    
    /**
     * Retrieves the value of the String setting with the given name.
     * 
     * @param name The name of the setting
     * @return The value of the setting or null if it doesn't exist
     */
    protected String getStringSetting(String name) {
        if (stringSettings.containsKey(name)) {
            return stringSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    /**
     * Adds an Integer setting.
     * 
     * @param name The name of the setting
     * @param size The size of the editbox
     * @param editable Whether the value can be changed by the user
     * @return The JTextField used for this setting
     */
    protected JTextField addSimpleLongSetting(String name, int size, boolean editable) {
        SimpleLongSetting result = new SimpleLongSetting(size, editable);
        addLongSetting(name, result);
        return result;
    }
    
    protected void addLongSetting(String settingName, LongSetting setting) {
        longSettings.put(settingName, setting);
    }
    
    /**
     * Changes the value of an Integer setting to the given value. Does nothing
     * if the setting doesn't exist.
     * 
     * @param name The name of the setting
     * @param value The new value
     */
    protected void setLongSetting(String name, Long value) {
        if (longSettings.containsKey(name)) {
            longSettings.get(name).setSettingValue(value);
        }
    }
    
    /**
     * Retrieves the Integer value for the given Integer setting. Returns null
     * if value couldn't be parsed as an Integer or if the setting doesn't
     * exist.
     * 
     * @param name
     * @return 
     */
    protected Long getLongSetting(String name) {
        if (longSettings.containsKey(name)) {
            return longSettings.get(name).getSettingValue();
        }
        return null;
    }
    
    /**
     * Adds a List setting.
     * 
     * @param name
     * @param width
     * @param height
     * @return 
     */
    protected ListSelector addListSetting(String name, int width, int height) {
        ListSelector result = new ListSelector();
        result.setPreferredSize(new Dimension(width, height));
        listSettings.put(name, result);
        return result;
    }
    
    protected void clearHistory() {
        owner.clearHistory();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ok) {
            saveSettings();
            setVisible(false);
        }
        else if (e.getSource() == cancel) {
            setVisible(false);
        }
    }
    
    protected LinkLabelListener getLinkLabelListener() {
        return owner.getLinkLabelListener();
    }
    
    protected LinkLabelListener getSettingsHelpLinkLabelListener() {
        return settingsHelpLinkLabelListener;
    }
    
}