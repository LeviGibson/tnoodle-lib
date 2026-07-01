package levigibson.fto3phase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static levigibson.fto3phase.FtoCoord.*;

public class Search {

    public Search(){
        moves = new int[64];
    }

    private static boolean initialized = false;

    private int[] moves;
    private long[] nodes;

    public boolean searchPhaseTwo(int depth, int maxl, int edge, int tri, int tp0, int tp1, int tp2, int tp3){
        nodes[depth]++;

        int edgePrun = FtoCoord.g2PrunEdge(edge);
        if (depth < edgePrun)
            return false;

        int triplePrun = FtoCoord.g2PrunTriple(tp0, tp1, tp2, tp3);
        if (depth < triplePrun)
            return false;

        int trianglePrun = FtoCoord.g2PrunTriangle(tri);
        if (depth < trianglePrun)
            return false;

        if (depth <= 7) {
            int centerPrun = prunG2Centers(edge, tri);

            if (depth < centerPrun)
                return false;
        }

        if (triplePrun == 0 && edgePrun == 0 && trianglePrun == 0){
            return true;
        }

        if (depth == 0){
            return false;
        }

        for (int move = 0; move < 10; move++) {
            if (maxl > 0) {
                int la = moves[maxl - 1] / 2;
                int ca = move / 2;
                if (((invalidMoves[la] >> ca) & 1) == 1) continue;
            }

            moves[maxl] = move;

            boolean res = searchPhaseTwo(depth-1, maxl+1,
                g2TurnEdges(edge, move),
                g2TurnTris(tri, move),
                FtoCoord.g2TurnTriple(tp0, move),
                FtoCoord.g2TurnTriple(tp1, move),
                FtoCoord.g2TurnTriple(tp2, move),
                FtoCoord.g2TurnTriple(tp3, move)
            );

            if (res)
                return true;
        }

        return false;
    }

    public void searchPhaseOne(int depth, int maxl, int edge, int tri, ArrayList<int[]> candidates){

        if (FtoCoord.isPhaseOne(edge, tri)){
            candidates.add(Arrays.copyOf(moves, maxl));
            return;
        }

        if (depth < FtoCoord.g1PrunEdge(edge))
            return;

        if (depth <= 0){
            return;
        }

        for (int move = 0; move < 16; move++) {

            if (maxl > 0) {
                int la = moves[maxl - 1] / 2;
                int ca = move / 2;
                if (((invalidMoves[la] >> ca) & 1) == 1) continue;
            }

            moves[maxl] = move;

            searchPhaseOne(depth-1, maxl+1,
                FtoCoord.g1TurnEdges(edge, move), FtoCoord.g1TurnTriangles(tri, move), candidates);

        }
    }

    public ArrayList<int[]> iteratePhaseOne(FtoCubie cubie){

        ArrayList<int[]> candidates = new ArrayList<>();

        int edge = cubie.packG1Edges();
        int tri = cubie.packG1Triangles();
        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            searchPhaseOne(depth, 0, edge, tri, candidates);
            if (candidates.size() >= 500) break;
        }

