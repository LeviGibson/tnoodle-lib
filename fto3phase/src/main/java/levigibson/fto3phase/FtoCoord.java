package levigibson.fto3phase;

import java.util.*;

public class FtoCoord {
    private static boolean initialized;


    //-------------- PHASE ONE --------------//

    private static int[][] PHASE_ONE_EDGE_MOVES;
    private static int[][] PHASE_ONE_TRIANGLE_MOVES;
    private static int[][] PHASE_TWO_TRIANGLE_MOVES;
    private static int[][] PHASE_TWO_EDGE_MOVES;
    private static int[][] PHASE_TWO_TRIPLE_MOVES;
    private static int[][] PHASE_THREE_EDGE_MOVES;
    private static int[][] PHASE_THREE_CORNER_MOVES;

    private static byte[] edgePrun;
    private static byte[] triplePrun;
    private static byte[] trianglePrun;

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

    static byte[] generateTrianglePruningTable() {
        final int size = 1680;

        final int[] edgeMoves = {FtoCubie.U, FtoCubie.UP, FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP};
        final int moves = edgeMoves.length;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        frontier.add(new FtoCubie().packPhaseTwoTris());
        prun[frontier.get(0)] = 0;

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < moves; m++) {
                    int nextIdx = PHASE_TWO_TRIANGLE_MOVES[idx][edgeMoves[m]];
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

        trianglePrun = generateTrianglePruningTable();
    }

    public static Set<Integer> PHASE_TWO_SOLVED_EDGES;

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

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

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

    public static int prunG2Edge(int idx){
        return edgePrun[idx];
    }

    public static int prunG2Triple(int idx1, int idx2, int idx3, int idx4){
        int prun = 0;
        prun = Math.max(prun, triplePrun[idx1]);
        prun = Math.max(prun, triplePrun[idx2]);
        prun = Math.max(prun, triplePrun[idx3]);
        prun = Math.max(prun, triplePrun[idx4]);
        return prun;
    }

    private static LinkedList<Integer> generateTripleFrontier(){
        final int size = 35200;
        LinkedList<Integer> all = new LinkedList<>();

        final int[] g3Moves = {FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.D, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP, FtoCubie.DP};
        final int moves = g3Moves.length;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        LinkedList<Integer> frontier = new LinkedList<>();
        frontier.add(new FtoCubie().packTriples(0));

        int depth = 0;
        while (!frontier.isEmpty()) {
            all.addAll(frontier);
            LinkedList<Integer> next = new LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < moves; m++) {
                    int nextIdx = PHASE_TWO_TRIPLE_MOVES[idx][g3Moves[m]];
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }

            frontier = next;
            depth++;
        }

        return all;
    }

    static byte[] generateTriplePruningTable() {
        final int size = 35200;

        final int[] edgeMoves = {FtoCubie.D, FtoCubie.DP, FtoCubie.U, FtoCubie.UP, FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP};
        final int moves = edgeMoves.length;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        LinkedList<Integer> frontier = generateTripleFrontier();
        for (Integer i : frontier) {
            prun[i] = 0;
        }

        int depth = 0;
        while (!frontier.isEmpty()) {
            LinkedList<Integer> next = new LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < moves; m++) {
                    int nextIdx = PHASE_TWO_TRIPLE_MOVES[idx][edgeMoves[m]];
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

    public static void initTriples(){
        PHASE_TWO_TRIPLE_MOVES = new int[35200][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 35200; idx++) {
            fto.setTriples(idx, 0);
            int[] moves = PHASE_TWO_TRIPLE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packTriples(0);
            }
        }

        triplePrun = generateTriplePruningTable();
    }

    private static void initPhaseThreeEdges(){
        PHASE_THREE_EDGE_MOVES = new int[81][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 81; idx++) {
            fto.setPhaseThreeEdges(idx);
            int[] moves = PHASE_THREE_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                if (move == FtoCubie.U || move == FtoCubie.UP){
                    moves[move] = -1;
                    continue;
                }

                fto.turn(move, turned);
                moves[move] = turned.packPhaseThreeEdges();
            }
        }
    }

    private static void initPhaseThreeCorners(){
        PHASE_THREE_CORNER_MOVES = new int[11520][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 11520; idx++) {
            fto.setPhaseThreeCorners(idx);
            int[] moves = PHASE_THREE_CORNER_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                if (move == FtoCubie.U || move == FtoCubie.UP){
                    moves[move] = -1;
                    continue;
                }

                fto.turn(move, turned);
                moves[move] = turned.packPhaseThreeCorners();
            }
        }
    }

    private static final int SOLVED_G3_EDGES;
    private static final int SOLVED_G3_CORNERS;

    static {
        SOLVED_G3_EDGES = new FtoCubie().packPhaseThreeEdges();
        SOLVED_G3_CORNERS = new FtoCubie().packPhaseThreeCorners();
    }

    public static boolean isSolvedG3Edges(int idx){
        return (idx == SOLVED_G3_EDGES);
    }

    public static boolean isSolvedG3Corners(int idx){
        return (idx == SOLVED_G3_CORNERS);
    }

    public static int turnG3Edge(int idx, int move){
        return PHASE_THREE_EDGE_MOVES[idx][move];
    }

    public static int turnG3Corner(int idx, int move){
        return PHASE_THREE_CORNER_MOVES[idx][move];
    }

    public static int turnG2Triple(int idx, int move){
        return PHASE_TWO_TRIPLE_MOVES[idx][move];
    }

    public static int prunG2Triangle(int idx){
        return trianglePrun[idx];
    }

    public static synchronized void init(){
        if (initialized) return;

        initPhaseOneEdges();
        initPhaseOneTriangles();
        initPhaseTwoTris();
        initPhaseTwoEdges();
        initTriples();
        initPhaseThreeEdges();
        initPhaseThreeCorners();

        initialized = true;
    }

}
