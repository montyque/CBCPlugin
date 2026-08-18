package combat.display;

import neonique.cbcplugin_new.combat.display.DeathMessage;
import org.bukkit.configuration.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static testutil.TestUtil.configFromString;

public class DeathMessageLoadTest {

    @Nested
    class SuccessTests {

        @Test
        @DisplayName("Tests an empty config to check for default death messages")
        public void testEmpty () {

            Configuration config = configFromString("");

            DeathMessage result = DeathMessage.fromConfig(config);

            assertEquals(" was killed by ", result.middle());
            assertEquals("", result.after());

        }

        @Test
        @DisplayName("Tests a config only specifying middle")
        public void testMiddle () {

            Configuration config = configFromString("middle: ' was blown to a bits by a creeper fired by '");

            DeathMessage result = DeathMessage.fromConfig(config);

            assertEquals(" was blown to a bits by a creeper fired by ", result.middle());
            assertEquals("", result.after());

        }

        @Test
        @DisplayName("Tests a config specifying both middle and after")
        public void testFull () {

            String configString = """
                    middle: ' suffered death to '
                    after: '''s aim'
                    """;
            Configuration config = configFromString(configString);

            DeathMessage result = DeathMessage.fromConfig(config);

            assertEquals(" suffered death to ", result.middle());
            assertEquals("'s aim", result.after());

        }

    }


}
