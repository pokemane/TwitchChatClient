
package chatty.gui;

import chatty.util.settings.Settings;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class WindowStateManager {
    
    private static final String SETTING = "windows";
    private static final String MODE_SETTING = "restoreMode";
    
    public static final int DONT_RESTORE = 0;
    public static final int RESTORE_MAIN = 1;
    public static final int RESTORE_ALL = 2;
    public static final int RESTORE_ON_START = 3;
    public static final int REOPEN_ON_START = 4;
    
    private final HashMap<Window, StateItem> windows = new HashMap<>();
    private final Set<Window> setLocation = new HashSet<>();
    private final Settings settings;
    private final Window defaultParent;
    private Window primaryWindow;

    public WindowStateManager(Window defaultParent, Settings settings) {
        this.settings = settings;
        this.defaultParent = defaultParent;
    }
    
    public void addWindow(Window window, String id, boolean saveSize, boolean reopen) {
        windows.put(window, new StateItem(id, saveSize, reopen));
    }
    
    public void setPrimaryWindow(Window window) {
        this.primaryWindow = window;
    }
    
    public void saveWindowStates() {
        for (Window window : windows.keySet()) {
            saveWindowState(window);
        }
    }
    
    public void loadWindowStates() {
        for (Window window : windows.keySet()) {
            loadWindowState(window);
        }
    }
    
    private void saveWindowState(Window window) {
        // Only save state when window state was actually set during this
        // this session at least once
        if (!setLocation.contains(window) && window != primaryWindow) {
            return;
        }
        // Should still save whether it was open
        StateItem item = windows.get(window);
        Point location = window.getLocation();
        String state = location.x+","+location.y;
        Dimension size = window.getSize();
        state += ";"+size.width+","+size.height;
        state += ";"+(window.isVisible() ? "1" : "0");
        settings.mapPut(SETTING, item.id, state);
    }
    
    private void loadWindowState(Window window) {
        StateItem item = windows.get(window);
        String state = (String)settings.mapGet(SETTING, item.id);
        if (state == null) {
            return;
        }
        String[] states = state.split(";");
        if (states.length < 3) {
            return;
        }
        
        if (mode() >= RESTORE_ON_START
                || (mode() >= RESTORE_MAIN && window == primaryWindow)) {
            IntegerPair locationTemp = getNumbersFromString(states[0]);
            if (locationTemp != null) {
                Point location = new Point(locationTemp.a, locationTemp.b);
                window.setLocation(location);
                setLocation.add(window);
            }

            IntegerPair sizeTemp = getNumbersFromString(states[1]);
            if (sizeTemp != null && item.saveSize) {
                Dimension size = new Dimension(sizeTemp.a, sizeTemp.b);
                window.setSize(size);
            }
        }
        
        item.wasOpen = states[2].equals("1");
    }
    
    public void setWindowPosition(Window window) {
        boolean setLocationBefore = setLocation.contains(window);
        if (mode() < RESTORE_ALL || !setLocationBefore) {
            window.setLocationRelativeTo(defaultParent);
            setLocation.add(window);
        }
    }
    
    public boolean wasOpen(Window window) {
        StateItem item = windows.get(window);
        return item != null && item.wasOpen;
    }
    
    public boolean shouldReopen(Window window) {
        return mode() >= REOPEN_ON_START && wasOpen(window);
    }
    
    private int mode() {
        return (int)settings.getLong(MODE_SETTING);
    }
    
    private static IntegerPair getNumbersFromString(String input) {
        String[] split = input.split(",");
        if (split.length != 2) {
            return null;
        }
        try {
            int a = Integer.parseInt(split[0]);
            int b = Integer.parseInt(split[1]);
            return new IntegerPair(a, b);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    private static class IntegerPair {
        public final int a;
        public final int b;
        
        public IntegerPair(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    private static class StateItem {
        public final boolean saveSize;
        public final String id;
        public final boolean reopen;
        public boolean wasOpen;
        public boolean setLocation;
        //private final boolean saveLocation;
        
        StateItem(String id, boolean saveSize, boolean reopen) {
            this.saveSize = saveSize;
            this.reopen = reopen;
            this.id = id;
        }
    }
        
}
