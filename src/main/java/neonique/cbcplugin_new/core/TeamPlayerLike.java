package neonique.cbcplugin_new.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.annotation.Nullable;
import java.util.Optional;

public interface TeamPlayerLike extends PlayerLike {

    @Nullable
    TeamLike team ();

    default Optional<TeamLike> teamOptional () {
        return Optional.ofNullable(team());
    }

    default TextColor nameColor () {
        return teamOptional()
                .map(TeamLike::textColor)
                .orElse(NamedTextColor.WHITE);
    }

    default Component nameComponentWithTeamPrefix () {
        if (team() == null) return nameComponent();
        Component prefix = team().prefixComponent()
                .decorate(TextDecoration.BOLD);
        return prefix.append(Component.text(" ")).append(nameComponent());
    }

}
