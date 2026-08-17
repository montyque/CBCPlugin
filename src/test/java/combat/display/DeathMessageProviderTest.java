package combat.display;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.combat.display.DeathMessage;
import neonique.cbcplugin_new.combat.display.DeathMessageGenerator;
import neonique.cbcplugin_new.combat.display.DeathMessageProvider;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    public static Configuration configFromString (String content) {
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );
    }

}
