
package chatty;

import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class TestTimer implements Runnable {

    TwitchClient client;
    int max;
    public TestTimer(TwitchClient client, int max) {
        this.client = client;
        this.max = max;
    }
    
    @Override
    public void run() {
        
        SecureRandom random = new SecureRandom();
        long start = System.currentTimeMillis();
        for (int i=0;i<max;i++) {
            User user = client.users.getUser("", "tduvatest");
            user.setUsercolorManager(client.usercolorManager);
            String line = "Line: "+i+" Kappa FrankerZ abc mah a b c d ef gh ij klm nop qrstu vw";
            //client.userJoined("#test","user"+ new BigInteger(20,random).toString());
            client.g.printMessage("", user, line, false);
            //client.g.printDebug(line);
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestTimer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Total: "+((System.currentTimeMillis() - start) / 1000));
        
    }
    
}
