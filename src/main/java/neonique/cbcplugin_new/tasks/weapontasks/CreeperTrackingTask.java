package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CreeperTrackingTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public CreeperTrackingTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    private void creeperexplode(Creeper creeper, boolean enemyNearby) {

        // Display particles
        Particle.DustOptions dustOptions = getDustOptionsForColor(creeper);

        try {
            if (creeper.customName() != null) {
                TextComponent text = (TextComponent) creeper.customName();
            }
        } catch (ClassCastException ignored) {}

        creeper.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, creeper.getLocation(), 1);
        creeper.getWorld().spawnParticle(Particle.DUST, creeper.getLocation(), 30, 1.5, 1.5, 1.5, 1, dustOptions, false);

        // Creeper is going to explode
        creeper.setMaxFuseTicks(0);
        creeper.ignite();
        // If it was a direct hit, play particles
        if (enemyNearby) {
            creeper.getWorld().spawnParticle(Particle.ENCHANTED_HIT, creeper.getLocation(), 150, 0, 1, 0, 1);
        }
    }

    @Override
    public void run() {

        Set<Creeper> explodedCreepers = new HashSet<>();

        if (this.combatManager.creeperProjectileSet == null) {
            return;
        }

        Set<UUID> creeperSet = this.combatManager.creeperProjectileSet;

        for (UUID creeperUUID : creeperSet) {

            Entity entity = gameManager.getWorld().getEntity(creeperUUID);
            if (entity == null) continue;
            if (!(entity instanceof Creeper)) continue;
            Creeper creeper = (Creeper) entity;

            if (creeper.isOnGround()) {
                creeperexplode(creeper, false);
                explodedCreepers.add(creeper);
                continue;
            }

            Location creeperLocation = creeper.getLocation();

            if (creeper.getScoreboardTags().contains("canWallExplode")) {

                double x = creeperLocation.getX();
                double y = creeperLocation.getY();
                double z = creeperLocation.getZ();
                World w = creeperLocation.getWorld();
                Material xp = new Location(w, x + 0.5, y, z).getBlock().getType();
                Material xn = new Location(w, x - 0.5, y, z).getBlock().getType();
                Material zp = new Location(w, x, y, z + 0.5).getBlock().getType();
                Material zn = new Location(w, x, y, z - 0.5).getBlock().getType();
                Material xpc = new Location(w, x + 0.5, y, z + 0.5).getBlock().getType();
                Material xnc = new Location(w, x - 0.5, y, z + 0.5).getBlock().getType();
                Material zpc = new Location(w, x - 0.5, y, z - 0.5).getBlock().getType();
                Material znc = new Location(w, x + 0.5, y, z - 0.5).getBlock().getType();

                if (xp.isSolid() || xn.isSolid() || zp.isSolid() || zn.isSolid() || xpc.isSolid() || xnc.isSolid() || zpc.isSolid() || znc.isSolid()) {
                    creeperexplode(creeper, false);
                    explodedCreepers.add(creeper);
                    continue;
                }
            }

            // Get a list of players nearby
            Collection<Player> playersNearby = creeperLocation.getNearbyEntitiesByType(Player.class, 1.0);
            boolean enemyNearby = false;
            // Check if there are actually any enemies nearby
            if (!playersNearby.isEmpty()) {
                // Check for each player in the collection if they are part of the CBC playerbase
                for (Player player : playersNearby) {
                    // Check if player is a CBC player
                    if (gameManager.hasPlayer(player)) {
                        CBCPlayer cbcPlayer = gameManager.getPlayer(player);
                        // Check if player is alive and is not the owner of this creeper
                        if (!cbcPlayer.isEntityAlly(creeper) && cbcPlayer.isAlive()) {
                            // Signal to trigger an automatic explosion
                            enemyNearby = true;
                            break;
                        }
                    }
                }
            }

            // Cause an explosion if the creeper hits an enemy, hits the ground or hits liquid
            if (enemyNearby) {
                creeperexplode(creeper, true);
                explodedCreepers.add(creeper);
                continue;
            }

            if (creeperLocation.getBlock().isLiquid()) {
                creeperexplode(creeper, false);
                explodedCreepers.add(creeper);
            }
        }

        // Remove all exploded creepers from creeper list
        for (Creeper creeper : explodedCreepers) {
            this.combatManager.creeperProjectileSet.remove(creeper.getUniqueId());
        }
    }

    public Particle.DustOptions getDustOptionsForColor (Creeper creeper) {

        // Display particles
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0F);

        try {
            if (creeper.customName() != null) {
                TextComponent text = (TextComponent) creeper.customName();
                switch (Objects.requireNonNull(text).content()) {
                    case "RedCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.RED), 1.0F);
                        break;
                    case "BlueCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.BLUE), 1.0F);
                        break;
                    case "GreenCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.GREEN), 1.0F);
                        break;
                    case "YellowCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.YELLOW), 1.0F);
                        break;
                    case "CyanCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.AQUA), 1.0F);
                        break;
                    case "OrangeCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.GOLD), 1.0F);
                        break;
                    case "MagentaCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.LIGHT_PURPLE), 1.0F);
                        break;
                    case "PurpleCreeper":
                        dustOptions = new Particle.DustOptions(namedTextColorToColor(NamedTextColor.DARK_PURPLE), 1.0F);
                        break;
                }
            }
        } catch (ClassCastException ignored) {}

        return dustOptions;
    }

    private Color namedTextColorToColor (NamedTextColor color) {
        return Color.fromRGB(color.red(), color.green(), color.blue());
    }
}
