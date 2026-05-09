package cs.fto3phase;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

/**
 * Mutable internal representation of a Face Turning Octahedron state.
 *
 * <p>The representation tracks corner permutation and orientation, edge
 * permutation, center colors, and center piece identities used by the phase-two
 * triple pruning logic. Public mutators update the current object in place.</p>
 */
public class FullFto {

    //--------------- State ---------------//

    //Corner has permutation and orientation encoded within int
    //Use methods encodeCorner, getCornerIndex, and getCornerOrientation
    private final int[] corners = new int[6];
    //Only permutation (FTO edges cannot be flipped)
    private final int[] edges = new int[12];
    //Only permutation
    private final int[] centers = new int[24];
    //Used for pruning table generation
    private final int[] centerIndices = new int[24];

    //History of moves applied to the FTO
    //Cleared before search
    public Stack<Move> moveStack;

    //All possible moves
    public enum Move{
        R(0), L(1), U(2), D(3), F(4), B(5), BR(6), BL(7),
        RP(8), LP(9), UP(10), DP(11), FP(12), BP(13), BRP(14), BLP(15);

        final int id;

        Move(int id) {
            this.id = id;
        }
    }

    //--------------- Nitty-Gritty stuff ---------------//

    enum Corner {
        U_L, U_R, U_F, D_L, D_R, D_B
    }

    enum Edge {
        U_B, U_R, U_L, F_L, F_R, R_BR, B_BL, B_BR, L_BL, D_F, D_BR, D_BL
    }

    /**
     * Ordinal values of the centers
     * internal representation of centers is {U, U, U, F, F, F ...}
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
     * Not actually what is stored in centers[]
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

    //Solved state saved for speed
    private static final int[] SOLVED_CORNERS = new int[6];
    private static final int[] SOLVED_EDGES = new int[12];
    private static final int[] SOLVED_CENTERS = new int[24];
    static{
        //Initialize to solved state
        for (int i = 0; i < 6; i++) {
            SOLVED_CORNERS[i] = encodeCorner(i, 0);
        }
        for (int i = 0; i < 12; i++) {
            SOLVED_EDGES[i] = i;
        }
        for (int i = 0; i < 24/3; i++) {
            for (int j = 0; j < 3; j++){
                SOLVED_CENTERS[(i*3)+j] = i;
            }
        }
    }

    /**
     * Corners are encoded in a single integer
     * 2 rightmost bits for orientation
     * remaining bits for permutation
     * @param perm corner index (0-5)
     * @param orientation corner orientation (0, 1, 2, 3)
     * @return encoded corner
     */
    private static int encodeCorner(int perm, int orientation){
        return ((perm << 2) | orientation);
    }

    /**
     * Gets a corner's index
     * @param corner corner from `corners` array
     * @return corner index
     */
    private static int getCornerIndex(int corner){
        return corner>>2;
    }

    /**
     * Gets a corner's orientation
     * @param corner corner from `corners` array
     * @return corner orientation
     */
    private static int getCornerOrientation(int corner){
        return corner&0b11;
    }

    /**
     * Cycles corners without impacting orientation
     * @param i1 first index
     * @param i2 second index
     * @param i3 third index
     */
    private void cycleCorners(int i1, int i2, int i3){
        int[] corners = this.corners;
        int tmp = corners[i3];
        corners[i3] = corners[i2];
        corners[i2] = corners[i1];
        corners[i1] = tmp;
    }

    /**
     * Cycles edges without impacting orientation
     * @param i1 first index
     * @param i2 second index
     * @param i3 third index
     */
    private void cycleEdges(int i1, int i2, int i3){
        int[] edges = this.edges;
        int tmp = edges[i3];
        edges[i3] = edges[i2];
        edges[i2] = edges[i1];
        edges[i1] = tmp;
    }

    /**
     * Cycles centers
     * @param i1 first index
     * @param i2 second index
     * @param i3 third index
     */
    private void cycleThreeCenters(int i1, int i2, int i3){
        int[] centers = this.centers;
        int tmp = centers[i3];
        centers[i3] = centers[i2];
        centers[i2] = centers[i1];
        centers[i1] = tmp;

        int[] centerIndices = this.centerIndices;
        tmp = centerIndices[i3];
        centerIndices[i3] = centerIndices[i2];
        centerIndices[i2] = centerIndices[i1];
        centerIndices[i1] = tmp;
    }

