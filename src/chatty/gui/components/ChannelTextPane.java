
package chatty.gui.components;

import chatty.gui.MouseClickedListener;
import chatty.gui.UserListener;
import chatty.gui.HtmlColors;
import chatty.gui.LinkListener;
import chatty.gui.StyleServer;
import chatty.gui.UrlOpener;
import chatty.gui.MainGui;
import chatty.User;
import chatty.gui.components.menus.ChannelContextMenu;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.menus.UrlContextMenu;
import chatty.gui.components.menus.UserContextMenu;
import chatty.util.DateTime;
import chatty.util.api.ChatIcons;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonUser;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;



/**
 *
 * @author tduva
 */
public class ChannelTextPane extends JTextPane implements LinkListener, EmoticonUser {
    
    private static final Logger LOGGER = Logger.getLogger(ChannelTextPane.class.getName());
    
    private StyledDocument doc;
    
    private static final Color BACKGROUND_COLOR = new Color(250,250,250);
    
    // Compact mode
    private String compactMode = null;
    private long compactModeStart = 0;
    private int compactModeLength = 0;
    private static final int MAX_COMPACTMODE_LENGTH = 10;
    private static final int MAX_COMPACTMODE_TIME = 30*1000;
    
    /**
     * Min and max buffer size to restrict the setting range
     */
    private static final int BUFFER_SIZE_MIN = 10;
    private static final int BUFFER_SIZE_MAX = 10000;
    
    /**
     * The regex String for finding URLs in messages.
     */
    private static final String urlRegex =
        "(?i)\\b(?:(?:https?)://|www\\.)[-A-Z0-9+&@#/%=~_|$?!:,.]*[A-Z0-9+&@#/%=~_|$]";
    /**
     * The Matcher to use for finding URLs in messages.
     */
    private static final Matcher urlMatcher =
            Pattern.compile(urlRegex).matcher("");
    
    public MainGui main;

    protected LinkController linkController = new LinkController();
    private static StyleServer styleServer;
    
    /**
     * Attribute key for the User object
     */
    public static final String CHATTY_USER = "CHATTY_USER";
    public static final String CHATTY_USER_MESSAGE = "CHATTY_USER_MESSAGE";
    public static final String CHATTY_URL_DELETED = "CHATTY_URL_DELETED";
    public static final String CHATTY_DELETED_LINE = "CHATTY_DELETED_LINE";
    public static final String CHATTY_EMOTICON  = "CHATTY_EMOTICON";
    
    /**
     * Whether the next line needs a newline-character prepended
     */
    private boolean newlineRequired = false;
    
    public static final String TIMESTAMP_ENABLED = "TIMESTAMP_ENABLED";
    public static final String EMOTICONS_ENABLED = "EMOTICONS_ENABLED";
    public static final String USERICONS_ENABLED = "USERICONS_ENABLED";
    public static final String SHOW_BANMESSAGES = "SHOW_BANMESSAGES";
    public static final String AUTO_SCROLL = "AUTO_SCROLL";
    public static final String DELETE_MESSAGES = "DELETE_MESSAGES";
    public static final String DELETED_MESSAGES_MODE = "DELETED_MESSAGES";
    public static final String FRANKERFACEZ_MOD = "FRANKERFACEZ_MOD";
    public static final String BUFFER_SIZE = "BUFFER_SIZE";
    
    private static final long DELETED_MESSAGES_KEEP = 0;
    private static final long DELETED_MESSAGES_HIDE = -1;
    
    private static final SimpleDateFormat TIMESTAMP_USERINFO = new SimpleDateFormat("HH:mm:ss");
    
    protected final Styles styles = new Styles();
    private final ScrollManager scrollManager = new ScrollManager();
    
    public ChannelTextPane(MainGui main, StyleServer styleServer) {
        ChannelTextPane.styleServer = styleServer;
        this.main = main;
        this.setBackground(BACKGROUND_COLOR);
        this.addMouseListener(linkController);
        this.addMouseMotionListener(linkController);
        linkController.setUserListener(main.getUserListener());
        linkController.setLinkListener(this);
        setEditorKit(new MyEditorKit());
        this.setDocument(new MyDocument());
        doc = getStyledDocument();
        setEditable(false);
        DefaultCaret caret = (DefaultCaret)getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        styles.setStyles();
    }
    
    public void setContextMenuListener(ContextMenuListener listener) {
        linkController.setContextMenuListener(listener);
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        linkController.setMouseClickedListener(listener);
    }
    
    /**
     * Can be called when an icon finished loading, so it is displayed correctly.
     * 
     * This seems pretty ineffecient, because it refreshes the whole document.
     */
    @Override
    public void iconLoaded() {
        ((MyDocument)doc).refresh();
    }
 
    /**
     * Prints a message from a user to the main text area
     * 
     * @param user
     * @param text 
     */
    public void printMessage(final User user, final String text, boolean action,
            boolean highlighted) {
        
        closeCompactMode();

        MutableAttributeSet style;
        if (highlighted) {
            style = styles.highlight();
        }
        else {
            style = styles.standard();
        }
        print(getTimePrefix(), style);
        printUser(user,action);
        printSpecials(text, user, style);
        printNewline();
    }
    

    /**
     * Called when a user is banned or timed out and outputs a message as well
     * as deletes the lines of the user.
     * 
     * @param user 
     */
    public void userBanned(User user) {
        if (styles.showBanMessages()) {
            closeCompactMode();
            print(getTimePrefix(), styles.standard());
            print(user.getNick(),styles.nick(user, styles.info()));
            print(" has been banned from talking", styles.info());
            printNewline();
        }
        ArrayList<Integer> lines = getLinesFromUser(user);
        Iterator<Integer> it = lines.iterator();
        /**
         * values > 0 mean strike through, shorten message
         * value == 0 means strike through
         * value < 0 means delete message
         */
        boolean delete = styles.deletedMessagesMode() < DELETED_MESSAGES_KEEP;
        while (it.hasNext()) {
            if (delete) {
                deleteMessage(it.next());
            } else {
                deleteLine(it.next(), styles.deletedMessagesMode());
            }
        }
    }
    
    /**
     * Searches the Document for all lines by the given user.
     * 
     * @param nick
     * @return 
     */
    private ArrayList<Integer> getLinesFromUser(User user) {
        Element root = doc.getDefaultRootElement();
        ArrayList<Integer> result = new ArrayList<>();
        for (int i=0;i<root.getElementCount();i++) {
            Element line = root.getElement(i);
            if (isLineFromUser(line, user)) {
                result.add(i);
            }
        }
        return result;
    }
    
