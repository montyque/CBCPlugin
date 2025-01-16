package neonique.cbcplugin_new.util;

import neonique.cbcplugin_new.gamemodes._base.PlayerStatObject;

import java.util.Comparator;
import java.util.List;

public class StatsUtil {

    public static List<PlayerStatObject> sortPlayerStatList (List<PlayerStatObject> list, boolean descending) {

        // Sort list
        if (descending) {
            list.sort(Comparator.comparingInt(PlayerStatObject::getValue).reversed());
        }
        else {
            list.sort(Comparator.comparingInt(PlayerStatObject::getValue));
        }

        // Set placements for statistics
        int currentValue = 0;
        int placement = 0;
        int i = 0;

        for (PlayerStatObject player : list) {

            boolean tied = false;

            boolean newPlacement;

            if (descending) {
                newPlacement = player.getValue() < currentValue;
            }
            else {
                newPlacement = player.getValue() > currentValue;
            }

            if (newPlacement || i == 0) {
                placement = i + 1;
                currentValue = player.getValue();
                if (list.size() - 1 != i) {
                    if (list.get(i + 1).getValue() == currentValue) {
                        tied = true;
                    }
                }
            }
            else if (currentValue == player.getValue()) {
                tied = true;
            }

            player.setPlacement(placement, tied);

            i++;

        }

        return list;
    }

}
