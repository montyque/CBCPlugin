package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.NotNull;


public enum DeathCause {

    CREEPER
    {
        @Override
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
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
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, Sound.Source.PLAYER, 4, 1),
                    location.getX(), location.getY(), location.getZ());
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
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_GLASS_BREAK, Sound.Source.PLAYER, 4, 0),
                    location.getX(), location.getY(), location.getZ());
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
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ITEM_BREAK, Sound.Source.PLAYER, 4, 1),
                    location.getX(), location.getY(), location.getZ());
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE403").color(NamedTextColor.WHITE);
        }
    },

    VOID
    {
        @Override
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ITEM_PICKUP, Sound.Source.PLAYER, 4, 1),
                    location.getX(), location.getY(), location.getZ());
            location.getWorld().spawnParticle(Particle.INSTANT_EFFECT, location.clone().add(0, 1, 0), 
                    80, 0, 1, 0, 1, new Particle.Spell(Color.WHITE, 1));
        }
        @Override
        public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
            return Component.text("\uE404").color(victim.nameColor());
        }
    },
    
    LAVA 
    {
        @Override
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_HURT_ON_FIRE, Sound.Source.PLAYER, 3, 1),
                    location.getX(), location.getY(), location.getZ());
            location.getWorld().spawnParticle(Particle.LAVA, location.clone().add(0, 1, 0),
                    40, 0.5, 0.5, 0.5, 0.5);
        }
    },
    
    DROWN 
    {
        @Override
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, Sound.Source.PLAYER, 3, 1),
                    location.getX(), location.getY(), location.getZ());
            location.getWorld().spawnParticle(Particle.BUBBLE_POP, location.clone().add(0, 1, 0), 
                    150, 0.5, 0.5, 0.5, 0.5);
        }
    },
    
    DISCONNECT,
    LEAVE_PRACTICE,
    
    COMMAND {
        @Override
        public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.Source.PLAYER, 3, 1),
                    location.getX(), location.getY(), location.getZ());
            audience.playSound(Sound.sound(org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_IMPACT, Sound.Source.PLAYER, 3, 1),
                    location.getX(), location.getY(), location.getZ());
            location.getWorld().spawnParticle(Particle.INSTANT_EFFECT, location.clone().add(0, 1, 0), 
                    80, 0.5, 0.5, 0.5, 0.5);
        }
    },
    
    DEATH_BORDER,
    NATURAL,
    XBOW_PIGLIN;
    
    public void playDeathEffect (Audience audience, Location location, CBCPlayer victim) {}
    
    public Component deathIconComponent (CBCPlayer victim, CBCPlayer killer) {
        return Component.text(killer == null ? "\uE405" : "\uE406").color(victim.nameColor());
    }
    
}