    /**
     * Checks if the given element is a line that is associated with the given
     * User.
     * 
     * @param line
     * @param user
     * @return 
     */
    private boolean isLineFromUser(Element line, User user) {
        for (int j = 0; j < 10; j++) {
            Element element = line.getElement(j);
            User elementUser = getUserFromElement(element);
            // If the User object matches, we're done
            if (elementUser == user) {
                return true;
            }
            // Stop if any User object was found
            if (elementUser != null) {
                return false;
            }
        }
        // No User object was found, so it's probably not a chat message
        return false;
    }
    
    /**
     * Gets the User-object from an element. If there is none, it returns null.
     * 
     * @param element
     * @return The User object or null if none was found
     */
    private User getUserFromElement(Element element) {
        if (element != null) {
            User elementUser = (User)element.getAttributes().getAttribute(CHATTY_USER);
            Boolean isMessage = (Boolean)element.getAttributes().getAttribute(CHATTY_USER_MESSAGE);
            if (isMessage != null && isMessage.booleanValue() == true) {
                return elementUser;
            }
        }
        return null;
    }
    
    /**
     * Crosses out the specified line. This is used for messages that are
     * removed because a user was banned/timed out. Optionally shortens the
     * message to maxLength.
     * 
     * @param line The number of the line in the document
     * @param maxLength The maximum number of characters to shorten the message
     *  to. If maxLength <= 0 then it is not shortened.
     */
    private void deleteLine(int line, int maxLength) {
        Element elementToRemove = doc.getDefaultRootElement().getElement(line);
        if (elementToRemove == null) {
            LOGGER.warning("Line "+line+" is unexpected null.");
            return;
        }
        if (isLineDeleted(elementToRemove)) {
            //System.out.println(line+"already deleted");
            return;
        }
        
        // Determine the offsets of the whole line and the message part
        int[] offsets = getMessageOffsets(elementToRemove);
        if (offsets.length != 2) {
            return;
        }
        int startOffset = elementToRemove.getStartOffset();
        int endOffset = elementToRemove.getEndOffset();
        int messageStartOffset = offsets[0];
        int messageEndOffset = offsets[1];
        int length = endOffset - startOffset;
        int messageLength = messageEndOffset - messageStartOffset - 1;
        
        if (maxLength > 0 && messageLength > maxLength) {
            // Delete part of the message if it exceeds the maximum length
            try {
                int removedStart = messageStartOffset + maxLength;
                int removedLength = messageLength - maxLength;
                doc.remove(removedStart, removedLength);
                length = length - removedLength - 1;
                doc.insertString(removedStart, "..", styles.info());
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
        }
        doc.setCharacterAttributes(startOffset, length, styles.deleted(), false);
        setLineDeleted(startOffset);
        //styles.deleted().
    }
    
    /**
     * Deletes the message of the given line by replacing it with
     * <message deleted>.
     * 
     * @param line The number of the line in the document
     */
    private void deleteMessage(int line) {
        Element elementToRemove = doc.getDefaultRootElement().getElement(line);
        if (elementToRemove == null) {
            LOGGER.warning("Line "+line+" is unexpected null.");
            return;
        }
        if (isLineDeleted(elementToRemove)) {
            //System.out.println(line+"already deleted");
            return;
        }
        int[] messageOffsets = getMessageOffsets(elementToRemove);
        if (messageOffsets.length == 2) {
            int startOffset = messageOffsets[0];
            int endOffset = messageOffsets[1];
            try {
                // -1 to length to not delete newline character (I think :D)
                doc.remove(startOffset, endOffset - startOffset - 1);
                doc.insertString(startOffset, "<message deleted>", styles.info());
                setLineDeleted(startOffset);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location: "+startOffset+"-"+endOffset+" "+ex.getLocalizedMessage());
            }
        }
    }
    
    
    /**
     * Checks if the given line contains an attribute indicating that the line
     * is already deleted.
     * 
     * @param line The element representing this line
     * @return 
     */
    private boolean isLineDeleted(Element line) {
        return line.getAttributes().containsAttribute(CHATTY_DELETED_LINE, true);
    }
    
    /**
     * Adds a attribute to the paragraph at offset to prevent trying to delete
     * it again. 
    * 
     * @param offset 
     */
    private void setLineDeleted(int offset) {
        doc.setParagraphAttributes(offset, 1, styles.deletedLine(), false);
    }
    
    private int[] getMessageOffsets(Element line) {
        int count = line.getElementCount();
        int start = 0;
        for (int i=0;i<count;i++) {
            Element element = line.getElement(i);
            if (element.getAttributes().isDefined(CHATTY_USER)) {
                start = i + 1;
                break;
            }
        }
        if (start < count) {
            int startOffset = line.getElement(start).getStartOffset();
            int endOffset = line.getElement(count - 1).getEndOffset();
            return new int[]{startOffset, endOffset};
        }
        return new int[0];
    }
    
    private Element lastSearchPos = null;
    
    private boolean doesLineExist(Object line) {
        int count = doc.getDefaultRootElement().getElementCount();
        for (int i=0;i<count;i++) {
            if (doc.getDefaultRootElement().getElement(i) == line) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Perform search in the chat buffer. Starts searching for the given text
     * backwards from the last found position.
     * 
     * @param searchText 
     */
    public boolean search(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return false;
        }
        clearSearchResult();
        int count = doc.getDefaultRootElement().getElementCount();
        if (lastSearchPos != null && !doesLineExist(lastSearchPos)) {
            //System.out.println(lastSearchPos+"doesnt exist");
            lastSearchPos = null;
        }
        // Determine if search should start immediately.
        boolean startSearch = lastSearchPos == null;
        searchText = searchText.toLowerCase();
        // Loop through all lines
        for (int i=count-1;i>=0;i--) {
            //System.out.println(i+"/"+count);
            Element element = doc.getDefaultRootElement().getElement(i);
            if (element == lastSearchPos) {
                // If this lines contained the last result, start searching
                // on next line
                startSearch = true;
                if (i == 0) {
                    lastSearchPos = null;
                }
                continue;
            }
            if (!startSearch) {
                continue;
            }
            int startOffset = element.getStartOffset();
            int endOffset = element.getEndOffset() - 1;
            int length = endOffset - startOffset;
            try {
                String text = doc.getText(startOffset, length);
                if (text.toLowerCase().contains(searchText)) {
                    //this.setCaretPosition(startOffset);
                    //this.moveCaretPosition(endOffset);
                    doc.setCharacterAttributes(startOffset, length, styles.searchResult(), false);
                    scrollManager.scrollToOffset(startOffset);
                    //System.out.println(text);
//                    if (i == 0) {
//                        lastSearchPos = null;
//                    } else {
                        lastSearchPos = element;
//                    }
                    break;
                }
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad location");
            }
            lastSearchPos = null;
        }
        if (lastSearchPos == null) {
            scrollManager.scrollDown();
            return false;
        }
        return true;
    }
    
    /**
     * Remove any highlighted search results and start the search from the
     * beginning next time.
     */
    public void resetSearch() {
        clearSearchResult();
        lastSearchPos = null;
    }
    
    /**
     * Removes any prior style changes used to highlight a search result.
     */
    private void clearSearchResult() {
        doc.setCharacterAttributes(0, doc.getLength(), styles.clearSearchResult(), false);
    }

    /**
     * Outputs a clickable and colored nickname.
     * 
     * @param user
     * @param action 
     */
    public void printUser(User user, boolean action) {
        String userName = user.toString();
        if (styles.showUsericons()) {
            printUserIcons(user);
            userName = user.getNick();
        }
        if (action) {
            print("* "+userName+" ",styles.nick(user, null));
        }
        else {
            print(userName+": ",styles.nick(user, null));
        }
        
    }
    
    /**
     * Prints the icons for the given User.
     * 
     * @param user 
     */
    private void printUserIcons(User user) {
        if (user.isBroadcaster()) {
            print("~", styles.broadcasterIcon());
        }
        else if (user.isStaff()) {
            print("!!", styles.staffIcon());
        }
        else if (user.isAdmin()) {
            print("!", styles.adminIcon());
        }
        else if (user.isModerator()) {
            print("@", styles.modIcon());
        }
        if (user.hasTurbo()) {
            print("+", styles.turboIcon());
        }
        if (user.isSubscriber()) {
            print("%", styles.subscriberIcon());
        }
        
    }
    
    /**
     * Removes some chat lines from the top, depending on the current
     * scroll position.
     */
    private void clearSomeChat() {
        int count = doc.getDefaultRootElement().getElementCount();
        int max = styles.bufferSize();
        if (count > max || ( count > max*0.75 && scrollManager.isScrollpositionAtTheEnd() )) {
            removeFirstLines(2);
        }
        //if (doc.getDefaultRootElement().getElementCount() > 500) {
        //    removeFirstLine();
        //}
    }

    /**
     * Removes the specified amount of lines from the top (oldest messages).
     * 
     * @param amount 
     */
    public void removeFirstLines(int amount) {
        if (amount < 1) {
            amount = 1;
        }
        Element firstToRemove = doc.getDefaultRootElement().getElement(0);
        Element lastToRemove = doc.getDefaultRootElement().getElement(amount - 1);
        int startOffset = firstToRemove.getStartOffset();
        int endOffset = lastToRemove.getEndOffset();
        try {
            doc.remove(startOffset,endOffset);
        } catch (BadLocationException ex) {
            Logger.getLogger(ChannelTextPane.class.getName()).log(Level.SEVERE, null, ex);
        }
   }
    
    /**
     * Prints something in compact mode, meaning that nick events of the same
     * type appear in the same line, for as long as possible.
     * 
     * This is mainly used for a compact way of printing joins/parts/mod/unmod.
     * 
     * @param type 
     * @param user 
     */
    public void printCompact(String type, User user) {
        String seperator = ", ";
        if (startCompactMode(type)) {
            // If compact mode has actually been started for this print,
            // print prefix first
            print(getTimePrefix(), styles.compact());
            print(type+": ", styles.compact());
            seperator = "";
        }
        print(seperator, styles.compact());
        print(user.getNick(), styles.nick(user, styles.compact()));
        
        compactModeLength++;
        // If max number of compact prints happened, close compact mode to
        // start a new line
        if (compactModeLength >= MAX_COMPACTMODE_LENGTH) {
            closeCompactMode();
        }
    }
    
    /**
     * Enters compact mode, closes it first if necessary.
     *
     * @param type
     * @return
     */
    private boolean startCompactMode(String type) {
        
        // Check if max time has passed, and if so close first
        long timePassed = System.currentTimeMillis() - compactModeStart;
        if (timePassed > MAX_COMPACTMODE_TIME) {
            closeCompactMode();
        }

        // If this is another type, close first
        if (!type.equals(compactMode)) {
            closeCompactMode();
        }
        
        // Only start if not already/still going
        if (compactMode == null) {
            compactMode = type;
            compactModeStart = System.currentTimeMillis();
            compactModeLength = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Leaves compact mode (if necessary).
     */
    protected void closeCompactMode() {
        if (compactMode != null) {
            printNewline();
            compactMode = null;
        }
    }
    
    /*
     * ########################
     * # General purpose print
     * ########################
     */
    
    protected void printNewline() {
        newlineRequired = true;
    }
    
   /**
     * Prints a regular-styled line (ended with a newline).
     * @param line 
     */
    public void printLine(String line) {
        printLine(line, styles.info());
    }
    
    
    
    /**
     * Prints a line in the given style (ended with a newline).
     * @param line
     * @param style 
     */
    public void printLine(String line,AttributeSet style) {
        // Close compact mode, because this is definately a new line (timestamp)
        closeCompactMode();
        print(getTimePrefix()+line,style);
        newlineRequired = true;
    }

    /**
     * Print special stuff in the text like links and emoticons differently.
     * 
     * First a map of all special stuff that can be found in the text is built,
     * in a way that stuff doesn't overlap with previously found stuff.
     * 
     * Then all the special stuff in this map is printed accordingly, while
     * printing the stuff inbetween with regular style.
     * 
     * @param text 
     * @param user 
     * @param style 
     */
    protected void printSpecials(String text, User user, MutableAttributeSet style) {
        // Where stuff was found
        TreeMap<Integer,Integer> ranges = new TreeMap<>();
        // The style of the stuff (basicially metadata)
        HashMap<Integer,MutableAttributeSet> rangesStyle = new HashMap<>();
        
        findLinks(text, ranges, rangesStyle);
        
        if (styles.showEmoticons()) {
            findEmoticons(text, user, ranges, rangesStyle);
        }
        
        // Actually print everything
        int lastPrintedPos = 0;
        Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Entry<Integer, Integer> range = rangesIt.next();
            int start = range.getKey();
            int end = range.getValue();
            if (start > lastPrintedPos) {
                // If there is anything between the special stuff, print that
                // first as regular text
                print(text.substring(lastPrintedPos, start), style);
            }
            print(text.substring(start, end + 1),rangesStyle.get(start));
            lastPrintedPos = end + 1;
        }
        // If anything is left, print that as well as regular text
        if (lastPrintedPos < text.length()) {
            print(text.substring(lastPrintedPos), style);
        }
        
    }
    
    private void findLinks(String text, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find links
        urlMatcher.reset(text);
        while (urlMatcher.find()) {
            int start = urlMatcher.start();
            int end = urlMatcher.end() - 1;
            if (!inRanges(start, ranges) && !inRanges(end,ranges)) {
                String foundUrl = urlMatcher.group();
                if (checkUrl(foundUrl)) {
                    ranges.put(start, end);
                    rangesStyle.put(start, styles.url(foundUrl));
                }
            }
        }
    }
    
    
    private void findEmoticons(String text, User user, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        for (Integer set : user.getEmoteSet()) {
            HashSet<Emoticon> emoticons = main.emoticons.getEmoticons(set);
            findEmoticons(emoticons, text, ranges, rangesStyle);
        }
        HashSet<Emoticon> emoticons = main.emoticons.getEmoticons();
        findEmoticons(emoticons, text, ranges, rangesStyle);
        
        HashSet<Emoticon> otherEmoticons = main.emoticons.getEmoticons(null);
        findEmoticons(otherEmoticons, text, ranges, rangesStyle);
        
        HashSet<Emoticon> otherEmoticons2 = main.emoticons.getEmoticons(user.getStream());
        findEmoticons(otherEmoticons2, text, ranges, rangesStyle);
    }
    
    private void findEmoticons(HashSet<Emoticon> emoticons, String text,
            Map<Integer, Integer> ranges, Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find emoticons
        Iterator<Emoticon> it = emoticons.iterator();
        while (it.hasNext()) {
            // Check the text for every single emoticon
            Emoticon emoticon = it.next();
            Matcher m = emoticon.getMatcher(text);
            while (m.find()) {
                // As long as this emoticon is still found in the text, add
                // it's position (if it doesn't overlap with something already
                // found) and move on
                int start = m.start();
                int end = m.end() - 1;
                if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
                    if (emoticon.getIcon(this) != null) {
                        ranges.put(start, end);
                        MutableAttributeSet attr = styles.emoticon(emoticon);
                        // Add an extra attribute, making this Style unique
                        // (else only one icon will be output if two of the same
                        // follow in a row)
                        attr.addAttribute("start", start);
                        rangesStyle.put(start,attr);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if the given integer is within the range of any of the key=value
     * pairs of the Map (inclusive).
     * 
     * @param i
     * @param ranges
     * @return 
     */
    private boolean inRanges(int i, Map<Integer,Integer> ranges) {
        Iterator<Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Entry<Integer, Integer> range = rangesIt.next();
            if (i >= range.getKey() && i <= range.getValue()) {
                return true;
            }
        }
        return false;
    }


    
    /**
     * Checks if the Url can be later used as a URI.
     * 
     * @param uriToCheck
     * @return 
     */
    private boolean checkUrl(String uriToCheck) {
        try {
            new URI(uriToCheck);
        } catch (URISyntaxException ex) {
            return false;
        }
        return true;
    }
    
    /*
     * # Without timestamp #
     */
    
    public void printRawLine(String line) {
        printRawLine(line, styles.standard());
    }
    
    public void printRawLine(String line, AttributeSet style) {
        print(line,style);
        newlineRequired = true;
    }
    
    /**
     * Prints the given text.
     * @param text 
     */
    public void print(String text) {
        print(text, styles.standard());
    }
    
    
    /**
     * Prints the given text in the given style. Runs the function that actually
     * adds the text in the Event Dispatch Thread.
     * 
     * @param text
     * @param style 
     */
    public void print(final String text,final AttributeSet style) {
       printInternal(text,style);
    }
 
    /**
     * Adds the text to the main text area. Only for use in the EDT.
     * 
     * @param text
     * @param printStyle 
     */
    private void printInternal(String text, AttributeSet printStyle) {
        //System.out.println("EDT:"+SwingUtilities.isEventDispatchThread());
        try {
            String newline = "";
            if (newlineRequired) {
                newline = "\n";
                newlineRequired = false;
                clearSomeChat();
            }
            doc.insertString(doc.getLength(), newline+text, printStyle);
            // TODO: check how this works
            doc.setParagraphAttributes(doc.getLength(), 1, styles.paragraph(), true);

            // Scrolling if necessary
            if ((scrollManager.isScrollpositionAtTheEnd() || scrollManager.scrolledUpTimeout())
                    && lastSearchPos == null) {
            //if (false) {
                scrollManager.scrollDown();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        scrollManager.scrollDown();
                    }
                });

            }
        } catch (BadLocationException e) {
            System.err.println("BadLocationException");
        }
    }
    
    
    
    /**
     * Sets the scrollpane used for this JTextPane. Should be possible to do
     * this more elegantly.
     * 
     * @param scroll
     */
    
    public void setScrollPane(JScrollPane scroll) {
        scrollManager.setScrollPane(scroll);
    }
    
    /**
     * Makes the time prefix.
     * 
     * @return 
     */
    protected String getTimePrefix() {
        if (styles.timestampFormat() != null) {
            return DateTime.currentTime(styles.timestampFormat())+" ";
        }
        return " ";
    }
    
    private String getTimePrefixUserinfo() {
        return DateTime.currentTime(TIMESTAMP_USERINFO);
    }
    
    public void setChatIcons(ChatIcons icons) {
        styles.setIcons(icons);
    }
    
    public void refreshStyles() {
        styles.refresh();
    }

    /**
     * Simply uses UrlOpener to prompt the user to open the given URL. The
     * prompt is centered on this object (the text pane).
     * 
     * @param url 
     */
    @Override
    public void linkClicked(String url) {
        UrlOpener.openUrlPrompt(this.getTopLevelAncestor(), url);
    }
    
    private class ScrollManager {
        
        private JScrollPane scrollpane;
        private long lastChanged = 0;
        private int lastScrollPosition = 0;
        private static final int SCROLLED_UP_TIMEOUT = 30;
        
        public void setScrollPane(JScrollPane pane) {
            this.scrollpane = pane;
            addListeners();
        }
        
        private void addListeners() {
            scrollpane.getVerticalScrollBar().addAdjustmentListener(
                    new AdjustmentListener() {

                        @Override
                        public void adjustmentValueChanged(AdjustmentEvent e) {
                            if (e.getValue() != lastScrollPosition) {
                                lastChanged = System.currentTimeMillis();
                                lastScrollPosition = e.getValue();
                            }
                        }
                    });
        }
        
        /**
         * Checks if the scroll position is at the end of the document, with
         * some margin or error.
         *
         * @return true if scroll position is at the end, false otherwise
         */
        private boolean isScrollpositionAtTheEnd() {
            JScrollBar vbar = scrollpane.getVerticalScrollBar();
            return vbar.getMaximum() - 20 <= vbar.getValue() + vbar.getVisibleAmount();
        }

        private boolean scrolledUpTimeout() {
            if (!styles.autoScroll()) {
                return false;
            }
            //JScrollBar vbar = scrollpane.getVerticalScrollBar();
            //int current = vbar.getValue();
            //System.out.println("Scroll"+current);
//            if (current != lastScrollPosition) {
//                lastChanged = System.currentTimeMillis();
//                //System.out.println("changed");
//                lastScrollPosition = current;
//            }
            long timePassed = System.currentTimeMillis() - lastChanged;
            //System.out.println(timePassed);
            if (timePassed > 1000 * SCROLLED_UP_TIMEOUT) {
                LOGGER.info("ScrolledUp Timeout (" + timePassed + ")");
                return true;
            }
            return false;
        }

        /**
         * Scrolls to the very end of the document.
         */
        private void scrollDown() {
            try {
                int endPosition = doc.getLength();
                Rectangle bottom = modelToView(endPosition);
                bottom.height = bottom.height + 100;
                scrollRectToVisible(bottom);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad Location");
            }
        }

        private void scrollToOffset(int offset) {
            try {
                Rectangle rect = modelToView(offset);
                //System.out.println(rect);
                scrollRectToVisible(rect);
            } catch (BadLocationException ex) {
                LOGGER.warning("Bad Location");
            }
        }
        
    }
    
    /**
     * Manages everything to do with styles (AttributeSets).
     */
    class Styles {
        /**
         * Styles that are get from the StyleServer
         */
        private final String[] baseStyles = new String[]{"standard","special","info","base","highlight","paragraph"};
        /**
         * Default icons
         */
        private final String[] defaultIcons = new String[]{"mod","turbo","admin","staff","broadcaster","subscriber"};
        /**
         * Stores the styles
         */
        private final HashMap<String,MutableAttributeSet> styles = new HashMap<>();
        /**
         * Stores immutable/unmodified copies of the styles got from the
         * StyleServer for comparison
         */
        private final HashMap<String,AttributeSet> rawStyles = new HashMap<>();
        /**
         * Stores boolean settings
         */
        private final HashMap<String,Boolean> settings = new HashMap<>();
        
        private final Map<String, Integer> numericSettings = new HashMap<>();
        /**
         * Stores all the style types that were changed in the most recent update
         */
        private final ArrayList<String> changedStyles = new ArrayList<>();
        /**
         * Key for the style type attribute
         */
        private final String TYPE = "ChannelTextPanel Style Type";
        /**
         * Whether the global icons styles were already created
         */
        private boolean defaultIconsCreated = false;
        /**
         * Store the timestamp format
         */
        private SimpleDateFormat timestampFormat;
        
        private boolean defaultIconsReplaced = false;
        
        private final Set<Object> defaultIconReplaced = new HashSet<>();
        
        /**
         * Set new icons, either from the Twitch API or from FFZ. Only replace
         * if FFZ is enabled and it's the FFZ, or if FFZ is disabled and it's
         * the Twitch icons.
         * 
         * @param icons 
         */
        public void setIcons(ChatIcons icons) {
            boolean replace = false;
            boolean ffz = settings.get(FRANKERFACEZ_MOD);
            if (!ffz && icons.type == ChatIcons.FRANKERFACEZ) {
                return;
            }
            if (ffz && icons.type == ChatIcons.FRANKERFACEZ
                    || !ffz && icons.type == ChatIcons.TWITCH) {
                replace = true;
            }
            setIcon(subscriberIcon(), icons.getSubscriberIcon(), replace);
            setIcon(modIcon(), icons.getModeratorIcon(), replace);
            setIcon(turboIcon(), icons.getTurboIcon(), replace);
            setIcon(staffIcon(), icons.getStaffIcon(), replace);
            setIcon(adminIcon(), icons.getAdminIcon(), replace);
            setIcon(broadcasterIcon(), icons.getBroadcasterIcon(), replace);
        }
        
        /**
         * Only set icon if it's not null and if either the replace argument is
         * true or if only the default icon was set before.
         * 
         * @param style
         * @param icon 
         */
        private void setIcon(MutableAttributeSet style, ImageIcon icon, boolean replace) {
            if (icon != null && (replace || !defaultIconReplaced.contains(style))) {
                StyleConstants.setIcon(style, addSpaceToIcon(icon));
                defaultIconReplaced.add(style);
            }
        }
        
        /**
         * Creates a new ImageIcon based on the given ImageIcon that has a small
         * space on the side, so it can be displayed properly.
         * 
         * @param icon
         * @return 
         */
        private ImageIcon addSpaceToIcon(ImageIcon icon) {
            int width = icon.getIconWidth();
            int height = icon.getIconHeight();
            int hspace = 3;
            BufferedImage res = new BufferedImage(width + hspace, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = res.getGraphics();
            g.drawImage(icon.getImage(), 0, 0, null);
            g.dispose();

            return new ImageIcon(res);
        }
        
        /**
         * Set any global icons, but only once. This should be done as soon
         * as the styles are first set.
         */
        private void setDefaultIcons() {
            if (defaultIconsCreated) {
                return;
            }
            defaultIconsCreated = true;
            for (String name : defaultIcons) {
                styles.put(name, makeDefaultIconStyle(name));
            }
        }
        
        /**
         * Create a style for the given icon name. This involves modifying the
         * icon to fit correctly.
         * 
         * @param iconName
         * @return 
         */
        private MutableAttributeSet makeDefaultIconStyle(String iconName) {
            ImageIcon icon = styleServer.getIcon(iconName);
            SimpleAttributeSet style = new SimpleAttributeSet(nick());
            if (icon != null) {
                StyleConstants.setIcon(style, addSpaceToIcon(icon));
            }
            return style;
        }
        
        /**
         * Get the current styles from the StyleServer and also set some
         * other special styles based on that.
         * 
         * @return 
         */
        public boolean setStyles() {
            LOGGER.info("setStyles");
            changedStyles.clear();
            boolean somethingChanged = false;
            for (String styleName : baseStyles) {
                if (loadStyle(styleName)) {
                    somethingChanged = true;
                    changedStyles.add(styleName);
                }
            }
            
            // Additional styles
            SimpleAttributeSet nick = new SimpleAttributeSet(base());
            StyleConstants.setBold(nick, true);
            styles.put("nick", nick);
            
            MutableAttributeSet paragraph = styles.get("paragraph");
            //StyleConstants.setLineSpacing(paragraph, 0.3f);
            paragraph.addAttribute(CHATTY_DELETED_LINE, false);
            styles.put("paragrahp", paragraph);
            
            SimpleAttributeSet deleted = new SimpleAttributeSet();
            StyleConstants.setStrikeThrough(deleted, true);
            StyleConstants.setUnderline(deleted, false);
            deleted.addAttribute(CHATTY_URL_DELETED, true);
            styles.put("deleted", deleted);
            
            SimpleAttributeSet deletedLine = new SimpleAttributeSet();
            deletedLine.addAttribute(CHATTY_DELETED_LINE, true);
            //StyleConstants.setAlignment(deletedLine, StyleConstants.ALIGN_RIGHT);
            styles.put("deletedLine", deletedLine);
            
            SimpleAttributeSet searchResult = new SimpleAttributeSet();
            StyleConstants.setBackground(searchResult, styleServer.getColor("searchResult"));
            styles.put("searchResult", searchResult);
            
            SimpleAttributeSet clearSearchResult = new SimpleAttributeSet();
            StyleConstants.setBackground(clearSearchResult, new Color(0,0,0,0));
            styles.put("clearSearchResult", clearSearchResult);
            
            setBackground(styleServer.getColor("background"));
            
            // Load other stuff from the StyleServer
            setDefaultIcons();
            setSettings();
            
            return somethingChanged;
        }
        
        /**
         * Loads some settings from the StyleServer.
         */
        private void setSettings() {
            addSetting(EMOTICONS_ENABLED,true);
            addSetting(USERICONS_ENABLED, true);
            addSetting(TIMESTAMP_ENABLED, true);
            addSetting(SHOW_BANMESSAGES, false);
            addSetting(AUTO_SCROLL, true);
            addSetting(DELETE_MESSAGES, false);
            addSetting(FRANKERFACEZ_MOD, false);
            addNumericSetting(DELETED_MESSAGES_MODE, 30, -1, 9999999);
            addNumericSetting(BUFFER_SIZE, 250, BUFFER_SIZE_MIN, BUFFER_SIZE_MAX);
            timestampFormat = styleServer.getTimestampFormat();
        }
        
        /**
         * Gets a single boolean setting from the StyleServer.
         * 
         * @param key
         * @param defaultValue 
         */
        private void addSetting(String key, boolean defaultValue) {
            MutableAttributeSet loadFrom = styleServer.getStyle("settings");
            Object obj = loadFrom.getAttribute(key);
            boolean result = defaultValue;
            if (obj != null && obj instanceof Boolean) {
                result = (Boolean)obj;
            }
            settings.put(key, result);
        }
        
        private void addNumericSetting(String key, int defaultValue, int min, int max) {
            MutableAttributeSet loadFrom = styleServer.getStyle("settings");
            Object obj = loadFrom.getAttribute(key);
            int result = defaultValue;
            if (obj != null && obj instanceof Number) {
                result = ((Number)obj).intValue();
            }
            if (result > max) {
                result = max;
            }
            if (result < min) {
                result = min;
            }
            numericSettings.put(key, result);
        }
        
        /**
         * Retrieves a single style from the StyleServer and compares it to
         * the previously saved style.
         * 
         * @param name
         * @return true if the style has changed, false otherwise
         */
        private boolean loadStyle(String name) {
            MutableAttributeSet newStyle = styleServer.getStyle(name);
            AttributeSet oldStyle = rawStyles.get(name);
            if (oldStyle != null && oldStyle.isEqual(newStyle)) {
                // Nothing in the style has changed, so nothing further to do
                return false;
            }
            // Save immutable copy of new style for next comparison
            rawStyles.put(name, newStyle.copyAttributes());
            // Add type attribute to the style, so it can be recognized when
            // refreshing the styles in the document
            newStyle.addAttribute(TYPE, name);
            styles.put(name, newStyle);
            return true;
        }
        
        /**
         * Retrieves the current styles and updates the elements in the document
         * as necessary. Scrolls down since font size changes and such could
         * move the scroll position.
         */
        public void refresh() {
            if (!setStyles()) {
                return;
            }

            LOGGER.info("Update styles (only types "+changedStyles+")");
            Element root = doc.getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                Element line = root.getElement(i);
                for (int j = 0; j < line.getElementCount(); j++) {
                    Element element = line.getElement(j);
                    String type = (String)element.getAttributes().getAttribute(TYPE);
                    int start = element.getStartOffset();
                    int end = element.getEndOffset();
                    int length = end - start;
                    if (type == null) {
                        type = "base";
                    }
                    MutableAttributeSet style = styles.get(type);
                    // Only change style if this style type was different from
                    // the previous one
                    // (seems to be faster than just setting all styles)
                    if (changedStyles.contains(type)) {
                        if (type.equals("paragraph")) {
                            //doc.setParagraphAttributes(start, length, rawStyles.get(type), false);
                        } else {
                            doc.setCharacterAttributes(start, length, style, false);
                        }
                    }
                }
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    scrollManager.scrollDown();
                }
            });
        }
        
        public MutableAttributeSet base() {
            return styles.get("base");
        }
        
        public MutableAttributeSet info() {
            return styles.get("info");
        }
        
        public MutableAttributeSet compact() {
            return styles.get("special");
        }
        
        public MutableAttributeSet standard() {
            return styles.get("standard");
        }
        
        public MutableAttributeSet nick() {
            return styles.get("nick");
        }
        
        public MutableAttributeSet deleted() {
            return styles.get("deleted");
        }
        
        public MutableAttributeSet deletedLine() {
            return styles.get("deletedLine");
        }
        
        public MutableAttributeSet paragraph() {
            return styles.get("paragrahp");
        }
        
        public MutableAttributeSet highlight() {
            return styles.get("highlight");
        }
        
        public MutableAttributeSet searchResult() {
            return styles.get("searchResult");
        }
        
        public MutableAttributeSet clearSearchResult() {
            return styles.get("clearSearchResult");
        }
        
        /**
         * Makes a style for the given User, containing the User-object itself
         * and the user-color. Changes the color to hopefully improve readability.
         * 
         * @param user The User-object to base this style on
         * @return 
         */
        public MutableAttributeSet nick(User user, MutableAttributeSet style) {
            SimpleAttributeSet userStyle;
            if (style == null) {
                userStyle = new SimpleAttributeSet(nick());
                userStyle.addAttribute(CHATTY_USER_MESSAGE, true);
                Color userColor = user.getColor();
                if (!user.hasChangedColor()) {
                    userColor = HtmlColors.correctReadability(userColor, getBackground());
                    user.setCorrectedColor(userColor);
                }
                StyleConstants.setForeground(userStyle, userColor);
            }
            else {
                userStyle = new SimpleAttributeSet(style);
            }
            userStyle.addAttribute(CHATTY_USER, user);
            return userStyle;
        }
        
        public MutableAttributeSet subscriberIcon() {
            return styles.get("subscriber");
        }
        
        public MutableAttributeSet modIcon() {
            return styles.get("mod");
        }
        
        public MutableAttributeSet turboIcon() {
            return styles.get("turbo");
        }
        
        public MutableAttributeSet adminIcon() {
            return styles.get("admin");
        }
        
        public MutableAttributeSet staffIcon() {
            return styles.get("staff");
        }
        
        public MutableAttributeSet broadcasterIcon() {
            return styles.get("broadcaster");
        }
        
        public boolean showTimestamp() {
            return settings.get(TIMESTAMP_ENABLED);
        }
        
        public boolean showUsericons() {
            return settings.get(USERICONS_ENABLED);
        }
        
        public boolean showEmoticons() {
            return settings.get(EMOTICONS_ENABLED);
        }
        
        public boolean showBanMessages() {
            return settings.get(SHOW_BANMESSAGES);
        }
        
        public boolean autoScroll() {
            return settings.get(AUTO_SCROLL);
        }
        
        public boolean deleteMessages() {
            return settings.get(DELETE_MESSAGES);
        }
        
        public int deletedMessagesMode() {
            return (int)numericSettings.get(DELETED_MESSAGES_MODE);
        }
        
        /**
         * Make a link style for the given URL.
         * 
         * @param url
         * @return 
         */
        public MutableAttributeSet url(String url) {
            SimpleAttributeSet urlStyle = new SimpleAttributeSet(standard());
            StyleConstants.setUnderline(urlStyle, true);
            urlStyle.addAttribute(HTML.Attribute.HREF, url);
            return urlStyle;
        }
        
        /**
         * Make a style with the given icon.
         * 
         * @param icon
         * @return 
         */
        public MutableAttributeSet emoticon(Emoticon emoticon) {
            // Does this need any other attributes e.g. standard?
            SimpleAttributeSet emoteStyle = new SimpleAttributeSet();
            StyleConstants.setIcon(emoteStyle, emoticon.getIcon(ChannelTextPane.this));
            emoteStyle.addAttribute(CHATTY_EMOTICON, emoticon);
            return emoteStyle;
        }
        
        public SimpleDateFormat timestampFormat() {
            return timestampFormat;
        }
        
        public int bufferSize() {
            return (int)numericSettings.get(BUFFER_SIZE);
        }
    }
    
    
}

/**
 * Adds a way to refresh the (whole) document.
 * 
 * This is currently used to display Icons after they are fully loaded, although
 * there should be a better way to do this.
 * 
 * @author tduva
 */
class MyDocument extends DefaultStyledDocument {
    
