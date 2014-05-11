
package chatty;

import chatty.util.Replacer;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Some static helper methods.
 * 
 * @author tduva
 */
public class Helper {
    
    public static final DecimalFormat VIEWERCOUNT_FORMAT = new DecimalFormat();
    
    public static String formatViewerCount(int viewerCount) {
        return VIEWERCOUNT_FORMAT.format(viewerCount);
    }
    
    public static String[] parseChannels(String channels, boolean prepend) {
        String[] parts = channels.split(",");
        Vector<String> result = new Vector<>();
        for (String part : parts) {
            String channel = part.trim();
            if (validateChannel(channel)) {
                if (prepend && !channel.startsWith("#")) {
                    channel = "#"+channel;
                }
                result.add(channel.toLowerCase());
            }
        }
        String[] resultArray = new String[result.size()];
        result.copyInto(resultArray);
        return resultArray;
    }
    
    public static String[] parseChannels(String channels) {
        return parseChannels(channels, true);
    }
    
    /**
     * Takes a Set of Strings and builds a single comma-seperated String of
     * streams out of it.
     * 
     * @param set
     * @return 
     */
    public static String buildStreamsString(Set<String> set) {
        String result = "";
        String sep = "";
        for (String channel : set) {
            result += sep+channel.replace("#", "");
            sep = ", ";
        }
        return result;
    }
    
    public static boolean validateChannel(String channel) {
        try {
            return channel.matches("(?i)^#{0,1}[a-z0-9_]+$");
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }
    
    /**
     * Checks if the given stream/channel is valid and turns it into a channel
     * if necessary.
     *
     * @param channel The channel, valid or invalid, leading # or not.
     * @return The channelname with leading #, or null if channel was invalid.
     */
    public static String checkChannel(String channel) {
        if (!validateChannel(channel)) {
            return null;
        }
        if (!channel.startsWith("#")) {
            channel = "#"+channel;
        }
        return channel;
    }
    
    public static String toStream(String channel) {
        return channel.replace("#", "");
    }
    
    /**
     * Makes a readable message out of the given reason code.
     * 
     * @param reason
     * @param reasonMessage
     * @return 
     */
    public static String makeDisconnectReason(int reason, String reasonMessage) {
        String result = "";
        
        switch (reason) {
            case Irc.ERROR_UNKNOWN_HOST:
                result = "Unknown host";
                break;
            case Irc.REQUESTED_DISCONNECT:
                result = "Requested";
                break;
            case Irc.ERROR_CONNECTION_CLOSED:
                result = "";
                break;
            case Irc.ERROR_REGISTRATION_FAILED:
                result = "Failed to complete login.";
                break;
            case Irc.ERROR_SOCKET_TIMEOUT:
                result = "Connection timed out.";
                break;
        }
        
        if (!result.isEmpty()) {
            result = " ("+result+")";
        }
        
        return result;
    }
    
    /**
     * Tries to turn the given Object into a List of Strings.
     * 
     * If the given Object is a List, go through all items and copy those
     * that are Strings into a new List of Strings.
     * 
     * @param obj
     * @return 
     */
    public static List<String> getStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                if (item instanceof String) {
                    result.add((String)item);
                }
            }
        }
        return result;
    }
    
    /**
     * Copy the given text to the clipboard.
     * 
     * @param text 
     */
    public static void copyToClipboard(String text) {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(text), null);
    }
    
    public static String join(Collection<String> items, String delimiter) {
        StringBuilder b = new StringBuilder();
        Iterator<String> it = items.iterator();
        while (it.hasNext()) {
            b.append(it.next());
            if (it.hasNext()) {
                b.append(delimiter);
            }
        }
        return b.toString();
    }
    
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    
    public static String removeDuplicateWhitespace(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ");
    }
    
    private static final Replacer HTMLSPECIALCHARS;
    
    static {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("&amp;", "&");
        replacements.put("&lt;", "<");
        replacements.put("&gt;", ">");
        HTMLSPECIALCHARS = new Replacer(replacements);
    }
    
    public static String htmlspecialchars_decode(String s) {
        if (s == null) {
            return null;
        }
        return HTMLSPECIALCHARS.replace(s);
    }
    
    private static final Pattern REMOVE_LINEBREAKS = Pattern.compile("\\r?\\n");
    
    /**
     * Removes any linebreaks from the given String.
     * 
     * @param s The String (can be empty or null)
     * @return The modified String or null if the given String was null
     */
    public static String removeLinebreaks(String s) {
        if (s == null) {
            return null;
        }
        return REMOVE_LINEBREAKS.matcher(s).replaceAll(" ");
    }
    
    public static String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }
    
    public static void unhandledException() {
        String[] a = new String[0];
        String b = a[1];
    }
    
}
