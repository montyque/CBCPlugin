package neonique.cbcplugin_new.commands.arguments;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.mapconfig.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EnumArgumentParser<T extends Enum<T>> implements CustomArgument.CustomArgumentInfoParser<T, String> {

    private final Supplier<Collection<T>> validValues;
    private final Class<T> enumClass;

    public EnumArgumentParser (Class<T> enumClass) {
        this.enumClass = enumClass;
        validValues = () -> List.of(enumClass.getEnumConstants());
    }

    public EnumArgumentParser (Class<T> enumClass, Supplier<Collection<T>> validValues) {
        this.enumClass = enumClass;
        this.validValues = validValues;
    }

    @Override
    public T apply(CustomArgument.CustomArgumentInfo<String> info) throws CustomArgument.CustomArgumentException {

        Collection<T> values = validValues.get();
        String input = info.input().toUpperCase();

        try {
            T value = T.valueOf(enumClass, input);
            if (values.contains(value)) {
                return T.valueOf(enumClass, input);
            } else {
                throw CustomArgument.CustomArgumentException.fromAdventureComponent(
                        Component.text(input + " is not an allowed option.")
                                .color(NamedTextColor.YELLOW)
                );
            }
        } catch (IllegalArgumentException e) {
            throw CustomArgument.CustomArgumentException.fromAdventureComponent(
                    Component.text(input + " is not a valid constant for enum " + enumClass.getSimpleName() + ".")
                            .color(NamedTextColor.YELLOW)
            );
        }

    }

    /**
     * Creates a custom argument for parsing an enum, and adds tab-completion suggestions to it.
     * @param arg The argument name.
     * @param enumClass The class object of the enum.
     * @param validValues A supplier that provides the valid enum values.
     * @return The custom argument.
     * @param <T> The Enum's type.
     */
    public static <T extends Enum<T>> Argument<T> argument (String arg, Class<T> enumClass, Supplier<Collection<T>> validValues) {
        return new CustomArgument<>(new StringArgument(arg), new EnumArgumentParser<>(enumClass, validValues))
                .replaceSuggestions(ArgumentSuggestions.strings(
                        i -> validValues.get().stream()
                                .map(t -> t.name().toLowerCase())
                                .toArray(String[]::new)
                        )
                );
    }

    /**
     * Creates a custom argument for parsing an enum, and adds tab-completion suggestions to it.
     * The list of valid enums for this method includes all enum constants. If you wish to limit it to a certain range,
     * use the other argument generator.
     * @param arg The argument name.
     * @param enumClass The class object of the enum.
     * @return The custom argument.
     * @param <T> The Enum's type.
     */
    public static <T extends Enum<T>> Argument<T> argument (String arg, Class<T> enumClass) {
        return new CustomArgument<>(new StringArgument(arg), new EnumArgumentParser<>(enumClass))
                .replaceSuggestions(ArgumentSuggestions.strings(
                                i -> Arrays.stream(enumClass.getEnumConstants())
                                        .map(t -> t.name().toLowerCase())
                                        .toArray(String[]::new)
                        )
                );
    }

}