    private void swapCorners(int i1, int i2){
        int[] corners = this.corners;
        int tmp = corners[i2];
        corners[i2] = corners[i1];
        corners[i1] = tmp;
    }

    private void swapEdges(int i1, int i2){
        int[] edges = this.edges;
        int tmp = edges[i2];
        edges[i2] = edges[i1];
        edges[i1] = tmp;
    }

    private void swapCenters(int i1, int i2){
        int[] centers = this.centers;
        int tmp = centers[i2];
        centers[i2] = centers[i1];
        centers[i1] = tmp;
    }


    /**
     * Twist corner
     * @param i index to twist corner at
     * @param dir direction (1=clockwise, 2=180 degrees, 3=counterClockwise)
     */
    private void twistCorner(int i, int dir){
        int corner = corners[i];
        corners[i] = (corner & ~0b11) | ((corner + dir) & 0b11);
    }

    /**
     * Array that shows what centers need to match up with which corners to make a triple
     * MATCHING_CENTERS[getCornerIndex(corner)][corner orientation]
     */
    int[][] MATCHING_CENTERS =  {
        {0, 3, 3, 0}, // U_L
        {0, 2, 2, 0}, // U_R
        {0, 1, 1, 0}, // U_F
        {1, 1, 3, 3}, // D_L
        {2, 2, 1, 1}, // D_R
        {3, 3, 2, 2} // D_B
    };

    int[][] MATCHING_CENTER_INDICES =  {
        {0, 9, 9, 0}, // U_L
        {1, 6, 6, 1}, // U_R
        {2, 3, 3, 2}, // U_F
        {5, 5, 10, 10}, // D_L
        {8, 8, 4, 4}, // D_R
        {11, 11, 7, 7} // D_B
    };

    /**
     * for each corner, where should the matching centers be to make a triple?
     */
    int[][] TRIPLE_LOCATIONS =  {
        {0, 9}, // U_L
        {1, 6}, // U_R
        {2, 3}, // U_F
        {5, 10}, // D_L
        {8, 4}, // D_R
        {11, 7} // D_B
    };

