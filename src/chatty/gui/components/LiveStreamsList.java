
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.StreamsContextMenu;
import chatty.util.api.StreamInfo;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 *
 * @author tduva
 */
public class LiveStreamsList extends JList<StreamInfo> {

    private static final int UPDATE_TIMER_DELAY = 5;
    private static final int CHECK_DELAY = 20;
    private static final int REPAINT_DELAY = 60;
    
    private final SortedListModel<StreamInfo> data;
    private final List<ContextMenuListener> contextMenuListeners;
    private final LiveStreamListener liveStreamListener;
    /**
     * How long after the last stream status change, that it uses the TITLE_NEW
     * border.
     */
    private static final int STREAMINFO_NEW_TIME = 180;
    
    private ListDataChangedListener listDataChangedListener;
    
    private JPopupMenu lastContextMenu;
    
    private long lastChecked = 0;
    private long lastRepainted = 0;
    
    public LiveStreamsList(LiveStreamListener liveStreamListener) {
        data = new SortedListModel<>();
        setModel(data);
        setCellRenderer(new MyCellRenderer());
        contextMenuListeners = new ArrayList<>();
        this.liveStreamListener = liveStreamListener;
        addListeners();
        new UpdateTimer();
    }

    public void addContextMenuListener(ContextMenuListener listener) {
        if (listener != null) {
            contextMenuListeners.add(listener);
        }
    }
    
    public void addStreams(List<StreamInfo> infos) {
        for (StreamInfo info : infos) {
            addStream(info);
        }
    }
    
    public void setComparator(Comparator<StreamInfo> comparator) {
        data.setComparator(comparator);
    }
    
    /**
     * Adds or removes and readds a stream.
     * 
     * @param info 
     */
    public void addStream(StreamInfo info) {
        if (info.isValid() && info.getOnline()) {
            if (data.contains(info)) {
                data.remove(info);
            }
            data.add(info);
            itemAdded(info);
        } else if (data.contains(info)) {
            data.remove(info);
            itemRemoved(info);
        }
        listDataChanged();
    }
    
    /**
     * Adds the listener to notify about list data changes.
     * 
     * @param listener 
     */
    public void addListDataChangedListener(ListDataChangedListener listener) {
        this.listDataChangedListener = listener;
    }
    
    /**
     * Checks all added streams and removes invalid ones.
     */
    private void checkStreams() {
        if ((System.currentTimeMillis() - lastChecked) / 1000 < CHECK_DELAY) {
            return;
        }
        lastChecked = System.currentTimeMillis();
        Set<StreamInfo> remove = new HashSet<>();
        for (StreamInfo info : data) {
            if (!info.isValid() || !info.getOnline()) {
                remove.add(info);
            }
        }
        // Remove invalid items
        for (StreamInfo info : remove) {
            data.remove(info);
            itemRemoved(info);
        }
        // Update and inform only if items were actually removed
        if (remove.isEmpty()) {
            listDataChanged();
        }
    }
    
    /**
     * Clears the selection if dialog is not active.
     */
    private void checkToClearSelection() {
        if (!isFocusOwner() &&
                (lastContextMenu == null || !lastContextMenu.isVisible())) {
            clearSelection();
        }
    }
    
    /**
     * Call to all the regular update stuff.
     */
    private void update() {
        checkToClearSelection();
        checkStreams();
        checkToRepaint();
    }
    
    /**
     * Repaints the list on a set delay to update colors.
     */
    private void checkToRepaint() {
        long timePassed = (System.currentTimeMillis() - lastRepainted) / 1000;
        if (timePassed > REPAINT_DELAY) {
            repaint();
            lastRepainted = System.currentTimeMillis();
        }
    }
    
    /**
     * Inform the listener that the list data has changed (new items, removed
     * items, updated new streams count).
     */
    private void listDataChanged() {
        if (listDataChangedListener != null) {
            listDataChangedListener.listDataChanged();
        }
    }
    
    private void itemRemoved(StreamInfo item) {
        if (listDataChangedListener != null) {
            listDataChangedListener.itemRemoved(item);
        }
    }
    
    private void itemAdded(StreamInfo item) {
        if (listDataChangedListener != null) {
            listDataChangedListener.itemAdded(item);
        }
    }
    
