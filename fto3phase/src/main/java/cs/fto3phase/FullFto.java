package cs.fto3phase;

import java.util.ArrayList;
import java.util.Random;

public class FullFto {
    /** The internal representation of the FTO puzzle state. */
    private InnerState state = new InnerState();

    /** History stacks supporting {@link #undo()} and {@link #isRepetition(Move)}. */
    private final ArrayList<InnerState> stateHistory = new ArrayList<>();
    private final ArrayList<Move> moveHistory = new ArrayList<>();

    //--------------- Constructors ---------------//

    /** Creates a new FullFto in the solved state. */
    public FullFto(){
    }

    /**
     * Creates a copy of the given FullFto.
     * @param fto the FullFto to copy
     */
    public FullFto(FullFto fto){
        this.state = new InnerState(fto.state);

        this.stateHistory.addAll(fto.stateHistory);
        this.moveHistory.addAll(fto.moveHistory);
    }

    public void scrambleRandomG2State(Random random, int i) {
        for (int j = 0; j < i; j++) {
            turn(FtoSearch.PHASE_TWO_MOVES[random.nextInt(FtoSearch.PHASE_TWO_MOVES.length)]);
        }
        clearMoveStack();
    }

    //--------------- Enums ---------------//

    /**
     * Ordinal values of the centers.
     * The internal representation of packedCenters is {U, U, U, F, F, F ...}
     */
    private enum CenterOrd { U, F, BR, BL, L, R, B, D }

    enum Corner { U_L, U_R, U_F, D_L, D_R, D_B }

    enum Edge { U_B, U_R, U_L, F_L, F_R, R_BR, B_BL, B_BR, L_BL, D_F, D_BR, D_BL }

    /**
     * Indices values of the centers
     * Not actually what is stored in centers
     * see above ordinals
     */
    private enum CenterInd {
        U_BL, U_BR, U_F,
        F_U, F_BR, F_BL,
        BR_U, BR_BL, BR_F,
        BL_U, BL_F, BL_BR,
        L_B, L_R, L_D,
        R_L, R_B, R_D,
        B_R, B_L, B_D,
        D_L, D_R, D_B
    }

    /**
     * Puzzle moves including both clockwise and counter-clockwise variants.
     */
    public enum Move{
        R(0), L(1), U(2), D(3), F(4), B(5), BR(6), BL(7),
        RP(8), LP(9), UP(10), DP(11), FP(12), BP(13), BRP(14), BLP(15);

        final int id;

        Move(int id) {
            this.id = id;
        }
    }

    //--------------- Move History Operations ---------------//

    /**
     * Applies a move to the puzzle, saving the previous state to the history stack.
     * @param move the move to apply
     */
    public void turn(Move move){
        moveHistory.add(move);
        stateHistory.add(new InnerState(state));
        state.turn(move);
    }

    /**
     * Reverts the most recent move, restoring the previous state.
     */
    public void undo(){
        assert (!moveHistory.isEmpty());
        moveHistory.remove(moveHistory.size() - 1);
        state = stateHistory.remove(stateHistory.size() - 1);
    }

    /**
     * Returns the number of moves applied to the FTO.
     * @return the number of moves in the move history
     */
    public int historyLength() {
        assert (moveHistory.size() == stateHistory.size());
        return moveHistory.size();
    }

    /**
     * Clears the move and state history. Affects {@link #undo()} and {@link #isRepetition(Move)}.
     */
    public void clearMoveStack(){
        moveHistory.clear();
        stateHistory.clear();
    }

    /**
     * Returns the most recently applied move.
     * @return the last move
     */
    public Move lastMove(){
        return moveHistory.get(moveHistory.size() - 1);
    }

    /**
     * Returns the i-th most recent move (0 = most recent).
     * @param i offset from the most recent move
     * @return the move at the given offset
     */
    public Move lastMove(int i){
        return moveHistory.get(moveHistory.size() - 1 - i);
    }

    /**
     * Checks whether applying the given move would immediately repeat or invert the last move(s),
     * considering parallel move relationships.
     * @param move the move to check
     * @return true if the move would create a repetition
     */
    public boolean isRepetition(Move move){

        if (moveHistory.isEmpty())
            return false;

        Move lastMove = moveHistory.get(moveHistory.size() - 1);
        int moveId = move.id;
        if (move == lastMove || INVERT_MOVE[moveId] == lastMove) {
            return true;
        }

        if (moveHistory.size() < 2){
            return false;
        }

        Move lastLastmove = moveHistory.get(moveHistory.size()-2);

        if (move == lastLastmove || INVERT_MOVE[moveId] == lastLastmove) {
            Move[] parallelMoves = PARALLEL_MOVES[lastLastmove.id];
            return parallelMoves[0] == move || parallelMoves[1] == move;
        }

        return false;
    }

