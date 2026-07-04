package neonique.cbcplugin_new.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.io.ObjectInputFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigUtil {

    private ConfigUtil () {}

    public static class MissingConfigKeyException extends RuntimeException {
        public MissingConfigKeyException(ConfigurationSection config, String key, String expectedType) {
            super("Missing required key '" + key + "' of expected type '" + expectedType + "'" +
                    "in config section '" + config.getCurrentPath());
        }
    }

    public static class InvalidConfigValueException extends RuntimeException {
        public InvalidConfigValueException(ConfigurationSection config, String key, String value, String reason) {
            super("Invalid value '" + value + "' for key '" + key + "'" +
                    "in config section '" + config.getCurrentPath() + ": " + reason);
        }
        public InvalidConfigValueException(ConfigurationSection config, String key, String reason) {
            super("Invalid value for key '" + key + "'" +
                    "in config section '" + config.getCurrentPath() + ": " + reason);
        }
        public InvalidConfigValueException(ConfigurationSection config, String key, Throwable cause) {
            super("Invalid value for key '" + key + "'" + "in config section '" + config.getCurrentPath(), cause);
        }
    }


    public static void requireKey (ConfigurationSection config, String key, String expectedType) {
        if (!config.contains(key)) throw new MissingConfigKeyException(config, key, expectedType);
    }

    public static void requireKeyType (ConfigurationSection config, String key, boolean typeCheck, String expectedType) {
        Object obj = config.get(key);
        if (obj != null && config.contains(key) && !typeCheck) {
            throw new InvalidConfigValueException(config, key, String.valueOf(config.get(key)),
                    "expected " + expectedType + " but got " + obj.getClass().getSimpleName());
        }
    }

    public static Optional<Integer> getInt (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isInt(key), "int");
        return Optional.of(config.getInt(key));
    }

    public static int requireInt (ConfigurationSection config, String key) {
        return getInt(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "int"));
    }

    public static Optional<Double> getDouble (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isDouble(key) || config.isInt(key), "double");
        return Optional.of(config.getDouble(key));
    }

    public static double requireDouble (ConfigurationSection config, String key) {
        return getDouble(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "double"));
    }

    public static Optional<String> getString (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isString(key), "string");
        return Optional.ofNullable(config.getString(key));
    }

    public static String requireString (ConfigurationSection config, String key) {
        return getString(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "string"));
    }

    public static Optional<Boolean> getBoolean (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isBoolean(key), "boolean");
        return Optional.of(config.getBoolean(key));
    }

    public static boolean requireBoolean (ConfigurationSection config, String key) {
        return getBoolean(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "boolean"));
    }

    public static Optional<ConfigurationSection> getConfigurationSection (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isConfigurationSection(key), "ConfigurationList");
        return Optional.ofNullable(config.getConfigurationSection(key));
    }

    public static ConfigurationSection requireConfigurationSection (ConfigurationSection config, String key) {
        return getConfigurationSection(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "ConfigurationList"));
    }

    public static Optional<List<?>> getList (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isList(key), "List");
        return Optional.ofNullable(config.getList(key));
    }

    public static List<?> requireList (ConfigurationSection config, String key) {
        return getList(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "List"));
    }

    public static Optional<List<String>> getStringList (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        requireKeyType(config, key, config.isList(key), "List of String");
        return Optional.of(config.getStringList(key));
    }

    public static List<String> requireStringList (ConfigurationSection config, String key) {
        return getStringList(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "List of String"));
    }

    public static <T extends Enum<T>> Optional<T> getEnum (ConfigurationSection config, String key, Class<T> enumClass) {
        if (!config.contains(key)) return Optional.empty();
        String value = requireString(config, key);
        try {
            return Optional.of(Enum.valueOf(enumClass, value));
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigValueException(config, key, value,
                    "Value must be a constant of enum " + enumClass.getSimpleName());
        }
    }

    public static <T extends Enum<T>> T requireEnum (ConfigurationSection config, String key, Class<T> enumClass) {
        return getEnum(config, key, enumClass)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "Enum " + enumClass.getSimpleName()));
    }

    public static Optional<Vector> getVector (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        List<?> list = requireList(config, key);
        try {
            return Optional.of(VectorUtil.listToVec(list));
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigValueException(config, key, list.toString(),
                    "Could not parse Vector[3]: " + e.getMessage());
        }
    }

    public static Vector requireVector (ConfigurationSection config, String key) {
        return getVector(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "Vector[3]"));
    }

    public static Optional<List<Vector>> getVectorList (ConfigurationSection config, String key) {
        if (!config.contains(key)) return Optional.empty();
        List<?> list = requireList(config, key);
        List<Vector> vectorList = new ArrayList<>();
        // Check all elements in list are lists
        for (Object obj : list) {
            if (obj == null) throw new InvalidConfigValueException(config, key, "Vector list cannot contain null");
            if (!(obj instanceof List<?>)) throw new InvalidConfigValueException(config, key, "Vector must be of type List");
            try {
                vectorList.add(VectorUtil.listToVec(list));
            } catch (IllegalArgumentException e) {
                throw new InvalidConfigValueException(config, key, obj.toString(),
                        "Could not parse vector: " + e.getMessage());
            }
        }
        return Optional.of(vectorList);
    }

    public static List<Vector> requireVectorList (ConfigurationSection config, String key) {
        return getVectorList(config, key)
                .orElseThrow(() -> new MissingConfigKeyException(config, key, "List[Vector[3]]"));
    }

    public static Map<String, ConfigurationSection> getAllConfigSections (ConfigurationSection section) {
        return section.getKeys(false).stream()
                .filter(section::isConfigurationSection)
                .collect(Collectors.toMap(
                        k -> k,
                        k -> section.getConfigurationSection(k)
                ));
    }

}
