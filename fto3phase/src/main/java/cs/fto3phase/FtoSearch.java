package cs.fto3phase;

import java.util.HashMap;

import static cs.fto3phase.FullFto.Move;

public class FtoSearch {


    private String[] solution;

    private static final int PHASE_ONE_PRUNING_DEPTH = 4;
    private static final int PHASE_TWO_PRUNING_DEPTH = 7;
    private static final int PHASE_THREE_PRUNING_DEPTH = 5;
    private static HashMap<Long, Integer>[] pruningTables;

    public FtoSearch(){
        solution = new String[3];
    }

    private static void pruningSearch(int depth, FullFto fto, int phase, Move[] generator){

        long hash = fto.hash(phase);
        Integer lookup = pruningTables[phase].get(hash);

        if (lookup == null || fto.historyLength() < lookup){
            pruningTables[phase].put(hash, fto.historyLength());
        }

        if (depth == 0){
            return;
        }

        for (Move move : generator){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            pruningSearch(depth-1, fto, phase, generator);
            fto.undo();
        }
    }

    private static void phaseTwoPruningSearch(int depth, FullFto fto){

    }

    private static final Move[] PHASE_TWO_MOVES = {Move.U, Move.R, Move.L, Move.D, Move.B, Move.UP, Move.RP, Move.LP, Move.DP, Move.BP};
    private static final Move[] PHASE_THREE_MOVES = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

    static{
        pruningTables = new HashMap[3];
        pruningTables[0] = new HashMap<Long, Integer>();
        pruningTables[1] = new HashMap<Long, Integer>();
        pruningTables[2] = new HashMap<Long, Integer>();

        long startTime = System.nanoTime();

        pruningSearch(PHASE_ONE_PRUNING_DEPTH, new FullFto(), 0, Move.values());
//        pruningSearch(PHASE_TWO_PRUNING_DEPTH, new FullFto(), 1, PHASE_TWO_MOVES);
//        pruningSearch(PHASE_THREE_PRUNING_DEPTH, new FullFto(), 2, PHASE_THREE_MOVES);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime); // total time in nanoseconds

        System.out.println("Pruning table generation time: " + (duration / 1_000_000) + " ms");

    }


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

        if (depth <= PHASE_ONE_PRUNING_DEPTH){
            Integer lookup = pruningTables[0].get(fto.phaseOneHash());
            if (lookup == null || lookup > depth){
                return false;
            }
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

    private boolean searchPhaseTwo(int depth, FullFto fto){
        if (fto.isPhaseTwo()){
            System.out.print("Found Phase 2 Solution: ");
            System.out.println(fto.history());
            solution[1] = fto.history();

            return true;
        }

        if (depth == 0){
            return false;
        }

        if (depth <= PHASE_TWO_PRUNING_DEPTH){
            Integer lookup = pruningTables[1].get(fto.phaseTwoCentersHash());
            if (lookup == null || lookup > depth){
                return false;
            }
        }

        if (depth < 3 && fto.tripleCount() < 3){
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

        long startTime = System.nanoTime();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseOne(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime); // total time in nanoseconds

        System.out.println("Phase 1 search time: " + (duration / 1_000_000) + " ms");

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
        fto.parseAlg("R' B' D' B L D B L BR R BR D BR R' BR' R D L D B U' R L' U' BR D' BL");
        fto.clearMoveStack();
        FtoSearch search = new FtoSearch();
        search.solution(fto);
    }
}
