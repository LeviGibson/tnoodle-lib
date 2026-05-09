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
    public enum Move{R, L, U, D, F, B, BR, BL, RP, LP, UP, DP, FP, BP, BRP, BLP}

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
        U, F, BR, BL, L, R, B, D
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
        {CenterOrd.U.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.U.ordinal()}, // U_L
        {CenterOrd.U.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.U.ordinal()}, // U_R
        {CenterOrd.U.ordinal(), CenterOrd.F.ordinal(), CenterOrd.F.ordinal(), CenterOrd.U.ordinal()}, // U_F
        {CenterOrd.F.ordinal(), CenterOrd.F.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal()}, // D_L
        {CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.F.ordinal(), CenterOrd.F.ordinal()}, // D_R
        {CenterOrd.BL.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.BR.ordinal()} // D_B
    };

    int[][] MATCHING_CENTER_INDICES =  {
        {CenterInd.U_BL.ordinal(), CenterInd.BL_U.ordinal(), CenterInd.BL_U.ordinal(), CenterInd.U_BL.ordinal()}, // U_L
        {CenterInd.U_BR.ordinal(), CenterInd.BR_U.ordinal(), CenterInd.BR_U.ordinal(), CenterInd.U_BR.ordinal()}, // U_R
        {CenterInd.U_F.ordinal(), CenterInd.F_U.ordinal(), CenterInd.F_U.ordinal(), CenterInd.U_F.ordinal()}, // U_F
        {CenterInd.F_BL.ordinal(), CenterInd.F_BL.ordinal(), CenterInd.BL_F.ordinal(), CenterInd.BL_F.ordinal()}, // D_L
        {CenterInd.BR_F.ordinal(), CenterInd.BR_F.ordinal(), CenterInd.F_BR.ordinal(), CenterInd.F_BR.ordinal()}, // D_R
        {CenterInd.BL_BR.ordinal(), CenterInd.BL_BR.ordinal(), CenterInd.BR_BL.ordinal(), CenterInd.BR_BL.ordinal()} // D_B
    };

    /**
     * for each corner, where should the matching centers be to make a triple?
     */
    int[][] TRIPLE_LOCATIONS =  {
        {CenterInd.U_BL.ordinal(), CenterInd.BL_U.ordinal()}, // U_L
        {CenterInd.U_BR.ordinal(), CenterInd.BR_U.ordinal()}, // U_R
        {CenterInd.U_F.ordinal(), CenterInd.F_U.ordinal()}, // U_F
        {CenterInd.F_BL.ordinal(), CenterInd.BL_F.ordinal()}, // D_L
        {CenterInd.BR_F.ordinal(), CenterInd.F_BR.ordinal()}, // D_R
        {CenterInd.BL_BR.ordinal(), CenterInd.BR_BL.ordinal()} // D_B
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
        {Edge.U_B.ordinal(), Edge.U_L.ordinal(), Edge.U_R.ordinal()},
        {Edge.F_L.ordinal(), Edge.D_F.ordinal(), Edge.F_R.ordinal()},
        {Edge.B_BR.ordinal(), Edge.D_BR.ordinal(), Edge.R_BR.ordinal()},
        {Edge.B_BL.ordinal(), Edge.D_BL.ordinal(), Edge.L_BL.ordinal()},
        {Edge.L_BL.ordinal(), Edge.F_L.ordinal(), Edge.U_L.ordinal()},
        {Edge.R_BR.ordinal(), Edge.U_R.ordinal(), Edge.F_R.ordinal()},
        {Edge.B_BL.ordinal(), Edge.B_BR.ordinal(), Edge.U_B.ordinal()},
        {Edge.D_BL.ordinal(), Edge.D_BR.ordinal(), Edge.D_F.ordinal()},
    };

    /**
     * Index with [CenterOrd][-]
     */
    private static final int[][] CENTERS_ON_FACE = {
        {CenterInd.U_BL.ordinal(), CenterInd.U_BR.ordinal(), CenterInd.U_F.ordinal()},
        {CenterInd.F_BL.ordinal(), CenterInd.F_BR.ordinal(), CenterInd.F_U.ordinal()},
        {CenterInd.BR_BL.ordinal(), CenterInd.BR_F.ordinal(), CenterInd.BR_U.ordinal()},
        {CenterInd.BL_U.ordinal(), CenterInd.BL_F.ordinal(), CenterInd.BL_BR.ordinal()},
        {CenterInd.L_B.ordinal(), CenterInd.L_D.ordinal(), CenterInd.L_R.ordinal()},
        {CenterInd.R_B.ordinal(), CenterInd.R_D.ordinal(), CenterInd.R_L.ordinal()},
        {CenterInd.B_L.ordinal(), CenterInd.B_D.ordinal(), CenterInd.B_R.ordinal()},
        {CenterInd.D_B.ordinal(), CenterInd.D_L.ordinal(), CenterInd.D_R.ordinal()},
    };

    /**
     * Helper function for isFaceSolved
     * @param face face to check
     * @param angle which direction should the face be turned? 0 = no turn
     * @return t/f
     */
    private boolean areEdgesSolvedOnFace(CenterOrd face, int angle){
        for (int i = 0; i < 3; i++) {
            if (edges[EDGES_ON_FACE[face.ordinal()][(i+angle)%3]] != EDGES_ON_FACE[face.ordinal()][i]){
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
        for (int i = 0; i < 3; i++) {
            if (centers[CENTERS_ON_FACE[face.ordinal()][i]] != face.ordinal()){
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

        for (int i = 0; i < 24; i++) {
            if (centers[i] == center.ordinal()){
                hash ^= PHASE2_CENTER_KEYS[center.ordinal()][i];
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

        //Get edges we're looking for
        int[] e = Arrays.copyOf(EDGES_ON_FACE[center.ordinal()], 3);

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
        for (int i = 0; i < 3; i++){
            hash ^= PHASE2_EDGE_KEYS[center.ordinal()][i][e[i]];
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

        if (PARALLEL_MOVES[lastMove.ordinal()][0] == lastLastMove)
            return false;

        if (PARALLEL_MOVES[lastMove.ordinal()][1] == lastLastMove)
            return false;

        return true;
    }

    /**
     * Undo the last move performed on FTO
     */
    public void undo() {
        turn(INVERT_MOVE[moveStack.pop().ordinal()]);
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
        if (centers[CenterInd.D_L.ordinal()] != CenterOrd.D.ordinal() ||
            centers[CenterInd.D_R.ordinal()] != CenterOrd.D.ordinal() ||
            centers[CenterInd.D_B.ordinal()] != CenterOrd.D.ordinal())
            return false;
        return (edges[Edge.D_F.ordinal()] == Edge.D_F.ordinal() &&
            edges[Edge.D_BR.ordinal()] == Edge.D_BR.ordinal() &&
            edges[Edge.D_BL.ordinal()] == Edge.D_BL.ordinal()) ||
                (edges[Edge.D_F.ordinal()] == Edge.D_BL.ordinal() &&
                    edges[Edge.D_BR.ordinal()] == Edge.D_F.ordinal() &&
                    edges[Edge.D_BL.ordinal()] == Edge.D_BR.ordinal()) ||
                (edges[Edge.D_F.ordinal()] == Edge.D_BR.ordinal() &&
                    edges[Edge.D_BR.ordinal()] == Edge.D_BL.ordinal() &&
                    edges[Edge.D_BL.ordinal()] == Edge.D_F.ordinal());
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
        if (move == lastMove || INVERT_MOVE[move.ordinal()] == lastMove) {
            return true;
        }

        if (moveStack.size() < 2){
            return false;
        }

        Move lastLastmove = moveStack.get(moveStack.size()-2);

        if (move == lastLastmove || INVERT_MOVE[move.ordinal()] == lastLastmove) {
            return PARALLEL_MOVES[lastLastmove.ordinal()][0] == move || PARALLEL_MOVES[lastLastmove.ordinal()][1] == move;
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
                cycleCorners(Corner.U_F.ordinal(),
                    Corner.U_R.ordinal(),
                    Corner.D_R.ordinal());

                twistCorner(Corner.U_F.ordinal(), 3);
                twistCorner(Corner.U_R.ordinal(), 2);
                twistCorner(Corner.D_R.ordinal(), 3);

                cycleEdges(Edge.U_R.ordinal(),
                    Edge.R_BR.ordinal(),
                    Edge.F_R.ordinal());

                cycleThreeCenters(CenterInd.R_L.ordinal(),
                    CenterInd.R_B.ordinal(),
                    CenterInd.R_D.ordinal());

                cycleThreeCenters(CenterInd.F_U.ordinal(),
                    CenterInd.U_BR.ordinal(),
                    CenterInd.BR_F.ordinal());

                cycleThreeCenters(CenterInd.F_BR.ordinal(),
                    CenterInd.U_F.ordinal(),
                    CenterInd.BR_U.ordinal());
                break;

            case L:
                cycleCorners(Corner.U_L.ordinal(),
                    Corner.U_F.ordinal(),
                    Corner.D_L.ordinal());

                twistCorner(Corner.U_L.ordinal(), 3);
                twistCorner(Corner.U_F.ordinal(), 2);
                twistCorner(Corner.D_L.ordinal(), 3);

                cycleEdges(Edge.U_L.ordinal(),
                    Edge.F_L.ordinal(),
                    Edge.L_BL.ordinal());

                cycleThreeCenters(CenterInd.L_B.ordinal(),
                    CenterInd.L_R.ordinal(),
                    CenterInd.L_D.ordinal());

                cycleThreeCenters(CenterInd.U_BL.ordinal(),
                    CenterInd.F_U.ordinal(),
                    CenterInd.BL_F.ordinal());

                cycleThreeCenters(CenterInd.U_F.ordinal(),
                    CenterInd.F_BL.ordinal(),
                    CenterInd.BL_U.ordinal());
                break;
            case U:
                cycleCorners(Corner.U_L.ordinal(),
                    Corner.U_R.ordinal(),
                    Corner.U_F.ordinal());

                cycleEdges(Edge.U_B.ordinal(),
                    Edge.U_R.ordinal(),
                    Edge.U_L.ordinal());

                cycleThreeCenters(CenterInd.U_BL.ordinal(),
                    CenterInd.U_BR.ordinal(),
                    CenterInd.U_F.ordinal());

                cycleThreeCenters(CenterInd.R_L.ordinal(),
                    CenterInd.L_B.ordinal(),
                    CenterInd.B_R.ordinal());

                cycleThreeCenters(CenterInd.R_B.ordinal(),
                    CenterInd.L_R.ordinal(),
                    CenterInd.B_L.ordinal());
                break;
            case D:
                cycleCorners(Corner.D_L.ordinal(),
                    Corner.D_R.ordinal(),
                    Corner.D_B.ordinal());

                cycleEdges(Edge.D_F.ordinal(),
                    Edge.D_BR.ordinal(),
                    Edge.D_BL.ordinal());

                cycleThreeCenters(CenterInd.D_L.ordinal(),
                    CenterInd.D_R.ordinal(),
                    CenterInd.D_B.ordinal());

                cycleThreeCenters(CenterInd.F_BL.ordinal(),
                    CenterInd.BR_F.ordinal(),
                    CenterInd.BL_BR.ordinal());

                cycleThreeCenters(CenterInd.F_BR.ordinal(),
                    CenterInd.BR_BL.ordinal(),
                    CenterInd.BL_F.ordinal());
                break;
            case F:
                cycleCorners(Corner.U_F.ordinal(),
                    Corner.D_R.ordinal(),
                    Corner.D_L.ordinal());

                twistCorner(Corner.U_F.ordinal(), 3);
                twistCorner(Corner.D_R.ordinal(), 3);
                twistCorner(Corner.D_L.ordinal(), 2);

                cycleEdges(Edge.F_R.ordinal(),
                    Edge.D_F.ordinal(),
                    Edge.F_L.ordinal());

                cycleThreeCenters(CenterInd.F_U.ordinal(),
                    CenterInd.F_BR.ordinal(),
                    CenterInd.F_BL.ordinal());

                cycleThreeCenters(CenterInd.L_R.ordinal(),
                    CenterInd.R_D.ordinal(),
                    CenterInd.D_L.ordinal());

                cycleThreeCenters(CenterInd.R_L.ordinal(),
                    CenterInd.D_R.ordinal(),
                    CenterInd.L_D.ordinal());
                break;
            case B:
                cycleCorners(Corner.U_R.ordinal(),
                    Corner.U_L.ordinal(),
                    Corner.D_B.ordinal());

                twistCorner(Corner.U_R.ordinal(), 3);
                twistCorner(Corner.U_L.ordinal(), 2);
                twistCorner(Corner.D_B.ordinal(), 3);

                cycleEdges(Edge.U_B.ordinal(),
                    Edge.B_BL.ordinal(),
                    Edge.B_BR.ordinal());

                cycleThreeCenters(CenterInd.B_R.ordinal(),
                    CenterInd.B_L.ordinal(),
                    CenterInd.B_D.ordinal());

                cycleThreeCenters(CenterInd.U_BR.ordinal(),
                    CenterInd.BL_U.ordinal(),
                    CenterInd.BR_BL.ordinal());

                cycleThreeCenters(CenterInd.U_BL.ordinal(),
                    CenterInd.BL_BR.ordinal(),
                    CenterInd.BR_U.ordinal());
                break;
            case BR:
                cycleCorners(Corner.U_R.ordinal(),
                    Corner.D_B.ordinal(),
                    Corner.D_R.ordinal());

                twistCorner(Corner.U_R.ordinal(), 3);
                twistCorner(Corner.D_B.ordinal(), 3);
                twistCorner(Corner.D_R.ordinal(), 2);

                cycleEdges(Edge.R_BR.ordinal(),
                    Edge.B_BR.ordinal(),
                    Edge.D_BR.ordinal());

                cycleThreeCenters(CenterInd.BR_U.ordinal(),
                    CenterInd.BR_BL.ordinal(),
                    CenterInd.BR_F.ordinal());

                cycleThreeCenters(CenterInd.R_B.ordinal(),
                    CenterInd.B_D.ordinal(),
                    CenterInd.D_R.ordinal());

                cycleThreeCenters(CenterInd.R_D.ordinal(),
                    CenterInd.B_R.ordinal(),
                    CenterInd.D_B.ordinal());
                break;
            case BL:
                cycleCorners(Corner.U_L.ordinal(),
                    Corner.D_L.ordinal(),
                    Corner.D_B.ordinal());

                twistCorner(Corner.U_L.ordinal(), 3);
                twistCorner(Corner.D_L.ordinal(), 3);
                twistCorner(Corner.D_B.ordinal(), 2);

                cycleEdges(Edge.L_BL.ordinal(),
                    Edge.D_BL.ordinal(),
                    Edge.B_BL.ordinal());

                cycleThreeCenters(CenterInd.BL_U.ordinal(),
                    CenterInd.BL_F.ordinal(),
                    CenterInd.BL_BR.ordinal());

                cycleThreeCenters(CenterInd.B_L.ordinal(),
                    CenterInd.L_D.ordinal(),
                    CenterInd.D_B.ordinal());

                cycleThreeCenters(CenterInd.L_B.ordinal(),
                    CenterInd.D_L.ordinal(),
                    CenterInd.B_D.ordinal());
                break;
            case RP:
                cycleCorners(Corner.U_F.ordinal(),
                    Corner.D_R.ordinal(),
                    Corner.U_R.ordinal());

                twistCorner(Corner.U_F.ordinal(), 2);
                twistCorner(Corner.U_R.ordinal(), 1);
                twistCorner(Corner.D_R.ordinal(), 1);

                cycleEdges(Edge.U_R.ordinal(),
                    Edge.F_R.ordinal(),
                    Edge.R_BR.ordinal());

                cycleThreeCenters(CenterInd.R_L.ordinal(),
                    CenterInd.R_D.ordinal(),
                    CenterInd.R_B.ordinal());

                cycleThreeCenters(CenterInd.F_U.ordinal(),
                    CenterInd.BR_F.ordinal(),
                    CenterInd.U_BR.ordinal());

                cycleThreeCenters(CenterInd.F_BR.ordinal(),
                    CenterInd.BR_U.ordinal(),
                    CenterInd.U_F.ordinal());
                break;
            case LP:
                cycleCorners(Corner.U_L.ordinal(),
                    Corner.D_L.ordinal(),
                    Corner.U_F.ordinal());

                twistCorner(Corner.U_L.ordinal(), 2);
                twistCorner(Corner.U_F.ordinal(), 1);
                twistCorner(Corner.D_L.ordinal(), 1);

                cycleEdges(Edge.U_L.ordinal(),
                    Edge.L_BL.ordinal(),
                    Edge.F_L.ordinal());

                cycleThreeCenters(CenterInd.L_B.ordinal(),
                    CenterInd.L_D.ordinal(),
                    CenterInd.L_R.ordinal());

                cycleThreeCenters(CenterInd.U_BL.ordinal(),
                    CenterInd.BL_F.ordinal(),
                    CenterInd.F_U.ordinal());

                cycleThreeCenters(CenterInd.U_F.ordinal(),
                    CenterInd.BL_U.ordinal(),
                    CenterInd.F_BL.ordinal());
                break;
            case UP:
                cycleCorners(Corner.U_L.ordinal(),
                    Corner.U_F.ordinal(),
                    Corner.U_R.ordinal());

                cycleEdges(Edge.U_B.ordinal(),
                    Edge.U_L.ordinal(),
                    Edge.U_R.ordinal());

                cycleThreeCenters(CenterInd.U_BL.ordinal(),
                    CenterInd.U_F.ordinal(),
                    CenterInd.U_BR.ordinal());

                cycleThreeCenters(CenterInd.R_L.ordinal(),
                    CenterInd.B_R.ordinal(),
                    CenterInd.L_B.ordinal());

                cycleThreeCenters(CenterInd.R_B.ordinal(),
                    CenterInd.B_L.ordinal(),
                    CenterInd.L_R.ordinal());
                break;
            case DP:
                cycleCorners(Corner.D_L.ordinal(),
                    Corner.D_B.ordinal(),
                    Corner.D_R.ordinal());

                cycleEdges(Edge.D_F.ordinal(),
                    Edge.D_BL.ordinal(),
                    Edge.D_BR.ordinal());

                cycleThreeCenters(CenterInd.D_L.ordinal(),
                    CenterInd.D_B.ordinal(),
                    CenterInd.D_R.ordinal());

                cycleThreeCenters(CenterInd.F_BL.ordinal(),
                    CenterInd.BL_BR.ordinal(),
                    CenterInd.BR_F.ordinal());

                cycleThreeCenters(CenterInd.F_BR.ordinal(),
                    CenterInd.BL_F.ordinal(),
                    CenterInd.BR_BL.ordinal());
                break;
            case FP:
                cycleCorners(Corner.U_F.ordinal(),
                    Corner.D_L.ordinal(),
                    Corner.D_R.ordinal());

                twistCorner(Corner.U_F.ordinal(), 1);
                twistCorner(Corner.D_R.ordinal(), 2);
                twistCorner(Corner.D_L.ordinal(), 1);

                cycleEdges(Edge.F_R.ordinal(),
                    Edge.F_L.ordinal(),
                    Edge.D_F.ordinal());

                cycleThreeCenters(CenterInd.F_U.ordinal(),
                    CenterInd.F_BL.ordinal(),
                    CenterInd.F_BR.ordinal());

                cycleThreeCenters(CenterInd.L_R.ordinal(),
                    CenterInd.D_L.ordinal(),
                    CenterInd.R_D.ordinal());

                cycleThreeCenters(CenterInd.R_L.ordinal(),
                    CenterInd.L_D.ordinal(),
                    CenterInd.D_R.ordinal());
                break;
            case BP:
                cycleCorners(Corner.U_R.ordinal(),
                    Corner.D_B.ordinal(),
                    Corner.U_L.ordinal());

                twistCorner(Corner.U_R.ordinal(), 2);
                twistCorner(Corner.U_L.ordinal(), 1);
                twistCorner(Corner.D_B.ordinal(), 1);

                cycleEdges(Edge.U_B.ordinal(),
                    Edge.B_BR.ordinal(),
                    Edge.B_BL.ordinal());

                cycleThreeCenters(CenterInd.B_R.ordinal(),
                    CenterInd.B_D.ordinal(),
                    CenterInd.B_L.ordinal());

                cycleThreeCenters(CenterInd.U_BR.ordinal(),
                    CenterInd.BR_BL.ordinal(),
                    CenterInd.BL_U.ordinal());

                cycleThreeCenters(CenterInd.U_BL.ordinal(),
                    CenterInd.BR_U.ordinal(),
                    CenterInd.BL_BR.ordinal());
                break;
            case BRP:
                cycleCorners(Corner.U_R.ordinal(),
                    Corner.D_R.ordinal(),
                    Corner.D_B.ordinal());

                twistCorner(Corner.U_R.ordinal(), 1);
                twistCorner(Corner.D_B.ordinal(), 2);
                twistCorner(Corner.D_R.ordinal(), 1);

                cycleEdges(Edge.R_BR.ordinal(),
                    Edge.D_BR.ordinal(),
                    Edge.B_BR.ordinal());

                cycleThreeCenters(CenterInd.BR_U.ordinal(),
                    CenterInd.BR_F.ordinal(),
                    CenterInd.BR_BL.ordinal());

                cycleThreeCenters(CenterInd.R_B.ordinal(),
                    CenterInd.D_R.ordinal(),
                    CenterInd.B_D.ordinal());

                cycleThreeCenters(CenterInd.R_D.ordinal(),
                    CenterInd.D_B.ordinal(),
                    CenterInd.B_R.ordinal());
                break;
            case BLP:
                cycleCorners(Corner.U_L.ordinal(),
                    Corner.D_B.ordinal(),
                    Corner.D_L.ordinal());

                twistCorner(Corner.U_L.ordinal(), 1);
                twistCorner(Corner.D_L.ordinal(), 2);
                twistCorner(Corner.D_B.ordinal(), 1);

                cycleEdges(Edge.L_BL.ordinal(),
                    Edge.B_BL.ordinal(),
                    Edge.D_BL.ordinal());

                cycleThreeCenters(CenterInd.BL_U.ordinal(),
                    CenterInd.BL_BR.ordinal(),
                    CenterInd.BL_F.ordinal());

                cycleThreeCenters(CenterInd.B_L.ordinal(),
                    CenterInd.D_B.ordinal(),
                    CenterInd.L_D.ordinal());

                cycleThreeCenters(CenterInd.L_B.ordinal(),
                    CenterInd.B_D.ordinal(),
                    CenterInd.D_L.ordinal());
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
