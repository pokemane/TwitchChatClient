
package chatty;

import chatty.gui.MainGui;
import chatty.util.DateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Twitch Chat commands. All the Twitch specific commands like /mod, /timeout..
 * 
 * @author tduva
 */
public class TwitchCommands {
    
    private MainGui g;
    private TwitchClient c;
    
    public TwitchCommands(MainGui g, TwitchClient c) {
        this.g = g;
        this.c = c;
    }
    
    private boolean onChannel(String channel, boolean message) {
        return c.onChannel(channel, message);
    }
    
    private void sendMessage(String channel, String message, String echo) {
        c.sendCommandMessage(channel, message, echo);
    }
    
    protected void commandTimeout(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /to <nick> [time]");
            return;
        }
        String[] parts = parameter.split(" ");
        if (parts.length < 1) {
            g.printLine("Usage: /to <nick> [time]");
        }
        else if (parts.length < 2) {
            timeout(channel, parts[0], 0);
        }
        else {
            try {
                int time = Integer.parseInt(parts[1]);
                timeout(channel, parts[0], time);
            } catch (NumberFormatException ex) {
                g.printLine("Usage: /to <nick> [time] (no valid time specified)");
            }
        }
    }
    
    protected void commandSlowmodeOn(String channel, String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            slowmodeOn(channel, 0);
        }
        else {
            try {
                int time = Integer.parseInt(parameter);
                slowmodeOn(channel, time);
            } catch (NumberFormatException ex) {
                g.printLine("Usage: /slow [time] (invalid time specified)");
            }
        }
    }
    
    protected void commandUnban(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /unban <nick>");
        }
        else {
            unban(channel, parameter);
        }
    }
    
    protected void commandBan(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /ban <nick>");
        }
        else {
            ban(channel, parameter);
        }
    }
    
    protected void commandMod(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /mod <nick>");
        }
        else {
            mod(channel, parameter);
        }
    }
    
    protected void commandUnmod(String channel, String parameter) {
        if (parameter == null) {
            g.printLine("Usage: /unmod <nick>");
        }
        else {
            unmod(channel, parameter);
        }
    }
    
    /**
     * Turn on slowmode with the given amount of seconds or the default time
     * (without specifying a time).
     * 
     * @param channel The name of the channel
     * @param time The time in seconds, 0 or negative numbers will make it give
     *  not time at all
     */
    public void slowmodeOn(String channel, int time) {
        if (onChannel(channel, true)) {
            if (time <= 0) {
                sendMessage(channel,".slow", "Trying to turn on slowmode..");
            }
            else {
                sendMessage(channel,".slow "+time, "Trying to turn on slowmode ("+time+"s)");
            }
        }
    }
    
    /**
     * Turns off slowmode in the given channel.
     * 
     * @param channel The name of the channel.
     */
    public void slowmodeOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".slowoff", "Trying to turn off slowmode..");
        }
    }
    
    /**
     * Turns on subscriber only mode in the given channel.
     * 
     * @param channel The name of the channel.
     */
    public void subscribersOn(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".subscribers", "Trying to turn on subscribers mode..");
        }
    }
    
    public void subscribersOff(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".subscribersoff", "Trying to turn off subscribers mode..");
        }
    }
    
    public void clearChannel(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".clear", "Trying to clear channel..");
        }
    }

    public void ban(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".ban "+name, "Trying to ban "+name+"..");
        }
    }
    
    public void mod(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".mod "+name, "Trying to mod "+name+"..");
        }
    }
    
    public void unmod(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".unmod "+name, "Trying to unmod "+name+"..");
        }
    }
    
    /**
     * Sends a timeout command to the server.
     * 
     * @param channel
     * @param name
     * @param time 
     */
    public void timeout(String channel, String name, int time) {
        if (onChannel(channel, true)) {
            if (time <= 0) {
                sendMessage(channel,".timeout "+name, "Trying to timeout "+name+"..");
            }
            else {
                String formatted = DateTime.duration(time, true, false);
                String onlySeconds = time+"s";
                String timeString = formatted.equals(onlySeconds)
                        ? onlySeconds : onlySeconds+"/"+formatted;
                sendMessage(channel,".timeout "+name+" "+time,
                        "Trying to timeout "+name+" ("+timeString+")");
            }
            
        }
    }
    
    public void unban(String channel, String name) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".unban "+name, "Trying to unban "+name+"..");
        }
    }
    
    public void mods(String channel) {
        if (onChannel(channel, true)) {
            sendMessage(channel,".mods", "Requesting moderator list..");
        }
    }
    
    /**
     * Prase the list of mods as returned from the Twitch Chat. The
     * comma-seperated list should start after the first colon ("The moderators
     * of this room are: ..").
     *
     * @param text The text as received from the Twitch Chat
     * @return A List of moderator names
     */
    public static List<String> parseModsList(String text) {
        int start = text.indexOf(":") + 1;
        List<String> modsList = new ArrayList<>();
        if (start > 1 && text.length() > start) {
            String mods = text.substring(start);
            if (!mods.trim().isEmpty()) {
                String[] modsArray = mods.split(",");
                for (String mod : modsArray) {
                    modsList.add(mod.trim());
                }
            }
        }
        return modsList;
    }
}
