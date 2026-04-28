package cs.fto3phase;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class FullFto {

    //--------------- State ---------------//

    //Corner has permutation and orientation encoded within int
    //Use methods encodeCorner, getCornerIndex, and getCornerOrientation
    private int[] corners = new int[6];
    //Only permutation (FTO edges cannot be flipped)
    private int[] edges = new int[12];
    //Only permutation
    private int[] centers = new int[24];
    //Used for pruning table generation
    private int[] centerIndices = new int[24];

    Stack<Move> moveStack;

    public enum Move{R, L, U, D, F, B, BR, BL, RP, LP, UP, DP, FP, BP, BRP, BLP}

    //--------------- Nitty-Gritty stuff ---------------//

    enum Corner {
        U_L, U_R, U_F, D_L, D_R, D_B
    }

    enum Edge {
        U_B, U_R, U_L, //U face edges
        F_L, F_R, R_BR, B_BL, B_BR, L_BL, //Middle slice edges,
        D_F, D_BR, D_BL //D face edges
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

    static int encodeCorner(int perm, int orientation){
        return ((perm << 2) | orientation);
    }

    static int getCornerIndex(int corner){
        return corner>>2;
    }

    static int getCornerOrientation(int corner){
        return corner&0b11;
    }

    /**
     * Cycles corners without impacting orientation
     * @param i1 first index
     * @param i2 second index
     * @param i3 third index
     */
    private void cycleCorners(int i1, int i2, int i3){
        int tmp = corners[i3];
        corners[i3] = corners[i2];
        corners[i2] = corners[i1];
        corners[i1] = tmp;
    }

    private void swapCorners(int i1, int i2){
        int tmp = corners[i2];
        corners[i2] = corners[i1];
        corners[i1] = tmp;
    }

    private void swapEdges(int i1, int i2){
        int tmp = edges[i2];
        edges[i2] = edges[i1];
        edges[i1] = tmp;
    }

    private void swapCenters(int i1, int i2){
        int tmp = centers[i2];
        centers[i2] = centers[i1];
        centers[i1] = tmp;
    }

    private void cycleEdges(int i1, int i2, int i3){
        int tmp = edges[i3];
        edges[i3] = edges[i2];
        edges[i2] = edges[i1];
        edges[i1] = tmp;
    }

    private void cycleThreeCenters(int i1, int i2, int i3){
        int tmp = centers[i3];
        centers[i3] = centers[i2];
        centers[i2] = centers[i1];
        centers[i1] = tmp;

        tmp = centerIndices[i3];
        centerIndices[i3] = centerIndices[i2];
        centerIndices[i2] = centerIndices[i1];
        centerIndices[i1] = tmp;
    }

    /**
     * Twist corner
     * @param i index to twist corner at
     * @param dir direction (1=clockwise, 2=180 degrees, 3=counterClockwise)
     */
    private void twistCorner(int i, int dir){
        corners[i] = encodeCorner(getCornerIndex(corners[i]), (getCornerOrientation(corners[i])+dir)%4);
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
    private int triplePairsOnCorner(int cornerLocation){
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
     * @param face
     * @return t/f
     */
    boolean isFaceSolved(CenterOrd face){

        return areCentersSolvedOnFace(face) &&
            (areEdgesSolvedOnFace(face, 0) ||
            areEdgesSolvedOnFace(face, 1) ||
            areEdgesSolvedOnFace(face, 2));
    }

    //--------------- Hash Functions ---------------//

    private static long[][] PHASE2_CENTER_KEYS = new long[8][24];
    private static long[][][] PHASE2_EDGE_KEYS = new long[12][3][12];

    //Initialize keys for hash functions
    static {
        // This random is not used for generating random states.
        // If you're looking at it suspiciously I know what you're thinking.
        // Don't worry about it
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
    }


    private long centerHash(CenterOrd center){
        long hash = 0;

        for (int i = 0; i < 24; i++) {
            if (centers[i] == center.ordinal()){
                hash ^= PHASE2_CENTER_KEYS[center.ordinal()][i];
            }
        }

        return hash;
    }

    private static boolean contains(int[] arr, int target) {
        for (int num : arr) {
            if (num == target) return true;
        }
        return false;
    }

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

    public int phaseTwoEdgeIndex() {
        int index = 0;
        for (int i = 0; i < 8; i++) {
            int smaller = 0;
            for (int j = i + 1; j < 9; j++) {
                if (edges[j] < edges[i]) {
                    smaller++;
                }
            }
            index += smaller * FACTORIAL[8 - i];
        }
        return index / 2;
    }

    public long phaseOneHash(){
        return edgeHash(CenterOrd.D) ^ centerHash(CenterOrd.D);
    }

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

        throw new RuntimeException("Invalid Phase: " + Integer.toString(phaseId));
    }

    public long phaseThreeHash(){
        return 0;
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
        this.moveStack = new Stack<>();
        this.moveStack.addAll(other.moveStack);
    }

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

    public String history(){
        StringBuilder builder = new StringBuilder();

        for (Move move : moveStack){
            builder.append(move.toString().replace("P", "'") + " ");
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

    public void scrambleRandomG2State(Random r){
        scrambleRandomG2State(r, 500);
    }

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
    public void scrambleRandomState(Random r){

        for (int i = 0; i < 20; i++) {
            Move move = Move.values()[r.nextInt(16)];
            turn(move);
            System.out.print(move);
            System.out.print(" ");
        }
        System.out.println();
        clearMoveStack();

        return;
//
//        //Randomize corner permutation
//        //Swap each corner with a random corner (can be itself)
//        for (int i = 0; i < 6; i++) {
//            swapCorners(i, r.nextInt(6));
//        }
//
//        //Randomize corner orientation
//        int coParity = 0;
//        for (int i = 0; i < 5; i++) {
//            int twist = r.nextInt(4);
//            twistCorner(i, twist);
//            coParity += twist;
//        }
//        //Account for unsolvable states
//        twistCorner(5, coParity % 4);
//
//        //Randomize edge permutation
//        //Swap each corner with a random corner (can be itself)
//        for (int i = 0; i < 12; i++) {
//            swapEdges(i, r.nextInt(12));
//        }
//
//        //Randomize center permutation
//        //Swap each corner with a random corner (can be itself)
//        for (int i = 0; i < 12; i++) {
//            swapEdges(i, r.nextInt(12));
//        }
//
//        for (int i = 0; i < 12; i++) {
//            swapEdges(i, r.nextInt(12));
//            swapCenters(i+12, (r.nextInt(12))+12);
//        }
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
     * Detects redundent move sequences like R R, R R', R BL R
     * @param move
     * @return
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
            if (PARALLEL_MOVES[lastLastmove.ordinal()][0] == move || PARALLEL_MOVES[lastLastmove.ordinal()][1] == move){
                return true;
            }
        }

        return false;
    }


    /**
     * Turn the FTO!
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

                cycleThreeCenters(CenterInd.U_F.ordinal(),
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

                cycleThreeCenters(CenterInd.U_F.ordinal(),
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

    //--------------- Main method for development ---------------//
    //TODO remove before PR
    public static void main(String[] args) {
        FullFto fto = new FullFto();


        fto.parseAlg("F D D L B BL BR U' BR U' D B B' F L' D' L' F B' R'");
        fto.parseAlg("F BL' U' D' F BL'");
        System.out.println(fto.centers[CenterInd.D_R.ordinal()]);
        System.out.println(fto.centers[CenterInd.D_L.ordinal()]);
        System.out.println(fto.centers[CenterInd.D_B.ordinal()]);

        System.out.println(fto.isPhaseOne());
//        long hash = fto.packPhaseTwoTripleData();

//        fto.parseAlg("R D B L");

//        System.out.println(fto.checkPhaseTwoTripleData(hash));

    }

}
