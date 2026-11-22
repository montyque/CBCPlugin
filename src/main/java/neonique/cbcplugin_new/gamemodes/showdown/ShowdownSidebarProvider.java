package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.hud.*;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.objective.ScoreFormat;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static neonique.cbcplugin_new.util.TextUtil.*;

public class ShowdownSidebarProvider implements SidebarProvider {

    private final ShowdownGame game;
    private List<TeamRow> teamRows = List.of();

    public ShowdownSidebarProvider (ShowdownGame game) {
        this.game = game;
    }

    public void updateTeamRows () {
        teamRows = game.getTeams().stream()
                .map(t -> new TeamRow(t.teamColor(), t.name(), t.prefix(), t.getRoundsWon(),
                        game.settings().roundsToWin(), t.getPlayersLeftAlive()))
                .sorted()
                .toList();
    }

    @Override
    public boolean visible (Player player) {
        return true;
    }

    @Override
    public SidebarComponent title () {
        return SidebarComponent.staticLine(Component.text()
                .append(Component.text("CBC: ").color(NamedTextColor.AQUA).font(ResourcePackFont.SMALL_5X5.getFontKey()))
                .append(Component.text("SHOWDOWN").color(NamedTextColor.YELLOW).font(ResourcePackFont.SMALL_5X5.getFontKey()))
                .build());
    }

    @Override
    public SidebarComponent lines (Player client) {

        List<SidebarRow> rows = getAllRows(client);

        // Get maximum width of all rows
        int maxWidth = rows.stream()
                .mapToInt(SidebarRow::minWidth)
                .max()
                .orElse(0);

        // Create list of components from rows
        List<Component> rowComponents = rows.stream()
                .map(r -> r.component(maxWidth))
                .toList();

        SidebarComponent.Builder builder = SidebarComponent.builder();
        rowComponents.forEach(c -> builder.addStaticLine(c, ScoreFormat.blank()));
        return builder.build();

    }

    public List<AlignedRow> getTeamRows (TeamColor color) {
        return teamRows.stream()
                .map(t -> t.getRowComponent(t.color() == color))
                .toList();
    }

    /// Returns all rows to be displayed for a given client.
    public List<SidebarRow> getAllRows (Player client) {

        Optional<ShowdownPlayer> player = game.getPlayerByUUID(client.getUniqueId());
        List<SidebarRow> rows = new ArrayList<>();
        rows.add(SidebarRow.blank());

        // Add team standings
        rows.addAll(getTeamRows(player.map(p -> game.getPlayerTeam(p).teamColor()).orElse(null)));
        rows.add(SidebarRow.blank());

        // Add individual statistics
        player.ifPresent(p -> rows.addAll(getPlayerStats(p)));
        return List.copyOf(rows);

    }

    /// Returns the player's statistics to be displayed on the sidebar.
    public List<SidebarRow> getPlayerStats (ShowdownPlayer player) {

        List<SidebarRow> rows = new ArrayList<>();

        rows.add(SingleSidebarComponent.of(Component.text()
                .append(getComponentSpaceOfLength(10))
                .append(Component.text("Kills: ").color(NamedTextColor.GREEN))
                .append(Component.text(player.getKills()).color(NamedTextColor.YELLOW))
                .build()));
        rows.add(SingleSidebarComponent.of(Component.text()
                .append(getComponentSpaceOfLength(10))
                .append(Component.text("Round Kills: ").color(NamedTextColor.GREEN))
                .append(Component.text(player.getPlayerRoundKills()).color(NamedTextColor.YELLOW))
                .build()));
        rows.add(SingleSidebarComponent.of(Component.text()
                .append(getComponentSpaceOfLength(10))
                .append(Component.text("Time Alive: ").color(NamedTextColor.GREEN))
                .append(Component.text(TextUtil.timerToText(player.getPlayerSecondsAlive())).color(NamedTextColor.YELLOW))
                .build()));
        rows.add(SidebarRow.blank());

        return List.copyOf(rows);

    }

}

record TeamRow (TeamColor color,
                String name,
                String prefix,
                int roundsWon,
                int roundsToWin,
                int playersAlive) implements Comparable<TeamRow> {

    public static final int NAME_COL_WIDTH = 65;
    public static final int PLAYER_COUNT_COL_WIDTH = 20;

    public AlignedRow getRowComponent (boolean highlight) {
        AlignedRow.Builder builder = AlignedRow.builder();
        return builder
                .left(SidebarRowComponent.space(3))
                .left(pointer(highlight))
                .left(SidebarRowComponent.space(3))
                .left(prefixComponent())
                .left(SidebarRowComponent.space(4))
                .left(nameComponent())
                .right(SidebarRowComponent.space(4))
                .right(playerCountComponent())
                .right(SidebarRowComponent.space(4))
                .right(roundsWonComponent())
                .right(SidebarRowComponent.space(3))
                .build();
    }

    private SidebarRowComponent pointer (boolean highlight) {
        Component pointer = highlight ? Component.text("\uE880").color(NamedTextColor.YELLOW) : getComponentSpaceOfLength(4);
        return new SidebarRowComponent(
                new SingleSidebarComponent(pointer, 4), 4, ComponentAlignment.LEFT
        );
    }

    private SidebarRowComponent prefixComponent () {
        int length = getPixelLengthOfText(prefix) + 2;
        return new SidebarRowComponent(
                new SingleSidebarComponent(Component.text(prefix)
                        .color(color.color())
                        .decorate(TextDecoration.BOLD),
                        length), length, ComponentAlignment.LEFT
        );
    }

    private SidebarRowComponent nameComponent () {
        return new SidebarRowComponent(
                SingleSidebarComponent.of(Component.text(name + " Team").color(color.color())), NAME_COL_WIDTH, ComponentAlignment.LEFT
        );
    }

    private SidebarRowComponent playerCountComponent () {
        TextComponent.Builder builder = Component.text();
        int contentWidth = String.valueOf(playersAlive).length() * 6 + 9;
        builder.append(Component.text(playersAlive + "\uE881"));
        if (playersAlive == 0) builder.color(NamedTextColor.GRAY);
        else builder.color(NamedTextColor.GREEN);
        return new SidebarRowComponent(
                new SingleSidebarComponent(builder.build(), contentWidth), PLAYER_COUNT_COL_WIDTH, ComponentAlignment.RIGHT
        );
    }

    private SidebarRowComponent roundsWonComponent () {
        int width = roundsToWin * 6;
        TextComponent.Builder builder = Component.text();
        for (int i = 1; i <= roundsToWin; i++) {
            if (i <= roundsWon) {
                builder.append(Component.text("■").color(NamedTextColor.GREEN));
            } else {
                builder.append(Component.text("□").color(NamedTextColor.WHITE));
            }
        }
        return new SidebarRowComponent(new SingleSidebarComponent(builder.build(), width), width, ComponentAlignment.RIGHT);
    }

    @Override
    public int compareTo(@NotNull TeamRow o) {
        return color.num() - o.color().num();
    }

}
