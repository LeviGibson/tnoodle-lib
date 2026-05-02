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
        defaultColorScheme.put("BL", new Color(255, 128, 0));
        defaultColorScheme.put("BR", Color.GRAY); // Orange #FF8000
    }

    @Override
    public Map<String, Color> getDefaultColorScheme() {
        return new HashMap<>(defaultColorScheme);
    }


    private static final int FACE_TURNING_OCTAHEDRON_UNIT_SIZE = 36;
    private static final int FACE_TURNING_OCTAHEDRON_MARGIN = 4;

    @Override
    public Dimension getPreferredSize() {
        return getImageSize(FACE_TURNING_OCTAHEDRON_UNIT_SIZE);
    }

    private static Dimension getImageSize(int radius) {
        return new Dimension(getWidth(radius), getHeight(radius));
    }

    private static int getWidth(int radius) {
        return 12 * radius + 2 * FACE_TURNING_OCTAHEDRON_MARGIN;
    }

    private static int getHeight(int radius) {
        return 6 * radius + 2 * FACE_TURNING_OCTAHEDRON_MARGIN;
    }

    @Override
    public String getLongName() {
        return "Face Turning Octahedron";
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

        /**
         * Copy constructor
         * @param copyFrom state to copy from
         */
        public FaceTurningOctahedronState(FaceTurningOctahedronState copyFrom) {
            System.arraycopy(copyFrom.corners, 0, corners, 0, 6);
            System.arraycopy(copyFrom.edges, 0, edges, 0, 12);
            System.arraycopy(copyFrom.centers, 0, centers, 0, 24);
        }

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

        @Override
        protected Svg drawScramble(Map<String, Color> colorSchemeMap) {

            Color[] scheme = new Color[8];
            for(int i = 0; i < scheme.length; i++) {
                scheme[i] = colorSchemeMap.get(CenterOrd.values()[i].toString());
            }

            int[][] image = getImage();

            int unit = FACE_TURNING_OCTAHEDRON_UNIT_SIZE;
            int margin = FACE_TURNING_OCTAHEDRON_MARGIN;
            Svg svg = new Svg(getImageSize(unit));

            drawFace(svg, image[4], scheme,
                margin, margin,
                margin, margin + 6 * unit,
                margin + 3 * unit, margin + 3 * unit);
            drawFace(svg, image[0], scheme,
                margin, margin,
                margin + 6 * unit, margin,
                margin + 3 * unit, margin + 3 * unit);
            drawFace(svg, image[1], scheme,
                margin, margin + 6 * unit,
                margin + 6 * unit, margin + 6 * unit,
                margin + 3 * unit, margin + 3 * unit);
            drawFace(svg, image[5], scheme,
                margin + 3 * unit, margin + 3 * unit,
                margin + 6 * unit, margin,
                margin + 6 * unit, margin + 6 * unit);
            drawFace(svg, image[3], scheme,
                margin + 6 * unit, margin,
                margin + 9 * unit, margin + 3 * unit,
                margin + 6 * unit, margin + 6 * unit);
            drawFace(svg, image[6], scheme,
                margin + 6 * unit, margin,
                margin + 12 * unit, margin,
                margin + 9 * unit, margin + 3 * unit);
            drawFace(svg, image[7], scheme,
                margin + 6 * unit, margin + 6 * unit,
                margin + 12 * unit, margin + 6 * unit,
                margin + 9 * unit, margin + 3 * unit);
            drawFace(svg, image[2], scheme,
                margin + 12 * unit, margin,
                margin + 12 * unit, margin + 6 * unit,
                margin + 9 * unit, margin + 3 * unit);

            return svg;
        }

        private void drawFace(Svg svg, int[] image, Color[] scheme, double ax, double ay, double bx, double by, double cx, double cy) {
            int stickerIndex = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3 - i; j++) {
                    drawSticker(svg, scheme[image[stickerIndex++]],
                        facePoint(ax, ay, bx, by, cx, cy, i, j),
                        facePoint(ax, ay, bx, by, cx, cy, i + 1, j),
                        facePoint(ax, ay, bx, by, cx, cy, i, j + 1));

                    if (i + j < 2) {
                        drawSticker(svg, scheme[image[stickerIndex++]],
                            facePoint(ax, ay, bx, by, cx, cy, i + 1, j),
                            facePoint(ax, ay, bx, by, cx, cy, i + 1, j + 1),
                            facePoint(ax, ay, bx, by, cx, cy, i, j + 1));
                    }
                }
            }
        }

        private double[] facePoint(double ax, double ay, double bx, double by, double cx, double cy, int i, int j) {
            double bWeight = i / 3.0;
            double cWeight = j / 3.0;
            double aWeight = 1 - bWeight - cWeight;
            return new double[] {
                aWeight * ax + bWeight * bx + cWeight * cx,
                aWeight * ay + bWeight * by + cWeight * cy
            };
        }

        private void drawSticker(Svg svg, Color color, double[] p1, double[] p2, double[] p3) {
            Path sticker = new Path();
            sticker.moveTo(p1[0], p1[1]);
            sticker.lineTo(p2[0], p2[1]);
            sticker.lineTo(p3[0], p3[1]);
            sticker.closePath();
            sticker.setFill(color);
            sticker.setStroke(Color.BLACK);
            svg.appendChild(sticker);
        }

        int[][] CORNER_COLORS = {
            {CenterOrd.U.ordinal(), CenterOrd.B.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.L.ordinal()},
            {CenterOrd.U.ordinal(), CenterOrd.R.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.B.ordinal()},
            {CenterOrd.U.ordinal(), CenterOrd.L.ordinal(), CenterOrd.F.ordinal(), CenterOrd.R.ordinal()},
            {CenterOrd.D.ordinal(), CenterOrd.F.ordinal(), CenterOrd.L.ordinal(), CenterOrd.BL.ordinal()},
            {CenterOrd.D.ordinal(), CenterOrd.BR.ordinal(), CenterOrd.R.ordinal(), CenterOrd.F.ordinal()},
            {CenterOrd.D.ordinal(), CenterOrd.BL.ordinal(), CenterOrd.B.ordinal(), CenterOrd.BR.ordinal()}
        };

        int[][] EDGE_COLORS = {
            {CenterOrd.U.ordinal(), CenterOrd.B.ordinal()}, // U_B
            {CenterOrd.U.ordinal(), CenterOrd.R.ordinal()}, // U_R
            {CenterOrd.U.ordinal(), CenterOrd.L.ordinal()}, // U_L
            {CenterOrd.F.ordinal(), CenterOrd.L.ordinal()}, // F_L
            {CenterOrd.F.ordinal(), CenterOrd.R.ordinal()}, // F_R
            {CenterOrd.BR.ordinal(), CenterOrd.R.ordinal()}, // R_BR
            {CenterOrd.BL.ordinal(), CenterOrd.B.ordinal()}, // B_BL
            {CenterOrd.BR.ordinal(), CenterOrd.B.ordinal()}, // B_BR
            {CenterOrd.BL.ordinal(), CenterOrd.L.ordinal()}, // L_BL
            {CenterOrd.F.ordinal(), CenterOrd.D.ordinal()}, // D_F
            {CenterOrd.BR.ordinal(), CenterOrd.D.ordinal()}, // D_BR
            {CenterOrd.BL.ordinal(), CenterOrd.D.ordinal()} // D_BL
        };

        private int cornerColor(Corner index, int orientation){
            int corner = corners[index.ordinal()];
            return CORNER_COLORS[getCornerIndex(corner)][(getCornerOrientation(corner) + orientation)%4];
        }



        private int edgeColor(Edge index, int orientation){
            int edge = edges[index.ordinal()];
            return EDGE_COLORS[edge][orientation];
        }

        private int[][] getImage(){

            int[][] image = new int[8][9];

            //U face
            image[0][0] = cornerColor(Corner.U_L, 0);
            image[0][1] = centers[CenterInd.U_BL.ordinal()];
            image[0][2] = edgeColor(Edge.U_L, 0);
            image[0][3] = centers[CenterInd.U_F.ordinal()];
            image[0][4] = cornerColor(Corner.U_F, 0);
            image[0][5] = edgeColor(Edge.U_B, 0);
            image[0][6] = centers[CenterInd.U_BR.ordinal()];
            image[0][7] = edgeColor(Edge.U_R, 0);
            image[0][8] = cornerColor(Corner.U_R, 0);

            //F face
            image[1][0] = cornerColor(Corner.D_L, 1);
            image[1][1] = centers[CenterInd.F_BL.ordinal()];
            image[1][2] = edgeColor(Edge.F_L, 0);
            image[1][3] = centers[CenterInd.F_U.ordinal()];
            image[1][4] = cornerColor(Corner.U_F, 2);
            image[1][5] = edgeColor(Edge.D_F, 0);
            image[1][6] = centers[CenterInd.F_BR.ordinal()];
            image[1][7] = edgeColor(Edge.F_R, 0);
            image[1][8] = cornerColor(Corner.D_R, 3);

            //BL face
            image[2][0] = cornerColor(Corner.U_L, 2);
            image[2][1] = centers[CenterInd.BL_U.ordinal()];
            image[2][2] = edgeColor(Edge.B_BL, 0);
            image[2][3] = centers[CenterInd.BL_BR.ordinal()];
            image[2][4] = cornerColor(Corner.D_B, 1);
            image[2][5] = edgeColor(Edge.L_BL, 0);
            image[2][6] = centers[CenterInd.BL_F.ordinal()];
            image[2][7] = edgeColor(Edge.D_BL, 0);
            image[2][8] = cornerColor(Corner.D_L, 3);

            //BR face
            image[3][0] = cornerColor(Corner.U_R, 2);
            image[3][1] = centers[CenterInd.BR_U.ordinal()];
            image[3][2] = edgeColor(Edge.R_BR, 0);
            image[3][3] = centers[CenterInd.BR_F.ordinal()];
            image[3][4] = cornerColor(Corner.D_R, 1);
            image[3][5] = edgeColor(Edge.B_BR, 0);
            image[3][6] = centers[CenterInd.BR_BL.ordinal()];
            image[3][7] = edgeColor(Edge.D_BR, 0);
            image[3][8] = cornerColor(Corner.D_B, 3);

            //L face
            image[4][0] = cornerColor(Corner.U_L, 3);
            image[4][1] = centers[CenterInd.L_B.ordinal()];
            image[4][2] = edgeColor(Edge.U_L, 1);
            image[4][3] = centers[CenterInd.L_R.ordinal()];
            image[4][4] = cornerColor(Corner.U_F, 1);
            image[4][5] = edgeColor(Edge.L_BL, 1);
            image[4][6] = centers[CenterInd.L_D.ordinal()];
            image[4][7] = edgeColor(Edge.F_L, 1);
            image[4][8] = cornerColor(Corner.D_L, 2);

            //R face
            image[5][0] = cornerColor(Corner.U_F, 3);
            image[5][1] = centers[CenterInd.R_L.ordinal()];
            image[5][2] = edgeColor(Edge.F_R, 1);
            image[5][3] = centers[CenterInd.R_D.ordinal()];
            image[5][4] = cornerColor(Corner.D_R, 2);
            image[5][5] = edgeColor(Edge.U_R, 1);
            image[5][6] = centers[CenterInd.R_B.ordinal()];
            image[5][7] = edgeColor(Edge.R_BR, 1);
            image[5][8] = cornerColor(Corner.U_R, 1);

            //B face
            image[6][0] = cornerColor(Corner.U_R, 3);
            image[6][1] = centers[CenterInd.B_R.ordinal()];
            image[6][2] = edgeColor(Edge.B_BR, 1);
            image[6][3] = centers[CenterInd.B_D.ordinal()];
            image[6][4] = cornerColor(Corner.D_B, 2);
            image[6][5] = edgeColor(Edge.U_B, 1);
            image[6][6] = centers[CenterInd.B_L.ordinal()];
            image[6][7] = edgeColor(Edge.B_BL, 1);
            image[6][8] = cornerColor(Corner.U_L, 1);

            //D face
            image[7][0] = cornerColor(Corner.D_R, 0);
            image[7][1] = centers[CenterInd.D_R.ordinal()];
            image[7][2] = edgeColor(Edge.D_BR, 1);
            image[7][3] = centers[CenterInd.D_B.ordinal()];
            image[7][4] = cornerColor(Corner.D_B, 0);
            image[7][5] = edgeColor(Edge.D_F, 1);
            image[7][6] = centers[CenterInd.D_L.ordinal()];
            image[7][7] = edgeColor(Edge.D_BL, 1);
            image[7][8] = cornerColor(Corner.D_L, 0);

            return image;
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
