package neonique.cbcplugin_new.commands.parsers;

import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.mapconfig.MapData;
import neonique.cbcplugin_new.practice.PracticeManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CBCMapDataParser<C, M extends MapData> implements ArgumentParser<C, M> {

    private final Supplier<Map<String, M>> maps;

    public CBCMapDataParser (Supplier<Map<String, M>> maps) {
        this.maps = maps;
    }

    @Override
    public @NotNull ArgumentParseResult<M> parse(@NotNull CommandContext<C> context, @NotNull CommandInput input) {

        String mapId = input.peekString();
        Map<String, M> maps = this.maps.get();

        if (maps.containsKey(mapId)) {
            return ArgumentParseResult.success(maps.get(mapId));
        } else {
            return ArgumentParseResult.failure(new IllegalArgumentException("Not a valid map"));
        }

        /*
        final String input = commandInput.peekString(); // Does not remove the string from the input!
        try {
            final UUID uuid = UUID.fromString(input);
            commandInput.readString(); // Removes the string from the input.
            return ArgumentParseResult.success(uuid);
        } catch (final IllegalArgumentException e) {
            return ArgumentParseResult.failure(new UUIDParseException(input, context));
        }*/
    }

    public static <C, M extends MapData> ParserDescriptor<C, M> mapDataParser (Supplier<Map<String, M>> maps, Class<M> type) {
        return ParserDescriptor.of(new CBCMapDataParser<>(maps), type);
    }

}
