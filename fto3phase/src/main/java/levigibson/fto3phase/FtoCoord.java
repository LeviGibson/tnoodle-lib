package levigibson.fto3phase;

import java.util.*;

public class FtoCoord {
    private static boolean initialized;


    //-------------- Move Tables --------------//

    private static int[][] G1_EDGE_MOVES;
    private static int[][] G1_TRIANGLE_MOVES;

    private static int[][] G2_TRIANGLE_MOVES;
    private static int[][] G2_EDGE_MOVES;
    private static int[][] G2_TRIPLE_MOVES;

    private static int[][] G3_EDGE_MOVES;
    private static int[][] G3_CORNER_MOVES;

    //-------------- Pruning Tables --------------//

    private static byte[] g2edgePrun;
    private static byte[] g2triplePrun;
    private static byte[] g2trianglePrun;
    private static byte[] g1EdgePrun;

    //-------------- Public Turn Functions --------------//

    public static int g1TurnEdges(int idx, int move){
        if (!initialized) throw new IllegalStateException("Can not turn the cube when its not initialized");
        return G1_EDGE_MOVES[idx][move];
    }

    public static int g1TurnTriangles(int idx, int move){
        if (!initialized) throw new IllegalStateException("Can not turn the cube when its not initialized");
        return G1_TRIANGLE_MOVES[idx][move];
    }

    public static int g3TurnEdge(int idx, int move){
        return G3_EDGE_MOVES[idx][move];
    }

    public static int g3TurnCorner(int idx, int move){
        return G3_CORNER_MOVES[idx][move];
    }

    public static int g2TurnTriple(int idx, int move){
        return G2_TRIPLE_MOVES[idx][move];
    }

    public static int g2TurnEdges(int idx, int move){
        return G2_EDGE_MOVES[idx][move];
    }

    public static int g2TurnTris(int idx, int move){
        return G2_TRIANGLE_MOVES[idx][move];
    }

    //-------------- Public Pruning Functions --------------//

    public static int g1PrunEdge(int idx){
        return g1EdgePrun[idx];
    }

    public static int g2PrunEdge(int idx){
        return g2edgePrun[idx];
    }

    public static int g2PrunTriple(int idx1, int idx2, int idx3, int idx4){
        int prun = 0;
        prun = Math.max(prun, g2triplePrun[idx1]);
        prun = Math.max(prun, g2triplePrun[idx2]);
        prun = Math.max(prun, g2triplePrun[idx3]);
        prun = Math.max(prun, g2triplePrun[idx4]);
        return prun;
    }

    public static int g2PrunTriangle(int idx){
        return g2trianglePrun[idx];
    }

    //-------------- Solve Check Functions --------------//

    public static boolean isPhaseOne(int edgeIdx, int triIdx){
        return triIdx == 219 && (edgeIdx == 1314 || edgeIdx == 1318 || edgeIdx == 1317);
    }

    private static final int SOLVED_G3_EDGES;
    private static final int SOLVED_G3_CORNERS;

    static {
        SOLVED_G3_EDGES = new FtoCubie().packG3Edges();
        SOLVED_G3_CORNERS = new FtoCubie().packG3Corners();
    }

    public static boolean isSolvedG3Edges(int idx){
        return (idx == SOLVED_G3_EDGES);
    }

    public static boolean isSolvedG3Corners(int idx){
        return (idx == SOLVED_G3_CORNERS);
    }

    //-------------- Pruning Table Generation --------------//

