package levigibson.fto3phase;

public class Search {

    public Search(){
        moves = new int[64];
    }

    private int[] moves;
    public void searchPhaseOne(int depth, int maxl, int edge, int tri){

        if (FtoCoord.isPhaseOne(edge, tri)){
            System.out.println(Util.moveArrayToString(moves, maxl));
        }

        if (depth <= 0){
            return;
        }

        for (int move = 0; move < 16; move++) {
            moves[maxl] = move;

            searchPhaseOne(depth-1, maxl+1,
                FtoCoord.turnG1Edges(edge, move), FtoCoord.turnG1Triangles(tri, move));

        }
    }

    public void iteratePhaseOne(FtoCubie cubie){
        int edge = cubie.packPhaseOneEdges();
        int tri = cubie.packPhaseOneTriangles();
        for (int depth = 0; depth < Integer.MAX_VALUE; depth++) {
            System.out.println("SD " + depth);
            searchPhaseOne(depth, 0, edge, tri);
        }
    }

    public synchronized String solution(FtoCubie RANDOM_STATE){
        FtoCoord.init();

        iteratePhaseOne(RANDOM_STATE);

        return "";
    }

    public static void main(String[] args) {
        Search search = new Search();
        FtoCubie rs = Util.applyAlg("L' D L' D' R' L' D' L' U B R' L U' R' U' L' B U R D R BL' U' B R' BR B'");
        search.solution(rs);


    }

}
