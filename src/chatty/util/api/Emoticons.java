
package chatty.util.api;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Add emoticons and get a list of them matching a certain emoteset.
 * 
 * @author tduva
 */
public class Emoticons {
    
    /**
     * Emoticons associated with an emoteset (Twitch Emotes)
     */
    HashMap<Integer,HashSet<Emoticon>> emoticons = new HashMap<>();
    /**
     * Emoticons associated with a channel (FrankerFaceZ)
     */
    HashMap<String,HashSet<Emoticon>> otherEmoticons = new HashMap<>();
    
    public Emoticons() {
        
    }
    
    /**
     * Adds a Map of emotesets, containing a set of emotes, whereas the key null
     * is the default emoteset.
     * 
     * @param newEmoticons 
     */
    public void addEmoticons(HashMap<Integer,HashSet<Emoticon>> newEmoticons) {
        emoticons.putAll(newEmoticons);
    }
    
    /**
     * Adds emoticons associated with a channel.
     * 
     * @param newEmoticons 
     */
    public void addOtherEmoticons(HashMap<String,HashSet<Emoticon>> newEmoticons) {
        otherEmoticons.putAll(newEmoticons);
    }

    /**
     * Gets a list of all emoticons that don't have an emoteset associated
     * with them.
     * 
     * @return 
     */
    public HashSet<Emoticon> getEmoticons() {
        HashSet<Emoticon> result = emoticons.get(null);
        if (result == null) {
            result = new HashSet<>();
        }
        return result;
    }
    
    /**
     * Gets a list of emoticons that are associated with the given emoteset.
     * 
     * @param emoteSet
     * @return 
     */
    public HashSet<Emoticon> getEmoticons(int emoteSet) {
        HashSet<Emoticon> result = emoticons.get(emoteSet);
        if (result == null) {
            result = new HashSet<>();
        }
        return result;
    }
    
    /**
     * Gets a list of emoticons that are associated with the given channel.
     * 
     * @param channel The name of the channel
     * @return 
     */
    public HashSet<Emoticon> getEmoticons(String channel) {
        HashSet<Emoticon> result = otherEmoticons.get(channel);
        if (result == null) {
            result = new HashSet<>();
        }
        return result;
    }
}
