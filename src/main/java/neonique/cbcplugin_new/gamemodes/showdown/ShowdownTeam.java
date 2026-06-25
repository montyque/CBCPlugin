package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;

public class ShowdownTeam extends CBCTeam<ShowdownPlayer> {

    // Set variables relating to showdown game
    private final ShowdownGame game;
    private int roundsWon;
    private int playersLeftAlive;
    private boolean teamAlive;
    private ShowdownSpawn currentRoundSpawn;

    public ShowdownTeam (ShowdownGame game, TeamLike originalTeam, String teamIdNum) {
        super(originalTeam, teamIdNum);
        this.game = game;
        roundsWon = 0;
        teamAlive = false;
    }

    public int getRoundsWon () {
        return roundsWon;
    }

    public void teleportPlayers (ShowdownSpawn spawn) {
        for (ShowdownPlayer player : onlinePlayers()) {
            player.teleportPlayerToSpawn(spawn, game.getMap().getMapCentre());
            player.playerSetupRound();
        }
    }

    public void setupRound () {
        playersLeftAlive = updatePlayersLeftAlive(false);
        teamAlive = playersLeftAlive != 0;
    }

    public int updatePlayersLeftAlive (boolean checkAlive) {
        if (checkAlive) {
            playersLeftAlive = alivePlayers().size();
        } else {
            playersLeftAlive = onlinePlayers().size();
        }
        return playersLeftAlive;
    }

    public void eliminateTeam () {

        teamAlive = false;
        // Send message
        game.getGameManager().sendGlobalMessage(
                Component.text("TEAM ELIMINATED > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(name()).decorate(TextDecoration.BOLD).color(textColor()))
                        .append(Component.text(" has been eliminated!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).color(NamedTextColor.WHITE))
        );
    }

    public void reviveTeam () {

        teamAlive = true;

        // Send message
        game.getGameManager().sendGlobalMessage(
                Component.text("TEAM REVIVED > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(name()).decorate(TextDecoration.BOLD).color(textColor()))
                        .append(Component.text(" has been revived as at least one member is back alive!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).color(NamedTextColor.WHITE))
        );

    }

    public void teamWonRound () {
        roundsWon++;
    }

    public boolean isTeamAlive () {
        return teamAlive;
    }

    public int getPlayersLeftAlive () {
        return playersLeftAlive;
    }

    public void setRoundSpawn(ShowdownSpawn spawn) {
        currentRoundSpawn = spawn;
    }

    public ShowdownSpawn getRoundSpawn() {
        return currentRoundSpawn;
    }
}