    public void refresh() {
        refresh(0, getLength());
    }
    
    public void refresh(int offset, int len) {
        DefaultDocumentEvent changes = new DefaultDocumentEvent(offset,len, DocumentEvent.EventType.CHANGE);
        Element root = getDefaultRootElement();
        Element[] removed = new Element[0];
        Element[] added = new Element[0];
        changes.addEdit(new ElementEdit(root, 0, removed, added));
        changes.end();
        fireChangedUpdate(changes);
    }

}

/**
 * Replaces some Views by custom ones to change display behaviour.
 * 
 * @author tduva
 */
class MyEditorKit extends StyledEditorKit {

    @Override
    public ViewFactory getViewFactory() {
        return new StyledViewFactory();
    }
 
    static class StyledViewFactory implements ViewFactory {

        @Override
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                if (kind.equals(AbstractDocument.ContentElementName)) {
                    return new WrapLabelView(elem);
                } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                    return new MyParagraphView(elem);
                } else if (kind.equals(AbstractDocument.SectionElementName)) {
                    return new ChatBoxView(elem, View.Y_AXIS);
                } else if (kind.equals(StyleConstants.ComponentElementName)) {
                    return new ComponentView(elem);
                } else if (kind.equals(StyleConstants.IconElementName)) {
                    return new MyIconView(elem);
                }
            }
            return new LabelView(elem);
        }

    }
}

