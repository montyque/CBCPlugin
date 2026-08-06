package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MapMechanicsManager {

    private final PlayerStore players;

    private final List<MapMechanic> activeMechanics = new ArrayList<>();

    public MapMechanicsManager (PlayerStore players) {
        this.players = players;
    }

    public void setupMapMechanics (CBCMap map) {

        World world = map.getWorld();
        List<MapMechanic> mechanics = map.mechanicSpecs().stream()
                .map(m -> m.createMechanic(world))
                .toList();
        mechanics.forEach(this::register);

    }

    /*public void setupMapMechanics (CBCMap map) {

        VoidMechanic voidMechanic = new VoidMechanic(map.getMapCentre(), map.getVoidPlane());
        register(voidMechanic);

        HealthPadMechanic healthPadMechanic = new HealthPadMechanic(map.getHealthPads());
        register(healthPadMechanic);

        JumpPadMechanic jumpPadMechanic = new JumpPadMechanic(map.getJumpPads());
        register(jumpPadMechanic);

        DashPadMechanic dashPadMechanic = new DashPadMechanic(map.getDashPads());
        register(dashPadMechanic);

        if (map.isSwimTimerEnabled()) {
            SwimTimerMechanic swimTimerMechanic = new SwimTimerMechanic(map.getSwimTimerLength());
            register(swimTimerMechanic);
        }

        if (map.isInstaKillLava()) {
            LavaKillMechanic lavaKillMechanic = new LavaKillMechanic();
            register(lavaKillMechanic);
        }

    }*/

    public void register (MapMechanic mechanic) {
        activeMechanics.add(mechanic);
        mechanic.activate(players);
    }

    public void unregisterAll () {
        activeMechanics.forEach(MapMechanic::deactivate);
        activeMechanics.clear();
    }

    @SuppressWarnings("unchecked")
    public <T extends MapMechanic> Collection<T> getMechanicsOfType (Class<T> mechanicType) {
        return activeMechanics.stream()
                .filter(mechanicType::isInstance)
                .map(m -> (T) m)
                .collect(Collectors.toList());
    }


}
