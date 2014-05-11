
package chatty.util.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Gets stream info from the TwitchApi.
 * 
 * @author tduva
 */
public class StreamInfoManager {
    
    private final static Logger LOGGER = Logger.getLogger(StreamInfoManager.class.getName());

    /**
     * How often to request the StreamInfo from the API.
     */
    private static final int UPDATE_STREAMINFO_DELAY = 120;
    
    private static final int UPDATE_FOLLOWS_DELAY = 200;
    
    /**
     * How often to request the StreamInfo from the API, after there was a 404.
     */
    private static final int UPDATE_STREAMINFO_DELAY_NOT_FOUND = 300;
    
    
    /**
     * The number of followed streams to get in one request. 100 is the limit
     * Twitch has.
     */
    public static final int FOLLOWED_STREAMS_LIMIT = 100;
    
    /**
     * Number of requests made to get followed streams. This is used as a
     * safeguard to prevent too many follow-up requests (when more than 100
     * followed streams are live or at least the program thinks so). It is
     * only set to 1 when a new normal request is started, which shouldn't
     * happen more often than the UPDATE_FOLLOWS_DELAY.
     */
    private int followedStreamsRequests = 0;
    /**
     * The maximum number of requests for followed streams in the set delay.
     */
    private static final int FOLLOWED_STREAMS_REQUEST_LIMIT = 3;
    
    /**
     * Stores the already created StreamInfo objects.
     */
    private final HashMap<String, StreamInfo> cachedStreamInfo = new HashMap<>();
    
    /**
     * Stores when the streams info was last requested, so the delay can be
     * ensured.
     */
    private long streamsInfoLastRequested = 0;
    private long followsLastRequested = 0;
    
    private int followsRequestErrors = 0;
    private String prevToken = "";
    
    private int streamsRequestErrors = 0;
    
    /**
     * Stores streams requests, so the list of requested streams can be accessed
     * when the response come in.
     */
    private final Map<String, Set<StreamInfo>> pendingRequests;

    private final StreamInfoListener listener;
    private final TwitchApi api;
    
    /**
     * A StreamInfo object to represent invalid stream info.
     */
    private final StreamInfo invalidStreamInfo;

    /**
     * Create a new manager object.
     * 
     * @param api A reference back to the API, so requests can be done.
     * @param listener Listener that is informed once stream info is updated.
     */
    public StreamInfoManager(TwitchApi api, StreamInfoListener listener) {
        this.listener = listener;
        this.api = api;
        pendingRequests = new HashMap<>();
        // Create StreamInfo object for invalid stream names
        invalidStreamInfo = new StreamInfo("invalid", listener);
        // Set as requested so it won't ever be requested
        invalidStreamInfo.setRequested();
    }
    
    public void getFollowedStreams(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        if (!prevToken.equals(token)) {
            followsRequestErrors = 0;
        }
        if (checkTimePassed(followsLastRequested, UPDATE_FOLLOWS_DELAY,
                followsRequestErrors)) {
            prevToken = token;
            followsLastRequested = System.currentTimeMillis();
            followedStreamsRequests = 1;
            api.requestFollowedStreams(token, null);
        }
    }
    
    private void getFollowedStreamsNext(String url) {
        if (url != null && followedStreamsRequests < FOLLOWED_STREAMS_REQUEST_LIMIT) {
            followedStreamsRequests++;
            api.requestFollowedStreams(prevToken, url);
        } else {
            LOGGER.warning("Followed streams: Not getting next url '"+url+"' "
                    + "(requests: "+followedStreamsRequests+", "
                    + "limit: "+FOLLOWED_STREAMS_REQUEST_LIMIT+")");
        }
    }
    
