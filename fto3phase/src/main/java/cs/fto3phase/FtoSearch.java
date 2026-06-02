package cs.fto3phase;

import  java.io.*;
import java.util.*;

import static cs.fto3phase.FullFto.Move;

/**
 * Three-phase IDA* solver for Face Turning Octahedron random-state scrambles.
 *
 * <p>The search first solves the D-face centers and edges, then reduces the
 * state to an Octaminx, and finally solves cube with {R L B D}.
 *
 * Scrambled Cube: {R L U D F B BR BL}
 * Phase 1 Solved: {R L U D B}
 * Phase 2 Solved: {R L D B}
 * Solved Cube : { ¯\_(ツ)_/¯ }
 *

 *
 * <h2> Phase 1 </h2>
 *<p>
 * Phase 1: Solve the centers and edges on the D face. The D
 *
 * Phase 1 is rather trivial to create an IDA* Search. The hash function, {@link FullFto#phaseOneHash()},
 * creates a unique hash from the permutation of the D-face centers and edges. The hash is normalized
 *
 *
 * </p>
 *
 */
public class FtoSearch {

    //--------------- Constants ---------------//

    /** Depth bound for the pruning tables. */
    private static final int PHASE_ONE_PRUNING_DEPTH = 5;
    private static final int PHASE_TWO_PRUNING_DEPTH = 8;
    private static final int PHASE_THREE_PRUNING_DEPTH = 4;

    /**
     * Maximum number of phase-one candidates to collect.
     * Higher values produce shorter solutions at the cost of search speed.
     */
    private static final int PHASE_ONE_CANDIDATE_LIMIT = 1000;
    private static final double PHASE_ONE_CANDIDATE_THRESHOLD = 0.3;

    //--------------- Public API ---------------//

    /**
     * Finds a random-state solution for the given FTO position.
     *
     * <p>The solver runs three sequential IDA* phases:
     * <ol>
     *   <li>Solve the D-face center and edge orbit.
     *   <li>Reduce to the octaminx subgroup.
     *   <li>Solve the reduced state.
     * </ol>
     *
     * The resulting solution is inverted and post-processed so that
     * applying it to a solved puzzle produces the given random state.
     *
     * @param fto the random puzzle state to solve
     * @return the scramble string using TNoodle-compatible move names
     */
    public String solution(FullFto fto){
        fto = new FullFto(fto);
        fto.clearMoveStack();

        ArrayList<FullFto> candidates = iteratePhaseOneCandidates(fto);
        FtoSymmetry phaseTwoSym = iteratePhaseTwo(candidates);
        String solvedSolution = iteratePhaseThree(phaseTwoSym);

        return postProcess(solvedSolution, fto);
    }

    //--------------- Search Methods ---------------//

