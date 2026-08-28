package neonique.cbcplugin_new.commands.arguments;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.PlayerLike;
import neonique.cbcplugin_new.mapconfig.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PlayerArgumentParser<T extends PlayerLike> implements CustomArgument.CustomArgumentInfoParser<T, String> {

    private final Function<CommandArguments, Map<String, T>> playerSupplier;

    public PlayerArgumentParser (Supplier<Collection<T>> playerSupplier) {
        this.playerSupplier = args -> playerSupplier.get().stream()
                .collect(Collectors.toMap(
                        PlayerLike::name,
                        p -> p
                ));
    }

    public PlayerArgumentParser (Function<CommandArguments, Collection<T>> playerSupplier) {
        this.playerSupplier = args -> playerSupplier.apply(args).stream()
                .collect(Collectors.toMap(
                        PlayerLike::name,
                        p -> p
                ));
    }

    @Override
    public T apply(CustomArgument.CustomArgumentInfo<String> info) throws CustomArgument.CustomArgumentException {

        Map<String, T> players = playerSupplier.apply(info.previousArgs());
        String input = info.input();
        T player = players.get(input);

        if (player != null) {
            return player;
        } else {
            throw CustomArgument.CustomArgumentException.fromAdventureComponent(
                    Component.text(input + " is not a valid player name.")
                            .color(NamedTextColor.YELLOW)
            );
        }

    }

    public static <T extends PlayerLike> Argument<T> argument (String arg, Supplier<Collection<T>> playerSupplier) {
        return new CustomArgument<>(new StringArgument(arg), new PlayerArgumentParser<>(playerSupplier))
                .replaceSuggestions(ArgumentSuggestions.strings(args ->
                        playerSupplier.get().stream()
                        .map(PlayerLike::name)
                        .toArray(String[]::new)));
    }

    public static <T extends PlayerLike> Argument<T> argument (String arg, Function<CommandArguments, Collection<T>> playerSupplier) {
        return new CustomArgument<>(new StringArgument(arg), new PlayerArgumentParser<>(playerSupplier))
                .replaceSuggestions(ArgumentSuggestions.strings(args ->
                        playerSupplier.apply(args.previousArgs()).stream()
                        .map(PlayerLike::name)
                        .toArray(String[]::new)));
    }

}
