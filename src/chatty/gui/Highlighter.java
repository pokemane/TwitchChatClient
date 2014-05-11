
package chatty.gui;

import chatty.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Checks if a given String matches previously given highlight items.
 * 
 * @author tduva
 */
public class Highlighter {
    
    private static final Logger LOGGER = Logger.getLogger(Highlighter.class.getName());
    
    private static final int LAST_HIGHLIGHTED_TIMEOUT = 10*1000;
    
    private final Map<String, Long> lastHighlighted = new HashMap<>();
    private final List<HighlightItem> items = new ArrayList<>();
    private Pattern usernamePattern;
    
    // Settings
    private boolean highlightUsername;
    private boolean highlightNextMessages;
    
    /**
     * Clear current items and load the new ones.
     * 
     * @param newItems 
     */
    public void update(List<String> newItems) {
        items.clear();
        for (String item : newItems) {
            if (item != null && !item.isEmpty()) {
                items.add(new HighlightItem(item));
            }
        }
    }
    
    /**
     * Sets the current username.
     * 
     * @param username 
     */
    public void setUsername(String username) {
        if (username == null) {
            usernamePattern = null;
        }
        else {
            // Create pattern to match username on word boundaries
            try {
                usernamePattern = Pattern.compile("(?i).*\\b"+username+"\\b.*");
            } catch (PatternSyntaxException ex) {
                LOGGER.warning("Invalid regex for username: " + ex.getLocalizedMessage());
                usernamePattern = null;
            }
        }
    }
    
    /**
     * Sets whether the username should be highlighted.
     * 
     * @param highlighted 
     */
    public void setHighlightUsername(boolean highlighted) {
        this.highlightUsername = highlighted;
    }
    
    public void setHighlightNextMessages(boolean highlight) {
        this.highlightNextMessages = highlight;
    }
    
    public boolean check(User fromUser, String text) {
        if (checkMatch(fromUser, text)) {
            addMatch(fromUser.getNick());
            return true;
        }
        return false;
    }
    
    /**
     * Checks whether the given message consisting of username and text should
     * be highlighted.
     * 
     * @param userName The name of the user who send the message
     * @param text The text of the message
     * @return true if the message should be highlighted, false otherwise
     */
    private boolean checkMatch(User user, String text) {
        
        String lowercaseText = text.toLowerCase();
        String lowercaseUserName = user.getNick().toLowerCase();
        
        // Try to match own name first (if enabled)
        if (highlightUsername && usernamePattern != null &&
                usernamePattern.matcher(text).matches()) {
            return true;
        }
        
        // Then try to match against the items
        for (HighlightItem item : items) {
            if (item.matches(user, lowercaseUserName, text, lowercaseText)) {
                return true;
            }
        }
        
        // Then see if there is a recent match
        if (highlightNextMessages && hasRecentMatch(user.getNick())) {
            return true;
        }
        return false;
    }
    
    private void addMatch(String fromUsername) {
        lastHighlighted.put(fromUsername, System.currentTimeMillis());
    }
    
    private boolean hasRecentMatch(String fromUsername) {
        clearRecentMatches();
        return lastHighlighted.containsKey(fromUsername);
    }
    
    private void clearRecentMatches() {
        Iterator<Map.Entry<String, Long>> it = lastHighlighted.entrySet().iterator();
        while (it.hasNext()) {
            if (System.currentTimeMillis() - it.next().getValue() > LAST_HIGHLIGHTED_TIMEOUT) {
                it.remove();
            }
        }
    }
    
    /**
     * A single item that itself parses the item String and prepares it for
     * matching. The item can be asked whether it matches a message.
     * 
     * A message matches the item if the message text contains the text of this
     * item (case-insensitive).
     * 
     * Prefixes that change this behaviour:
     * user: - to match the exact username the message is from
     * cs: - to match the following term case-sensitive
     * re: - to match as regex
     * 
     * An item can be prefixed with a user:username, so the username as well
     * as the item after it has to match.
     */
    static class HighlightItem {
        
        private String username;
        private Pattern pattern;
        private String caseSensitive;
        private String caseInsensitive;
        private String category;
        
        HighlightItem(String item) {
            prepare(item);
        }
        
        /**
         * Prepare an item for matching by checking for prefixes and handling
         * the different types accordingly.
         * 
         * @param item 
         */
        private void prepare(String item) {
            item = item.trim();
            if (item.startsWith("re:") && item.length() > 3) {
                compilePattern(item.substring(3));
            } else if (item.startsWith("w:") && item.length() > 2) {
                compilePattern("(?i).*\\b"+item.substring(2)+"\\b.*");
            } else if (item.startsWith("wcs:") && item.length() > 4) {
                compilePattern(".*\\b"+item.substring(4)+"\\b.*");
            } else if (item.startsWith("cs:") && item.length() > 3) {
                caseSensitive = item.substring(3);
            } else if (item.startsWith("cat:")) {
                String[] split = item.split(" ", 2);
                if (split.length == 2) {
                    prepare(split[1]);
                }
                category = split[0].substring(4);
            } else if (item.startsWith("user:")) {
                String[] split = item.split(" ", 2);
                if (split.length == 2) {
                    // There is something after the username, so prepare that
                    // just like another item (but of course added to this object).
                    prepare(split[1]);
                }
                username = split[0].substring(5).toLowerCase();

            } else {
                caseInsensitive = item.toLowerCase();
            }
        }
        
        private void compilePattern(String patternString) {
            try {
                pattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException ex) {
                LOGGER.warning("Invalid regex: " + ex.getLocalizedMessage());
            }
        }
        
        /**
         * Check whether a message matches this item.
         * 
         * @param lowercaseUsername The username in lowercase
         * @param text The text as received
         * @param lowercaseText The text in lowercase (minor optimization, so
         *  it doesn't have to be made lowercase for every item)
         * @return true if it matches, false otherwise
         */
        public boolean matches(User user, String lowercaseUsername, String text, String lowercaseText) {
            if (username != null && !username.equals(lowercaseUsername)) {
                // When a username is defined, but doesn't match, stop right
                // here, because it can't meet all the requirements anymore.
                return false;
            }
            if (category != null && !user.hasCategory(category)) {
                // When a category is defined, but doesn't match, stop right
                // here, because it can't meet all the requirements anymore.
                return false;
            }
            if (pattern != null && pattern.matcher(text).matches()) {
                return true;
            }
            if (caseSensitive != null && text.contains(caseSensitive)) {
                return true;
            }
            if (caseInsensitive != null && lowercaseText.contains(caseInsensitive)) {
                return true;
            }
            if ((username != null || category != null) && pattern == null
                    && caseSensitive == null && caseInsensitive == null) {
                // If username matched (right at the beginning), and no other
                // requirements where defined, then it's a match overall
                return true;
            }
            return false;
        }
        
    }
    
}
