package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ShowdownTeam extends CBCTeam {

    // Set variables relating to showdown game
    private final ShowdownGame showdownGame;
    private int roundsWon;
    private int playersLeftAlive;
    private boolean teamAlive;
    private ShowdownSpawn currentRoundSpawn;

    public ShowdownTeam(ShowdownGame showdownGame, String teamId, String teamName, String teamNumId, NamedTextColor teamColor,
                        String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamName, teamNumId, teamColor, prefix, item, glassHead);
        this.showdownGame = showdownGame;
        roundsWon = 0;
        teamAlive = false;
    }

    public int getRoundsWon () {return roundsWon;}

    public void teleportPlayers (ShowdownSpawn spawn) {
        for (CBCPlayer player : getOnlinePlayers()) {
            ((ShowdownPlayer) player).teleportPlayerToSpawn(spawn);
            ((ShowdownPlayer) player).playerSetupRound();
        }
    }

    public void setupRound () {
        playersLeftAlive = updatePlayersLeftAlive(false);
        teamAlive = playersLeftAlive != 0;
    }

    public int updatePlayersLeftAlive (boolean checkAlive) {
        if (checkAlive) {
            playersLeftAlive = getAlivePlayers().size();
        } else {
            playersLeftAlive = getOnlinePlayers().size();
        }
        return playersLeftAlive;
    }

    public void eliminateTeam () {

        teamAlive = false;
        // Send message
        showdownGame.getGameManager().sendGlobalMessage(
                Component.text("TEAM ELIMINATED > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(getTeamName()).decorate(TextDecoration.BOLD).color(getColor()))
                        .append(Component.text(" has been eliminated!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).color(NamedTextColor.WHITE))
        );
    }

    public void reviveTeam () {

        teamAlive = true;
        // Send message
        showdownGame.getGameManager().sendGlobalMessage(
                Component.text("TEAM REVIVED > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(getTeamName()).decorate(TextDecoration.BOLD).color(getColor()))
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
