package cs.fto3phase;

import  java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static cs.fto3phase.FullFto.Move;

/**
 * Three-phase IDA* solver for Face Turning Octahedron random-state scrambles.
 *
 * <p>The search first solves the D-face center and edge orbit, then reduces the
 * state to the Octaminx subgroup, and finally solves the reduced state.
 * Small phase-two pruning tables are loaded from bundled resources; larger pruning
 * tables are generated in memory during class initialization.</p>
 */
public class FtoSearch {

    //Depths at which the pruning tables are set to
    private static final int PHASE_ONE_PRUNING_DEPTH = 6;
    private static final int PHASE_TWO_PRUNING_DEPTH = 8;
    private static final int PHASE_THREE_PRUNING_DEPTH = 4;


    /**
     * Important value!
     * The higher the value, the slower the search, and the shorter the solution
     */
    private static int PHASE_ONE_CANDIDATE_LIMIT = 1000;
    private static double PHASE_ONE_CANDIDATE_THREASHOLD = 0.1;
    private static final int PHASE_TWO_CANDIDATE_LIMIT = 50;

    //Pruning tables
    private static HashMap<Long, Integer> phaseOnePruningTable;
    private static HashMap<Long, HashSet<Long>> phaseTwoPruningTable;
    private static HashMap<Long, Integer> phaseThreePruningTable;

    //--------------- Static Pruning Table Generation ---------------//

    /**
     * Betas for logistic regression model
     * This is used for pruning during phase two in depths 8-19
     */
    private static final double[][] BETAS = {
        {0, 0, 0}, // 0
        {0, 0, 0}, // 1
        {0, 0, 0}, // 2
        {0, 0, 0}, // 3
        {0, 0, 0}, // 4
        {0, 0, 0}, // 5
        {0, 0, 0}, // 6
        {0, 0, 0}, // 7
        {2.60980, 1.39171, -1.08659, -0.67610}, // 8
        {5.02109, 1.13821, -1.10259, -0.73025}, // 9
        {2.54998, 1.23541, -0.90102, -0.54790}, // 10
        {2.56226, 1.07618, -0.72079, -0.56310}, // 11
        {-0.17529, 1.25463, -0.65238, -0.32376}, // 12
        {1.76425, 1.00602, -0.53846, -0.50472}, // 13
        {-0.23506, 1.08614, -0.54071, -0.23815}, // 14
        {1.47921, 0.97283, -0.56099, -0.36157}, // 15
        {1.89530, 0.80994, -0.43749, -0.42357}, // 16
        {-0.03786, 0.92725, -0.31888, -0.31122}, // 17
        {0.21819, 0.88277, -0.37334, -0.24598}, // 18
        {0.64805, 0.78102, -0.30848, -0.29557}, // 19
    };

    /**
     * Thresholds to prune at for logistic pruning index=depth
     */
    public static double[] THRESHOLDS = {
        0.986641027960064,
        0.961649420842541,
        0.894885223305345,
        0.742958928062686,
        0.495291928080875,
        0.249914082445017,
        0.101624061096067,
        0.036985375660089,
        0.012871511674730,
        0.004407539222975,
        0.001500793803529,
        0.000510047301430,
    };

    /**
     * Moves allowed during Phase 1 -> Phase 2
     */
    public static final Move[] PHASE_TWO_MOVES = {Move.U, Move.UP, Move.R, Move.L, Move.B, Move.RP, Move.LP, Move.BP, Move.DP, Move.D};
    /**
     * Moves allowed during Phase 2 -> Phase 3
     */
    public static final Move[] PHASE_THREE_MOVES = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

    private static byte[] phaseTwoEdgePruningTable;
    private static byte[] phaseTwoCenterPruningTable;
    private static byte[] phaseTwoTriplePruningTable;



