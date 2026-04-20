package cs.fto3phase;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class FtoSearch {

    String solution;

    private boolean searchPhaseOne(int depth, FullFto fto){
        if (fto.isPhaseOne()){
            System.out.print("Found Phase 1 Solution: ");
            System.out.println(fto.history());
            solution += fto.history();
        }

        if (depth == 0){
            return false;
        }

        for (FullFto.Move move : FullFto.Move.values()){
            fto.turn(move);
            boolean foundSolution = searchPhaseOne(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    public String solution(FullFto fto){

        fto.clearMoveStack();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Printing depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseOne(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        return "";
    }

    public static void main(String[] args) {
        FullFto fto = new FullFto();
        fto.parseAlg("B D R' B D' L' B R' U' L D' U' B U R D U' B' U' L R BL BR' BL U B U'");
        fto.clearMoveStack();
        FtoSearch search = new FtoSearch();
        search.solution(fto);
    }
}
