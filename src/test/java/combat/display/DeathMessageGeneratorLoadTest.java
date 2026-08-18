package combat.display;

import neonique.cbcplugin_new.combat.display.DeathMessage;
import neonique.cbcplugin_new.combat.display.DeathMessageGenerator;
import org.bukkit.configuration.Configuration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static testutil.TestUtil.configFromString;

public class DeathMessageGeneratorLoadTest {

    @Test
    public void testLoadingList () {

        String configString = """
                direct:
                    d1:
                        middle: ' was blown to a bits by a creeper fired by '
                    d2:
                        middle: ' disintegrated due to a creeper fired by '
                    d3:
                        middle: ' went kaboom due to a creeper fired by '
                """;
        Configuration config = configFromString(configString);

        List<DeathMessage> messages = DeathMessageGenerator.listFromConfig(config, "direct");

        assertEquals(new DeathMessage(" was blown to a bits by a creeper fired by ", ""), messages.get(0));
        assertEquals(new DeathMessage(" disintegrated due to a creeper fired by ", ""), messages.get(1));
        assertEquals(new DeathMessage(" went kaboom due to a creeper fired by ", ""), messages.get(2));

    }

    @Test
    public void testLoadingAll () {

        String configString = """
                direct:
                    d1:
                        middle: ' was blown to a bits by a creeper fired by '
                indirect:
                    i1:
                        middle: ' blew up while fighting '
                self:
                    s1:
                        middle: ' used a creeper to self-destruct'
                """;
        Configuration config = configFromString(configString);

        DeathMessageGenerator gen = DeathMessageGenerator.fromConfig(config);

        assertEquals(List.of(new DeathMessage(" was blown to a bits by a creeper fired by ", "")), gen.direct());
        assertEquals(List.of(new DeathMessage(" blew up while fighting ", "")), gen.indirect());
        assertEquals(List.of(new DeathMessage(" used a creeper to self-destruct", "")), gen.self());

    }

}
