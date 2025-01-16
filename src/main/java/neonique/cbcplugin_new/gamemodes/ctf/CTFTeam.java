package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class CTFTeam extends CBCTeam {

    // Set variables relating to game
    private final CTFGame ctfGame;

    // General variables
    private boolean teamEliminated = false;

    // Flag related variables
    private int flagsLeft;
    private boolean playersRespawn = true;
    protected CTFPlayer flagHolder = null;
    private UUID hologramUUID = null;

    private Location flagLocation;
    private Set<Location> teamSpawns;

    // Statistics
    protected int timesFlagPickedUp = 0;
    protected int timesFlagCaptured = 0;

    private int teamTimeAlive = 0;

    public CTFTeam(CTFGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                   String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);
        ctfGame = game;
        flagsLeft = game.getFlagsStart();
    }

    public ArmorStand getHologram () {
        if (hologramUUID == null) {
            return null;
        } else {
            try {
                return (ArmorStand) ctfGame.getWorld().getEntity(hologramUUID);
            } catch (ClassCastException e) {
                return null;
            }
        }
    }

    @Override
    public void removeTeam () {
        super.removeTeam();
        if (hologramUUID != null) {
            getHologram().remove();
        }
    }

    public void setBaseVariables(Location flagLocation, Set<Location> teamSpawns) {
        this.flagLocation = flagLocation;
        this.teamSpawns = teamSpawns;

        this.flagLocation.setYaw(getAngle(new Vector(flagLocation.getX(), 0, flagLocation.getZ()), ctfGame.getMap().getMapCentre().toVector()));
        setBannerBlock();

        ArmorStand hg = (ArmorStand) ctfGame.getWorld().spawnEntity(flagLocation, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.COMMAND,
                hologram -> {
                    hologram.setGravity(false);
                    hologram.setInvulnerable(true);
                    hologram.setCustomNameVisible(true);
                    hologram.customName(Component.text("⚑ " + getTeamName() + " Flag ⚑").color(getColor()).decorate(TextDecoration.BOLD));
                }
        );
        hg.setInvisible(true);
        hologramUUID = hg.getUniqueId();
    }

    public void flagPickedUp(CTFPlayer player) {

        flagHolder = player;

        // Set flag block to air
        Block block = flagLocation.getBlock();
        block.setType(Material.AIR);

        // Play sound and title to all players on team
        Title title = Title.title(
                Component.text("Your flag was picked up!").color(getColor()).decorate(TextDecoration.BOLD),
                Component.text("picked up by ").color(NamedTextColor.WHITE)
                        .append(player.getNameComponent()),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150))
        );

        for (CBCPlayer teamplr : getPlayers()) {
            if (teamplr.isOnline()) {

                CTFPlayer ctfPlayerObj = (CTFPlayer) teamplr;
                Set<Player> glowingPlayers = ctfPlayerObj.getGlowingPlayers();
                ctfGame.getGlowManager().updateGlowingList(teamplr.getPlayer(), glowingPlayers);

                teamplr.getPlayer().playSound(teamplr.getPlayer().getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 100, 0);
                teamplr.getPlayer().showTitle(title);
            }
        }

        timesFlagPickedUp++;
        getHologram().setCustomNameVisible(false);

        for (CBCPlayer cplayer : getOnlinePlayers()) {

            Set<Player> glowingPlayers = ((CTFPlayer) cplayer).getGlowingPlayers();
            ctfGame.getGlowManager().updateGlowingList(cplayer.getPlayer(), glowingPlayers);

            try {
                ((CTFPlayer) cplayer).updateFlagHolderLaser();
            } catch (ReflectiveOperationException ignored) {}
        }

        ctfGame.getSidebarManager().updateServerBoard();
    }

    public void flagCaptured() {

        flagsLeft--;

        Component title;
        Component subtitle;

        if (flagsLeft == 0) {
            playersRespawn = false;
            title = Component.text("Your flags are gone!").color(getColor()).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Captured by ").color(NamedTextColor.WHITE)
                    .append(flagHolder.getNameComponent())
                    .append(Component.text(" - no more respawning!").color(NamedTextColor.WHITE));
        }
        else {
            title = Component.text("Your flag was captured!").color(getColor()).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Captured by ").color(NamedTextColor.WHITE)
                    .append(flagHolder.getNameComponent())
                    .append(Component.text(" - " + flagsLeft + " ⚑ left").color(NamedTextColor.WHITE));
        }

        for (CBCPlayer teamplr : getOnlinePlayers()) {
            if (teamplr.isOnline()) {

                teamplr.getPlayer().showTitle(
                        Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150)))
                );
            }
        }

        // Eliminate all offline players
        if (flagsLeft == 0) {
            for (CBCPlayer teamplr : getPlayers()) {
                if (!teamplr.isOnline()) {
                    ((CTFPlayer) teamplr).eliminatePlayer();
                }
            }
        }

        timesFlagCaptured++;
        flagReset();

        ctfGame.checkIfFlagsLeft();
    }

    public void flagReset() {
        flagHolder = null;
        if (flagsLeft > 0) {
            getHologram().setCustomNameVisible(true);
            setBannerBlock();
        }

        for (CBCPlayer player : getOnlinePlayers()) {

            Set<Player> glowingPlayers = ((CTFPlayer) player).getGlowingPlayers();
            ctfGame.getGlowManager().updateGlowingList(player.getPlayer(), glowingPlayers);

            try {
                ((CTFPlayer) player).updateFlagHolderLaser();
            } catch (ReflectiveOperationException ignored) {}
        }

        ctfGame.getSidebarManager().updateServerBoard();
    }

    public Location getFlagLocation() {
        return flagLocation;
    }

    public Location getPlayerSpawn() {

        List<Location> validSpawns = new ArrayList<>();
        for (Location spawn : teamSpawns) {
            boolean validSpawn = true;
            for (Player player : spawn.getNearbyEntitiesByType(Player.class, 0.3)) {
                if (ctfGame.getPlayer(player) != null) {
                    validSpawn = false;
                    break;
                }
            }
            if (validSpawn) {
                validSpawns.add(spawn);
            }
        }

        if (validSpawns.size() == 0) {
            validSpawns = new ArrayList<>(teamSpawns);
        }

        return validSpawns.get(new Random().nextInt(validSpawns.size()));
    }

    public ItemStack getBannerItem() {

        Material bannerPrimaryColor = null;
        DyeColor bannerSecondaryColor = null;
        PatternType bannerPattern = null;

        switch (getTeamName()) {
            case "Red":
                bannerPrimaryColor = Material.RED_BANNER;
                bannerSecondaryColor = DyeColor.ORANGE;
                bannerPattern = PatternType.FLOWER;
                break;
            case "Blue":
                bannerPrimaryColor = Material.LIGHT_BLUE_BANNER;
                bannerSecondaryColor = DyeColor.BLUE;
                bannerPattern = PatternType.MOJANG;
                break;
            case "Green":
                bannerPrimaryColor = Material.LIME_BANNER;
                bannerSecondaryColor = DyeColor.GREEN;
                bannerPattern = PatternType.CREEPER;
                break;
            case "Yellow":
                bannerPrimaryColor = Material.YELLOW_BANNER;
                bannerSecondaryColor = DyeColor.ORANGE;
                bannerPattern = PatternType.GLOBE;
        }

        if (bannerPrimaryColor == null) {
            return null;
        }

        ItemStack banner = new ItemStack(bannerPrimaryColor);
        BannerMeta bannerMeta = (BannerMeta) banner.getItemMeta();

        bannerMeta.addPattern(new Pattern(bannerSecondaryColor, PatternType.GRADIENT_UP));
        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, bannerPattern));

        banner.setItemMeta(bannerMeta);
        return banner;
    }

    public void setBannerBlock() {

        Block block = flagLocation.getBlock();

        Material bannerPrimaryColor = null;
        DyeColor bannerSecondaryColor = null;
        PatternType bannerPattern = null;

        switch (getTeamName()) {
            case "Red":
                bannerPrimaryColor = Material.RED_BANNER;
                bannerSecondaryColor = DyeColor.ORANGE;
                bannerPattern = PatternType.FLOWER;
                break;
            case "Blue":
                bannerPrimaryColor = Material.LIGHT_BLUE_BANNER;
                bannerSecondaryColor = DyeColor.BLUE;
                bannerPattern = PatternType.MOJANG;
                break;
            case "Green":
                bannerPrimaryColor = Material.LIME_BANNER;
                bannerSecondaryColor = DyeColor.GREEN;
                bannerPattern = PatternType.CREEPER;
                break;
            case "Yellow":
                bannerPrimaryColor = Material.YELLOW_BANNER;
                bannerSecondaryColor = DyeColor.ORANGE;
                bannerPattern = PatternType.GLOBE;
        }

        if (bannerPrimaryColor == null) {
            return;
        }

        block.setType(bannerPrimaryColor);
        Banner bannerState = (Banner) block.getState();

        List<Pattern> patterns = new ArrayList<>();
        patterns.add(new Pattern(bannerSecondaryColor, PatternType.GRADIENT_UP));
        patterns.add(new Pattern(DyeColor.WHITE, bannerPattern));
        bannerState.setPatterns(patterns);
        bannerState.update();

        // Set direction of banner
        Rotatable blockData = (Rotatable) block.getBlockData();
        float yaw = flagLocation.getYaw() % 360f;
        if (yaw >= (360f / 16f) && yaw < (360f / 16f) * 3f) {
            blockData.setRotation(BlockFace.SOUTH_WEST);
        } else if (yaw >= (360f / 16f) * 3f && yaw < (360f / 16f) * 5f) {
            blockData.setRotation(BlockFace.WEST);
        } else if (yaw >= (360f / 16f) * 5f && yaw < (360f / 16f) * 7f) {
            blockData.setRotation(BlockFace.NORTH_WEST);
        } else if (yaw >= (360f / 16f) * 7f && yaw < (360f / 16f) * 9f) {
            blockData.setRotation(BlockFace.NORTH);
        } else if (yaw >= (360f / 16f) * 9f && yaw < (360f / 16f) * 11f) {
            blockData.setRotation(BlockFace.NORTH_EAST);
        } else if (yaw >= (360f / 16f) * 11f && yaw < (360f / 16f) * 13f) {
            blockData.setRotation(BlockFace.EAST);
        } else if (yaw >= (360f / 16f) * 13f && yaw < (360f / 16f) * 15f) {
            blockData.setRotation(BlockFace.SOUTH_EAST);
        } else {
            blockData.setRotation(BlockFace.SOUTH);
        }

        block.setBlockData(blockData);
    }

    public boolean canRespawn() {
        return playersRespawn;
    }

    private float getAngle(Vector point1, Vector point2) {
        double dx = point2.getX() - point1.getX();
        double dz = point2.getZ() - point1.getZ();
        float angle = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        if (angle < 0) {
            angle += 360.0F;
        }
        return angle;
    }

    public boolean isFlagAtBase() {
        return !(flagsLeft == 0 || flagHolder != null);
    }

    public void eliminateTeam() {
        teamEliminated = true;
        // Send message
        ctfGame.getGameManager().sendGlobalMessage(
                Component.newline().append(Component.text("TEAM ELIMINATED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(Component.text(getTeamName() + " Team").color(getColor()))
                        .append(Component.text(" has been eliminated!").color(NamedTextColor.WHITE)).append(Component.newline())
        );
        // Check if game can end
        ctfGame.checkIfWinner();
    }

    public boolean isTeamEliminated() {
        return teamEliminated;
    }

    public int countNonEliminatedPlayers () {
        int nonEliminatedPlayers = 0;
        for (CBCPlayer player : getPlayers()) {
            CTFPlayer ctfplayer = (CTFPlayer) player;
            if (!ctfplayer.isEliminated()) {
                nonEliminatedPlayers++;
            }
        }

        // Eliminate team if no more players are not eliminated
        if (nonEliminatedPlayers == 0 && !teamEliminated) {
            eliminateTeam();
        }
        return nonEliminatedPlayers;
    }

    public void setFlagsLeft(int i) {
        flagsLeft = i;
        if (flagsLeft <= 0) {
            // If a player currently has the flag, drop it
            if (flagHolder != null) {
                flagHolder.playerDropFlag();
                flagReset();
            }
        }
        ctfGame.checkIfFlagsLeft();
        ctfGame.getSidebarManager().updateServerBoard();
    }

    public int getFlagsLeft() {
        return flagsLeft;
    }

    public int getTimesFlagPickedUp() {
        return timesFlagPickedUp;
    }

    public int getTimesFlagCaptured() {
        return timesFlagCaptured;
    }

    public void incrementTimeTeamAlive() {
        if (!teamEliminated) teamTimeAlive++;
    }

    public int getTeamTimeAlive() {
        return teamTimeAlive;
    }

    public CTFPlayer getFlagHolder() {
        return flagHolder;
    }

    public void allyPickedUpFlag() {

        for (CBCPlayer player : getPlayers()) {
            if (player.isOnline()) {
                player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 300, 2);
            }
        }
    }

    public void removeFlag() {

        flagsLeft--;

        Component title;
        Component subtitle;

        if (flagsLeft <= 0) {
            playersRespawn = false;
            title = Component.text("Your flags are gone!").color(getColor()).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Flag removed by timer").color(NamedTextColor.WHITE)
                    .append(Component.text(" - no more respawning!").color(NamedTextColor.WHITE));
            // If a player currently has the flag, drop it
            if (flagHolder != null) {
                flagHolder.playerDropFlag();
                flagReset();
            }
        }
        else {
            title = Component.text("Flags decreased!").color(getColor()).decorate(TextDecoration.BOLD);
            subtitle = Component.text("Flag removed by timer ").color(NamedTextColor.WHITE)
                    .append(Component.text(" - " + flagsLeft + " ⚑ left").color(NamedTextColor.WHITE));
        }

        for (CBCPlayer teamplr : getOnlinePlayers()) {
            if (teamplr.isOnline()) {
                Player entity = teamplr.getPlayer();
                entity.playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 200, 1);
                entity.showTitle(
                        Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150)))
                );
            }
        }

        // Eliminate all offline players
        if (flagsLeft == 0) {
            for (CBCPlayer teamplr : getPlayers()) {
                if (!teamplr.isOnline()) {
                    ((CTFPlayer) teamplr).eliminatePlayer();
                }
            }
            // Remove hologram and set banner to air
            getHologram().setCustomNameVisible(false);
            Block block = flagLocation.getBlock();
            block.setType(Material.AIR);
        }
    }
}
