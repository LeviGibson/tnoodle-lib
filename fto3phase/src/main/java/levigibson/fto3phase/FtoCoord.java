package levigibson.fto3phase;

public class FtoCoord {
    private static boolean initialized;

    public static int[][][] PHASE_ONE_EDGE_LOCATION_MOVES;

    private static synchronized void initPhaseOneEdgeLocations(){
        PHASE_ONE_EDGE_LOCATION_MOVES = new int[220][6][16];
    }

    public static synchronized void init(){
        if (initialized) return;
        initialized = true;

        initPhaseOneEdgeLocations();
    }

}
