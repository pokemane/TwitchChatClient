
package chatty.gui;

import chatty.gui.components.ChannelTextPane;
import chatty.util.api.ChatIcons;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.text.*;

/**
 * Provides style information to other objects based on the settings.
 * 
 * @author tduva
 */
public class StyleManager implements StyleServer {
    
    private final static Logger LOGGER = Logger.getLogger(StyleManager.class.getName());
    
    public static final Set<String> settingNames = new HashSet<>(Arrays.asList(
            "font", "fontSize", "timestampEnabled", "emoticonsEnabled",
            "foregroundColor","infoColor","compactColor","backgroundColor",
            "inputBackgroundColor","inputForegroundColor","usericonsEnabled",
            "timestamp","highlightColor","showBanMessages","autoScroll",
            "deletedMessagesMode", "deletedMessagesMaxLength","searchResultColor",
            "lineSpacing", "ffzModIcon", "bufferSize"
            ));
    
    private MutableAttributeSet baseStyle;
    private MutableAttributeSet standardStyle;
    private MutableAttributeSet specialStyle;
    private MutableAttributeSet infoStyle;
    private MutableAttributeSet paragraphStyle;
    private MutableAttributeSet other;
    private MutableAttributeSet highlightStyle;
    private Font font;
    private Color backgroundColor;
    private Color foregroundColor;
    private Color inputBackgroundColor;
    private Color inputForegroundColor;
    private Color highlightColor;
    private Color searchResultColor;
    
    private ImageIcon turboIcon;
    private ImageIcon modIcon;
    private ImageIcon staffIcon;
    private ImageIcon adminIcon;
    private ImageIcon broadcasterIcon;
    private ImageIcon subIcon;
    
    private Settings settings;
    
    public StyleManager(Settings settings) {
        this.settings = settings;
        makeStyles();
        makeDefaultIcons();
    }
    
    /**
     * Remakes the styles, usually when a setting was changed.
     */
    public void refresh() {
        makeStyles();
    }
    
    /**
     * Replace the default chat icons with the ones received from the API. These
     * can be used when a new channel is opened.
     * 
     * @param icons 
     */
    public void setChatIcons(ChatIcons icons) {
        turboIcon = setChatIcon(turboIcon, icons.getTurboIcon());
        modIcon = setChatIcon(modIcon, icons.getModeratorIcon());
        broadcasterIcon = setChatIcon(broadcasterIcon, icons.getBroadcasterIcon());
        staffIcon = setChatIcon(staffIcon, icons.getStaffIcon());
        adminIcon = setChatIcon(adminIcon, icons.getAdminIcon());
    }
    
    /**
     * Only replace icon if the new one isn't null.
     * 
     * @param icon
     * @param newIcon
     * @return 
     */
    private ImageIcon setChatIcon(ImageIcon icon, ImageIcon newIcon) {
        if (newIcon != null) {
            return newIcon;
        }
        return icon;
    }
    
