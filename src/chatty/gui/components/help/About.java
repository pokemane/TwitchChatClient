
package chatty.gui.components.help;

import chatty.Chatty;
import chatty.gui.UrlOpener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;

/**
 * Simple Frame that shows a HTML page as About/Help.
 * 
 * @author tduva
 */
public class About extends JFrame implements ActionListener {
    
    private static final Logger LOGGER = Logger.getLogger(About.class.getName());
    
    private static final String DEFAULT_PAGE = "help.html";
    
    private final JTextPane textPane = new JTextPane();
    private String reference;
    private String currentPage = "";
    private String currentReference = "";
    
    public About() {
        setTitle("About/Help - Chatty");
        
        // Text pane
        JScrollPane scroll = new JScrollPane(textPane);
        textPane.setEditable(false);
        textPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Jump to another place in the document
                    String url = e.getURL().toString();
                    String protocol = e.getURL().getProtocol();
                    if (protocol.equals("http") || protocol.equals("https")) {
                        UrlOpener.openUrlPrompt(About.this, url, true);
                    } else if (protocol.equals("file") || protocol.equals("jar")) {
                        String path = e.getURL().getFile();
                        String file = path.substring(path.lastIndexOf("/")+1);
                        open(file, e.getURL().getRef());
                    } else {
                        jumpTo(e.getURL().getRef());
                    }
                }
            }
        });
        textPane.addPropertyChangeListener("page", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                pageLoaded();
            }
        });
        
        
        add(scroll);
        scroll.setPreferredSize(new Dimension(630,400));
        loadPage(DEFAULT_PAGE);
        
        // Close button
        JButton button = new JButton("Close");
        button.addActionListener(this);
        add(button,BorderLayout.SOUTH);
        
        pack();
        
    }
    
    /**
     * Loads the given page into the window.
     * 
     * @param page 
     */
    private void loadPage(String page) {
        try {
            textPane.setPage(getClass().getResource(page));
            currentPage = page;
        } catch (IOException ex) {
            LOGGER.warning("Invalid page: "+page+" ("+ex.getLocalizedMessage()+")");
        }
    }

    /**
     * Close window (or reload page if in DEBUG mode)
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (Chatty.DEBUG) {
            Document doc = textPane.getDocument();
            doc.putProperty(Document.StreamDescriptionProperty, null);
            reference = currentReference;
            loadPage(currentPage);
        }
        else {
            setVisible(false);
        }
    }
    
    public void open(String page, final String ref) {
        if (page == null) {
            page = DEFAULT_PAGE;
        }
        if (currentPage != null && currentPage.equals(page)) {
            jumpTo(ref);
        } else {
            reference = ref;
        }
        loadPage(page);
        
    }
    
    private void jumpTo(final String ref) {
        textPane.scrollToReference(ref);
        currentReference = ref;
    }
    
    private void pageLoaded() {
        if (reference != null) {
            jumpTo(reference);
            reference = null;
        }
    }
}
