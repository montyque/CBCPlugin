package neonique.cbcplugin_new.testutil;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TestUtil {

    private TestUtil() {}

    public static Configuration configFromString (String content) {
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
        );
    }

}
