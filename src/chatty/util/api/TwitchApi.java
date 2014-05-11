
package chatty.util.api;

import chatty.Helper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Handles TwitchApi requests and responses.
 * 
 * @author tduva
 */
public class TwitchApi {

    private final static Logger LOGGER = Logger.getLogger(TwitchApi.class.getName());
    
    private final HashMap<String,String> pendingRequest = new HashMap<>();
    
    /**
     * How long the Emoticons can be cached in a file after they are updated
     * from the API.
     */
    public static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60*60*24;
    
    public static final int TOKEN_CHECK_DELAY = 600;
    
    /**
     * Request type for getting StreamInfo
     */
    private static final int REQUEST_TYPE_STREAM = 1;
    /**
     * Request type for getting Emoticons
     */
    private static final int REQUEST_TYPE_EMOTICONS = 2;
    /**
     * Request type for getting Token information
     */
    private static final int REQUEST_TYPE_VERIFY_TOKEN = 3;
    /**
     * Request type for getting chat icons (subscriber)
     */
    private static final int REQUEST_TYPE_CHAT_ICONS = 4;
    /**
     * Request type for getting channel info (different from stream)
     */
    private static final int REQUEST_TYPE_CHANNEL = 5;
    /**
     * Request type for changing channel info
     */
    private static final int REQUEST_TYPE_CHANNEL_PUT = 6;
    /**
     * Request type for searching for games
     */
    private static final int REQUEST_TYPE_GAME_SEARCH = 7;
    /**
     * Request type for running commercials
     */
    private static final int REQUEST_TYPE_COMMERCIAL = 8;
    /**
     * Request info for several streams
     */
    private static final int REQUEST_TYPE_STREAMS = 9;
    /**
     * Request followed streams
     */
    private static final int REQUEST_TYPE_FOLLOWED_STREAMS = 10;
    
    public static final int ACCESS_DENIED = 0;
    public static final int SUCCESS = 1;
    public static final int FAILED = 2;
    public static final int NOT_FOUND = 3;
    public static final int RUNNING_COMMERCIAL = 4;
    public static final int INVALID_CHANNEL = 5;
    
    private final TwitchApiResultListener resultListener;
    
    private final StreamInfoManager streamInfoManager;
    private final EmoticonManager emoticonManager;
    
    private volatile long tokenLastChecked = 0;
    
    /**
     * Save received ChatIcons in case they get requested again.
     */
    private final Map<String, ChatIcons> cachedChatIcons = 
            Collections.synchronizedMap(new HashMap<String, ChatIcons>());

    public TwitchApi(TwitchApiResultListener apiResultListener,
            StreamInfoListener streamInfoListener) {
        this.resultListener = apiResultListener;
        this.streamInfoManager = new StreamInfoManager(this, streamInfoListener);
        emoticonManager = new EmoticonManager(apiResultListener);
        
    }
    
    /**
     * Get StreamInfo for the given stream. Always returns a StreamInfo object,
     * which may however be marked as invalid if the stream is no valid stream
     * name or does not exist or data hasn't been requested yet.
     * 
     * The first request per stream is always invalid, because the info has
     * to be requested from the server first. Further request return a cached
     * version of the StreamInfo, until the info is marked as expired.
     * 
     * @param stream
     * @return The StreamInfo object
     */
    public synchronized StreamInfo getStreamInfo(String stream, Set<String> streams) {
        if (streams == null) {
            streams = new HashSet<>();
        }
        return streamInfoManager.getStreamInfo(stream, streams);
    }
    
    public synchronized void getFollowedStreams(String token) {
        streamInfoManager.getFollowedStreams(token);
    }
    
    protected void requestFollowedStreams(String token, String nextUrl) {
        String url;
        if (nextUrl != null) {
            url = nextUrl;
        } else {
            url = "https://api.twitch.tv/kraken/streams/followed?limit="
                    + StreamInfoManager.FOLLOWED_STREAMS_LIMIT + "&offset=0";
        }
        TwitchApiRequest request = new TwitchApiRequest(this,
                REQUEST_TYPE_FOLLOWED_STREAMS, url, token);
        new Thread(request).start();
    }
    
