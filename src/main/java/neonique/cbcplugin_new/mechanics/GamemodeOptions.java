package neonique.cbcplugin_new.mechanics;

import neonique.cbcplugin_new.core.CBCGamemode;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class GamemodeOptions {

    private final CBCGamemode gamemode;
    private final String gamemodeId;
    private final String gamemodeName;

    private final TextColor color;

    private final boolean teamGamemode;
    private int minTeams;
    private int maxTeams;

    private int minPlayers = 1;

    // Game variables
    HashMap<String, Integer> defaultGameInts;
    HashMap<String, Boolean> defaultGameBools;
    HashMap<String, String> defaultGameStrings;

    public GamemodeOptions(CBCGamemode gamemode, String gamemodeId, String gamemodeName,
                           boolean teamGamemode, int minTeams, int maxTeams) {

        this.gamemode = gamemode;

        this.gamemodeId = gamemodeId;
        this.gamemodeName = gamemodeName;
        this.teamGamemode = teamGamemode;

        this.color = gamemode.getColor();

        if (teamGamemode) {
            this.minTeams = minTeams;
            this.maxTeams = maxTeams;
        }
        setGameVars();
    }

    public GamemodeOptions(CBCGamemode gamemode, String gamemodeId, String gamemodeName, boolean teamGamemode, int minPlayers) {

        this.gamemode = gamemode;

        this.gamemodeId = gamemodeId;
        this.gamemodeName = gamemodeName;
        this.teamGamemode = teamGamemode;

        this.minPlayers = minPlayers;

        this.color = gamemode.getColor();

        if (teamGamemode) {
            this.minTeams = 2;
            this.maxTeams = 4;
        }
        setGameVars();
    }

    public void setGameVars() {
        defaultGameInts = new HashMap<>();
        defaultGameBools = new HashMap<>();
        defaultGameStrings = new HashMap<>();

        switch (gamemodeId) {
            case "showdown":
                defaultGameBools.put("playersGlow", true);
                defaultGameBools.put("suddenDeathEnabled", true);
                defaultGameInts.put("pointsToWin", 4);
                defaultGameInts.put("suddenDeathTimer", 60);
                break;
            case "ctf":
                defaultGameInts.put("flagsStart", 4);
                defaultGameInts.put("initialFlagsRemovedTimer", 1200);
                defaultGameInts.put("flagsRemovedTimerAfterFirst", 600);
                break;
            case "holdthegold":
                defaultGameInts.put("pointsStart", 40);
                defaultGameInts.put("ticksToScore", 40);
                defaultGameInts.put("teamsToWin", 1);
                defaultGameInts.put("finalRunLength", 7);
                break;
            case "tdm":
                defaultGameBools.put("gameByTimer", true);
                defaultGameBools.put("playersGlow", true);
                defaultGameInts.put("gameTimer", 600);
                defaultGameInts.put("killsToWin", 50);
                defaultGameInts.put("overtimeThreshold", 2);
                defaultGameInts.put("_maprush_gameMapTimer", 60);
                defaultGameInts.put("_maprush_mapsAmount", 10);
                break;
            case "rendezvous":
                defaultGameStrings.put("_redRunnerOrder", "0");
                defaultGameStrings.put("_blueRunnerOrder", "0");
                defaultGameStrings.put("_greenRunnerOrder", "0");
                defaultGameStrings.put("_yellowRunnerOrder", "0");
                defaultGameInts.put("checkpointsToWin", 20);
                defaultGameBools.put("canHalfStick", true);
                defaultGameBools.put("finalCheckpointEnabled", true);
                defaultGameStrings.put("swapSystemType", "timer");
                defaultGameInts.put("swapTimer", 120);
                defaultGameInts.put("captureTime", 30);
                defaultGameInts.put("teamsToWin", 1);
                break;
            case "flagrush":
                defaultGameInts.put("scoreStart", 0);
                defaultGameInts.put("gameTimer", 1200);
                defaultGameInts.put("flagCapturePoints", 100);
                defaultGameInts.put("flagLostPoints", 50);
                break;
            case "throwdown":
                defaultGameInts.put("pointsToWin", 3);
                defaultGameBools.put("suddenDeathEnabled", true);
                defaultGameInts.put("suddenDeathTimer", 60);
                break;
            case "kmation":
                defaultGameInts.put("maxPlayersInFinalCycle", 4);
                defaultGameInts.put("cycleLength", 60);
                break;
            case "assassin":
                defaultGameInts.put("targetsToKill", 16);
                defaultGameInts.put("targetChangeTimer", 60);
                break;
            case "cbctag":
                defaultGameInts.put("roundLength", 150);
                defaultGameInts.put("roundsPerTeam", 1);
                defaultGameInts.put("taggerRespawnTimer", 4);
                defaultGameInts.put("MAX_POINTS_FOR_SURVIVAL", 150);
                defaultGameInts.put("MAX_POINTS_FOR_WIPEOUT", 150);
                defaultGameInts.put("MAX_EVADER_KILL_POINTS", 50);
                defaultGameInts.put("MIN_EVADER_KILL_POINTS", 15);
                defaultGameInts.put("SURVIVAL_BONUS", 10);
                defaultGameInts.put("WIPEOUT_BONUS", 20);
                break;
            case "koth":
                defaultGameInts.put("pointsStart", 101);
                defaultGameInts.put("ticksToScore", 20);
                defaultGameInts.put("captureMajorityPercentage", 40);
                defaultGameInts.put("teamsToWin", 1);
                break;
        }
    }


    public String getGamemodeId() {
        return gamemodeId;
    }

    public String getGamemodeName() {
        return gamemodeName;
    }

    public TextColor getColor() {
        return color;
    }

    public ItemStack getGamemodeIcon () {
        return gamemode.getGamemodeIconItem();
    }

    public boolean isTeamGamemode() {
        return teamGamemode;
    }

    public int getMinTeams() {
        return minTeams;
    }

    public int getMaxTeams() {
        return maxTeams;
    }

    public HashMap<String, Boolean> getDefaultGameBooleans () {
        return defaultGameBools;
    }

    public HashMap<String, Integer> getDefaultGameInts () {
        return defaultGameInts;
    }

    public HashMap<String, String> getDefaultGameStrings () {
        return defaultGameStrings;
    }

    public int getMinPlayers() {
        return minPlayers;
    }
}
