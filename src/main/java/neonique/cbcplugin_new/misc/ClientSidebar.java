package neonique.cbcplugin_new.misc;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;
import static neonique.cbcplugin_new.util.TextUtil.getSpaceOfLength;

public class ClientSidebar {

    private final Scoreboard scoreboard;

    private final UUID playerUUID;

    private final HashMap<Integer, String> old = new HashMap<>();
    HashMap<Integer, String> newComponentStrings = new HashMap<>();
    private final HashMap<Integer, String> intToChar = new HashMap<>();
    private final HashMap<Integer, Team> intToTeam = new HashMap<>();

    private Objective scoreboardObjective;
    private boolean hideNumbers;

    public ClientSidebar (Player player, Scoreboard scoreboard, String sidebarName, Component scoreboardDisplayName,
                          boolean hideNumbers) {

        this.playerUUID = player.getUniqueId();
        this.scoreboard = scoreboard;

        // Create scoreboard objective
        try {
            scoreboardObjective = scoreboard.registerNewObjective(sidebarName, "dummy", scoreboardDisplayName);
        } catch (IllegalArgumentException e) {
            scoreboardObjective = scoreboard.getObjective(sidebarName);
        }

        assert scoreboardObjective != null;

        scoreboardObjective.displayName(scoreboardDisplayName);
        scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        this.hideNumbers = hideNumbers;
        if (hideNumbers) {
            scoreboardObjective.numberFormat(NumberFormat.blank());
        }

        // Create teams for scoreboard
        for (int i = 0; i < 15; i++) {
            Team team;
            try {
                team = scoreboard.registerNewTeam("sidebar" + i);
            } catch (IllegalArgumentException e) {
                team = scoreboard.getTeam("sidebar" + i);
            }

            if (team == null) continue;

            String character = String.valueOf(((char) (0xFF00 + i)));
            team.addEntry(character);
            intToChar.put(i, character);
            intToTeam.put(i, team);
        }
    }

    public void removeSidebar () {

        scoreboardObjective.unregister();

        for (Team team : intToTeam.values()) {
            team.unregister();
        }

        scoreboardObjective = null;
    }

    public OfflinePlayer getOfflinePlayer () {
        return Bukkit.getOfflinePlayer(playerUUID);
    }

    public Player getPlayer() {
        OfflinePlayer player = getOfflinePlayer();
        if (player.getPlayer() != null) {
            return player.getPlayer();
        } else {
            return null;
        }
    }

    public void setSidebarComponents (ArrayList<Component> sidebarStringList) {

        if (scoreboardObjective == null) return;

        Collections.reverse(sidebarStringList);

        newComponentStrings.clear();

        for (int i = 0; i < 15; i++) {

            Team team = intToTeam.get(i);
            String character = intToChar.get(i);
            Component componentRow;
            try {
                componentRow = sidebarStringList.get(i);
            } catch (IndexOutOfBoundsException e) {
                componentRow = null;
            }

            String oldString = old.getOrDefault(i, null);
            String gsonSerialisedComponent;


            if (componentRow == null) {
                if (oldString != null) {
                    team.suffix(Component.text(""));
                    scoreboard.resetScores(character);
                    newComponentStrings.put(i, null);
                }
            }
            else {
                // If numbers are hidden, add space to make up for it
                if (hideNumbers) {
                    componentRow = componentRow.append(getComponentSpaceOfLength(8));
                }

                gsonSerialisedComponent = GsonComponentSerializer.gson().serialize(componentRow);
                newComponentStrings.put(i, gsonSerialisedComponent);
                if (oldString == null) {
                    Score score = scoreboardObjective.getScore(character);
                    score.setScore(i);
                    team.suffix(componentRow);
                }
                else {
                    if (!gsonSerialisedComponent.equals(oldString)) {
                        Score score = scoreboardObjective.getScore(character);
                        score.setScore(i);
                        team.suffix(componentRow);
                    }
                }
            }
        }

        old.clear();
        old.putAll(newComponentStrings);
    }

    public void setSidebar (ArrayList<String> sidebarStringList) {

        if (scoreboardObjective == null) return;

        ArrayList<Component> componentList = new ArrayList<>();
        for (String string : sidebarStringList) {
            componentList.add(Component.text(string));
        }

        setSidebarComponents(componentList);
    }

    /*public void updateSidebar () {

        if (!getOfflinePlayer().isOnline()) return;
        Player player = getPlayer();

        HashMap<Integer, String> lineHashMap = new HashMap<>();

        // Iterate through all elements in new list
        List<String> usedLines = new ArrayList<>();
        for (int index = 0; index < sidebarLinesNew.size(); index++) {

            String newString = sidebarLinesNew.get(index);
            int scoreboardNum = sidebarLinesNew.size() - (index + 1);

            lineHashMap.put(scoreboardNum, newString);

            // Check if this string is already in the usedLines list
            while (usedLines.contains(newString)) {
                newString = newString + " ";
            }

            // This means that at this display number, there is already an existing string there
            if (sidebarLinesOld.getOrDefault(scoreboardNum, null) != null) {

                String oldString = sidebarLinesOld.get(scoreboardNum);

                // Check if this existing string matches the new string at the index, and if so no need to rewrite
                if (newString.equals(oldString)) {
                    continue;
                }
            }

            PacketContainer packet = manager.createPacket(PacketType.Play.Server.SCOREBOARD_SCORE);
            packet.getStrings().write(0, newString);
            packet.getStrings().write(1, scoreboardObjective.getName());
            packet.getIntegers().write(0, scoreboardNum);

            manager.sendServerPacket(player, packet);

            usedLines.add(newString);
        }
    }*/
}
