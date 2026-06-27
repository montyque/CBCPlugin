package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.core.CBCPlayer;
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
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class CTFTeam extends CBCTeam<CTFPlayer> {

    // Set variables relating to game
    private final CTFGame game;

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

    public CTFTeam (CTFGame game, TeamLike originalTeam, String teamIdNum) {
        super(originalTeam, teamIdNum);
        this.game = game;
        flagsLeft = game.getFlagsStart();
    }

    public AreaEffectCloud getHologram () {
        if (hologramUUID == null) {
            return null;
        } else {
            try {
                return (AreaEffectCloud) game.getWorld().getEntity(hologramUUID);
            } catch (ClassCastException e) {
                return null;
            }
        }
    }

    @Override
    public void removeTeam () {
        super.removeTeam();
        AreaEffectCloud hg = getHologram();
        if (hg != null) {
            getHologram().remove();
        }
    }

    public void setBaseVariables(Location flagLocation, Set<Location> teamSpawns) {
        this.flagLocation = flagLocation;
        this.teamSpawns = teamSpawns;

        this.flagLocation.setYaw(getAngle(new Vector(flagLocation.getX(), 0, flagLocation.getZ()), game.getMap().getMapCentre().toVector()));
        setBannerBlock();
        createFlagHologram(this.flagLocation.clone().add(0, 2, 0));

    }

    public void createFlagHologram(Location hologramLocation) {

        // Delete any nearby holograms
        Collection<AreaEffectCloud> nearbyHolograms = hologramLocation.getNearbyEntitiesByType(AreaEffectCloud.class, 0.1);
        for (AreaEffectCloud h : nearbyHolograms) {
            if (!h.isDead()) {
                h.remove();
            }
        }

        AreaEffectCloud hologram = (AreaEffectCloud) hologramLocation.getWorld().spawnEntity(hologramLocation, EntityType.AREA_EFFECT_CLOUD);
        hologram.clearCustomEffects();
        hologram.setRadius(0);
        hologram.setDuration(30000000);
        hologramUUID = hologram.getUniqueId();

        hologram.setCustomNameVisible(true);
        hologram.customName(Component.text("⚑ " + name() + " Flag ⚑").color(textColor()).decorate(TextDecoration.BOLD));

    }

    public void flagPickedUp(CTFPlayer player) {

        flagHolder = player;

        // Play sound and title to all players on team
        Title title = Title.title(
                Component.text("Your flag was picked up!").color(textColor()).decorate(TextDecoration.BOLD),
                Component.text("picked up by ").color(NamedTextColor.WHITE)
                        .append(player.nameComponent()),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150))
        );

        for (CTFPlayer teamPlayer : players()) {
            if (teamPlayer.isOnline()) {

                Set<Player> glowingPlayers = teamPlayer.getGlowingPlayers();
                game.getGlowManager().updateGlowingList(teamPlayer.getPlayer(), glowingPlayers);

                teamPlayer.getPlayer().playSound(teamPlayer.getPlayer().getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 100, 0);
                teamPlayer.getPlayer().showTitle(title);

            }
        }

        timesFlagPickedUp++;
        getHologram().setCustomNameVisible(false);
        flagLocation.getBlock().setType(Material.AIR);

        game.updateServerSidebar();
    }

    public void flagCaptured() {

        timesFlagCaptured++;
        removeFlag(flagHolder);
        flagReset();
        game.checkIfFlagsLeft();

    }

    public void flagReset() {

        flagHolder = null;

        if (flagsLeft > 0) {
            getHologram().setCustomNameVisible(true);
            setBannerBlock();
        } else {
            getHologram().setCustomNameVisible(false);
            flagLocation.getBlock().setType(Material.AIR);
        }

        for (CTFPlayer player : players()) {
            if (!player.isOnline()) continue;
            Set<Player> glowingPlayers = player.getGlowingPlayers();
            game.getGlowManager().updateGlowingList(player.getPlayer(), glowingPlayers);
        }

        game.updateServerSidebar();

    }

    public Location getFlagLocation() {
        return flagLocation;
    }

    public Location getPlayerSpawn() {

        List<Location> validSpawns = new ArrayList<>();
        for (Location spawn : teamSpawns) {
            boolean validSpawn = true;
            for (Player player : spawn.getNearbyEntitiesByType(Player.class, 0.3)) {
                if (game.getPlayer(player) != null) {
                    validSpawn = false;
                    break;
                }
            }
            if (validSpawn) {
                validSpawns.add(spawn);
            }
        }

        if (validSpawns.isEmpty()) {
            validSpawns = new ArrayList<>(teamSpawns);
        }

        return validSpawns.get(new Random().nextInt(validSpawns.size()));
    }

    public ItemStack getBannerItem() {

        Material bannerPrimaryColor = null;
        DyeColor bannerSecondaryColor = null;
        PatternType bannerPattern = null;

        switch (name()) {
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

        switch (name()) {
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
        Rotatable blockData = getRotatable(block);

        block.setBlockData(blockData);
    }

    private Rotatable getRotatable(Block block) {
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
        return blockData;
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
        game.getGameManager().sendGlobalMessage(
                Component.newline().append(Component.text("TEAM ELIMINATED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(Component.text(name() + " Team").color(textColor()))
                        .append(Component.text(" has been eliminated!").color(NamedTextColor.WHITE)).append(Component.newline())
        );

        // Check if game can end
        game.checkIfWinner();
    }

    public void reviveTeam() {

        teamEliminated = false;

        // Send message
        game.getGameManager().sendGlobalMessage(
                Component.newline().append(Component.text("TEAM REVIVED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(Component.text(name() + " Team").color(textColor()))
                        .append(Component.text(" has been revived!").color(NamedTextColor.WHITE)).append(Component.newline())
        );

        // Check if game can end
        game.checkIfWinner();
    }

    public boolean isTeamEliminated() {
        return teamEliminated;
    }

    public void checkIfEliminated() {

        int nonEliminatedPlayers = 0;
        for (CTFPlayer player : players()) {
            if (!player.isEliminated()) {
                nonEliminatedPlayers++;
            }
        }

        // Eliminate team if no more players are not eliminated
        if (nonEliminatedPlayers == 0 && !teamEliminated) {
            eliminateTeam();
        }

    }

    public void setFlagsLeft(int i) {
        flagsLeft = i;
        if (flagsLeft <= 0) {
            // If a player currently has the flag, drop it
            if (flagHolder != null) {
                flagHolder.playerDropFlag();
            }
            flagReset();
        }
        game.checkIfFlagsLeft();
        game.updateServerSidebar();
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

        for (CBCPlayer player : players()) {
            if (player.isOnline()) {
                player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 300, 2);
            }
        }
    }

    public void removeFlag (CTFPlayer flagCapturer) {

        flagsLeft--;

        Component title;
        Component subtitle;

        if (flagsLeft == 0) {
            playersRespawn = false;
            title = Component.text("Your flags are gone!").color(textColor()).decorate(TextDecoration.BOLD);

            if (flagCapturer != null) {
                subtitle = Component.text("Captured by ").color(NamedTextColor.WHITE)
                        .append(flagCapturer.nameComponent())
                        .append(Component.text(" - no more respawning!").color(NamedTextColor.WHITE));
            } else {
                subtitle = Component.text("Flag removed by timer").color(NamedTextColor.WHITE)
                        .append(Component.text(" - no more respawning!").color(NamedTextColor.WHITE));

                // If a player currently has this team's flag, drop it
                if (flagHolder != null) {
                    flagHolder.playerDropFlag();
                }
                flagReset();
            }
        }
        else {
            if (flagCapturer != null) {
                title = Component.text("Your flag was captured!").color(textColor()).decorate(TextDecoration.BOLD);
                subtitle = Component.text("Captured by ").color(NamedTextColor.WHITE)
                        .append(flagCapturer.nameComponent())
                        .append(Component.text(" - " + flagsLeft + " ⚑ left").color(NamedTextColor.WHITE));
            } else {
                title = Component.text("Flag lost!").color(textColor()).decorate(TextDecoration.BOLD);
                subtitle = Component.text("Flag removed by timer ").color(NamedTextColor.WHITE)
                        .append(Component.text(" - " + flagsLeft + " ⚑ left").color(NamedTextColor.WHITE));
            }
        }

        for (CTFPlayer teamPlayer : players()) {
            if (teamPlayer.isOnline()) {
                Player entity = teamPlayer.getPlayer();
                entity.playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 200, 1);
                entity.showTitle(
                        Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150)))
                );
            } else if (flagsLeft == 0) {
                // Eliminate any players offline if no flags are remaining
                teamPlayer.eliminatePlayer();
            }
        }
    }
}
