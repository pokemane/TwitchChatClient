
package chatty.gui.components.menus;

import chatty.util.api.StreamInfo;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 *
 * @author tduva
 */
public class StreamsContextMenu extends ContextMenu {

    private final List<StreamInfo> streams;
    
    private static final String SORT_SUBMENU = "Sort by..";
    
    public StreamsContextMenu(List<StreamInfo> selected, boolean liveStreams) {
        this.streams = selected;
        
        String s = "";
        String count = "";
        if (selected.size() > 1) {
            s = "s";
            count = String.valueOf(selected.size())+" ";
        }
        
        if (!selected.isEmpty()) {
            String streamSubmenu = "Twitch Stream"+s;
            addItem("openChannelInfo", "Info: "+selected.get(0).getStream());
            addSeparator();
            addItem("stream", "Normal", streamSubmenu);
            addItem("streamPopout", "Popout", streamSubmenu);
            addItem("streamsMultitwitchtv", "Multitwitch.tv", streamSubmenu);
            addItem("streamsSpeedruntv", "Speedrun.tv", streamSubmenu);
            addItem("profile", "Twitch Profile" + s);
            addSeparator();
            addItem("join", "Join " + count + "channel" + s);
            if (liveStreams) {
                addSeparator();
            }
        }
        if (liveStreams) {
            addItem("sortRecent", "Recent", SORT_SUBMENU);
            addItem("sortName", "Name", SORT_SUBMENU);
            addItem("sortGame", "Game", SORT_SUBMENU);
            addItem("showRemovedList", "Removed Streams..");
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.streamsMenuItemClicked(e, streams);
        }
    }
    
}