/**
 * Changes the FlowStrategy to increase performance when i18n is enabled in the
 * Document. Not quite sure why this works.. ;)
 * 
 * @author tduva
 */
class MyParagraphView extends ParagraphView {
    
    public static int MAX_VIEW_SIZE = 50;
    
    public MyParagraphView(Element elem) {
        super(elem);
        //System.out.println(strategy.getClass());
        strategy = new MyParagraphView.MyFlowStrategy();
        //System.out.println(strategy.getClass());
    }
    
    public static class MyFlowStrategy extends FlowStrategy {
        
        @Override
        protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
            View res = super.createView(fv, startOffset, spanLeft, rowIndex);
            
            if (res.getEndOffset() - res.getStartOffset() > MAX_VIEW_SIZE) {
                //res = res.createFragment(startOffset, startOffset + MAX_VIEW_SIZE);
            }
            return res;
        }
    }
    
//    @Override
//    public int getResizeWeight(int axis) {
//        return 0;
//    }
}


/**
 * Starts adding text at the bottom instead of at the top.
 * 
 * @author tduva
 */
class ChatBoxView extends BoxView {
    
    public ChatBoxView(Element elem, int axis) {
        super(elem,axis);
    }
    
    @Override
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {

        super.layoutMajorAxis(targetSpan,axis,offsets,spans);
        int textBlockHeight = 0;
        int offset = 0;
 
        for (int i = 0; i < spans.length; i++) {

            textBlockHeight += spans[i];
        }
        offset = (targetSpan - textBlockHeight);
        //System.out.println(offset);
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] += offset;
        }

    }
}    

