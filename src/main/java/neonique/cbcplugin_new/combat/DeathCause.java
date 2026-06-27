package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;


public enum DeathCause {

    CREEPER
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            Firework firework = (Firework) location.getWorld().spawnEntity(location.clone().add(0, 2, 0),
                    EntityType.FIREWORK_ROCKET, CreatureSpawnEvent.SpawnReason.COMMAND);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            Color fireworkColor = Color.fromRGB(victim.nameColor().value());
            fireworkMeta.addEffect(FireworkEffect.builder().withColor(fireworkColor)
                    .with(FireworkEffect.Type.BALL).build());
            fireworkMeta.setPower(0);
            firework.setFireworkMeta(fireworkMeta);
            firework.detonate();
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE400").color(NamedTextColor.WHITE);
        }
    },

    FLAMEZONE
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.ITEM_FIRECHARGE_USE, 4, 1);
            location.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, location, 15,
                    0.4, 0, 0.4, 0.01);
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE401").color(NamedTextColor.WHITE);
        }
    },

    XBOW
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.BLOCK_GLASS_BREAK, 4, 0);
            location.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location.clone().add(0, 1, 0),
                    20, 0, 0, 0, 0.5);
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE402").color(NamedTextColor.WHITE);
        }
    },

    MELEE
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.ENTITY_ITEM_BREAK, 4, 1);
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE403").color(NamedTextColor.WHITE);
        }
    },

    VOID
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.ENTITY_ITEM_PICKUP, 4, 1);
            location.getWorld().spawnParticle(Particle.INSTANT_EFFECT, location.clone().add(0, 1, 0), 
                    80, 0, 1, 0, 1);
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE404").color(victim.nameColor());
        }
    },
    
    LAVA 
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 3, 1);
            location.getWorld().spawnParticle(Particle.LAVA, location.clone().add(0, 1, 0),
                    40, 0.5, 0.5, 0.5, 0.5);
        }
    },
    
    DROWN 
    {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, 3F, (float) 1);
            location.getWorld().spawnParticle(Particle.BUBBLE_POP, location.clone().add(0, 1, 0), 
                    150, 0.5, 0.5, 0.5, 0.5);
        }
    },
    
    DISCONNECT,
    LEAVE_PRACTICE,
    
    COMMAND {
        @Override
        public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {
            gameManager.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3F, (float) 1);
            gameManager.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 3F, (float) 1);
            location.getWorld().spawnParticle(Particle.INSTANT_EFFECT, location.clone().add(0, 1, 0), 
                    80, 0.5, 0.5, 0.5, 0.5);
        }
    },
    
    DEATH_BORDER,
    NATURAL,
    XBOW_PIGLIN;
    
    public void playDeathEffect (GameManager gameManager, Location location, CBCPlayer victim) {}
    
    public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
        return Component.text(killer == null ? "\uE405" : "\uE406").color(victim.nameColor());
    }
    
}
