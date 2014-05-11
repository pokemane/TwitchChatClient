
package chatty;

import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Logger;

/**
 * Provides methods to get a (maybe new) User object for a channel/username 
 * combination and search for User objects by channel, username etc.
 * 
 * @author tduva
 */
public class UserManager {

    private static final Logger LOGGER = Logger.getLogger(UserManager.class.getName());
    
    /**
     * How many milliseconds after the subscriber message the actual channel
     * message has to be received from the same user to make him a subscriber
     */
    private static final int SUBSCRIBER_BUFFER_TIME = 500;
    
    /**
     * How long for the modlist requests to expire. Saving the modlist requests
     * is also a measure to prevent lag from mixing up requests/responses, so
     * this should be relatively high compared to the valid time.
     */
    private static final int MODLIST_EXPIRE_TIME = 15*1000;
    
    /**
     * How long after the request should a modlist response be assumed to belong
     * to the request (and thus the channel).
     */
    private static final int MODLIST_VALID_TIME = 5*1000;
    
    private final HashMap<String, HashMap<String, User>> users = new HashMap<>();
    private final HashMap<String, String> cachedEmoteSets = new HashMap<>();
    private final HashMap<String, String> cachedColors = new HashMap<>();
    private final HashSet<String> cachedTurbo = new HashSet<>();
    private final HashSet<String> cachedAdmin = new HashSet<>();
    private final HashSet<String> cachedStaff = new HashSet<>();
    private final HashMap<String, Long> cachedSubscriber = new HashMap<>();
    private final HashMap<String, Long> modsListRequested = new HashMap<>();
    private boolean capitalizedNames = false;
    
    private final User errorUser = new User("[Error]", "#[error]");

    private UsercolorManager usercolorManager;
    private Addressbook addressbook;
    
    public void setCapitalizedNames(boolean capitalized) {
        capitalizedNames = capitalized;
    }
    
    public void setUsercolorManager(UsercolorManager manager) {
        usercolorManager = manager;
    }
    
    public void setAddressbook(Addressbook addressbook) {
        this.addressbook = addressbook;
    }
    
    /**
     * Gets a Map of all User objects in the given channel.
     * 
     * @param channel
     * @return 
     */
    public synchronized HashMap<String, User> getUsersByChannel(String channel) {
        HashMap<String, User> result = users.get(channel);
        if (result == null) {
            result = new HashMap<>();
            users.put(channel, result);
        }
        return result;
    }