/**
 * Always wrap long words.
 * 
 * @author tduva
 */
class WrapLabelView extends LabelView {

    public WrapLabelView(Element elem) {
        super(elem);
    }

    /**
     * Always return 0 for the X_AXIS of the minimum span, so long words are
     * always wrapped.
     * 
     * @param axis
     * @return 
     */
    @Override
    public float getMinimumSpan(int axis) {
        switch (axis) {
            case View.X_AXIS:
                return 0;
            case View.Y_AXIS:
                return super.getMinimumSpan(axis);
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
        }
    }
}

/**
 * Changes the position of the icon slightly, so it overlaps with following text
 * as to not take as much space. Not perfect still, but ok.
 * 
 * @author tduva
 */
class MyIconView extends IconView {
    public MyIconView(Element elem) {
        super(elem);
    }
    
    private static final int lineHeight = 20;
    
    @Override
    public float getAlignment(int axis) {
        //System.out.println(this.getElement());

        if (axis ==  View.Y_AXIS) {
            //System.out.println(this.getElement());
//            float height = super.getPreferredSpan(axis);
//            double test = 1.5 - lineHeight / height * 0.5;
//            System.out.println(height+" "+test+" "+this.getAttributes());
//            return (float)test;
            return 1f;
            
        }
        return super.getAlignment(axis);
    }
    
