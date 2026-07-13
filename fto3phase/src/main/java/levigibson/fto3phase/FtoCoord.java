package levigibson.fto3phase;

import java.util.*;

import static levigibson.fto3phase.Util.*;

class FtoCoord {
    private static volatile boolean initialized;

    //-------------- Move Tables --------------//

    private static int[][] G1_EDGE_MOVES;
    private static int[][] G1_TRIANGLE_MOVES;

    private static int[][] G2_TRIANGLE_MOVES;
    private static int[][] G2_EDGE_MOVES;
    private static int[][] G2_TRIPLE_MOVES;

    private static int[][] G3_EDGE_MOVES;
    private static int[][] G3_CORNER_MOVES;

    //-------------- Pruning Tables --------------//

    private static byte[] g1Prun;

    private static byte[] g2TriplePrun;
    private static byte[] g2TxEPrun;

    private static byte[] g3CornerPrun;

    private static final int G1_TRIANGLES_SIZE = nCr(12,3);
    private static final int G1_EDGES_SIZE = nCr(12,3) * 2;
    private static final int G2_TRIANGLES_SIZE = nCr(9,3) * nCr(6,3);
    private static final int G2_EDGES_SIZE = nCr(9,3) * nCr(6,3) * 2 * 2;
    private static final int G2_TRIPLE_SIZE = nCr(12,3) * nCr(6,3) * pow(2,3);
    private static final int G3_CORNERS_SIZE = (fact(6)/2) * pow(2,5);
    private static final int G3_EDGE_SIZE = pow(3,4);

    //-------------- Public Turn Functions --------------//

    public static int g1TurnEdges(int idx, int move){
        return G1_EDGE_MOVES[idx][move];
    }

