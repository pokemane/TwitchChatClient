
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HistoryContextMenu;
import chatty.util.api.StreamInfoHistoryItem;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.JComponent;

/**
 * Shows a graph with the viewer count history.
 * 
 * @author tduva
 */
public class ViewerHistory extends JComponent {
    
    private static final float ONE_HOUR = 60*60*1000;
    
    /**
     * The sizes of the points
     */
    private static final int POINT_SIZE = 5;
    
    private static final Color FIRST_COLOR = new Color(20,180,62);
    
    private static final Color SECOND_COLOR = new Color(0,0,220);
    
    private static final Color OFFLINE_COLOR = new Color(255,140,140);
    
    
//        private static final Color FIRST_COLOR = new Color(0,0,255);
//    
//    private static final Color SECOND_COLOR = new Color(30,160,0);
//    
//    private static final Color OFFLINE_COLOR = new Color(200,180,180);
    
    private static final Color HOVER_COLOR = Color.WHITE;
    /**
     * How much tolerance when hovering over entries with the mouse (how far
     * away the mouse pointer can be for it to be still associated with the
     * entry).
     */
    private static final int HOVER_RADIUS = 18;
    /**
     * The font to use for text.
     */
    private static final Font FONT = new Font("Consolas",Font.PLAIN,12);
    /**
     * The margin all around the drawing area.
     */
    private static final int MARGIN = 8;
    
    /**
     * How long the latest viewercount data should be displayed as "now:".
     */
    private static final int CONSIDERED_AS_NOW = 200*1000;
    
    /**
     * How to format times.
     */
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    
    /**
     * The background color, can be set from the outside thus no constant.
     */
    private Color background_color = new Color(250,250,250);
    
    /**
     * Store the current history.
     */
    private LinkedHashMap<Long,StreamInfoHistoryItem> history;
    
    /*
     * Values that only change when the history is updated, so it's enough to
     * update those then
     */
    private int maxValue;
    private int minValue;
    private long startTime;
    private long endTime;
    private long duration;
    /**
     * Time in milliseconds that is displayed, going back from the newest.
     * Values <= 0 indicate that the whole data currentRange is displayed.
     */
    private long currentRange = 0;
    private long fixedStartTime = 0;
    private long fixedEndTime = 0;
    
    private final Map<String, Long> fixedStartTimes = new HashMap<>();
    private final Map<String, Long> fixedEndTimes = new HashMap<>();
    
    private String currentStream = null;
    
    
    /**
     * Store the actual locations of points on the component, this is updated
     * with every redraw.
     */
    private LinkedHashMap<Point, Long> locations = new LinkedHashMap<>();
    /**
     * Store color for every entry, this is updated when a new history is set.
     */
    private LinkedHashMap<Long, Color> colors = new LinkedHashMap<>();
    
    private final ContextMenu contextMenu = new HistoryContextMenu();
    
    /*
     * Values that affect what is rendered.
     */
    private boolean mouseEntered = false;
    private boolean showInfo = true;
    private long hoverEntry = -1;
    private boolean fixedHoverEntry = false;
    private boolean showFullRange = false;
    
