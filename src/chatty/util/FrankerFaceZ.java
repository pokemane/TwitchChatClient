
package chatty.util;

import chatty.util.api.ChatIcons;
import chatty.util.api.Emoticon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request FrankerFaceZ emoticons and mod icons.
 * 
 * @author tduva
 */
public class FrankerFaceZ {
    
    private static final Logger LOGGER = Logger.getLogger(FrankerFaceZ.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final String BASE_URL = "http://frankerfacez.storage.googleapis.com/";
    
    /**
     * The channels that have already been requested in this session.
     */
    private final Set<String> alreadyRequested = new HashSet<>();
    /**
     * Save mod icons that were already received, in case they get requested
     * again (e.g. if a channel is closed, then opened again).
     */
    private final Map<String, ChatIcons> cachedIcons = new HashMap<>();
    
    private static final Pattern CODE = Pattern.compile("content:\"([^\"]+)");
    private static final Pattern HEIGHT = Pattern.compile("height:([0-9]+)px");
    private static final Pattern WIDTH = Pattern.compile("width:([0-9]+)px");
    private static final Pattern IMAGE = Pattern.compile("background-image:url\\(\"([^\"]+)\"\\)");
    
    private final FrankerFaceZListener listener;
    
    public FrankerFaceZ(FrankerFaceZListener listener) {
        this.listener = listener;
        //parseEmote("");
    }
    
    /**
     * Requests the emotes for this channel, if not already done.
     * 
     * @param channel The name of the channel/stream
     */
    public void requestEmotes(String channel) {
        if (channel.startsWith("#")) {
            channel = channel.replaceAll("#", "");
        }
        if (channel == null || channel.isEmpty()) {
            return;
        }
        // Request only if not already done
        if (!alreadyRequested.contains(channel)) {
            requestForChannel(channel);
        }
        // Return mod icon if already cached for this channel
        if (cachedIcons.containsKey(channel)) {
            listener.iconsReceived(channel, cachedIcons.get(channel));
        }
        // Request global emotes if not already done
        if (!alreadyRequested.contains(null)) {
            requestForChannel(null);
        }
    }
    
    /**
     * Creates a request with the given channel and starts it in a new Thread.
     * 
     * @param channel The channel to request the emotes for.
     */
    private void requestForChannel(String channel) {
        alreadyRequested.add(channel);
        EmotesRequest request = new EmotesRequest(channel);
        new Thread(request).start();
    }
    
    
    /**
     * Work with the response of the request. Parse the emotes and give them
     * to the listener.
     * 
     * @param channel The channel this request was for.
     * @param result The result consisting of lines of text, or null if an error
     *  occured.
     */
    private void requestResult(String channel, ArrayList<String> result) {
        if (result == null) {
            return;
        }
        HashSet<Emoticon> emotes = new HashSet<>();
        ChatIcons icons = null;
        // Go through all lines and try to parse them as an emote or mod icon.
        for (String line : result) {
            Emoticon parsedEmote = parseEmote(line);
            if (parsedEmote != null) {
                emotes.add(parsedEmote);
            }
            String modIconUrl = parseModIconUrl(line);
            if (modIconUrl != null && channel != null) {
                icons = new ChatIcons(ChatIcons.FRANKERFACEZ);
                icons.setModeratorImage2(modIconUrl);
                LOGGER.info("FFZ ("+channel+"): Mod icon found.");
            }
        }
        if (channel == null) {
            LOGGER.info("FFZ: "+emotes.size()+" global emotes received.");
        } else {
            LOGGER.info("FFZ ("+channel+"): "+emotes.size()+" emotes received.");
        }
        // Package emotes and return them
        HashMap<String, HashSet<Emoticon>> emotes2 = new HashMap<>();
        emotes2.put(channel, emotes);
        listener.emoticonsReceived(emotes2);
        // Return icons if mod icon was found
        if (icons != null) {
            cachedIcons.put(channel, icons);
            listener.iconsReceived(channel, icons);
        }
    }
    
    /**
     * Parses a single emote.
     * 
     * @param stream
     * @param line
     * @return 
     */
    private Emoticon parseEmote(String line) {
        if (line == null || line.isEmpty() || line.startsWith(".line")
                || line.contains(".badges .moderator")
                || line.contains(".badges .ffz-donor")) {
            return null;
        }
        // Remove all whitespace
        line = line.replaceAll("\\s", "");
        //line = ".eidgod-1 {content: \"EidBox\"; background-image: url(\"http://frankerfacez.storage.googleapis.com/eidgod/EidBox.png\"); height: 25px; width: 26px; margin: -3.5px 0px;}";
        String code = get(CODE, line);
        String image = get(IMAGE, line);
        Integer width = getInteger(WIDTH, line);
        Integer height = getInteger(HEIGHT, line);
        if (code == null || image == null || width == null || height == null) {
            LOGGER.warning("FFZ: Could not parse emote: "+line);
            return null;
        }
        return new Emoticon(code, image, width, height);
    }
    
    /**
     * Parses a line to find a mod icon image url. 
     * 
     * @param line The line that may contain a mod icon image url.
     * @return The mod icon image url.
     */
    private String parseModIconUrl(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        if (!line.startsWith(".line .mod") && !line.contains(".badges .moderator")) {
            return null;
        }
        // Remove all whitespace
        line = line.replaceAll("\\s", "");
        String image = get(IMAGE, line);
        if (image == null) {
            LOGGER.warning("FFZ: Could not parse mod image: "+line);
        }
        return image;
    }

    /**
     * Retrieves the text contained in this Patterns first match group from the
     * line.
     * 
     * @param pattern
     * @param line
     * @return 
     */
    private String get(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Retrieves an Integer instead of a String.
     * 
     * @param pattern
     * @param line
     * @return 
     */
    private Integer getInteger(Pattern pattern, String line) {
        try {
            return Integer.parseInt(get(pattern, line));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    /**
     * Requests a file containing the emotes from the internet in a seperate
     * Thread.
     */
    private class EmotesRequest implements Runnable {

        /**
         * Which channel this request is for.
         */
        private final String channel;
        
        /**
         * Creates a new request.
         * 
         * @param channel The channel this request is meant for.
         */
        private EmotesRequest(String channel) {
            this.channel = channel;
        }
        
        @Override
        public void run() {
            String targetUrl;
            // Make the URL based on the channel
            if (channel == null) {
                targetUrl = BASE_URL + "global.css";
            } else {
                targetUrl = BASE_URL + channel + ".css";
            }
            // Perform the request and give the result back
            requestResult(channel, getUrl(targetUrl));
        }
        
        /**
         * Gets the given URL and reads the result line by line.
         * 
         * @param targetUrl
         * @return 
         */
        private ArrayList<String> getUrl(String targetUrl) {
            LOGGER.info("FFZ ("+channel+"): Requesting "+targetUrl);
            
            HttpURLConnection connection = null;
            try {
                URL url = new URL(targetUrl);
                connection = (HttpURLConnection)url.openConnection();
                
                // Read response
                InputStream input = connection.getInputStream();
                
                ArrayList<String> response;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, CHARSET))) {
                    String line;
                    response = new ArrayList<>();
                    while ((line = reader.readLine()) != null) {
                        response.add(line);
                    }
                }
                return response;
            } catch (MalformedURLException ex) {
                LOGGER.warning("FFZ ("+channel+"): Invalid URL to get emotes ("+targetUrl+")");
                return null;
            } catch (IOException ex) {
                LOGGER.warning("FFZ ("+channel+"): Error getting emotes ("+ex.getLocalizedMessage()+")");
                return null;
            } finally {
                if (connection != null) {
                    try {
                        int responseCode = connection.getResponseCode();
                        if (responseCode != 200) {
                            LOGGER.warning("FFZ ("+channel+"): Error requesting emotes ("+responseCode+")");
                        }
                    } catch (IOException ex) {
                        // If this doesn't work, just don't show the response code message
                    }
                }
            }
        }
    }
}
