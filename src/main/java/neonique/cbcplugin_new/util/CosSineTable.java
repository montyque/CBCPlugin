package neonique.cbcplugin_new.util;

public class CosSineTable {

    private final double[] sines;
    private final double[] cosines;
    private final int size;

    public CosSineTable(int size) {

        this.size = size;
        cosines = new double[size];
        sines = new double[size];

        for (int i = 0; i < size; i++) {
            double degree = (Math.PI * 2) * ((double) i / (double) size);
            cosines[i] = Math.cos(degree);
            sines[i] = Math.sin(degree);
        }

    }

    public double getSin(int index) {
        return sines[index % size];
    }

    public double getCos(int index) {
        return cosines[index % size];
    }

}