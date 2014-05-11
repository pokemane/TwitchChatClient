
package chatty;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author tduva
 */
public class ReconnectionTimer extends Timer {

    TimerTask task;
    
    public ReconnectionTimer(final TwitchClient client, final int reason, int seconds) {
        task = new TimerTask() {

            @Override
            public void run() {
                client.reconnect(reason);
            }
        };
        this.schedule(task, 1000*seconds);
    }
    
}
