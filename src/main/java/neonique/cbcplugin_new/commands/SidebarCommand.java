package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.tasks.CircularParticlesTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SidebarCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player user = (Player) sender;
        summonGold(user.getLocation());
        return true;
    }

    public void summonGold (Location loc) {

        ArmorStand armorStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.COMMAND,
                stand -> {
                    stand.setInvulnerable(true);
                });
        armorStand.setInvisible(true);
        AttributeInstance scaleAttr = armorStand.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(0.001);
        }

        // Create block display
        BlockDisplay goldBlock = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY, CreatureSpawnEvent.SpawnReason.COMMAND,
                entity -> {
                    entity.setInvulnerable(true);
                });

        goldBlock.setBlock(Bukkit.createBlockData(Material.GOLD_BLOCK));
        goldBlock.setTransformation(
                new Transformation(new Vector3f(-0.4f, 1.2f, -0.4f), new Quaternionf(0, 0, 0, 1),
                        new Vector3f(0.8f, 0.8f, 0.8f), new Quaternionf(0, 0, 0, 1))
        );
        goldBlock.setGlowColorOverride(Color.fromRGB(255, 139, 22));
        goldBlock.setGlowing(true);

        armorStand.addPassenger(goldBlock);

    }
}
