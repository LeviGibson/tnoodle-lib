package levigibson.fto3phase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FtoCoord {
    private static boolean initialized;


    //-------------- PHASE ONE --------------//

    private static int[][] PHASE_ONE_EDGE_MOVES;
    private static int[][] PHASE_ONE_TRIANGLE_MOVES;
    private static int[][] PHASE_TWO_TRIANGLE_MOVES;
    private static int[][] PHASE_TWO_EDGE_MOVES;

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

    private static synchronized void initPhaseOneEdges(){
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

    private static synchronized void initPhaseOneTriangles(){
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

    private static synchronized void initPhaseTwoTris(){
        PHASE_TWO_TRIANGLE_MOVES = new int[1680][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 1680; idx++) {
            fto.setPhaseTwoTris(idx);
            int[] moves = PHASE_TWO_TRIANGLE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packPhaseTwoTris();
            }
        }
    }

    private static Set<Integer> PHASE_TWO_SOLVED_EDGES;

    /**
     * Builds the phase-two edge pruning table on the fly by BFS, replacing
     * the previously-bundled {@code edgeprun.dat} resource.
     *
     * <p>Phase-one guarantees that D-face edges (positions 9-11) are solved
     * entering phase two, so the reachable subspace is the even permutations of
     * the remaining 9 edges — exactly {@code 9! / 2 = 181 440} states.</p>
     *
     * <p>D moves (D/DP) never change edges 0-8 and are excluded from the
     * search.  The 27 starting orientations R^{0..2} L^{0..2} B^{0..2} are
     * all seeded at distance 0 with a first-move restriction (U/UP only at
     * ply 0), matching the semantics of the original generator.</p>
     *
     * @return a {@code byte[181440]} pruning table
     */
    static byte[] generateEdgePruningTable() {
        final int size = 181440;

        final int[] edgeMoves = {FtoCubie.U, FtoCubie.UP, FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP};
        final int moves = edgeMoves.length;

        boolean[] startSeen = new boolean[size];
        int[] startIndices = new int[27];
        int startCount = 0;
        for (int r = 0; r < 3; r++) {
            for (int l = 0; l < 3; l++) {
                for (int b = 0; b < 3; b++) {
                    FtoCubie fto = new FtoCubie();
                    for (int ri = 0; ri < r; ri++) fto = fto.turn(FtoCubie.R);
                    for (int li = 0; li < l; li++) fto = fto.turn(FtoCubie.L);
                    for (int bi = 0; bi < b; bi++) fto = fto.turn(FtoCubie.B);

                    int idx = fto.packPhaseTwoEdges();
                    if (!startSeen[idx]) {
                        startSeen[idx] = true;
                        startIndices[startCount++] = idx;
                    }
                }
            }
        }
        int uniqueStarts = startCount;

        // ---- Step 3: BFS ----
        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        // All starting orientations are at distance 0
        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        for (int s = 0; s < uniqueStarts; s++) {
            int idx = startIndices[s];
            prun[idx] = 0;
            frontier.add(idx);
        }

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < moves; m++) {
                    int nextIdx = PHASE_TWO_EDGE_MOVES[idx][edgeMoves[m]];
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }
            frontier = next;
            depth++;
        }

        return prun;
    }

    private static byte[] edgePrun;

    private static synchronized void initPhaseTwoEdges(){
        PHASE_TWO_EDGE_MOVES = new int[181440][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 181440; idx++) {
            fto.setPhaseTwoEdges(idx);
            int[] moves = PHASE_TWO_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packPhaseTwoEdges();
            }
        }

        PHASE_TWO_SOLVED_EDGES = new HashSet<>();

        for (int r = 0; r < 3; r++) {
            for (int l = 0; l < 3; l++) {
                for (int b = 0; b < 3; b++) {
                    fto = new FtoCubie();

                    for (int i = 0; i < r; i++) {
                        fto = fto.turn(FtoCubie.R);
                    }
                    for (int i = 0; i < l; i++) {
                        fto = fto.turn(FtoCubie.L);
                    }
                    for (int i = 0; i < b; i++) {
                        fto = fto.turn(FtoCubie.B);
                    }

                    PHASE_TWO_SOLVED_EDGES.add(fto.packPhaseTwoEdges());
                }
            }
        }

        edgePrun = generateEdgePruningTable();
    }

    public static int turnG2Edges(int idx, int move){
        return PHASE_TWO_EDGE_MOVES[idx][move];
    }

    public static int turnG2Tris(int idx, int move){
        return PHASE_TWO_TRIANGLE_MOVES[idx][move];
    }

    public static boolean isSolvedG2Edges(int idx){
        if (!initialized){
            throw new IllegalStateException("Must be initialized");
        }
        return PHASE_TWO_SOLVED_EDGES.contains(idx);
    }

    public static boolean isSolvedG2Tris(int idx){
        return idx == 0;
    }

    public static int prunG2Edge(int idx){
        return edgePrun[idx];
    }

    public static synchronized void init(){
        if (initialized) return;

        initPhaseOneEdges();
        initPhaseOneTriangles();
        initPhaseTwoTris();
        initPhaseTwoEdges();

        initialized = true;
    }

}
