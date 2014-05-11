
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ColorSettings extends SettingsPanel {
    
    private final SettingsDialog d;
    private final Map<String, ColorSetting> colorSettings = new HashMap<>();
    private final ColorChooser colorChooser;
    private final JPanel main;
    
    public ColorSettings(SettingsDialog d) {
        this.d = d;
        colorChooser = new ColorChooser(d);

        main = addTitledPanel("Customize Colors", 0);
        
        GridBagConstraints gbc;
        gbc = d.makeGbc(0, 0, 1, 1);
        main.add(new Presets(), gbc);
        addColorSetting("backgroundColor", ColorSetting.BACKGROUND, "foregroundColor", "Background","Background Color", 1);
        addColorSetting("foregroundColor", ColorSetting.FOREGROUND, "backgroundColor", "Foreground","Normal message", 2);
        addColorSetting("infoColor", ColorSetting.FOREGROUND, "backgroundColor", "Info","You have joined #channel", 3);
        addColorSetting("compactColor", ColorSetting.FOREGROUND, "backgroundColor", "Compact","MOD: nick1, nick2", 4);
        addColorSetting("highlightColor", ColorSetting.FOREGROUND, "backgroundColor", "Highlight","Highlighted message", 5);
        addColorSetting("inputBackgroundColor", ColorSetting.BACKGROUND, "inputForegroundColor", "Input Background","Input Background", 6);
        addColorSetting("inputForegroundColor", ColorSetting.FOREGROUND, "inputBackgroundColor", "Input Foreground","Input Foreground", 7);
        addColorSetting("searchResultColor", ColorSetting.BACKGROUND, "foregroundColor", "Search Result","Search Result", 8);
    }
    
    
    private ColorSetting addColorSetting(String setting, int type,
            String baseSetting, String name, String text, int row) {
        ColorSetting colorSetting = new ColorSetting(type, baseSetting, name, text, colorChooser);
        colorSetting.setListener(new MyColorSettingListener(setting));
        d.addStringSetting(setting, colorSetting);
        colorSettings.put(setting, colorSetting);
        GridBagConstraints gbc = d.makeGbc(0, row, 1, 1);
        gbc.insets = new Insets(0,0,0,0);
        main.add(colorSetting, gbc);
        return colorSetting;
    }
    
    /**
     * A single setting was updated, so tell all settings that have the updated
     * color as base (background) to update their preview.
     * 
     * @param setting 
     */
    private void updated(String setting) {
        //System.out.println(setting);
        String newColor = colorSettings.get(setting).getSettingValue();
        //updatedSetting.updated();
        for (ColorSetting colorSetting : colorSettings.values()) {
            if (colorSetting.hasBase(setting)) {
                colorSetting.update(newColor);
            }
        }
    }
    
    /**
     * Listen for a color setting to be updated. Save the setting name so it's
     * clear which setting it was.
     */
    class MyColorSettingListener implements ColorSettingListener {

        private String setting;
        
        MyColorSettingListener(String setting) {
            this.setting = setting;
        }
        
        @Override
        public void colorUpdated() {
            updated(setting);
        }
        
    }
    
    /**
     * Defines color presets, kind of a color theme, that can be loaded into
     * the settings by selecting it from a combo box.
     */
    class Presets extends JPanel {
        
        private final Map<String, Map<String, String>> presets = new HashMap<>();
        private final JComboBox<String> combo = new JComboBox<>();
        private final JButton loadPreset = new JButton("Load preset");
        
        Presets() {
            initPresets();
            
            combo.setEditable(false);
            
            for (String presetName : presets.keySet()) {
                combo.addItem(presetName);
            }
            
            add(combo);
            add(loadPreset);
            
            loadPreset.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String selected = combo.getItemAt(combo.getSelectedIndex());
                    loadPreset(selected);
                }
            });
        }
        
        /**
         * Load the preset with the given name. Sets all the String settings
         * defined in this preset.
         * 
         * @param presetName 
         */
        private void loadPreset(String presetName) {
            if (!presets.containsKey(presetName)) {
                return;
            }
            
            Map<String, String> loadedPreset = presets.get(presetName);
            for (String setting : loadedPreset.keySet()) {
                String color = loadedPreset.get(setting);
                d.setStringSetting(setting, color);
            }
        }
        
        /**
         * Create presets for each setting. Using the exact setting names as
         * keys for the Map.
         */
        private void initPresets() {
            Map<String, String> defaultColors = new HashMap<>();
            defaultColors.put("backgroundColor", "#FAFAFA");
            defaultColors.put("foregroundColor", "#111111");
            defaultColors.put("infoColor", "#001480");
            defaultColors.put("compactColor", "#A0A0A0");
            defaultColors.put("highlightColor", "Red");
            defaultColors.put("inputBackgroundColor", "White");
            defaultColors.put("inputForegroundColor", "Black");
            defaultColors.put("searchResultColor", "LightYellow");
            
            presets.put("default", defaultColors);
            
            Map<String, String> darkColors = new HashMap<>();
            darkColors.put("backgroundColor", "#111111");
            darkColors.put("foregroundColor", "LightGrey");
            darkColors.put("infoColor", "DeepSkyBlue");
            darkColors.put("compactColor", "#A0A0A0");
            darkColors.put("highlightColor", "Red");
            darkColors.put("inputBackgroundColor", "#222222");
            darkColors.put("inputForegroundColor", "White");
            darkColors.put("searchResultColor", "DarkSlateBlue");
            
            presets.put("dark", darkColors);
            
            Map<String, String> darkColors2 = new HashMap<>();
            darkColors2.put("backgroundColor", "Black");
            darkColors2.put("foregroundColor", "White");
            darkColors2.put("infoColor", "#FF9900");
            darkColors2.put("compactColor", "#FFCC00");
            darkColors2.put("highlightColor", "#66FF66");
            darkColors2.put("inputBackgroundColor", "#FFFFFF");
            darkColors2.put("inputForegroundColor", "#000000");
            darkColors2.put("searchResultColor", "#333333");
            
            presets.put("dark2", darkColors2);
        }
    }
    
    
}