    static byte[] g1GenerateEdgesPruningTable() {
        final int size = 1320;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        frontier.add(new FtoCubie().packG1Edges());
        prun[frontier.get(0)] = 0;

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < 16; m++) {
                    int nextIdx = G1_EDGE_MOVES[idx][m];
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

    static byte[] g2GenerateEdgesPruningTable() {
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

                    int idx = fto.packG2Edges();
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
                    int nextIdx = G2_EDGE_MOVES[idx][edgeMoves[m]];
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

    static byte[] g2GenerateTrianglePruningTable() {
        final int size = 1680;

        final int[] edgeMoves = {FtoCubie.U, FtoCubie.UP, FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP};
        final int moves = edgeMoves.length;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        frontier.add(new FtoCubie().packG2Tris());
        prun[frontier.get(0)] = 0;

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < moves; m++) {
                    int nextIdx = G2_TRIANGLE_MOVES[idx][edgeMoves[m]];
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

    private static LinkedList<Integer> generateTripleFrontier(){
        final int size = 35200;
        LinkedList<Integer> all = new LinkedList<>();

        final int[] g3Moves = {FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.D, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP, FtoCubie.DP};
        final int moves = g3Moves.length;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        LinkedList<Integer> frontier = new LinkedList<>();
        frontier.add(new FtoCubie().packG2Triples(0));

        int depth = 0;
        while (!frontier.isEmpty()) {
            all.addAll(frontier);
            LinkedList<Integer> next = new LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < moves; m++) {
                    int nextIdx = G2_TRIPLE_MOVES[idx][g3Moves[m]];
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
                    int nextIdx = G2_TRIPLE_MOVES[idx][edgeMoves[m]];
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

    //-------------- Move Table Generation --------------//

    private static synchronized void initPhaseOneEdges(){
        G1_EDGE_MOVES = new int[1320][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 220 * 6; idx++) {
                fto.setG1Edges(idx);
                int[] locMoves = G1_EDGE_MOVES[idx];

                for (int move = 0; move < 16; move++) {
                    fto.turn(move, turned);
                    locMoves[move] = turned.packG1Edges();
                }
        }

        g1EdgePrun = g1GenerateEdgesPruningTable();
    }

    private static synchronized void initPhaseOneTriangles(){
        G1_TRIANGLE_MOVES = new int[220][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 220; idx++) {
            fto.setG1Triangles(idx);
            int[] moves = G1_TRIANGLE_MOVES[idx];

            for (int move = 0; move < 16; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packG1Triangles();
            }
        }
    }

    private static synchronized void initPhaseTwoTris(){
        G2_TRIANGLE_MOVES = new int[1680][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 1680; idx++) {
            fto.setG2Triangles(idx);
            int[] moves = G2_TRIANGLE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packG2Tris();
            }
        }

        g2trianglePrun = g2GenerateTrianglePruningTable();
    }

    public static Set<Integer> PHASE_TWO_SOLVED_EDGES;

    private static synchronized void initPhaseTwoEdges(){
        G2_EDGE_MOVES = new int[181440][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 181440; idx++) {
            fto.setG2Edges(idx);
            int[] moves = G2_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packG2Edges();
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

                    PHASE_TWO_SOLVED_EDGES.add(fto.packG2Edges());
                }
            }
        }

        g2edgePrun = g2GenerateEdgesPruningTable();
    }

    public static void initTriples(){
        G2_TRIPLE_MOVES = new int[35200][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 35200; idx++) {
            fto.setG2Triples(idx, 0);
            int[] moves = G2_TRIPLE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packG2Triples(0);
            }
        }

        g2triplePrun = generateTriplePruningTable();
    }

    private static void initPhaseThreeEdges(){
        G3_EDGE_MOVES = new int[81][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 81; idx++) {
            fto.setG3Edges(idx);
            int[] moves = G3_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                if (move == FtoCubie.U || move == FtoCubie.UP){
                    moves[move] = -1;
                    continue;
                }

                fto.turn(move, turned);
                moves[move] = turned.packG3Edges();
            }
        }
    }

    private static void initPhaseThreeCorners(){
        G3_CORNER_MOVES = new int[11520][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 11520; idx++) {
            fto.setG3Corners(idx);
            int[] moves = G3_CORNER_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                if (move == FtoCubie.U || move == FtoCubie.UP){
                    moves[move] = -1;
                    continue;
                }

                fto.turn(move, turned);
                moves[move] = turned.packG3Corners();
            }
        }
    }

    //-------------- Public Initialization Functions --------------//

    public static boolean getInitialized(){
        return initialized;
    }

    public static synchronized void init(){
        if (initialized) {
            System.out.println("FtoCoord::init() called twice");
            return;
        };

        initPhaseOneEdges();
        initPhaseOneTriangles();
        initPhaseTwoTris();
        long start = System.currentTimeMillis();

        initPhaseTwoEdges();
        System.out.println("Time to initialize: " + (System.currentTimeMillis() - start));

        initTriples();
        initPhaseThreeEdges();
        initPhaseThreeCorners();

        initialized = true;
    }

}
