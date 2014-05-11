
package chatty;

import chatty.gui.HtmlColors;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class UsercolorManager {
    
    private final Settings settings;
    private volatile List<UsercolorItem> data;
    
    public UsercolorManager(Settings settings) {
        this.settings = settings;
        loadFromSettings();
        // TEST
//        data = new ArrayList<>();
//        for (int i=0;i<10000;i++) {
//            data.add(new UsercolorItem("user"+i, Color.BLACK));
//        }
    }
    
    private void loadFromSettings() {
        List<String> l = new LinkedList<>();
        settings.getList("usercolors", l);
//        synchronized(settings.LOCK) {
//            List<String> settingList = settings.getList2("usercolors");
//            l.addAll(settingList);
//        }
        List<UsercolorItem> loadedData = new ArrayList<>();
        for (String entry : l) {
            int splitAt = entry.lastIndexOf(",");
            if (splitAt > 0 && entry.length() > splitAt+1) {
                String id = entry.substring(0, splitAt);
                Color color = HtmlColors.decode(entry.substring(splitAt + 1));
                loadedData.add(new UsercolorItem(id, color));
            }
        }
        data = loadedData;
        
//        synchronized(settingMap) {
//            Map<String, String> m = settingMap;
//            List<UsercolorItem> loadedData = new ArrayList<>();
//            for (Map.Entry<String, String> entry : m.entrySet()) {
//                String id = entry.getKey();
//                Color color = HtmlColors.decode(entry.getValue());
//                loadedData.add(new UsercolorItem(id, color));
//            }
//            data = loadedData;
//        }
    }
    
    /**
     * Copy the current data to the settings.
     */
    private void saveToSettings() {
        List<String> dataToSave = new LinkedList<>();
        for (UsercolorItem item : data) {
            dataToSave.add(item.getId()+","+HtmlColors.getColorString(item.getColor()));
        }
//        synchronized (settings.LOCK) {
//            List<String> settingList = settingsgetList2t("usercolors");
//            settingList.clear();
//            settingList.addAll(dataToSave);
//        }
        settings.putList("usercolors", dataToSave);
    }
    
    /**
     * Gets the current data.
     * 
     * @return An ordered list of {@link UsercolorItem}s.
     */
    public synchronized List<UsercolorItem> getData() {
        return new ArrayList<>(data);
    }
    
    /**
     * Sets new data and copies it to the settings as well.
     * 
     * @param newData A list of ordered {@link UsercolorItem}s.
     */
    public synchronized void setData(List<UsercolorItem> newData) {
        data = new ArrayList<>(newData);
        saveToSettings();
    }
    
    /**
     * Returns the color for this user, or null if no items matched this user.
     * 
     * @param user
     * @return 
     */
    public synchronized Color getColor(User user) {
        if (data == null || !settings.getBoolean("customUsercolors")) {
            return null;
        }
        for (UsercolorItem item : data) {
            if (item.type == UsercolorItem.TYPE_COLOR) {
                if (item.idColor.equals(user.getPlainColor())) {
                    return item.color;
                }
            } else if (item.type == UsercolorItem.TYPE_NAME) {
                if (item.id.equalsIgnoreCase(user.getNick())) {
                    return item.color;
                }
            } else if (item.type == UsercolorItem.TYPE_STATUS) {
                if (       (item.id.equals("$mod") && user.isModerator())
                        || (item.id.equals("$sub") && user.isSubscriber())
                        || (item.id.equals("$turbo") && user.hasTurbo())
                        || (item.id.equals("$admin") && user.isAdmin())
                        || (item.id.equals("$broadcaster") && user.isBroadcaster())
                        || (item.id.equals("$staff") && user.isStaff())) {
                    return item.color;
                }
            } else if (item.type == UsercolorItem.TYPE_CATEGORY) {
                if (user.hasCategory(item.category)) {
                    return item.color;
                }
            } else if (item.type == UsercolorItem.TYPE_ALL) {
                return item.color;
            }
        }
        return null;
    }
    
}
