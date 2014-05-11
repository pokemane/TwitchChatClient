
package chatty.util.api;

import chatty.Helper;
import chatty.util.DateTime;
import java.util.LinkedHashMap;

/**
 * Holds the current info (name, viewers, title, game) of a stream, as well
 * as a history of the same information and stuff like when the info was
 * last requested, whether it's currently waiting for an API response etc.
 * 
 * @author tduva
 */
public class StreamInfo {
    
    private final String stream;
    
    private long lastUpdated = 0;
    private long lastStatusChange = 0;
    private String status = "";
    private String game = "";
    private int viewers = 0;
    private boolean online = false;
    private boolean updateSucceeded = false;
    private int updateFailedCounter = 0;
    private boolean requested = false;
    private boolean followed = false;
    
    // When the viewer stats where last calculated
    private long lastViewerStats;
    // How long at least between viewer stats calculations
    private static final int VIEWERSTATS_DELAY = 30*60*1000;
    // How long a stats range can be at most
    private static final int VIEWERSTATS_MAX_LENGTH = 35*60*1000;
    
    
    /**
     * The current full status (title + game), updated when new data is set.
     */
    private String currentFullStatus;
    private String prevFullStatus;
    
    private final LinkedHashMap<Long,StreamInfoHistoryItem> history = new LinkedHashMap<>();
    
    private int expiresAfter = 300;
    
    private final StreamInfoListener listener;

    public StreamInfo(String stream, StreamInfoListener listener) {
        this.listener = listener;
        this.stream = stream;
    }
    
    private void streamInfoUpdated() {
        if (listener != null) {
            listener.streamInfoUpdated(this);
        }
    }
    
    public void setRequested() {
        this.requested = true;
    }
    
    public boolean isRequested() {
        return requested;
    }
    
    private void streamInfoStatusChanged() {
        lastStatusChange = System.currentTimeMillis();
        if (listener != null) {
            listener.streamInfoStatusChanged(this, getFullStatus());
        }
    }
    
    @Override
    public String toString() {
        return "Online: "+online+
                " Status: "+status+
                " Game: "+game+
                " Viewers: "+viewers;
    }
    
    public String getFullStatus() {
        return currentFullStatus;
    }
    
    private String makeFullStatus() {
        if (online) {
            String fullStatus = status;
            if (status == null) {
                fullStatus = "No stream title set";
            }
            if (game != null) {
                fullStatus += " ("+game+")";
            }
            return fullStatus;
        }
        else if (!updateSucceeded) {
            return "";
        }
        else {
            return "Stream offline";
        }
    }
    
    public String getStream() {
        return stream;
    }
    
    public void setFollowed(String status, String game, int viewers) {
        //System.out.println(status);
        followed = true;
        boolean saveToHistory = false;
        if (hasExpired()) {
            saveToHistory = true;
        }
        set(status, game, viewers, saveToHistory);
    }
    
    public void set(String status, String game, int viewers) {
        set(status, game, viewers, true);
    }
    
    /**
     * This should only be used when the update was successful.
     * 
     * @param status The current title of the stream
     * @param game The current game
     * @param viewers The current viewercount
     */
    private void set(String status, String game, int viewers, boolean saveToHistory) {
        this.status = Helper.trim(Helper.removeLinebreaks(status));
        this.game = game;
        this.viewers = viewers;
        this.online = true;
        if (saveToHistory) {
            addHistoryItem(System.currentTimeMillis(),new StreamInfoHistoryItem(viewers, status, game));
        }
        setUpdateSucceeded(true);
    }
    
    public void setExpiresAfter(int expiresAfter) {
        this.expiresAfter = expiresAfter;
    }
    
    public void setUpdateSucceeded(boolean succeeded) {
        updateSucceeded = succeeded;
        setUpdated();
        if (succeeded) {
            updateFailedCounter = 0;
        }
        else {
            updateFailedCounter++;
        }
        currentFullStatus = makeFullStatus();
        if (succeeded && !currentFullStatus.equals(prevFullStatus) ||
                lastUpdateLongAgo()) {
            prevFullStatus = currentFullStatus;
            streamInfoStatusChanged();
        }
        // Call at the end, so stuff is already updated
        streamInfoUpdated();
    }
    
    public void setOffline() {
        this.online = false;
        addHistoryItem(System.currentTimeMillis(),new StreamInfoHistoryItem());
        setUpdateSucceeded(true);
    }
    
    public boolean getFollowed() {
        return followed;
    }
    
    public boolean getOnline() {
        return this.online;
    }
    
    private void setUpdated() {
        lastUpdated = System.currentTimeMillis() / 1000;
        requested = false;
    }
    
    // Getters
    
    /**
     * Gets the status stored for this stream. May not be correct, check
     * isValid() before using any data.
     * 
     * @return 
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Gets the title stored for this stream, which is the same as the status,
     * unless the status is null. As opposed to getStatus() this never returns
     * null.
     * 
     * @return 
     */
    public String getTitle() {
        if (status == null) {
            return "No stream title set";
        }
        return status;
    }
    
    /**
     * Gets the game stored for this stream. May not be correct, check
     * isValid() before using any data.
     * 
     * @return 
     */
    public String getGame() {
        return game;
    }
    
    /**
     * Gets the viewers stored for this stream. May not be correct, check
     * isValid() before using any data.
     * 
     * @return 
     */
    public int getViewers() {
        return viewers;
    }
    
    /**
     * Calculates the number of seconds that passed after the last update
     * 
     * @return Number of seconds that have passed after the last update
     */
    public long getUpdatedDelay() {
        return (System.currentTimeMillis() / 1000) - lastUpdated;
    }
    
