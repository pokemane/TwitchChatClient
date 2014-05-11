
package chatty.gui;

import chatty.Helper;
import java.awt.Component;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Helper class to build and open Twitch related URLs.
 * 
 * @author tduva
 */
public class TwitchUrl {
    public static void openTwitchProfile(String nick, Component parent) {
        if (nick == null) {
            JOptionPane.showMessageDialog(parent, "Unable to open Twitch Profile URL (Not on a channel)",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String url = makeTwitchProfileUrl(nick);
            UrlOpener.openUrlPrompt(parent, url);
        }
    }
    
    public static void openTwitchStream(String nick, Component parent) {
        openTwitchStream(nick, false, parent);
    }
    
    public static void openTwitchStream(String nick, boolean popout, Component parent) {
        if (nick == null) {
            JOptionPane.showMessageDialog(parent, "Unable to open Twitch Stream URL (Not on a channel)",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String url = makeTwitchStreamUrl(nick, popout);
            UrlOpener.openUrlPrompt(parent, url);
        }
    }
    
    public static String makeTwitchProfileUrl(String channel) {
        return "http://twitch.tv/" + channel + "/profile";
    }
    
    public static String makeTwitchStreamUrl(String channel, boolean popout) {
        String url = "http://twitch.tv/" + channel + "";
        if (popout) {
            url += "/popout";
        }
        return url;
    }
    
    public static final String MULTITWITCH = "http://multitwitch.tv/";
    public static final String SPEEDRUNTV = "http://speedrun.tv/";
    
    public static void openMultitwitch(List<String> streams, Component parent, String type) {
        if (streams == null || streams.isEmpty()) {
            return;
        }
        UrlOpener.openUrlPrompt(parent, makeMultitwitchUrl(streams, type));
    }
    
    public static String makeMultitwitchUrl(List<String> streams, String type) {
        String streamsText = Helper.join(streams, "/");
        String url = type+streamsText;
        return url;
    }
}
