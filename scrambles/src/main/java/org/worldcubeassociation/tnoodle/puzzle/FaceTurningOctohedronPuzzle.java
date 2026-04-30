package org.worldcubeassociation.tnoodle.puzzle;

import cs.fto3phase.FtoSearch;
import cs.fto3phase.FullFto;
import cs.sq12phase.FullCube;
import cs.sq12phase.Search;
import org.worldcubeassociation.tnoodle.scrambles.*;
import org.worldcubeassociation.tnoodle.svglite.Color;
import org.worldcubeassociation.tnoodle.svglite.Dimension;
import org.worldcubeassociation.tnoodle.svglite.Svg;
import org.worldcubeassociation.tnoodle.svglite.Transform;
import org.worldcubeassociation.tnoodle.svglite.Path;
import org.worldcubeassociation.tnoodle.svglite.Rectangle;

import java.util.*;

import org.timepedia.exporter.client.Export;

@Export
public class FaceTurningOctohedronPuzzle extends Puzzle {

    private enum Move {R, L, U, D, F, B, BR, BL, RP, LP, UP, DP, FP, BP, BRP, BLP}
    private final String[] MOVE_NAMES = {"R", "L", "U", "D", "F", "B", "BR", "BL", "R'", "L'", "U'", "D'", "F'", "B'", "BR'", "BL'"};

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

//    private static final int radius = 32;

    private final ThreadLocal<FtoSearch> threePhaseSearcher;

    public FaceTurningOctohedronPuzzle() {
        wcaMinScrambleDistance = 2;
        threePhaseSearcher = ThreadLocal.withInitial(FtoSearch::new);
    }

    private static final Map<String, Color> defaultColorScheme = new HashMap<>();

    static {
        defaultColorScheme.put("B", Color.BLUE);
        defaultColorScheme.put("D", Color.YELLOW);
        defaultColorScheme.put("F", Color.GREEN);
        defaultColorScheme.put("L", new Color(124, 2, 158)); // Purple #7c029e
        defaultColorScheme.put("R", Color.RED);
        defaultColorScheme.put("U", Color.WHITE);
        defaultColorScheme.put("BL", Color.GRAY);
        defaultColorScheme.put("BR", new Color(255, 128, 0)); // Orange #FF8000
    }

    @Override
    public Map<String, Color> getDefaultColorScheme() {
        return new HashMap<>(defaultColorScheme);
    }


    //TODO
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, 0);
    }

    private static Dimension getImageSize(int radius) {
        return new Dimension(getWidth(radius), getHeight(radius));
    }

    //    private static final double RADIUS_MULTIPLIER = Math.sqrt(2) * Math.cos(Math.toRadians(15));
