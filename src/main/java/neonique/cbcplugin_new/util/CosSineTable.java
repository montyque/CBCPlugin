package neonique.cbcplugin_new.util;

public class CosSineTable {

    double[] cos = new double[361];
    double[] sin = new double[361];
    private static final CosSineTable table = new CosSineTable();

    private CosSineTable() {
        for (int i = 0; i <= 360; i++) {
            cos[i] = Math.cos(Math.toRadians(i));
            sin[i] = Math.sin(Math.toRadians(i));
        }
    }

    public double getSine(int angle) {
        int angleCircle = angle % 360;
        return sin[angleCircle];
    }

    public double getCos(int angle) {
        int angleCircle = angle % 360;
        return cos[angleCircle];
    }

    public static CosSineTable getTable() {
        return table;
    }

}