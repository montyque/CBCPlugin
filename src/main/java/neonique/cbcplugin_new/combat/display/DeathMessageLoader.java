package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.combat.DeathCause;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class DeathMessageLoader {

    private DeathMessageProvider defaultProvider = DeathMessageProvider.empty();

    public void loadDefaults (File dir) throws FileNotFoundException {

        // Get file that has all death messages
        File file = new File(dir, "deathmessages.yml");
        if (!file.exists()) throw new FileNotFoundException();

        YamlConfiguration deathMessagesFile = YamlConfiguration.loadConfiguration(file);
        defaultProvider = DeathMessageProvider.fromConfig(deathMessagesFile);

    }

    public void setDefaultProvider (DeathMessageProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public DeathMessageProvider getOverriddenProvider (DeathMessageProvider overrides) {
        Map<DeathCause, DeathMessageGenerator> generators = new HashMap<>();
        for (DeathCause cause : defaultProvider.generators().keySet()) {
            generators.put(cause, overrides.generators().getOrDefault(cause, defaultProvider.generators().get(cause)));
        }
        return new DeathMessageProvider(generators);
    }



}
