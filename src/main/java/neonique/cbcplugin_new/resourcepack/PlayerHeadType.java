package neonique.cbcplugin_new.resourcepack;

public enum PlayerHeadType {

    NORMAL (57344), TRANSPARENT (57408),
    DOWN_24_NORMAL (57472), DOWN_24_TRANSPARENT (57536);

    private final int startingUnicodeId;

    PlayerHeadType (int startingUnicodeId) {
        this.startingUnicodeId = startingUnicodeId;
    }

    public int getStartingUnicodeId() {
        return startingUnicodeId;
    }

}
