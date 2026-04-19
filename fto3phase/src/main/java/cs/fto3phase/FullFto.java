package cs.fto3phase;

import java.util.Arrays;

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


    private enum Centers {
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
        for (int i = 0; i < 24; i++) {
            SOLVED_CENTERS[i] = i;
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
        for (int i = 0; i < 24; i++) {
            centers[i] = i;
        }
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
     * Turn the FTO!
     * @param move move
     */
    public void turn(Move move){
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

                cycleThreeCenters(Centers.R_L.ordinal(),
                    Centers.R_B.ordinal(),
                    Centers.R_D.ordinal());

                cycleThreeCenters(Centers.F_U.ordinal(),
                    Centers.U_BR.ordinal(),
                    Centers.BR_F.ordinal());

                cycleThreeCenters(Centers.F_BR.ordinal(),
                    Centers.U_F.ordinal(),
                    Centers.BR_U.ordinal());
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

                cycleThreeCenters(Centers.L_B.ordinal(),
                    Centers.L_R.ordinal(),
                    Centers.L_D.ordinal());

                cycleThreeCenters(Centers.U_BL.ordinal(),
                    Centers.F_U.ordinal(),
                    Centers.BL_F.ordinal());

                cycleThreeCenters(Centers.U_F.ordinal(),
                    Centers.F_BL.ordinal(),
                    Centers.BL_U.ordinal());
                break;
            case U:
                cycleCorners(Corners.U_L.ordinal(),
                    Corners.U_R.ordinal(),
                    Corners.U_F.ordinal());

                cycleEdges(Edges.U_B.ordinal(),
                    Edges.U_R.ordinal(),
                    Edges.U_L.ordinal());

                cycleThreeCenters(Centers.U_BL.ordinal(),
                    Centers.U_BR.ordinal(),
                    Centers.U_F.ordinal());

                cycleThreeCenters(Centers.R_L.ordinal(),
                    Centers.L_B.ordinal(),
                    Centers.B_R.ordinal());

                cycleThreeCenters(Centers.R_B.ordinal(),
                    Centers.L_R.ordinal(),
                    Centers.BR_BL.ordinal());
                break;
            case D:
                cycleCorners(Corners.D_L.ordinal(),
                    Corners.D_R.ordinal(),
                    Corners.D_B.ordinal());

                cycleEdges(Edges.D_F.ordinal(),
                    Edges.D_BR.ordinal(),
                    Edges.D_BL.ordinal());

                cycleThreeCenters(Centers.D_L.ordinal(),
                    Centers.D_R.ordinal(),
                    Centers.D_B.ordinal());

                cycleThreeCenters(Centers.F_BL.ordinal(),
                    Centers.BR_F.ordinal(),
                    Centers.BL_BR.ordinal());

                cycleThreeCenters(Centers.F_BR.ordinal(),
                    Centers.BR_BL.ordinal(),
                    Centers.BL_F.ordinal());
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

                cycleThreeCenters(Centers.F_U.ordinal(),
                    Centers.F_BR.ordinal(),
                    Centers.F_BL.ordinal());

                cycleThreeCenters(Centers.U_F.ordinal(),
                    Centers.R_D.ordinal(),
                    Centers.D_L.ordinal());

                cycleThreeCenters(Centers.R_L.ordinal(),
                    Centers.D_R.ordinal(),
                    Centers.L_D.ordinal());
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

                cycleThreeCenters(Centers.B_R.ordinal(),
                    Centers.B_L.ordinal(),
                    Centers.B_D.ordinal());

                cycleThreeCenters(Centers.U_BR.ordinal(),
                    Centers.BL_U.ordinal(),
                    Centers.BR_BL.ordinal());

                cycleThreeCenters(Centers.U_BL.ordinal(),
                    Centers.BL_BR.ordinal(),
                    Centers.BR_U.ordinal());
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

                cycleThreeCenters(Centers.BR_U.ordinal(),
                    Centers.BR_BL.ordinal(),
                    Centers.BR_F.ordinal());

                cycleThreeCenters(Centers.R_B.ordinal(),
                    Centers.B_D.ordinal(),
                    Centers.D_R.ordinal());

                cycleThreeCenters(Centers.R_D.ordinal(),
                    Centers.B_R.ordinal(),
                    Centers.D_B.ordinal());
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

                cycleThreeCenters(Centers.BL_U.ordinal(),
                    Centers.BL_F.ordinal(),
                    Centers.BL_BR.ordinal());

                cycleThreeCenters(Centers.B_L.ordinal(),
                    Centers.L_D.ordinal(),
                    Centers.D_B.ordinal());

                cycleThreeCenters(Centers.L_B.ordinal(),
                    Centers.D_L.ordinal(),
                    Centers.B_D.ordinal());
                break;
            case RP:
                turn(Move.R);
                turn(Move.R);
                break;
            case LP:
                turn(Move.L);
                turn(Move.L);
                break;
            case UP:
                turn(Move.U);
                turn(Move.U);
                break;
            case DP:
                turn(Move.D);
                turn(Move.D);
                break;
            case FP:
                turn(Move.F);
                turn(Move.F);
                break;
            case BP:
                turn(Move.B);
                turn(Move.B);
                break;
            case BRP:
                turn(Move.BR);
                turn(Move.BR);
                break;
            case BLP:
                turn(Move.BL);
                turn(Move.BL);
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

    }

}
