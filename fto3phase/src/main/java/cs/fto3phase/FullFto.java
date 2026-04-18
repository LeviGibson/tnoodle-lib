package cs.fto3phase;

import java.lang.reflect.Array;
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


    //--------------- Nitty-Gritty performance related stuff ---------------//

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

    private enum Corners{
        U_L, U_R, U_F, D_L, D_R, D_B
    }

    private enum Edges{
        U_B, U_R, U_L, //U face edges
        D_F, D_BR, D_BL, //D face edges
        F_L, F_R, R_BL, B_BL, B_BR, L_BL //Middle slice edges
    }

    private enum Centers {
        U_BL, U_BR, U_F,
        L_B, L_R, L_D,
        F_U, F_BR, F_BL,
        R_L, R_B, R_D,
        BR_U, BR_BL, BR_F,
        B_R, B_L, B_D,
        BL_U, BL_F, BL_BR,
        D_L, D_R, D_B
    }


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
                    Edges.R_BL.ordinal(),
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

            case L:
            case U:
            case D:
            case F:
            case B:
            case BR:
            case BL:
        }
    }

    //--------------- Main method for development ---------------//
    //TODO remove before PR
    public static void main(String[] args){
        FullFto fto = new FullFto();
        fto.turn(Move.R);
        fto.turn(Move.R);
        fto.turn(Move.R);
        System.out.println(fto.isSolved());

    }

}
