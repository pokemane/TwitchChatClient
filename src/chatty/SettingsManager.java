
package chatty;

import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class SettingsManager {
    
    private static final Logger LOGGER = Logger.getLogger(SettingsManager.class.getName());
    
    private final Settings settings;
    
    private final String[] debugSettings = {
        "server",
        "port",
        "ontop",
        "dontSaveSettings",
        "usePassword",
        "ignoreJoinsParts",
        "debugLogIrc",
        "showJoinsParts",
        "showBanMessages",
        "emoticonsEnabled",
        "showBanMessages",
        "saveChannelHistory",
        "historyClear",
        "autoScroll",
        "bufferSize"
    };
    
    public SettingsManager(Settings settings) {
        this.settings = settings;
    }
    
    /**
     * Defines what settings there are and their default values.
     */
    void defineSettings() {
        
        // Additional files (in addition to the default file)
        String loginFile = Chatty.getUserDataDirectory()+"login";
        String historyFile = Chatty.getUserDataDirectory()+"favoritesAndHistory";
        String statusPresetsFile = Chatty.getUserDataDirectory()+"statusPresets";
        
        settings.addFile(loginFile);
        settings.addFile(historyFile);
        settings.addFile(statusPresetsFile);
        
        // Global
        settings.addBoolean("ontop", false);
        settings.addBoolean("dontSaveSettings",false);
        settings.addString("timeoutButtons","5,2m,10m,30m");
        
        // Connecting/Login data
        settings.addString("serverDefault", "irc.twitch.tv");
        settings.addString("portDefault", "6667,80");
        // Seperate settings for commandline/temp so others can be saved
        settings.addString("server", "", false);
        settings.addString("port", "", false);
        
        settings.addBoolean("tc3", false);
        
        settings.addString("channel", "");
        settings.addString("username", "");
        settings.setFile("username", loginFile);
        settings.addString("password", "", false);
        settings.addBoolean("connectOnStartup", false, false);
        settings.addString("token","");
        settings.setFile("token", loginFile);
        // Don't save setting, login with password isn't possible anymore
        settings.addBoolean("usePassword", false, false);
        // Scopes
        settings.addBoolean("token_editor", false);
        settings.setFile("token_editor", loginFile);
        settings.addBoolean("token_commercials", false);
        settings.setFile("token_commercials", loginFile);
        settings.addBoolean("token_user", false);
        settings.setFile("token_user", loginFile);
        
        // Chat messages
        settings.addBoolean("ignoreJoinsParts",false);
        settings.addBoolean("showJoinsParts", false);
        settings.addBoolean("showModMessages", true);
        settings.addBoolean("debugLogIrc", false);
        settings.addBoolean("showBanMessages", false);
        settings.addBoolean("deleteMessages", false);
        settings.addString("deletedMessagesMode", "keepShortened");
        settings.addLong("deletedMessagesMaxLength", 50);
        settings.addLong("bufferSize", 250);
        settings.addBoolean("twitchnotifyAsInfo", true);
        settings.addBoolean("printStreamStatus", true);
        
        // Chat appearance
        settings.addBoolean("emoticonsEnabled",true);
        settings.addBoolean("usericonsEnabled",true);
        settings.addString("font","Consolas");
        settings.addLong("fontSize",14);
        settings.addLong("lineSpacing", 3);
        //settings.addBoolean("timestampEnabled",true);
        settings.addString("timestamp","[HH:mm]");
        settings.addBoolean("capitalizedNames", false);
        settings.addBoolean("ffz", true);
        settings.addBoolean("ffzModIcon", true);
        settings.addString("tabOrder", "normal");
        
        
        // Colors
        settings.addString("foregroundColor","#111111");
        settings.addString("backgroundColor","#FAFAFA");
        settings.addString("infoColor","#001480");
        settings.addString("compactColor","#A0A0A0");
        settings.addString("inputBackgroundColor","#FFFFFF");
        settings.addString("inputForegroundColor","#000000");
        settings.addString("highlightColor","#FF0000");
        settings.addString("searchResultColor", "LightYellow");
        
        
        // History and Favorites
        settings.addMap("channelHistory",new TreeMap(), Setting.LONG);
        settings.setFile("channelHistory", historyFile);
        settings.addList("channelFavorites", new ArrayList(), Setting.STRING);
        settings.setFile("channelFavorites", historyFile);
        settings.addList("gamesFavorites",new ArrayList(), Setting.STRING);
        settings.setFile("gamesFavorites", historyFile);
        settings.addLong("channelHistoryKeepDays", 30);
        settings.addBoolean("saveChannelHistory", true);
        settings.addBoolean("historyClear", true);
        
        // Commercials
        settings.addString("commercialHotkey","");
        settings.addBoolean("adDelay", false);
        settings.addLong("adDelayLength", 300);
        
        // Other
        settings.addBoolean("channelsWarning", true);
        settings.addBoolean("autoScroll", true);
        settings.addLong("versionLastChecked", 0);
        settings.addBoolean("checkNewVersion", true);
        settings.addString("liveStreamsSorting", "recent");
        settings.addLong("historyRange", 0);
        settings.addString("spamProtection", "18/30");
        settings.addLong("userlistWidth", 120);
        
        settings.addString("currentVersion", "");
        
        settings.addBoolean("urlPrompt", true);
        
        // Window
        settings.addBoolean("maximized", false);
        settings.addBoolean("nod3d", true);
        settings.addBoolean("noddraw", false);
        
        settings.addMap("windows", new HashMap<>(), Setting.STRING);
        settings.addLong("restoreMode", 1);
        
        // Highlight
        settings.addList("highlight",new ArrayList(), Setting.STRING);
        settings.addBoolean("highlightEnabled", true);
        settings.addBoolean("highlightUsername", true);
        settings.addBoolean("highlightOwnText", false);
        settings.addBoolean("highlightNextMessages", false);
        
        // Sounds
        settings.addBoolean("sounds", false);
        settings.addString("highlightSound", "off");
        settings.addString("highlightSoundFile", "ding.wav");
        settings.addLong("highlightSoundDelay", 15);
        settings.addLong("soundDelay", 15);
        settings.addLong("highlightSoundVolume", 100);
        settings.addString("statusSound","off");
        settings.addString("statusSoundFile","dingdong.wav");
        settings.addLong("statusSoundVolume",100);
        settings.addLong("statusSoundDelay", 15);
        settings.addString("messageSound","off");
        settings.addString("messageSoundFile","dingdong.wav");
        settings.addLong("messageSoundVolume",100);
        settings.addLong("messageSoundDelay", 5);
        settings.addString("joinPartSound","off");
        settings.addString("joinPartSoundFile","dingdong.wav");
        settings.addLong("joinPartSoundVolume",100);
        settings.addLong("joinPartSoundDelay", 10);
        
        // Notifications
        settings.addString("highlightNotification", "either");
        settings.addString("statusNotification", "either");
        settings.addBoolean("requestFollowedStreams", true);
        
        settings.addBoolean("useCustomNotifications", true);
        
        settings.addLong("nScreen", -1);
        settings.addLong("nPosition", 3);
        settings.addLong("nDisplayTime", 10);
        settings.addLong("nMaxDisplayTime", 60*30);
        settings.addLong("nMaxDisplayed", 4);
        settings.addLong("nMaxQueueSize", 4);
        settings.addBoolean("nActivity", false);
        settings.addLong("nActivityTime", 10);
        
        settings.addLong("v0.5", 0);
        settings.addBoolean("tips", true);
        settings.addLong("lastTip", 0);
        
        // Logging
        settings.addString("logMode", "off");
        settings.addBoolean("logMod", true);
        settings.addBoolean("logJoinPart", false);
        settings.addBoolean("logBan", true);
        settings.addBoolean("logSystem", false);
        settings.addBoolean("logInfo", true);
        settings.addBoolean("logViewerstats", true);
        settings.addBoolean("logViewercount", false);
        settings.addList("logWhitelist",new ArrayList(), Setting.STRING);
        settings.addList("logBlacklist",new ArrayList(), Setting.STRING);
        
        settings.addBoolean("debugCommands", false, false);
        
        settings.addBoolean("customUsercolors", false);
        settings.addList("usercolors", new LinkedList(), Setting.STRING);
        
        settings.addString("abCommandsChannel", "");
        settings.addString("abCommands", "add,set,remove");
        
        settings.addList("statusPresets", new ArrayList(), Setting.LIST);
        settings.setFile("statusPresets", statusPresetsFile);
        
        settings.addBoolean("saveStatusHistory", true);
        settings.addBoolean("statusHistoryClear", true);
        settings.addLong("statusHistoryKeepDays", 30);
        
        //settings.getList2("highlight").add(".*abc.*");
        //settings.getMap2("channelHistory").put("#joshimuz","test");
    }
    
    /**
     * Tries to load the settings from file.
     */
    void loadSettingsFromFile() {
        settings.loadSettingsFromJson();
    }
    
    /**
     * Goes through the commandline options and sets the settings accordingly.
     * 
     * Commandline options consist of key=value pairs, although empty values
     * are possible.
     * 
     * @param args Map with commandline settings, key=value pairs
     */
    void loadCommandLineSettings(HashMap<String, String> args) {
        for (String key : args.keySet()) {
            // Go through all commandline options
            String value = args.get(key);
            if (key == null) {
                continue;
            }
            switch (key) {
                case "user":
                    settings.setString("username", value);
                    break;
                case "channel":
                    settings.setString("channel", value);
                    break;
                case "connect":
                    settings.setBoolean("connectOnStartup", true);
                    break;
                case "token":
                    if (!value.isEmpty()) {
                        settings.setString("token", value);
                    }
                    settings.setBoolean("usePassword", false);
                    break;
                case "password":
                    settings.setString("password", value);
                    settings.setBoolean("usePassword", true);
                    break;
                case "ds":
                    settings.setBoolean("dontSaveSettings", true);
                    break;
                case "server":
                    settings.setString("server", value);
                    break;
                case "port":
                    settings.setString("port", value);
                    break;
            }
        }
    }
    
    public void debugSettings() {
        StringBuilder result = new StringBuilder("Settings: ");
        boolean first = true;
        for (String setting : debugSettings) {
            if (!first) {
                result.append(", ");
            } else {
                first = false;
            }
            result.append(setting);
            result.append(":");
            result.append(settings.settingValueToString(setting));
        }
        LOGGER.info(result.toString());
    }
    
}