    /**
     * Tests if there is a triple (two centers connected to corner) at location
     * @param cornerLocation location on fto
     * @return is there a triple T/F
     */
    private boolean isTriple(int cornerLocation){
        int cornerIndex = getCornerIndex(corners[cornerLocation]);
        int cornerOrientation = getCornerOrientation(corners[cornerLocation]);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation+2)%4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        return centers[testSpotOne] == matchingCenterOne &&
                centers[testSpotTwo] == matchingCenterTwo;
    }

    /**
     * Tests how many centers are paired to corner for a triple
     * @param cornerLocation location on fto
     * @return 0, 1, 2
     */
    public int triplePairsOnCorner(int cornerLocation){
        int cornerIndex = getCornerIndex(corners[cornerLocation]);
        int cornerOrientation = getCornerOrientation(corners[cornerLocation]);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation+2)%4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        int count = 0;
        if (centers[testSpotOne] == matchingCenterOne)
            count++;
        if (centers[testSpotTwo] == matchingCenterTwo)
            count++;

        return count;
    }

    public int tripleIndexHelper(int cornerLocation){
        int cornerIndex = getCornerIndex(corners[cornerLocation]);
        int cornerOrientation = getCornerOrientation(corners[cornerLocation]);

        int matchingCenterOne = MATCHING_CENTERS[cornerIndex][cornerOrientation];
        int matchingCenterTwo = MATCHING_CENTERS[cornerIndex][(cornerOrientation+2)%4];

        int testSpotOne = TRIPLE_LOCATIONS[cornerLocation][0];
        int testSpotTwo = TRIPLE_LOCATIONS[cornerLocation][1];

        int count = 0;
        if (centers[testSpotOne] == matchingCenterOne)
            count |= 1;
        if (centers[testSpotTwo] == matchingCenterTwo)
            count |= 2;

        return count;
    }

    public int phaseTwoTripleIndex(){
        int index = 0;
        for (int i = 0; i < 6; i++) {
            index |= tripleIndexHelper(i) << 2*i;
        }
        return index;
    }

    /**
     * Index with [CenterOrd][-]
     */
    int[][] EDGES_ON_FACE = {
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
     * Index with [CenterOrd][-]
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
     * Helper function for isFaceSolved
     * @param face face to check
     * @param angle which direction should the face be turned? 0 = no turn
     * @return t/f
     */
    private boolean areEdgesSolvedOnFace(CenterOrd face, int angle){
        int faceId = face.id;
        int[] faceEdges = EDGES_ON_FACE[faceId];
        for (int i = 0; i < 3; i++) {
            if (edges[faceEdges[(i+angle)%3]] != faceEdges[i]){
                return false;
            }
        }
        return true;
    }

    /**
     * Helper function for isFaceSolved
     * @param face face to check
     * @return t/f
     */
    private boolean areCentersSolvedOnFace(CenterOrd face){
        int faceId = face.id;
        int[] faceCenters = CENTERS_ON_FACE[faceId];
        for (int i = 0; i < 3; i++) {
            if (centers[faceCenters[i]] != faceId){
                return false;
            }
        }
        return true;
    }

    /**
     * Are the centers and edges solved on this face? (checks all 3 angles)
     * @param face face to check
     * @return t/f
     */
    boolean isFaceSolved(CenterOrd face){

        return areCentersSolvedOnFace(face) &&
            (areEdgesSolvedOnFace(face, 0) ||
            areEdgesSolvedOnFace(face, 1) ||
            areEdgesSolvedOnFace(face, 2));
    }

    //--------------- Hash Functions ---------------//

    private static final long[][] PHASE2_CENTER_KEYS = new long[8][24];
    private static final long[][][] PHASE2_EDGE_KEYS = new long[12][3][12];
    private static final long[][][] PHASE3_CORNER_KEYS = new long[6][6][4];
    private static final long[][] PHASE3_EDGE_KEYS = new long[9][9];

    // Initialize keys for the in-memory pruning hashes. The values only need to
    // be stable within one JVM because the corresponding pruning tables are also
    // generated in memory during class initialization.
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

    /**
     * Get the hash of a specific center
     * @param center center ordinal
     * @return hash
     */
    private long centerHash(CenterOrd center){
        long hash = 0;
        int centerId = center.id;
        long[] centerKeys = PHASE2_CENTER_KEYS[centerId];

        for (int i = 0; i < 24; i++) {
            if (centers[i] == centerId){
                hash ^= centerKeys[i];
            }
        }

        return hash;
    }

    /**
     * Static helper function
     */
    private static boolean contains(int[] arr, int target) {
        for (int num : arr) {
            if (num == target) return true;
        }
        return false;
    }

    /**
     * Static helper function
     */
    private static int indexOf(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (target == arr[i]) return i;
        }
        return -1;
    }

    /**
     * Internal function - generates hash of three edges on specific face.
     * This function doesn't hash the permutation
     * rather, it hashes the 3-cycle
     *
     * @param center center
     * @return hash
     */
    private long edgeHash(CenterOrd center){
        //This function needs to return the same hash for any version of the same 3-cycle of edges
        int centerId = center.id;

        //Get edges we're looking for
        int[] e = Arrays.copyOf(EDGES_ON_FACE[centerId], 3);

        int firstMachingEdge = -1;

        //Find first matching edge
        for (int i = 0; i < 24; i++){
            if (contains(e, edges[i])){
                firstMachingEdge = edges[i];
                break;
            }
        }

        if (firstMachingEdge == -1){
            throw new IllegalStateException("Cannot find matching edge in edgeHash()");
        }

        //Cycle e until the first edge is the first in the sequence
        while (e[0] != firstMachingEdge){
            int tmp = e[2];
            e[2] = e[1];
            e[1] = e[0];
            e[0] = tmp;
        }

        //Find the indicies of the edges
        e[0] = indexOf(edges, e[0]);
        e[1] = indexOf(edges, e[1]);
        e[2] = indexOf(edges, e[2]);

        if (contains(e, -1)){
            throw new IllegalStateException("Cannot find matching edge in edgeHash()");
        }

        //Generate hash
        long hash = 0;
        long[][] edgeKeys = PHASE2_EDGE_KEYS[centerId];
        for (int i = 0; i < 3; i++){
            hash ^= edgeKeys[i][e[i]];
        }

        //Return hash
        return hash;
    }

    private static final int[] FACTORIAL = {1, 1, 2, 6, 24, 120, 720, 5040, 40320};

    /**
     * Turns the G2 edge state into a permutation index for edges on the R, L,
     * and B faces. This is used for the phase-two edge pruning table lookup.
     *
     * @return phase-two edge pruning table index
     */
    public int phaseTwoEdgeIndex() {
        int index = 0;
        int seen = 0;
        for (int i = 8; i >= 0; i--) {
            int edge = edges[i];
            int smaller = Integer.bitCount(seen & ((1 << edge) - 1));
            index += smaller * FACTORIAL[8 - i];
            seen |= 1 << edge;
        }
        return index / 2;
    }

    static int[] fact = new int[10];
    static int[][][] multinomial = new int[4][4][4];
    static {
        fact[0] = 1;
        for (int i = 1; i < fact.length; i++) {
            fact[i] = fact[i - 1] * i;
        }

        for (int a = 0; a < multinomial.length; a++) {
            for (int b = 0; b < multinomial[a].length; b++) {
                for (int c = 0; c < multinomial[a][b].length; c++) {
                    multinomial[a][b][c] = multinomial(a, b, c);
                }
            }
        }
    }

    // multinomial: n! / (a! b! c!)
    static int multinomial(int a, int b, int c) {
        int n = a + b + c;
        return fact[n] / (fact[a] * fact[b] * fact[c]);
    }

    /**
     * Turns the phase-two center orbit into a multinomial index.
     *
     * @return phase-two center pruning table index
     */
    public int phaseTwoCenterIndex() {
        int count0 = 3, count1 = 3, count2 = 3;
        int index = 0;

        for (int i = 0; i < 9; i++) {
            switch (centers[i + 12] - 4) {
                case 0:
                    count0--;
                    break;
                case 1:
                    if (count0 > 0) {
                        index += multinomial[count0 - 1][count1][count2];
                    }
                    count1--;
                    break;
                case 2:
                    if (count0 > 0) {
                        index += multinomial[count0 - 1][count1][count2];
                    }
                    if (count1 > 0) {
                        index += multinomial[count0][count1 - 1][count2];
                    }
                    count2--;
                    break;
                default:
                    throw new IllegalStateException("Invalid phase-two center value");
                }
        }

        return index;
    }

    /**
     * Hash of the pieces relevant to phase one, where the D face center and
     * edge orbit must be solved.
     *
     * @return phase-one pruning hash
     */
    public long phaseOneHash(){
        return edgeHash(CenterOrd.D) ^ centerHash(CenterOrd.D);
    }

    /**
     * Hash used for lookup during Phase 2 IDA* search
     * Does not contain triple data. That is handled by `packPhaseTwoTripleData`
     * @return hash
     */
    public long phaseTwoCentersHash(){
        long hash = 0;

        hash ^= centerHash(CenterOrd.R);
        hash ^= centerHash(CenterOrd.L);
        hash ^= centerHash(CenterOrd.B);

        hash ^= edgeHash(CenterOrd.R);
        hash ^= edgeHash(CenterOrd.L);
        hash ^= edgeHash(CenterOrd.B);

        return hash;
    }


    /**
     * Gets hash for Phase 3 IDA* search
     * @return hash
     */
    public long phaseThreeHash(){
        long hash = 0;

        for (int i = 0; i < 9; i++) {
            hash ^= PHASE3_EDGE_KEYS[i][edges[i]];
        }

        for (int i = 0; i < 6; i++) {
            hash ^= PHASE3_CORNER_KEYS[i][getCornerIndex(corners[i])][getCornerOrientation(corners[i])];
        }

        return hash;
    }

    /**
     * Packs all triple relations into 64-bit integer for later lookup
     * While this is called phaseTwoTripleHash, it is not actually a hash function
     * @return long for lookup
     */
    public long packPhaseTwoTripleData(){
        long hash = 0;
        for (int cid = 0; cid < 6; cid++) {
            for (int cside = 0; cside < 2; cside++){
                int corner = corners[cid];
                int ci = getCornerIndex(corner);
                int co = getCornerOrientation(corner);
                int matchingCenter = MATCHING_CENTER_INDICES[ci][(co+(2*cside))%4];
                for (long i = 0; i < 24; i++) {
                    if (matchingCenter == centerIndices[(int)i]){
                        hash |= (i << (10*cid + 5*cside));
                    }
                }
            }
        }

        return hash;
    }

    /**
     * Checks phase two triple data to see if it matches
     * See `packPhaseTwoTripleData`
     * @param hash phase two triple data
     * @return t/f
     */
    public boolean checkPhaseTwoTripleData(long hash){
        //Loop over all 6 corner locations
        for (int cid = 0; cid < 6; cid++) {
            //Each triple must have two matching centers
            for (int cside = 0; cside < 2; cside++){
                //Get information about the corner in locaiton `cid`
                int corner = corners[cid];
                int ci = getCornerIndex(corner);
                int co = getCornerOrientation(corner);

                //hash consists of a bunch of packed 5-bit integers
                //These 5-bit integers tell us where the centers should be in relation to the corner
                //This code unpacks them
                long targetIndex = hash & 0b11111;
                hash >>= 5;

                int matchingCenter = MATCHING_CENTERS[ci][(co + (2 * cside))%4];

                //If it doesn't match, then the hash doesn't hit
                if (centers[(int)targetIndex] != matchingCenter){
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Hash with phase index
     * NOTE: 0 = phase 1
     * 1 = phase 2
     * 2 = phase 3
     * 3 = invalid (will throw error)
     * @param phaseId index from zero
     * @return hash
     */
    public long hash(int phaseId){
        switch (phaseId){
            case 0:
                return phaseOneHash();
            case 1:
                return phaseTwoCentersHash();
        }

        throw new RuntimeException("Invalid Phase: " + phaseId);
    }

    //--------------- Main Public Functions ---------------//

    /**
     * Internal FTO representation for fto3phase
     */
    public FullFto() {
        //Initialize to solved state
        for (int i = 0; i < 6; i++) {
            corners[i] = encodeCorner(i, 0);
        }
        for (int i = 0; i < 12; i++) {
            edges[i] = i;
        }

        for (int i = 0; i < 24/3; i++) {
            for (int j = 0; j < 3; j++){
                centers[(i*3)+j] = i;
            }
        }

        for (int i = 0; i < 24; i++) {
            centerIndices[i] = i;
        }

        moveStack = new Stack<>();
    }

    /**
     * Copy constructor - creates a deep copy of another FullFto
     * @param other the FullFto to copy
     */
    public FullFto(FullFto other) {
        System.arraycopy(other.corners, 0, this.corners, 0, other.corners.length);
        System.arraycopy(other.edges, 0, this.edges, 0, other.edges.length);
        System.arraycopy(other.centers, 0, this.centers, 0, other.centers.length);
        System.arraycopy(other.centerIndices, 0, this.centerIndices, 0, other.centerIndices.length);
        this.moveStack = new Stack<>();
        this.moveStack.addAll(other.moveStack);
    }

    /**
     * Number of moves applied to the FTO
     * @return num
     */
    public int historyLength(){
        return this.moveStack.size();
    }

    static final Move[] INVERT_MOVE = {Move.RP, Move.LP, Move.UP, Move.DP, Move.FP, Move.BP, Move.BRP, Move.BLP,
        Move.R, Move.L, Move.U, Move.D, Move.F, Move.B, Move.BR, Move.BL};

    /**
     * Used for detecting repetitions
     */
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

    public boolean isValidPhaseOneFinishingSequence(Move lastMove, Move lastLastMove){
        if (!isPhaseOneBreakingMove(lastMove))
            return false;

        Move[] parallelMoves = PARALLEL_MOVES[lastMove.id];
        if (parallelMoves[0] == lastLastMove)
            return false;

        if (parallelMoves[1] == lastLastMove)
            return false;

        return true;
    }

    /**
     * Undo the last move performed on FTO
     */
    public void undo() {
        turn(INVERT_MOVE[moveStack.pop().id]);
        moveStack.pop();
    }

    /**
     * Is this FTO solved?
     * @return true/false
     */
    public boolean isSolved(){
        return Arrays.equals(corners, SOLVED_CORNERS) &&
            Arrays.equals(edges, SOLVED_EDGES) &&
            Arrays.equals(centers, SOLVED_CENTERS);
    }

    /**
     * Get all previous moves as string
     * Used for sending scramble to TNoodle
     * @return scramble string
     */
    public String history(){
        StringBuilder builder = new StringBuilder();

        for (Move move : moveStack){
            builder.append(move.toString().replace("P", "'"));
            builder.append(" ");
        }

        return builder.toString();
    }

    /**
     * Clears all history. pop() will no longer do anything (until you make more moves)
     */
    public void clearMoveStack(){
        moveStack.clear();
    }

    /**
     * Phase one is centers and edges solved on the D face
     * @return t/f
     */
    public boolean isPhaseOne(){
        if (centers[21] != 7 ||
            centers[22] != 7 ||
            centers[23] != 7)
            return false;
        return (edges[9] == 9 &&
            edges[10] == 10 &&
            edges[11] == 11) ||
                (edges[9] == 11 &&
                    edges[10] == 9 &&
                    edges[11] == 10) ||
                (edges[9] == 10 &&
                    edges[10] == 11 &&
                    edges[11] == 9);
    }

    /**
     * Phase two is Octominx reduction
     * Can be solved with moveset [D B R L]
     * @return t/f
     */
    public boolean isPhaseTwo(){
        //All the triples must be made
        for (int i = 0; i < 6; i++) {
            if (!isTriple(i))
                return false;
        }

        //All the centers on one orbit must be solved
        return isFaceSolved(CenterOrd.R) && isFaceSolved(CenterOrd.L) && isFaceSolved(CenterOrd.B);
    }

    /**
     * Number of corner-center triples in G2 triple locations
     * @return num
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
     * Number of corner-center pairs in G2 triple locations
     * @return num
     */
    public int triplePairCount(){
        int count = 0;

        for (int i = 0; i < 6; i++) {
            count += triplePairsOnCorner(i);
        }

        return count;
    }

    /**
     * Scramble `this` to a random G2 state
     * @param r random
     */
    public void scrambleRandomG2State(Random r){
        scrambleRandomG2State(r, 500);
    }

    /**
     * Scramble `this` to a random G2 state
     * @param r random
     * @param numMoves number of phase-two moves to apply before clearing history
     */
    public void scrambleRandomG2State(Random r, int numMoves){
        Move[] PHASE_TWO_MOVES = {Move.U, Move.R, Move.L, Move.D, Move.B, Move.UP, Move.RP, Move.LP, Move.DP, Move.BP};

        for (int i = 0; i < numMoves; i++) {
            Move move;

            do{
                move = PHASE_TWO_MOVES[r.nextInt(10)];
            } while (isRepetition(move));

            turn(move);
        }

        clearMoveStack();
    }

    /**
     * Turns self into a random state
     * @param r secure random
     */
    private void scrambleRandomState(Random r){
        //Randomize corner permutation
        //Swap each corner with a random corner (can be itself)
        int parity = 0;

        for (int i = 0; i < 6; i++) {
            int target = r.nextInt(6);
            swapCorners(i, target);
            if (i != target)
                parity++;
        }

        if (parity % 2 == 1)
            swapCorners(0, 1);

        //Randomize corner orientation
        parity = 0;
        for (int i = 0; i < 6; i++) {
            if ((i < 3) == (getCornerIndex(corners[i]) < 3)){
                twistCorner(i, r.nextInt(2)*2);
            } else {
                twistCorner(i, r.nextInt(2)*2+1);
            }
            parity += getCornerOrientation(corners[i]);
        }

        if (parity % 4 == 2){
            twistCorner(0, 2);
        }

        //Randomize edge permutation
        //Swap each corner with a random corner (can be itself)
        parity = 0;
        for (int i = 0; i < 12; i++) {
            int target = r.nextInt(12);
            swapEdges(i, target);
            if (target == i)
                parity++;
        }
        if (parity % 2 == 1)
            swapEdges(0, 1);

        //Randomize center permutation
        //Swap each corner with a random corner (can be itself)
        for (int i = 0; i < 12; i++) {
            swapCenters(i, r.nextInt(12));
            swapCenters(i+12, (r.nextInt(12))+12);
        }

        clearMoveStack();
    }


    /**
     * Returns random state FTO. Fast function, no searches performed.
     * @param r seeded random
     * @return random state FTO
     */
    public static FullFto randomCube(Random r){
        FullFto fto = new FullFto();
        fto.scrambleRandomState(r);
        return fto;
    }

    /**
     * Detects redundant move sequences like R R, R R', and R BL R.
     *
     * @param move move being considered
     * @return true when the move repeats or immediately cancels recent history
     */
    boolean isRepetition(Move move){

        if (moveStack.isEmpty())
            return false;

        Move lastMove = moveStack.peek();
        int moveId = move.id;
        if (move == lastMove || INVERT_MOVE[moveId] == lastMove) {
            return true;
        }

        if (moveStack.size() < 2){
            return false;
        }

        Move lastLastmove = moveStack.get(moveStack.size()-2);

        if (move == lastLastmove || INVERT_MOVE[moveId] == lastLastmove) {
            Move[] parallelMoves = PARALLEL_MOVES[lastLastmove.id];
            return parallelMoves[0] == move || parallelMoves[1] == move;
        }

        return false;
    }


    /**
     * Turn the FTO! This function
     * 1. Cycles the pieces based on the move parameter
     * 2. Adds the move to an internal stack
     * @param move move
     */
    public void turn(Move move){
        moveStack.push(move);
        switch (move){
            case R:
                cycleCorners(2,
                    1,
                    4);

                twistCorner(2, 3);
                twistCorner(1, 2);
                twistCorner(4, 3);

                cycleEdges(1,
                    5,
                    4);

                cycleThreeCenters(15,
                    16,
                    17);

                cycleThreeCenters(3,
                    1,
                    8);

                cycleThreeCenters(4,
                    2,
                    6);
                break;

            case L:
                cycleCorners(0,
                    2,
                    3);

                twistCorner(0, 3);
                twistCorner(2, 2);
                twistCorner(3, 3);

                cycleEdges(2,
                    3,
                    8);

                cycleThreeCenters(12,
                    13,
                    14);

                cycleThreeCenters(0,
                    3,
                    10);

                cycleThreeCenters(2,
                    5,
                    9);
                break;
            case U:
                cycleCorners(0,
                    1,
                    2);

                cycleEdges(0,
                    1,
                    2);

                cycleThreeCenters(0,
                    1,
                    2);

                cycleThreeCenters(15,
                    12,
                    18);

                cycleThreeCenters(16,
                    13,
                    19);
                break;
            case D:
                cycleCorners(3,
                    4,
                    5);

                cycleEdges(9,
                    10,
                    11);

                cycleThreeCenters(21,
                    22,
                    23);

                cycleThreeCenters(5,
                    8,
                    11);

                cycleThreeCenters(4,
                    7,
                    10);
                break;
            case F:
                cycleCorners(2,
                    4,
                    3);

                twistCorner(2, 3);
                twistCorner(4, 3);
                twistCorner(3, 2);

                cycleEdges(4,
                    9,
                    3);

                cycleThreeCenters(3,
                    4,
                    5);

                cycleThreeCenters(13,
                    17,
                    21);

                cycleThreeCenters(15,
                    22,
                    14);
                break;
            case B:
                cycleCorners(1,
                    0,
                    5);

                twistCorner(1, 3);
                twistCorner(0, 2);
                twistCorner(5, 3);

                cycleEdges(0,
                    6,
                    7);

                cycleThreeCenters(18,
                    19,
                    20);

                cycleThreeCenters(1,
                    9,
                    7);

                cycleThreeCenters(0,
                    11,
                    6);
                break;
            case BR:
                cycleCorners(1,
                    5,
                    4);

                twistCorner(1, 3);
                twistCorner(5, 3);
                twistCorner(4, 2);

                cycleEdges(5,
                    7,
                    10);

                cycleThreeCenters(6,
                    7,
                    8);

                cycleThreeCenters(16,
                    20,
                    22);

                cycleThreeCenters(17,
                    18,
                    23);
                break;
            case BL:
                cycleCorners(0,
                    3,
                    5);

                twistCorner(0, 3);
                twistCorner(3, 3);
                twistCorner(5, 2);

                cycleEdges(8,
                    11,
                    6);

                cycleThreeCenters(9,
                    10,
                    11);

                cycleThreeCenters(19,
                    14,
                    23);

                cycleThreeCenters(12,
                    21,
                    20);
                break;
            case RP:
                cycleCorners(2,
                    4,
                    1);

                twistCorner(2, 2);
                twistCorner(1, 1);
                twistCorner(4, 1);

                cycleEdges(1,
                    4,
                    5);

                cycleThreeCenters(15,
                    17,
                    16);

                cycleThreeCenters(3,
                    8,
                    1);

                cycleThreeCenters(4,
                    6,
                    2);
                break;
            case LP:
                cycleCorners(0,
                    3,
                    2);

                twistCorner(0, 2);
                twistCorner(2, 1);
                twistCorner(3, 1);

                cycleEdges(2,
                    8,
                    3);

                cycleThreeCenters(12,
                    14,
                    13);

                cycleThreeCenters(0,
                    10,
                    3);

                cycleThreeCenters(2,
                    9,
                    5);
                break;
            case UP:
                cycleCorners(0,
                    2,
                    1);

                cycleEdges(0,
                    2,
                    1);

                cycleThreeCenters(0,
                    2,
                    1);

                cycleThreeCenters(15,
                    18,
                    12);

                cycleThreeCenters(16,
                    19,
                    13);
                break;
            case DP:
                cycleCorners(3,
                    5,
                    4);

                cycleEdges(9,
                    11,
                    10);

                cycleThreeCenters(21,
                    23,
                    22);

                cycleThreeCenters(5,
                    11,
                    8);

                cycleThreeCenters(4,
                    10,
                    7);
                break;
            case FP:
                cycleCorners(2,
                    3,
                    4);

                twistCorner(2, 1);
                twistCorner(4, 2);
                twistCorner(3, 1);

                cycleEdges(4,
                    3,
                    9);

                cycleThreeCenters(3,
                    5,
                    4);

                cycleThreeCenters(13,
                    21,
                    17);

                cycleThreeCenters(15,
                    14,
                    22);
                break;
            case BP:
                cycleCorners(1,
                    5,
                    0);

                twistCorner(1, 2);
                twistCorner(0, 1);
                twistCorner(5, 1);

                cycleEdges(0,
                    7,
                    6);

                cycleThreeCenters(18,
                    20,
                    19);

                cycleThreeCenters(1,
                    7,
                    9);

                cycleThreeCenters(0,
                    6,
                    11);
                break;
            case BRP:
                cycleCorners(1,
                    4,
                    5);

                twistCorner(1, 1);
                twistCorner(5, 2);
                twistCorner(4, 1);

                cycleEdges(5,
                    10,
                    7);

                cycleThreeCenters(6,
                    8,
                    7);

                cycleThreeCenters(16,
                    22,
                    20);

                cycleThreeCenters(17,
                    23,
                    18);
                break;
            case BLP:
                cycleCorners(0,
                    5,
                    3);

                twistCorner(0, 1);
                twistCorner(3, 2);
                twistCorner(5, 1);

                cycleEdges(8,
                    6,
                    11);

                cycleThreeCenters(9,
                    11,
                    10);

                cycleThreeCenters(19,
                    23,
                    14);

                cycleThreeCenters(12,
                    20,
                    21);
                break;
        }
    }

    public static boolean isPhaseOneBreakingMove(Move move){
        return move == Move.F || move == Move.FP || move == Move.BR || move == Move.BRP || move == Move.BL || move == Move.BLP;
    }

    /**
     * Parse an algorithm string and apply each move.
     * Format: space-separated moves, e.g. "R D' F BL' U"
     * @param alg algorithm string
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
}