    private ViewerHistoryListener listener;

    
    public ViewerHistory() {
        // Test data
        if (Chatty.DEBUG) {
            history = new LinkedHashMap<>();
            history.put((long) 1000*1000, new StreamInfoHistoryItem(5,"Leveling Battlemage","The Elder Scrolls V: Skyrim"));
            history.put((long) 1120*1000, new StreamInfoHistoryItem(4,"Leveling Battlemage","The Elder Scrolls V: Skyrim"));
            history.put((long) 1240*1000, new StreamInfoHistoryItem(4,"Leveling Battlemage","The Elder Scrolls V: Skyrim"));
            history.put((long) 1360*1000, new StreamInfoHistoryItem(6,"Leveling Battlemage","The Elder Scrolls V: Skyrim"));
            history.put((long) 1480*1000, new StreamInfoHistoryItem(8,"Leveling Battlemage","The Elder Scrolls V: Skyrim"));
            history.put((long) 1600*1000, new StreamInfoHistoryItem(8,"Pause","The Elder Scrolls V: Skyrim"));
            history.put((long) 1720*1000, new StreamInfoHistoryItem(10,"Pause","The Elder Scrolls V: Skyrim"));
            history.put((long) 1840*1000, new StreamInfoHistoryItem(12,"Pause","The Elder Scrolls V: Skyrim"));
            history.put((long) 1960*1000, new StreamInfoHistoryItem(12,"Diebesgilde","The Elder Scrolls V: Skyrim"));
            history.put((long) 2080*1000, new StreamInfoHistoryItem(18,"Diebesgilde","The Elder Scrolls V: Skyrim"));
            history.put((long) 2200*1000, new StreamInfoHistoryItem(20,"Diebesgilde","The Elder Scrolls V: Skyrim"));
            history.put((long) 2320*1000, new StreamInfoHistoryItem(22,"Diebesgilde","The Elder Scrolls V: Skyrim"));
            history.put((long) 2440*1000, new StreamInfoHistoryItem(1000,"Diebesgilde","The Elder Scrolls V: Skyrim"));
            history.put((long) 2560*1000, new StreamInfoHistoryItem(2500,"Diebesgilde","The Elder Scrolls V: Skyrim"));
            //history.put((long) 2680*1000, new StreamInfoHistoryItem());
            //history.put((long) 2800*1000, new StreamInfoHistoryItem());
            //history.put((long) 2920*1000, new StreamInfoHistoryItem());
            
            //history.put((long) 1000 * 1000, 40);
            //history.put((long) 1300 * 1000, 290);
//        history.put((long)1600*1000,400);
//        history.put((long)2200*1000,90);
//        history.put((long)3000*1000,123);
//        history.put((long)3300*1000,-1);
//        history.put((long)3600*1000,0);
            setHistory("", history);
        }
        MyMouseListener mouseListener = new MyMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        contextMenu.addContextMenuListener(new MyContextMenuListener());
    }
    
    public void setListener(ViewerHistoryListener listener) {
        this.listener = listener;
    }
    
    
    
    /**
     * Should info (min/max) be shown.
     * 
     * @return 
     */
    private boolean isShowingInfo() {
        if (showInfo) {
            return true;
        }
        else {
            return mouseEntered;
        }
    }
    