    /**
     * Validates whether the given move can be applied without creating a redundant parallel sequence in search.
     * Example: R L = Good
     *          L R = Bad
     * This makes sure we don't search any redundant sequences in the search.
     * @param move the move to validate
     * @return true if the move is valid in the current sequence
     */
    public boolean isValidParallelSequence(Move move){

        if (moveHistory.isEmpty()) return true;

        Move lastMove = moveHistory.get(moveHistory.size() - 1);

        Move[] parallelMoves = PARALLEL_MOVES[lastMove.id];
        if ((move == parallelMoves[0] || move == parallelMoves[1]) && (move.id < lastMove.id)) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the given move would break phase 1 constraints (F, BR, BL and their inverses).
     * @param move the move to check
     * @return true if the move breaks phase 1
     */
    public static boolean isPhaseOneBreakingMove(Move move){
        return move == Move.F || move == Move.FP || move == Move.BR || move == Move.BRP || move == Move.BL || move == Move.BLP;
    }

    /**
     * Checks whether the last two moves form a valid phase 1 finishing sequence.
     * @return true if the sequence is valid or if fewer than 2 moves have been applied
     */
    public boolean isValidPhaseOneFinishingSequence(){

        if (historyLength() < 2) return true;

        Move lastMove = lastMove();
        Move lastLastMove = lastMove(1);

        if (!isPhaseOneBreakingMove(lastMove))
            return false;

        Move[] parallelMoves = PARALLEL_MOVES[lastMove.id];
        if (parallelMoves[0] == lastLastMove)
            return false;

        return parallelMoves[1] != lastLastMove;
    }

    //--------------- State Queries ---------------//

    /**
     * Returns whether the puzzle is in the solved state.
     * @return true if solved
     */
    public boolean isSolved(){
        return state.isSolved();
    }

    /**
     * Returns whether the puzzle is in a normalized orientation (R face center at R_L position).
     * @return true if normalized
     */
    public boolean isNormalized(){
        return state.getCenterOrdinal(CenterInd.R_L.ordinal()) == CenterOrd.R.ordinal();
    }

    /**
     * Checks whether the puzzle is in phase 1 of the solve.
     * D-face centers must be solved and D-face edges must be in a valid cycle.
     * @return true if the puzzle state satisfies phase 1 requirements
     */
    public boolean isPhaseOne(){
        //Are centers on D face solved?
        if ((state.centers >> 42 & 0b111111) != 0b111111)
            return false;

        //Are edgeso on D face solved?
        return (state.getEdge(9) == 9 &&
            state.getEdge(10) == 10 &&
            state.getEdge(11) == 11) ||
                (state.getEdge(9) == 11 &&
                    state.getEdge(10) == 9 &&
                    state.getEdge(11) == 10) ||
                (state.getEdge(9) == 10 &&
                    state.getEdge(10) == 11 &&
                    state.getEdge(11) == 9);
    }

    /**
     * Checks whether the puzzle is in phase 2 of the solve.
     * All corners must form complete triples, and the R, L, and B faces must be solved.
     * @return true if the puzzle state satisfies phase 2 requirements
     */
    public boolean isPhaseTwo(){
        for (int i = 0; i < 6; i++) {
            if (!isTriple(i))
                return false;
        }

        return state.isFaceSolved(CenterOrd.R) && state.isFaceSolved(CenterOrd.L) && state.isFaceSolved(CenterOrd.B);
    }

    /**
     * Compares this FullFto with another for structural equality.
     * @param fto the FullFto to compare with
     * @return true if the internal corner, edge, and center state match
     */
    public boolean equals(FullFto fto){
        return fto.state.corners == this.state.corners &&
                fto.state.edges == this.state.edges &&
                fto.state.centers == this.state.centers;
    }

    //--------------- Triple Methods ---------------//

    /**
     * Returns whether the given corner forms a triple with its adjacent centers.
     * @param cornerLocation the corner index (0-5)
     * @return 3 if both matching centers are in the correct positions
     */
    private int triplePairsMask(int cornerLocation) {
        int corner = state.getCorner(cornerLocation);
        int cornerIndex = InnerState.getCornerIndex(corner);
        int cornerOrientation = InnerState.getCornerOrientation(corner);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation + 2) % 4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        int mask = 0;
        if (state.getCenterOrdinal(testSpotOne) == matchingCenterOne) mask |= 1;
        if (state.getCenterOrdinal(testSpotTwo) == matchingCenterTwo) mask |= 2;
        return mask;
    }

    public boolean isTriple(int cornerLocation){
        return triplePairsMask(cornerLocation) == 3;
    }

    /**
     * Counts how many matching centers are in the correct position for the given corner (0-2).
     * @param cornerLocation the corner index (0-5)
     * @return the number of correctly positioned matching centers (0, 1, or 2)
     */
    public int triplePairsOnCorner(int cornerLocation){
        return Integer.bitCount(triplePairsMask(cornerLocation));
    }

    /**
     * Returns a packed index representing the triple state of all 6 corners.
     * Each corner contributes 2 bits via {@link #triplePairsMask(int)}.
     * @return packed triple index
     */
    public int phaseTwoTripleIndex(){
        int index = 0;
        for (int i = 0; i < 6; i++) {
            index |= triplePairsMask(i) << (2 * i);
        }
        return index;
    }

