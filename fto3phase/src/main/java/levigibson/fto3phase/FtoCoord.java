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
    private static byte[] g2TxEPrun;
    private static byte[] g1Prun;

    //-------------- Public Turn Functions --------------//

    public static int g1TurnEdges(int idx, int move){
        return G1_EDGE_MOVES[idx][move];
    }

    public static int g1TurnTriangles(int idx, int move){
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

    public static int g1Prun(int edge, int tris){
        return g1Prun[packG1(edge, tris)];
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

    public static int g2PrunTxE(int edge, int tris){
        return g2TxEPrun[packTxE(edge, tris)];
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

    private static int packG1(int edge, int tris){
        return edge * 220 + tris;
    }

    private static int turnG1(int idx, int move){
        int edge = idx / 220;
        int tri = idx % 220;

        edge = g1TurnEdges(edge, move);
        tri = g1TurnTriangles(tri, move);

        return packG1(edge, tri);
    }

    static byte[] g1GeneratePruningTable() {
        final int size = 440 * 220;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        FtoCubie solved = new FtoCubie();
        frontier.add(packG1(solved.packG1Edges(), solved.packG1Triangles()));
        prun[frontier.get(0)] = 0;

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < 16; m++) {
                    int nextIdx = turnG1(idx, m);
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

//    static byte[] g1GenerateTrianglesPruningTable() {
//        final int size = 220;
//
//        byte[] prun = new byte[size];
//        Arrays.fill(prun, (byte) -1);
//
//        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
//        frontier.add(new FtoCubie().packG1Edges());
//        prun[frontier.get(0)] = 0;
//
//        int depth = 0;
//        while (!frontier.isEmpty()) {
//            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();
//
//            for (int idx : frontier) {
//                for (int m = 0; m < 16; m++) {
//                    int nextIdx = G1_EDGE_MOVES[idx][m];
//                    if (prun[nextIdx] == -1) {
//                        prun[nextIdx] = (byte) (depth + 1);
//                        next.add(nextIdx);
//                    }
//                }
//            }
//            frontier = next;
//            depth++;
//        }
//
//        return prun;
//    }

    static byte[] g2GenerateEdgesPruningTable() {
        final int size = 6720;

        final int[] edgeMoves = {FtoCubie.U, FtoCubie.UP, FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP};
        final int moves = edgeMoves.length;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        frontier.add(new FtoCubie().packG2Edges());
        prun[frontier.get(0)] = 0;

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

    private static int packTxE(int edge, int tri){
        return edge * 1680 + tri;
    };

    private static int turnTxE(int idx, int move){
        int edge = idx / 1680;
        int tri = idx % 1680;

        edge = g2TurnEdges(edge, move);
        tri = g2TurnTris(tri, move);

        return packTxE(edge, tri);
    }

    private static synchronized void initTxEPrun(){
        final int size = 6720 * 1680;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        FtoCubie solved = new FtoCubie();
        frontier.add(packTxE(solved.packG2Edges(), solved.packG2Tris()));
        prun[frontier.get(0)] = 0;

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            for (int idx : frontier) {
                for (int m = 0; m < 10; m++) {
                    int nextIdx = turnTxE(idx, m);
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }
            frontier = next;
            depth++;
        }

        g2TxEPrun = prun;
    }

    //-------------- Move Table Generation --------------//

    private static synchronized void initPhaseOneEdges(){
        G1_EDGE_MOVES = new int[440][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 440; idx++) {
                fto.setG1Edges(idx);
                int[] locMoves = G1_EDGE_MOVES[idx];

                for (int move = 0; move < 16; move++) {
                    fto.turn(move, turned);
                    locMoves[move] = turned.packG1Edges();
                }
        }
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

    private static synchronized void initPhaseTwoEdges(){
        G2_EDGE_MOVES = new int[6720][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 6720; idx++) {
            fto.setG2Edges(idx);
            int[] moves = G2_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.packG2Edges();
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

        long start = System.currentTimeMillis();

        initPhaseOneEdges();
        initPhaseOneTriangles();
        g1Prun = g1GeneratePruningTable();

        initPhaseTwoTris();
        initPhaseTwoEdges();

        initTriples();
        initPhaseThreeEdges();
        initPhaseThreeCorners();

        initTxEPrun();

        System.out.println("Time to initialize: " + (System.currentTimeMillis() - start));


        initialized = true;
    }

}
