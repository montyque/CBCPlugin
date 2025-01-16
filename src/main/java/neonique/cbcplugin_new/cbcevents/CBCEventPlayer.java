package neonique.cbcplugin_new.cbcevents;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CBCEventPlayer {

    private final UUID playerUUID;
    private CBCEventTeam team = null;
    private List<Integer> gameScores;
    private int currentEventScore;

    public CBCEventPlayer (Player playerEntity) {
        this.playerUUID = playerEntity.getUniqueId();
        this.gameScores = new ArrayList<>();

        // Add game scores for four games
        for (int games = 1; games <= 4; games++) {
            gameScores.add(0);
        }
        addGameScores();
    }

    public void setGameScoreForGame (int game, int score) {
        gameScores.set(game - 1, score);
        addGameScores();
    }

    public void addGameScores () {
        int score = 0;
        for (int gameScore : gameScores) {
            score += gameScore;
        }
        currentEventScore = score;
    }

    public int getEventScore () {
        return currentEventScore;
    }

    public int getEventScoreForGame (int gameNum) {
        if (gameNum < 1 || gameNum > gameScores.size()) {
            return 0;
        }
        else {
            return gameScores.get(gameNum - 1);
        }
    }

    public void setTeam (CBCEventTeam team) {
        this.team = team;
    }

    public CBCEventTeam getTeam () {
        return team;
    }

    public NamedTextColor getTeamColor () {
        if (team == null) return null;
        return team.getTeamColor();
    }

    public Component getNameComponent() {
        NamedTextColor textColor = NamedTextColor.WHITE;
        if (getTeamColor() != null) {
            textColor = getTeamColor();
        }
        return Component.text(getName()).color(textColor);
    }

    public boolean isOnline () {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return player.isOnline();
    }

    public Player getPlayer () {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (player.getPlayer() != null) {
            return player.getPlayer();
        } else {
            return null;
        }
    }

    public OfflinePlayer getOfflinePlayer () {
        return Bukkit.getOfflinePlayer(playerUUID);
    }

    public UUID getPlayerUUID () {
        return playerUUID;
    }

    public String getName() {
        return getOfflinePlayer().getName();
    }
}