    /**
     * Checks if the info should be updated. The stream info takes longer
     * to expire when there were failed attempts at downloading the info from
     * the API. This only affects hasExpired(), not isValid().
     * 
     * @return true if the info should be updated, false otherwise
     */
    public boolean hasExpired() {
        return getUpdatedDelay() > expiresAfter * (1+ updateFailedCounter / 2);
    }
    
    /**
     * Checks if the info is valid, taking into account if the last request
     * succeeded and how old the data is.
     * 
     * @return true if the info can be used, false otherwise
     */
    public boolean isValid() {
        if (!updateSucceeded || getUpdatedDelay() > expiresAfter*2) {
            return false;
        }
        return true;
    }
    
    public boolean lastUpdateLongAgo() {
        if (updateSucceeded && getUpdatedDelay() > expiresAfter*4) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns the number of seconds the last status change is ago.
     * 
     * @return 
     */
    public long getStatusChangeTimeAgo() {
        return (System.currentTimeMillis() - lastStatusChange) / 1000;
    }
    
    public long getStatusChangeTime() {
        return lastStatusChange;
    }
    
    private void addHistoryItem(Long time, StreamInfoHistoryItem item) {
        synchronized(history) {
            history.put(time, item);
        }
    }
    
    public LinkedHashMap<Long,StreamInfoHistoryItem> getHistory() {
        synchronized(history) {
            return new LinkedHashMap<>(history);
        }
    }
    
    /**
     * Create a summary of the viewercount in the interval that hasn't been
     * calculated yet (delay set as a constant).
     * 
     * @param force Get stats even if the delay hasn't passed yet.
     * @return 
     */
    public ViewerStats getViewerStats(boolean force) {
        synchronized(history) {
            if (lastViewerStats == 0 && !force) {
                // No stats output yet, so assume current time as start, so
                // it's output after the set delay
                lastViewerStats = System.currentTimeMillis() - 5000;
            }
            long timePassed = System.currentTimeMillis() - lastViewerStats;
            if (!force && timePassed < VIEWERSTATS_DELAY) {
                return null;
            }
            long startAt = lastViewerStats+1;
            // Only calculate the max length
            if (timePassed > VIEWERSTATS_MAX_LENGTH) {
                startAt = System.currentTimeMillis() - VIEWERSTATS_MAX_LENGTH;
            }
            int min = -1;
            int max = -1;
            int total = 0;
            int count = 0;
            long firstTime = -1;
            long lastTime = -1;
            StringBuilder b = new StringBuilder();
            // Initiate with -2, because -1 already means offline
            int prevViewers = -2;
            for (long time : history.keySet()) {
                if (time < startAt) {
                    continue;
                }
                // Start doing anything for values >= startAt
                
                // Update so that it contains the last value that was looked at
                // at the end of this method
                lastViewerStats = time;
                
                int viewers = history.get(time).getViewers();

                // Append to viewercount development String
                if (prevViewers > -1 && viewers != -1) {
                    // If there is a prevViewers set and if online
                    int diff = viewers - prevViewers;
                    if (diff >= 0) {
                        b.append("+");
                    }
                    b.append(Helper.formatViewerCount(diff));
                } else if (viewers != -1) {
                    if (prevViewers == -1) {
                        // Previous was offline, so show that
                        b.append("_");
                    }
                    b.append(Helper.formatViewerCount(viewers));
                }
                prevViewers = viewers;
                
                
                if (viewers == -1) {
                    continue;
                }
                // Calculate min/max/sum/count only when online
                if (firstTime == -1) {
                    firstTime = time;
                }
                lastTime = time;
                
                if (viewers > max) {
                    max = viewers;
                }
                if (min == -1 || viewers < min) {
                    min = viewers;
                }
                total += viewers;
                count++;
            }
            
            // After going through all values, do some finishing work
            if (prevViewers == -1) {
                // Last value was offline, so show that
                b.append("_");
            }
            if (count == 0) {
                return null;
            }
            int avg = total / count;
            return new ViewerStats(min, max, avg, firstTime, lastTime, count, b.toString());
        }
    }
    
    /**
     * Holds a set of immutable values that make up viewerstats.
     */
    public static class ViewerStats {
        public final int max;
        public final int min;
        public final int avg;
        public final long startTime;
        public final long endTime;
        public final int count;
        public final String history;
        
        public ViewerStats(int min, int max, int avg, long startTime,
                long endTime, int count, String history) {
            this.max = max;
            this.min = min;
            this.avg = avg;
            this.startTime = startTime;
            this.endTime = endTime;
            this.count = count;
            this.history = history;
        }
        
        /**
         * Which duration the data in this stats covers. This is not necessarily
         * the whole duration that was worked with (e.g. if the stream went
         * offline at the end, that data may not be included). This is the range
         * between the first and last valid data point.
         * 
         * @return The number of seconds this data covers.
         */
        public long duration() {
            return (endTime - startTime) / 1000;
        }
        
        /**
         * Checks if these viewerstats contain any viewer data.
         * 
         * @return 
         */
        public boolean isValid() {
            // If min was set to another value than the initial one, then this
            // means at least one data point with a viewercount was there.
            return min != -1;
        }
        
        @Override
        public String toString() {
            return "Viewerstats ("+DateTime.format2(startTime)
                    +"-"+DateTime.format2(endTime)+"):"
                    + " avg:"+Helper.formatViewerCount(avg)
                    + " min:"+Helper.formatViewerCount(min)
                    + " max:"+Helper.formatViewerCount(max)
                    + " ["+count+"/"+history+"]";
        }
    }
}
