package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BaseTeamGameCommands extends BaseGameCommands {

    private final TeamGame<?, ?, ?> game;

    public BaseTeamGameCommands(GameManager gm, CombatManager wm, TeamGame<?, ?, ?> game) {
        super(gm, wm);
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

        CBCTeam<?> currentTeam = player.getTeam();
        if (currentTeam == null) return;

        game.removePlayerFromTeam(player, currentTeam);

        if (player.isOnline()) {
            player.getPlayer().sendMessage(
                    Component.text("You have been removed from ").color(NamedTextColor.YELLOW).append(
                            Component.text(currentTeam.getTeamName()).color(currentTeam.getColor())
                    ).append(
                            Component.text(" team.").color(NamedTextColor.YELLOW)
                    )
            );
        }

        if (removePlayerFromGame) {
            game.removePlayer(player);
        }

    }

    public void putPlayerOnTeam (CBCPlayer player, CBCTeam<?> team, boolean spawnImmediately) {

        // Kill player if player is alive
        if (player.isAlive()) {
            if (player.getLastPlayerHitBy() != null) {
                combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.COMMAND, false);
            } else {
                combatManager.playerDeath(player, null, DeathCause.COMMAND, false);
            }
        }

        removePlayerFromTeam(player, false);
        game.addPlayerToTeam(player, team);

        if (player.isOnline()) {
            player.getPlayer().sendMessage(
                    Component.text("You have been added to ").color(NamedTextColor.GREEN).append(
                            Component.text(team.getTeamName()).color(team.getColor())
                    ).append(
                            Component.text(" team!").color(NamedTextColor.GREEN)
                    ).decorate(TextDecoration.BOLD)
            );
        }

        if (spawnImmediately) {
            combatManager.playerRespawn(player);
        }
    }

    @Override
    public void run(Player user, String[] args, int perms) {
        // Go through each base game command
        super.run(user, args, perms);
        // Add gamemode specific commands
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

        CBCPlayer playerObj;
        if (gameManager.getPlayer(playerEntity) == null) {
            playerObj = game.addPlayer(playerEntity);
            putPlayerOnTeam(playerObj, team, true);
        } else {
            playerObj = gameManager.getPlayer(playerEntity);
            if (playerObj.getTeam() != null) {
                if (Objects.equals(playerObj.getTeam().getTeamName(), team.getTeamName())) {
                    sendColorMessage(user, playerName + " is already on " + team.getTeamName() + "!", NamedTextColor.YELLOW);
                    return;
                }
            }
            putPlayerOnTeam(gameManager.getPlayer(playerEntity), team, false);
        }
        broadcastAction(user, "used /game join to put " + playerName + " on " + team.getTeamName());
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
