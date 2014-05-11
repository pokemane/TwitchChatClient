
package chatty;

import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;
import com.melloware.jintellitype.JIntellitypeException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Actually initialize JIntellitype and register the hotkeys.
 * 
 * @author tduva
 */
public class HotkeySetter implements HotkeyListener {
    
    private static final Logger LOGGER = Logger.getLogger(HotkeySetter.class.getName());
    
    private boolean initialized = false;
    private TwitchClient client;
    private static final int COMMERCIAL_HOTKEY = 1;
    private static final int TEST_HOTKEY = 2;

    private final Map<Integer, Long> lastPressed = new HashMap<>();
    private final Map<Integer, Long> lastAction = new HashMap<>();
    
    public HotkeySetter(TwitchClient client) {
        this.client = client;
        initialize();
    }
    
    /**
     * Adds a hotkey listener.
     */
    public final void initialize() {
        try {
            JIntellitype.getInstance().addHotKeyListener(this);
            initialized = true;
        } catch (JIntellitypeException ex) {
            String info = "Failed adding HotKeyListener: "+ex.getLocalizedMessage();
            client.warning(info);
            LOGGER.warning(info);
        }
        if (Chatty.DEBUG) {
            setHotkey(TEST_HOTKEY, "ca84");
        }
    }
    
    @Override
    public void onHotKey(int i) {
        if (i == COMMERCIAL_HOTKEY && delay(COMMERCIAL_HOTKEY, 200, 1000)) {
            client.runCommercial();
        } else if (i == TEST_HOTKEY) {
            client.testHotkey();
        }
    }

    public void setCommercialHotkey(String hotkey) {
        setHotkey(COMMERCIAL_HOTKEY, hotkey);
    }

    /**
     * Sets a hotkey with the given id. The hotkey consists of modifiers
     * (c == ctrl, s == shift, ..) and the keyCode.
     * 
     * @param id
     * @param hotkey 
     */
    private void setHotkey(int id, String hotkey) {
        if (!initialized) {
            return;
        }
        try {
            // Remove previously under this id registered hotkey (if there is
            // any)
            JIntellitype.getInstance().unregisterHotKey(id);
            if (hotkey.isEmpty()) {
                LOGGER.info("Unregistered hotkey (if necessary): " + id);
                return;
            }
            int mod = getModFromHotkey(hotkey);
            int keyCode = getKeycodeFromHotkey(hotkey);
            if (keyCode == -1) {
                LOGGER.info("Invalid hotkey " + id + ", no keycode found.");
                return;
            }
            LOGGER.info("Trying to register hotkey: " + id + "/" + mod + "/" + keyCode);
            JIntellitype.getInstance().registerHotKey(id, mod, keyCode);
        } catch (JIntellitypeException ex) {
            LOGGER.info("Couldn't register hotkey: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Gets the JIntellitype-type modifier integer, based on the given hotkey.
     * 
     * @param hotkey
     * @return 
     */
    private int getModFromHotkey(String hotkey) {
        int mod = 0;
        if (hotkey.contains("c")) {
            mod += JIntellitype.MOD_CONTROL;
        }
        if (hotkey.contains("a")) {
            mod += JIntellitype.MOD_ALT;
        }
        if (hotkey.contains("s")) {
            mod += JIntellitype.MOD_SHIFT;
        }
        if (hotkey.contains("w")) {
            mod += JIntellitype.MOD_WIN;
        }
        return mod;
    }

    /**
     * Gets the keyCode from the hotkey, which is simply the only number in
     * this String.
     * 
     * @param hotkey
     * @return 
     */
    private int getKeycodeFromHotkey(String hotkey) {
        int keyCode = -1;
        try {
            keyCode = Integer.parseInt(hotkey.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException ex) {
        }
        return keyCode;
    }
    
    /**
     * Checks for a required delay between keypresses for the given identifier.
     * This can prevent an action from triggering if you keep the hotkey pressed
     * (repeatDelay) as well as put in a general delay between successfully
     * triggered actions (actionDelay).
     * 
     * @param identifier The identifier of the hotkey
     * @param repeatDelay Time in milliseconds that has to be in between
     *  keypresses
     * @param actionDelay Time in milliseconds that has to be in between actions
     *  this hotkey has triggered
     * @return 
     */
    private boolean delay(int identifier, int repeatDelay, int actionDelay) {
        boolean result = true;
        if (lastPressed.containsKey(identifier)) {
            long timePassed = System.currentTimeMillis() - lastPressed.get(identifier);
            result = timePassed > repeatDelay;
        }
        lastPressed.put(identifier, System.currentTimeMillis());
        if (result) {
            if (lastAction.containsKey(identifier)) {
                long timePassed = System.currentTimeMillis() - lastAction.get(identifier);
                result = timePassed > actionDelay;
            }
        }
        if (result) {
            lastAction.put(identifier, System.currentTimeMillis());
        }
        return result;
    }
    
}
