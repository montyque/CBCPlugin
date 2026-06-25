package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.core.CBCPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.ToIntFunction;

/**
 * A generator of a list of players sorted by a certain statistic.
 * @param <T> A CBCPlayer subclass depending on the gamemode.
 */
public class PlayerStatList<T extends CBCPlayer> {

    private final ToIntFunction<T> statMethodGetter;
    private final boolean isTimeFormatted;
    private final String statName;

    /**
     *
     * @param statName The display name of the statistic.
     * @param statMethodGetter A method reference that gets the value for this statistic. The method reference must return an integer.
     * @param isTimeFormatted Whether the statistic should be displayed in MM:SS format when displayed.
     */
    public PlayerStatList (String statName, ToIntFunction<T> statMethodGetter, boolean isTimeFormatted) {
        this.statName = statName;
        this.statMethodGetter = statMethodGetter;
        this.isTimeFormatted = isTimeFormatted;
    }

    /**
     * Generates a list of players sorted by the statistic.
     * @param players The collection of players to sort
     * @param ascending Whether they should be sorted in ascending order
     * @return List of players sorted by the statistic associated with this object.
     */
    public ArrayList<T> getSortedList (Collection<T> players, boolean ascending) {
        ArrayList<T> playerList = new ArrayList<>(players);
        if (ascending) {
            playerList.sort(Comparator.comparingInt(statMethodGetter));
        } else {
            playerList.sort(Comparator.comparingInt(statMethodGetter).reversed());
        }
        return playerList;
    }

    /**
     * Gets the value of the statistic associated with this object for a certain player.
     * @param player The player to get the statistic value of
     * @return The statistic value of this player
     */
    public int getPlayerStat (T player) {
        return statMethodGetter.applyAsInt(player);
    }

    /**
     * Gets the integer placement of a player within a list of sorted players.
     * @param sortedPlayers A list of players already sorted by the statistic associated with this object
     * @param player The player to find the placement of within the list of players
     * @return The placement of the player within the list of players sorted by this statistic
     */
    public int getPlacementOfPlayerSorted (ArrayList<T> sortedPlayers, T player) {
        int placement = 1;
        for (T comparablePlayer : sortedPlayers) {
            if (getPlayerStat(comparablePlayer) > getPlayerStat(player)) {
                placement++;
            } else {
                return placement;
            }
        }
        return sortedPlayers.size();
    }

    public boolean isTimeFormatted() {
        return isTimeFormatted;
    }

    public String getName() {
        return statName;
    }
}
