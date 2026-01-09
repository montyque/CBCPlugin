package neonique.cbcplugin_new.enums;

public enum TeamColor {

    RED (0),
    BLUE (1),
    GREEN (2),
    YELLOW (3),
    CYAN (4),
    ORANGE (5),
    MAGENTA (6),
    PURPLE (7);

    private final int rpNumber;

    TeamColor(int rpNumber) {
        this.rpNumber = rpNumber;
    }

    public int getRpNumber() {
        return rpNumber;
    }
}