    /**
     * Saves .dat files in main/resources
     * @param table table
     * @param filename name of file
     * @throws IOException exception
     */
    private static void saveTable(byte[] table, String filename) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(table);
        }
    }

    /**
     * Loads .dat files in main/resources
     * @param filename name of file
     * @param size size of file in bytes
     * @return array in bytes
     * @throws IOException exception
     */
    private static byte[] loadTable(String filename, int size) throws IOException {
        try (InputStream is = FtoSearch.class.getResourceAsStream("/" + filename);
             BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] table = new byte[size];
            bis.read(table);
            return table;
        }
    }

    /**
     * This is a function that generates edgeprun.dat
     * Don't run this function unless you want to wait for several hours
     */
    private static void phaseTwoEdgePruningSearch(int depth, FullFto fto){

        if (depth == 0)
            return;

        int edgeIndex = fto.phaseTwoEdgeIndex();
        int ply = fto.historyLength();

        if (phaseTwoEdgePruningTable[edgeIndex] > ply){
            phaseTwoEdgePruningTable[edgeIndex] = (byte)ply;
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            if (move == Move.D || move == Move.DP)
                continue;

            if (ply == 0 && (move == Move.R || move == Move.RP || move == Move.L || move == Move.LP || move == Move.B || move == Move.BP))
                continue;

            fto.turn(move);
            phaseTwoEdgePruningSearch(depth-1, fto);
            fto.undo();
        }

    }

    /**
     * This is a function that generates edgeprun.dat
     * Don't run this function unless you want to wait for several hours
     */
    private static void generateEdgePruning(){

        for (int i = 0; i < 362880/2; i++) {
            phaseTwoEdgePruningTable[i] = 25;
        }

        FullFto[] angles = new FullFto[3*3*3];
        int anglesFound = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    angles[anglesFound] = new FullFto();

                    for (int l = 0; l < i; l++) {
                        angles[anglesFound].turn(Move.R);
                    }
                    for (int l = 0; l < j; l++) {
                        angles[anglesFound].turn(Move.L);
                    }
                    for (int l = 0; l < k; l++) {
                        angles[anglesFound].turn(Move.B);
                    }

                    anglesFound++;
                }
            }
        }

        for (int depth = 0; depth < 20; depth++) {
            System.out.println("Searching depth " + Integer.toString(depth));

            for (int i = 0; i < 3*3*3; i++) {
                angles[i].clearMoveStack();
                phaseTwoEdgePruningSearch(depth, angles[i]);
            }

            int capacity = 362880/2;
            for (int i = 0; i < 362880/2; i++) {
                if (phaseTwoEdgePruningTable[i] != 25){
                    capacity--;
                }
            }

            System.out.println("Left: " + Integer.toString(capacity));

            try {
                saveTable(phaseTwoEdgePruningTable, "fto3phase/src/resources/edgeprun.dat");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static{
        try {
            phaseTwoTriplePruningTable = loadTable("triple_d10.dat", 2*2*2*2*2*2*2*2*2*2*2*2);
            phaseTwoEdgePruningTable = loadTable("edgeprun.dat", 362880/2);
            phaseTwoCenterPruningTable = loadTable("centerprun.dat", 1680);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 362880/2; i++) {
            if (phaseTwoEdgePruningTable[i] == 24){
                phaseTwoEdgePruningTable[i] = 11;
            }
        }
    }

    /**
     * Helper function for generating pruning tables
     * This is not called in the actual search, just called once on program startup.
     * @param depth depth
     * @param fto fto
     */
    private static void phaseOnePruningSearch(int depth, FullFto fto){

        long hash = fto.phaseOneHash();
        Integer lookup = phaseOnePruningTable.get(hash);

        if (lookup == null || fto.historyLength() < lookup){
            phaseOnePruningTable.put(hash, fto.historyLength());
        }

        if (depth == 0){
            return;
        }

        for (Move move : Move.values()){
            if (fto.isRepetition(move))
                continue;

            if (fto.historyLength() == 0 && move != Move.F && move != Move.FP &&
                                            move != Move.BR && move != Move.BRP &&
                                            move != Move.BL && move != Move.BLP)
                continue;

            fto.turn(move);
            phaseOnePruningSearch(depth-1, fto);
            fto.undo();
        }
    }


    /**
     * Helper function for generating pruning tables
     * This is not called in the actual search, just called once on program startup.
     * This table is saved to the user's home folder after it is generated
     * @param depth depth
     * @param fto fto
     */
    private static void phaseTwoPruningSearch(int depth, FullFto fto){
        long centerHash = fto.phaseTwoCentersHash();
        long triples = fto.packPhaseTwoTripleData();
        HashSet<Long> lookup = phaseTwoPruningTable.get(centerHash);

        if (lookup == null){
            HashSet<Long> entry = new HashSet<>();
            entry.add(triples);
            phaseTwoPruningTable.put(centerHash, entry);
        } else {
            lookup.add(triples);
        }

        if (depth == 0){
            return;
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            //Don't look at moves that don't break phase 2 as a first move
            if (fto.historyLength() == 0 && move != Move.U && move != Move.UP){
                continue;
            }

            if (fto.historyLength() == 1 && (move == Move.D || move == Move.DP)){
                continue;
            }

            fto.turn(move);
            phaseTwoPruningSearch(depth-1, fto);
            fto.undo();
        }
    }


    /**
     * Helper function for generating pruning tables
     * This is not called in the actual search, just called once on program startup.
     * @param depth depth
     * @param fto fto
     */
    private static void phaseThreePruningSearch(int depth, FullFto fto){

        long hash = fto.phaseThreeHash();
        Integer lookup = phaseThreePruningTable.get(hash);

        if (lookup == null || fto.historyLength() < lookup){
            phaseThreePruningTable.put(hash, fto.historyLength());
        }

        if (depth == 0){
            return;
        }

        for (Move move : PHASE_THREE_MOVES){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            phaseThreePruningSearch(depth-1, fto);
            fto.undo();
        }
    }

    //Generate pruning tables (2000ms on first program run depending on your machine)
    static{
        phaseOnePruningTable = new HashMap<Long, Integer>();
        phaseThreePruningTable = new HashMap<Long, Integer>();

        phaseOnePruningSearch(PHASE_ONE_PRUNING_DEPTH, new FullFto());
        long startTime = System.currentTimeMillis();
        phaseTwoPruningTable = new HashMap<Long, HashSet<Long>>();
        phaseTwoPruningSearch(PHASE_TWO_PRUNING_DEPTH, new FullFto());
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Total time (ms): " + totalTime);
        phaseThreePruningSearch(PHASE_THREE_PRUNING_DEPTH, new FullFto());
//        System.exit(0);
    }

    /**
     * An instance of this class is passed to the search and used for storing solutions
     */
    private static class State{
        String[] solution = new String[3];
    }

    private static class PhaseTwoCandidate {
        FullFto phaseOneSolution;
        FullFto phaseTwoSolution;

        PhaseTwoCandidate(FullFto phaseOneSolution, FullFto phaseTwoSolution) {
            this.phaseOneSolution = phaseOneSolution;
            this.phaseTwoSolution = phaseTwoSolution;
        }
    }


    /**
     * Simple IDA* algorithm for solving Phase 1 (AKA. the bottom center)
     * @param depth depth
     * @param fto fto
     * @return Found solution? t/f
     */
    private boolean searchPhaseOne(int depth, State state, ArrayList<FullFto> candidates, FullFto fto){
        nodes++;

        if (fto.isPhaseOne()){
            Move lastMove = fto.moveStack.peek();
            Move lastLastMove = fto.moveStack.get(fto.moveStack.size() - 2);
            //Nodes % n == 0
            //The performance of the solver is significantly better
            //when the phase 1 candidates are "spread out" (not too similar to each other)
            //so Nodes % n == 0 is a pseudo-random number generator which does that.
            double p = logisticRegression(fto, 19-fto.historyLength());

            if (fto.isValidPhaseOneFinishingSequence(lastMove, lastLastMove) && p  > PHASE_ONE_CANDIDATE_THREASHOLD){
                state.solution[0] = fto.history();
                candidates.add(new FullFto(fto));
            }

            return candidates.size() >= PHASE_ONE_CANDIDATE_LIMIT;
        }

        if (depth == 0){
            return false;
        }

        if (depth <= PHASE_ONE_PRUNING_DEPTH){
            Integer lookup = phaseOnePruningTable.get(fto.phaseOneHash());
            if (lookup == null || lookup > depth){
                return false;
            }
        }

        for (FullFto.Move move : FullFto.Move.values()){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseOne(depth-1, state, candidates, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    int nodes;

    /**
     * IDA* recursive function for finding solutions for Phase 1 -> Phase 2
     * AKA. Bottom center solved -> Octaminx reduction
     * This step takes a lot of moves, so it is pruned rather aggressively
     * @param depth depth
     * @param fto fto
     * @return Found solution? t/f
     */
    private boolean searchPhaseTwo(int depth, State state, ArrayList<FullFto> candidates, FullFto fto){
        nodes++;

        if (fto.isPhaseTwo()){
            state.solution[1] = fto.history();
            candidates.add(new FullFto(fto));
            return candidates.size() >= PHASE_TWO_CANDIDATE_LIMIT;
        }

        if (depth == 0){
            return false;
        }

        int edgeLookup = (int)(phaseTwoEdgePruningTable[fto.phaseTwoEdgeIndex()]);
        if (edgeLookup > depth) {
            return false;
        }

        if (depth < 10) {
            int centerLookup = (int) (phaseTwoCenterPruningTable[fto.phaseTwoCenterIndex()]);
            if (centerLookup > depth) {
                return false;
            }
        }

        int ply = fto.historyLength();
        //Logistic Regression model determines the likelihood of the current subtree having a solution
        //Subtrees that are unlikely to have a solution are cut
        if (depth > PHASE_TWO_PRUNING_DEPTH && depth < 20 && ply > 0){

            int tripleLookup = (int)(phaseTwoTriplePruningTable[fto.phaseTwoTripleIndex()]);


            if (tripleLookup == 25)
                tripleLookup = 10;

            double p = logisticRegression(depth, fto, edgeLookup, tripleLookup);
            if (p < THRESHOLDS[depth-8])
                return false;
        }

        //IDA* lookup
        if (depth <= PHASE_TWO_PRUNING_DEPTH) {
            HashSet<Long> lookup = phaseTwoPruningTable.get(fto.phaseTwoCentersHash());
            if (lookup == null) {
                return false;
            } else {
                boolean hashHit = false;

                for (long triples : lookup) {
                    if (fto.checkPhaseTwoTripleData(triples)) {
                        hashHit = true;
                        break;
                    }
                }

                if (!hashHit) {
                    return false;
                }
            }
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseTwo(depth-1, state, candidates, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    private static double logisticRegression(int depth, FullFto fto, int edgeLookup, int tripleLookup) {
        double logOdds = BETAS[depth][0] + BETAS[depth][1] * (double) fto.triplePairCount() + BETAS[depth][2] * (double)edgeLookup + BETAS[depth][3] * (double)tripleLookup;

        double odds = Math.pow(2.71828182846, logOdds);

        return odds/(1+odds);
    }

    private static double logisticRegression(FullFto fto, int depth) {
        int tripleLookup = phaseTwoTriplePruningTable[fto.phaseTwoTripleIndex()];
        if (tripleLookup == 25)
            tripleLookup = 10;
        return logisticRegression(depth, fto, phaseTwoEdgePruningTable[fto.phaseTwoEdgeIndex()], tripleLookup);
    }

    /**
     * IDA* Search algorithm for phase 3
     * @param depth depth
     * @param fto fto
     * @return found solution t/f
     */
    private boolean searchPhaseThree(int depth, State state, FullFto fto){
        nodes++;

        if (fto.isSolved()){
            state.solution[2] = fto.history();
            return true;
        }

        if (depth == 0){
            return false;
        }

        if (depth <= PHASE_THREE_PRUNING_DEPTH){
            Integer lookup = phaseThreePruningTable.get(fto.phaseThreeHash());
            if (lookup == null || depth < lookup){
                return false;
            }
        }

        for (Move move : PHASE_THREE_MOVES){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseThree(depth-1, state, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    private int algLen(String alg){
        if (alg == null || alg.trim().isEmpty()) {
            return 0;
        }
        return alg.trim().split("\\s+").length;
    }

    private ArrayList<FullFto> solvePhaseOneCandidates(FullFto fto) {
        State state = new State();
        ArrayList<FullFto> candidates = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        //Run IDA* search for phase 1
        for (int depth = 0; depth < 100; depth++) {
            boolean foundEnoughSolutions = searchPhaseOne(depth, state, candidates, fto);

            if (foundEnoughSolutions)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
//        System.out.println("FTO Phase 1 solution time: " + totalTime + " ms");

        return candidates;
    }

    private ArrayList<PhaseTwoCandidate> solvePhaseTwoCandidates(ArrayList<FullFto> candidates) {
        State state = new State();
        ArrayList<PhaseTwoCandidate> phaseTwoCandidates = new ArrayList<>();

        for (int depth = 0; depth < 100; depth++) {
            for (FullFto phaseOneCandidate : candidates){
                int depthForSearch = Math.max(0, depth-phaseOneCandidate.historyLength());

                if (depthForSearch == 0)
                    continue;

                FullFto copy = new FullFto(phaseOneCandidate);
                copy.clearMoveStack();

                ArrayList<FullFto> phaseTwoCandidateSolutions = new ArrayList<>();
                boolean foundEnoughSolutions = searchPhaseTwo(depthForSearch, state, phaseTwoCandidateSolutions, copy);

                for (FullFto phaseTwoCandidate : phaseTwoCandidateSolutions) {
                    phaseTwoCandidates.add(new PhaseTwoCandidate(phaseOneCandidate, phaseTwoCandidate));
                    if (phaseTwoCandidates.size() >= PHASE_TWO_CANDIDATE_LIMIT) {
                        break;
                    }
                }

                if (foundEnoughSolutions || phaseTwoCandidates.size() >= PHASE_TWO_CANDIDATE_LIMIT) {
                    break;
                }
            }

            if (phaseTwoCandidates.size() >= PHASE_TWO_CANDIDATE_LIMIT)
                break;

            if (depth == 99 && phaseTwoCandidates.isEmpty()){
                throw new RuntimeException("Could not find FTO Phase 2 solution");
            }
        }

        return phaseTwoCandidates;
    }

    private String solvePhaseThreeBestCandidate(ArrayList<PhaseTwoCandidate> phaseTwoCandidates) {
        String bestSolution = null;
        int bestSolutionLength = Integer.MAX_VALUE;

        for (PhaseTwoCandidate candidate : phaseTwoCandidates) {
            FullFto copy = new FullFto(candidate.phaseTwoSolution);
            copy.clearMoveStack();

            State candidateState = new State();
            candidateState.solution[0] = candidate.phaseOneSolution.history();
            candidateState.solution[1] = candidate.phaseTwoSolution.history();

            //IDA* search for phase 3
            for (int depth = 0; depth < 100; depth++) {
                boolean foundSolution = searchPhaseThree(depth, candidateState, copy);

                if (foundSolution)
                    break;

                if (depth == 99){
                    throw new RuntimeException("Could not find FTO Phase 3 solution");
                }
            }

            String candidateSolution = candidateState.solution[0] + candidateState.solution[1] + candidateState.solution[2];
            int candidateSolutionLength = algLen(candidateSolution);
            if (candidateSolutionLength < bestSolutionLength) {
                bestSolution = candidateSolution;
                bestSolutionLength = candidateSolutionLength;
            }
        }

        return bestSolution;
    }


    /**
     * Finds a solution for the supplied FTO state.
     *
     * @param fto state to solve
     * @return solution algorithm with TNoodle-compatible move names
     */
    public String solution(FullFto fto){
        fto = new FullFto(fto); // Make a copy
        nodes = 0;

        fto.clearMoveStack();

        long startTime = System.currentTimeMillis();
        ArrayList<FullFto> candidates = solvePhaseOneCandidates(fto);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
//        System.out.println("Phase 1 Time: " + totalTime + " ms");

        startTime = System.currentTimeMillis();
        ArrayList<PhaseTwoCandidate> phaseTwoCandidates = solvePhaseTwoCandidates(candidates);
        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;
//        System.out.println("Phase 2 Time: " + totalTime + " ms");

        startTime = System.currentTimeMillis();
        String bestSolution = solvePhaseThreeBestCandidate(phaseTwoCandidates);
        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;
//        System.out.println("Phase 3 Time: " + totalTime + " ms");
        return invertSolution(bestSolution);
    }

    private String invertSolution(String s) {
        if (s == null || s.isEmpty()) return s;

        String[] moves = s.trim().split("\\s+");

        // Reverse the order, then toggle prime on each move
        String[] inverted = new String[moves.length];
        for (int i = 0; i < moves.length; i++) {
            String move = moves[moves.length - 1 - i];
            if (move.endsWith("'")) {
                inverted[i] = move.substring(0, move.length() - 1); // remove prime
            } else {
                inverted[i] = move + "'"; // add prime
            }
        }

        return String.join(" ", inverted);
    }

    /**
     * Helper function to genData
     * This is only called during development
     * @param fto
     * @param depth
     * @param label
     */
    private static void write(FullFto fto, int depth, boolean label){

        int tripleLookup = (int)(phaseTwoTriplePruningTable[fto.phaseTwoTripleIndex()]);
        if (tripleLookup == 25)
            tripleLookup = 10;

        System.out.print(fto.tripleCount());
        System.out.print(",");
        System.out.print(fto.triplePairCount());
        System.out.print(",");
        System.out.print((int)(phaseTwoEdgePruningTable[fto.phaseTwoEdgeIndex()]));
        System.out.print(",");
        System.out.print((int)(phaseTwoCenterPruningTable[fto.phaseTwoCenterIndex()]));
        System.out.print(",");
        System.out.print((int)(tripleLookup));
        System.out.print(",");
        System.out.print(depth);
        System.out.print(",");
        System.out.print(label);
        System.out.println();
    }


    /**
     * Generates training data for pruning model
     * This is only called during development
     */
    private static void genData(){

        Random r = new Random();

        for (int depth = 8; depth < 20; depth++) {
            for (int iter = 0; iter < 2000; iter++) {
                FullFto randomFto = new FullFto();
                FullFto closeFto = new FullFto();

                randomFto.scrambleRandomG2State(r);
                closeFto.scrambleRandomG2State(r, depth);

                write(randomFto, depth, false);
                write(closeFto, depth, true);
            }
        }

    }

    public static long performanceTest(int num){

        long totalTime = 0;
        long totalMoves = 0;
        long totalNodes = 0;

        FullFto fto;
        Random r = new Random();
        for (int i = 0; i < num; i++) {
            long startTime = System.nanoTime();

            fto = FullFto.randomCube(r);
            FtoSearch search = new FtoSearch();
            String s = search.solution(fto);
            totalNodes += (long)search.nodes;

            long endTime = System.nanoTime();
            long duration = (endTime - startTime); // total time in nanoseconds

            System.out.println("NEW," + (duration / 1_000_000));
            totalTime += duration;
            totalMoves += s.split(" ").length;
        }

        System.out.println("Average Moves: " + (double)totalMoves / (double)num);
        return totalNodes;
    }

        /**
     * Code for generating .dat files IN main/resources
     * These files are packaged with the build, so no need to keep this code in
    private static int found = 0;

    private static void phaseTwoTriplePruningSearch(int depth, FullFto fto){
        if (depth == 0)
            return;

        int ply = fto.historyLength();

        int index = fto.phaseTwoTripleIndex();

        if (phaseTwoTriplePruningTable[index] > ply){
            found++;
            phaseTwoTriplePruningTable[index] = (byte)ply;
            System.out.println(found);
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            if (ply == 0 && (move == Move.R || move == Move.RP || move == Move.L || move == Move.LP || move == Move.B || move == Move.BP))
                continue;

            fto.turn(move);
            phaseTwoTriplePruningSearch(depth-1, fto);
            fto.undo();
        }
    }

    static{
        for (int i = 0; i < 1000; i++) {
            FullFto fto = FullFto.randomCube(new Random());
            System.out.println(phaseTwoTriplePruningTable[fto.phaseTwoTripleIndex()]);
        }

        phaseTwoTriplePruningTable = new byte[2*2*2*2*2*2*2*2*2*2*2*2];
        Arrays.fill(phaseTwoTriplePruningTable, (byte) 25);
        for (int depth = 0; depth < 20; depth++) {
            System.out.println("Searching depth " + depth);
            phaseTwoTriplePruningSearch(depth, new FullFto());
            try {
                saveEdgeTable(phaseTwoTriplePruningTable, "depth " + depth + ".dat");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

     private static void phaseTwoCenterPruningSearch(int depth, FullFto fto){
        if (depth == 0)
            return;

        int centerIndex = fto.phaseTwoCenterIndex();
        int ply = fto.historyLength();

        if (phaseTwoCenterPruningTable[centerIndex] > ply){
            phaseTwoCenterPruningTable[centerIndex] = (byte)ply;
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            if (move == Move.D || move == Move.DP)
                continue;

            if (ply == 0 && (move == Move.R || move == Move.RP || move == Move.L || move == Move.LP || move == Move.B || move == Move.BP))
                continue;

            fto.turn(move);
            phaseTwoCenterPruningSearch(depth-1, fto);
            fto.undo();
        }
    }

    private static void generateCenterPruning(){
        phaseTwoCenterPruningTable = new byte[1680];

        for (int i = 0; i < 1680; i++) {
            phaseTwoCenterPruningTable[i] = (byte) 25;
        }

        for (int depth = 0; depth < 11; depth++) {
            System.out.println("Searching depth " + depth);
            phaseTwoCenterPruningSearch(depth, new FullFto());

            int remaining = 1680;
            for (int i = 0; i < 1680; i++) {
                if (phaseTwoCenterPruningTable[i] != 25)
                    remaining--;
            }
            System.out.println("Remaining:" + remaining);
        }

        try {
            saveTable(phaseTwoCenterPruningTable, "fto3phase/src/resources/centerprun.dat");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

     **/

    public static void main(String[] args) {
//        System.out.println("Starting FTO Search");
        performanceTest(100);
    }
}
