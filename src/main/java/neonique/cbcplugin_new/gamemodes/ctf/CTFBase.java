package neonique.cbcplugin_new.gamemodes.ctf;

import org.bukkit.Location;

import java.util.List;

public record CTFBase (Location flagLocation, List<Location> spawns) {}