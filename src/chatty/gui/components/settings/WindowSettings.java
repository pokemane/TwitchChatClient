
package chatty.gui.components.settings;

import chatty.gui.WindowStateManager;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class WindowSettings extends SettingsPanel {
    
    public WindowSettings(final SettingsDialog d) {
        
        JPanel dialogs = addTitledPanel("Dialogs Location/Size", 0);
        
        dialogs.add(new JLabel("Restore dialogs:"), d.makeGbc(0, 0, 1, 1));
        
        Map<Long, String> restoreModeOptions = new LinkedHashMap<>();
        restoreModeOptions.put((long)WindowStateManager.RESTORE_MAIN, "Open dialogs in default location");
        restoreModeOptions.put((long)WindowStateManager.RESTORE_ALL, "Keep location during session");
        restoreModeOptions.put((long)WindowStateManager.RESTORE_ON_START, "Restore dialogs from last session");
        restoreModeOptions.put((long)WindowStateManager.REOPEN_ON_START, "Reopen dialogs from last session");
        ComboLongSetting restoreMode = new ComboLongSetting(restoreModeOptions);
        d.addLongSetting("restoreMode", restoreMode);
        dialogs.add(restoreMode, d.makeGbc(1, 0, 1, 1));

        JPanel other = addTitledPanel("Other", 1);
        
        other.add(d.addBooleanSetting("urlPrompt", "Prompt dialog when opening URL",
                "Show a prompt to confirm you want to open an URL."),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        other.add(new JLabel("Tab Order:"), d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        other.add(
                d.addComboStringSetting("tabOrder", 1, false, new String[]{"normal", "alphabetical"}),
                d.makeGbc(1, 1, 1, 1)
        );

        other.add(new JLabel("Default Userlist Width:"),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        other.add(d.addSimpleLongSetting("userlistWidth", 3, true),
                d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        
        
    }
    
}
