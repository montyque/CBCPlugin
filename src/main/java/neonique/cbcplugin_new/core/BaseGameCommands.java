package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BaseGameCommands {

    private final Game<?, ?> game;

    public BaseGameCommands (Game<?, ?> game) {
        this.game = game;
    }

    public boolean checkIfPerms (Player user, int userPerms, int permsNeeded) {
        if (userPerms < permsNeeded) {
            user.sendMessage(Component.text("You do not have permission to run this command.").color(NamedTextColor.RED));
            return true;
        } else {
            return false;
        }
    }

    public void sendColorMessage (Player receiver, String message, TextColor color) {
        receiver.sendMessage(Component.text(message).color(color));
    }

    public Player findPlayerWithName (String name) {
        return Bukkit.getServer().getPlayer(name);
    }

    public CBCPlayer findPlayerInGame (Player user, String name) {

        // Find player targeted
        Player targetedPlayer = findPlayerWithName(name);
        if (targetedPlayer == null) {
            sendColorMessage(user, "Could not find player " + name + "!", NamedTextColor.YELLOW);
            return null;
        }

        CBCPlayer player = game.getPlayer(targetedPlayer);
        if (player == null) {
            sendColorMessage(user, name + " is not in the game right now!", NamedTextColor.YELLOW);
            return null;
        }

        return player;

    }

    public Set<String> getPlayerNamesOnline () {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> getPlayerNamesInGame (Predicate<CBCPlayer> condition) {
        return game.getPlayers().stream()
                .filter(condition)
                .map(CBCPlayer::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void broadcastAction (Player user, String message) {
        game.getGameManager().sendGlobalMessage(
                Component.text("[" + user.getName() + ": " + message + "]").decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY)
        );
    }

    public void run(Player user, String[] args, int perms) {
        // Go through each command
        switch (args[0]) {
            case "kill":
                kill(user, args, perms);
                break;
            case "revive":
                revive(user, args, perms);
                break;
        }
    }

    public List<String> tabComplete(Player user, String[] args, int perms) {

        int level = args.length;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 1) {
            if (perms >= 1) {
                tabCompletions.add("kill");
                tabCompletions.add("revive");
            }
        }
        else if (level >= 2) {
            switch (args[0]) {
                case "kill":
                    tabCompletions = killTabCompletions(args, perms);
                    break;
                case "revive":
                    tabCompletions = reviveTabCompletions(args, perms);
                    break;
            }
        }

        return tabCompletions;
    }

    public void kill(Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (args.length < 2) {
            sendColorMessage(user, "You must include a player name!", NamedTextColor.YELLOW);
            return;
        }

        // Check if player is alive right now
        String playerName = args[1];
        CBCPlayer playerObj = findPlayerInGame(user, args[1]);

        if (playerObj == null) return;

        if (!playerObj.isAlive()) {
            sendColorMessage(user, playerName + " is currently dead! You can only use this command on alive players.", NamedTextColor.YELLOW);
            return;
        }

        game.getCombatManager().playerDeath(playerObj, playerObj.getLastPlayerHitBy(), DeathCause.COMMAND, false);

        sendColorMessage(user, playerName + " has been killed!", NamedTextColor.GREEN);
        broadcastAction(user, "used /game kill to kill " + playerName);
    }

    public void revive(Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (args.length < 2) {
            sendColorMessage(user, "You must include a player name!", NamedTextColor.YELLOW);
            return;
        }

        // Check if player is alive right now
        String playerName = args[1];
        CBCPlayer playerObj = findPlayerInGame(user, args[1]);

        if (playerObj == null) return;

        if (playerObj.isAlive()) {
            sendColorMessage(user, playerName + " is currently alive! You can only use this command on dead players.", NamedTextColor.YELLOW);
            return;
        }

        game.getCombatManager().playerRespawn(playerObj);

        sendColorMessage(user, playerName + "has been revived!", NamedTextColor.GREEN);
        broadcastAction(user, "used /game revive to revive " + playerName);

    }

    public List<String> killTabCompletions(String[] args, int perms) {
        int level = args.length;
        List<String> tabCompletions = new ArrayList<>();
        if (perms < 1) return tabCompletions;
        if (level == 2) tabCompletions = new ArrayList<>(getPlayerNamesInGame(p -> p.isOnline() && p.isAlive()));
        return tabCompletions;
    }

    public List<String> reviveTabCompletions(String[] args, int perms) {
        int level = args.length;
        List<String> tabCompletions = new ArrayList<>();
        if (perms < 1) return tabCompletions;
        if (level == 2) tabCompletions = new ArrayList<>(getPlayerNamesInGame(p -> p.isOnline() && !p.isAlive()));
        return tabCompletions;
    }


}
