
package chatty.util.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface definition for API response results.
 * 
 * @author tduva
 */
public interface TwitchApiResultListener {
    void receivedEmoticons(HashMap<Integer,HashSet<Emoticon>> emoticons);
    void receivedChatIcons(String channel, ChatIcons icons);
    void gameSearchResult(Set<String> games);
    void tokenVerified(String token, TokenInfo tokenInfo);
    void runCommercialResult(String stream, String text, int result);
    void putChannelInfoResult(int result);
    void receivedChannelInfo(String channel, ChannelInfo info, int result);
    void accessDenied();
}
