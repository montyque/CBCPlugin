package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

public class DeathMessageGenerator {

    private final List<DeathMessage> directDeathMessages;
    private final List<DeathMessage> indirectDeathMessages;
    private final List<DeathMessage> indirectOwnDeathMessages;

    private final Random randomGenerator;

    public DeathMessageGenerator (List<String> directStrings, List<String> indirectStrings,
                                  List<String> indirectOwnStrings) {

        // Convert all strings to DeathMessages
        directDeathMessages = stringListToDeathMessages(directStrings);
        indirectDeathMessages = stringListToDeathMessages(indirectStrings);
        indirectOwnDeathMessages = stringListToDeathMessages(indirectOwnStrings);

        randomGenerator = new Random();

    }

    public static HashMap<DeathCause, DeathMessageGenerator> loadDeathMessageGenerators 
            (ConfigurationSection deathMessagesSection) {
        
        HashMap<DeathCause, DeathMessageGenerator> deathMessageGenerators = new HashMap<>();
        
        // Iterate through all keys (named after DeathCause enums) in file
        for (String key : deathMessagesSection.getKeys(false)) {

            ConfigurationSection deathCauseSection = deathMessagesSection.getConfigurationSection(key);
            if (deathCauseSection == null) continue;

            // Check if key matches with a value in the DeathCause enum
            DeathCause deathCause;
            try {
                deathCause = DeathCause.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }

            // Get all death messages for this death cause in string form
            List<String> directStrings = deathCauseSection.getStringList("DIRECT");
            List<String> indirectStrings = deathCauseSection.getStringList("INDIRECT");
            List<String> indirectNoKillerStrings = deathCauseSection.getStringList("INDIRECT_NO_KILLER");

            // Create death message generator and link it to this DeathCause enum
            DeathMessageGenerator dmGen = new DeathMessageGenerator(directStrings, indirectStrings, indirectNoKillerStrings);
            deathMessageGenerators.put(deathCause, dmGen);

        }
        
        return deathMessageGenerators;

    }

    public Component getDeathMessageComponent (CBCPlayer playerKilled, CBCPlayer playerKiller, boolean direct) {

        DeathMessage deathMessage;
        if (playerKiller != null) {
            if (direct) {
                deathMessage = getRandomMessage(directDeathMessages);
            }
            else {
                deathMessage = getRandomMessage(indirectDeathMessages);
            }
        }
        else {
            deathMessage = getRandomMessage(indirectOwnDeathMessages);
        }

        // Convert death message to component
        return deathMessage.getMessageComponent(playerKilled, playerKiller);

    }

    private DeathMessage getRandomMessage (List<DeathMessage> messagesPool) {
        return messagesPool.get(randomGenerator.nextInt(messagesPool.size()));
    }

    private List<DeathMessage> stringListToDeathMessages (List<String> strings) {

        List<DeathMessage> deathMessages = new ArrayList<>();

        for (String string : strings) {
            // Create DeathMessage to store this string
            DeathMessage deathMessage = new DeathMessage(string);
            deathMessages.add(deathMessage);
        }

        return deathMessages;

    }



}
