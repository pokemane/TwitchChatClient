
package chatty.util.api;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 * Holds the icons used in chat like mod, admin, staff, turbo etc.
 * 
 * @author tduva
 */
public class ChatIcons {
    
    private static final Logger LOGGER = Logger.getLogger(ChatIcons.class.getName());
    
    private static final Color COLOR_MOD = new Color(0,153,0);
    private static final Color COLOR_TURBO = new Color(100,65,165);
    private static final Color COLOR_STAFF = new Color(100,65,165);
    private static final Color COLOR_ADMIN = Color.RED;
    private static final Color COLOR_BROADCASTER = Color.BLACK;
    
    public static final int DEFAULT = 0;
    public static final int TWITCH = 1;
    public static final int FRANKERFACEZ = 2;
    
    public final int type;
    
    private ImageIcon moderator;
    private ImageIcon admin;
    private ImageIcon staff;
    private ImageIcon subscriber;
    private ImageIcon turbo;
    private ImageIcon broadcaster;

    public ChatIcons(int type) {
        this.type = type;
    }
    
    public ChatIcons(ChatIcons other) {
        this.type = other.type;
        this.moderator = other.getModeratorIcon();
        this.admin = other.getAdminIcon();
        this.staff = other.getStaffIcon();
        this.subscriber = other.getSubscriberIcon();
        this.turbo = other.getTurboIcon();
        this.broadcaster = other.getBroadcasterIcon();
    }
    
    public void setModeratorImage(String url) {
        // TODO: Temporary hardcoded URL until the one in the API is fixed
        url = "http://www-cdn.jtvnw.net/images/xarth/g/g18_sword-FFFFFF80.png";
        moderator = addColor(getIcon(url), COLOR_MOD);
    }
    
    /**
     * Temporary second method to actually set the image to the given URL.
     * 
     * @param url 
     */
    public void setModeratorImage2(String url) {
        moderator = addColor(getIcon(url), COLOR_MOD);
    }
    
    public void setTurboImage(String url) {
        turbo = addColor(getIcon(url), COLOR_TURBO);
    }
    
    public void setSubscriberImage(String url) {
        subscriber = getIcon(url);
    }
    
    public void setStaffImage(String url) {
        staff = addColor(getIcon(url), COLOR_STAFF);
    }
    
    public void setAdminImage(String url) {
        admin = addColor(getIcon(url), COLOR_ADMIN);
    }
    
    public void setBroadcasterImage(String url) {
        broadcaster = addColor(getIcon(url), COLOR_BROADCASTER);
    }
    
    private ImageIcon getIcon(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            ImageIcon icon = new ImageIcon(new URL(url));
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                return icon;
            } else {
                LOGGER.warning("Could not load icon: "+url);
            }
        } catch (MalformedURLException ex) {
            LOGGER.warning("Invalid icon url: "+url);
        }
        return null;
    }
    
    private ImageIcon addColor(ImageIcon icon, Color color) {
        if (icon == null || color == null) {
            return icon;
        }
        BufferedImage image = new BufferedImage(icon.getIconWidth(),
                icon.getIconWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.drawImage(icon.getImage(), 0, 0, color, null);
        g.dispose();
        return new ImageIcon(image);
    }
    
    public ImageIcon getModeratorIcon() {
        return moderator;
    }
    
    public ImageIcon getTurboIcon() {
        return turbo;
    }
    
    public ImageIcon getSubscriberIcon() {
        return subscriber;
    }
    
    public ImageIcon getAdminIcon() {
        return admin;
    }
    
    public ImageIcon getStaffIcon() {
        return staff;
    }
    
    public ImageIcon getBroadcasterIcon() {
        return broadcaster;
    }
    
}
