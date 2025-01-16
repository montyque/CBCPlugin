package neonique.cbcplugin_new.enums;

public enum EasingType {

    QUAD_IN {
        @Override
        public double getProgress(double x) {
            return 1 - (1 - x) * (1 - x);
        }
    },
    QUAD_OUT {
        @Override
        public double getProgress(double x) {
            return x * x;
        }
    },
    QUAD_IN_OUT {
        @Override
        public double getProgress(double x) {
            return x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2;
        }
    },
    CUBIC_IN {
        @Override
        public double getProgress(double x) {
            return x * x * x;
        }
    },
    CUBIC_OUT {
        @Override
        public double getProgress(double x) {
            return 1 - Math.pow(1 - x, 3);
        }
    },
    CUBIC_IN_OUT {
        @Override
        public double getProgress(double x) {
            return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
        }
    };

    public abstract double getProgress(double x);

}
