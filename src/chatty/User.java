
package chatty;

import chatty.gui.HtmlColors;
import chatty.gui.NamedColor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a single user on a specific channel.
 * 
 * @author tduva
 */
public class User implements Comparable {
    
    private static final NamedColor[] defaultColors = {
        new NamedColor("Red", 255, 0, 0),
        new NamedColor("Blue", 0, 0, 255),
        new NamedColor("Green", 0, 255, 0),
        new NamedColor("FireBrick", 178, 34, 34),
        new NamedColor("Coral", 255, 127, 80),
        new NamedColor("YellowGreen", 154, 205, 50),
        new NamedColor("OrangeRed", 255, 69, 0),
        new NamedColor("SeaGreen", 46, 139, 87),
        new NamedColor("GoldenRod", 218, 165, 32),
        new NamedColor("Chocolate", 210, 105, 30),
        new NamedColor("CadetBlue", 95, 158, 160),
        new NamedColor("DodgerBlue", 30, 144, 255),
        new NamedColor("HotPink", 255, 105, 180),
        new NamedColor("BlueViolet", 138, 43, 226),
        new NamedColor("SpringGreen", 0, 255, 127)
    };
    
    private final String nick;
    private String fullNick;
    private final String channel;
    
    private Addressbook addressbook;
    
    private UsercolorManager colorManager;
    private Color color = HtmlColors.decode("");
    private Color correctedColor = HtmlColors.decode("");
    private boolean hasDefaultColor = true;
    private boolean hasCorrectedColor = false;
    private boolean hasChangedColor = false;
    
    private boolean online = false;
    private boolean isModerator = false;
    private boolean isBroadcaster = false;
    private boolean isAdmin = false;
    private boolean isStaff = false;
    private boolean hasTurbo = false;
    private boolean isSubscriber = false;
    
    private int probablySubscriber = 0;
    
    private final Set<Integer> emoteSets = new HashSet<>();
    private final List<Message> messages = new ArrayList<>();
    
    private final long createdAt = System.currentTimeMillis();
    private int numberOfMessages = 0;
    private int numberOfLines = 0;
    private static final int MAXLINES = 20;
    
    public User(String nick, String channel) {
        this.nick = nick;
        this.channel = channel;
        setDefaultColor();
        updateFullNick();
    }
    
    public void setUsercolorManager(UsercolorManager manager) {
        this.colorManager = manager;
    }
    
    public void setAddressbook(Addressbook addressbook) {
        this.addressbook = addressbook;
    }
    
    /**
     * Gets the categories from the addressbook for this user.
     * 
     * @return The categories or <tt>null</tt> if this user could not be found or if no
     * addressbook was specified.
     */
    public Set<String> getCategories() {
        if (addressbook != null) {
            AddressbookEntry entry = addressbook.get(nick);
            if (entry != null) {
                return entry.getCategories();
            }
        }
        return null;
    }
    
    public List<String> getPresetCategories() {
        if (addressbook != null) {
            return addressbook.getCategories();
        }
        return null;
    }
    