//    private static final double multiplier = 1.4;
    private static int getWidth(int radius) {
//        return (int) (2 * RADIUS_MULTIPLIER * multiplier * radius);
        return 0;
    }

    private static int getHeight(int radius) {
//        return (int) (4 * RADIUS_MULTIPLIER * multiplier * radius);
        return 0;
    }

    @Override
    public String getLongName() {
        return "Face-Turning Octahedron";
    }

    @Override
    public String getShortName() {
        return "fto";
    }

    @Override
    public PuzzleState getSolvedState() {
        return new FaceTurningOctahedronState();
    }

    //TODO
    @Override
    protected int getRandomMoveCount() {
        return 0;
    }

    @Override
    public PuzzleStateAndGenerator generateRandomMoves(Random r) {
        FullFto randomState = FullFto.randomCube(r);

        String scramble = threePhaseSearcher.get().solution(randomState).trim();
        PuzzleState state;
        try {
            state = getSolvedState().applyAlgorithm(scramble);
        } catch (InvalidScrambleException e) {
            throw new RuntimeException(e);
        }
        return new PuzzleStateAndGenerator(state, scramble);
    }


    public class FaceTurningOctahedronState extends PuzzleState {

        private int[] corners = new int[6];
        private int[] edges = new int[12];
        private int[] centers = new int[24];

        private int encodeCorner(int perm, int orientation) {
            return ((perm << 2) | orientation);
        }

        private int getCornerIndex(int corner) {
            return corner >> 2;
        }

        private int getCornerOrientation(int corner) {
            return corner & 0b11;
        }

        public FaceTurningOctahedronState() {
            for (int i = 0; i < 6; i++) {
                corners[i] = encodeCorner(i, 0);
            }
            for (int i = 0; i < 12; i++) {
                edges[i] = i;
            }

            for (int i = 0; i < 24 / 3; i++) {
                for (int j = 0; j < 3; j++) {
                    centers[(i * 3) + j] = i;
                }
            }
        }

        public FaceTurningOctahedronState(FaceTurningOctahedronState copyfrom) {
            System.arraycopy(copyfrom.corners, 0, corners, 0, 6);
            System.arraycopy(copyfrom.edges, 0, edges, 0, 12);
            System.arraycopy(copyfrom.centers, 0, centers, 0, 24);
        }

        //TODO
        @Override
        public Map<String, FaceTurningOctahedronState> getScrambleSuccessors() {
            return null;
        }

        //TODO
        @Override
        public Map<String, FaceTurningOctahedronState> getSuccessorsByName() {
            Map<String, FaceTurningOctahedronState> successors = new LinkedHashMap<>();
            for (Move move : Move.values()){
                String key = MOVE_NAMES[move.ordinal()];
                FaceTurningOctahedronState successor = new FaceTurningOctahedronState(this);
                successor.turn(move);
                successors.put(key, successor);
            }
            return successors;
        }

        //TODO
        @Override
        public String solveIn(int n) {
            return null;
        }

        //TODO
        @Override
        public boolean equals(Object other) {
            FaceTurningOctahedronState o = ((FaceTurningOctahedronState) other);
            return Arrays.equals(centers, o.centers) &&
                Arrays.equals(corners, o.corners) &&
                Arrays.equals(edges, o.edges);
        }

        //TODO
        @Override
        public int hashCode() {
            return Arrays.hashCode(corners) ^ Arrays.hashCode(edges) ^ Arrays.hashCode(centers);
        }

        //TODO
        @Override
        protected Svg drawScramble(Map<String, Color> colorSchemeMap) {
            return null;
        }

        //TODO
        public String toString() {
            return null;
        }

        private void cycleCorners(int i1, int i2, int i3) {
            int tmp = corners[i3];
            corners[i3] = corners[i2];
            corners[i2] = corners[i1];
            corners[i1] = tmp;
        }

        /**
         * Cycles edges without impacting orientation
         *
         * @param i1 first index
         * @param i2 second index
         * @param i3 third index
         */
        private void cycleEdges(int i1, int i2, int i3) {
            int tmp = edges[i3];
            edges[i3] = edges[i2];
            edges[i2] = edges[i1];
            edges[i1] = tmp;
        }

        /**
         * Cycles centers
         *
         * @param i1 first index
         * @param i2 second index
         * @param i3 third index
         */
        private void cycleThreeCenters(int i1, int i2, int i3) {
            int tmp = centers[i3];
            centers[i3] = centers[i2];
            centers[i2] = centers[i1];
            centers[i1] = tmp;
        }

        private void swapCorners(int i1, int i2) {
            int tmp = corners[i2];
            corners[i2] = corners[i1];
            corners[i1] = tmp;
        }

        private void swapEdges(int i1, int i2) {
            int tmp = edges[i2];
            edges[i2] = edges[i1];
            edges[i1] = tmp;
        }

        private void swapCenters(int i1, int i2) {
            int tmp = centers[i2];
            centers[i2] = centers[i1];
            centers[i1] = tmp;
        }

        private void twistCorner(int i, int dir) {
            corners[i] = encodeCorner(getCornerIndex(corners[i]), (getCornerOrientation(corners[i]) + dir) % 4);
        }


        public void turn(Move move) {
            switch (move) {
                case R:
                    cycleCorners(Corner.U_F.ordinal(), Corner.U_R.ordinal(), Corner.D_R.ordinal());
                    twistCorner(Corner.U_F.ordinal(), 3);
                    twistCorner(Corner.U_R.ordinal(), 2);
                    twistCorner(Corner.D_R.ordinal(), 3);
                    cycleEdges(Edge.U_R.ordinal(), Edge.R_BR.ordinal(), Edge.F_R.ordinal());
                    cycleThreeCenters(CenterInd.R_L.ordinal(), CenterInd.R_B.ordinal(), CenterInd.R_D.ordinal());
                    cycleThreeCenters(CenterInd.F_U.ordinal(), CenterInd.U_BR.ordinal(), CenterInd.BR_F.ordinal());
                    cycleThreeCenters(CenterInd.F_BR.ordinal(), CenterInd.U_F.ordinal(), CenterInd.BR_U.ordinal());
                    break;
                case L:
                    cycleCorners(Corner.U_L.ordinal(), Corner.U_F.ordinal(), Corner.D_L.ordinal());
                    twistCorner(Corner.U_L.ordinal(), 3);
                    twistCorner(Corner.U_F.ordinal(), 2);
                    twistCorner(Corner.D_L.ordinal(), 3);
                    cycleEdges(Edge.U_L.ordinal(), Edge.F_L.ordinal(), Edge.L_BL.ordinal());
                    cycleThreeCenters(CenterInd.L_B.ordinal(), CenterInd.L_R.ordinal(), CenterInd.L_D.ordinal());
                    cycleThreeCenters(CenterInd.U_BL.ordinal(), CenterInd.F_U.ordinal(), CenterInd.BL_F.ordinal());
                    cycleThreeCenters(CenterInd.U_F.ordinal(), CenterInd.F_BL.ordinal(), CenterInd.BL_U.ordinal());
                    break;
                case U:
                    cycleCorners(Corner.U_L.ordinal(), Corner.U_R.ordinal(), Corner.U_F.ordinal());
                    cycleEdges(Edge.U_B.ordinal(), Edge.U_R.ordinal(), Edge.U_L.ordinal());
                    cycleThreeCenters(CenterInd.U_BL.ordinal(), CenterInd.U_BR.ordinal(), CenterInd.U_F.ordinal());
                    cycleThreeCenters(CenterInd.R_L.ordinal(), CenterInd.L_B.ordinal(), CenterInd.B_R.ordinal());
                    cycleThreeCenters(CenterInd.R_B.ordinal(), CenterInd.L_R.ordinal(), CenterInd.B_L.ordinal());
                    break;
                case D:
                    cycleCorners(Corner.D_L.ordinal(), Corner.D_R.ordinal(), Corner.D_B.ordinal());
                    cycleEdges(Edge.D_F.ordinal(), Edge.D_BR.ordinal(), Edge.D_BL.ordinal());
                    cycleThreeCenters(CenterInd.D_L.ordinal(), CenterInd.D_R.ordinal(), CenterInd.D_B.ordinal());
                    cycleThreeCenters(CenterInd.F_BL.ordinal(), CenterInd.BR_F.ordinal(), CenterInd.BL_BR.ordinal());
                    cycleThreeCenters(CenterInd.F_BR.ordinal(), CenterInd.BR_BL.ordinal(), CenterInd.BL_F.ordinal());
                    break;
                case F:
                    cycleCorners(Corner.U_F.ordinal(), Corner.D_R.ordinal(), Corner.D_L.ordinal());
                    twistCorner(Corner.U_F.ordinal(), 3);
                    twistCorner(Corner.D_R.ordinal(), 3);
                    twistCorner(Corner.D_L.ordinal(), 2);
                    cycleEdges(Edge.F_R.ordinal(), Edge.D_F.ordinal(), Edge.F_L.ordinal());
                    cycleThreeCenters(CenterInd.F_U.ordinal(), CenterInd.F_BR.ordinal(), CenterInd.F_BL.ordinal());
                    cycleThreeCenters(CenterInd.L_R.ordinal(), CenterInd.R_D.ordinal(), CenterInd.D_L.ordinal());
                    cycleThreeCenters(CenterInd.R_L.ordinal(), CenterInd.D_R.ordinal(), CenterInd.L_D.ordinal());
                    break;
                case B:
                    cycleCorners(Corner.U_R.ordinal(), Corner.U_L.ordinal(), Corner.D_B.ordinal());
                    twistCorner(Corner.U_R.ordinal(), 3);
                    twistCorner(Corner.U_L.ordinal(), 2);
                    twistCorner(Corner.D_B.ordinal(), 3);
                    cycleEdges(Edge.U_B.ordinal(), Edge.B_BL.ordinal(), Edge.B_BR.ordinal());
                    cycleThreeCenters(CenterInd.B_R.ordinal(), CenterInd.B_L.ordinal(), CenterInd.B_D.ordinal());
                    cycleThreeCenters(CenterInd.U_BR.ordinal(), CenterInd.BL_U.ordinal(), CenterInd.BR_BL.ordinal());
                    cycleThreeCenters(CenterInd.U_BL.ordinal(), CenterInd.BL_BR.ordinal(), CenterInd.BR_U.ordinal());
                    break;
                case BR:
                    cycleCorners(Corner.U_R.ordinal(), Corner.D_B.ordinal(), Corner.D_R.ordinal());
                    twistCorner(Corner.U_R.ordinal(), 3);
                    twistCorner(Corner.D_B.ordinal(), 3);
                    twistCorner(Corner.D_R.ordinal(), 2);
                    cycleEdges(Edge.R_BR.ordinal(), Edge.B_BR.ordinal(), Edge.D_BR.ordinal());
                    cycleThreeCenters(CenterInd.BR_U.ordinal(), CenterInd.BR_BL.ordinal(), CenterInd.BR_F.ordinal());
                    cycleThreeCenters(CenterInd.R_B.ordinal(), CenterInd.B_D.ordinal(), CenterInd.D_R.ordinal());
                    cycleThreeCenters(CenterInd.R_D.ordinal(), CenterInd.B_R.ordinal(), CenterInd.D_B.ordinal());
                    break;
                case BL:
                    cycleCorners(Corner.U_L.ordinal(), Corner.D_L.ordinal(), Corner.D_B.ordinal());
                    twistCorner(Corner.U_L.ordinal(), 3);
                    twistCorner(Corner.D_L.ordinal(), 3);
                    twistCorner(Corner.D_B.ordinal(), 2);
                    cycleEdges(Edge.L_BL.ordinal(), Edge.D_BL.ordinal(), Edge.B_BL.ordinal());
                    cycleThreeCenters(CenterInd.BL_U.ordinal(), CenterInd.BL_F.ordinal(), CenterInd.BL_BR.ordinal());
                    cycleThreeCenters(CenterInd.B_L.ordinal(), CenterInd.L_D.ordinal(), CenterInd.D_B.ordinal());
                    cycleThreeCenters(CenterInd.L_B.ordinal(), CenterInd.D_L.ordinal(), CenterInd.B_D.ordinal());
                    break;
                case RP:
                    cycleCorners(Corner.U_F.ordinal(), Corner.D_R.ordinal(), Corner.U_R.ordinal());
                    twistCorner(Corner.U_F.ordinal(), 2);
                    twistCorner(Corner.U_R.ordinal(), 1);
                    twistCorner(Corner.D_R.ordinal(), 1);
                    cycleEdges(Edge.U_R.ordinal(), Edge.F_R.ordinal(), Edge.R_BR.ordinal());
                    cycleThreeCenters(CenterInd.R_L.ordinal(), CenterInd.R_D.ordinal(), CenterInd.R_B.ordinal());
                    cycleThreeCenters(CenterInd.F_U.ordinal(), CenterInd.BR_F.ordinal(), CenterInd.U_BR.ordinal());
                    cycleThreeCenters(CenterInd.F_BR.ordinal(), CenterInd.BR_U.ordinal(), CenterInd.U_F.ordinal());
                    break;
                case LP:
                    cycleCorners(Corner.U_L.ordinal(), Corner.D_L.ordinal(), Corner.U_F.ordinal());
                    twistCorner(Corner.U_L.ordinal(), 2);
                    twistCorner(Corner.U_F.ordinal(), 1);
                    twistCorner(Corner.D_L.ordinal(), 1);
                    cycleEdges(Edge.U_L.ordinal(), Edge.L_BL.ordinal(), Edge.F_L.ordinal());
                    cycleThreeCenters(CenterInd.L_B.ordinal(), CenterInd.L_D.ordinal(), CenterInd.L_R.ordinal());
                    cycleThreeCenters(CenterInd.U_BL.ordinal(), CenterInd.BL_F.ordinal(), CenterInd.F_U.ordinal());
                    cycleThreeCenters(CenterInd.U_F.ordinal(), CenterInd.BL_U.ordinal(), CenterInd.F_BL.ordinal());
                    break;
                case UP:
                    cycleCorners(Corner.U_L.ordinal(), Corner.U_F.ordinal(), Corner.U_R.ordinal());
                    cycleEdges(Edge.U_B.ordinal(), Edge.U_L.ordinal(), Edge.U_R.ordinal());
                    cycleThreeCenters(CenterInd.U_BL.ordinal(), CenterInd.U_F.ordinal(), CenterInd.U_BR.ordinal());
                    cycleThreeCenters(CenterInd.R_L.ordinal(), CenterInd.B_R.ordinal(), CenterInd.L_B.ordinal());
                    cycleThreeCenters(CenterInd.R_B.ordinal(), CenterInd.B_L.ordinal(), CenterInd.L_R.ordinal());
                    break;
                case DP:
                    cycleCorners(Corner.D_L.ordinal(), Corner.D_B.ordinal(), Corner.D_R.ordinal());
                    cycleEdges(Edge.D_F.ordinal(), Edge.D_BL.ordinal(), Edge.D_BR.ordinal());
                    cycleThreeCenters(CenterInd.D_L.ordinal(), CenterInd.D_B.ordinal(), CenterInd.D_R.ordinal());
                    cycleThreeCenters(CenterInd.F_BL.ordinal(), CenterInd.BL_BR.ordinal(), CenterInd.BR_F.ordinal());
                    cycleThreeCenters(CenterInd.F_BR.ordinal(), CenterInd.BL_F.ordinal(), CenterInd.BR_BL.ordinal());
                    break;
                case FP:
                    cycleCorners(Corner.U_F.ordinal(), Corner.D_L.ordinal(), Corner.D_R.ordinal());
                    twistCorner(Corner.U_F.ordinal(), 1);
                    twistCorner(Corner.D_R.ordinal(), 2);
                    twistCorner(Corner.D_L.ordinal(), 1);
                    cycleEdges(Edge.F_R.ordinal(), Edge.F_L.ordinal(), Edge.D_F.ordinal());
                    cycleThreeCenters(CenterInd.F_U.ordinal(), CenterInd.F_BL.ordinal(), CenterInd.F_BR.ordinal());
                    cycleThreeCenters(CenterInd.L_R.ordinal(), CenterInd.D_L.ordinal(), CenterInd.R_D.ordinal());
                    cycleThreeCenters(CenterInd.R_L.ordinal(), CenterInd.L_D.ordinal(), CenterInd.D_R.ordinal());
                    break;
                case BP:
                    cycleCorners(Corner.U_R.ordinal(), Corner.D_B.ordinal(), Corner.U_L.ordinal());
                    twistCorner(Corner.U_R.ordinal(), 2);
                    twistCorner(Corner.U_L.ordinal(), 1);
                    twistCorner(Corner.D_B.ordinal(), 1);
                    cycleEdges(Edge.U_B.ordinal(), Edge.B_BR.ordinal(), Edge.B_BL.ordinal());
                    cycleThreeCenters(CenterInd.B_R.ordinal(), CenterInd.B_D.ordinal(), CenterInd.B_L.ordinal());
                    cycleThreeCenters(CenterInd.U_BR.ordinal(), CenterInd.BR_BL.ordinal(), CenterInd.BL_U.ordinal());
                    cycleThreeCenters(CenterInd.U_BL.ordinal(), CenterInd.BR_U.ordinal(), CenterInd.BL_BR.ordinal());
                    break;
                case BRP:
                    cycleCorners(Corner.U_R.ordinal(), Corner.D_R.ordinal(), Corner.D_B.ordinal());
                    twistCorner(Corner.U_R.ordinal(), 1);
                    twistCorner(Corner.D_B.ordinal(), 2);
                    twistCorner(Corner.D_R.ordinal(), 1);
                    cycleEdges(Edge.R_BR.ordinal(), Edge.D_BR.ordinal(), Edge.B_BR.ordinal());
                    cycleThreeCenters(CenterInd.BR_U.ordinal(), CenterInd.BR_F.ordinal(), CenterInd.BR_BL.ordinal());
                    cycleThreeCenters(CenterInd.R_B.ordinal(), CenterInd.D_R.ordinal(), CenterInd.B_D.ordinal());
                    cycleThreeCenters(CenterInd.R_D.ordinal(), CenterInd.D_B.ordinal(), CenterInd.B_R.ordinal());
                    break;
                case BLP:
                    cycleCorners(Corner.U_L.ordinal(), Corner.D_B.ordinal(), Corner.D_L.ordinal());
                    twistCorner(Corner.U_L.ordinal(), 1);
                    twistCorner(Corner.D_L.ordinal(), 2);
                    twistCorner(Corner.D_B.ordinal(), 1);
                    cycleEdges(Edge.L_BL.ordinal(), Edge.B_BL.ordinal(), Edge.D_BL.ordinal());
                    cycleThreeCenters(CenterInd.BL_U.ordinal(), CenterInd.BL_BR.ordinal(), CenterInd.BL_F.ordinal());
                    cycleThreeCenters(CenterInd.B_L.ordinal(), CenterInd.D_B.ordinal(), CenterInd.L_D.ordinal());
                    cycleThreeCenters(CenterInd.L_B.ordinal(), CenterInd.B_D.ordinal(), CenterInd.D_L.ordinal());
                    break;
            }
        }

    }
}
