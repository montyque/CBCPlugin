package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.combat.DeathCause;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BaseTeamGameCommands extends BaseGameCommands {

    private final TeamGame<? extends CBCPlayer, ?, ?> game;

    public BaseTeamGameCommands(TeamGame<?, ?, ?> game) {
        super(game);
        this.game = game;
    }

    public List<String> getTeamIds() {
        return new ArrayList<>(game.getGeneralTeamList().keySet());
    }

    public CBCTeam<?> findTeam (Player user, String teamId) {
        CBCTeam<?> team = game.getGeneralTeamList().get(teamId);
        if (team == null) {
            sendColorMessage(user, teamId + " is not a valid team!", NamedTextColor.YELLOW);
            return null;
        }
        return team;
    }

    public void removePlayerFromTeam (CBCPlayer player, boolean removePlayerFromGame) {

        CBCTeam<?> currentTeam = player.team();
        if (currentTeam == null) return;

        game.removePlayerFromTeam(player, currentTeam);

        if (player.isOnline()) {
            player.getPlayer().sendMessage(
                    Component.text("You have been removed from ").color(NamedTextColor.YELLOW).append(
                            Component.text(currentTeam.name()).color(currentTeam.textColor())
                    ).append(
                            Component.text(" team.").color(NamedTextColor.YELLOW)
                    )
            );
        }

        if (removePlayerFromGame) {
            game.removePlayerByBase(player);
        }

    }

    public void putPlayerOnTeam (CBCPlayer player, CBCTeam<?> team, boolean spawnImmediately) {

        // Kill player if player is alive
        if (player.isAlive()) {
            game.getCombatManager().playerDeath(player, DeathCause.COMMAND);
        }

        removePlayerFromTeam(player, false);
        game.addPlayerToTeam(player, team);

        if (player.isOnline()) {
            player.getPlayer().sendMessage(
                    Component.text("You have been added to ").color(NamedTextColor.GREEN).append(
                            Component.text(team.name()).color(team.textColor())
                    ).append(
                            Component.text(" team!").color(NamedTextColor.GREEN)
                    ).decorate(TextDecoration.BOLD)
            );
        }

        if (spawnImmediately) {
            game.getCombatManager().playerRespawn(player);
        }

    }

    @Override
    public void run(Player user, String[] args, int perms) {
        super.run(user, args, perms);
        if ("join".equals(args[0])) {
            join(user, args, perms);
        }
    }

    @Override
    public List<String> tabComplete(Player user, String[] args, int perms) {
        // Get default tab completions
        List<String> tabCompletions = super.tabComplete(user, args, perms);

        // Add other tab completions
        int level = args.length;
        if (level == 1) {
            if (perms >= 1) {
                tabCompletions.add("join");
            }
        }
        else if (level >= 2) {
            if ("join".equals(args[0])) {
                tabCompletions = joinTabComplete(user, args, perms);
            }
        }

        return tabCompletions;
    }

    public void join(Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (args.length < 2) {
            sendColorMessage(user, "You must include a player name!", NamedTextColor.YELLOW);
            return;
        }

        if (args.length < 3) {
            sendColorMessage(user, "You must include a team!", NamedTextColor.YELLOW);
            return;
        }

        // Check if player is alive right now
        Player playerEntity = findPlayerWithName(args[1]);

        if (playerEntity == null) {
            sendColorMessage(user, "Could not find player " + args[1] + "!", NamedTextColor.YELLOW);
            return;
        }

        String playerName = playerEntity.getName();

        // Find team
        CBCTeam<?> team = findTeam(user, args[2]);
        if (team == null) return;

        if (game.getPlayer(playerEntity) == null) {
            CBCPlayer p = game.createAndAddPlayer(playerEntity);
            putPlayerOnTeam(p, team, true);
        } else {
            CBCPlayer p = game.getPlayer(playerEntity);
            if (p.team() != null) {
                if (p.team() == team) {
                    sendColorMessage(user, playerName + " is already on " + team.name() + "!", NamedTextColor.YELLOW);
                    return;
                }
            }
            putPlayerOnTeam(p, team, false);
        }
        broadcastAction(user, "used /game join to put " + playerName + " on " + team.name());
    }

    public List<String> joinTabComplete(Player user, String[] args, int perms) {
        int level = args.length;
        List<String> tabCompletions = new ArrayList<>();
        if (perms < 1) return tabCompletions;
        if (level == 2) {
            tabCompletions = new ArrayList<>(getPlayerNamesOnline());
        }
        else if (level == 3) {
            tabCompletions = getTeamIds();
        }
        return tabCompletions;
    }

}
