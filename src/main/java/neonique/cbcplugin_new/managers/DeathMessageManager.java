package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.enums.DeathCauses;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.getDeathCauseIcon;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class DeathMessageManager {


    // List of possible death messages
    ArrayList<TextComponent> directCreeperMessages = new ArrayList<>();
    ArrayList<TextComponent> directXbowMessages = new ArrayList<>();
    ArrayList<TextComponent> directFlameMessages = new ArrayList<>();
    ArrayList<TextComponent> directMeleeMessages = new ArrayList<>();
    ArrayList<TextComponent> indirectVoidMessages = new ArrayList<>(); // When a player falls into the void after attacked
    ArrayList<TextComponent> indirectOwnVoidMessages = new ArrayList<>(); // When a player falls into the void without being attacked
    ArrayList<TextComponent> indirectCreeperMessages = new ArrayList<>(); // When a player dies to a creeper that is allied
    ArrayList<TextComponent> indirectOwnCreeperMessages = new ArrayList<>(); // When a player dies to a creeper that is theirs
    ArrayList<TextComponent> indirectOwnQuitMessages = new ArrayList<>(); // When a player disconnects
    ArrayList<TextComponent> indirectQuitMessages = new ArrayList<>(); // When a player disconnects while fighting someone
    ArrayList<TextComponent> indirectOwnLeavePracticeMessages = new ArrayList<>();
    ArrayList<TextComponent> indirectLeavePracticeMessages = new ArrayList<>();

    ArrayList<TextComponent> showdownBorderMessages = new ArrayList<>(); // When a player dies to the showdown border while fighting someone
    ArrayList<TextComponent> showdownOwnBorderMessages = new ArrayList<>(); // When a player dies to the showdown border

    ArrayList<TextComponent> drownMessages = new ArrayList<>(); // When a player dies to the showdown border while fighting someone
    ArrayList<TextComponent> drownOwnMessages = new ArrayList<>(); // When a player dies to the showdown border

    ArrayList<TextComponent> lavaMessages = new ArrayList<>(); // When a player dies to lava instakill while fighting someone
    ArrayList<TextComponent> lavaOwnMessages = new ArrayList<>(); // When a player dies to lava instakill

    ArrayList<TextComponent> commandMessages = new ArrayList<>(); // When a player dies to command while fighting someone
    ArrayList<TextComponent> commandOwnMessages = new ArrayList<>(); // When a player dies to command

    // PIGLIN KILLS
    ArrayList<TextComponent> piglinXbowKills = new ArrayList<>();

    // NATURAL CAUSE KILLS
    ArrayList<TextComponent> naturalMessages = new ArrayList<>();
    ArrayList<TextComponent> naturalOwnMessages = new ArrayList<>();

    public TextComponent writeDeathM (String before, String between, String after) {
        return Component.text()
                .content(before).color(NamedTextColor.GRAY)
                .append(Component.text().content("playerKilled").color(NamedTextColor.WHITE))
                .append(Component.text().content(between).color(NamedTextColor.GRAY))
                .append(Component.text().content("playerKiller").color(NamedTextColor.WHITE))
                .append(Component.text().content(after).color(NamedTextColor.GRAY)).build();
    }

    public TextComponent writeOwnDeathM (String before, String after) {
        return Component.text()
                .content(before).color(NamedTextColor.GRAY)
                .append(Component.text().content("playerKilled").color(NamedTextColor.WHITE))
                .append(Component.text().content(after).color(NamedTextColor.GRAY)).build();
    }

    public DeathMessageManager() {

        // ****************************************
        // DIRECT CREEPER DEATH MESSAGES
        directCreeperMessages.add(writeDeathM("", " was blown to bits by a creeper fired by ", ""));
        directCreeperMessages.add(writeDeathM("", " disintegrated due to a creeper fired by ", ""));
        directCreeperMessages.add(writeDeathM("", " went kaboom due to a creeper fired by ", ""));
        directCreeperMessages.add(writeDeathM("", " was blown up at the hands of ", ""));
        directCreeperMessages.add(writeDeathM("", " was obliterated by a creeper launched by ", ""));
        directCreeperMessages.add(writeDeathM("", " was sent packing by a creeper fired by ", ""));
        directCreeperMessages.add(writeDeathM("", " was destroyed by a creeper launched by ", ""));

        // ****************************************
        // DIRECT X-BOW MESSAGES
        directXbowMessages.add(writeDeathM("", " was pierced by an arrow fired by ", ""));
        directXbowMessages.add(writeDeathM("", " was sniped by ", ""));
        directXbowMessages.add(writeDeathM("", " suffered death to ", "'s aim"));
        directXbowMessages.add(writeDeathM("", " Achilles' heel was shot by ", ""));
        directXbowMessages.add(writeDeathM("", " took a direct arrow hit from ", ""));
        directXbowMessages.add(writeDeathM("", " lost the game to ", "'s arrow"));
        directXbowMessages.add(writeDeathM("", " tried to block ", "'s X-Bow with their face"));
        directXbowMessages.add(writeDeathM("", " was turned into dust by an arrow launched by ", ""));

        piglinXbowKills.add(writeOwnDeathM("", " was pierced by an arrow fired by a pig with a crossbow"));
        piglinXbowKills.add(writeOwnDeathM("", " was sniped by a Piglin"));
        piglinXbowKills.add(writeOwnDeathM("", " took a direct arrow hit from a pig with a crossbow"));

        // ****************************************
        // DIRECT FLAME MESSAGES
        directFlameMessages.add(writeDeathM("", " was lit on fire by ", ""));
        directFlameMessages.add(writeDeathM("", " was turned into ashes by ", ""));
        directFlameMessages.add(writeDeathM("", " burned to a crisp at the hands of ", ""));
        directFlameMessages.add(writeDeathM("", " was roasted by ", ""));
        directFlameMessages.add(writeDeathM("", " was set alight by ", ""));
        directFlameMessages.add(writeDeathM("", " was incinerated at the hands of ", ""));
        directFlameMessages.add(writeDeathM("", " was burned to the ground by ", ""));

        // ****************************************
        // DIRECT MELEE MESSAGES
        directMeleeMessages.add(writeDeathM("", " lost to ", " in the boxing ring"));
        directMeleeMessages.add(writeDeathM("", " was punched to death by ", ""));
        directMeleeMessages.add(writeDeathM("", " took a hard left hook from ", ""));
        directMeleeMessages.add(writeDeathM("", " took a punch to the face from ", ""));
        directMeleeMessages.add(writeDeathM("", " got struck with the fist of ", ""));

        // ****************************************
        // INDIRECT VOID MESSAGES
        indirectVoidMessages.add(writeDeathM("", " jumped into the void while fighting ", ""));
        indirectVoidMessages.add(writeDeathM("", " disappeared into the abyss while fighting ", ""));
        indirectVoidMessages.add(writeDeathM("", " didn't want to live in the same world as ", ""));
        indirectVoidMessages.add(writeDeathM("", " discovered there was nothing below them while fighting ", ""));
        indirectVoidMessages.add(writeDeathM("", " jumped into the void while fighting ", ""));
        indirectVoidMessages.add(writeDeathM("", "'s controller disconnected while fighting ", ""));

        // ****************************************
        // INDIRECT OWN VOID MESSAGES
        indirectOwnVoidMessages.add(writeOwnDeathM("", " jumped into the void"));
        indirectOwnVoidMessages.add(writeOwnDeathM("", " disappeared into the abyss"));
        indirectOwnVoidMessages.add(writeOwnDeathM("", " didn't want to live anymore, apparently"));
        indirectOwnVoidMessages.add(writeOwnDeathM("", " discovered there was nothing below them"));
        indirectOwnVoidMessages.add(writeOwnDeathM("", " jumped into the void"));
        indirectOwnVoidMessages.add(writeOwnDeathM("", "'s controller ran out of battery"));

        // ****************************************
        // INDIRECT CREEPER MESSAGES
        indirectCreeperMessages.add(writeDeathM("", " blew up while fighting ", ""));
        indirectCreeperMessages.add(writeDeathM("", " faced a creeper's wrath while fighting ", ""));
        indirectCreeperMessages.add(writeDeathM("", " was blown to smithereens while fighting ", ""));
        indirectCreeperMessages.add(writeDeathM("", " was blown up by a creeper while fighting ", ""));
        indirectCreeperMessages.add(writeDeathM("", " was caught in a creeper explosion while fighting ", ""));

        // ****************************************
        // INDIRECT OWN CREEPER MESSAGES
        indirectOwnCreeperMessages.add(writeOwnDeathM("",
                " was blown up into smithereens and sung their tiny symphony, all up and down a city street while " +
                        "trying to put their mind at ease like finishing this melody it feels like a necessity so this could " +
                        "be the death of them or maybe just a better them"));

        indirectQuitMessages.add(writeDeathM("", " disconnected whilst fighting ", ""));
        indirectOwnQuitMessages.add(writeOwnDeathM("", " died due to disconnecting from the game"));

        indirectLeavePracticeMessages.add(writeDeathM("", " left the practice arena whilst fighting ", ""));
        indirectOwnLeavePracticeMessages.add(writeOwnDeathM("", " left the practice arena"));

        showdownBorderMessages.add(writeDeathM("", " was engulfed by the sudden death border whilst fighting ", ""));
        showdownOwnBorderMessages.add(writeOwnDeathM("", " was engulfed by the sudden death border"));

        drownMessages.add(writeDeathM("", " drowned to death whilst fighting ", ""));
        drownMessages.add(writeDeathM("", " suffered a watery grave whilst fighting ", ""));
        drownMessages.add(writeDeathM("", " was engulfed by the waves whilst fighting ", ""));
        drownOwnMessages.add(writeOwnDeathM("", " drowned to death"));
        drownOwnMessages.add(writeOwnDeathM("", " suffered a watery grave"));
        drownOwnMessages.add(writeOwnDeathM("", " was engulfed by the waves"));

        lavaMessages.add(writeDeathM("", " burned during a once-in-a-lifetime swim lesson whilst fighting ", ""));
        lavaMessages.add(writeDeathM("", " was incinerated by lava whilst fighting ", ""));
        lavaMessages.add(writeDeathM("", " tried to swim in lava whilst fighting ", ""));
        lavaMessages.add(writeDeathM("", " received third degree lava burns at the hands of ", ""));
        lavaOwnMessages.add(writeOwnDeathM("", " burned during a once-in-a-lifetime swim lesson"));
        lavaOwnMessages.add(writeOwnDeathM("", " was incinerated by lava"));
        lavaOwnMessages.add(writeOwnDeathM("", " tried to swim in lava"));
        lavaOwnMessages.add(writeOwnDeathM("", " received third degree lava burns"));

        commandMessages.add(writeDeathM("", " was struck down by God whilst fighting ", ""));
        commandMessages.add(writeDeathM("", " was artificially executed whilst fighting ", ""));
        commandOwnMessages.add(writeOwnDeathM("", " was struck down by God"));
        commandOwnMessages.add(writeOwnDeathM("", " was artificially executed"));

        naturalMessages.add(writeDeathM("", " died of natural causes whilst fighting ", ""));
        naturalOwnMessages.add(writeOwnDeathM("", " died of natural causes"));

    }

    public Component getDeathMessage(CBCPlayer playerKilled, CBCPlayer playerKiller, DeathCauses cause, boolean direct) {

        ArrayList<TextComponent> listToUse = null;

        if (cause == DeathCauses.CREEPER) {
            if (direct) {
                listToUse = directCreeperMessages;}
            else {
                if (playerKiller == null) {listToUse = indirectOwnCreeperMessages;}
                else {listToUse = indirectCreeperMessages;}
            }
        }
        else if (cause == DeathCauses.FLAMEZONE) {listToUse = directFlameMessages;}
        else if (cause == DeathCauses.XBOW) {listToUse = directXbowMessages;}
        else if (cause == DeathCauses.MELEE) {listToUse = directMeleeMessages;}
        else if (cause == DeathCauses.VOID) {
            if (playerKiller == null) {
                listToUse = indirectOwnVoidMessages;
            } else {
                listToUse = indirectVoidMessages;
            }
        }
        else if (cause == DeathCauses.DISCONNECT) {
            if (playerKiller == null) {
                listToUse = indirectOwnQuitMessages;
            } else {
                listToUse = indirectQuitMessages;
            }
        }
        else if (cause == DeathCauses.SHOWDOWN_BORDER) {
            if (playerKiller == null) {
                listToUse = showdownOwnBorderMessages;
            } else {
                listToUse = showdownBorderMessages;
            }
        }
        else if (cause == DeathCauses.DROWN) {
            if (playerKiller == null) {
                listToUse = drownOwnMessages;
            } else {
                listToUse = drownMessages;
            }
        }
        else if (cause == DeathCauses.LEAVE_PRACTICE) {
            if (playerKiller == null) {
                listToUse = indirectOwnLeavePracticeMessages;
            } else {
                listToUse = indirectLeavePracticeMessages;
            }
        }
        else if (cause == DeathCauses.LAVA) {
            if (playerKiller == null) {
                listToUse = lavaOwnMessages;
            } else {
                listToUse = lavaMessages;
            }
        }
        else if (cause == DeathCauses.COMMAND) {
            if (playerKiller == null) {
                listToUse = commandOwnMessages;
            } else {
                listToUse = commandMessages;
            }
        }
        else if (cause == DeathCauses.XBOW_PIGLIN) {
            listToUse = piglinXbowKills;
        }
        else if (cause == DeathCauses.NATURAL) {
            if (playerKiller == null) {
                listToUse = naturalOwnMessages;
            } else {
                listToUse = naturalMessages;
            }
        }

        if (listToUse == null) {
            return null;
        }

        // Get random element from list
        Random rndm = new Random();
        int rndmNumber = rndm.nextInt(listToUse.size());
        TextComponent deathMessage = listToUse.get(rndmNumber);

        // Change death message names
        List<Component> dmChildren = new ArrayList<>(deathMessage.children());
        if (playerKiller == null) {
            dmChildren.set(0, playerKilled.getNameComponent());
        } else {
            dmChildren.set(0, playerKilled.getNameComponent());
            dmChildren.set(2, playerKiller.getNameComponent());
            // If player has multikill counter then add
            playerKiller.updateMultiKill();
            String multiText = "";
            if (playerKiller.getMultiKill() + 1 == 2) {
                multiText = " DOUBLE KILL!";
            } else if (playerKiller.getMultiKill() + 1 == 3) {
                multiText = " TRIPLE KILL!";
            } else if (playerKiller.getMultiKill() + 1 == 4) {
                multiText = " QUADRA KILL!";
            } else if (playerKiller.getMultiKill() + 1 == 5) {
                multiText = " PENTAKILL!";
            }
            dmChildren.add(smallText(multiText).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        }

        NamedTextColor color = NamedTextColor.WHITE;
        if (playerKilled.getTeam() != null) {
            color = playerKilled.getTeam().getColor();
        }

        Component deathIcon = getDeathCauseIcon(cause, playerKiller != null, color).append(Component.space());

        return deathIcon.append(deathMessage.children(dmChildren));
    }

    public Component getKillStreakMessage(CBCPlayer playerKiller) {

        Component playerComponent = playerKiller.getNameComponent().decorate(TextDecoration.BOLD);

        if (playerKiller.getKillStreak() == 5) {
            return playerComponent.append(Component.text(" is on a killing spree!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 10) {
            return playerComponent.append(Component.text(" is unstoppable!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 15) {
            return playerComponent.append(Component.text(" is dominating!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 20) {
            return playerComponent.append(Component.text(" is legendary!").color(NamedTextColor.WHITE)).decorate(TextDecoration.BOLD);
        }
        return null;
    }

    public Component getKillStreakEndedMessage(CBCPlayer playerKilled, CBCPlayer playerKiller) {

        int killStreak = playerKilled.getKillStreak();

        Component playerKilledComponent = playerKilled.getNameComponent();

        if (playerKiller == null) {
            return playerKilledComponent
                    .append(Component.text("'s kill streak of ").color(NamedTextColor.WHITE))
                    .append(Component.text(killStreak).color(NamedTextColor.GREEN))
                    .append(Component.text(" has ended!").color(NamedTextColor.WHITE))
                    .decorate(TextDecoration.BOLD);
        } else {
            return playerKilledComponent
                    .append(Component.text("'s kill streak of ").color(NamedTextColor.WHITE))
                    .append(Component.text(killStreak).color(NamedTextColor.GREEN))
                    .append(Component.text(" has been ended by ").color(NamedTextColor.WHITE))
                    .append(playerKiller.getNameComponent())
                    .append(Component.text("!").color(NamedTextColor.WHITE))
                    .decorate(TextDecoration.BOLD);
        }
    }
}