    public boolean hasCategory(String category) {
        if (addressbook != null) {
            AddressbookEntry entry = addressbook.get(nick);
            if (entry != null) {
                return entry.hasCategory(category);
            }
        }
        return false;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public String getStream() {
        return channel.replace("#", "");
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public int getNumberOfMessages() {
        return numberOfMessages;
    }
    
    public int getMaxNumberOfLines() {
        return MAXLINES;
    }
    
    public boolean maxNumberOfLinesReached() {
        if (numberOfLines > MAXLINES) {
            return true;
        }
        return false;
    }
    
    /**
     * Adds a single chatmessage with the current time.
     * 
     * @param line 
     */
    public synchronized void addMessage(String line) {
        addLine(new TextMessage(System.currentTimeMillis(), line));
        numberOfMessages++;
    }
    
    /**
     * Adds a single ban with the current time.
     */
    public synchronized void addBan() {
        addLine(new BanMessage(System.currentTimeMillis()));
    }
    
    /**
     * Adds a Message.
     * 
     * @param message The Message object containig the data for this line.
     */
    private void addLine(Message message) {
        messages.add(message);
        if (messages.size() > MAXLINES) {
            messages.remove(0);
        }
        numberOfLines++;
    }
    
    /**
     * Returns a copy of the current messages (defensive copying because it
     * might be used while being modified concurrently).
     * 
     * @return 
     */
    public synchronized List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public synchronized String getNick() {
        return nick;
    }
    
    public synchronized Color getColor() {
        if (colorManager != null) {
            Color result = colorManager.getColor(this);
            if (result != null) {
                hasChangedColor = true;
                return result;
            } else {
                hasChangedColor = false;
            }
        }
        return color;
    }
    
    public synchronized Color getPlainColor() {
        return color;
    }
    
    public synchronized boolean hasChangedColor() {
        return hasChangedColor;
    }
    
    public synchronized Color getCorrectedColor() {
        return correctedColor;
    }
    
    public synchronized boolean hasCorrectedColor() {
        return hasCorrectedColor;
    }
    
    public synchronized void setColor(String htmlColor) {
        hasDefaultColor = false;
        color = HtmlColors.decode(htmlColor);
    }
    
    public synchronized void setCorrectedColor(Color color) {
        correctedColor = color;
        hasCorrectedColor = true;
    }
    
    /**
     * Whether a Color has been set explicitely.
     * 
     * @return 
     */
    public synchronized boolean hasDefaultColor() {
        return hasDefaultColor;
    }
    
    /**
     * Sets the default color based on the nick. Based on what bGeorge posted.
     */
    private void setDefaultColor() {
        String name = nick.toLowerCase();
        int n = name.codePointAt(0) + name.codePointAt(name.length() - 1);
        color = defaultColors[n % defaultColors.length];
        hasDefaultColor = true;
    }
    
    public synchronized void setOnline(boolean online) {
        this.online = online;
    }
    
    public synchronized boolean isOnline() {
        return online;
    }

    @Override
    public synchronized int compareTo(Object o) {
        if (!(o instanceof User)) {
            return 0;
        }
        User u = (User)o;
        
        int broadcaster = 4;
        int admin = 3;
        int moderator = 2;
        int subscriber = 1;
        
        int result = 0;
        if (this.isAdmin() || this.isStaff()) {
            result = result - admin;
        }
        if (u.isAdmin() || u.isStaff()) {
            result = result + admin;
        }
        if (this.isBroadcaster()) {
            result = result - broadcaster;
        }
        if (u.isBroadcaster()) {
            result = result + broadcaster;
        }
        if (this.isSubscriber()) {
            result = result - subscriber;
        }
        if (u.isSubscriber()) {
            result = result + subscriber;
        }
        if (this.isModerator()) {
            result = result - moderator;
        }
        if (u.isModerator()) {
            result = result + moderator;
        }
        if (result == 0) {
            return this.getNick().compareToIgnoreCase(u.getNick());
        }
        return result;
    }
    
    @Override
    public synchronized String toString() {
        return fullNick;
    }
    
    public synchronized void setMode(String mode) {
        if (mode.equals("o")) {
            setModerator(true);
        } else {
            setModerator(false);
        }
    }

    public synchronized boolean isModerator() {
        return isModerator;
    }
    
    public synchronized boolean isAdmin() {
        return isAdmin;
    }
    
    public synchronized boolean isStaff() {
        return isStaff;
    }
    
    public synchronized boolean isBroadcaster() {
        return isBroadcaster;
    }
    
    public synchronized boolean isSubscriber() {
        return isSubscriber;
    }
    
    public synchronized boolean hasTurbo() {
        return hasTurbo;
    }
    
    public synchronized void setModerator(boolean mod) {
        isModerator = mod;
        updateFullNick();
    }
    
    public synchronized void setAdmin(boolean admin) {
        isAdmin = admin;
        updateFullNick();
    }
    
    public synchronized void setStaff(boolean staff) {
        isStaff = staff;
        updateFullNick();
    }
    
    public synchronized void setTurbo(boolean turbo) {
        hasTurbo = turbo;
        updateFullNick();
    }
    
    public synchronized void setSubscriber(boolean subscriber) {
        isSubscriber = subscriber;
        updateFullNick();
    }
    
    public synchronized void setProbablySubscriber() {
        if (isSubscriber) {
            return;
        }
        probablySubscriber++;
        if (probablySubscriber > 1) {
            setSubscriber(true);
        }
    }
    
    public synchronized void setBroadcaster(boolean broadcaster) {
        isBroadcaster = broadcaster;
        updateFullNick();
    }
    
    private void updateFullNick() {
        fullNick = getModeSymbol()+nick;
    }
    
    public synchronized String getModeSymbol() {
        String result = "";
        if (isSubscriber()) {
            result += "%";
        }
        if (hasTurbo()) {
            result += "+";
        }
        if (isAdmin()) {
            return "!"+result;
        }
        if (isStaff()) {
            return "!!"+result;
        }
        if (isBroadcaster()) {
            return "~"+result;
        }
        if (isModerator()) {
            return "@"+result;
        }
        return result;
    }
    
    /**
     * Sets the set of emoticons available for this user.
     * 
     * Splits at any character that is not a number, but usually it should
     * be a string like: [1,5,39]
     * 
     * @param newEmoteSets 
     */
    public synchronized void setEmoteSets(String newEmoteSets) {
        String[] split = newEmoteSets.split("[^0-9]");
        emoteSets.clear();
        for (String emoteSet : split) {
            if (!emoteSet.isEmpty()) {
                emoteSets.add(Integer.parseInt(emoteSet));
            }
        }
    }
    
    /**
     * Gets a Set of Integer containing the emotesets available to this user.
     * Defensive copying because it might be iterated over while being modified
     * concurrently.
     * 
     * @return 
     */
    public synchronized Set<Integer> getEmoteSet() {
        return new HashSet<>(emoteSets);
    }
    
    
    
    public static class Message {
        
        public static final int MESSAGE = 0;
        public static final int BAN = 1;
        
        private final Long time;
        private final int type;
        
        public Message(int type, Long time) {
            this.time = time;
            this.type = type;
        }
        
        public int getType() {
            return type;
        }
        
        public long getTime() {
            return time;
        }
    }
    
    public static class TextMessage extends Message {
        private final String text;
        
        public TextMessage(Long time, String message) {
            super(MESSAGE, time);
            this.text = message;
        }
        
        public String getText() {
            return text;
        }
    }
    
    public static class BanMessage extends Message {
        public BanMessage(Long time) {
            super(BAN, time);
        }
        
    }
    
}
