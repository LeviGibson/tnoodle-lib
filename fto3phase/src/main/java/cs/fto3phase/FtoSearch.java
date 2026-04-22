package cs.fto3phase;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static cs.fto3phase.FullFto.Move;

public class FtoSearch {


    String[] solution;

    public FtoSearch(){
        solution = new String[3];
    }



//    void phaseThreePruningSearch(int depth, FullFto fto){
//        if (depth == 0){
//            return;
//        }
//
//        for (Move move : PHASE_THREE_MOVES){
//            if (fto.isRepetition(move))
//                continue;
//
//            fto.turn(move);
//            phaseThreePruningSearch(depth-1, fto);
//            fto.undo();
//        }
//    }
//
//    static{
//
//    }

    private boolean searchPhaseOne(int depth, FullFto fto){
        if (fto.isPhaseOne()){
            System.out.print("Found Phase 1 Solution: ");
            System.out.println(fto.history());
            solution[0] = fto.history();
            return true;
        }

        if (depth == 0){
            return false;
        }

        for (FullFto.Move move : FullFto.Move.values()){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseOne(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    private Move[] PHASE_TWO_MOVES = {Move.U, Move.R, Move.L, Move.D, Move.B, Move.UP, Move.RP, Move.LP, Move.DP, Move.BP};
    private Move[] PHASE_THREE_MOVES = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

    private boolean searchPhaseTwo(int depth, FullFto fto){
        if (fto.isPhaseTwo()){
            System.out.print("Found Phase 2 Solution: ");
            System.out.println(fto.history());
            solution[1] = fto.history();

            return true;
        }

        if (depth < 3 && fto.tripleCount() < 3){
            return false;
        }

        if (depth == 0){
            return false;
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseTwo(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    private boolean searchPhaseThree(int depth, FullFto fto){
        if (fto.isSolved()){
            System.out.print("Found Phase 3 Solution: ");
            solution[2] = fto.history();
            System.out.println(fto.history());
            return true;
        }

        if (depth == 0){
            return false;
        }

        for (Move move : PHASE_THREE_MOVES){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseThree(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }


    public String solution(FullFto fto){

        fto.clearMoveStack();
        solution = new String[3];

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseOne(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        fto.parseAlg(solution[0]);
        fto.clearMoveStack();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseTwo(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 2 solution");
            }
        }

        fto.parseAlg(solution[1]);
        fto.clearMoveStack();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseThree(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 2 solution");
            }
        }

        return solution[0] + solution[1] + solution[2];
    }

    public static void main(String[] args) {
        FullFto fto = new FullFto();
//        fto.parseAlg("B D R' B D' L' B R' U' L D' U' B U R D U' B' U' L R BL BR' BL U B U'");
        fto.parseAlg("R L D B D B L R R D' R' B' L R R L D B D B U R D L BL BR");
        fto.clearMoveStack();
        FtoSearch search = new FtoSearch();
        search.solution(fto);
    }
}
