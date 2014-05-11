/*
 * From Stackoverflow question:
 * http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java/2581754#2581754
 * Answer by Carter Page: http://stackoverflow.com/users/309596/carter-page
 * 
 * Modified to change sorting order and to work with null values.
 */

package chatty.util;

import java.util.*;

public class MapUtil
{
    /**
     * Sort a Map by values.
     * 
     * @param <K>
     * @param <V>
     * @param map
     * @return 
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> 
        sortByValue( Map<K, V> map )
    {
        List<Map.Entry<K, V>> list =
            new LinkedList<>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
            {
                if (o1.getValue() == null) {
                    return 1;
                }
                if (o2.getValue() == null) {
                    return -1;
                }
                return -(o1.getValue()).compareTo( o2.getValue() );
            }
        } );

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }
}