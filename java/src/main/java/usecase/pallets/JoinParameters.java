package usecase.pallets;

import usecase.ExperimentID;

public class JoinParameters {

    public static long getWA(ExperimentID id) {
        switch (id) {
            case llj:
                return 500;
            case alj:
                return 500;
            case hlj:
                return 500;
            case lhj:
                return 500;
            case ahj:
                return 500;
            case hhj:
                return 500;
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static long getWS(ExperimentID id) {
        switch (id) {
            case llj:
                return 1000;
            case alj:
                return 1000;
            case hlj:
                return 1000;
            case lhj:
                return 2000;
            case ahj:
                return 2000;
            case hhj:
                return 2000;
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static double getMaxAverageDistance(ExperimentID id) {
        switch (id) {
            case llj:
                return 0.5;
            case alj:
                return 0.6;
            case hlj:
                return 0.7;
            case lhj:
                return 0.5;
            case ahj:
                return 0.6;
            case hhj:
                return 0.7;
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

}
