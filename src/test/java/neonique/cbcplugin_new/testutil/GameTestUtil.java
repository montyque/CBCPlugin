package neonique.cbcplugin_new.testutil;

import neonique.cbcplugin_new.core.PlayerLike;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.TeamLike;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameTestUtil {

    public static List<? extends PlayerLike> mockPlayerLikes (Collection<PlayerMock> entities) {
        return entities.stream()
                .map(MockPlayerLike::new)
                .toList();
    }

    public static List<? extends TeamLike> mockTeamLikes (Map<TeamColor, Collection<PlayerMock>> teamPlayers) {
        return teamPlayers.entrySet().stream()
                .map(e -> new MockTeamLike(
                        e.getKey().name(),
                        e.getKey().name(),
                        e.getKey(),
                        mockPlayerLikes(e.getValue())))
                .toList();
    }

}

class MockPlayerLike implements PlayerLike {

    private final UUID uuid;

    public MockPlayerLike (PlayerMock playerMock) {
        this.uuid = playerMock.getUniqueId();
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

}


class MockTeamLike implements TeamLike {

    private final String id;
    private final String name;
    private final TeamColor teamColor;
    private final List<? extends PlayerLike> players;

    public MockTeamLike (String id, String name, TeamColor teamColor, Collection<? extends PlayerLike> players) {
        this.id = id;
        this.name = name;
        this.teamColor = teamColor;
        this.players = List.copyOf(players);
    }


    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public TeamColor teamColor() {
        return teamColor;
    }

    @Override
    public Collection<? extends PlayerLike> players() {
        return players;
    }

}