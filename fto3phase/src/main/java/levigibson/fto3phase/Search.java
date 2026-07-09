package levigibson.fto3phase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static levigibson.fto3phase.FtoCoord.*;

public class Search {

    public boolean validateSolution(String moves, FtoCubie RANDOM_STATE){
        FtoCubie test = Util.applyAlg(moves);
        return (RANDOM_STATE.equals(test));
    }

    public synchronized String solution(FtoCubie RANDOM_STATE){
        if (!FtoCoord.getInitialized())
            FtoCoord.init();

        ArrayList<int[]> candidates = g1Iterate(RANDOM_STATE);
        FtoCubie fto = new FtoCubie(RANDOM_STATE);

        int[] g2sol = g2Iterate(fto, candidates);
        fto = fto.fromMoves(g2sol);
        int[] g3sol = g3Iterate(fto);

        int[] fullSolution = IntStream.concat(Arrays.stream(g2sol), Arrays.stream(g3sol)).toArray();
        String fullSolutionStr = Util.moveArrayToInvertedString(fullSolution);

        if (!validateSolution(fullSolutionStr, RANDOM_STATE)){
            throw new RuntimeException("CRITICAL: Found solution does not match random state");
        }

        return fullSolutionStr;
    }

    private static final int[] G1_MOVESET = {FtoCubie.R, FtoCubie.RP,
        FtoCubie.L, FtoCubie.LP, FtoCubie.B, FtoCubie.BP,
        FtoCubie.U, FtoCubie.UP, FtoCubie.D, FtoCubie.DP,
        FtoCubie.F, FtoCubie.FP, FtoCubie.BR, FtoCubie.BRP};
    private static final int[] G2_MOVESET = {FtoCubie.R, FtoCubie.RP,
        FtoCubie.L, FtoCubie.LP, FtoCubie.B, FtoCubie.BP,
        FtoCubie.U, FtoCubie.UP, FtoCubie.D, FtoCubie.DP};
    private static final int[] G3_MOVESET = {FtoCubie.R, FtoCubie.RP,
        FtoCubie.L, FtoCubie.LP, FtoCubie.B, FtoCubie.BP,FtoCubie.D, FtoCubie.DP};

    public Search(){
        moves = new int[64];
    }

    private final int[] moves;

    private boolean g2Search(int depth, int maxl, int edge, int tri, int tp0, int tp1, int tp2, int tp3){

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

        for (int move : G2_MOVESET) {
            if (maxl > 0 && !isValidMove(moves[maxl-1], move)) {
                continue;
            }

            moves[maxl] = move;

            boolean res = g2Search(depth-1, maxl+1,
                g2TurnEdges(edge, move),
                g2TurnTris(tri, move),
                g2TurnTriples(tp0, move),
                g2TurnTriples(tp1, move),
                g2TurnTriples(tp2, move),
                g2TurnTriples(tp3, move)
            );

            if (res)
                return true;
        }

        return false;
    }

    private void g1Search(int depth, int maxl, int edge, int tri, ArrayList<int[]> candidates){

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

        for (int move : G1_MOVESET) {

            if (maxl > 0 && !isValidMove(moves[maxl-1], move)) {
                continue;
            }

            moves[maxl] = move;

            g1Search(depth-1, maxl+1,
                FtoCoord.g1TurnEdges(edge, move), FtoCoord.g1TurnTris(tri, move), candidates);

        }
    }

    private ArrayList<int[]> g1Iterate(FtoCubie cubie){

        ArrayList<int[]> candidates = new ArrayList<>();

        int edge = cubie.g1PackEdges();
        int tri = cubie.g1PackTriangles();
        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            g1Search(depth, 0, edge, tri, candidates);
            if (candidates.size() >= 500) break;
        }

        return candidates;
    }

    private static int[][] buildStatesFromCandidates(FtoCubie cubie, ArrayList<int[]> candidates){
        int[][] states = new int[candidates.size()][6];
        FtoCubie fto = new FtoCubie(cubie);

        for (int i = 0; i < candidates.size(); i++) {
            int[] moves = candidates.get(i);

            FtoCubie candidateCubie = fto.fromMoves(moves);

            states[i][0] = candidateCubie.g2PackEdges();
            states[i][1] = candidateCubie.g2PackTris();
            states[i][2] = candidateCubie.g2PackTriples(0);
            states[i][3] = candidateCubie.g2PackTriples(1);
            states[i][4] = candidateCubie.g2PackTriples(2);
            states[i][5] = candidateCubie.g2PackTriples(3);
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

    private int[] g2Iterate(FtoCubie cubie, ArrayList<int[]> candidates){
        int[][] states = buildStatesFromCandidates(cubie, candidates);
        int[] lengths = buildLengthFromCandidates(candidates);

        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            for (int c = 0; c < states.length; c++) {

                int searchDepth = depth - lengths[c];
                if (searchDepth < 0) continue;

                int[] s = states[c];

                boolean res = g2Search(searchDepth, 0, s[0], s[1], s[2], s[3], s[4], s[5]);
                if (res){
                    int[] g1sol = candidates.get(c);
                    int[] g2sol = Arrays.copyOf(moves, searchDepth);

                    return IntStream.concat(Arrays.stream(g1sol), Arrays.stream(g2sol)).toArray();
                }
            }

        }

        throw new RuntimeException("Could not find Phase 2 solution");
    }

    private boolean g3Search(int depth, int maxl, int edges, int corners){
        int cornerPrun = FtoCoord.g3PrunCorners(corners);
        if (depth < cornerPrun) return false;

        if (corners == SOLVED_G3_CORNERS &&
            edges == SOLVED_G3_EDGES){

            return true;
        }

        if (depth == 0){
            return false;
        }

        for (int move : G3_MOVESET) {
            if (maxl > 0 && !isValidMove(moves[maxl-1], move)) {
                continue;
            }

            moves[maxl] = move;

            boolean res = g3Search(depth-1, maxl+1,
                g3TurnEdges(edges, move),
                g3TurnCorners(corners, move));

            if (res)
                return true;
        }

        return false;
    }

    private int[] g3Iterate(FtoCubie cubie){
        int edges = cubie.g3PackEdges();
        int corners = cubie.g3PackCorners();

        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            boolean res = g3Search(depth, 0, edges, corners);
            if (res){
                return Arrays.copyOf(moves, depth);
            }
        }

        throw new RuntimeException();
    }

    private static boolean isValidMove(int lastMove, int move){
        int la = lastMove / 2;
        int ca = move / 2;
        return ((invalidMoves[la] >> ca) & 1) != 1;
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
}
