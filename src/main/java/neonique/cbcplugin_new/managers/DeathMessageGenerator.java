package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.kyori.adventure.text.Component;

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