    /**
     * Returns the total number of complete triples on the puzzle.
     * @return count of complete triples (0-6)
     */
    public int tripleCount(){
        int count = 0;

        for (int i = 0; i < 6; i++) {
            if (isTriple(i)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns the total number of correctly positioned matching center pairs across all corners.
     * @return total pair count (0-12)
     */
    public int triplePairCount(){
        int count = 0;

        for (int i = 0; i < 6; i++) {
            count += triplePairsOnCorner(i);
        }

        return count;
    }

    /**
     * Packs the phase 2 triple data into a compact long.
     * @return packed triple data
     */
    public long packPhaseTwoTripleData(){
        assert state.isTrackingCenterIndices();

        long hash = 0;
        long centerLocations = state.phaseTwoCenterIndexLocations();
        for (int cid = 0; cid < 6; cid++) {
            int corner = state.getCorner(cid);
            int ci = InnerState.getCornerIndex(corner);
            int co = InnerState.getCornerOrientation(corner);

            int matchingCenterOne = MATCHING_CENTER_INDICES[ci][co];
            int matchingCenterTwo = MATCHING_CENTER_INDICES[ci][(co + 2) & 0b11];

            hash |= ((centerLocations >> (5 * matchingCenterOne)) & 0b11111) << (10 * cid);
            hash |= ((centerLocations >> (5 * matchingCenterTwo)) & 0b11111) << (10 * cid + 5);
        }

        return hash;
    }

    /**
     * Verifies that the given packed triple data matches the current puzzle state.
     * @param tripleData packed triple data to verify
     * @return true if the data is consistent with the current state
     */
    public boolean checkPhaseTwoTripleData(long tripleData){
        for (int cid = 0; cid < 6; cid++) {
            for (int cside = 0; cside < 2; cside++){
                int corner = state.getCorner(cid);
                int ci = InnerState.getCornerIndex(corner);
                int co = InnerState.getCornerOrientation(corner);

                long targetIndex = tripleData & 0b11111;
                tripleData >>= 5;

                int matchingCenter = MATCHING_CENTERS[ci][(co + (2 * cside)) % 4];

                if (state.getCenterOrdinal((int)targetIndex) != matchingCenter){
                    return false;
                }
            }
        }

        return true;
    }

    //--------------- Hash / Index Methods ---------------//

    /**
     * Converts the phase 2 edge permutation into a compact index.
     * @return a compact index representing the edge permutation of edges 0-8
     */
    public int phaseTwoEdgeLehmerIndex() {
        return state.phaseTwoEdgeIndex();
    }

    /**
     * Sets the permutation of edges 0-8 from a compact phase-two edge index,
     * and resets D-face edges (9-11) to their solved positions.
     * @param index the compact edge index, as returned by {@link #phaseTwoEdgeLehmerIndex()}
     */
    public void setPhaseTwoEdgeLehmerIndex(int index) {
        state.setPhaseTwoEdgeIndex(index);
    }

    /**
     * Returns the edge piece ordinal at the given position.
     * @param i the position index (0-11)
     * @return the edge piece ordinal (0-11)
     */
    public int getEdge(int i) {
        return state.getEdge(i);
    }

    /**
     * Returns the hash for phase 1.
     * Incorporates D-face edges and centers.
     * @return the phase 1 hash
     */
    public long phaseOneHash() {
        return state.phaseOneHash();
    }

    /**
     * Returns the hash for phase 2.
     * Incorporates R, L, B face edges and centers.
     * @return the phase 2 hash
     */
    public long phaseTwoHash() {
        return state.phaseTwoHash();
    }

    /**
     * Returns the hash for phase 3.
     * Incorporates all remaining corners and edges.
     * @return the phase 3 hash
     */
    public long phaseThreeHash() {
        return state.phaseThreeHash();
    }

    //--------------- Scramble Methods ---------------//

    //Public API is the randomCube() method
    private void scrambleRandomState(Random r) {
        int parity = 0;

        // Fisher-Yates: pick target from shrinking range [0, i]
        for (int i = 5; i > 0; i--) {
            int target = r.nextInt(i + 1);
            state.swapCorners(i, target);
            if (i != target)
                parity++;
        }
        //Parity fix for Corner Permutation
        if (parity % 2 == 1)
            state.swapCorners(0, 1);

        parity = 0;
        for (int i = 0; i < 6; i++) {
            int corner = state.getCorner(i);
            //FTO CO behaves weirdly
            //U face corners can only have even orientations when on the U face and odd orientations on the D face
            //And vise versa for the D face corners
            if ((i < 3) == (InnerState.getCornerIndex(corner) < 3)) {
                //Twist by 0 or 2
                state.twistCorner(i, r.nextInt(2) * 2);
            } else {
                //Twist by 1 or 3
                state.twistCorner(i, r.nextInt(2) * 2 + 1);
            }
            parity += InnerState.getCornerOrientation(state.getCorner(i));
        }

        //Parity check for Corner Orientation
        if (parity % 4 == 2) {
            state.twistCorner(0, 2);
        }

        parity = 0;
        // Fisher-Yates: pick target from shrinking range [0, i]
        for (int i = 11; i > 0; i--) {
            int target = r.nextInt(i + 1);
            state.swapEdges(i, target);
            if (i != target)
                parity++;
        }

        //Parity fix for edges
        if (parity % 2 == 1)
            state.swapEdges(0, 1);

        // Fisher-Yates for both center rings
        for (int i = 11; i > 0; i--) {
            state.swapCenters(i, r.nextInt(i + 1));
            state.swapCenters(i + 12, r.nextInt(i + 1) + 12);
        }

        clearMoveStack();
    }

    /**
     * Creates a new FullFto in a random scrambled state.
     * @param r random number source
     * @return a randomly scrambled FullFto
     */
    public static FullFto randomCube(Random r){
        FullFto fto = new FullFto();
        fto.scrambleRandomState(r);
        return fto;
    }

    //--------------- I/O Methods ---------------//

    /**
     * Parses and applies an algorithm string.
     * Moves are separated by whitespace; prime moves use the ' suffix (e.g. "R'").
     * @param alg the algorithm string to parse and apply
     */
    public void parseAlg(String alg){
        String[] tokens = alg.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            boolean isPrime = token.endsWith("'");
            String base = isPrime ? token.substring(0, token.length() - 1) + "P" : token;
            Move move = Move.valueOf(base);
            turn(move);
        }
    }

    /**
     * Returns all previous moves as a space-separated string.
     * @return the move history as a string
     */
    public String history(){
        StringBuilder builder = new StringBuilder();

        for (Move move : moveHistory){
            builder.append(move.toString().replace("P", "'"));
            builder.append(" ");
        }

        return builder.toString();
    }

    //--------------- Other Methods ---------------//

    /**
     * Enables tracking of center indices.
     * Must be called from the solved state.
     * @throws IllegalStateException if the puzzle is not solved
     */
    public void enableCenterIndexTracking(){
        if (!isSolved()) {
            throw new IllegalStateException("Center index tracking can only be enabled from a solved state.");
        }
        state.enableCenterIndexTracking();
    }

    /**
     * Rotates the entire puzzle
     * @param n 1 = y rotation, 2 = y' rotation
     */
    public void rotate(int n){
        state.rotate(n);
    }

    //--------------- Static Tables ---------------//

    /**
     * Indexed as MATCHING_CENTERS[cornerIndex][cornerOrientation].
     * Used for counting triples and triple pairs.
     */
    static final int[][] MATCHING_CENTERS =  {
        {CenterOrd.U.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.U.ordinal()}, // U_L
        {CenterOrd.U.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.U.ordinal()}, // U_R
        {CenterOrd.U.ordinal(), CenterOrd.F.ordinal(), CenterOrd.F.ordinal(), CenterOrd.U.ordinal()}, // U_F
        {CenterOrd.F.ordinal(), CenterOrd.F.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal()}, // D_L
        {CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.F.ordinal(), CenterOrd.F.ordinal()}, // D_R
        {CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal()} // D_B
    };

    /**
     * Helper table for triple functions.
     * Contains the location of where the matching centers should be.
     */
    static final int[][] TRIPLE_LOCATIONS =  {
        {0, 9}, // U_L
        {1, 6}, // U_R
        {2, 3}, // U_F
        {5, 10}, // D_L
        {8, 4}, // D_R
        {11, 7} // D_B
    };

    /**
     * Reference table used for triple data.
     */
    static final int[][] MATCHING_CENTER_INDICES =  {
        {0, 9, 9, 0}, // U_L
        {1, 6, 6, 1}, // U_R
        {2, 3, 3, 2}, // U_F
        {5, 5, 10, 10}, // D_L
        {8, 8, 4, 4}, // D_R
        {11, 11, 7, 7} // D_B
    };

    static final Move[] INVERT_MOVE = {Move.RP, Move.LP, Move.UP, Move.DP, Move.FP, Move.BP, Move.BRP, Move.BLP,
        Move.R, Move.L, Move.U, Move.D, Move.F, Move.B, Move.BR, Move.BL};

    static final Move[][] PARALLEL_MOVES = {
        {Move.BL, Move.BLP}, // R
        {Move.BR, Move.BRP}, // L
        {Move.D, Move.DP}, // U
        {Move.U, Move.UP}, // D
        {Move.F, Move.FP}, // F
        {Move.B, Move.BP}, // B
        {Move.L, Move.LP}, // BR
        {Move.R, Move.RP}, // BL
        {Move.BL, Move.BLP}, // RP
        {Move.BR, Move.BRP}, // LP
        {Move.D, Move.DP}, // UP
        {Move.U, Move.UP}, // DP
        {Move.F, Move.FP}, // FP
        {Move.B, Move.BP}, // BP
        {Move.L, Move.LP}, // BRP
        {Move.R, Move.RP}, // BLP
    };

    //--------------- Zobrist Hash Keys ---------------//

    private static final long[][] PHASE2_CENTER_KEYS = new long[8][24];
    private static final long[][][] PHASE2_EDGE_KEYS = new long[12][3][12];
    private static final long[][][] PHASE3_CORNER_KEYS = new long[6][6][4];
    private static final long[][] PHASE3_EDGE_KEYS = new long[9][9];

    private static void fillRandom(Random r, long[] arr) {
        for (int i = 0; i < arr.length; i++) arr[i] = r.nextLong();
    }
    private static void fillRandom(Random r, long[][] arr) {
        for (long[] row : arr) fillRandom(r, row);
    }
    private static void fillRandom(Random r, long[][][] arr) {
        for (long[][] plane : arr) fillRandom(r, plane);
    }

    static {
        Random r =  new Random();

        fillRandom(r, PHASE2_CENTER_KEYS);
        fillRandom(r, PHASE2_EDGE_KEYS);
        fillRandom(r, PHASE3_CORNER_KEYS);
        fillRandom(r, PHASE3_EDGE_KEYS);
    }

    private static final int[][] MOVE_CORNERS = {
        {2, 1, 4}, {0, 2, 3}, {0, 1, 2}, {3, 4, 5},
        {2, 4, 3}, {1, 0, 5}, {1, 5, 4}, {0, 3, 5},
        {2, 4, 1}, {0, 3, 2}, {0, 2, 1}, {3, 5, 4},
        {2, 3, 4}, {1, 5, 0}, {1, 4, 5}, {0, 5, 3}
    };

    private static final int[][][] MOVE_TWISTS = {
        {{2,3}, {1,2}, {4,3}}, {{0,3}, {2,2}, {3,3}}, {{-1,0}, {-1,0}, {-1,0}}, {{-1,0}, {-1,0}, {-1,0}},
        {{2,3}, {4,3}, {3,2}}, {{1,3}, {0,2}, {5,3}}, {{1,3}, {5,3}, {4,2}}, {{0,3}, {3,3}, {5,2}},
        {{2,2}, {1,1}, {4,1}}, {{0,2}, {2,1}, {3,1}}, {{-1,0}, {-1,0}, {-1,0}}, {{-1,0}, {-1,0}, {-1,0}},
        {{2,1}, {4,2}, {3,1}}, {{1,2}, {0,1}, {5,1}}, {{1,1}, {5,2}, {4,1}}, {{0,1}, {3,2}, {5,1}}
    };

    private static final int[][] MOVE_EDGES = {
        {1, 5, 4}, {2, 3, 8}, {0, 1, 2}, {9, 10, 11},
        {4, 9, 3}, {0, 6, 7}, {5, 7, 10}, {8, 11, 6},
        {1, 4, 5}, {2, 8, 3}, {0, 2, 1}, {9, 11, 10},
        {4, 3, 9}, {0, 7, 6}, {5, 10, 7}, {8, 6, 11}
    };

    private static final int[][][] MOVE_CENTERS = {
        {{15,16,17}, {3,1,8}, {4,2,6}},  {{12,13,14}, {0,3,10}, {2,5,9}},
        {{0,1,2}, {15,12,18}, {16,13,19}}, {{21,22,23}, {5,8,11}, {4,7,10}},
        {{3,4,5}, {13,17,21}, {15,22,14}}, {{18,19,20}, {1,9,7}, {0,11,6}},
        {{6,7,8}, {16,20,22}, {17,18,23}}, {{9,10,11}, {19,14,23}, {12,21,20}},
        {{15,17,16}, {3,8,1}, {4,6,2}},  {{12,14,13}, {0,10,3}, {2,9,5}},
        {{0,2,1}, {15,18,12}, {16,19,13}}, {{21,23,22}, {5,11,8}, {4,10,7}},
        {{3,5,4}, {13,21,17}, {15,14,22}}, {{18,20,19}, {1,7,9}, {0,6,11}},
        {{6,8,7}, {16,22,20}, {17,23,18}}, {{9,11,10}, {19,23,14}, {12,20,21}}
    };

    //--------------- Inner State --------------//

    private static class InnerState{
        int corners = SOLVED_CORNERS;
        long edges = SOLVED_EDGES;
        long centers = SOLVED_CENTERS;

        long packedCenterIndicesLow = SOLVED_CENTER_INDICES_LOW;
        long packedCenterIndicesHigh = SOLVED_CENTER_INDICES_HIGH;
        boolean trackCenterIndices = false;

        private static final int SOLVED_CORNERS;
        private static final long SOLVED_EDGES;
        private static final long SOLVED_CENTERS;
        private static final long SOLVED_CENTER_INDICES_LOW;
        private static final long SOLVED_CENTER_INDICES_HIGH;

        /**
         * Bit-width constants and masks used for packing puzzle state into integers and longs.
         */
        private static final int CORNER_BITS = 5;
        private static final int CORNER_MASK = 0b11111;
        private static final int[] CORNER_MASKS = new int[6];
        static {
            for (int i = 0; i < 6; i++) {
                CORNER_MASKS[i] = CORNER_MASK << (CORNER_BITS * i);
            }

            int solvedCorners = 0;
            for (int i = 0; i < 6; i++) {
                solvedCorners |= encodeCorner(i, 0) << (CORNER_BITS * i);
            }
            SOLVED_CORNERS = solvedCorners;
        }

        private static final int EDGE_BITS = 4;
        private static final long EDGE_MASK = 0b1111;
        private static final long[] EDGE_MASKS = new long[12];
        static {
            for (int i = 0; i < 12; i++) {
                EDGE_MASKS[i] = EDGE_MASK << (EDGE_BITS * i);
            }

            long solvedEdges = 0;
            for (long i = 0; i < 12; i++) {
                solvedEdges |= i << (EDGE_BITS * i);
            }
            SOLVED_EDGES = solvedEdges;
        }

        private static final int CENTER_BITS = 2;
        private static final long CENTER_MASK = 0b11;
        private static final long[] CENTER_MASKS = new long[24];
        static {
            for (int i = 0; i < 24; i++) {
                CENTER_MASKS[i] = CENTER_MASK << (CENTER_BITS * i);
            }

            long solvedCenters = 0;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 3; j++){
                    solvedCenters |= (i & CENTER_MASK) << (CENTER_BITS * ((i * 3) + j));
                }
            }
            SOLVED_CENTERS = solvedCenters;
        }

        private static final int[] CENTER_ORBIT_OFFSET = new int[24];
        static {
            for (int i = 0; i < 24; i++) {
                CENTER_ORBIT_OFFSET[i] = i > 11 ? 4 : 0;
            }
        }

        private static final int CENTER_INDEX_BITS = 5;
        private static final long CENTER_INDEX_MASK = 0b11111;
        private static final long[] CENTER_INDEX_MASKS = new long[12];
        static {
            for (int i = 0; i < 12; i++) {
                CENTER_INDEX_MASKS[i] = CENTER_INDEX_MASK << (CENTER_INDEX_BITS * i);
            }

            long solvedCenterIndicesLow = 0;
            long solvedCenterIndicesHigh = 0;
            for (int i = 0; i < 12; i++) {
                solvedCenterIndicesLow |= (long) i << (CENTER_INDEX_BITS * i);
                solvedCenterIndicesHigh |= (long) (i + 12) << (CENTER_INDEX_BITS * i);
            }
            SOLVED_CENTER_INDICES_LOW = solvedCenterIndicesLow;
            SOLVED_CENTER_INDICES_HIGH = solvedCenterIndicesHigh;
        }


        public InnerState(){

        }

        public InnerState(InnerState state) {
            this.corners = state.corners;
            this.edges = state.edges;
            this.centers = state.centers;
            this.packedCenterIndicesHigh =  state.packedCenterIndicesHigh;
            this.packedCenterIndicesLow = state.packedCenterIndicesLow;
            this.trackCenterIndices = state.trackCenterIndices;

        }

        public static int getCornerIndex(int corner){
            return corner >> 2;
        }

        public static int getCornerOrientation(int corner){
            return corner & 0b11;
        }

        public int getCorner(int i){
            return (corners >> (CORNER_BITS * i)) & CORNER_MASK;
        }

        public void setCorner(int i, int corner){
            int shift = CORNER_BITS * i;
            corners = (corners & ~CORNER_MASKS[i]) | (corner << shift);
        }

        public void cycleCorners(int i1, int i2, int i3){
            int c1 = getCorner(i1);
            int c2 = getCorner(i2);
            int c3 = getCorner(i3);

            setCorner(i1, c3);
            setCorner(i2, c1);
            setCorner(i3, c2);
        }

        public void swapCorners(int i1, int i2){
            int c1 = getCorner(i1);
            int c2 = getCorner(i2);

            setCorner(i1, c2);
            setCorner(i2, c1);
        }

        public void twistCorner(int i, int dir){
            int corner = getCorner(i);
            setCorner(i, encodeCorner(getCornerIndex(corner), (getCornerOrientation(corner) + dir) & 0b11));
        }

        int getEdge(int i){
            return (int) ((edges >> (EDGE_BITS * i)) & EDGE_MASK);
        }

        private void setEdge(int i, int edge){
            int shift = EDGE_BITS * i;
            edges = (edges & ~EDGE_MASKS[i]) | ((long) edge << shift);
        }

        public void cycleEdges(int i1, int i2, int i3){
            int e1 = getEdge(i1);
            int e2 = getEdge(i2);
            int e3 = getEdge(i3);

            setEdge(i1, e3);
            setEdge(i2, e1);
            setEdge(i3, e2);
        }

        public void swapEdges(int i1, int i2){
            int e1 = getEdge(i1);
            int e2 = getEdge(i2);

            setEdge(i1, e2);
            setEdge(i2, e1);
        }

        private int getCenter(int i){
            return (int) ((centers >> (CENTER_BITS * i)) & CENTER_MASK);
        }

        public void setCenter(int i, int center){
            int shift = CENTER_BITS * i;
            centers = (centers & ~CENTER_MASKS[i]) | ((long) center << shift);
        }

        private static int encodeCorner(int perm, int orientation){
            return ((perm << 2) | orientation);
        }


        /**
         * Returns the center index at the given position.
         * @param i position index (0-23)
         * @return the center index at that position
         */
        int getCenterIndex(int i){
            int packedIndex = i % 12;
            long packed = i < 12 ? packedCenterIndicesLow : packedCenterIndicesHigh;
            return (int) ((packed >> (CENTER_INDEX_BITS * packedIndex)) & CENTER_INDEX_MASK);
        }

        private long phaseTwoCenterIndexLocations(){
            long locations = 0;
            //Only loop over low center indices because orbits are so epic
            for (int i = 0; i < 12; i++) {
                long centerIndex = (packedCenterIndicesLow >> (CENTER_INDEX_BITS * i)) & CENTER_INDEX_MASK;
                if (centerIndex < 12) {
                    locations |= (long) i << (CENTER_INDEX_BITS * centerIndex);
                }
            }
            return locations;
        }

        public boolean isTrackingCenterIndices(){
            return trackCenterIndices;
        }

        /**
         * This switches on tracking specific center indices rather than just their colors
         * Used for generating the triple tables and nothing else
         * Must be enabled on a solved cube before applying moves if you want to pack triple data
         */
        public void enableCenterIndexTracking(){
            assert isSolved();
            trackCenterIndices = true;
            packedCenterIndicesLow = SOLVED_CENTER_INDICES_LOW;
            packedCenterIndicesHigh = SOLVED_CENTER_INDICES_HIGH;
        }

        private void setCenterIndex(int i, int centerIndex){
            int packedIndex = i % 12;
            int shift = CENTER_INDEX_BITS * packedIndex;
            if (i < 12) {
                packedCenterIndicesLow = (packedCenterIndicesLow & ~CENTER_INDEX_MASKS[packedIndex]) | ((long) centerIndex << shift);
            } else {
                packedCenterIndicesHigh = (packedCenterIndicesHigh & ~CENTER_INDEX_MASKS[packedIndex]) | ((long) centerIndex << shift);
            }
        }

        void cycleThreeCenters(int i1, int i2, int i3){
            int c1 = getCenter(i1);
            int c2 = getCenter(i2);
            int c3 = getCenter(i3);

            setCenter(i1, c3);
            setCenter(i2, c1);
            setCenter(i3, c2);

            if (trackCenterIndices) {
                int ci1 = getCenterIndex(i1);
                int ci2 = getCenterIndex(i2);
                int ci3 = getCenterIndex(i3);
                setCenterIndex(i1, ci3);
                setCenterIndex(i2, ci1);
                setCenterIndex(i3, ci2);
            }
        }

        void swapCenters(int i1, int i2){
            int c1 = getCenter(i1);
            int c2 = getCenter(i2);

            setCenter(i1, c2);
            setCenter(i2, c1);

            if (trackCenterIndices) {
                int ci1 = getCenterIndex(i1);
                int ci2 = getCenterIndex(i2);
                setCenterIndex(i1, ci2);
                setCenterIndex(i2, ci1);
            }
        }

        int getCenterOrdinal(int index){
            return ((int)(centers >> (index << 1)) & 3) + CENTER_ORBIT_OFFSET[index];
        }

        private static final int[] Y_ROTATION_CORNERS = {
            Corner.U_F.ordinal(),
            Corner.U_L.ordinal(),
            Corner.U_R.ordinal(),
            Corner.D_R.ordinal(),
            Corner.D_B.ordinal(),
            Corner.D_L.ordinal(),
        };

        private static final int[] Y_ROTATION_EDGES = {
            Edge.U_L.ordinal(),
            Edge.U_B.ordinal(),
            Edge.U_R.ordinal(),
            Edge.R_BR.ordinal(),
            Edge.B_BR.ordinal(),
            Edge.B_BL.ordinal(),
            Edge.F_L.ordinal(),
            Edge.L_BL.ordinal(),
            Edge.F_R.ordinal(),
            Edge.D_BR.ordinal(),
            Edge.D_BL.ordinal(),
            Edge.D_F.ordinal(),
        };

        private static final int[] Y_ROTATION_CENTERS = {
            CenterOrd.U.ordinal(),
            CenterOrd.BR.ordinal(),
            CenterOrd.BL.ordinal(),
            CenterOrd.F.ordinal(),
            CenterOrd.R.ordinal(),
            CenterOrd.B.ordinal(),
            CenterOrd.L.ordinal(),
            CenterOrd.D.ordinal(),
        };


        /**
         * Performs y / y' rotations on the puzzle.
         * Note that the solver always solves with white top, green front regardless of the initial orientation.
         * Can only be performed on a solved cube.
         * @param n 1 = y rotation, 2 = y' rotation
         */
        void rotate(int n){
            assert(n == 1 || n == 2);

            for (int i = 0; i < 6; i++) {
                setCorner(i, encodeCorner(Y_ROTATION_CORNERS[getCornerIndex(getCorner(i))], getCornerOrientation(getCorner(i))));
            }

            for (int i = 0; i < 12; i++) {
                setEdge(i, Y_ROTATION_EDGES[getEdge(i)]);
            }

            for (int i = 0; i < 24; i++) {
                // % 4:
                //Internal representation of centers are packed to 2-bits per center
                //This works because orbits are a thing on FTO
                setCenter(i, Y_ROTATION_CENTERS[getCenterOrdinal(i)] % 4);
            }

            if (n == 2){
                rotate(1);
            }
        }

        private static final int[] FACTORIAL = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800};

