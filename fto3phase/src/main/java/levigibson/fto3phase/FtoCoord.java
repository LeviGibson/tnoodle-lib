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

    private static byte[] g2TriplePrun;
    private static byte[] g2TxEPrun;
    private static byte[] g1Prun;

    //-------------- Public Turn Functions --------------//

    public static int g1TurnEdges(int idx, int move){
        return G1_EDGE_MOVES[idx][move];
    }

    public static int g1TurnTris(int idx, int move){
        return G1_TRIANGLE_MOVES[idx][move];
    }

    public static int g3TurnEdges(int idx, int move){
        return G3_EDGE_MOVES[idx][move];
    }

    public static int g3TurnCorners(int idx, int move){
        return G3_CORNER_MOVES[idx][move];
    }

    public static int g2TurnTriples(int idx, int move){
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

    public static int g2PrunTriple(int idx1, int idx2, int idx3, int idx4){
        int prun = 0;
        prun = Math.max(prun, g2TriplePrun[idx1]);
        prun = Math.max(prun, g2TriplePrun[idx2]);
        prun = Math.max(prun, g2TriplePrun[idx3]);
        prun = Math.max(prun, g2TriplePrun[idx4]);
        return prun;
    }

    public static int g2PrunTxE(int edge, int tris){
        return g2TxEPrun[packTxE(edge, tris)];
    }

    //-------------- Solve Check Functions --------------//

    private static final int SOLVED_G3_EDGES;
    private static final int SOLVED_G3_CORNERS;

    static {
        SOLVED_G3_EDGES = new FtoCubie().g3PackEdges();
        SOLVED_G3_CORNERS = new FtoCubie().g3PackCorners();
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
        tri = g1TurnTris(tri, move);

        return packG1(edge, tri);
    }

    static byte[] g1GeneratePrun() {
        final int size = 96800;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        FtoCubie solved = new FtoCubie();
        frontier.add(packG1(solved.g1PackEdges(), solved.g1PackTriangles()));
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

    private static LinkedList<Integer> g2GenerateTripleFrontier(){
        final int size = 35200;
        LinkedList<Integer> all = new LinkedList<>();

        final int[] g3Moves = {FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.D, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP, FtoCubie.DP};

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        LinkedList<Integer> frontier = new LinkedList<>();
        frontier.add(new FtoCubie().g2PackTriples(0));

        int depth = 0;
        while (!frontier.isEmpty()) {
            all.addAll(frontier);
            LinkedList<Integer> next = new LinkedList<>();

            for (int idx : frontier) {
                for (int move : g3Moves) {
                    int nextIdx = G2_TRIPLE_MOVES[idx][move];
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

    private static byte[] g2GenerateTriplePrun() {
        final int size = 35200;

        final int[] moves = {FtoCubie.D, FtoCubie.DP, FtoCubie.U, FtoCubie.UP, FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.RP, FtoCubie.LP, FtoCubie.BP};

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        LinkedList<Integer> frontier = g2GenerateTripleFrontier();
        for (Integer i : frontier) {
            prun[i] = 0;
        }

        int depth = 0;
        while (!frontier.isEmpty()) {
            LinkedList<Integer> next = new LinkedList<>();

            for (int idx : frontier) {
                for (int move : moves) {
                    int nextIdx = G2_TRIPLE_MOVES[idx][move];
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
    }

    private static int turnTxE(int idx, int move){
        int edge = idx / 1680;
        int tri = idx % 1680;

        edge = g2TurnEdges(edge, move);
        tri = g2TurnTris(tri, move);

        return packTxE(edge, tri);
    }

    private static byte[] g2GenerateTxEPrun(){
        final int size = 11_289_600;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        FtoCubie solved = new FtoCubie();
        frontier.add(packTxE(solved.g2PackEdges(), solved.g2PackTris()));
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

        return prun;
    }

    //-------------- Move Table Generation --------------//

    private static void g1InitEdges(){
        G1_EDGE_MOVES = new int[440][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 440; idx++) {
                fto.g1SetEdges(idx);
                int[] locMoves = G1_EDGE_MOVES[idx];

                for (int move = 0; move < 16; move++) {
                    fto.turn(move, turned);
                    locMoves[move] = turned.g1PackEdges();
                }
        }
    }

    private static void g1InitTriangles(){
        G1_TRIANGLE_MOVES = new int[220][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 220; idx++) {
            fto.g1SetTriangles(idx);
            int[] moves = G1_TRIANGLE_MOVES[idx];

            for (int move = 0; move < 16; move++) {
                fto.turn(move, turned);
                moves[move] = turned.g1PackTriangles();
            }
        }
    }

    private static void g2InitTris(){
        G2_TRIANGLE_MOVES = new int[1680][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 1680; idx++) {
            fto.g2SetTriangles(idx);
            int[] moves = G2_TRIANGLE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.g2PackTris();
            }
        }
    }

    private static void g2InitEdges(){
        G2_EDGE_MOVES = new int[6720][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 6720; idx++) {
            fto.g2SetEdges(idx);
            int[] moves = G2_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.g2PackEdges();
            }
        }
    }

    public static void g2InitTriples(){
        G2_TRIPLE_MOVES = new int[35200][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 35200; idx++) {
            fto.g2SetTriples(idx, 0);
            int[] moves = G2_TRIPLE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                fto.turn(move, turned);
                moves[move] = turned.g2PackTriples(0);
            }
        }
    }

    private static void g3InitEdges(){
        G3_EDGE_MOVES = new int[81][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 81; idx++) {
            fto.g3SetEdges(idx);
            int[] moves = G3_EDGE_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                if (move == FtoCubie.U || move == FtoCubie.UP){
                    moves[move] = -1;
                    continue;
                }

                fto.turn(move, turned);
                moves[move] = turned.g3PackEdges();
            }
        }
    }

    private static void g3InitCorners(){
        G3_CORNER_MOVES = new int[11520][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < 11520; idx++) {
            fto.g3SetCorners(idx);
            int[] moves = G3_CORNER_MOVES[idx];

            for (int move = 0; move < 10; move++) {
                if (move == FtoCubie.U || move == FtoCubie.UP){
                    moves[move] = -1;
                    continue;
                }

                fto.turn(move, turned);
                moves[move] = turned.g3PackCorners();
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
        }

        long start = System.currentTimeMillis();

        g1InitEdges();
        g1InitTriangles();
        g1Prun = g1GeneratePrun();

        g2InitTris();
        g2InitEdges();
        g2InitTriples();
        g2TxEPrun = g2GenerateTxEPrun();
        g2TriplePrun = g2GenerateTriplePrun();

        g3InitEdges();
        g3InitCorners();

        initialized = true;

        System.out.println("Time to initialize: " + (System.currentTimeMillis() - start));
    }

}
