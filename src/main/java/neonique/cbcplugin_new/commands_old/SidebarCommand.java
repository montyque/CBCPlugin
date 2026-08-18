package neonique.cbcplugin_new.commands_old;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

        // Create block display
        BlockDisplay goldBlock = (BlockDisplay) loc.getWorld().spawnEntity(loc, EntityType.BLOCK_DISPLAY, CreatureSpawnEvent.SpawnReason.COMMAND,
                entity -> {
                    entity.setInvulnerable(true);
                });

        goldBlock.setBlock(Bukkit.createBlockData(Material.GOLD_BLOCK));
        goldBlock.setTransformation(
                new Transformation(new Vector3f(-0.4f, 0f, 0f), new Quaternionf(0, 0, 0, 1),
                        new Vector3f(0.8f, 0.8f, 0.8f), new Quaternionf(0, 0, 0, 1))
        );
        goldBlock.setGlowColorOverride(Color.fromRGB(255, 139, 22));
        goldBlock.setGlowing(true);

    }
}