        public int phaseTwoEdgeIndex() {
            // Encode the permutation of edges 0-8 (the non-D-face edges) as a
            // Lehmer code (factorial number system), scanning from position 8
            // down to 0. Because FTO edges must have even permutation parity,
            // we divide by 2 to halve the index space — resulting in a [0 .. 9!/2)
            // range used for the phase-two edge pruning table.
            int index = 0;
            int seen = 0;
            for (int i = 8; i >= 0; i--) {
                int edge = getEdge(i);
                int smaller = Integer.bitCount(seen & ((1 << edge) - 1));
                index += smaller * FACTORIAL[8 - i];
                seen |= 1 << edge;
            }
            return index / 2;
        }

        /**
         * Decodes the raw (pre-/2) Lehmer-code value for the 9-element edge
         * permutation subset (edges 0-8). Returns edge piece IDs per position
         * (positions 0..8).
         */
        private int[] decodeEdgePermutation9(long encoded) {
            int[] perm = new int[9];
            boolean[] used = new boolean[9];
            long remaining = encoded;
            for (int pos = 0; pos < 9; pos++) {
                int factor = FACTORIAL[8 - pos];
                int rank = (int) (remaining / factor);
                remaining %= factor;
                // Walk unused edge IDs to find the one with ordinal 'rank'
                int count = 0;
                for (int elem = 0; elem < 9; elem++) {
                    if (!used[elem]) {
                        if (count == rank) {
                            perm[pos] = elem;
                            used[elem] = true;
                            break;
                        }
                        count++;
                    }
                }
            }
            return perm;
        }

