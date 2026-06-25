package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.BaseTeamGameCommands;
import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class ShowdownGameCommands extends BaseTeamGameCommands {

    private final ShowdownGame game;

    public ShowdownGameCommands(ShowdownGame game) {
        super(game);
        this.game = game;
    }

    @Override
    public void putPlayerOnTeam (CBCPlayer player, CBCTeam<?> team, boolean spawnImmediately) {

        ShowdownPlayer typedPlayer = game.getTypedPlayer(player);
        ShowdownTeam typedTeam = game.getTypedTeam(team);

        // Kill player if player is alive
        if (player.isAlive()) {
            game.getCombatManager().playerDeath(player, player.getLastPlayerHitBy(), DeathCause.COMMAND, false);
        }

        removePlayerFromTeam(player, false);
        typedTeam.addPlayer(typedPlayer);

        if (player.isOnline()) {
            player.getPlayer().sendMessage(
                    Component.text("You have been added to ").color(NamedTextColor.GREEN).append(
                            Component.text(team.name()).color(team.textColor())
                    ).append(
                            Component.text(" team!").color(NamedTextColor.GREEN)
                    ).decorate(TextDecoration.BOLD)
            );
        }

    }

    @Override
    public void revive (Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (game.isRoundNotInPlay()) {
            sendColorMessage(user, "Round is not in play right now!", NamedTextColor.YELLOW);
            return;
        }

        if (args.length < 2) {
            sendColorMessage(user, "You must include a player name!", NamedTextColor.YELLOW);
            return;
        }

        // Check if player is alive right now
        String playerName = args[1];
        Player targetedPlayer = findPlayerWithName(playerName);
        if (targetedPlayer == null) {
            sendColorMessage(user, "Could not find player " + playerName + "!", NamedTextColor.YELLOW);
            return;
        }

        ShowdownPlayer player = game.getPlayer(targetedPlayer);
        if (player == null) {
            sendColorMessage(user, playerName + " is not registered in the game!", NamedTextColor.YELLOW);
            return;
        }

        if (!player.isAlive()) {
            sendColorMessage(user, playerName + " is currently alive! You can only use this command on dead players.", NamedTextColor.YELLOW);
            return;
        }

        if (!player.isOnline()) {
            sendColorMessage(user, playerName + " is currently offline!", NamedTextColor.YELLOW);
            return;
        }

        ShowdownTeam team = game.getPlayerTeam(player);
        if (team == null) {
            sendColorMessage(user, playerName + " is not currently on a team!", NamedTextColor.YELLOW);
            return;
        }

        player.playerStartRound();
        player.teleportPlayerToSpawn(team.getRoundSpawn(), game.getMap().getMapCentre());

        if (!team.isTeamAlive()) {
            team.reviveTeam();
        }

        sendColorMessage(user, playerName + " has been revived!", NamedTextColor.GREEN);
        broadcastAction(user, "used /game revive to revive " + playerName);

    }
}
