
package chatty.util.api;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

// FrankerZ Kappa BionicBunion Kreygasm BibleThump :) :O :( ;) :P :\ ResidentSleeper

/**
 * A single emoticon, that contains a pattern, an URL to the image and
 * a width/height.
 * 
 * It also includes a facility to load the image in a seperate thread once
 * it is needed.
 * 
 * @author tduva
 */
public class Emoticon {
    
    private static final Logger LOGGER = Logger.getLogger(Emoticon.class.getName());
    
    private static final Matcher word = Pattern.compile("[^\\w]").matcher("");
    private static final int MAX_LOADING_ATTEMPTS = 3;
    
    private String search;
    private String url;
    private ImageIcon icon;
    private Matcher matcher;
    private int width = 1;
    private int height = 1;
    private final Set<EmoticonUser> users = new HashSet<>();
    private boolean loading = false;
    private boolean loadingError = false;
    private int loadingAttempts = 0;
    
    public Emoticon(String search, String url, int width, int height) {
        // Only match at word boundaries, unless there is a character that
        // isn't a word character
        if (!word.reset(search).find()) {
            search = "\\b"+search+"\\b";
        }
        // Replace &lt; with <
        search = search.replace("\\&lt\\;", "<");
        // Actually compile a Pattern from it
        try {
            matcher = Pattern.compile(search).matcher("");
        } catch (PatternSyntaxException ex) {
            LOGGER.warning("Error compiling pattern for '"+search+"' ["+ex.getLocalizedMessage()+"]");
            // Compile a pattern that doesn't match anything, so a Matcher
            // is still available
            matcher = Pattern.compile("(?!)").matcher("");
        }
        this.search = search;
        this.url = url;
        this.width = width;
        this.height = height;
    }
    
    public boolean matches(String text) {
        return matcher.reset(text).matches();
        //return pattern.matcher(text).matches();
    }
    
    public Matcher getMatcher(String text) {
        return matcher.reset(text);
    }
    
    /**
     * Requests an ImageIcon to be loaded, returns the default icon at first,
     * but starts a SwingWorker to get the actual image.
     * 
     * @param user
     * @return 
     */
    public ImageIcon getIcon(EmoticonUser user) {
        users.add(user);
        if (icon == null) {
            icon = getDefaultIcon();
            loadImage();
        } else if (loadingError) {
            if (loadImage()) {
                LOGGER.warning("Trying to load " + search + " again (" + url + ")");
            }
        }
        return icon;
    }
    
    /**
     * Try to load the image, if it's not already loading and if the max loading
     * attempts are not exceeded.
     * 
     * @return true if the image will be attempted to be loaded, false otherwise
     */
    private boolean loadImage() {
        if (!loading && loadingAttempts < MAX_LOADING_ATTEMPTS) {
            loading = true;
            loadingError = false;
            loadingAttempts++;
            (new IconLoader()).execute();
            return true;
        }
        return false;
    }
    
    /**
     * Construct a default image based on the size of this emoticon.
     * 
     * @param error If true, uses red color to indicate an error.
     * @return 
     */
    private Image getDefaultImage(boolean error) {
        BufferedImage res=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics g = res.getGraphics();
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.setColor(Color.LIGHT_GRAY);
        if (error) {
            g.setColor(Color.red);
        }
        g.drawString("[x]", width / 2, height / 2);

        g.dispose();
        return res;
    }
    
    /**
     * Construct a default icon based on the size of this emoticon.
     * 
     * @return 
     */
    private ImageIcon getDefaultIcon() {
        return new ImageIcon(getDefaultImage(false));
    }
    
    /**
     * A Worker class to load the Icon. Not doing this in it's own thread
     * can lead to lag when a lot of new icons are being loaded.
     */
    class IconLoader extends SwingWorker<ImageIcon,Object> {

        @Override
        protected ImageIcon doInBackground() throws Exception {
            ImageIcon loadedIcon;
            try {
                loadedIcon = new ImageIcon(new URL(url));
            } catch (MalformedURLException ex) {
                LOGGER.warning("Invalid url for "+search+": "+url);
                return null;
            }
            return loadedIcon;
        }
        
        /**
         * The image should be done loading, replace the defaulticon with the
         * actual loaded icon and tell the user that it's loaded.
         */
        @Override
        protected void done() {
            try {
                ImageIcon loadedIcon = get();
                if (loadedIcon == null
                        || loadedIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                    icon.setImage(getDefaultImage(true));
                    loadingError = true;
                } else {
                    icon.setImage(loadedIcon.getImage());
                }
                for (EmoticonUser user : users) {
                    user.iconLoaded();
                }
                loading = false;
                //users.clear();
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public String toString() {
        return search;
    }
    
    
    public static interface EmoticonUser {

        void iconLoaded();
    }
    
}