    /**
     * Collects phase-one candidate solutions for the given state.
     * Candidates are filtered through the logistic regression model
     * so that they are likely to yield short phase-two solutions.
     * @param fto puzzle state (must have a cleared move history)
     * @return list of phase-one-completed candidate states
     * @throws IllegalArgumentException if {@code fto} has a non-empty move history
     * @throws RuntimeException if no phase-one solution is found within the depth limit
     */
    private ArrayList<FullFto> iteratePhaseOneCandidates(FullFto fto) {

        if (fto.historyLength() != 0){
            throw new IllegalArgumentException("FTO must have cleared history to find phase 1 candidates");
        }

        ArrayList<FullFto> candidates = new ArrayList<>();

        //Run IDA* search for phase 1
        for (int depth = 0; depth < 100; depth++) {
            boolean foundEnoughSolutions = searchPhaseOne(depth, candidates, fto);

            if (foundEnoughSolutions)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        return candidates;
    }

    /**
     * IDA* search for phase one: solve the D-face center and edges.
     * @param depth remaining search depth
     * @param candidates collects promising phase-one completions
     * @param fto current puzzle state
     * @return {@code true} if enough candidates have been collected
     */
    private boolean searchPhaseOne(int depth, ArrayList<FullFto> candidates, FullFto fto){
        if (depth == 0 && fto.isPhaseOne()){
            double p = logisticRegression(new FtoSymmetry(fto), 19-fto.historyLength());

            if (fto.isValidPhaseOneFinishingSequence() && p > PHASE_ONE_CANDIDATE_THRESHOLD){
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

        Move[] moves = depth == 1 ? PHASE_ONE_BREAKING_MOVES : ALL_MOVES;

        for (FullFto.Move move : moves){
            if (fto.isRepetition(move))
                continue;

            if (!fto.isValidParallelSequence(move))
                continue;

            fto.turn(move);
            if (searchPhaseOne(depth-1, candidates, fto))
                return true;
            fto.undo();
        }

        return false;
    }

    private FtoSymmetry iteratePhaseTwo(ArrayList<FullFto> candidates) {
        for (int totalDepth = 0; totalDepth < 100; totalDepth++) {
            for (FullFto candidate : candidates) {
                int phaseTwoDepth = totalDepth - candidate.historyLength();
                if (phaseTwoDepth <= 0) continue;

                FtoSymmetry sym = new FtoSymmetry(candidate);
                if (searchPhaseTwo(phaseTwoDepth, 0, sym))
                    return sym;
            }
        }
        throw new RuntimeException("Could not find FTO Phase 2 solution");
    }

    private String iteratePhaseThree(FtoSymmetry sym) {
        for (int depth = 0; depth < 100; depth++) {
            FtoSymmetry copy = new FtoSymmetry(sym);
            if (searchPhaseThree(depth, copy))
                return copy.history();
        }
        throw new RuntimeException("Could not find FTO Phase 3 solution");
    }

    /**
     * Main recursive search function for phase 2 (Bottom Center solved to Octaminx Reduction)
     * Very very important function! Very performance critical
     * @param depth depth to search
     * @param ply distance from root
     * @param fto state of FTO
     * @return found solution?
     */
    private boolean searchPhaseTwo(int depth, int ply, FtoSymmetry fto){

        //Check for phase two!
        if (fto.isPhaseTwo()){
            return true;
        }

        //Escape condition
        if (depth == 0){
            return false;
        }

        //Edge pruning is very effective.
        //The number of edge permutations for phase 2 is rather small
        //So we have a lovely little precomputed byte array that contains
        //the optimal solution length for each edge permutation (for phase 2)
        int edgeLookup = fto.minEdgeLookup();
        if (edgeLookup > depth) {
            return false;
        }

        //Logistic Regression model determines the likelihood of the current subtree having a solution
        //Subtrees that are unlikely to have a solution are cut
        if (depth > PHASE_TWO_PRUNING_DEPTH && depth < 20 && ply > 0){

            int tripleLookup = fto.tripleLookup();

            double p = logisticRegression(depth, fto, edgeLookup, tripleLookup);

            if (p < THRESHOLDS[depth-8])
                return false;
        }

        //IDA* lookup
        //We've got all positions 8 moves away from solved in a big ol' hash table
        //How convenient
        if (depth <= PHASE_TWO_PRUNING_DEPTH && !fto.getPhaseTwoLookup()) {
            return false;
        }

        //Loop and recurse
        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            if (ply != 0 && !fto.isValidParallelSequence(move))
                continue;

            fto.turn(move);
            if (searchPhaseTwo(depth-1, ply+1, fto))
                return true;
            fto.undo();
        }

        return false;
    }

    /**
     * IDA* search for phase three: solve the reduced octaminx state.
     * This search takes a negligible amount of time compared to Phase 1 / Phase 2
     * @param depth remaining search depth
     * @param fto current symmetry state
     * @return {@code true} if a solution was found
     */
    private boolean searchPhaseThree(int depth, FtoSymmetry fto){
        if (fto.isSolved()){
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
            if (searchPhaseThree(depth-1, fto))
                return true;
            fto.undo();
        }

        return false;
    }

    //--------------- Helper Methods ---------------//

    /**
     * Looks up the phase-two edge-pruning distance for the given state.
     * @param fto puzzle state
     * @return estimated distance to solved edges
     */
    private static int edgeLookup(FullFto fto){
        int index = fto.phaseTwoEdgeLehmerIndex();
        return phaseTwoEdgePruningTable[index];
    }

    /**
     * Returns the predicted probability that the given state can be solved
     * within the remaining depth, based on a logistic regression model
     * trained on phase-two search data.
     * @param depth remaining search depth
     * @param fto current symmetry state
     * @param edgeLookup edge pruning distance
     * @param tripleLookup triple pruning distance
     * @return probability in [0, 1]
     */
    private static double logisticRegression(int depth, FtoSymmetry fto, int edgeLookup, int tripleLookup) {
        double triples = fto.angles[0].tripleCount();
        double triplePairs = fto.triplePairCount();

        double logOdds = -2.873964 +
            (-0.219440 * triples) +
            ( 1.861916 * triplePairs) +
            ( 0.402323 * depth) +
            (-0.868738 * edgeLookup) +
            (-1.085977 * tripleLookup) +
            (-0.017704 * depth * depth) +
            (-0.049115 * triplePairs * depth) +
            ( 0.039372 * edgeLookup * depth) +
            ( 0.044133 * tripleLookup * depth);

        double odds = Math.pow(2.71828182846, logOdds);

        return odds / (1 + odds);
    }

    /**
     * Convenience overload that computes {@code edgeLookup} and
     * {@code tripleLookup} internally before delegating to the
     * full logistic regression method.
     * @param fto current symmetry state
     * @param depth remaining search depth
     * @return probability in [0, 1]
     */
    private static double logisticRegression(FtoSymmetry fto, int depth) {
        return logisticRegression(depth, fto, fto.minEdgeLookup(), fto.tripleLookup());
    }

    /**
     * Reverses the move order and toggles the prime modifier on every move.
     * @param s solution string to invert
     * @return the inverted solution string
     */
    private static String invertSolution(String s) {
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
     * Rotates every move in a solution string by one step around the y-axis.
     *
     * <p>Example: {@code "R U R'"} becomes {@code "B U B'"}.
     *
     * @param solution the solution string to rotate
     * @return the rotated solution string
     */
    private static String rotateSolution(String solution){
        solution = solution.trim();
        String[] moves = solution.split(" ");

        StringBuilder rotatedSolution = new StringBuilder();

        for (String move : moves) {
            rotatedSolution.append(rotatedMoves.get(move));
            rotatedSolution.append(" ");
        }

        return rotatedSolution.toString().trim();
    }

    /**
     * Post-processes a raw solution so that applying it to a solved puzzle
     * produces the given random state.
     *
     * <ol>
     *   <li>Invert the solution (the solver finds a solution <em>from</em> the random state).
     *   <li>Rotate to the standard white-top-green-front orientation.
     *   <li>Verify the inverted solution matches the random state.
     * </ol>
     *
     * <p>A y/y' rotation away from the original random orientation is acceptable
     * because absolute orientation does not matter for scrambling.
     *
     * @param solution the concatenated three-phase solution
     * @param randomState the target random state
     * @return the post-processed scramble string
     */
    private static String postProcess(String solution, FullFto randomState){
        solution = invertSolution(solution);

        FullFto test = new FullFto();
        test.parseAlg(solution);

        if (test.equals(randomState)){
            return solution;
        }

        test.rotate(1);

        if (test.equals(randomState)){
            return rotateSolution(solution);
        }

        test.rotate(1);

        assert (test.equals(randomState));

        return rotateSolution(rotateSolution(solution));
    }

    //--------------- Pruning Table Generation ---------------//

    /**
     * Loads a pruning table from a bundled resource file.
     * @param filename name of the resource file
     * @param size exact expected size of the table in bytes
     * @return the loaded byte array
     * @throws IOException if the resource cannot be read
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
     * Builds the phase-two edge pruning table on the fly by BFS, replacing
     * the previously-bundled {@code edgeprun.dat} resource.
     *
     * <p>Phase-one guarantees that D-face edges (positions 9-11) are solved
     * entering phase two, so the reachable subspace is the even permutations of
     * the remaining 9 edges — exactly {@code 9! / 2 = 181 440} states.</p>
     *
     * <p>D moves (D/DP) never change edges 0-8 and are excluded from the
     * search.  The 27 starting orientations R^{0..2} L^{0..2} B^{0..2} are
     * all seeded at distance 0 with a first-move restriction (U/UP only at
     * ply 0), matching the semantics of the original generator.</p>
     *
     * @return a {@code byte[181440]} pruning table
     */
    static byte[] generateEdgePruningTable() {
        final int size = 181440;

        // Moves relevant to edges 0-8 (D/DP excluded — they never change
        // the phase-two edge index).  U and UP MUST be indices 0 and 1 so the
        // depth-0 first-move restriction can reference them by position.
        final FullFto.Move[] edgeMoves = {
            Move.U, Move.UP, Move.R, Move.L, Move.B, Move.RP, Move.LP, Move.BP
        };
        final int moves = edgeMoves.length;  // 8

        // ---- Step 1: 27 starting orientations (R^{0..2}×L^{0..2}×B^{0..2}) ----
        // Each combination produces an edge permutation that is trivially
        // "solved" from the perspective of some canonical orientation.
        boolean[] startSeen = new boolean[size];
        int[] startIndices = new int[27];
        int startCount = 0;
        for (int r = 0; r < 3; r++) {
            for (int l = 0; l < 3; l++) {
                for (int b = 0; b < 3; b++) {
                    FullFto fto = new FullFto();
                    for (int ri = 0; ri < r; ri++) fto.turn(Move.R);
                    for (int li = 0; li < l; li++) fto.turn(Move.L);
                    for (int bi = 0; bi < b; bi++) fto.turn(Move.B);

                    int idx = fto.phaseTwoEdgeLehmerIndex();
                    if (!startSeen[idx]) {
                        startSeen[idx] = true;
                        startIndices[startCount++] = idx;
                    }
                }
            }
        }
        int uniqueStarts = startCount;

        // ---- Step 2: build move transition table ----
        // Edge transitions are independent of corner/center state, so the table
        // can be built once from setPhaseTwoEdgeIndex(i) and reused for every BFS
        // hop regardless of how a state was originally reached.
        int[][] edgeMoveTable = new int[moves][size];
        FullFto worker = new FullFto();
        for (int i = 0; i < size; i++) {
            worker.setPhaseTwoEdgeLehmerIndex(i);
            for (int m = 0; m < moves; m++) {
                worker.turn(edgeMoves[m]);
                edgeMoveTable[m][i] = worker.phaseTwoEdgeLehmerIndex();
                worker.undo();
            }
        }

        // ---- Step 3: BFS ----
        byte[] prun = new byte[size];
        Arrays.fill(prun, (byte) -1);

        // All starting orientations are at distance 0
        java.util.LinkedList<Integer> frontier = new java.util.LinkedList<>();
        for (int s = 0; s < uniqueStarts; s++) {
            int idx = startIndices[s];
            prun[idx] = 0;
            frontier.add(idx);
        }

        int depth = 0;
        while (!frontier.isEmpty()) {
            java.util.LinkedList<Integer> next = new java.util.LinkedList<>();

            // Depth 0 → 1: first-move restriction — only U / UP (indices 0, 1).
            // Depth 1+ : all edgeMoves.
            int lastM = (depth == 0) ? 2 : moves;

            for (int idx : frontier) {
                for (int m = 0; m < lastM; m++) {
                    int nextIdx = edgeMoveTable[m][idx];
                    if (prun[nextIdx] == -1) {
                        prun[nextIdx] = (byte) (depth + 1);
                        next.add(nextIdx);
                    }
                }
            }
            frontier = next;
            depth++;
        }

        //Post-processing:
        //Make the table symmetry-independent
        FullFto helper = new FullFto();
        for (int i = 0; i < size; i++) {
            helper.setPhaseTwoEdgeLehmerIndex(i);

            int minDistance = prun[i];

            helper.rotate(1);
            minDistance = Math.min(minDistance, prun[helper.phaseTwoEdgeLehmerIndex()]);
            helper.rotate(1);
            minDistance = Math.min(minDistance, prun[helper.phaseTwoEdgeLehmerIndex()]);

            prun[i] = (byte)minDistance;
        }

        return prun;
    }

    /**
     * Generates the phase-one pruning table via BFS over solved states.
     * Called once during class initialization, not during search.
     * @param depth remaining search depth
     * @param fto current puzzle state
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

            //Don't search branches that don't impact the phase one pieces
            long newHash = fto.phaseOneHash();
            if (hash == newHash) {
                fto.undo();
                continue;
            }

            phaseOnePruningSearch(depth-1, fto);
            fto.undo();
        }
    }


    /**
     * Generates the phase-two pruning table via BFS over solved states.
     * Called once during class initialization, not during search.
     * @param depth remaining search depth
     * @param fto current puzzle state (center-index tracking enabled)
     */
    private static void phaseTwoPruningSearch(int depth, FullFto fto){
        long centerHash = fto.phaseTwoHash();
        long triples = fto.packPhaseTwoTripleData();
        ArrayList<Long> lookup = phaseTwoPruningTable.get(centerHash);

        if (lookup == null){
            ArrayList<Long> entry = new ArrayList<>();
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

            if (!fto.isValidParallelSequence(move))
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
     * Generates the phase-three pruning table via BFS over solved states.
     * Called once during class initialization, not during search.
     * @param depth remaining search depth
     * @param fto current puzzle state
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

    //--------------- Static Data Tables ---------------//

    //Pruning tables
    private static final HashMap<Long, Integer> phaseOnePruningTable;
    private static final HashMap<Long, ArrayList<Long>> phaseTwoPruningTable;
    private static final HashMap<Long, Integer> phaseThreePruningTable;

    private static final byte[] phaseTwoEdgePruningTable;
    private static final byte[] phaseTwoTriplePruningTable;

    /**
     * Logistic regression thresholds indexed by search depth.
     * Subtrees whose predicted probability falls below the threshold
     * for the current depth are pruned.
     */
    private static final double[] THRESHOLDS = {
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
     * All possible FTO Moves
     */
    public static final Move[] ALL_MOVES = FullFto.Move.values();

    /**
     * All possible moves that break Phase One
     */
    public static final Move[] PHASE_ONE_BREAKING_MOVES = {Move.F, Move.FP, Move.BR, Move.BRP, Move.BL, Move.BLP};

    /**
     * Moves available during the phase-two search (octaminx reduction).
     */
    public static final Move[] PHASE_TWO_MOVES = {Move.U, Move.UP, Move.R, Move.L, Move.B, Move.RP, Move.LP, Move.BP, Move.DP, Move.D};
    /**
     * Moves available during the phase-three search (final solve).
     */
    public static final Move[] PHASE_THREE_MOVES = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

    /**
     * Maps each move name to its image under a y-axis rotation.
     */
    private static final HashMap<String, String> rotatedMoves = new HashMap<>();

    //--------------- Static Initializers ---------------//

    static{
        try {
            phaseTwoTriplePruningTable = loadTable("triple_d10.dat", 4096);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        phaseTwoEdgePruningTable = generateEdgePruningTable();

        for (int i = 0; i < 4096; i++) {
            if (phaseTwoTriplePruningTable[i] == 25){
                phaseTwoTriplePruningTable[i] = 10;
            }
        }

        rotatedMoves.put("R", "B");
        rotatedMoves.put("B", "L");
        rotatedMoves.put("L", "R");
        rotatedMoves.put("F", "BR");
        rotatedMoves.put("BR", "BL");
        rotatedMoves.put("BL", "F");
        rotatedMoves.put("U", "U");
        rotatedMoves.put("D", "D");

        rotatedMoves.put("R'", "B'");
        rotatedMoves.put("B'", "L'");
        rotatedMoves.put("L'", "R'");
        rotatedMoves.put("F'", "BR'");
        rotatedMoves.put("BR'", "BL'");
        rotatedMoves.put("BL'", "F'");
        rotatedMoves.put("U'", "U'");
        rotatedMoves.put("D'", "D'");
    }

    /*
      Builds all three pruning tables in memory at class-load time.
      Phase-two and phase-three tables are partially preloaded from
      bundled resource files and patched; the rest is generated by BFS.
     */
    static{
        phaseOnePruningTable = new HashMap<>();
        phaseThreePruningTable = new HashMap<>();
        phaseTwoPruningTable = new HashMap<>(71741);

        FullFto phaseTwoPruningFto = new FullFto();
        phaseTwoPruningFto.enableCenterIndexTracking();

        phaseOnePruningSearch(PHASE_ONE_PRUNING_DEPTH, new FullFto());
        phaseTwoPruningSearch(PHASE_TWO_PRUNING_DEPTH, phaseTwoPruningFto);
        phaseThreePruningSearch(PHASE_THREE_PRUNING_DEPTH, new FullFto());
    }

    //--------------- Inner Classes ---------------//

    private static class FtoSymmetry {

        public FullFto[] angles;

        public FtoSymmetry(FullFto fto){
            angles = new FullFto[3];
            for (int i = 0; i < 3; i++) {
                angles[i] = new FullFto(fto);
            }

            angles[1].rotate(1);
            angles[2].rotate(2);
        }

        public FtoSymmetry(FtoSymmetry sym){
            angles = new FullFto[3];
            this.angles[0] = new FullFto(sym.angles[0]);
            this.angles[1] = new FullFto(sym.angles[1]);
            this.angles[2] = new FullFto(sym.angles[2]);
        }

        public void turn(FullFto.Move move){
            angles[0].turn(move);
            angles[1].turn(move);
            angles[2].turn(move);
        }

        public boolean isPhaseTwo(){
            return angles[0].isPhaseTwo() || angles[1].isPhaseTwo() || angles[2].isPhaseTwo();
        }

        private boolean phaseTwoLookupHelper(int angle){
            ArrayList<Long> lookup = phaseTwoPruningTable.get(angles[angle].phaseTwoHash());
            if (lookup == null) return false;
            for (long triples : lookup) {
                if (angles[angle].checkPhaseTwoTripleData(triples)) return true;
            }
            return false;
        }

        public boolean getPhaseTwoLookup(){
            return phaseTwoLookupHelper(0) ||  phaseTwoLookupHelper(1) || phaseTwoLookupHelper(2);
        }

        public String history() {
            return angles[0].history();
        }

        public int historyLength() {
            return angles[0].historyLength();
        }

        public int minEdgeLookup(){
            return edgeLookup(angles[0]);
        }

        public int tripleLookup(){
            int minEval = 1000;

            for (FullFto angle : angles) {
                minEval = Math.min(minEval, phaseTwoTriplePruningTable[angle.phaseTwoTripleIndex()]);
            }

            return minEval;
        }

        public boolean isRepetition(Move move) {
            return angles[0].isRepetition(move);
        }

        public void clearMoveStack(){
            for (FullFto angle : angles) {
                angle.clearMoveStack();
            }
        }

        public void undo(){
            for (FullFto angle : angles) {
                angle.undo();
            }
        }

        public int triplePairCount() {
            return angles[0].triplePairCount();
        }

        private FullFto normalize(){
            for (int i = 0; i < 3; i++) {
                if (angles[i].isNormalized())
                    return angles[i];
            }

            throw new IllegalStateException("No angles are normalized");
        }

        public boolean isSolved() {
            return normalize().isSolved();
        }

        public long phaseThreeHash(){
            return normalize().phaseThreeHash();
        }

        public boolean isValidParallelSequence(Move move) {
            return angles[0].isValidParallelSequence(move);
        }
    }
}