    /**
     * Searches all channels for the given username and returns a List of all
     * the associated User objects.
     * 
     * @param name The username to search for
     * @return The List of User-objects.
     */
    public synchronized List<User> getUsersByName(String name) {
        List<User> result = new ArrayList<>();
        Iterator<HashMap<String, User>> it = users.values().iterator();
        while (it.hasNext()) {
            HashMap<String, User> channelUsers = it.next();
            User user = channelUsers.get(name.toLowerCase());
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * Returns the User with the given name or creates a new User object if none
     * exists for this name.
     *
     * @param channel
     * @param name The name of the user
     * @return The matching User object
     * @see User
     */
    public synchronized User getUser(String channel, String name) {
        // Not sure if this makes sense
        if (name == null || name.isEmpty()) {
            return errorUser;
        }
        name = name.toLowerCase();
        User user = (User) getUsersByChannel(channel).get(name);
        if (user == null) {
            String displayedName = name;
            if (capitalizedNames) {
                displayedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            user = new User(displayedName, channel);
            user.setUsercolorManager(usercolorManager);
            user.setAddressbook(addressbook);
            // Initialize some values if present for this name
            if (cachedEmoteSets.containsKey(name)) {
                user.setEmoteSets(cachedEmoteSets.get(name));
            }
            if (cachedColors.containsKey(name)) {
                user.setColor(cachedColors.get(name));
            }
            if (cachedAdmin.contains(name)) {
                user.setAdmin(true);
            }
            if (cachedStaff.contains(name)) {
                user.setStaff(true);
            }
            if (cachedTurbo.contains(name)) {
                user.setTurbo(true);
            }
            // Put User into the map for the channel
            getUsersByChannel(channel).put(name, user);
        }
        return user;
    }
    
    /**
     * Searches all channels for the given username and returns a Map with
     * all channels the username was found in and the associated User objects.
     * 
     * @param name The username to be searched for
     * @return A Map with channel->User association
     */
    public synchronized HashMap<String,User> getChannelsAndUsersByUserName(String name) {
        HashMap<String,User> result = new HashMap<>();
        
        Iterator<Entry<String, HashMap<String, User>>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, HashMap<String, User>> channel = it.next();
            
            String channelName = channel.getKey();
            HashMap<String,User> channelUsers = channel.getValue();
            
            User user = channelUsers.get(name.toLowerCase());
            if (user != null) {
                result.put(channelName,user);
            }
        }
        return result;
    }
    
    public synchronized void clear() {
        users.clear();
    }
    
    public synchronized void clear(String channel) {
        getUsersByChannel(channel).clear();
    }
    
    public synchronized void setAllOffline() {
        Iterator<HashMap<String,User>> it = users.values().iterator();
        while (it.hasNext()) {
            HashMap<String,User> channel = it.next();
            for (User user : channel.values()) {
                user.setOnline(false);
            }
        }
    }
    
    
    protected synchronized void setEmoteSetForUsername(String userName, String emoteSet) {
        cachedEmoteSets.put(userName.toLowerCase(),emoteSet);
        List<User> userAllChans = getUsersByName(userName);
        for (User user : userAllChans) {
            user.setEmoteSets(emoteSet);
        }
    }
    
    /**
     * Sets the color of a user across all channels.
     * 
     * @param userName String The name of the user
     * @param color String The color as a string representation
     */
    protected synchronized void setColorForUsername(String userName, String color) {
        userName = userName.toLowerCase();
        cachedColors.put(userName,color);
        
        List<User> userAllChans = getUsersByName(userName);
        for (User user : userAllChans) {
            user.setColor(color);
        }
    }
    
    /**
     * Sets a user as a special user across all channels.
     * 
     * @param userName
     * @param type 
     * @param singleChannel Whether only one channel was open.
     */
    protected synchronized void userSetSpecialUser(String userName, String type, String channel) {
        userName = userName.toLowerCase();
        List<User> userAllChans;
        if (channel == null) {
             userAllChans = getUsersByName(userName);
        } else {
            userAllChans = new ArrayList<>();
            userAllChans.add(getUser(channel, userName));
        }
        
        for (User user : userAllChans) {
            if (type.equals("admin")) {
                user.setAdmin(true);
            } else if (type.equals("staff")) {
                user.setStaff(true);
            } else if (type.equals("turbo")) {
                user.setTurbo(true);
            } else if (type.equals("subscriber")) {
                if (channel != null) {
                    user.setSubscriber(true);
                }
            }
        }
        switch (type) {
            case "admin":
                cachedAdmin.add(userName);
                break;
            case "staff":
                cachedStaff.add(userName);
                break;
            case "turbo":
                cachedTurbo.add(userName);
                break;
            case "subscriber":
                cachedSubscriber.put(userName, System.currentTimeMillis());
        }
    }
    
    
    protected synchronized void channelMessage(User user) {
        if (cachedSubscriber.isEmpty()) {
            return;
        }
        Long cachedSubscriberTime = cachedSubscriber.remove(user.getNick().toLowerCase());
        if (cachedSubscriberTime != null) {
            long passedTime = System.currentTimeMillis() - cachedSubscriberTime;
            if (passedTime < SUBSCRIBER_BUFFER_TIME) {
                user.setProbablySubscriber();
            }
        }
        Iterator<Entry<String, Long>> it = cachedSubscriber.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > SUBSCRIBER_BUFFER_TIME) {
                it.remove();
            }
        }
        
    }
    
    protected synchronized void modsListRequested(String channel) {
        modsListRequested.put(channel, System.currentTimeMillis());
    }
    
    protected synchronized List<User> modsListReceived(List<String> modsList) {
        // Remove all entries from modlist requests that have expired
        Iterator<Map.Entry<String, Long>> it = modsListRequested.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > MODLIST_EXPIRE_TIME) {
                it.remove();
            }
        }
        
        // If this is the only entry after removing expired requests, then assume
        // this as the response to the request and thus determine the channel
        if (modsListRequested.size() == 1) {
            Map.Entry<String, Long> entry = modsListRequested.entrySet().iterator().next();
            String channel = entry.getKey();
            long ago = System.currentTimeMillis() - entry.getValue();
            if (ago < MODLIST_VALID_TIME) {
                return modsListReceived(channel, modsList);
            }
            modsListRequested.clear();
        }
        return null;
    }
    
    protected synchronized List<User> modsListReceived(String channel, List<String> modsList) {
        LOGGER.info("Setting users as mod for "+channel+": "+modsList);
        List<User> changedUsers = new ArrayList<>();
        for (String userName : modsList) {
            if (Helper.validateChannel(userName)) {
                User user = getUser(channel, userName);
                user.setModerator(true);
                changedUsers.add(user);
            }
        }
        return changedUsers;
    }
    
}
