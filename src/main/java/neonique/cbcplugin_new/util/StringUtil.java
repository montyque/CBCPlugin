package neonique.cbcplugin_new.util;

import java.util.HashMap;

public class StringUtil {

    private StringUtil () {}

    public static String getPlacementString(int placement) {
        if (placement % 10 > 0 && placement % 10 < 4) {
            if ((placement / 10) % 10 != 1) {
                if (placement == 1) {
                    return placement + "st";
                }
                else if (placement == 2) {
                    return placement + "nd";
                }
                else if (placement == 3) {
                    return placement + "rd";
                }
            }
        }
        return placement + "th";
    }

    public static String firstLetterUpper (String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String checkPlural (String formatString, int number) {
        return String.format(formatString, number, number == 1 ? "" : "s");
    }

}
