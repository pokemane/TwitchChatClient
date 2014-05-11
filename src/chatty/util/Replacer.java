
package chatty.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces Strings based on a Map. Creates a single regex to search for parts
 * that should be replaced.
 * 
 * @author tduva
 */
public class Replacer {
    
    private final Map<String, String> replacements;
    private final Matcher matcher;
    
    /**
     * Create a Replacer that replaces the keys in the Map with their values.
     * 
     * @param replacements 
     */
    public Replacer(Map<String, String> replacements) {
        this.replacements = replacements;
        StringBuilder sb = new StringBuilder("(");
        for (String item : replacements.keySet()) {
            if (sb.length() != 1) {
                sb.append("|");
            }
            sb.append(item);
        }
        sb.append(")");
        matcher = Pattern.compile(sb.toString()).matcher("");
    }
    
    /**
     * Replaces anything in the input String, based on the Map specified for
     * this Replacer.
     * 
     * @param input
     * @return 
     */
    public String replace(String input) {
        matcher.reset(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, replacements.get(matcher.group(1)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}