package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.BaseTeamGameCommands;
import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TagGameCommands extends BaseTeamGameCommands {

    private final TagGame game;

    public TagGameCommands (TagGame game) {
        super(game);
        this.game = game;
    }

    @Override
    public void run(Player user, String[] args, int perms) {
        // Go through each base game command
        super.run(user, args, perms);
        // Add gamemode specific commands
        if ("setingame".equals(args[0])) {
            setInGame(user, args, perms);
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
                // Add all sub commands
                tabCompletions.add("setingame");
            }
        }
        else if (level >= 2) {
            String subcommand = args[0];
            if (perms >= 1) {
                if (subcommand.equals("setingame")) {
                    if (level == 2) {
                        // Set in game sub command - first argument of player names
                        tabCompletions = new ArrayList<>(game.getPlayerNames());
                    }
                    else if (level == 3) {
                        // Set in game sub command - second argument of in game state
                        tabCompletions.add("true");
                        tabCompletions.add("false");
                    }
                }
            }
        }

        return tabCompletions;
    }

    public void setInGame(Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (args.length < 2) {
            sendColorMessage(user, "You must include a player!", NamedTextColor.YELLOW);
            return;
        }

        if (args.length < 3) {
            sendColorMessage(user, "Use 'true' or 'false' to set the in game status of a player.", NamedTextColor.YELLOW);
            return;
        }

        List<TagPlayer> players = game.getPlayers();
        TagPlayer playerTargeted = null;

        // Try find player
        for (TagPlayer player : players) {
            // Check if name matches with second argument (non case sensitive)
            if (player.getName().equalsIgnoreCase(args[1])) {
                playerTargeted = player;
                break;
            }
        }

        if (playerTargeted == null) {
            sendColorMessage(user, args[1] + " is not a registered player!", NamedTextColor.YELLOW);
            return;
        }

        // Set player's in game state to true or false
        String inGameState = args[2];
        if (inGameState.equalsIgnoreCase("true")) {
            // Set player's in game state to true
            sendColorMessage(user, playerTargeted.getName() + " is now set as In-Game.", NamedTextColor.GREEN);
            playerTargeted.setInGame(true);
        } else if (inGameState.equalsIgnoreCase("false")) {
            // Set player's in game state to true
            sendColorMessage(user, playerTargeted.getName() + " is now set as NOT In-Game.", NamedTextColor.GREEN);
            playerTargeted.setInGame(false);
        }
        else {
            // Set player's in game state to true
            sendColorMessage(user, "Use 'true' or 'false' to set the in game status of a player.", NamedTextColor.GREEN);
        }
    }

    @Override
    public void putPlayerOnTeam (CBCPlayer player, CBCTeam<?> team, boolean spawnImmediately) {

        TagPlayer tagPlayer = game.getTypedPlayer(player);
        TagTeam tagTeam = game.getTypedTeam(team);

        // Kill player if player is alive
        if (player.isAlive()) {
            game.getCombatManager().playerDeath(player, player.getLastPlayerHitBy(), DeathCause.COMMAND, false);
        }

        removePlayerFromTeam(player, false);
        game.addPlayerToTeam(player, team);

        if (player.isOnline()) {
            player.getPlayer().sendMessage(
                    Component.text("You have been added to ").color(NamedTextColor.GREEN).append(
                            Component.text(team.name()).color(team.textColor())
                    ).append(
                            Component.text(" team!").color(NamedTextColor.GREEN)
                    ).decorate(TextDecoration.BOLD)
            );

            // If player is tagger and joined before taggers released, put them in
            if (tagPlayer.isTagger()) {
                if (game.getTaggerReleaseTimer() > 0) {
                    // Setup player round and teleport them to tagger spawn
                    tagPlayer.setCanMove(true);
                    tagPlayer.playerSetupRound();
                    tagPlayer.teleportPlayerToSpawn(tagTeam.getRandomTaggerSpawn(), game.getMap().getMapCentre());
                    tagPlayer.setCanMove(false);
                }
            }
            // If player is evader and joined before evaders released, put them in
            else {
                if (!game.isRoundInPlay() && game.getTaggerReleaseTimer() > 0) {
                    // Setup player round and teleport them to tagger spawn
                    tagPlayer.setCanMove(true);
                    tagPlayer.playerSetupRound();
                    tagPlayer.teleportPlayerToSpawn(tagTeam.getRandomEvaderSpawn(), game.getMap().getMapCentre());
                    tagPlayer.setCanMove(false);
                }
            }
        }
    }

    @Override
    public void revive(Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (!game.isRoundInPlay()) {
            sendColorMessage(user, "Round is not in play right now!", NamedTextColor.YELLOW);
            return;
        }

        if (args.length < 2) {
            sendColorMessage(user, "You must include a player name!", NamedTextColor.YELLOW);
            return;
        }

        // Check if player is alive right now
        CBCPlayer playerObj = findPlayerInGame(user, args[1]);

        if (playerObj == null) return;
        String playerName = playerObj.getName();

        if (playerObj.isAlive()) {
            sendColorMessage(user, playerName + " is currently alive! You can only use this command on dead players.", NamedTextColor.YELLOW);
            return;
        }

        if (!(playerObj instanceof TagPlayer tagPlayer)) {
            sendColorMessage(user, playerName + " does not have a TagPlayer object.", NamedTextColor.YELLOW);
            return;
        }
        if (!(tagPlayer.getTeam() instanceof TagTeam team)) {
            sendColorMessage(user, playerName + " does not have a TagTeam object.", NamedTextColor.YELLOW);
            return;
        }

        if (!tagPlayer.isOnline()) {
            sendColorMessage(user, playerName + " cannot be revived as they are offline!", NamedTextColor.YELLOW);
            return;
        }

        tagPlayer.playerStartRound();

        if (tagPlayer.isTagger()) {
            // Teleport player to tagger spawn
            tagPlayer.teleportPlayerToSpawn(team.getRandomTaggerSpawn(), game.getMap().getMapCentre());
        }
        else {
            // Teleport player to evader spawn
            tagPlayer.teleportPlayerToSpawn(team.getRandomEvaderSpawn(), game.getMap().getMapCentre());
            tagPlayer.setEliminated(false);
        }

        sendColorMessage(user, playerName + " has been revived!", NamedTextColor.GREEN);
        broadcastAction(user, "used /game revive to revive " + playerName);

    }
}
