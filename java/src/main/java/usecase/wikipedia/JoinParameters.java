package usecase.wikipedia;

import usecase.ExperimentID;

public class JoinParameters {

    public static long getWA(ExperimentID id) {
        switch (id) {
            case LLJ:
                return 1000;
            case ALJ:
                return 1000;
            case HLJ:
                return 1000;
            case LHJ:
                return 1000;
            case AHJ:
                return 1000;
            case HHJ:
                return 1000;
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static long getWS(ExperimentID id) {
        switch (id) {
            case LLJ:
                return 3000;
            case ALJ:
                return 3000;
            case HLJ:
                return 3000;
            case LHJ:
                return 10000;
            case AHJ:
                return 10000;
            case HHJ:
                return 10000;
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

    public static long getMinLength(ExperimentID id) {
        switch (id) {
            case LLJ:
                return 210;
            case ALJ:
                return 150;
            case HLJ:
                return 100;
            case LHJ:
                return 210;
            case AHJ:
                return 150;
            case HHJ:
                return 100;
            default:
                throw new RuntimeException("Unrecognized experiment id");
        }
    }

}