    @Override
    public float getPreferredSpan(int axis) {
        if (axis == View.Y_AXIS) {
            float height = super.getPreferredSpan(axis);
//            float test = lineHeight / height;
            float test = 0.7f;
            //System.out.println(test);
            height *= test;
            return height;
        }
        return super.getPreferredSpan(axis);
    }
}

/**
 * Detects any clickable text in the document and reacts accordingly. It shows
 * the appropriate cursor when moving over it with the mouse and reacts to
 * clicks on clickable text.
 * 
 * It knows to look for links and User objects at the moment.
 * 
 * @author tduva
 */
class LinkController extends MouseAdapter implements MouseMotionListener {
    
    private static Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static Cursor normalCursor = Cursor.getDefaultCursor();
    
    /**
     * When a User is clicked, the User object is send here
     */
    private UserListener userListener;
    /**
     * When a link is clicked, the String with the url is send here
     */
    private LinkListener linkListener;
    
    private MouseClickedListener mouseClickedListener;
    
    private ContextMenuListener contextMenuListener;
    
    private ContextMenu defaultContextMenu;
    
    /**
     * Set the object that should receive the User object once a User is clicked
     * 
     * @param listener 
     */
    public void setUserListener(UserListener listener) {
        userListener = listener;
    }
    
    /**
     * Set the object that should receive the url String once a link is clicked
     * 
     * @param listener 
     */
    public void setLinkListener(LinkListener listener) {
        linkListener = listener;
    }
    
