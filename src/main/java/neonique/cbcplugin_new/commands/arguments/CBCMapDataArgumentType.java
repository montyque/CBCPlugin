package neonique.cbcplugin_new.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import neonique.cbcplugin_new.mapconfig.MapData;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class CBCMapDataArgumentType implements CustomArgumentType.Converted<MapData, String> {

    private static final DynamicCommandExceptionType INVALID_MAP_ID = new DynamicCommandExceptionType(map -> {
        return MessageComponentSerializer.message().serialize(Component.text(map + " is not a valid map ID!"));
    });

    private final Supplier<Map<String, ? extends MapData>> maps;

    public CBCMapDataArgumentType (Supplier<Map<String, ? extends MapData>> maps) {
        this.maps = maps;
    }

    @Override
    public @NotNull MapData convert (@NotNull String mapId) throws CommandSyntaxException {
        var currentMaps = maps.get();
        if (currentMaps.containsKey(mapId)) {
            return currentMaps.get(mapId);
        } else {
            throw INVALID_MAP_ID.create(mapId);
        }
    }

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        for (String mapId : maps.get().keySet()) {
            if (mapId.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(mapId);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
