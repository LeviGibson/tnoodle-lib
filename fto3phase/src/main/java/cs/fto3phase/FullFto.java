package cs.fto3phase;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

/**
 * Three-phase solver for the FTO puzzle.
 */
public class FullFto {

    //--------------- State ---------------//

    //Corner has permutation and orientation encoded within int
    //Use methods encodeCorner, getCornerIndex, and getCornerOrientation
    private int[] corners = new int[6];
    //Only permutation (FTO edges cannot be flipped)
    private int[] edges = new int[12];
    //Only permutation
    private int[] centers = new int[24];

    private Stack<Move> moveStack;

    public void ughhhhh() {
        for (Move m : moveStack){
            System.out.println(m);
        }
    }


    public enum Move{R, L, U, D, F, B, BR, BL, RP, LP, UP, DP, FP, BP, BRP, BLP}


    //--------------- Nitty-Gritty stuff ---------------//

    private enum Corners{
        U_L, U_R, U_F, D_L, D_R, D_B
    }

    private enum Edges{
        U_B, U_R, U_L, //U face edges
        D_F, D_BR, D_BL, //D face edges
        F_L, F_R, R_BR, B_BL, B_BR, L_BL //Middle slice edges
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

    private static int encodeCorner(int perm, int orientation){
        return ((perm << 2) | orientation);
    }

    private static int getCornerIndex(int corner){
        return corner>>2;
    }

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

    private static final Move[] INVERT_MOVE = {Move.RP, Move.LP, Move.UP, Move.DP, Move.FP, Move.BP, Move.BRP, Move.BLP,
        Move.R, Move.L, Move.U, Move.D, Move.F, Move.B, Move.BR, Move.BL};

    /**
     * Used for detecting repetitions
     */
    private static final Move[][] PARALLEL_MOVES = {
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
        return (edges[Edges.D_F.ordinal()] == Edges.D_F.ordinal() &&
            edges[Edges.D_BR.ordinal()] == Edges.D_BR.ordinal() &&
            edges[Edges.D_BL.ordinal()] == Edges.D_BL.ordinal()) ||
                (edges[Edges.D_F.ordinal()] == Edges.D_BL.ordinal() &&
                    edges[Edges.D_BR.ordinal()] == Edges.D_F.ordinal() &&
                    edges[Edges.D_BL.ordinal()] == Edges.D_BR.ordinal()) ||
                (edges[Edges.D_F.ordinal()] == Edges.D_BR.ordinal() &&
                    edges[Edges.D_BR.ordinal()] == Edges.D_BL.ordinal() &&
                    edges[Edges.D_BL.ordinal()] == Edges.D_F.ordinal());
    }