    public void setMouseClickedListener(MouseClickedListener listener) {
        mouseClickedListener = listener;
    }
    
    /**
     * Set the listener for all context menus.
     * 
     * @param listener 
     */
    public void setContextMenuListener(ContextMenuListener listener) {
        contextMenuListener = listener;
        if (defaultContextMenu != null) {
            defaultContextMenu.addContextMenuListener(listener);
        }
    }
    
    /**
     * Set the context menu for when no special context menus (user, link) are
     * appropriate.
     * 
     * @param contextMenu 
     */
    public void setDefaultContextMenu(ContextMenu contextMenu) {
        defaultContextMenu = contextMenu;
        contextMenu.addContextMenuListener(contextMenuListener);
    }
   
    /**
     * Handles mouse presses. This is favourable to mouseClicked because it
     * might work better in a fast moving chat and you won't select text
     * instead of opening userinfo etc.
     * 
     * @param e 
     */
    @Override
    public void mousePressed(MouseEvent e) {
        
        if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
            String url = getUrl(e);
            if (url != null && !isUrlDeleted(e)) {
                if (linkListener != null) {
                    linkListener.linkClicked(url);
                }
                return;
            }
            User user = getUser(e);
            if (user != null) {
                if (userListener != null) {
                    userListener.userClicked(user);
                }
                return;
            }
        }
        else if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            openContextMenu(e);
        }
    }
    
    /**
     * Handle clicks (pressed and released) on the text pane.
     * 
     * @param e 
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseClickedListener != null) {
            // Doing this on mousePressed will prevent selection of text,
            // because this is used to change the focus to the input
            mouseClickedListener.mouseClicked();
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        
        JTextPane text = (JTextPane)e.getSource();
        
        String url = getUrl(e);
        if ((url != null && !isUrlDeleted(e)) || getUser(e) != null) {
            text.setCursor(handCursor);
        } else {
            text.setCursor(normalCursor);
        }
    }
    
    /**
     * Gets the URL from the MouseEvent (if there is any).
     * 
     * @param e
     * @return The URL or null if none was found.
     */
    private String getUrl(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (String)(attributes.getAttribute(HTML.Attribute.HREF));
        }
        return null;
    }
    
    private boolean isUrlDeleted(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            Boolean deleted = (Boolean)attributes.getAttribute(ChannelTextPane.CHATTY_URL_DELETED);
            if (deleted == null) {
                return false;
            }
            return (Boolean)(deleted);
        }
        return false;
    }
    
    /**
     * Gets the User object from the MouseEvent (if there is any).
     * 
     * @param e
     * @return The User object or null if none was found.
     */
    private User getUser(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (User)(attributes.getAttribute(ChannelTextPane.CHATTY_USER));
        }
        return null;
    }
    
    private Emoticon getEmoticon(MouseEvent e) {
        AttributeSet attributes = getAttributes(e);
        if (attributes != null) {
            return (Emoticon)(attributes.getAttribute(ChannelTextPane.CHATTY_EMOTICON));
        }
        return null;
    }
    
    /**
     * Gets the attributes from the element in the document the mouse is
     * pointing at.
     * 
     * @param e
     * @return The attributes of this element or null if the mouse wasn't
     *          pointing at an element
     */
    private AttributeSet getAttributes(MouseEvent e) {
        JTextPane text = (JTextPane)e.getSource();
        Point mouseLocation = new Point(e.getX(), e.getY());
        int pos = text.viewToModel(mouseLocation);
        
        if (pos >= 0) {
            StyledDocument doc = text.getStyledDocument();
            Element element = doc.getCharacterElement(pos);
            return element.getAttributes();
        }
        return null;
    }
    
    private void openContextMenu(MouseEvent e) {
        User user = getUser(e);
        String url = getUrl(e);
        Emoticon emote = getEmoticon(e);
        JPopupMenu m;
        if (user != null) {
            m = new UserContextMenu(user, contextMenuListener);
        }
        else if (url != null) {
            m = new UrlContextMenu(url, isUrlDeleted(e), contextMenuListener);
        }
        else if (emote != null) {
            m = new EmoteContextMenu(emote);
        }
        else {
            if (defaultContextMenu == null) {
                m = new ChannelContextMenu(contextMenuListener);
            } else {
                m = defaultContextMenu;
            }
        }
        m.show(e.getComponent(), e.getX(), e.getY());
    }
    
}