    /**
     * Draw the text and graph.
     * 
     * @param g 
     */
    @Override
    public void paintComponent(Graphics g) {
        locations.clear();
        
        // Background
        g.setColor(background_color);
        g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(Color.BLACK);
        
        // Anti-Aliasing
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Font
        FontMetrics fontMetrics = g.getFontMetrics(FONT);
        int fontHeight = fontMetrics.getHeight();
        g.setFont(FONT);

        // If there is any data and no hovered entry is shown, draw current
        // viewercount
        if (history != null && hoverEntry == -1) {
            Integer viewers = history.get(endTime).getViewers();
            long ago = System.currentTimeMillis() - endTime;
            String text;
            if (ago > CONSIDERED_AS_NOW) {
                text = "latest: " + Helper.formatViewerCount(viewers);
            } else {
                text = "now: " + Helper.formatViewerCount(viewers);
            }
            if (viewers == -1) {
                text = "Stream offline";
            }
            int x = getWidth() - fontMetrics.stringWidth(text);
            g.drawString(text, x, 10);
        }
        
        // Default text when no data is present
        if (history == null || history.size() < 2) {
            String text = "No viewer history yet";
            int x = (getWidth() - fontMetrics.stringWidth(text)) / 2;
            int y = getHeight() / 2;
            g.drawString(text, x, y);
            return;
        }
        
        //----------
        // From here only when actual data is to be rendered
        
        
        // Margins
        int vMargin = fontHeight + MARGIN;
        int hMargin = MARGIN;

        // Calculate actual usable size
        double width = getWidth() - hMargin * 2;
        double height = getHeight() - vMargin * 2;
        
        // Show info on hovered entry
        String maxValueText = "max: "+Helper.formatViewerCount(maxValue);
        boolean displayMaxValue = true;
        
        if (hoverEntry != -1) {
            Integer viewers = history.get(hoverEntry).getViewers();
            Date d = new Date(hoverEntry);
            String text = "Viewers: "+Helper.formatViewerCount(viewers)+" ("+sdf.format(d)+")";
            if (viewers == -1) {
                text = "Stream offline ("+sdf.format(d)+")";
            }
            int maxValueEnd = fontMetrics.stringWidth(maxValueText);
            int x = getWidth() - fontMetrics.stringWidth(text);
            if (maxValueEnd > x) {
                displayMaxValue = false;
            }
            g.drawString(text, x, 10);
        }
        
        String minText = "min: "+Helper.formatViewerCount(minValue);
        int minTextWidth = fontMetrics.stringWidth(minText);
        
        // Draw Times
        String timeText = makeTimesText(startTime, endTime);
        int timeTextWidth = fontMetrics.stringWidth(timeText);
        int textX = getWidth() - timeTextWidth;
        g.drawString(timeText, textX, getHeight() - 1);
        
        if (minValue >= 1000 && timeTextWidth + minTextWidth > width) {
            minText = "min: "+minValue / 1000+"k";
        }
        
        // Draw min/max if necessary
        if (isShowingInfo()) {
            if (displayMaxValue) {
                g.drawString(maxValueText, 0, 10);
            }
            g.drawString(minText, 0, getHeight() - 1);
        }
        

        // Calculation factors for calculating the points location
        int range = maxValue - minValue;
        if (showFullRange) {
            range = maxValue;
        }
        if (range == 0) {
            // Prevent division by zero
            range = 1;
        }
        double pixelPerViewer = height / range;
        double pixelPerTime = width / duration;
        
        // Go through all entries and calculate positions
        
        int prevX = -1;
        int prevY = -1;
        Iterator<Entry<Long,StreamInfoHistoryItem>> it = history.entrySet().iterator();
        while (it.hasNext()) { 
            Entry<Long,StreamInfoHistoryItem> entry = it.next();
            
            // Get time and value to draw next
            long time = entry.getKey();
            if (time < startTime || time > endTime) {
                continue;
            }
            long offsetTime = time - startTime;
            
            int viewers = entry.getValue().getViewers();
            if (viewers == -1) {
                viewers = 0;
            }
            
            // Calculate point location
            int x = (int)(hMargin + offsetTime * pixelPerTime);
            int y;
            if (showFullRange) {
                y = (int)(-vMargin + getHeight() - (viewers) * pixelPerViewer);
            }
            else {
                y = (int)(-vMargin + getHeight() - (viewers - minValue) * pixelPerViewer);
            }
            
            // Draw connecting line
            if (prevX != -1) {
                g.drawLine(x,y,prevX,prevY);
            }
            
            // Save point coordinates to be able to draw the line next loop
            prevX = x;
            prevY = y;
            
            locations.put(new Point(x,y),time);
        }
        
        
        
        // Draw points (after lines, so they are in front)
        for (Point point : locations.keySet()) {
            int x = point.x;
            int y = point.y;
            long seconds = locations.get(point);
            
            StreamInfoHistoryItem historyObject = history.get(seconds);
            
            // Highlight hovered entry
            if (seconds == hoverEntry) {
                g.setColor(HOVER_COLOR);
            } else {
                // Draw offline points differently
                if (!historyObject.isOnline()) {
                    g.setColor(OFFLINE_COLOR);
                } else {
                    g.setColor(colors.get(seconds));
                }
            }
            g.fillOval(x  - POINT_SIZE / 2, y  - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
        }
    }
    
    /**
     * Make the text for the start and end time, taking into consideration
     * whether a specific range is displayed.
     * 
     * @param startTime
     * @param endTime
     * @return 
     */
    private String makeTimesText(long startTime, long endTime) {
        String startTimeText = makeTimeText(startTime, fixedStartTime);
        String endTimeText = makeTimeText(endTime, fixedEndTime);
        
        String text = startTimeText+" - "+endTimeText;
        if (fixedStartTime <= 0 && currentRange > 0) {
            text += " (" + duration(currentRange) + "h)";
        }
        return text;
    }
    
    /**
     * Create the text for the start or end time, taking into consideration
     * whether it's a fixed time.
     * 
     * @param time The time in milliseconds to display
     * @param fixedTime The corresponding fixed time in milliseconds
     * @return 
     */
    private String makeTimeText(long time, long fixedTime) {
        Date date = new Date(time);
        if (time == fixedTime) {
            return "|"+sdf.format(date)+"|";
        }
        return sdf.format(date);
    }
    
    /**
     * Gets the starting time for the displayed range. If there is a fixed
     * starting time, use that, otherwise check if there is a range and
     * calculate the starting time from that. Otherwise return 0, meaning the
     * data is displayed from the start.
     * 
     * @param range The time in milliseconds that the data should  be displayed,
     *  going backwards, starting from the very end.
     * @return The start of the data displaying range in milliseconds.
     */
    private long getStartAt(long range) {
        if (fixedStartTime > 0) {
            return fixedStartTime;
        }
        if (range <= 0) {
            return 0;
        }
        long end = -1;
        for (long time : history.keySet()) {
            end = time;
        }
        long startAt = end - range;
        if (startAt < 0) {
            startAt = 0;
        }
        return startAt;
    }
    
    private long getEndAt() {
        if (fixedEndTime > 0 && fixedEndTime > fixedStartTime) {
            return fixedEndTime;
        }
        return -1;
    }
    
    /**
     * Update the start/end/duration/min/max variables which can be changed
     * when the data changes as well when the displayed range changes.
     */
    private void updateVars() {
        long startAt = getStartAt(currentRange);
        long endAt = getEndAt();
        int max = 0;
        int min = -1;
        long start = -1;
        long end = -1;
        for (Long time : history.keySet()) {
            if (time < startAt) {
                continue;
            }
            if (endAt > startAt && time > endAt) {
                continue;
            }
            // Start/End time
            if (start == -1) {
                start = time;
            }
            end = time;
            // Max/min value
            StreamInfoHistoryItem historyObj = history.get(time);
            int viewerCount = historyObj.getViewers();
            if (viewerCount < min || min == -1) {
                min = viewerCount;
            }
            if (viewerCount == -1) {
                min = 0;
            }
            if (viewerCount > max) {
                max = viewerCount;
            }
        }
        
        maxValue = max;
        minValue = min;
        startTime = start;
        endTime = end;
        duration = end - start;
    }
    
    /**
     * Updates the map of colors used for rendering. This creates the
     * alternating colors based on the full stream status, which should only
     * be changed when new data is set.
     */
    private void makeColors() {
        colors.clear();
        Iterator<Entry<Long, StreamInfoHistoryItem>> it = history.entrySet().iterator();
        String prevStatus = null;
        Color currentColor = FIRST_COLOR;
        while (it.hasNext()) {
            Entry<Long, StreamInfoHistoryItem> entry = it.next();
            long time = entry.getKey();
            StreamInfoHistoryItem item = entry.getValue();
            String newStatus = item.getStatusAndGame();
            // Only change color if neither the previous nor the new status
            // are null (offline) and the previous and new status are not equal.
            if (prevStatus != null && newStatus != null
                    && !prevStatus.equals(newStatus)) {
                // Change color
                if (currentColor == FIRST_COLOR) {
                    currentColor = SECOND_COLOR;
                } else {
                    currentColor = FIRST_COLOR;
                }
            }
            colors.put(time, currentColor);
            // Save this status as previous status, but only if it's not
            // offline.
            if (newStatus != null) {
                prevStatus = newStatus;
            }
        }
    }

    /**
     * Sets a new history dataset, update the variables needed for rendering
     * and repaints the display.
     * 
     * @param stream
     * @param newHistory 
     */
    public void setHistory(String stream, LinkedHashMap<Long,StreamInfoHistoryItem> newHistory) {
        manageChannelSpecificVars(stream);
        // Make a copy so changes are not reflected in this
        history = newHistory;
        // Only update variables when the history contains something, else
        // set to null so nothing is rendered that isn't supposed to
        if (history != null && !history.isEmpty()) {
            updateVars();
            checkVars();
            makeColors();
        } else {
            history = null;
        }
        repaint();
    }
    
    /**
     * Saves current fixed times and loads fixed times for the new channel, if
     * the channel changed.
     * 
     * @param stream 
     */
    private void manageChannelSpecificVars(String stream) {
        if (!stream.equals(currentStream)) {
            hoverEntry = -1;
            fixedStartTimes.put(currentStream, fixedStartTime);
            fixedEndTimes.put(currentStream, fixedEndTime);
            currentStream = stream;
            fixedStartTime = 0;
            fixedEndTime = 0;
            if (fixedStartTimes.containsKey(stream)) {
                fixedStartTime = fixedStartTimes.get(stream);
            }
            if (fixedEndTimes.containsKey(stream)) {
                fixedEndTime = fixedEndTimes.get(stream);
            }
        }
    }
    
    /**
     * Checks if start and end times are valid and if not, resets the fixed
     * times.
     */
    private void checkVars() {
        if (startTime == -1 || endTime == -1) {
            fixedStartTime = 0;
            fixedEndTime = 0;
            updateVars();
        }
    }

    /**
     * Set a new background color and repaint.
     * 
     * @param color 
     */
    public void setBackgroundColor(Color color) {
        background_color = color;
        repaint();
    }
    
    /**
     * Sets the time range to this numbre of milliseconds.
     * 
     * @param range 
     */
    public void setRange(long range) {
        this.currentRange = range;
        fixedStartTime = -1;
        fixedEndTime = -1;
        if (history != null) {
            updateVars();
        }
        repaint();
    }

    public void addContextMenuListener(ContextMenuListener l) {
        contextMenu.addContextMenuListener(l);
    }
    
    public void setFixedStartAt(long startAt) {
        if (startAt == endTime) {
            // Don't use last entry as start
            return;
        }
        if (fixedStartTime > 0 && startAt > 0) {
            fixedEndTime = startAt;
        } else {
            fixedStartTime = startAt;
            fixedEndTime = startAt;
        }
        if (history != null) {
            updateVars();
        }
        repaint();
    }
    
    private String duration(long time) {
        float hours = time / ONE_HOUR;
        if (hours < 1) {
            return String.format("%.1f", hours);
        }
        return String.valueOf((int)hours);
    }
    
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            contextMenu.show(this, e.getX(), e.getY());
        }
    }
    
    private class MyContextMenuListener extends ContextMenuAdapter {

        @Override
        public void menuItemClicked(ActionEvent e) {
            if (e.getActionCommand().equals("toggleShowFullVerticalRange")) {
                showFullRange = !showFullRange;
                repaint();
            }
        }
        
    }
    
    private class MyMouseListener extends MouseAdapter {
        /**
         * Toggle displaying stuff on mouse-click.
         *
         * Left-click: Keep info displayed even when outside the component
         * Right-click: Switch between 0-max and min-max rendering
         *
         * @param e
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (e.getClickCount() == 2) {
                    fixedHoverEntry = false;
                    setFixedStartAt(hoverEntry);
                } else {
                    long actualHoverEntry = findHoverEntry(e.getPoint());
                    fixedHoverEntry = actualHoverEntry != -1;
                    if (hoverEntry != actualHoverEntry) {
                        updateHoverEntry(e.getPoint());
                    }
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            mouseEntered = true;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseEntered = false;
            // No entry selected since mouse isn't in the window
            // (entries near the border would still be selected)
            if (fixedHoverEntry) {
                return;
            }
            if (hoverEntry > 0 && listener != null) {
                listener.noItemSelected();
            }
            hoverEntry = -1;
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            openContextMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            openContextMenu(e);
        }

        /**
         * Detect if mouse is moved over a point.
         *
         * @param e
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            if (!fixedHoverEntry) {
                updateHoverEntry(e.getPoint());
            }
        }
    }
    
    /**
     * Finds the key (time) of the currently hovered entry, or -1 if none is
     * hovered.
     * 
     * @param p The current position of the mouse.
     * @return 
     */
    private long findHoverEntry(Point p) {
        double smallestDistance = HOVER_RADIUS;
        long hoverEntry = -1;
        for (Point point : locations.keySet()) {
            double distance = p.distance(point);
            if (distance < HOVER_RADIUS) {
                if (distance < smallestDistance) {
                    hoverEntry = locations.get(point);
                    smallestDistance = distance;
                }
            }
        }
        return hoverEntry;
    }
    
    /**
     * Update the hoverEntry with the currently hovered entry, or none if none
     * is hovered. Repaints and informs listeners if something has changed.
     * 
     * @param p Where the mouse is currently.
     */
    private void updateHoverEntry(Point p) {
        long hoverEntryBefore = hoverEntry;
        hoverEntry = findHoverEntry(p);
        // If something has changed, then redraw.
        if (hoverEntry != hoverEntryBefore) {
            repaint();
            if (listener != null) {
                if (hoverEntry == -1) {
                    listener.noItemSelected();
                } else {
                    StreamInfoHistoryItem item = history.get(hoverEntry);
                    listener.itemSelected(item.getViewers(), item.getTitle(), item.getGame());
                }
            }
        }
    }
    
}