    private void makeStyles() {
        foregroundColor = makeColor("foregroundColor", Color.BLACK);
        HtmlColors.setDefaultColor(foregroundColor);
        backgroundColor = makeColor("backgroundColor");
        inputBackgroundColor = makeColor("inputBackgroundColor");
        inputForegroundColor = makeColor("inputForegroundColor");
        highlightColor = makeColor("highlightColor");
        searchResultColor = makeColor("searchResultColor");
        
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        baseStyle = new SimpleAttributeSet(defaultStyle);
        StyleConstants.setFontFamily(baseStyle,settings.getString("font"));
        StyleConstants.setFontSize(baseStyle,(int)settings.getLong("fontSize"));
        
        standardStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(standardStyle, makeColor("foregroundColor"));
        
        highlightStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(highlightStyle, highlightColor);
        
        specialStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(specialStyle, makeColor("compactColor"));

        infoStyle = new SimpleAttributeSet(baseStyle);
        StyleConstants.setForeground(infoStyle, makeColor("infoColor"));
        
        paragraphStyle = new SimpleAttributeSet();
        // Divide by 10 so integer values can be used for this setting
        float spacing = settings.getLong("lineSpacing") / (float)10.0;
        StyleConstants.setLineSpacing(paragraphStyle, spacing);
        
        other = new SimpleAttributeSet();
        //other.addAttribute(ChannelTextPane.TIMESTAMP_ENABLED, settings.getBoolean("timestampEnabled"));
        other.addAttribute(ChannelTextPane.EMOTICONS_ENABLED, settings.getBoolean("emoticonsEnabled"));
        other.addAttribute(ChannelTextPane.USERICONS_ENABLED, settings.getBoolean("usericonsEnabled"));
        other.addAttribute(ChannelTextPane.SHOW_BANMESSAGES, settings.getBoolean("showBanMessages"));
        other.addAttribute(ChannelTextPane.AUTO_SCROLL, settings.getBoolean("autoScroll"));
        other.addAttribute(ChannelTextPane.FRANKERFACEZ_MOD, settings.getBoolean("ffzModIcon"));
        other.addAttribute(ChannelTextPane.BUFFER_SIZE, settings.getLong("bufferSize"));
        // Deleted Messages Settings
        String deletedMessagesMode = settings.getString("deletedMessagesMode");
        long deletedMessagesModeNumeric = 0;
        if (deletedMessagesMode.equals("delete")) {
            deletedMessagesModeNumeric = -1;
        } else if (deletedMessagesMode.equals("keepShortened")) {
            deletedMessagesModeNumeric = settings.getLong("deletedMessagesMaxLength");
        }
        other.addAttribute(ChannelTextPane.DELETED_MESSAGES_MODE, deletedMessagesModeNumeric);
        
        String fontFamily = settings.getString("font");
        int fontSize = (int)settings.getLong("fontSize");
        font = new Font(fontFamily,Font.PLAIN,fontSize);
    }
    
    private Color makeColor(String setting) {
        return makeColor(setting, foregroundColor);
    }
    
    private Color makeColor(String setting, Color defaultColor) {
        return HtmlColors.decode(settings.getString(setting),defaultColor);
    }

    @Override
    public MutableAttributeSet getStyle() {
        return getStyle("regular");
    }

    @Override
    public MutableAttributeSet getStyle(String type) {
        switch (type) {
            case "special":
                return specialStyle;
            case "standard":
                return standardStyle;
            case "info":
                return infoStyle;
            case "highlight":
                return highlightStyle;
            case "paragraph":
                return paragraphStyle;
            case "settings":
                return other;
        }
        return baseStyle;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public Color getColor(String type) {
        switch (type) {
            case "foreground":
                return foregroundColor;
            case "background":
                return backgroundColor;
            case "inputBackground":
                return inputBackgroundColor;
            case "inputForeground":
                return inputForegroundColor;
            case "searchResult":
                return searchResultColor;
        }
        return foregroundColor;
    }
    
    private ImageIcon getImageIcon(String fileName) {
        return new ImageIcon(getClass().getResource(fileName));
    }
    
    private ImageIcon getImageIcon(String fileName, Color color) {
        ImageIcon icon = new ImageIcon(getClass().getResource(fileName));
        BufferedImage image = new BufferedImage(icon.getIconWidth(),
                icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.drawImage(icon.getImage(), 0, 0, color, null);
        g.dispose();
        return new ImageIcon(image);
    }
    
    private void makeDefaultIcons() {
         turboIcon = getImageIcon("icon_turbo.png", new Color(100,65,165));
         subIcon = getImageIcon("icon_sub.png");
         staffIcon = getImageIcon("icon_staff.png", new Color(100, 65, 165));
         modIcon = getImageIcon("icon_mod.png", new Color(0,153,0));
         adminIcon = getImageIcon("icon_admin.png", Color.RED);
         broadcasterIcon = getImageIcon("icon_broadcaster.png", Color.BLACK);
    }
   
    
    @Override
    public ImageIcon getIcon(String type) {
        switch (type) {
            case "turbo":
                return turboIcon;
            case "staff":
                return staffIcon;
            case "admin":
                return adminIcon;
            case "mod":
                return modIcon;
            case "broadcaster":
                return broadcasterIcon;
            case "subscriber":
                return subIcon;
        }
        return turboIcon;
    }
    
    @Override
    public SimpleDateFormat getTimestampFormat() {
        String timestamp = settings.getString("timestamp");
        if (!timestamp.equals("off")) {
            try {
                return new SimpleDateFormat(timestamp);
            } catch (IllegalArgumentException ex) {
                LOGGER.warning("Invalid timestamp: "+timestamp);
            }
        }
        return null;
    }
    
}
