package levigibson.fto3phase;

public class FtoCoord {
    private static boolean initialized;

    public static int[][][] PHASE_ONE_EDGE_LOCATION_MOVES;
    public static int[][][] PHASE_ONE_EDGE_PERMUTATION_MOVES;
    public static int[][] PHASE_ONE_TRIANGLE_MOVES;

    private static synchronized void initPhaseOneEdgeMoves(){
        PHASE_ONE_EDGE_LOCATION_MOVES = new int[220][6][16];
        PHASE_ONE_EDGE_PERMUTATION_MOVES = new int[220][6][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int loc = 0; loc < 220; loc++) {
            for (int perm = 0; perm < 6; perm++) {
                fto.setPhaseOneEdges(loc, perm);
                int[] locMoves = PHASE_ONE_EDGE_LOCATION_MOVES[loc][perm];
                int[] permMoves = PHASE_ONE_EDGE_PERMUTATION_MOVES[loc][perm];

                for (int move = 0; move < 16; move++) {
                    fto.turn(move, turned);
                    locMoves[move] = turned.packPhaseOneEdgeLocations();
                    permMoves[move] = turned.packPhaseOneEdgePermutation();
                }
            }
        }
    }

    private static synchronized void initPhaseOneTriangleMoves(){
        PHASE_ONE_TRIANGLE_MOVES = new int[220][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 220; idx++) {
            fto.setPhaseOneTriangles(idx);
            int[] moves = PHASE_ONE_TRIANGLE_MOVES[idx];

            for (int move = 0; move < 16; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packPhaseOneTriangles();
            }
        }

    }

    public static synchronized void init(){
        if (initialized) return;

        initPhaseOneEdgeMoves();
        initPhaseOneTriangleMoves();

        initialized = true;
    }

}
