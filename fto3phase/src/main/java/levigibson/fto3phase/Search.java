package levigibson.fto3phase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static levigibson.fto3phase.FtoCoord.*;

public class Search {

    public Search(){
        moves = new int[64];
    }

    private int[] moves;

    public boolean searchPhaseTwo(int depth, int maxl, int edge, int tri, int tp0, int tp1, int tp2, int tp3){

        int triplePrun = FtoCoord.g2PrunTriple(tp0, tp1, tp2, tp3);
        if (depth < triplePrun)
            return false;

        int txEPrun = FtoCoord.g2PrunTxE(edge, tri);
        if (depth < txEPrun){
            return false;
        }

        if (triplePrun == 0 && txEPrun == 0){
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

        int prun = FtoCoord.g1Prun(edge, tri);

        if (depth < prun)
            return;

        if (prun == 0){
            candidates.add(Arrays.copyOf(moves, maxl));
            return;
        }

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

    private static int[][] buildStatesFromCandidates(FtoCubie cubie, ArrayList<int[]> candidates){
        int[][] states = new int[candidates.size()][6];

        for (int i = 0; i < candidates.size(); i++) {
            FtoCubie fto = new FtoCubie(cubie);
            int[] moves = candidates.get(i);

            for (int move : moves) {
                fto = fto.turn(move);
            }

            states[i][0] = fto.packG2Edges();
            states[i][1] = fto.packG2Tris();
            states[i][2] = fto.packG2Triples(0);
            states[i][3] = fto.packG2Triples(1);
            states[i][4] = fto.packG2Triples(2);
            states[i][5] = fto.packG2Triples(3);
        }

        return states;
    }

    private static int[] buildLengthFromCandidates(ArrayList<int[]> candidates){
        int[] lengths = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            lengths[i] = candidates.get(i).length;
        }
        return lengths;
    }

    public int[] iteratePhaseTwo(FtoCubie cubie, ArrayList<int[]> candidates){
        int[][] states = buildStatesFromCandidates(cubie, candidates);
        int[] lengths = buildLengthFromCandidates(candidates);

        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            for (int c = 0; c < states.length; c++) {

                int searchDepth = depth - lengths[c];
                if (searchDepth < 0) continue;

                int[] s = states[c];

                boolean res = searchPhaseTwo(searchDepth, 0, s[0], s[1], s[2], s[3], s[4], s[5]);
                if (res){
                    int[] g1sol = candidates.get(c);
                    int[] g2sol = Arrays.copyOf(moves, searchDepth);

                    return IntStream.concat(Arrays.stream(g1sol), Arrays.stream(g2sol)).toArray();
                }
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

        ArrayList<int[]> candidates = iteratePhaseOne(RANDOM_STATE);
        FtoCubie fto = new FtoCubie(RANDOM_STATE);

        int[] g2sol = iteratePhaseTwo(fto, candidates);
        for (int move : g2sol){ fto = fto.turn(move);}
        int[] g3sol = iteratePhaseThree(fto);

        int[] fullSolution = IntStream.concat(Arrays.stream(g2sol), Arrays.stream(g3sol)).toArray();

        return Util.moveArrayToInvertedString(fullSolution);
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

    static void performanceTest(int n){
        Random r = new Random(42);
        long start = System.currentTimeMillis();

        Search search = new Search();
        for (int i = 0; i < n; i++) {
            FtoCubie rs = FtoCubie.randomCube(r);
            System.out.println(search.solution(rs));
        }

        System.out.println((System.currentTimeMillis() - start) / n);
    }


    public static void main(String[] args) {
        performanceTest(500);
//        Search search = new Search();
//        FtoCubie fto = Util.applyAlg("R' B L D' R L BR' B R' L BR D' R' B' BR' R D B D U' F' R' BL U L");
//        System.out.println(search.solution(fto));

    }

}
