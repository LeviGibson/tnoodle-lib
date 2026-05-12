package cs.fto3phase;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class FullFto {
    /**
     * Internal State
     */

    private InnerState state = new InnerState();
    private InnerState rot1 = new InnerState();
    private InnerState rot2 = new InnerState();


    private boolean trackCenterIndices;
    private static boolean trackCenterIndicesByDefault = true;

    /**
     * History, used for undo() method
     */
    private Stack<InnerState> stateHistory = new Stack<InnerState>();

    private Stack<Long> centerIndicesLowHistory = new Stack<Long>();
    private Stack<Long> centerIndicesHighHistory = new Stack<Long>();
    private Stack<Move> moveHistory = new Stack<Move>();

    /**
     * Main Constructor
     */
    public FullFto(){
        this.trackCenterIndices = trackCenterIndicesByDefault;
    }

    public static void setCenterIndexTrackingDefault(boolean enabled) {
        trackCenterIndicesByDefault = enabled;
    }

    /**
     * Copy Constructor
     * @param fto copy from
     */
    public FullFto(FullFto fto){
        this.state.corners = fto.state.corners;
        this.state.edges = fto.state.edges;
        this.state.centers = fto.state.centers;
        this.trackCenterIndices = fto.trackCenterIndices;

        this.stateHistory.addAll(fto.stateHistory);
        this.centerIndicesLowHistory.addAll(fto.centerIndicesLowHistory);
        this.centerIndicesHighHistory.addAll(fto.centerIndicesHighHistory);
        this.moveHistory.addAll(fto.moveHistory);
    }

    /**
     * Get number of moves applied to the FTO
     * @return number of moves
     */
    public int historyLength() {
        assert (moveHistory.size() == stateHistory.size());
        return moveHistory.size();
    }

    /**
     * Get rid of history. undo() and isRepetition() are the effected methods.
     */
    public void clearMoveStack(){
        centerIndicesLowHistory.clear();
        centerIndicesHighHistory.clear();
        moveHistory.clear();
        stateHistory.clear();
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

    //------------------BITWISE HELPER OPERATIONS------------------//

    private void ensureCenterIndexTracking() {
        if (!trackCenterIndices) {
            throw new IllegalStateException("Center index tracking is disabled for this FullFto instance");
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
        int cornerIndex = state.getCornerIndex(corner);
        int cornerOrientation = state.getCornerOrientation(corner);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation + 2) % 4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        return state.getCenterOrdinal(testSpotOne) == matchingCenterOne &&
                state.getCenterOrdinal(testSpotTwo) == matchingCenterTwo;
    }

    public int triplePairsOnCorner(int cornerLocation){
        int corner = state.getCorner(cornerLocation);
        int cornerIndex = state.getCornerIndex(corner);
        int cornerOrientation = state.getCornerOrientation(corner);

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
        int cornerIndex = state.getCornerIndex(corner);
        int cornerOrientation = state.getCornerOrientation(corner);

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

    private boolean areEdgesSolvedOnFace(CenterOrd face, int angle){
        int faceIndex = face.ordinal();
        int[] faceEdges = EDGES_ON_FACE[faceIndex];
        for (int i = 0; i < 3; i++) {
            if (state.getEdge(faceEdges[(i + angle) % 3]) != faceEdges[i]){
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
            if (state.getCenterOrdinal(faceCenters[i]) != centerId){
                return false;
            }
        }
        return true;
    }

    boolean isFaceSolved(CenterOrd face){
        return areCentersSolvedOnFace(face) &&
            (areEdgesSolvedOnFace(face, 0) ||
            areEdgesSolvedOnFace(face, 1) ||
            areEdgesSolvedOnFace(face, 2));
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

        return isFaceSolved(CenterOrd.R) && isFaceSolved(CenterOrd.L) && isFaceSolved(CenterOrd.B);
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

    private int indexOfEdge(int target) {
        for (int i = 0; i < 12; i++) {
            if (target == state.getEdge(i)) return i;
        }
        return -1;
    }

    private long edgeHash(CenterOrd center){
        int centerIndex = center.ordinal();
        int[] e = Arrays.copyOf(EDGES_ON_FACE[centerIndex], 3);

        int firstMatchingEdge = -1;
        for (int i = 0; i < 12; i++){
            int edge = state.getEdge(i);
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

    private static final int[] FACTORIAL = {1, 1, 2, 6, 24, 120, 720, 5040, 40320};

    public int phaseTwoEdgeIndex() {
        int index = 0;
        int seen = 0;
        for (int i = 8; i >= 0; i--) {
            int edge = state.getEdge(i);
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
            if (state.getCenterOrdinal(i) == centerId){
                hash ^= centerKeys[i];
            }
        }

        return hash;
    }

    public long phaseOneHash(){
        return edgeHash(CenterOrd.D) ^ centerHash(CenterOrd.D);
    }

    public long phaseTwoHash(){
        long hash = 0;

        hash ^= (state.centers & 0b111111111111111111000000000000000000000000L);

        hash ^= edgeHash(CenterOrd.R);
        hash ^= edgeHash(CenterOrd.L);
        hash ^= edgeHash(CenterOrd.B);

        return hash;
    }

    public long phaseThreeHash(){
        long hash = 0;

        for (int i = 0; i < 9; i++) {
            hash ^= PHASE3_EDGE_KEYS[i][state.getEdge(i)];
        }

        for (int i = 0; i < 6; i++) {
            int corner = state.getCorner(i);
            hash ^= PHASE3_CORNER_KEYS[i][state.getCornerIndex(corner)][state.getCornerOrientation(corner)];
        }

        return hash;
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
        ensureCenterIndexTracking();
        long hash = 0;
        for (int cid = 0; cid < 6; cid++) {
            for (int cside = 0; cside < 2; cside++){
                int corner = state.getCorner(cid);
                int ci = state.getCornerIndex(corner);
                int co = state.getCornerOrientation(corner);
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
                int ci = state.getCornerIndex(corner);
                int co = state.getCornerOrientation(corner);

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

    public long hash(int phaseId){
        switch (phaseId){
            case 0:
                return phaseOneHash();
            case 1:
                return phaseTwoHash();
            case 2:
                return phaseThreeHash();
        }

        throw new RuntimeException("Invalid Phase: " + phaseId);
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

        if (parallelMoves[1] == lastLastMove)
            return false;

        return true;
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

    private void scrambleRandomState(Random r){
        int parity = 0;

        for (int i = 0; i < 6; i++) {
            int target = r.nextInt(6);
            state.swapCorners(i, target);
            if (i != target)
                parity++;
        }

        if (parity % 2 == 1)
            state.swapCorners(0, 1);

        parity = 0;
        for (int i = 0; i < 6; i++) {
            int corner = state.getCorner(i);
            if ((i < 3) == (state.getCornerIndex(corner) < 3)){
                state.twistCorner(i, r.nextInt(2) * 2);
            } else {
                state.twistCorner(i, r.nextInt(2) * 2 + 1);
            }
            parity += state.getCornerOrientation(state.getCorner(i));
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

    public void turn(Move move){
        moveHistory.push(move);
        stateHistory.push(new InnerState(state));
        if (trackCenterIndices) {
            centerIndicesLowHistory.push(state.packedCenterIndicesLow);
            centerIndicesHighHistory.push(state.packedCenterIndicesHigh);
        }

        state.turn(move);
    }

    public void undo(){
        if (!moveHistory.empty()) {
            moveHistory.pop();
            state = stateHistory.pop();
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

    public static void main(String[] args) {
        FullFto fto = new  FullFto();
        System.out.println(fto.isSolved());
        fto.parseAlg("R D L' B' L R B' L' F' R' L B' L' F L' D' F D R F L' BR' U B' R' U F U' BL U D' BL' F L' BR' F L' R L U' R' L B U D B U R' D' B L R' D' R' L ");
        System.out.println(fto.isSolved());
    }

}
