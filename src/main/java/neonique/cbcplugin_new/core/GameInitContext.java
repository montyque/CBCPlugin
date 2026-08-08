package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public record GameInitContext (Plugin plugin, CBCScoreboardManager scoreboardManager, World world) {}
