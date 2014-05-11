
package chatty.util.api;

/**
 * Holds info (username, scopes) about the current access token.
 * 
 * @author tduva
 */
public class TokenInfo {
    
    private final boolean channel_commercials;
    private final boolean channel_editor;
    private final boolean chat_access;
    private final boolean user_read;
    private final String name;
    private final boolean valid;
    
    public TokenInfo() {
        valid = false;
        channel_commercials = false;
        channel_editor = false;
        chat_access = false;
        user_read = false;
        name = null;
    }
    
    public TokenInfo(String name, boolean chat_access, boolean channel_editor,
            boolean channel_commercials, boolean user_read) {
        this.name = name;
        this.channel_editor = channel_editor;
        this.channel_commercials = channel_commercials;
        this.chat_access = chat_access;
        this.user_read = user_read;
        valid = true;
    }
    
    public String getUsername() {
        return name;
    }
    
    public boolean isTokenValid() {
        return valid;
    }
    
    public boolean channel_editor() {
        return channel_editor;
    }
    
    public boolean channel_commercials() {
        return channel_commercials;
    }
    
    public boolean chat_access() {
        return chat_access;
    }
    
    public boolean user_read() {
        return user_read;
    }
    
}
