package levigibson.fto3phase;

import java.util.ArrayList;
import java.util.Arrays;

public class Search {

    public Search(){
        moves = new int[64];
    }

    private int[] moves;

    public void searchPhaseOne(int depth, int maxl, int edge, int tri, ArrayList<int[]> candidates){

        if (FtoCoord.isPhaseOne(edge, tri)){
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
                FtoCoord.turnG1Edges(edge, move), FtoCoord.turnG1Triangles(tri, move), candidates);

        }
    }

    public ArrayList<int[]> iteratePhaseOne(FtoCubie cubie){

        ArrayList<int[]> candidates = new ArrayList<>();

        int edge = cubie.packPhaseOneEdges();
        int tri = cubie.packPhaseOneTriangles();
        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            searchPhaseOne(depth, 0, edge, tri, candidates);
            if (candidates.size() >= 500) break;
        }

        return candidates;
    }

    public synchronized String solution(FtoCubie RANDOM_STATE){
        FtoCoord.init();

        ArrayList<int[]> candidates = iteratePhaseOne(RANDOM_STATE);

        return "";
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


    public static void main(String[] args) {
        Search search = new Search();
        FtoCubie rs = Util.applyAlg("L' D L' D' R' L' D' L' U B R' L U' R' U' L' B U R D R BL' U' B R' BR B'");
        search.solution(rs);


    }

}