    public void requestChatIcons(String channel) {
        String url = "https://api.twitch.tv/kraken/chat/"+channel+"/badges";
        if (attemptRequest(url, channel)) {
            if (!cachedChatIcons.containsKey(channel)) {
                // Only request icons if they haven't been received before
                TwitchApiRequest request = new TwitchApiRequest(this, REQUEST_TYPE_CHAT_ICONS, url);
                new Thread(request).start();
            } else {
                // If icons are already cached, just give them back as result
                resultListener.receivedChatIcons(channel, cachedChatIcons.get(channel));
            }
        }
    }
    
    /**
     * Request to get the emoticons list from the API. If the list is already
     * available in a local file and is recent, that one is used. Otherwise
     * a request is issued and the emoticons are received and parsed when that
     * request is answered.
     */
    public void requestEmoticons() {
        if (!emoticonManager.loadEmoticons()) {
            String url = "https://api.twitch.tv/kraken/chat/emoticons";
            TwitchApiRequest request = new TwitchApiRequest(this, REQUEST_TYPE_EMOTICONS, url);
            new Thread(request).start();
            //requestResult(REQUEST_TYPE_EMOTICONS,"")
        }
    }
    
    /**
     * Sends a request to get streaminfo of the given stream.
     * 
     * @param stream 
     */
    protected void requestStreamInfo(String stream) {
        String url = "https://api.twitch.tv/kraken/streams/"+stream;
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,REQUEST_TYPE_STREAM,url);
            new Thread(request).start();
        }
    }
    
    protected String requestStreamsInfo(Set<String> streams) {
        String streamsString = Helper.join(streams, ",");
        String url = "https://api.twitch.tv/kraken/streams?offset=0&limit=100&channel="+streamsString;
        TwitchApiRequest request =
                    new TwitchApiRequest(this,REQUEST_TYPE_STREAMS,url);
            new Thread(request).start();
        return url;
    }
    
    public void getChannelInfo(String stream) {
        if (stream == null) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/channels/"+stream;
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,REQUEST_TYPE_CHANNEL,url);
            new Thread(request).start();
        }
    }
    
    public void getGameSearch(String game) {
        if (game == null || game.isEmpty()) {
            return;
        }
        String encodedGame = "";
        try {
            encodedGame = URLEncoder.encode(game, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(TwitchApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        final String url = "https://api.twitch.tv/kraken/search/games?type=suggest&q="+encodedGame;
        TwitchApiRequest request =
                new TwitchApiRequest(this, REQUEST_TYPE_GAME_SEARCH, url);
        new Thread(request).start();
    }
    
    public void putChannelInfo(String stream, ChannelInfo info, String token) {
        if (stream == null) {
            return;
        }
        String url = "https://api.twitch.tv/kraken/channels/"+stream;
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request =
                    new TwitchApiRequest(this,REQUEST_TYPE_CHANNEL_PUT,url,token);
            request.setData(makeChannelInfoJson(info), "PUT");
            new Thread(request).start();
        }
    }
    
    /**
     * Checks if a request with the given url can be made. Returns true if no
     * request with that url is currently waiting for a response, false
     * otherwise.
     * 
     * This also saves the stream this request url is associated with, so it
     * can more easily be retrieved when the reponse comes in.
     * 
     * @param url The URL of the request
     * @param stream The name of the stream
     * @return true if request can be made, false if it shouldn't
     */
    public synchronized boolean attemptRequest(String url, String stream) {
        synchronized (pendingRequest) {
            if (!pendingRequest.containsKey(url)) {
                pendingRequest.put(url, stream);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Removes the given url from the requests that are waiting for a response
     * and retrieves the name of the stream this url is associated with.
     * 
     * @param url The URL of the request.
     * @return The name of the stream associated with this request (or null if
     *  no stream was set for this url or the url wasn't found).
     */
    private String removeRequest(String url) {
        synchronized(pendingRequest) {
            return pendingRequest.remove(url);
        }
    }
    
    /**
     * When access was denied when doing an authenticated request. Check the
     * token maybe subsequently.
     */
    protected void accessDenied() {
        resultListener.accessDenied();
    }
    
    /**
     * Verifies token, but only once the delay has passed. For automatic checks
     * instead of manual ones.
     * 
     * @param token 
     */
    public void checkToken(String token) {
        if (token != null && !token.isEmpty() &&
                (System.currentTimeMillis() - tokenLastChecked) / 1000 > TOKEN_CHECK_DELAY) {
            LOGGER.info("Checking token..");
            tokenLastChecked = System.currentTimeMillis();
            verifyToken(token);
        }
    }
    
    public void verifyToken(String token) {
        String url = "https://api.twitch.tv/kraken/";
        TwitchApiRequest request =
                new TwitchApiRequest(this,REQUEST_TYPE_VERIFY_TOKEN,url,token);
        new Thread(request).start();
    }
    
    public void runCommercial(String stream, String token, int length) {
        String url = "https://api.twitch.tv/kraken/channels/"+stream+"/commercial";
        if (attemptRequest(url, stream)) {
            TwitchApiRequest request
                    = new TwitchApiRequest(this, REQUEST_TYPE_COMMERCIAL, url, token);
            request.setData("length=" + length, "POST");
            request.setContentType("application/x-www-form-urlencoded");
            new Thread(request).start();
        }
    }
    
    /**
     * Called by the request thread to work on the result of the request.
     * 
     * @param type The type of request
     * @param url The url of the request
     * @param result The result, usually json from the Twitch API or null if
     * an error occured
     * @param responseCode The HTTP response code
     */
    protected synchronized void requestResult(int type, String url, String result, int responseCode) {
        int length = -1;
        if (result != null) {
            length = result.length();
        }
        LOGGER.info("GOT ("+responseCode+", "+length+"): "+url);
        String stream = removeRequest(url);
        if (type == REQUEST_TYPE_STREAM) {
            streamInfoManager.requestResult(url, result, responseCode, stream);
        }
        else if (type == REQUEST_TYPE_STREAMS) {
            streamInfoManager.requestResultStreams(url, result, responseCode);
        }
        else if (type == REQUEST_TYPE_EMOTICONS) {
            emoticonManager.emoticonsReceived(result);
        }
        else if (type == REQUEST_TYPE_CHAT_ICONS) {
            if (result == null) {
                LOGGER.warning("Error requesting stream icons: "+result);
                return;
            }
            ChatIcons icons = parseChatIcons(result);
            if (icons == null) {
                LOGGER.warning("Error parsing stream icons: "+result);
                return;
            }
            // If everything seems ok, store the icons in case they are
            // requested again at a later time
            cachedChatIcons.put(stream, icons);
            resultListener.receivedChatIcons(stream, icons);
        }
        else if (type == REQUEST_TYPE_CHANNEL || type == REQUEST_TYPE_CHANNEL_PUT) {
            handleChannelInfoResult(type, url, result, responseCode, stream);
        }
        else if (type == REQUEST_TYPE_GAME_SEARCH) {
            if (result == null) {
                LOGGER.warning("Error searching for game");
                return;
            }
            Set<String> games = parseGameSearch(result);
            if (games == null) {
                LOGGER.warning("Error parsing game search result");
                return;
            }
            resultListener.gameSearchResult(games);
        }
        
    }
    
    /**
     * Result for authorized requests.
     * 
     * @param type The type of request
     * @param url The requested URL
     * @param result The returned JSON
     * @param responseCode The HTTP response code
     * @param token The token used for authorization
     */
    protected synchronized void requestResult(int type, String url, String result, int responseCode, String token) {
        int length = -1;
        if (result != null) {
            length = result.length();
        }
        LOGGER.info("GOT ("+responseCode+", "+length+"): "+url+" (using authorization)");
        if (type == REQUEST_TYPE_VERIFY_TOKEN) {
            TokenInfo tokenInfo = parseVerifyToken(result);
            resultListener.tokenVerified(token, tokenInfo);
        }
        else if (type == REQUEST_TYPE_CHANNEL_PUT) {
            requestResult(type, url, result, responseCode);
        }
        else if (type == REQUEST_TYPE_COMMERCIAL) {
            String stream = removeRequest(url);
            String resultText = "Commercial probably not running (unknown response: "+responseCode+")";
            int resultCode = -1;
            if (responseCode == 204) {
                resultText = "Running commercial..";
                resultCode = RUNNING_COMMERCIAL;
            }
            else if (responseCode == 422) {
                resultText = "Commercial length not allowed or trying to run too early.";
                resultCode = FAILED;
            }
            else if (responseCode == 401 || responseCode == 403) {
                resultText = "Can't run commercial: Access denied";
                resultCode = ACCESS_DENIED;
                accessDenied();
            }
            else if (responseCode == 404) {
                resultText = "Can't run commercial: Channel '"+stream+"' not found";
                resultCode = INVALID_CHANNEL;
            }
            if (resultListener != null) {
                resultListener.runCommercialResult(stream, resultText, resultCode);
            }
        }
        else if (type == REQUEST_TYPE_FOLLOWED_STREAMS) {
            streamInfoManager.requestResultFollows(result, responseCode);
        }
    }
    
    /**
     * Handle the ChannelInfo request result, which can also be from changing
     * the channel info.
     * 
     * @param type The type of request, in this case from changing channel info
     *  or simply from requesting ChannelInfo
     * @param url The URL that was called
     * @param result The JSON received
     * @param responseCode The HTTP response code of the request
     */
    private void handleChannelInfoResult(int type, String url, String result,
            int responseCode, String stream) {
        // Handle requested ChannelInfo but also the result of changing
        // channel info, since that returns ChannelInfo as well.
        if (result == null || responseCode != 200) {
            handleChannelInfoResultError(stream, type, responseCode);
            return;
        }
        // Request should have gone through fine
        ChannelInfo info = parseChannelInfo(result);
        if (info == null) {
            LOGGER.warning("Error parsing channel info: " + result);
            handleChannelInfoResultError(stream, type, responseCode);
            return;
        }
        if (type == REQUEST_TYPE_CHANNEL_PUT) {
            resultListener.putChannelInfoResult(SUCCESS);
        }
        resultListener.receivedChannelInfo(stream, info, SUCCESS);
    }
    
    /**
     * Handle the error of a ChannelInfo request result, this can be from
     * changing the channel info as well. Handle by logging the error as well
     * as informing the client who can inform the user on the GUI.
     * 
     * @param type
     * @param responseCode 
     */
    private void handleChannelInfoResultError(String stream, int type, int responseCode) {
        if (type == REQUEST_TYPE_CHANNEL) {
            if (responseCode == 404) {
                resultListener.receivedChannelInfo(stream, null, NOT_FOUND);
            } else {
                resultListener.receivedChannelInfo(stream, null, FAILED);
            }
        } else {
            // The result of changing channel info requires some extra
            // handling, because it can have different response codes.
            if (responseCode == 404) {
                resultListener.putChannelInfoResult(NOT_FOUND);
            } else if (responseCode == 401 || responseCode == 403) {
                LOGGER.warning("Error setting channel info: Access denied");
                resultListener.putChannelInfoResult(ACCESS_DENIED);
                accessDenied();
            } else {
                LOGGER.warning("Error setting channel info: Unknown error (" + responseCode + ")");
                resultListener.putChannelInfoResult(FAILED);
            }
        }
    }
    
    /**
     * Read out the error from JSON, although this shouldn't happen with how
     * the data is requested. Probably not even needed.
     * 
     * @param json The JSON to parse from
     * @return true if no error occured, false otherwise
     */
//    private boolean checkStatus(String json) {
//        try {
//            JSONParser parser = new JSONParser();
//            JSONObject root = (JSONObject)parser.parse(json);
//            Number status = (Number)root.get("status");
//            if (status == null) {
//                return true;
//            }
//            LOGGER.warning((String)root.get("message"));
//            return false;
//        } catch (ParseException ex) {
//            LOGGER.warning("Error parsing for status: "+json+" "+ex.getLocalizedMessage());
//            return false;
//        }
//    }
    
    /**
     * Parses the JSON returned from the TwitchAPI that contains the token
     * info into a TokenInfo object.
     * 
     * @param json
     * @return The TokenInfo or null if an error occured.
     */
    private TokenInfo parseVerifyToken(String json) {
        if (json == null) {
            LOGGER.warning("Error parsing verify token result (null)");
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            
            JSONObject token = (JSONObject) root.get("token");
            
            if (token == null) {
                return null;
            }
            
            boolean valid = (Boolean)token.get("valid");
            
            if (!valid) {
                return new TokenInfo();
            }
            
            String username = (String)token.get("user_name");
            JSONObject authorization = (JSONObject)token.get("authorization");
            JSONArray scopes = (JSONArray)authorization.get("scopes");
            
            boolean allowCommercials = scopes.contains("channel_commercial");
            boolean allowEditor = scopes.contains("channel_editor");
            boolean chatAccess = scopes.contains("chat_login");
            boolean userAccess = scopes.contains("user_read");
            
            return new TokenInfo(username, chatAccess, allowEditor, allowCommercials, userAccess);
        }
        catch (ParseException e) {
            return null;
        }
    }
    
    /**
     * Parses the icons info returned from the TwitchAPI into a ChatIcons object
     * containing the URLs.
     * 
     * @param json
     * @return 
     */
    private ChatIcons parseChatIcons(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            ChatIcons icons = new ChatIcons(ChatIcons.TWITCH);
            
            icons.setModeratorImage(getChatIconUrl(root, "mod", "alpha"));
            icons.setTurboImage(getChatIconUrl(root,"turbo", "alpha"));
            icons.setSubscriberImage(getChatIconUrl(root,"subscriber", "image"));
            icons.setAdminImage(getChatIconUrl(root,"admin", "alpha"));
            icons.setStaffImage(getChatIconUrl(root,"staff", "alpha"));
            icons.setBroadcasterImage(getChatIconUrl(root,"broadcaster", "alpha"));
            
            return icons;
        }
        catch (ParseException ex) {
            LOGGER.warning("Error parsing chat icons: "+ex.getLocalizedMessage());
            return null;
        }
    }
    
    /**
     * Returns the URL for a single icon, read from the given JSONObject.
     * 
     * @param root The JSONObject that contains the icon info.
     * @param name The name of the icon.
     * @param type The type of the icon (alpha or regular image).
     * @return 
     */
    private String getChatIconUrl(JSONObject root, String name, String type) {
        try {
            JSONObject icon = (JSONObject)root.get(name);
            String image = (String)icon.get(type);
            return image;
        } catch (NullPointerException ex) {
            LOGGER.warning("Error parsing chat icon "+name+": unexpected null");
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing chat icon "+name+": unexpected type");
        }
        return null;
    }
    
    /**
     * Parses the channel info returned from the Twitch API into a ChannelInfo
     * object.
     * 
     * @param json
     * @return 
     */
    private ChannelInfo parseChannelInfo(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            String status = (String)root.get("status");
            String game = (String)root.get("game");
            
            return new ChannelInfo(status,game);
        }
        catch (ParseException ex) {
            LOGGER.warning("Error parsing ChannelInfo.");
            return null;
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing ChannelInfo: Unexpected type");
            return null;
        }
        
    }
    
    /**
     * Turns a ChannelInfo object to JOSN to send it to the API.
     * 
     * @param info The ChannelInfo object
     * @return The created JSON
     */
    private String makeChannelInfoJson(ChannelInfo info) {
        JSONObject root = new JSONObject();
        Map channel = new HashMap();
        channel.put("status",info.getStatus());
        channel.put("game",info.getGame());
        root.put("channel",channel);
        return root.toJSONString();
    }
    
    /**
     * Parse the list of games that was returned by the game search.
     * 
     * @param json
     * @return 
     */
    private Set<String> parseGameSearch(String json) {
        Set<String> result = new HashSet<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            Object games = root.get("games");
            
            if (!(games instanceof JSONArray)) {
                LOGGER.warning("Error parsing game search: Should be array");
                return null;
            }
            Iterator it = ((JSONArray)games).iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof JSONObject) {
                    String name = (String)((JSONObject)obj).get("name");
                    result.add(name);
                }
            }
            return result;
            
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing game search.");
            return null;
        } catch (NullPointerException ex) {
            LOGGER.warning("Error parsing game search: Unexpected null");
            return null;
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing game search: Unexpected type");
            return null;
        }
    }
}