package neonique.cbcplugin_new.gamemodes;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GameSetting<T> {

    private final String name;
    private final Function<String, T> parser;
    private final Function<T, String> display;
    private final Supplier<List<String>> tabCompletions;
    private final String desc;
    private T value;

    public GameSetting (String name,
                        T defaultValue,
                        Function<String, T> parser,
                        Function<T, String> display,
                        Supplier<List<String>> tabCompletions,
                        String desc) {

        this.name = name;
        this.parser = parser;
        this.display = display;
        this.desc = desc;
        this.tabCompletions = tabCompletions;
        this.value = defaultValue;

    }

    public GameSetting (String name,
                        T defaultValue,
                        Function<String, T> parser,
                        Function<T, String> display,
                        String desc) {
        this(name, defaultValue, parser, display, List::of, desc);
    }

    public String name () {
        return name;
    }

    public void parseAndSetValue (String strValue) {
        value = parser.apply(strValue);
    }

    public String desc () {
        return desc;
    }

    public T value () {
        return value;
    }

    public String valueString () {
        return display.apply(value);
    }

    public List<String> tabCompletions () {
        return tabCompletions.get();
    }

    public static GameSetting<Integer> intSettingWithBounds (String name, String desc, int defaultValue,
                                                             int min, int max) {
        return new GameSetting<>(
                name, defaultValue,
                (s) -> {
                    try {
                        int num = Integer.parseInt(s);
                        if (num < min || num > max)
                            throw new IllegalArgumentException("Value is required to be within " + min + " and " + max);
                        return num;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Value not parseable as integer");
                    }
                },
                String::valueOf, desc
        );
    }

    public static GameSetting<Boolean> booleanSetting (String name, String desc, boolean defaultValue) {
        return new GameSetting<>(
                name, defaultValue,
                (s) -> {
                    if (s.equalsIgnoreCase("true")) return true;
                    if (s.equalsIgnoreCase("false")) return false;
                    else throw new IllegalArgumentException("Value must be 'true' or 'false'");
                },
                String::valueOf, desc
        );
    }

    public static <E extends Enum<E>> GameSetting<E> enumSetting (String name, String desc, E defaultValue, Class<E> enumClass) {
        return new GameSetting<E>(
                name, defaultValue,
                (s) -> {
                    try {
                        return Enum.valueOf(enumClass, name);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Value must be one of "
                            + Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", ")));
                    }
                },
                Enum::name, desc
        );
    }



}
