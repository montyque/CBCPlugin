package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.PlayerSession;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.scoreboard.SidebarProvider;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class ShowdownSidebarProvider implements SidebarProvider {

    private PlayerSession<ShowdownPlayer> players;
    private Runnable globalUpdateListener;
    private Consumer<Player> clientUpdateListener;

    private int nameColumnWidth = 62;
    private int playerCountColumnWidth = 20;
    private int roundWinColumnWidth = 24;

    private int roundsToWin;

    private List<TeamRow> teamRows;

    public ShowdownSidebarProvider (PlayerSession<ShowdownPlayer> players,
                                    Runnable globalUpdateListener,
                                    Consumer<Player> clientUpdateListener) {
        this.globalUpdateListener = globalUpdateListener;
        this.clientUpdateListener = clientUpdateListener;
    }

    @Override
    public Component getSidebarHeader() {
        return Component.text()
                .content("CBC: ")
                .color(NamedTextColor.AQUA)
                .font(ResourcePackFont.SMALL_5X5.getFontKey())
                .content("SHOWDOWN")
                .color(NamedTextColor.YELLOW)
                .font(ResourcePackFont.SMALL_5X5.getFontKey())
                .build();
    }

    @Override
    public List<Component> getClientDisplay(Player client) {

        ShowdownPlayer player = players.getPlayer(client);
        TeamColor teamColor = player.teamOptional().map(TeamLike::teamColor).orElse(null);

        List<Component> components = new ArrayList<>();
        components.add(Component.space());
        components.addAll(getTeamComponents(teamColor));

        if (player != null) {

        }

        components.add(Component.space());

        return Collections.unmodifiableList(components);

    }

    public void setRoundsToWin (int roundsToWin) {
        this.roundsToWin = roundsToWin;
        this.roundWinColumnWidth = 6 * roundsToWin;
    }

    public void updateTeamRows (Collection<ShowdownTeam> teams) {
        teamRows.clear();
        for (ShowdownTeam team : teams) {
            teamRows.add(new TeamRow(
                    team.teamColor(),
                    team.name(),
                    team.prefix(),
                    team.getRoundsWon(),
                    roundsToWin,
                    team.getPlayersLeftAlive()
            ));
        }
        Collections.sort(teamRows);
        globalUpdateListener.run();
    }

    public List<Component> getTeamComponents (TeamColor ownTeam) {
        return teamRows.stream()
                .map(t -> t.getRowComponent(t.color() == ownTeam, nameColumnWidth,
                        playerCountColumnWidth, roundWinColumnWidth))
                .toList();
    }

}

record TeamRow (TeamColor color,
                String name,
                String prefix,
                int roundsWon,
                int roundsToWin,
                int playersAlive) implements Comparable<TeamRow> {

    public Component getRowComponent (boolean highlight,
                                      int nameColumnWidth,
                                      int playerCountColumnWidth,
                                      int roundWinColumnWidth) {

        TextComponent.Builder builder = Component.text();

        if (highlight) builder.append(Component.text("\uE880").color(NamedTextColor.YELLOW));
        else builder.append(getComponentSpaceOfLength(5));

        builder.append(getComponentSpaceOfLength(4));

        builder.content(prefix)
                .color(color.color())
                .decorate(TextDecoration.BOLD);

        addTeamName(builder, nameColumnWidth);
        addPlayerCount(builder, playerCountColumnWidth);
        builder.content(" ");
        addRoundsWon(builder, roundWinColumnWidth);

        return builder.build();

    }

    private void addTeamName (TextComponent.Builder builder, int width) {

        // Left aligned
        builder.content(name)
                .color(color.color());

        int contentWidth = TextUtil.getPixelLengthOfText(name);
        builder.append(getComponentSpaceOfLength(width - contentWidth));

    }

    private void addPlayerCount (TextComponent.Builder builder, int width) {

        // Right aligned
        int contentWidth = String.valueOf(playersAlive).length() * 6 + 8;
        builder.append(getComponentSpaceOfLength(width - contentWidth));
        builder.content(playersAlive + "\uE881");

        if (playersAlive == 0) builder.color(NamedTextColor.GRAY);
        else builder.color(NamedTextColor.GREEN);

    }

    private void addRoundsWon (TextComponent.Builder builder, int width) {

        // Right aligned
        int contentWidth = roundsToWin * 6;
        builder.append(getComponentSpaceOfLength(width - contentWidth));

        for (int i = 1; i <= roundsToWin; i++) {
            if (i <= roundsWon) {
                builder.content("■")
                        .color(NamedTextColor.GREEN);
            } else {
                builder.content("□")
                        .color(NamedTextColor.WHITE);
            }
        }

    }

    @Override
    public int compareTo(@NotNull TeamRow o) {
        return color.num() - o.color().num();
    }

}
