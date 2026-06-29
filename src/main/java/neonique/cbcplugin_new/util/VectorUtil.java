package neonique.cbcplugin_new.util;

import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class VectorUtil {

    public static final Vector BLOCK_CENTER_OFFSET = new Vector(0.5, 0, 0.5);

    private VectorUtil () {}

    public static Vector strToVec (String string) {
        String[] splitStr = string.split(" ");
        try {
            return new Vector(
                    Double.parseDouble(splitStr[0]),
                    Double.parseDouble(splitStr[1]),
                    Double.parseDouble(splitStr[2])
            );
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Could not parse string '%s' as 3D vector".formatted(string), e);
        }
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
