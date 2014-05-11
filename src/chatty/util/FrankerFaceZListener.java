
package chatty.util;

import chatty.util.api.ChatIcons;
import chatty.util.api.Emoticon;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author tduva
 */
public interface FrankerFaceZListener {
    public void emoticonsReceived(HashMap<String, HashSet<Emoticon>> newEmotes);
    public void iconsReceived(String channel, ChatIcons icons);
}
