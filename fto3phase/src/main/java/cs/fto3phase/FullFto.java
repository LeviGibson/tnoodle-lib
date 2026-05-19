package cs.fto3phase;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class FullFto {
    /**
     * Internal State
     */

    private InnerState state = new InnerState();
    private InnerState rot1 = new InnerState(1);
    private InnerState rot2 = new InnerState(2);

    /**
     * History, used for undo() method
     */
    private final Stack<InnerState> stateHistory = new Stack<>();
    private final Stack<InnerState> rot1History = new Stack<>();
    private final Stack<InnerState> rot2History = new Stack<>();

    private final Stack<Move> moveHistory = new Stack<>();

    /**
     * Main Constructor
     */
    public FullFto(){
    }

    /**
     * Copy Constructor
     * @param fto copy from
     */
    public FullFto(FullFto fto){
        this.state = new InnerState(fto.state);
        this.rot1 = new InnerState(fto.rot1);
        this.rot2 = new InnerState(fto.rot2);

        this.stateHistory.addAll(fto.stateHistory);
        this.rot1History.addAll(fto.rot1History);
        this.rot2History.addAll(fto.rot2History);
        this.moveHistory.addAll(fto.moveHistory);
    }

    /**
     * Get number of moves applied to the FTO
     * @return number of moves
     */
    public int historyLength() {
        assert (moveHistory.size() == stateHistory.size());
        assert (moveHistory.size() == rot1History.size());
        assert (moveHistory.size() == rot2History.size());
        return moveHistory.size();
    }

    /**
     * Get rid of history. undo() and isRepetition() are the effected methods.
     */
    public void clearMoveStack(){
        moveHistory.clear();
        stateHistory.clear();
        rot1History.clear();
        rot2History.clear();
    }

//    private InnerState getNormalizedState(){
//        return state;
//    }

    public int phaseTwoEdgeIndex(int angle) {
        switch (angle){
            case 0:
                return state.phaseTwoEdgeIndex();
            case 1:
                return rot1.phaseTwoEdgeIndex();
            case 2:
                return rot2.phaseTwoEdgeIndex();
            default:
                 throw new IllegalArgumentException("Invalid angle: " + angle);
        }
    }

    public long phaseOneHash() {
        return state.phaseOneHash();
    }

    public long phaseTwoHash() {
        return state.phaseTwoHash();
    }

    public long phaseThreeHash() {
        return state.phaseThreeHash();
    }

    /**
     * Ordinal values of the packedCenters
     * internal representation of packedCenters is {U, U, U, F, F, F ...}
     */
    private enum CenterOrd {
        U(0), F(1), BR(2), BL(3), L(4), R(5), B(6), D(7);

        final int id;

        CenterOrd(int id) {
            this.id = id;
        }
    }

    enum Corner {
        U_L, U_R, U_F, D_L, D_R, D_B
    }

    enum Edge {
        U_B, U_R, U_L, F_L, F_R, R_BR, B_BL, B_BR, L_BL, D_F, D_BR, D_BL
    }

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
     * PUBLIC! Move enum. Used in the public interface
     */
    public enum Move{
        R(0), L(1), U(2), D(3), F(4), B(5), BR(6), BL(7),
        RP(8), LP(9), UP(10), DP(11), FP(12), BP(13), BRP(14), BLP(15);

        final int id;

        Move(int id) {
            this.id = id;
        }
    }

    /**
     * MATCHING_CENTERS[getCornerIndex(corner)][corner orientation]
     * Used for counting triples and triple pairs
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
     * Helper table to triple functions
     * Contains the location of where the matching centers should be
     */
    static final int[][] TRIPLE_LOCATIONS =  {
        {0, 9}, // U_L
        {1, 6}, // U_R
        {2, 3}, // U_F
        {5, 10}, // D_L
        {8, 4}, // D_R
        {11, 7} // D_B
    };

    private boolean isTriple(int cornerLocation){
        int corner = state.getCorner(cornerLocation);
        int cornerIndex = InnerState.getCornerIndex(corner);
        int cornerOrientation = InnerState.getCornerOrientation(corner);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation + 2) % 4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        return state.getCenterOrdinal(testSpotOne) == matchingCenterOne &&
                state.getCenterOrdinal(testSpotTwo) == matchingCenterTwo;
    }

    public int triplePairsOnCorner(int cornerLocation){
        int corner = state.getCorner(cornerLocation);
        int cornerIndex = InnerState.getCornerIndex(corner);
        int cornerOrientation = InnerState.getCornerOrientation(corner);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation + 2) % 4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        int count = 0;
        if (state.getCenterOrdinal(testSpotOne) == matchingCenterOne)
            count++;
        if (state.getCenterOrdinal(testSpotTwo) == matchingCenterTwo)
            count++;

        return count;
    }

    public int tripleIndexHelper(int cornerLocation){
        int corner = state.getCorner(cornerLocation);
        int cornerIndex = InnerState.getCornerIndex(corner);
        int cornerOrientation = InnerState.getCornerOrientation(corner);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation + 2) % 4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        int count = 0;
        if (state.getCenterOrdinal(testSpotOne) == matchingCenterOne)
            count |= 1;
        if (state.getCenterOrdinal(testSpotTwo) == matchingCenterTwo)
            count |= 2;

        return count;
    }

    public int phaseTwoTripleIndex(){
        int index = 0;
        for (int i = 0; i < 6; i++) {
            index |= tripleIndexHelper(i) << (2 * i);
        }
        return index;
    }

    public int tripleCount(){
        int count = 0;

        for (int i = 0; i < 6; i++) {
            if (isTriple(i)) {
                count++;
            }
        }

        return count;
    }

    public int triplePairCount(){
        int count = 0;

        for (int i = 0; i < 6; i++) {
            count += triplePairsOnCorner(i);
        }

        return count;
    }

    public boolean isPhaseOne(){
        if (state.getCenterOrdinal(21) != CenterOrd.D.id ||
            state.getCenterOrdinal(22) != CenterOrd.D.id ||
            state.getCenterOrdinal(23) != CenterOrd.D.id)
            return false;
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

    public boolean isPhaseTwo(){
        for (int i = 0; i < 6; i++) {
            if (!isTriple(i))
                return false;
        }

        return state.isFaceSolved(CenterOrd.R) && state.isFaceSolved(CenterOrd.L) && state.isFaceSolved(CenterOrd.B);
    }

    //--------------- Hash Functions ---------------//

    private static final long[][] PHASE2_CENTER_KEYS = new long[8][24];
    private static final long[][][] PHASE2_EDGE_KEYS = new long[12][3][12];
    private static final long[][][] PHASE3_CORNER_KEYS = new long[6][6][4];
    private static final long[][] PHASE3_EDGE_KEYS = new long[9][9];

    static {
        Random r =  new Random();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 24; j++) {
                PHASE2_CENTER_KEYS[i][j] = r.nextLong();
            }
        }

        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 12; k++) {
                    PHASE2_EDGE_KEYS[i][j][k] = r.nextLong();
                }
            }
        }

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                for (int k = 0; k < 4; k++) {
                    PHASE3_CORNER_KEYS[i][j][k] = r.nextLong();
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                PHASE3_EDGE_KEYS[i][j] = r.nextLong();
            }
        }
    }

    private static boolean contains(int[] arr, int target) {
        for (int num : arr) {
            if (num == target) return true;
        }
        return false;
    }


    /**
     * Reference table used for triple data
     */
    static final int[][] MATCHING_CENTER_INDICES =  {
        {0, 9, 9, 0}, // U_L
        {1, 6, 6, 1}, // U_R
        {2, 3, 3, 2}, // U_F
        {5, 5, 10, 10}, // D_L
        {8, 8, 4, 4}, // D_R
        {11, 11, 7, 7} // D_B
    };

    public long packPhaseTwoTripleData(){
        long hash = 0;
        for (int cid = 0; cid < 6; cid++) {
            for (int cside = 0; cside < 2; cside++){
                int corner = state.getCorner(cid);
                int ci = InnerState.getCornerIndex(corner);
                int co = InnerState.getCornerOrientation(corner);
                int matchingCenter = MATCHING_CENTER_INDICES[ci][(co + (2 * cside)) % 4];
                for (long i = 0; i < 24; i++) {
                    if (matchingCenter == state.getCenterIndex((int)i)){
                        hash |= i << (10 * cid + 5 * cside);
                    }
                }
            }
        }

        return hash;
    }

    public boolean checkPhaseTwoTripleData(long hash){
        for (int cid = 0; cid < 6; cid++) {
            for (int cside = 0; cside < 2; cside++){
                int corner = state.getCorner(cid);
                int ci = InnerState.getCornerIndex(corner);
                int co = InnerState.getCornerOrientation(corner);

                long targetIndex = hash & 0b11111;
                hash >>= 5;

                int matchingCenter = MATCHING_CENTERS[ci][(co + (2 * cside)) % 4];

                if (state.getCenterOrdinal((int)targetIndex) != matchingCenter){
                    return false;
                }
            }
        }

        return true;
    }

    public Move lastMove(){
        return moveHistory.peek();
    }

    public Move lastMove(int i){
        return moveHistory.get(moveHistory.size() - 1 - i);
    }

    public static boolean isPhaseOneBreakingMove(Move move){
        return move == Move.F || move == Move.FP || move == Move.BR || move == Move.BRP || move == Move.BL || move == Move.BLP;
    }

    public static boolean isValidPhaseOneFinishingSequence(Move lastMove, Move lastLastMove){
        if (!isPhaseOneBreakingMove(lastMove))
            return false;

        Move[] parallelMoves = PARALLEL_MOVES[lastMove.id];
        if (parallelMoves[0] == lastLastMove)
            return false;

        return parallelMoves[1] != lastLastMove;
    }

    static final Move[] INVERT_MOVE = {Move.RP, Move.LP, Move.UP, Move.DP, Move.FP, Move.BP, Move.BRP, Move.BLP,
        Move.R, Move.L, Move.U, Move.D, Move.F, Move.B, Move.BR, Move.BL};

    static final Move[][] PARALLEL_MOVES = {
        {Move.BL, Move.BLP}, // R
        {Move.BR, Move.BRP}, // L
        {Move.D, Move.DP}, // U
        {Move.U, Move.UP}, // D
        {Move.B, Move.BP}, // F
        {Move.F, Move.FP}, // B
        {Move.L, Move.LP}, // BR
        {Move.R, Move.RP}, // BL
        {Move.BL, Move.BLP}, // RP
        {Move.BR, Move.BRP}, // LP
        {Move.D, Move.DP}, // UP
        {Move.U, Move.UP}, // DP
        {Move.B, Move.BP}, // FP
        {Move.F, Move.FP}, // BP
        {Move.L, Move.LP}, // BRP
        {Move.R, Move.RP}, // BLP
    };

    public boolean isRepetition(Move move){

        if (moveHistory.isEmpty())
            return false;

        Move lastMove = moveHistory.peek();
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

    public void scrambleRandomG2State(Random r){
        scrambleRandomG2State(r, 500);
    }

    public void scrambleRandomG2State(Random r, int numMoves){
        Move[] phaseTwoMoves = {Move.U, Move.R, Move.L, Move.D, Move.B, Move.UP, Move.RP, Move.LP, Move.DP, Move.BP};
        Move[] phaseThreeMoves = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

        for (int i = 0; i < 500; i++) {
            Move move;

            do {
                move = phaseThreeMoves[r.nextInt(phaseThreeMoves.length)];
            } while (isRepetition(move));

            turn(move);
        }

        for (int i = 0; i < numMoves; i++) {
            Move move;

            do {
                move = phaseTwoMoves[r.nextInt(phaseTwoMoves.length)];
            } while (isRepetition(move));

            turn(move);
        }

        clearMoveStack();
    }

    private void swapCornersForAllRots(int i1, int i2){
        state.swapCorners(i1, i2);
        rot1.swapCorners(i1, i2);
        rot2.swapCorners(i1, i2);
    }

    private void scrambleRandomState(Random r){
        int parity = 0;

        for (int i = 0; i < 6; i++) {
            int target = r.nextInt(6);
            swapCornersForAllRots(i, target);
            if (i != target)
                parity++;
        }

        if (parity % 2 == 1)
            swapCornersForAllRots(0, 1);

        parity = 0;
        for (int i = 0; i < 6; i++) {
            int corner = state.getCorner(i);
            if ((i < 3) == (InnerState.getCornerIndex(corner) < 3)){
                state.twistCorner(i, r.nextInt(2) * 2);
            } else {
                state.twistCorner(i, r.nextInt(2) * 2 + 1);
            }
            parity += InnerState.getCornerOrientation(state.getCorner(i));
        }

        if (parity % 4 == 2){
            state.twistCorner(0, 2);
        }

        parity = 0;
        for (int i = 0; i < 12; i++) {
            int target = r.nextInt(12);
            state.swapEdges(i, target);
            if (target == i)
                parity++;
        }
        if (parity % 2 == 1)
            state.swapEdges(0, 1);

        for (int i = 0; i < 12; i++) {
            state.swapCenters(i, r.nextInt(12));
            state.swapCenters(i + 12, r.nextInt(12) + 12);
        }

        clearMoveStack();
    }

    public static FullFto randomCube(Random r){
        FullFto fto = new FullFto();
        fto.scrambleRandomState(r);
        return fto;
    }

    private final Move[] MOVE_TRANSFORM_ROT1 = {
        Move.B, Move.R, Move.U, Move.D, Move.BR, Move.L, Move.BL, Move.F,
        Move.BP, Move.RP, Move.UP, Move.DP, Move.BRP, Move.LP, Move.BLP, Move.FP
    };

    private final Move[] MOVE_TRANSFORM_ROT2 = {
        Move.L, Move.B, Move.U, Move.D, Move.BL, Move.R, Move.F, Move.BR,
        Move.LP, Move.BP, Move.UP, Move.DP, Move.BLP, Move.RP, Move.FP, Move.BRP,
    };

    public void turn(Move move){
        moveHistory.push(move);
        stateHistory.push(new InnerState(state));
        rot1History.push(new InnerState(rot1));
        rot2History.push(new InnerState(rot2));

        state.turn(move);
        rot1.turn(move);
        rot2.turn(move);
    }

    public void undo(){
        if (!moveHistory.empty()) {
            moveHistory.pop();
            state = stateHistory.pop();
            rot1 = rot1History.pop();
            rot2 = rot2History.pop();
        }
    }

    public boolean isSolved(){
        return state.isSolved();
    }

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
     * Get all previous moves as string
     * Used for sending scramble to TNoodle
     * @return scramble string
     */
    public String history(){
        StringBuilder builder = new StringBuilder();

        for (Move move : moveHistory){
            builder.append(move.toString().replace("P", "'"));
            builder.append(" ");
        }

        return builder.toString();
    }

    private static class InnerState{
        int corners = SOLVED_CORNERS;
        long edges = SOLVED_EDGES;
        long centers = SOLVED_CENTERS;

        long packedCenterIndicesLow = SOLVED_CENTER_INDICES_LOW;
        long packedCenterIndicesHigh = SOLVED_CENTER_INDICES_HIGH;

        private static final int SOLVED_CORNERS;
        private static final long SOLVED_EDGES;
        private static final long SOLVED_CENTERS;
        private static final long SOLVED_CENTER_INDICES_LOW;
        private static final long SOLVED_CENTER_INDICES_HIGH;

        /**
         * Initialization of bit-hacking
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

        public InnerState(int rotation){
            rotate(rotation);
        }

        public InnerState(InnerState state) {
            this.corners = state.corners;
            this.edges = state.edges;
            this.centers = state.centers;
            this.packedCenterIndicesHigh =  state.packedCenterIndicesHigh;
            this.packedCenterIndicesLow = state.packedCenterIndicesLow;

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
         * Get center index in centerIndices
         * @param i index
         * @return center
         */
        int getCenterIndex(int i){
            int packedIndex = i % 12;
            long packed = i < 12 ? packedCenterIndicesLow : packedCenterIndicesHigh;
            return (int) ((packed >> (CENTER_INDEX_BITS * packedIndex)) & CENTER_INDEX_MASK);
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

            int ci1 = getCenterIndex(i1);
            int ci2 = getCenterIndex(i2);
            int ci3 = getCenterIndex(i3);
            setCenterIndex(i1, ci3);
            setCenterIndex(i2, ci1);
            setCenterIndex(i3, ci2);
        }

        void swapCenters(int i1, int i2){
            int c1 = getCenter(i1);
            int c2 = getCenter(i2);

            setCenter(i1, c2);
            setCenter(i2, c1);

            int ci1 = getCenterIndex(i1);
            int ci2 = getCenterIndex(i2);
            setCenterIndex(i1, ci2);
            setCenterIndex(i2, ci1);
        }

        int getCenterOrdinal(int index){
            return (int)(centers >> (2 * index) & CENTER_MASK) + (index > 11 ? 4 : 0);
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
            CenterOrd.U.ordinal(), CenterOrd.U.ordinal(), CenterOrd.U.ordinal(),
            CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(),
            CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(),
            CenterOrd.F.ordinal(), CenterOrd.F.ordinal(), CenterOrd.F.ordinal(),
            CenterOrd.R.ordinal(), CenterOrd.R.ordinal(), CenterOrd.R.ordinal(),
            CenterOrd.B.ordinal(), CenterOrd.B.ordinal(), CenterOrd.B.ordinal(),
            CenterOrd.L.ordinal(), CenterOrd.L.ordinal(), CenterOrd.L.ordinal(),
            CenterOrd.D.ordinal(), CenterOrd.D.ordinal(), CenterOrd.D.ordinal()
        };

        private static final int[] YP_ROTATION_CORNERS = {
            Corner.U_R.ordinal(),
            Corner.U_F.ordinal(),
            Corner.U_L.ordinal(),
            Corner.D_B.ordinal(),
            Corner.D_L.ordinal(),
            Corner.D_R.ordinal(),
        };

        private static final int[] YP_ROTATION_EDGES = {
            Edge.U_R.ordinal(),
            Edge.U_L.ordinal(),
            Edge.U_B.ordinal(),
            Edge.B_BL.ordinal(),
            Edge.L_BL.ordinal(),
            Edge.F_L.ordinal(),
            Edge.R_BR.ordinal(),
            Edge.F_R.ordinal(),
            Edge.B_BR.ordinal(),
            Edge.D_BL.ordinal(),
            Edge.D_F.ordinal(),
            Edge.D_BR.ordinal(),
        };

        private static final int[] YP_ROTATION_CENTERS = {
            CenterOrd.U.ordinal(), CenterOrd.U.ordinal(), CenterOrd.U.ordinal(),
            CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(),
            CenterOrd.F.ordinal(), CenterOrd.F.ordinal(), CenterOrd.F.ordinal(),
            CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(),
            CenterOrd.B.ordinal(), CenterOrd.B.ordinal(), CenterOrd.B.ordinal(),
            CenterOrd.L.ordinal(), CenterOrd.L.ordinal(), CenterOrd.L.ordinal(),
            CenterOrd.R.ordinal(), CenterOrd.R.ordinal(), CenterOrd.R.ordinal(),
            CenterOrd.D.ordinal(), CenterOrd.D.ordinal(), CenterOrd.D.ordinal()
        };


        /**
         * Do y / y' rotations!
         * Note that the solver will always solve to white top
         * green front no matter what orientation the puzzle starts in
         * Can only be done on a solved cube
         * @param n 1 = y, 2 = y'
         */
        void rotate(int n){
            assert(this.isSolved());
            assert(n == 1 || n == 2);

            int[] co = n == 1 ? Y_ROTATION_CORNERS : YP_ROTATION_CORNERS;
            int[] e = n == 1 ? Y_ROTATION_EDGES : YP_ROTATION_EDGES;
            int[] ce = n == 1 ? Y_ROTATION_CENTERS : YP_ROTATION_CENTERS;

            for (int i = 0; i < 6; i++) {
                setCorner(i, encodeCorner(co[i], 0));
            }

            for (int i = 0; i < 12; i++) {
                setEdge(i, e[i]);
            }

            for (int i = 0; i < 24; i++) {
                // % 4:
                //Internal representation of centers are packed to 2-bits per center
                //This works because orbits are a thing on FTO
                setCenter(i, ce[i] % 4);
            }
        }

        private static final int[] FACTORIAL = {1, 1, 2, 6, 24, 120, 720, 5040, 40320};

        public int phaseTwoEdgeIndex() {
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

        private int indexOfEdge(int target) {
            for (int i = 0; i < 12; i++) {
                if (target == getEdge(i)) return i;
            }
            return -1;
        }

        /**
         * Index with [CenterOrd][-]
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

        private long edgeHash(CenterOrd center){
            int centerIndex = center.ordinal();
            int[] e = Arrays.copyOf(EDGES_ON_FACE[centerIndex], 3);

            int firstMatchingEdge = -1;
            for (int i = 0; i < 12; i++){
                int edge = getEdge(i);
                if (contains(e, edge)){
                    firstMatchingEdge = edge;
                    break;
                }
            }

            if (firstMatchingEdge == -1){
                throw new IllegalStateException("Cannot find matching edge in edgeHash()");
            }

            while (e[0] != firstMatchingEdge){
                int tmp = e[2];
                e[2] = e[1];
                e[1] = e[0];
                e[0] = tmp;
            }

            e[0] = indexOfEdge(e[0]);
            e[1] = indexOfEdge(e[1]);
            e[2] = indexOfEdge(e[2]);

            if (contains(e, -1)){
                throw new IllegalStateException("Cannot find matching edge in edgeHash()");
            }

            long hash = 0;
            long[][] edgeKeys = PHASE2_EDGE_KEYS[centerIndex];
            for (int i = 0; i < 3; i++){
                hash ^= edgeKeys[i][e[i]];
            }

            return hash;
        }

        public long phaseOneHash(){
            return edgeHash(CenterOrd.D) ^ centerHash(CenterOrd.D);
        }

        public long phaseTwoHash(){
            long hash = 0;

            hash ^= (centers & 0b111111111111111111000000000000000000000000L);

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

        public boolean areEdgesSolvedOnFace(CenterOrd face, int angle){
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
         * Index with [CenterOrd][-]
         * Helps out areCentersSolvedOnFace
         */
        private static final int[][] CENTERS_ON_FACE = {
            {0, 1, 2},
            {5, 4, 3},
            {7, 8, 6},
            {9, 10, 11},
            {12, 14, 13},
            {16, 17, 15},
            {19, 20, 18},
            {23, 21, 22},
        };

        /**
         * Are all the centers solved on a particular face?
         * @param face face
         * @return t/f
         */
        private boolean areCentersSolvedOnFace(CenterOrd face){
            int centerId = face.id;
            int[] faceCenters = CENTERS_ON_FACE[face.ordinal()];
            for (int i = 0; i < 3; i++) {
                if (getCenterOrdinal(faceCenters[i]) != centerId){
                    return false;
                }
            }
            return true;
        }

        public boolean isFaceSolved(CenterOrd face){
            return areCentersSolvedOnFace(face) &&
                (areEdgesSolvedOnFace(face, 0) ||
                    areEdgesSolvedOnFace(face, 1) ||
                    areEdgesSolvedOnFace(face, 2));
        }


        public void turn(FullFto.Move move){
            switch (move){
                case R:
                    cycleCorners(2, 1, 4);
                    twistCorner(2, 3);
                    twistCorner(1, 2);
                    twistCorner(4, 3);
                    cycleEdges(1, 5, 4);
                    cycleThreeCenters(15, 16, 17);
                    cycleThreeCenters(3, 1, 8);
                    cycleThreeCenters(4, 2, 6);
                    break;
                case L:
                    cycleCorners(0, 2, 3);
                    twistCorner(0, 3);
                    twistCorner(2, 2);
                    twistCorner(3, 3);
                    cycleEdges(2, 3, 8);
                    cycleThreeCenters(12, 13, 14);
                    cycleThreeCenters(0, 3, 10);
                    cycleThreeCenters(2, 5, 9);
                    break;
                case U:
                    cycleCorners(0, 1, 2);
                    cycleEdges(0, 1, 2);
                    cycleThreeCenters(0, 1, 2);
                    cycleThreeCenters(15, 12, 18);
                    cycleThreeCenters(16, 13, 19);
                    break;
                case D:
                    cycleCorners(3, 4, 5);
                    cycleEdges(9, 10, 11);
                    cycleThreeCenters(21, 22, 23);
                    cycleThreeCenters(5, 8, 11);
                    cycleThreeCenters(4, 7, 10);
                    break;
                case F:
                    cycleCorners(2, 4, 3);
                    twistCorner(2, 3);
                    twistCorner(4, 3);
                    twistCorner(3, 2);
                    cycleEdges(4, 9, 3);
                    cycleThreeCenters(3, 4, 5);
                    cycleThreeCenters(13, 17, 21);
                    cycleThreeCenters(15, 22, 14);
                    break;
                case B:
                    cycleCorners(1, 0, 5);
                    twistCorner(1, 3);
                    twistCorner(0, 2);
                    twistCorner(5, 3);
                    cycleEdges(0, 6, 7);
                    cycleThreeCenters(18, 19, 20);
                    cycleThreeCenters(1, 9, 7);
                    cycleThreeCenters(0, 11, 6);
                    break;
                case BR:
                    cycleCorners(1, 5, 4);
                    twistCorner(1, 3);
                    twistCorner(5, 3);
                    twistCorner(4, 2);
                    cycleEdges(5, 7, 10);
                    cycleThreeCenters(6, 7, 8);
                    cycleThreeCenters(16, 20, 22);
                    cycleThreeCenters(17, 18, 23);
                    break;
                case BL:
                    cycleCorners(0, 3, 5);
                    twistCorner(0, 3);
                    twistCorner(3, 3);
                    twistCorner(5, 2);
                    cycleEdges(8, 11, 6);
                    cycleThreeCenters(9, 10, 11);
                    cycleThreeCenters(19, 14, 23);
                    cycleThreeCenters(12, 21, 20);
                    break;
                case RP:
                    cycleCorners(2, 4, 1);
                    twistCorner(2, 2);
                    twistCorner(1, 1);
                    twistCorner(4, 1);
                    cycleEdges(1, 4, 5);
                    cycleThreeCenters(15, 17, 16);
                    cycleThreeCenters(3, 8, 1);
                    cycleThreeCenters(4, 6, 2);
                    break;
                case LP:
                    cycleCorners(0, 3, 2);
                    twistCorner(0, 2);
                    twistCorner(2, 1);
                    twistCorner(3, 1);
                    cycleEdges(2, 8, 3);
                    cycleThreeCenters(12, 14, 13);
                    cycleThreeCenters(0, 10, 3);
                    cycleThreeCenters(2, 9, 5);
                    break;
                case UP:
                    cycleCorners(0, 2, 1);
                    cycleEdges(0, 2, 1);
                    cycleThreeCenters(0, 2, 1);
                    cycleThreeCenters(15, 18, 12);
                    cycleThreeCenters(16, 19, 13);
                    break;
                case DP:
                    cycleCorners(3, 5, 4);
                    cycleEdges(9, 11, 10);
                    cycleThreeCenters(21, 23, 22);
                    cycleThreeCenters(5, 11, 8);
                    cycleThreeCenters(4, 10, 7);
                    break;
                case FP:
                    cycleCorners(2, 3, 4);
                    twistCorner(2, 1);
                    twistCorner(4, 2);
                    twistCorner(3, 1);
                    cycleEdges(4, 3, 9);
                    cycleThreeCenters(3, 5, 4);
                    cycleThreeCenters(13, 21, 17);
                    cycleThreeCenters(15, 14, 22);
                    break;
                case BP:
                    cycleCorners(1, 5, 0);
                    twistCorner(1, 2);
                    twistCorner(0, 1);
                    twistCorner(5, 1);
                    cycleEdges(0, 7, 6);
                    cycleThreeCenters(18, 20, 19);
                    cycleThreeCenters(1, 7, 9);
                    cycleThreeCenters(0, 6, 11);
                    break;
                case BRP:
                    cycleCorners(1, 4, 5);
                    twistCorner(1, 1);
                    twistCorner(5, 2);
                    twistCorner(4, 1);
                    cycleEdges(5, 10, 7);
                    cycleThreeCenters(6, 8, 7);
                    cycleThreeCenters(16, 22, 20);
                    cycleThreeCenters(17, 23, 18);
                    break;
                case BLP:
                    cycleCorners(0, 5, 3);
                    twistCorner(0, 1);
                    twistCorner(3, 2);
                    twistCorner(5, 1);
                    cycleEdges(8, 6, 11);
                    cycleThreeCenters(9, 11, 10);
                    cycleThreeCenters(19, 23, 14);
                    cycleThreeCenters(12, 20, 21);
                    break;
            }
        }

        public boolean isSolved(){
            return corners == SOLVED_CORNERS && edges == SOLVED_EDGES && centers == SOLVED_CENTERS;
        }
    }

    public static void main(String[] args) {
        FullFto fto = new  FullFto();
        System.out.println(fto.isSolved());
        fto.parseAlg("R D L' B' L R B' L' F' R' L B' L' F L' D' F D R F L' BR' U B' R' U F U' BL U D' BL' F L' BR' F L' R L U' R' L B U D B U R' D' B L R' D' R' L ");
        System.out.println(fto.isSolved());
    }

}
