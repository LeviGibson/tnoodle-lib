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
    private static final int PHASE_ONE_PRUNING_DEPTH = 4;
    private static final int PHASE_TWO_PRUNING_DEPTH = 7;
    private static final int PHASE_THREE_PRUNING_DEPTH = 4;


    /**
     * Important value!
     * The higher the value, the slower the search, and the shorter the solution
     */
    private static int PHASE_ONE_CANDIDATE_LIMIT = 1000;

    //Pruning tables
    private static HashMap<Long, Integer> phaseOnePruningTable;
    private static HashMap<Long, LinkedList<PhaseTwoPruningEntry>> phaseTwoPruningTable;
    private static HashMap<Long, Integer> phaseThreePruningTable;


    /**
     * Contains all the data needed for a phase-2 hash lookup
     */
    private static class PhaseTwoPruningEntry{
        public int distanceToSolved;
        public long triples;
        public PhaseTwoPruningEntry(int distanceToSolved, long triples){
            this.distanceToSolved = distanceToSolved;
            this.triples = triples;
        }
    }

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
        LinkedList<PhaseTwoPruningEntry> lookup = phaseTwoPruningTable.get(centerHash);

        if (lookup == null){
            LinkedList<PhaseTwoPruningEntry> entry = new LinkedList<>();
            entry.add(new PhaseTwoPruningEntry(fto.historyLength(), triples));
            phaseTwoPruningTable.put(centerHash, entry);
        } else {
            boolean foundMatchingEntry = false;

            for (PhaseTwoPruningEntry e : lookup){
                if (e.triples == triples){
                    foundMatchingEntry = true;
                    if (e.distanceToSolved > fto.historyLength()){
                        e.distanceToSolved = fto.historyLength();
                    }
                }
            }

            if (!foundMatchingEntry){
                lookup.add(new PhaseTwoPruningEntry(fto.historyLength(), triples));
            }
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
        phaseTwoPruningTable = new HashMap<Long, LinkedList<PhaseTwoPruningEntry>>();
        phaseTwoPruningSearch(PHASE_TWO_PRUNING_DEPTH, new FullFto());
        phaseThreePruningSearch(PHASE_THREE_PRUNING_DEPTH, new FullFto());
    }

    /**
     * An instance of this class is passed to the search and used for storing solutions
     */
    private static class State{
        String[] solution = new String[3];
    }


    /**
     * Simple IDA* algorithm for solving Phase 1 (AKA. the bottom center)
     * @param depth depth
     * @param fto fto
     * @return Found solution? t/f
     */
    private boolean searchPhaseOne(int depth, State state, ArrayList<String> candidates, FullFto fto){
        nodes++;

        if (fto.isPhaseOne()){
            Move lastMove = fto.moveStack.peek();
            Move lastLastMove = fto.moveStack.get(fto.moveStack.size() - 2);
            //Nodes % n == 0
            //The performance of the solver is significantly better
            //when the phase 1 candidates are "spread out" (not too similar to each other)
            //so Nodes % n == 0 is a pseudo-random number generator which does that.
            if (fto.isValidPhaseOneFinishingSequence(lastMove, lastLastMove) && nodes % 5 == 0){
                state.solution[0] = fto.history();
                candidates.add(fto.history());
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
    private boolean searchPhaseTwo(int depth, State state, FullFto fto){
        nodes++;

        if (fto.isPhaseTwo()){
            state.solution[1] = fto.history();
            return true;
        }

        if (depth == 0){
            return false;
        }

        int edgeLookup = (int)(phaseTwoEdgePruningTable[fto.phaseTwoEdgeIndex()]);
        if (edgeLookup > depth) {
            return false;
        }

        int centerLookup = (int)(phaseTwoCenterPruningTable[fto.phaseTwoCenterIndex()]);
        if (centerLookup > depth){
            return false;
        }

        int tripleLookup = (int)(phaseTwoTriplePruningTable[fto.phaseTwoTripleIndex()]);
        if (tripleLookup == 25)
            tripleLookup = 10;

        int ply = fto.historyLength();

        //Logistic Regression model determines the likelihood of the current subtree having a solution
        //Subtrees that are unlikely to have a solution are cut
        if (depth > 7 && depth < 20 && ply > 0){
            double logOdds = BETAS[depth][0] + BETAS[depth][1] * (double)fto.triplePairCount() + BETAS[depth][2] * (float)edgeLookup + BETAS[depth][3] * (float)tripleLookup;

            double odds = Math.pow(2.71828182846, logOdds);

            double p = odds/(1+odds);

            if (p < THRESHOLDS[depth-8])
                return false;
        }

        //IDA* lookup
        if (depth <= PHASE_TWO_PRUNING_DEPTH) {
            LinkedList<PhaseTwoPruningEntry> lookup = phaseTwoPruningTable.get(fto.phaseTwoCentersHash());
            if (lookup == null) {
                return false;
            } else {
                boolean hashHit = false;

                for (PhaseTwoPruningEntry e : lookup) {
                    if (fto.checkPhaseTwoTripleData(e.triples) && e.distanceToSolved <= depth) {
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
            boolean foundSolution = searchPhaseTwo(depth-1, state, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
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
        return alg.split(" ").length;
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

        State state = new State();

        ArrayList<String> candidates = new ArrayList<>();

        //Run IDA* search for phase 1
        for (int depth = 0; depth < 100; depth++) {
            boolean foundSolution = searchPhaseOne(depth, state, candidates, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        //Run IDA* with pruning heuristics for phase 2
        for (int depth = 0; depth < 100; depth++) {
            boolean foundSolution = false;

            for (String phaseOneCandidate : candidates){
                int depthForSearch = Math.max(0, depth-algLen(phaseOneCandidate));

                FullFto copy = new FullFto(fto);
                copy.parseAlg(phaseOneCandidate);
                copy.clearMoveStack();

                foundSolution = searchPhaseTwo(depthForSearch, state, copy);

                if (foundSolution) {
                    state.solution[0] = phaseOneCandidate;
                    break;
                }

                if (depth == 99){
                    throw new RuntimeException("Could not find FTO Phase 2 solution");
                }
            }

            if (foundSolution)
                break;
        }

        fto.parseAlg(state.solution[0]);
        fto.parseAlg(state.solution[1]);
        fto.clearMoveStack();

        //IDA* search for phase 3
        for (int depth = 0; depth < 100; depth++) {
            boolean foundSolution = searchPhaseThree(depth, state, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 2 solution");
            }
        }

        String s = state.solution[0] + state.solution[1] + state.solution[2];
        return invertSolution(s);
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
            System.out.println(s);
            totalNodes += (long)search.nodes;

            long endTime = System.nanoTime();
            long duration = (endTime - startTime); // total time in nanoseconds

            System.out.println("Scramble time: " + (duration / 1_000_000) + " ms");
            totalTime += duration;
            totalMoves += s.trim().split(" ").length;
        }

        System.out.println("Average Time:" + ((totalTime/num) / 1_000_000) + " ms" );
        System.out.println("Average Moves:" + (((float)totalMoves/(float)num)));

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
        performanceTest(25);
    }
}
