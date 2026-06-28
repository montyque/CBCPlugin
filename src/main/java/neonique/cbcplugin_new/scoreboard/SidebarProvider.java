package neonique.cbcplugin_new.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;


public interface SidebarProvider {

    Component getSidebarHeader ();
    List<Component> getClientDisplay (Player client);

}