    /**
     * Phase two is Octominx reduction
     * Can be solved with moveset [D B R L]
     * @return t/f
     */
    public boolean isPhaseTwo(){
        for (int i = 0; i < 6; i++) {
            if (!isTriple(i))
                return false;
        }

        return true;
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

    /**
     * Turns self into a random state
     * @param r secure random
     */
    public void scrambleRandomState(Random r){
        //Randomize corner permutation
        //Swap each corner with a random corner (can be itself)
        for (int i = 0; i < 6; i++) {
            swapCorners(i, r.nextInt() % 6);
        }

        //Randomize corner orientation
        int coParity = 0;
        for (int i = 0; i < 5; i++) {
            int twist = r.nextInt()%4;
            twistCorner(i, twist);
            coParity += twist;
        }
        //Account for unsolvable states
        twistCorner(5, coParity % 4);

        //Randomize edge permutation
        //Swap each corner with a random corner (can be itself)
        for (int i = 0; i < 12; i++) {
            swapEdges(i, r.nextInt() % 12);
        }

        //Randomize center permutation
        //Swap each corner with a random corner (can be itself)
        for (int i = 0; i < 12; i++) {
            swapEdges(i, r.nextInt() % 12);
        }

        for (int i = 0; i < 12; i++) {
            swapEdges(i, r.nextInt() % 12);
            swapCenters(i+12, (r.nextInt() % 12)+12);
        }
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
    public boolean isRepetition(Move move){

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
                cycleCorners(Corners.U_F.ordinal(),
                    Corners.U_R.ordinal(),
                    Corners.D_R.ordinal());

                twistCorner(Corners.U_F.ordinal(), 3);
                twistCorner(Corners.U_R.ordinal(), 2);
                twistCorner(Corners.D_R.ordinal(), 3);

                cycleEdges(Edges.U_R.ordinal(),
                    Edges.R_BR.ordinal(),
                    Edges.F_R.ordinal());

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
                cycleCorners(Corners.U_L.ordinal(),
                    Corners.U_F.ordinal(),
                    Corners.D_L.ordinal());

                twistCorner(Corners.U_L.ordinal(), 3);
                twistCorner(Corners.U_F.ordinal(), 2);
                twistCorner(Corners.D_L.ordinal(), 3);

                cycleEdges(Edges.U_L.ordinal(),
                    Edges.F_L.ordinal(),
                    Edges.L_BL.ordinal());

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
                cycleCorners(Corners.U_L.ordinal(),
                    Corners.U_R.ordinal(),
                    Corners.U_F.ordinal());

                cycleEdges(Edges.U_B.ordinal(),
                    Edges.U_R.ordinal(),
                    Edges.U_L.ordinal());

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
                cycleCorners(Corners.D_L.ordinal(),
                    Corners.D_R.ordinal(),
                    Corners.D_B.ordinal());

                cycleEdges(Edges.D_F.ordinal(),
                    Edges.D_BR.ordinal(),
                    Edges.D_BL.ordinal());

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
                cycleCorners(Corners.U_F.ordinal(),
                    Corners.D_R.ordinal(),
                    Corners.D_L.ordinal());

                twistCorner(Corners.U_F.ordinal(), 3);
                twistCorner(Corners.D_R.ordinal(), 3);
                twistCorner(Corners.D_L.ordinal(), 2);

                cycleEdges(Edges.F_R.ordinal(),
                    Edges.D_F.ordinal(),
                    Edges.F_L.ordinal());

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
                cycleCorners(Corners.U_R.ordinal(),
                    Corners.U_L.ordinal(),
                    Corners.D_B.ordinal());

                twistCorner(Corners.U_R.ordinal(), 3);
                twistCorner(Corners.U_L.ordinal(), 2);
                twistCorner(Corners.D_B.ordinal(), 3);

                cycleEdges(Edges.U_B.ordinal(),
                    Edges.B_BL.ordinal(),
                    Edges.B_BR.ordinal());

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
                cycleCorners(Corners.U_R.ordinal(),
                    Corners.D_B.ordinal(),
                    Corners.D_R.ordinal());

                twistCorner(Corners.U_R.ordinal(), 3);
                twistCorner(Corners.D_B.ordinal(), 3);
                twistCorner(Corners.D_R.ordinal(), 2);

                cycleEdges(Edges.R_BR.ordinal(),
                    Edges.B_BR.ordinal(),
                    Edges.D_BR.ordinal());

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
                cycleCorners(Corners.U_L.ordinal(),
                    Corners.D_L.ordinal(),
                    Corners.D_B.ordinal());

                twistCorner(Corners.U_L.ordinal(), 3);
                twistCorner(Corners.D_L.ordinal(), 3);
                twistCorner(Corners.D_B.ordinal(), 2);

                cycleEdges(Edges.L_BL.ordinal(),
                    Edges.D_BL.ordinal(),
                    Edges.B_BL.ordinal());

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
                turn(Move.R);
                turn(Move.R);
                moveStack.pop();
                moveStack.pop();
                break;
            case LP:
                turn(Move.L);
                turn(Move.L);
                moveStack.pop();
                moveStack.pop();
                break;
            case UP:
                turn(Move.U);
                turn(Move.U);
                moveStack.pop();
                moveStack.pop();
                break;
            case DP:
                turn(Move.D);
                turn(Move.D);
                moveStack.pop();
                moveStack.pop();
                break;
            case FP:
                turn(Move.F);
                turn(Move.F);
                moveStack.pop();
                moveStack.pop();
                break;
            case BP:
                turn(Move.B);
                turn(Move.B);
                moveStack.pop();
                moveStack.pop();
                break;
            case BRP:
                turn(Move.BR);
                turn(Move.BR);
                moveStack.pop();
                moveStack.pop();
                break;
            case BLP:
                turn(Move.BL);
                turn(Move.BL);
                moveStack.pop();
                moveStack.pop();
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
    public static void main(String[] args){
        FullFto fto = new FullFto();
        fto.parseAlg("U R U");
        System.out.println(fto.tripleCount());

    }

}
