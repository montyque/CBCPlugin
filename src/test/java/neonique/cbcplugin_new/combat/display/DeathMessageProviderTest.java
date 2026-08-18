package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.combat.DeathCause;
import org.bukkit.configuration.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static neonique.cbcplugin_new.testutil.TestUtil.configFromString;

public class DeathMessageProviderTest {

    @Test
    public void testDeathCauses () {

        String configString = """
                CREEPER:
                    direct:
                        d1:
                            middle: ' was blown to a bits by a creeper fired by '
                    indirect:
                        i1:
                            middle: ' blew up while fighting '
                    self:
                        s1:
                            middle: ' used a creeper to self-destruct'
                FLAMEZONE:
                    direct:
                        d1:
                            middle: ' was lit on fire by '
                """;

        Configuration config = configFromString(configString);
        DeathMessageProvider provider = DeathMessageProvider.fromConfig(config);

        assertAll(
                () -> assertTrue(provider.generators().containsKey(DeathCause.CREEPER)),
                () -> assertTrue(provider.generators().containsKey(DeathCause.FLAMEZONE))
        );

    }

}