        /**
         * Sets the permutation of edges 0-8 from the compact phase-two edge
         * index (as returned by {@link #phaseTwoEdgeIndex()}).
         *
         * <p>The stored index represents the 9!/2 subspace of even-permutation
         * states. We decompress by trying base × 2, checking the resulting
         * permutation parity, and falling through to base × 2 + 1 if the
         * first candidate is odd. D-face edges (9,10,11) are reset to their
         * solved positions since phase two never moves them.</p>
         */
        public void setPhaseTwoEdgeIndex(int index) {
            // The two candidate raw Lehmer indices that share the same /2 slot
            long base = ((long) index) * 2;

            int[] perm = decodeEdgePermutation9(base);

            // Count inversions among the 9 elements to determine parity
            int parity = 0;
            for (int k = 0; k < 9; k++) {
                for (int j = k + 1; j < 9; j++) {
                    if (perm[k] > perm[j]) parity ^= 1;
                }
            }

            // If the first candidate has odd parity, its even partner is +1
            if (parity != 0) {
                perm = decodeEdgePermutation9(base + 1);
            }

            for (int pos = 0; pos < 9; pos++) {
                setEdge(pos, perm[pos]);
            }

            // D-face edges are always solved in phase two
            setEdge(9, 9);
            setEdge(10, 10);
            setEdge(11, 11);
        }

