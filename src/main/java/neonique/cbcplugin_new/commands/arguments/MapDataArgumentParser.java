package neonique.cbcplugin_new.commands.arguments;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.mapconfig.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.function.Supplier;

public class MapDataArgumentParser<M extends MapData> implements CustomArgument.CustomArgumentInfoParser<M, String> {

    private final Supplier<Map<String, M>> mapSupplier;

    public MapDataArgumentParser (Supplier<Map<String, M>> mapSupplier) {
        this.mapSupplier = mapSupplier;
    }

    @Override
    public M apply(CustomArgument.CustomArgumentInfo<String> info) throws CustomArgument.CustomArgumentException {

        Map<String, M> maps = mapSupplier.get();
        String input = info.input();

        M map = maps.get(input);

        if (map != null) {
            return map;
        } else {
            throw CustomArgument.CustomArgumentException.fromAdventureComponent(
                    Component.text(input + " is not a valid map ID.")
                            .color(NamedTextColor.YELLOW)
            );
        }

    }

    public static <T extends MapData> Argument<T> argument (String arg, Supplier<Map<String, T>> mapSupplier) {
        return new CustomArgument<>(new StringArgument(arg), new MapDataArgumentParser<>(mapSupplier))
                .replaceSuggestions(ArgumentSuggestions.strings(
                        i -> mapSupplier.get().keySet().toArray(new String[0])));
    }

}
