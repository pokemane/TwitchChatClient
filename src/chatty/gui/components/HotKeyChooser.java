
package chatty.gui.components;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class HotKeyChooser extends JPanel implements KeyListener, ActionListener {
    
    private static final Insets SMALL_BUTTON = new Insets(-1,10,-1,10);
    
    private final JButton set = new JButton("Set");
    private final JButton clear = new JButton("Clear");
    private final JTextField textField = new JTextField(5);
    private final JCheckBox ctrl = new JCheckBox("Ctrl");
    private final JCheckBox alt = new JCheckBox("Alt");
    private final JCheckBox shift = new JCheckBox("Shift");
    private final JCheckBox win = new JCheckBox("Win");
    
    private final String id;
    private final HotKeyChooserListener listener;
    
    private int keyCode = -1;
    private String previousHotkey = null;

    public HotKeyChooser(String id, HotKeyChooserListener listener) {
        ((FlowLayout)getLayout()).setVgap(2);
        this.listener = listener;
        this.id = id;
        textField.addKeyListener(this);
        set.addActionListener(this);
        clear.addActionListener(this);
        
        set.setMargin(SMALL_BUTTON);
        clear.setMargin(SMALL_BUTTON);
        
        add(ctrl);
        add(alt);
        add(shift);
        add(win);
        add(textField);
        add(set);
        add(clear);
        
    }
    
    /**
     * Returns the hotkey as a String. It includes the keycode as well as
     * modifiers (c = ctrl, a = alt, s = shift, w = win).
     * @return 
     */
    public String getHotKey() {
        if (keyCode == -1) {
            return "";
        }
        String result = "";
        if (ctrl.isSelected()) {
            result += "c";
        }
        if (alt.isSelected()) {
            result += "a";
        }
        if (shift.isSelected()) {
            result += "s";
        }
        if (win.isSelected()) {
            result += "w";
        }
        result += keyCode;
        return result;
    }
    
    public void clear() {
        keyCode = -1;
        textField.setText("");
    }
    
    /**
     * Allows to set the hotkey that should be displayed.
     * 
     * @param hotkey 
     */
    public void setHotKey(String hotkey) {
        ctrl.setSelected(false);
        alt.setSelected(false);
        shift.setSelected(false);
        win.setSelected(false);
        if (hotkey.isEmpty()) {
            textField.setText("");
            keyCode = -1;
            return;
        }
        if (hotkey.contains("c")) {
            ctrl.setSelected(true);
        }
        if (hotkey.contains("a")) {
            alt.setSelected(true);
        }
        if (hotkey.contains("s")) {
            shift.setSelected(true);
        }
        if (hotkey.contains("w")) {
            win.setSelected(true);
        }
        try {
            keyCode = Integer.parseInt(hotkey.replaceAll("[^\\d]", ""));
        } catch (NumberFormatException ex) {
            // Invalid hotkey, so stop here
            return;
        }
        textField.setText(KeyEvent.getKeyText(keyCode));
        
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //textField.setText(""+e.getKeyChar());
        e.consume();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //System.out.println(e.getKeyCode());
        int key = e.getKeyCode();
        keyCode = key;
        String text = KeyEvent.getKeyText(key);
        textField.setText(text);
        e.consume();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == set) {
            hotKeyUpdated();
        }
        else if (e.getSource() == clear) {
            clear();
            hotKeyUpdated();
        }
    }
    
    /**
     * Either the set or clear button were pressed, so check if the hotkey
     * has changed since last sending it to the listener and send if necessary.
     */
    private void hotKeyUpdated() {
        if (listener != null) {
            String hotkey = getHotKey();
            if (!hotkey.equals(previousHotkey)) {
                listener.hotkeyUpdated(id, hotkey);
            }
            previousHotkey = hotkey;
        }
    }
}
