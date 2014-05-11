
package chatty.gui;

import chatty.Helper;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Opens the given URL in the default browser, with or without prompt.
 * 
 * @author tduva
 */
public class UrlOpener {
    
    private static final Logger LOGGER = Logger.getLogger(UrlOpener.class.getName());
    
    private final static int MAX_URL_LENGTH = 80;
    private static boolean prompt = true;
    
    /**
     * Changes whether to use a prompt by default.
     * 
     * @param usePrompt 
     */
    public static void setPrompt(boolean usePrompt) {
        prompt = usePrompt;
    }
    
    /**
     * Open a single URL with a prompt if enabled.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param url The URL as a String.
     */
    public static void openUrlPrompt(Component parent, String url) {
        openUrlPrompt(parent, url, false);
    }
    
    /**
     * Open a single URL with a prompt if enabled or if the prompt is forced
     * by the parameter.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param url The URL as a String.
     * @param forcePrompt Whether to force a prompt or use the default setting.
     */
    public static void openUrlPrompt(Component parent, String url,
            boolean forcePrompt) {
        List<String> list = new ArrayList<>();
        list.add(url);
        openUrlsPrompt(parent, list, forcePrompt);
    }
    
    /**
     * Directly open the given URL.
     * 
     * @param url The URL as a String.
     */
    public static void openUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException ex) {
                LOGGER.warning("IOException when opening URL "+ex.getLocalizedMessage());
            } catch (URISyntaxException ex) {
                LOGGER.warning("URISyntaxException when opening URL "+ex.getLocalizedMessage());
            }
        }
    }
    
    /**
     * Open several URLs by using a prompt if it's enabled.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param urls The list of URLs as Strings.
     */
    public static void openUrlsPrompt(Component parent, List<String> urls) {
        openUrlsPrompt(parent, urls, false);
    }
    
    /**
     * Opens several URLs by using a prompt if it's enabled or if a prompt is
     * forced by the parameter.
     * 
     * @param parent The Component that should be the parent of a prompt
     * @param urls The list of URLs as Strings
     * @param forcePrompt Always show a prompt, even if it's not the default
     *  setting
     */
    public static void openUrlsPrompt(Component parent, List<String> urls,
            boolean forcePrompt) {
        if (urls.isEmpty()) {
            return;
        }
        if (!forcePrompt && !prompt) {
            openUrls(urls);
            return;
        }
        switch (showUrlsPrompt(parent, urls)) {
            case 0: openUrls(urls); break;
            case 1: Helper.copyToClipboard(urls.get(0));
        }
    }
    
    private static String splitUrl(String url) {
        if (url.length() > MAX_URL_LENGTH) {
            return url.substring(0, MAX_URL_LENGTH)+" "+splitUrl(url.substring(MAX_URL_LENGTH));
        }
        return url;
    }
    
    public static void openUrls(List<String> urls) {
        for (String url : urls) {
            openUrl(url);
        }
    }
    
    /**
     * Actually show the dialog that contain the given URLs and give the user
     * the option to open the URL, copy it or cancel the dialog.
     * 
     * @param parent The Component that will be used as parent for the prompt.
     * @param urls The list of URLs as Strings
     * @return 0 if the URL should be opened, 1 if it should be copied, 2 if
     *  nothing should be done
     */
    private static int showUrlsPrompt(Component parent, List<String> urls) {
        // Make text
        String text = "<html><body style='width: 100px;'>";
        for (String url : urls) {
            url = splitUrl(url);
            text += url + "<br />";
        }
        // Make options
        String okOption = "Open URL";
        if (urls.size() > 1) {
            okOption = "Open "+urls.size()+" URLs";
        }
        String[] options = {okOption, "Cancel"};
        if (urls.size() == 1) {
            options = new String[]{okOption, "Copy URL", "Cancel"};
        }
        // Show dialog
        int chosenOption = JOptionPane.showOptionDialog(parent,
                text,
                "Open in default browser?",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, 1);
        
        return chosenOption;
    }
}