    /**
     * Open context menu for this user, if the event points at one.
     * 
     * @param e 
     */
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            selectClicked(e, false);
            List<StreamInfo> selected = getSelectedValuesList();
            StreamsContextMenu m = new StreamsContextMenu(selected, true);
            for (ContextMenuListener cml : contextMenuListeners) {
                m.addContextMenuListener(cml);
            }
            lastContextMenu = m;
            m.show(this, e.getX(), e.getY());
        }
    }
    
    /**
     * Adds selection of the clicked element, or removes selection if no
     * element was clicked.
     * 
     * @param e
     * @param onlyOutside 
     */
    private void selectClicked(MouseEvent e, boolean onlyOutside) {
        int index = locationToIndex(e.getPoint());
        Rectangle bounds = getCellBounds(index, index);
        if (bounds != null && bounds.contains(e.getPoint())) {
            if (!onlyOutside) {
                if (isSelectedIndex(index)) {
                    addSelectionInterval(index, index);
                } else {
                    setSelectedIndex(index);
                }
            }
        } else {
            clearSelection();
        }
    }
    
    private void addListeners() {
        ComponentListener l = new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
// Trick from kleopatra:
// http://stackoverflow.com/questions/7306295/swing-jlist-with-multiline-text-and-dynamic-height
                // next line possible if list is of type JXList
                // list.invalidateCellSizeCache();
                // for core: force cache invalidation by temporarily setting fixed height
                setFixedCellHeight(10);
                setFixedCellHeight(-1);
            }

        };
        addComponentListener(l);
        
        addMouseListener(new MouseAdapter() {
        
            @Override
            public void mousePressed(MouseEvent e) {
                selectClicked(e, true);
                openContextMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    StreamInfo info = getSelectedValue();
                    if (info != null && liveStreamListener != null) {
                        liveStreamListener.liveStreamClicked(info);
                    }
                }
            }
            
        });
    }
    
    /**
     * To prevent horizontal scrolling and allow for tracking of the viewport
     * width.
     * 
     * @return 
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    /**
     * Custom renderer to use a text area and borders etc.
     */
    private static class MyCellRenderer extends DefaultListCellRenderer {

        private static final Border PADDING =
                BorderFactory.createEmptyBorder(2, 3, 1, 3);
        private static final Border MARGIN =
                BorderFactory.createEmptyBorder(4, 3, 1, 3);
        private static final Border TITLE =
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY);
        private static final Border TITLE_SELECTED =
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(165,165,165));
        private static final Border TITLE_NEW =
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);
        
        private final JTextArea area;
        
        public MyCellRenderer() {
            area = new JTextArea();
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            //System.out.println("Getting rubberstamp for "+value);
            StreamInfo info = (StreamInfo)value;
            // Make Text
            String text = info.getTitle();
            if (info.getGame() != null) {
                text += "\n("+info.getGame()+")";
            }
            area.setText(text);
            // Adjust size
            int width = list.getWidth();
            if (width > 0) {
                area.setSize(width, Short.MAX_VALUE);
            }
            // Add Borders
            String title = info.getStream()+" ("+Helper.formatViewerCount(info.getViewers())+" Viewers)";
            Border titleBaseBorder = isSelected ? TITLE_SELECTED : TITLE;
            if (info.getStatusChangeTimeAgo() < STREAMINFO_NEW_TIME) {
                titleBaseBorder = TITLE_NEW;
            }
            Border titleBorder = BorderFactory.createTitledBorder(titleBaseBorder,
                    title, TitledBorder.CENTER, TitledBorder.TOP, null, null);
            Border innerBorder = BorderFactory.createCompoundBorder(titleBorder, PADDING);
            Border border = BorderFactory.createCompoundBorder(MARGIN, innerBorder);
            area.setBorder(border);
            // Selected Color
            if (isSelected) {
                area.setBackground(list.getSelectionBackground());
                area.setForeground(list.getSelectionForeground());
            } else {
                area.setBackground(list.getBackground());
                area.setForeground(list.getForeground());
            }
            return area;
        }
    }

    /**
     * Periodically check what of the list should be updated. This is used for
     * clearing focus, removing old elements etc.
     */
    private class UpdateTimer extends Timer {

        public UpdateTimer() {
            
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            update();
                        }
                    });
                }
            };
            this.schedule(task, UPDATE_TIMER_DELAY*1000, UPDATE_TIMER_DELAY*1000);
        }
        
        
    }
    
    public interface ListDataChangedListener {
        void listDataChanged();
        void itemRemoved(StreamInfo item);
        void itemAdded(StreamInfo item);
    }
    
}