        return candidates;
    }

    public int[] iteratePhaseTwo(FtoCubie cubie){
        int edge = cubie.packG2Edges();
        int tri = cubie.packG2Tris();
        int tp0 = cubie.packG2Triples(0);
        int tp1 = cubie.packG2Triples(1);
        int tp2 = cubie.packG2Triples(2);
        int tp3 = cubie.packG2Triples(3);

        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            boolean res = searchPhaseTwo(depth, 0, edge, tri, tp0, tp1, tp2, tp3);
            if (res){
                return Arrays.copyOf(moves, depth);
            }
        }

        throw new RuntimeException("Could not find Phase 2 solution");
    }

    private boolean searchPhaseThree(int depth, int maxl, int edge, int corner){

        if (isSolvedG3Corners(corner) &&
            isSolvedG3Edges(edge)){

            return true;
        }

        if (depth == 0){
            return false;
        }

        for (int move = 0; move < 10; move++) {
            if (move == FtoCubie.U || move == FtoCubie.UP)
                continue;

            if (maxl > 0) {
                int la = moves[maxl - 1] / 2;
                int ca = move / 2;
                if (((invalidMoves[la] >> ca) & 1) == 1) continue;
            }

            moves[maxl] = move;

            boolean res = searchPhaseThree(depth-1, maxl+1,
                g3TurnEdge(edge, move),
                g3TurnCorner(corner, move));

            if (res)
                return true;
        }

        return false;
    }

    public int[] iteratePhaseThree(FtoCubie cubie){

        int edge = cubie.packG3Edges();
        int corner = cubie.packG3Corners();

        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            boolean res = searchPhaseThree(depth, 0, edge, corner);
            if (res){
                return Arrays.copyOf(moves, depth);
            }
        }

        throw new RuntimeException();
    }

    public synchronized String solution(FtoCubie RANDOM_STATE){

        if (!FtoCoord.getInitialized())
            FtoCoord.init();

        if (initialized == false)
            initCenterPruning();

        long start = System.currentTimeMillis();

        nodes = new long[24];

        ArrayList<int[]> candidates = iteratePhaseOne(RANDOM_STATE);
        int[] g1sol = candidates.get(0);

        System.out.println("Phase 1 time: " + (System.currentTimeMillis() - start));

        FtoCubie fto = new FtoCubie(RANDOM_STATE);
        for (int move : candidates.get(0)){
            fto = fto.turn(move);
        }

        int[] g2sol = iteratePhaseTwo(fto);

        for (int move : g2sol){
            fto = fto.turn(move);
        }

        System.out.println("Phase 2 time: " + (System.currentTimeMillis() - start));

        int[] g3sol = iteratePhaseThree(fto);

        System.out.println("Phase 3 time: " + (System.currentTimeMillis() - start));

        return Util.moveArrayToString(g1sol) + " " + Util.moveArrayToString(g2sol) + " " + Util.moveArrayToString(g3sol);
    }

    private static final int[] invalidMoves;

    private static int[] initInvalidMoveTable() {
        int numAxes = 8;
        int[] invalid = new int[numAxes];

        for (int a1 = 0; a1 < numAxes; a1++) {
            invalid[a1] = 1 << a1;
            for (int a2 = 0; a2 < a1; a2++) {
                FtoCubie fto1 = new FtoCubie().turn(a1 * 2).turn(a2 * 2);
                FtoCubie fto2 = new FtoCubie().turn(a2 * 2).turn(a1 * 2);
                if (fto1.equals(fto2)) {
                    invalid[a1] |= 1 << a2;
                }
            }
        }
        return invalid;
    }

    static {
        invalidMoves = initInvalidMoveTable();
    }

    private static HashMap<Integer, Integer> centerPrun;

    private static void centerPruningSearch(int depth, int maxl, int lm, int edge, int tri){

        int key = edge | (tri << 19);

        if (centerPrun.containsKey(key)){
            centerPrun.put(key, Math.min(centerPrun.get(key), depth));
        } else {
            centerPrun.put(key, depth);
        }

        if (depth == 0){
            return;
        }

        for (int move = 0; move < 10; move++) {
            if (maxl > 0) {
                int la = lm / 2;
                int ca = move / 2;
                if (((invalidMoves[la] >> ca) & 1) == 1) continue;
            }

            centerPruningSearch(depth-1, maxl+1, move,
                g2TurnEdges(edge, move),
                g2TurnTris(tri, move));

        }
    }

    public static int prunG2Centers(int edge, int tri){
        int key = edge | (tri << 19);
        if (!centerPrun.containsKey(key))
            return Integer.MAX_VALUE;

        return centerPrun.get(key);
    }

    private static void initCenterPruning(){
        centerPrun = new HashMap<>();

        for (Integer edge : PHASE_TWO_SOLVED_EDGES) {
            centerPruningSearch(7, 0, 0, edge, 0);
        }

        initialized = true;
    }


    public static void main(String[] args) {
        Search search = new Search();
        FtoCubie rs = Util.applyAlg("L' D' R' L R D B' D U R' D U' R U' R D B L U F' R L' F' BR B BR");
        System.out.println(search.solution(rs));
    }

}
