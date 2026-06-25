package levigibson.fto3phase;

public class FtoCoord {
    private static boolean initialized;

    public static int[][][] PHASE_ONE_EDGE_LOCATION_MOVES;
    public static int[][][] PHASE_ONE_EDGE_PERMUTATION_MOVES;

    private static synchronized void initPhaseOneEdgeMoves(){
        PHASE_ONE_EDGE_LOCATION_MOVES = new int[220][6][16];
        PHASE_ONE_EDGE_PERMUTATION_MOVES = new int[220][6][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int loc = 0; loc < 220; loc++) {
            for (int perm = 0; perm < 6; perm++) {
                fto.setG1Edges(loc, perm);
                int[] locMoves = PHASE_ONE_EDGE_LOCATION_MOVES[loc][perm];
                int[] permMoves = PHASE_ONE_EDGE_PERMUTATION_MOVES[loc][perm];

                for (int move = 0; move < 16; move++) {
                    fto.turn(move, turned);
                    locMoves[move] = turned.idxPhaseOneEdgeLocations();
                    permMoves[move] = turned.idxPhaseOneEdgePermutation();
                }
            }
        }
    }

    public static synchronized void init(){
        if (initialized) return;
        initialized = true;

        initPhaseOneEdgeMoves();
    }

}
