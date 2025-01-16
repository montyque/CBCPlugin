package neonique.cbcplugin_new.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.normalText;

public class TextUtil {

    private static final HashMap<Character, Integer> charLengths;
    private static final int defaultCharLength;

    // Special space lengths
    private static final HashMap<Integer, String> lengthsForSpaces;

    static {

        // Set character lengths
        defaultCharLength = 5;

        charLengths = new HashMap<>();
        charLengths.put('I', 3);
        charLengths.put('f', 4);
        charLengths.put('i', 1);
        charLengths.put('k', 4);
        charLengths.put('l', 2);
        charLengths.put('t', 3);
        charLengths.put(' ', 3);
        charLengths.put(':', 1);
        charLengths.put('!', 1);

        lengthsForSpaces = new HashMap<>();
        lengthsForSpaces.put(1, "\uF821");
        lengthsForSpaces.put(2, "\uF822");
        lengthsForSpaces.put(3, "\uF823");
        lengthsForSpaces.put(4, "\uF824");
        lengthsForSpaces.put(5, "\uF825");
        lengthsForSpaces.put(6, "\uF826");
        lengthsForSpaces.put(7, "\uF827");
        lengthsForSpaces.put(8, "\uF828");
        lengthsForSpaces.put(16, "\uF829");
        lengthsForSpaces.put(32, "\uF82A");
        lengthsForSpaces.put(64, "\uF82B");
        lengthsForSpaces.put(128, "\uF82C");
        lengthsForSpaces.put(256, "\uF82D");
        lengthsForSpaces.put(512, "\uF82E");
        lengthsForSpaces.put(1024, "\uF82F");
    }

    public static int getPixelLengthOfText (String string) {

        int length = 0;
        int i = 0;

        // Convert string to `char[]` array
        char[] chars = string.toCharArray();

        // Iterate over `char[]` array using enhanced for-loop
        for (char ch : chars) {
            // Check if character has a length different
            length += charLengths.getOrDefault(ch, defaultCharLength);
            i++;

            // If not the last character, account for the 1 pixel width space in between characters
            if (i != chars.length) {
                length++;
            }
        }
        return length;
    }

    public static String getSpaceOfLength (int length) {

        int currentLength = length;
        StringBuilder stringBuilder = new StringBuilder();
        Integer[] possibleLengths = lengthsForSpaces.keySet().toArray(new Integer[0]);
        Arrays.sort(possibleLengths, Collections.reverseOrder());

        while (currentLength > 0) {
            for (Integer spaceLength : possibleLengths) {
                if (currentLength >= spaceLength) {
                    // Add string to string builder
                    stringBuilder.append(lengthsForSpaces.get(spaceLength));
                    currentLength -= spaceLength;
                    break;
                }
            }
        }

        stringBuilder.append("\uF801");

        return stringBuilder.toString();
    }

    public static Component getComponentSpaceOfLength (int length) {

        return normalText(getSpaceOfLength(length)).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .color(NamedTextColor.WHITE);

    }

    public static Component blankComponent () {
        return Component.text("");
    }

    public static String timerToText(int timer) {
        return String.format("%d:%02d", timer / 60, timer % 60);
    }

    public static Title.Times titleTimes (int fadeInMilli, int stayMilli, int fadeOutMilli) {
        return Title.Times.times(
                Duration.ofMillis(fadeInMilli),
                Duration.ofMillis(stayMilli),
                Duration.ofMillis(fadeOutMilli)
        );
    }
}
