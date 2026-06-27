package levigibson.fto3phase;

public class FtoCoord {
    private static boolean initialized;


    //-------------- PHASE ONE --------------//

    private static int[][] PHASE_ONE_EDGE_MOVES;
    private static int[][] PHASE_ONE_TRIANGLE_MOVES;

    public static int turnG1Edges(int idx, int move){
        if (!initialized) throw new IllegalStateException("Can not turn the cube when its not initialized");
        return PHASE_ONE_EDGE_MOVES[idx][move];
    }

    public static int turnG1Triangles(int idx, int move){
        if (!initialized) throw new IllegalStateException("Can not turn the cube when its not initialized");
        return PHASE_ONE_TRIANGLE_MOVES[idx][move];
    }

    public static boolean isPhaseOne(int edgeIdx, int triIdx){
        return triIdx == 219 && (edgeIdx == 1314 || edgeIdx == 1318 || edgeIdx == 1317);
    }

    private static synchronized void initPhaseOneEdgeMoves(){
        PHASE_ONE_EDGE_MOVES = new int[220 * 6][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 220 * 6; idx++) {
                fto.setPhaseOneEdges(idx);
                int[] locMoves = PHASE_ONE_EDGE_MOVES[idx];

                for (int move = 0; move < 16; move++) {
                    fto.turn(move, turned);
                    locMoves[move] = turned.packPhaseOneEdges();
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