        private long centerHash(CenterOrd center){
            long hash = 0;
            int centerId = center.ordinal();
            long[] centerKeys = PHASE2_CENTER_KEYS[center.ordinal()];

            for (int i = 0; i < 24; i++) {
                if (getCenterOrdinal(i) == centerId){
                    hash ^= centerKeys[i];
                }
            }

            return hash;
        }

        /**
         * Indexed by CenterOrd. Each entry lists the 3 edge indices on that face.
         */
        static final int[][] EDGES_ON_FACE = {
            {0, 2, 1},
            {3, 9, 4},
            {7, 10, 5},
            {6, 11, 8},
            {8, 3, 2},
            {5, 1, 4},
            {6, 7, 0},
            {11, 10, 9},
        };

        /**
         * Takes the locations of the edges on a face and turns them into a hash
         * !!! This normalizes the edges, so it's assuming you don't care about
         * what symmetry you're solving to
         *
         * Example: [U], [R U], [R' U]
         * Each of these three states will produce the same hash for:
         * fto.edgeHash(R);
         *
         * @param center
         * @return
         */
        private long edgeHash(CenterOrd center){
            int centerIndex = center.ordinal();
            int[] faceEdges = EDGES_ON_FACE[centerIndex];
            int e0 = faceEdges[0];
            int e1 = faceEdges[1];
            int e2 = faceEdges[2];

            int pos0 = -1, pos1 = -1, pos2 = -1;
            int firstMatchingEdge = -1;
            int firstPos = 12;

            for (int i = 0; i < 12; i++){
                int edge = getEdge(i);
                if (edge == e0) { pos0 = i; if (i < firstPos) { firstPos = i; firstMatchingEdge = e0; } }
                if (edge == e1) { pos1 = i; if (i < firstPos) { firstPos = i; firstMatchingEdge = e1; } }
                if (edge == e2) { pos2 = i; if (i < firstPos) { firstPos = i; firstMatchingEdge = e2; } }
            }

            if (firstMatchingEdge == -1){
                throw new IllegalStateException("Cannot find matching edge in edgeHash()");
            }

            while (e0 != firstMatchingEdge){
                int tmp = e2; e2 = e1; e1 = e0; e0 = tmp;
                int tmpP = pos2; pos2 = pos1; pos1 = pos0; pos0 = tmpP;
            }

            long hash = 0;
            long[][] edgeKeys = PHASE2_EDGE_KEYS[centerIndex];
            hash ^= edgeKeys[0][pos0];
            hash ^= edgeKeys[1][pos1];
            hash ^= edgeKeys[2][pos2];

            return hash;
        }

