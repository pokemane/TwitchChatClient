
package chatty;

/**
 * Initialize the actual HotkeySetter, if enabled.
 * 
 * @author tduva
 */
public class HotkeyManager {
    
    private HotkeySetter setter;
    
    public HotkeyManager(TwitchClient client, boolean enabled) {
        if (enabled) {
            setter = new HotkeySetter(client);
        }
    }

    public void setCommercialHotkey(String hotkey) {
        if (setter != null) {
            setter.setCommercialHotkey(hotkey);
        }
    }

}
