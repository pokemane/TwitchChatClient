
package chatty;

import chatty.util.TimedCounter;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * A single connection to a server that can receive and send data.
 * 
 * @author tduva
 */
public class Connection implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());
    
    private final InetSocketAddress address;
    private final Irc irc;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private int connectionCheckedCount;
    
    private static final int ACTIVITY_STATS_INTERVAL = 60*1000;
    private static final int ACTIVITY_STATS_ACCURACY = 5*1000;
    private final TimedCounter activityStats = new TimedCounter(ACTIVITY_STATS_INTERVAL,
            ACTIVITY_STATS_ACCURACY);
    
    private static final int CONNECT_TIMEOUT = 10*1000; // 10 seconds timeout
    private static final int SOCKET_BLOCK_TIMEOUT = 10*1000; // 10 seconds
    private static final int CHECK_CONNECTION_TIMEOUT = 5*60*1000; // 5 minutes
    
    public Connection(Irc irc, InetSocketAddress address) {
        this.irc = irc;
        this.address = address;
    }
    
    public InetSocketAddress getAddress() {
        return address;
    }
    
    /**
     * Thread that opens the connection and receives data from the connection.
     */
    @Override
    public void run() {
        Charset charset = Charset.forName("UTF-8");
        try {
            LOGGER.info("Opening socket to "+address);
            // Try to connect and open streams
            socket = new Socket();
            socket.connect(address, CONNECT_TIMEOUT);
            LOGGER.info("Connecting to "
                    +socket.getRemoteSocketAddress().toString());
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(),charset)
                    );
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(),charset)
                );
            socket.setSoTimeout(SOCKET_BLOCK_TIMEOUT);
        } catch (UnknownHostException ex) {
            irc.disconnected(Irc.ERROR_UNKNOWN_HOST);
            LOGGER.warning(ex.getLocalizedMessage());
            return;
        } catch (SocketTimeoutException ex) {
            LOGGER.warning(ex.getLocalizedMessage());
            irc.disconnected(Irc.ERROR_SOCKET_TIMEOUT);
            return;
        } catch (IOException ex) {
            LOGGER.warning(ex.getLocalizedMessage());
            irc.disconnected(Irc.ERROR_SOCKET_ERROR,ex.getMessage());
            return;
        }
        // At this point the connection succeeded, but not registered with the
        // IRC server (wich is often called "connected" in this context)
        
        connected = true;
        irc.connected(socket.getInetAddress().toString(),address.getPort());

        while (true) {
            try {
                // Read line (blocks, but has a timeout set)
                String receivedData = in.readLine();
                if (receivedData == null) {
                    break;
                }
                // Data was received
                irc.received(receivedData);
                activity();
            } catch (SocketTimeoutException ex) {
                checkConnection();
            } catch (IOException ex) {
                LOGGER.info("Error reading from socket: "+ex.getLocalizedMessage());
                break;
            }
        }

        // No longer receiving data, so properly close connection if necessary.
        close();
    }
    
    /**
     * Notifies the activity tracker that there was activity on the connection.
     */
    private void activity() {
        activityStats.increase();
        connectionCheckedCount = 0;
    }
    
    /**
     * Checks if the server should be pinged. Takes into account the approximate
     * passed time and how active the connection was before it stopped being
     * active.
     */
    private void checkConnection() {
        connectionCheckedCount++;
        float statsFactor = 1 + activityStats.getCount(false) / 2f;
        float passedTime = connectionCheckedCount * statsFactor * SOCKET_BLOCK_TIMEOUT;
//        System.out.println("Time -"+(CHECK_CONNECTION_TIMEOUT - passedTime) / 1000
//                +" "+activityStats.getCount(false)
//                +" "+statsFactor*connectionCheckedCount);
        if (passedTime > CHECK_CONNECTION_TIMEOUT) {
            LOGGER.info("Pinging server to check connection..");
            send("PING");
            connectionCheckedCount = 0;
            activityStats.getCount(true);
        }
        // count * factor * block = timeout
        // timeout / (1+count/2) = time until ping
        // 300 / (1+x /2)
    }
    
    /**
     * Closes the connection if still connected and cleans up.
     */
    synchronized public void close() {
        if (connected) {
            LOGGER.info("Closing socket.");
            try {
                out.close();
                in.close();
                socket.close();
            } catch (IOException ex) {
                LOGGER.warning("Error closing socket: "+ex.getLocalizedMessage());
            }
            irc.disconnected(Irc.ERROR_CONNECTION_CLOSED);
        }
        connected = false;
    }
    
    /**
     * Send a line of data to the server.
     * 
     * @param data 
     */
    synchronized public void send(String data) {
        irc.sent(data);
        out.print(data+"\r\n");
        out.flush();
        activity();
    }
}
