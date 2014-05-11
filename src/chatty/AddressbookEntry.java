
package chatty;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable entry for an addressbook, only containing the name and categories
 * for now.
 * 
 * @author tduva
 */
public class AddressbookEntry {
    
    private final String name;
    private final Set<String> categories;
    
    /**
     * Creates a new entry.
     * 
     * @param name The name, shouldn't be empty or null, will be turned into
     * lowercase, shouldn't contain any spaces
     * @param categories The categories, can be empty, but not null
     */
    public AddressbookEntry(String name, Set<String> categories) {
        this.name = name.toLowerCase();
        this.categories = new HashSet<>(categories);
    }
    
    /**
     * Creates a new entry based on another entry, but adding the given
     * categories.
     * 
     * @param other The other entry to base this new entry on.
     * @param categories The categories to add, can be empty, but not null.
     */
    public AddressbookEntry(AddressbookEntry other, Set<String> categories) {
        this.name = other.getName().toLowerCase();
        this.categories = new HashSet<>(other.getCategories());
        this.categories.addAll(categories);
    }
    
    /**
     * Gets the name for this entry. Always in lowercase.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns a copy of the categories.
     * 
     * @return 
     */
    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }
    
    /**
     * Checks if this entry contains the given category.
     * 
     * @param category
     * @return 
     */
    public boolean hasCategory(String category) {
        return categories.contains(category);
    }
    
    @Override
    public String toString() {
        return name+" "+categories.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof AddressbookEntry) {
            return name.equals(((AddressbookEntry)o).getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.name);
        return hash;
    }
    
}