    public static int g1TurnTris(int idx, int move){
        return G1_TRIANGLE_MOVES[idx][move];
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

    public static int g3TurnEdges(int idx, int move){
        return G3_EDGE_MOVES[idx][move];
    }

    public static int g3TurnCorners(int idx, int move){
        return G3_CORNER_MOVES[idx][move];
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

    public static int g3PrunCorners(int corners){
        return g3CornerPrun[corners];
    }

    //-------------- Solve Check Functions --------------//

    public static final int SOLVED_G3_EDGES;
    public static final int SOLVED_G3_CORNERS;

    static {
        SOLVED_G3_EDGES = new FtoCubie().g3PackEdges();
        SOLVED_G3_CORNERS = new FtoCubie().g3PackCorners();
    }

    //-------------- Growable Primitive Int Array --------------//

    private static class IntArray {
        int[] data;
        int size;

        IntArray(int capacity) {
            this.data = new int[capacity];
        }

        void add(int v) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = v;
        }

        void clear() {
            size = 0;
        }
    }

    //-------------- Pruning Table Generation --------------//

    private static int packG1(int edge, int tris){
        return edge * G1_TRIANGLES_SIZE + tris;
    }

    private static int turnG1(int idx, int move){
        int edge = idx / G1_TRIANGLES_SIZE;
        int tri = idx % G1_TRIANGLES_SIZE;

        edge = g1TurnEdges(edge, move);
        tri = g1TurnTris(tri, move);

        return packG1(edge, tri);
    }

    static byte[] g1GeneratePrun() {
        final int size = G1_EDGES_SIZE * G1_TRIANGLES_SIZE;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        IntArray frontier = new IntArray(52063);
        IntArray next = new IntArray(52063);

        FtoCubie solved = new FtoCubie();
        frontier.add(packG1(solved.g1PackEdges(), solved.g1PackTriangles()));
        prun[frontier.data[0]] = 0;

        int depth = 0;
        while (frontier.size > 0) {

            for (int i = 0; i < frontier.size; i++) {
                int idx = frontier.data[i];
                for (int move : Search.G1_MOVESET) {
                    int nextIdx = turnG1(idx, move);
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }

            IntArray tmp = frontier;
            frontier = next;
            next = tmp;
            next.clear();

            depth++;
        }

        return prun;
    }

    private static IntArray g2GenerateTripleFrontier(){
        byte[] prun = new byte[G2_TRIPLE_SIZE];
        Arrays.fill(prun, (byte) -1);

        IntArray all = new IntArray(161);
        IntArray next = new IntArray(100);
        IntArray frontier = new IntArray(100);

        frontier.add(new FtoCubie().g2PackTriples(0));

        while (frontier.size > 0) {
            for (int i = 0; i < frontier.size; i++) {
                all.add(frontier.data[i]);
            }

            for (int i = 0; i < frontier.size; i++) {
                int idx = frontier.data[i];
                for (int move : Search.G3_MOVESET) {
                    int nextIdx = G2_TRIPLE_MOVES[idx][move];
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (1);
                        next.add(nextIdx);
                    }
                }
            }

            IntArray tmp = frontier;
            frontier = next;
            next = tmp;
            next.clear();
        }

        return all;
    }

    private static byte[] g2GenerateTriplePrun() {
        byte[] prun = new byte[G2_TRIPLE_SIZE];
        Arrays.fill(prun, (byte) -1);

        IntArray frontier = g2GenerateTripleFrontier();
        for (int i = 0; i < frontier.size; i++) {
            prun[frontier.data[i]] = 0;
        }

        int depth = 0;
        while (frontier.size > 0) {
            IntArray next = new IntArray(11484);

            for (int i = 0; i < frontier.size; i++) {
                int idx = frontier.data[i];
                for (int move : Search.G2_MOVESET) {
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
        return edge * G2_TRIANGLES_SIZE + tri;
    }

    private static int turnTxE(int idx, int move){
        int edge = idx / G2_TRIANGLES_SIZE;
        int tri = idx % G2_TRIANGLES_SIZE;

        edge = g2TurnEdges(edge, move);
        tri = g2TurnTris(tri, move);

        return packTxE(edge, tri);
    }

    private static byte[] g2GenerateTxEPrun(){
        final int size = G2_EDGES_SIZE * G2_TRIANGLES_SIZE;

        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        IntArray frontier = new IntArray(4_194_993);
        IntArray next = new IntArray(4_194_993);

        FtoCubie solved = new FtoCubie();
        frontier.add(packTxE(solved.g2PackEdges(), solved.g2PackTris()));
        prun[frontier.data[0]] = 0;

        int depth = 0;
        while (frontier.size > 0) {
            for (int i = 0; i < frontier.size; i++) {
                int idx = frontier.data[i];

                for (int move : Search.G2_MOVESET) {
                    int nextIdx = turnTxE(idx, move);
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }

            IntArray tmp = frontier;
            frontier = next;
            next = tmp;
            next.clear();

            depth++;
        }

        return prun;
    }

    private static byte[] g3GenerateCornerPrun(){
        byte[] prun = new byte[G3_CORNERS_SIZE];
        Arrays.fill(prun, (byte) -1);

        IntArray frontier = new IntArray(5405);
        IntArray next = new IntArray(5405);
        frontier.add(new FtoCubie().g3PackCorners());
        prun[frontier.data[0]] = 0;

        int depth = 0;
        while (frontier.size != 0) {
            for (int i = 0; i < frontier.size; i++) {
                int idx = frontier.data[i];
                for (int move : Search.G3_MOVESET) {
                    int nextIdx = g3TurnCorners(idx, move);
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }
            IntArray tmp = frontier;
            frontier = next;
            next = tmp;
            next.clear();

            depth++;
        }

        return prun;
    }

    //-------------- Move Table Generation --------------//

    private static void g1InitEdges(){
        G1_EDGE_MOVES = new int[G1_EDGES_SIZE][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G1_EDGES_SIZE; idx++) {
                fto.g1SetEdges(idx);
                int[] locMoves = G1_EDGE_MOVES[idx];

                for (int move : Search.G1_MOVESET) {
                    fto.turnInto(move, turned);
                    locMoves[move] = turned.g1PackEdges();
                }
        }
    }

    private static void g1InitTriangles(){
        G1_TRIANGLE_MOVES = new int[G1_TRIANGLES_SIZE][16];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G1_TRIANGLES_SIZE; idx++) {
            fto.g1SetTriangles(idx);
            int[] moves = G1_TRIANGLE_MOVES[idx];

            for (int move : Search.G1_MOVESET) {
                fto.turnInto(move, turned);
                moves[move] = turned.g1PackTriangles();
            }
        }
    }

    private static void g2InitTris(){
        G2_TRIANGLE_MOVES = new int[G2_TRIANGLES_SIZE][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G2_TRIANGLES_SIZE; idx++) {
            fto.g2SetTriangles(idx);
            int[] moves = G2_TRIANGLE_MOVES[idx];

            for (int move : Search.G2_MOVESET) {
                fto.turnInto(move, turned);
                moves[move] = turned.g2PackTris();
            }
        }
    }

    private static void g2InitEdges(){
        G2_EDGE_MOVES = new int[G2_EDGES_SIZE][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G2_EDGES_SIZE; idx++) {
            fto.g2SetEdges(idx);
            int[] moves = G2_EDGE_MOVES[idx];

            for (int move : Search.G2_MOVESET) {
                fto.turnInto(move, turned);
                moves[move] = turned.g2PackEdges();
            }
        }
    }

    private static void g2InitTriples(){
        G2_TRIPLE_MOVES = new int[G2_TRIPLE_SIZE][10];
        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G2_TRIPLE_SIZE; idx++) {
            fto.g2SetTriples(idx, 0);
            int[] moves = G2_TRIPLE_MOVES[idx];

            for (int move : Search.G2_MOVESET) {
                fto.turnInto(move, turned);
                moves[move] = turned.g2PackTriples(0);
            }
        }
    }

    private static void g3InitEdges(){
        G3_EDGE_MOVES = new int[G3_EDGE_SIZE][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G3_EDGE_SIZE; idx++) {
            fto.g3SetEdges(idx);
            int[] moves = G3_EDGE_MOVES[idx];

            for (int move : Search.G3_MOVESET) {
                fto.turnInto(move, turned);
                moves[move] = turned.g3PackEdges();
            }
        }
    }

    private static void g3InitCorners(){
        G3_CORNER_MOVES = new int[G3_CORNERS_SIZE][10];

        FtoCubie fto = new FtoCubie();
        FtoCubie turned = new FtoCubie();

        for (int idx = 0; idx < G3_CORNERS_SIZE; idx++) {
            fto.g3SetCorners(idx);
            int[] moves = G3_CORNER_MOVES[idx];

            for (int move : Search.G3_MOVESET) {

                fto.turnInto(move, turned);
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
            return;
        }

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
        g3CornerPrun = g3GenerateCornerPrun();

        initialized = true;
    }
}
