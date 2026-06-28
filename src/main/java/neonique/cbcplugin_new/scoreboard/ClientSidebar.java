package neonique.cbcplugin_new.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class ClientSidebar {

    private final Scoreboard scoreboard;

    private final UUID playerUUID;

    private final Map<Integer, String> old = new HashMap<>();
    private final Map<Integer, String> newComponentStrings = new HashMap<>();

    private final Map<Integer, String> intToChar = new HashMap<>();
    private final Map<Integer, Team> intToTeam = new HashMap<>();

    private Objective scoreboardObjective;
    private final boolean hideNumbers;

    public ClientSidebar (Player player, Scoreboard scoreboard, String sidebarName, boolean hideNumbers) {

        this.playerUUID = player.getUniqueId();
        this.scoreboard = scoreboard;

        // Create scoreboard objective
        try {
            scoreboardObjective = scoreboard.registerNewObjective(sidebarName, "dummy", Component.space());
        } catch (IllegalArgumentException e) {
            scoreboardObjective = scoreboard.getObjective(sidebarName);
            if (scoreboardObjective == null) {
                throw new RuntimeException("Could not register or find scoreboard with name '%s'".formatted(sidebarName), e);
            }
        }

        scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.hideNumbers = hideNumbers;
        if (hideNumbers) {
            scoreboardObjective.numberFormat(NumberFormat.blank());
        }

        createDisplayTeams();

    }

    public void createDisplayTeams () {
        intToChar.clear();
        intToTeam.clear();
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
        intToTeam.values().forEach(Team::unregister);
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

    public void setSidebarComponents (List<Component> sidebarStringList) {

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
            } else {

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
                } else {
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

    public void clearSidebar () {
        setSidebarComponents(List.of());
    }

    public void setSidebar (List<String> sidebarStringList) {
        if (scoreboardObjective == null) return;
        ArrayList<Component> componentList = new ArrayList<>();
        for (String string : sidebarStringList) {
            componentList.add(Component.text(string));
        }
        setSidebarComponents(componentList);
    }

    public void setDisplayHeader (Component header) {
        scoreboardObjective.displayName(header);
    }

}
