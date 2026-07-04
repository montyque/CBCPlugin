package neonique.cbcplugin_new.util;

import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class VectorUtil {

    public static final Vector VECTOR_ZERO = new Vector(0, 0, 0);
    public static final Vector BLOCK_CENTER_OFFSET = new Vector(0.5, 0, 0.5);

    private VectorUtil () {}

    public static Vector listToVec (List<?> list) {

        // Ensure list has 3 elements to match 3 dimensions of vector
        if (list.size() != 3) throw new IllegalArgumentException("Vector must have exactly 3 numerical elements");

        // Avoid potential rounding errors if all three numbers are integers
        if (list.stream().allMatch(v -> v instanceof Integer)) {
            return new Vector ((Integer) list.get(0), (Integer) list.get(1), (Integer) list.get(2));
        }

        if (list.stream().allMatch(v -> v instanceof Number)) {
            return new Vector (
                    ((Number) list.get(0)).doubleValue(),
                    ((Number) list.get(1)).doubleValue(),
                    ((Number) list.get(2)).doubleValue()
            );
        }

        throw new IllegalArgumentException("Not all objects in list are of type Number");

    }

    public static Location vecToLocation (Vector v, World w) {
        return new Location(w, v.getX(), v.getY(), v.getZ());
    }

    public static List<Double> vecToList (Vector v) {
        return List.of(v.getX(), v.getY(), v.getZ());
    }

    public static Vector strToVec (String string, Vector offset) {
        String[] splitStr = string.split(" ");
        try {
            return new Vector(
                    Double.parseDouble(splitStr[0]),
                    Double.parseDouble(splitStr[1]),
                    Double.parseDouble(splitStr[2])
            ).add(offset);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Could not parse string '%s' as 3D vector".formatted(string), e);
        }
    }

    public static Vector strToVec (String string) {
        return strToVec(string, VECTOR_ZERO);
    }

    public static Vector strToBlockVec (String string) {
        return strToVec(string, BLOCK_CENTER_OFFSET);
    }

    public static List<Vector> strListToVecList (List<String> strings) {
        return strings.stream()
                .map(VectorUtil::strToVec)
                .toList();
    }

    public static List<Vector> strListToVecList (List<String> strings, Vector offset) {
        return strings.stream()
                .map(VectorUtil::strToVec)
                .map(v -> v.add(offset))
                .toList();
    }

    public static List<Vector> blockStrListToVecList (List<String> strings) {
        return strListToVecList(strings, BLOCK_CENTER_OFFSET);
    }

    public static Map<String, Vector> blockStrMapToVecMap (Map<String, Object> strings) {
        return strMapToVecMap(strings, BLOCK_CENTER_OFFSET);
    }

    public static Map<String, Vector> strMapToVecMap (Map<String, Object> strings, Vector off) {
        return strings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Optional.of(e.getValue())
                                .filter(o -> o instanceof String)
                                .map(o -> strToVec((String) o))
                                .orElseThrow(IllegalArgumentException::new)
                ));
    }

}