    private boolean checkTimePassed(long lastTime, int delay, int errors) {
        long timePassed = System.currentTimeMillis() - lastTime;
        //System.out.println(delay + errors * delay/4);
        if (timePassed/1000 < delay + errors * delay/4) {
            return false;
        }
        return true;
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
     * @param stream A single stream that the StreamInfo is being requested for
     * @param streams A Set of streams, all of which will probably be requested
     *  soon or from which status changes are expected (for example all open
     *  channels in chat). These might all be requested at this time.
     * @return The StreamInfo object
     */
    public StreamInfo getStreamInfo(String stream, Set<String> streams) {
        // Prefer to request several stream infos at once
        requestStreamsInfo(streams);
        // Then check to request this one, which can happen if the streams
        // request was requested not too long ago. This ofc is also to actually
        // return the StreamInfo object.
        StreamInfo cached = getStreamInfo(stream);
        if (cached.hasExpired() && !cached.isRequested()) {
            cached.setRequested();
            api.requestStreamInfo(stream);
        }
        return cached;
    }
    
    /**
     * Gets a StreamInfo object for the given stream name. Either returns the
     * already existing StreamInfo object for this stream or creates a new one.
     * 
     * @param stream
     * @return 
     */
    private StreamInfo getStreamInfo(String stream) {
        if (stream == null || stream.isEmpty()) {
            return invalidStreamInfo;
        }
        StreamInfo cached = cachedStreamInfo.get(stream);
        if (cached == null) {
            cached = new StreamInfo(stream, listener);
            cached.setExpiresAfter(UPDATE_STREAMINFO_DELAY);
            cachedStreamInfo.put(stream, cached);
        }
        return cached;
    }
    
    /**
     * Request info for a list of streams. This first checks if enough time has
     * passed since the last request, then puts together a list of streams that
     * fit the criteria (aren't currently waiting for a response) and then
     * actually starts a request if there are streams to request.
     * 
     * @param streams 
     */
    private void requestStreamsInfo(Set<String> streams) {
        if (!checkTimePassed(streamsInfoLastRequested, UPDATE_STREAMINFO_DELAY,
                streamsRequestErrors)) {
            return;
        }
        Set<String> streamsForRequest = new HashSet<>();
        Set<StreamInfo> streamInfosForRequest = new HashSet<>();
        for (String stream : streams) {
            StreamInfo cached = getStreamInfo(stream);
            // Don't check for expired, so all open chans are
            // requested, meaning hopefully less requests
            if (!cached.isRequested()) {
                streamsForRequest.add(stream);
                streamInfosForRequest.add(cached);
                cached.setRequested();
            }
        }
        if (!streamsForRequest.isEmpty()) {
            streamsInfoLastRequested = System.currentTimeMillis();
            String url = api.requestStreamsInfo(streamsForRequest);
            pendingRequests.put(url, streamInfosForRequest);
        }
    }
    
    /**
     * Parse result of the /streams/:channel/ request.
     * 
     * @param url
     * @param result
     * @param responseCode
     * @param stream 
     */
    protected void requestResult(String url, String result, int responseCode, String stream) {
        // Probably don't use a lock here, but rely on the lock of the caller
        // so no deadlock can accur in case cachedStreamInfo is accessed from
        // somehwere where the TwitchApi isn't locked
        StreamInfo streamInfo = getStreamInfo(stream);
        if (result == null) {
            LOGGER.warning("Error requesting stream data: " + result);
            if (responseCode == 404) {
                streamInfo.setExpiresAfter(UPDATE_STREAMINFO_DELAY_NOT_FOUND);
            }
            streamInfo.setUpdateSucceeded(false);
            return;
        }
        parseStream(streamInfo, result);

    }
    
    /**
     * Parse result of the /streams?channel=[..] request.
     * 
     * @param url
     * @param result
     * @param responseCode 
     */
    protected void requestResultStreams(String url, String result, int responseCode) {
        Set<StreamInfo> expected = pendingRequests.remove(url);
        if (expected == null) {
            LOGGER.warning("No pending request for: "+url);
        } else if (responseCode != 200 || result == null) {
            if (responseCode == 404) {
                streamsRequestErrors += 2;
            } else {
                streamsRequestErrors++;
            }
            LOGGER.warning("Unexpected response code "+responseCode
                    +" or result null (errors: "+streamsRequestErrors+")");
            streamsRequestError(expected);
        } else {
            if (parseStreams(result, expected) == -1) {
                streamsRequestErrors++;
            } else {
                streamsRequestErrors = 0;
            }
        }
    }
    
    /**
     * Can be used to tell all the StreamInfo objects that the update failed.
     * 
     * @param expected 
     */
    private void streamsRequestError(Set<StreamInfo> expected) {
        for (StreamInfo info : expected) {
            info.setUpdateSucceeded(false);
        }
    }
    
    protected void requestResultFollows(String result, int responseCode) {
        //System.out.println(result);
        if (responseCode == 200 && result != null) {
            int count = parseStreams(result, null);
            LOGGER.info("Got "+count+" (limit: "+FOLLOWED_STREAMS_LIMIT+") followed streams.");
            if (count == FOLLOWED_STREAMS_LIMIT) {
                String nextUrl = getNextUrl(result);
                getFollowedStreamsNext(nextUrl);
            }
            followsRequestErrors = 0;
        } else if (responseCode == 401) {
            followsRequestErrors += 4;
            LOGGER.warning("Access denied when getting followed streams.");
            api.accessDenied();
        } else {
            followsRequestErrors++;
        }
    }

    /**
     * Parses a single stream info response, which can contain a stream object
     * if the stream is online.
     * 
     * This parses the response to /streams/:channel/
     *
     * @param streamInfo The StreamInfo object to write the changes into
     * @param json The JSON to parse from
     */
    protected void parseStream(StreamInfo streamInfo, String json) {
        
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);

            JSONObject stream = (JSONObject) root.get("stream");

            if (stream == null) {
                streamInfo.setOffline();
            } else {
                StreamInfo result = parseStream(stream, false);
                if (result == null || result != streamInfo) {
                    LOGGER.warning("Error parsing stream ("
                            +streamInfo.getStream()+"): "+json);
                    streamInfo.setUpdateSucceeded(false);
                }
            }
        }
        catch (ParseException ex) {
            streamInfo.setUpdateSucceeded(false);
            LOGGER.warning("Error parsing stream info: "+ex.getLocalizedMessage());
        }
        
    }

    /**
     * Parses a list of stream objects. Goes through all stream objects and
     * parses them (which also means it updates the appropriate StreamInfo
     * objects in the process), then sets all remaining streams that were
     * expected in this response to offline.
     * 
     * This parses the response to /streams?channel=[..]
     * 
     * @param json The JSON to parse, can't be null
     * @param streamInfos The StreamInfo objects that were expected for this
     *  response, can be null if none could be expected due to the nature of the
     *  request (e.g. followed channels)
     * @return The number of items or -1 if the whole json could not be parsed
     */
    private int parseStreams(String json, Set<StreamInfo> streamInfos) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject)parser.parse(json);
            
            Object streams = root.get("streams");
            if (!(streams instanceof JSONArray)) {
                LOGGER.warning("Streams should be array.");
                if (streamInfos != null) {
                    streamsRequestError(streamInfos);
                }
                return -1;
            }
            // Go through all streams, parse and update
            JSONArray streamsArray = (JSONArray)streams;
            for (Object obj : streamsArray) {
                if (obj instanceof JSONObject) {
                    StreamInfo parsedInfo =
                            parseStream((JSONObject)obj, streamInfos == null);
                    if (parsedInfo == null) {
                        // Can't use setUpdateSucceeded(false) because it is
                        // not known which StreamInfo object it would be.
                        LOGGER.warning("Error parsing stream "+(JSONObject)obj);
                    }
                    if (streamInfos != null) {
                        streamInfos.remove(parsedInfo);
                    }
                } else {
                    LOGGER.warning("Element in array wasn't JSONObject "+obj);
                }
            }
            // Anything remaining, that was requested, should be offline
            // (or invalid, but which it is can't be determined)
            if (streamInfos != null) {
                for (StreamInfo info : streamInfos) {
                    info.setOffline();
                }
            }
            return streamsArray.size();
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing streams info: "+ex.getLocalizedMessage());
            if (streamInfos != null) {
                streamsRequestError(streamInfos);
            }
            return -1;
        }
    }

    /**
     * Parse a stream object into a StreamInfo object. This gets the name of the
     * stream and gets the appropriate StreamInfo object, in which it then loads
     * the other extracted data like status, game, viewercount.
     * 
     * This is used for both the response from /streams/:channel/ as well as
     * /streams?channel=[..]
     *
     * @param stream The JSONObject containing the stream object.
     * @return The StreamInfo object or null if an error occured.
     */
    private StreamInfo parseStream(JSONObject stream, boolean follows) {
        if (stream == null) {
            LOGGER.warning("Error parsing stream: Should be JSONObject, not null");
            return null;
        }
        Number viewersTemp;
        String status;
        String game;
        String name;
        try {
            // Get stream data
            viewersTemp = (Number) stream.get("viewers");
            // Get channel data
            JSONObject channel = (JSONObject) stream.get("channel");
            if (channel == null) {
                LOGGER.warning("Error parsing StreamInfo: channel null");
                return null;
            }
            status = (String) channel.get("status");
            game = (String) channel.get("game");
            name = (String) channel.get("name");
        } catch (ClassCastException ex) {
            LOGGER.warning("Error parsing StreamInfo: unpexected type");
            return null;
        }
        // Checks (status and game are allowed to be null)
        if (name == null || name.isEmpty()) {
            LOGGER.warning("Error parsing StreamInfo: name null or empty");
            return null;
        }
        if (viewersTemp == null) {
            LOGGER.warning("Error parsing StreamInfo: viewercount null ("+name+")");
            return null;
        }

        // Final stuff
        int viewers = viewersTemp.intValue();
        if (viewers < 0) {
            viewers = 0;
            LOGGER.warning("Warning: Viewercount should not be negative, set to 0 ("+name+").");
        }

        // Get and update stream info
        StreamInfo streamInfo = getStreamInfo(name.toLowerCase());
        if (follows) {
            streamInfo.setFollowed(status, game, viewers);
        } else {
            streamInfo.set(status, game, viewers);
        }
        return streamInfo;
    }
    
    /**
     * Retrieves the next URL for pagination from the given JSON.
     * 
     * @param json The API response that contains the next URL.
     * @return The URL as a String or null if no URL could be retrieved.
     */
    private String getNextUrl(String json) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(json);
            Object links = root.get("_links");
            if (links != null && links instanceof JSONObject) {
                return (String)((JSONObject)links).get("next");
            }
        } catch (ParseException ex) {
            LOGGER.warning("Error parsing next url: " + ex.getLocalizedMessage());
        }
        return null;
    }
    
}