        public long phaseOneHash(){
            return edgeHash(CenterOrd.D) ^ centerHash(CenterOrd.D);
        }

        public long phaseTwoHash(){
            long hash = 0;

            hash ^= (centers & 0x3FFFF000000L);

            hash ^= edgeHash(CenterOrd.R);
            hash ^= edgeHash(CenterOrd.L);
            hash ^= edgeHash(CenterOrd.B);

            return hash;
        }

        public long phaseThreeHash(){
            long hash = 0;

            for (int i = 0; i < 9; i++) {
                hash ^= PHASE3_EDGE_KEYS[i][getEdge(i)];
            }

            for (int i = 0; i < 6; i++) {
                int corner = getCorner(i);
                hash ^= PHASE3_CORNER_KEYS[i][InnerState.getCornerIndex(corner)][InnerState.getCornerOrientation(corner)];
            }

            return hash;
        }

        private boolean areEdgesSolvedOnFace(CenterOrd face, int angle){
            int faceIndex = face.ordinal();
            int[] faceEdges = EDGES_ON_FACE[faceIndex];
            for (int i = 0; i < 3; i++) {
                if (getEdge(faceEdges[(i + angle) % 3]) != faceEdges[i]){
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns whether all three centers on the given face match the face color.
         * @param face the face to check
         * @return true if all centers on the face are solved
         */
        private boolean areCentersSolvedOnFace(CenterOrd face){
            int shift = face.ordinal() * 6;
            int raw = face.ordinal() & 3;
            int expected = raw | (raw << 2) | (raw << 4);
            return ((int)(centers >> shift) & 0b111111) == expected;
        }

        /**
         * Returns whether the given face is fully solved (centers and edges).
         * @param face the face to check
         * @return true if the face is solved
         */
        public boolean isFaceSolved(CenterOrd face){
            return areCentersSolvedOnFace(face) &&
                (areEdgesSolvedOnFace(face, 0) ||
                    areEdgesSolvedOnFace(face, 1) ||
                    areEdgesSolvedOnFace(face, 2));
        }


        public void turn(FullFto.Move move){
            int id = move.id;

            int[] corners = MOVE_CORNERS[id];
            cycleCorners(corners[0], corners[1], corners[2]);

            for (int[] twist : MOVE_TWISTS[id]) {
                if (twist[0] >= 0) {
                    twistCorner(twist[0], twist[1]);
                }
            }

            int[] edges = MOVE_EDGES[id];
            cycleEdges(edges[0], edges[1], edges[2]);

            for (int[] centers : MOVE_CENTERS[id]) {
                cycleThreeCenters(centers[0], centers[1], centers[2]);
            }
        }

        public boolean isSolved(){
            return corners == SOLVED_CORNERS && edges == SOLVED_EDGES && centers == SOLVED_CENTERS;
        }
    }
}
