package org.worldcubeassociation.tnoodle.puzzle;

import cs.fto3phase.FtoSearch;
import cs.fto3phase.FullFto;
import org.worldcubeassociation.tnoodle.scrambles.*;
import org.worldcubeassociation.tnoodle.svglite.Color;
import org.worldcubeassociation.tnoodle.svglite.Dimension;
import org.worldcubeassociation.tnoodle.svglite.Svg;
import org.worldcubeassociation.tnoodle.svglite.Path;

import java.util.*;

import org.timepedia.exporter.client.Export;

@Export
public class FaceTurningOctahedronPuzzle extends Puzzle {

    private enum Move {R, L, U, D, F, B, BR, BL, RP, LP, UP, DP, FP, BP, BRP, BLP}
    private static final String[] MOVE_NAMES = {"R", "L", "U", "D", "F", "B", "BR", "BL", "R'", "L'", "U'", "D'", "F'", "B'", "BR'", "BL'"};
    private static final String[] FACE_NAMES = {"U", "F", "BR", "BL", "L", "R", "B", "D"};

    private final ThreadLocal<FtoSearch> threePhaseSearcher;

    public FaceTurningOctahedronPuzzle() {
        wcaMinScrambleDistance = 2;
        threePhaseSearcher = ThreadLocal.withInitial(FtoSearch::new);
    }

    private static final Map<String, Color> defaultColorScheme = new HashMap<>();

    static {
        defaultColorScheme.put("B", Color.BLUE);
        defaultColorScheme.put("D", Color.YELLOW);
        defaultColorScheme.put("F", Color.GREEN);
        defaultColorScheme.put("L", new Color(124, 2, 158)); // Purple
        defaultColorScheme.put("R", Color.RED);
        defaultColorScheme.put("U", Color.WHITE);
        defaultColorScheme.put("BL", new Color(255, 128, 0)); // Orange
        defaultColorScheme.put("BR", Color.GRAY);
    }

    @Override
    public Map<String, Color> getDefaultColorScheme() {
        return new HashMap<>(defaultColorScheme);
    }

    private static final int FACE_TURNING_OCTAHEDRON_PIECE_SIZE = 30;
    private static final int FACE_TURNING_OCTAHEDRON_GAP = 3;
    private static final double FACE_TURNING_OCTAHEDRON_FACE_GAP = 6.0;

    @Override
    public Dimension getPreferredSize() {
        return getImageSize(FACE_TURNING_OCTAHEDRON_GAP, FACE_TURNING_OCTAHEDRON_PIECE_SIZE);
    }

    private static Dimension getImageSize(int gap, int pieceSize) {
        return new Dimension(getFTOViewWidth(gap, pieceSize), getFTOViewHeight(gap, pieceSize));
    }

    private static int getFTOViewWidth(int gap, int pieceSize) {
        return 12 * pieceSize + 4 * gap;
    }

    private static int getFTOViewHeight(int gap, int pieceSize) {
        return 6 * pieceSize + 4 * gap;
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

    @Override
    protected int getRandomMoveCount() {
        return 34;
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

        private final int[][] image = new int[8][9];

        private final int[] SOLVED_FACE_COLOR = {0, 1, 3, 2, 4, 5, 6, 7};

        public FaceTurningOctahedronState() {
            for (int f = 0; f < 8; f++) {
                int color = SOLVED_FACE_COLOR[f];
                for (int s = 0; s < 9; s++) {
                    image[f][s] = color;
                }
            }
        }

        public FaceTurningOctahedronState(FaceTurningOctahedronState copyFrom) {
            for (int f = 0; f < 8; f++) {
                System.arraycopy(copyFrom.image[f], 0, image[f], 0, 9);
            }
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

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FaceTurningOctahedronState)) {
                return false;
            }
            FaceTurningOctahedronState o = (FaceTurningOctahedronState) other;
            return Arrays.deepEquals(image, o.image);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(image);
        }

        @Override
        protected Svg drawScramble(Map<String, Color> colorSchemeMap) {
            Color[] scheme = new Color[8];
            for (int i = 0; i < scheme.length; i++) {
                scheme[i] = colorSchemeMap.get(FACE_NAMES[i]);
            }

            int unit = FACE_TURNING_OCTAHEDRON_PIECE_SIZE;
            int margin = FACE_TURNING_OCTAHEDRON_GAP;
            int m = margin;
            Svg svg = new Svg(getPreferredSize());

            //@formatter:off
            drawFace(svg, image[4], scheme, m,        m,        m,        m + 6*unit, m + 3*unit, m + 3*unit);
            drawFace(svg, image[0], scheme, m,        m,        m + 6*unit, m,          m + 3*unit, m + 3*unit);
            drawFace(svg, image[1], scheme, m,        m + 6*unit, m + 6*unit, m + 6*unit, m + 3*unit, m + 3*unit);
            drawFace(svg, image[5], scheme, m + 3*unit, m + 3*unit, m + 6*unit, m,          m + 6*unit, m + 6*unit);
            drawFace(svg, image[3], scheme, m + 6*unit, m,          m + 9*unit, m + 3*unit, m + 6*unit, m + 6*unit);
            drawFace(svg, image[6], scheme, m + 6*unit, m,          m + 12*unit, m,          m + 9*unit, m + 3*unit);
            drawFace(svg, image[7], scheme, m + 6*unit, m + 6*unit, m + 12*unit, m + 6*unit, m + 9*unit, m + 3*unit);
            drawFace(svg, image[2], scheme, m + 12*unit, m,          m + 12*unit, m + 6*unit, m + 9*unit, m + 3*unit);
            //@formatter:on

            return svg;
        }

        private void drawFace(Svg svg, int[] img, Color[] scheme, double ax, double ay, double bx, double by, double cx, double cy) {
            double[] a = insetFacePoint(ax, ay, bx, by, cx, cy);
            double[] b = insetFacePoint(bx, by, ax, ay, cx, cy);
            double[] c = insetFacePoint(cx, cy, ax, ay, bx, by);
            ax = a[0];
            ay = a[1];
            bx = b[0];
            by = b[1];
            cx = c[0];
            cy = c[1];

            int stickerIndex = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3 - i; j++) {
                    drawSticker(svg, scheme[img[stickerIndex++]],
                        facePoint(ax, ay, bx, by, cx, cy, i, j),
                        facePoint(ax, ay, bx, by, cx, cy, i + 1, j),
                        facePoint(ax, ay, bx, by, cx, cy, i, j + 1));

                    if (i + j < 2) {
                        drawSticker(svg, scheme[img[stickerIndex++]],
                            facePoint(ax, ay, bx, by, cx, cy, i + 1, j),
                            facePoint(ax, ay, bx, by, cx, cy, i + 1, j + 1),
                            facePoint(ax, ay, bx, by, cx, cy, i, j + 1));
                    }
                }
            }
        }

        private double[] insetFacePoint(double x, double y, double other1x, double other1y, double other2x, double other2y) {
            double centerX = (x + other1x + other2x) / 3.0;
            double centerY = (y + other1y + other2y) / 3.0;
            double dx = centerX - x;
            double dy = centerY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance == 0) {
                return new double[] { x, y };
            }
            double inset = Math.min(FACE_TURNING_OCTAHEDRON_FACE_GAP, distance);

            return new double[] {
                x + dx / distance * inset,
                y + dy / distance * inset
            };
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

        private void applyCycle(int[] cycles, int offset) {
            int f0 = cycles[offset],     s0 = cycles[offset + 1];
            int f1 = cycles[offset + 2], s1 = cycles[offset + 3];
            int f2 = cycles[offset + 4], s2 = cycles[offset + 5];
            int tmp = image[f0][s0];
            image[f0][s0] = image[f2][s2];
            image[f2][s2] = image[f1][s1];
            image[f1][s1] = tmp;
        }

        //What cycles does each move make on the individual stickers?
        //Each 3-cycle is encoded in 6 ints, 3 for the faces, 3 for the stickers
        private final int[][] MOVE_CYCLES = {
            {0,3,3,1,1,6, 0,4,3,0,1,8, 0,6,3,3,1,3, 0,7,3,2,1,7, 0,8,3,4,1,4, 4,4,6,0,7,0, 5,0,5,8,5,4, 5,1,5,6,5,3, 5,2,5,5,5,7},
            {0,0,1,4,2,8, 0,1,1,3,2,6, 0,2,1,2,2,5, 0,3,1,1,2,1, 0,4,1,0,2,0, 4,0,4,4,4,8, 4,1,4,3,4,6, 4,2,4,7,4,5, 5,0,7,8,6,8},
            {0,0,0,8,0,4, 0,1,0,6,0,3, 0,2,0,5,0,7, 1,4,2,0,3,0, 4,0,6,0,5,0, 4,1,6,1,5,1, 4,2,6,5,5,5, 4,3,6,6,5,6, 4,4,6,8,5,8},
            {1,0,3,4,2,4, 1,1,3,3,2,3, 1,5,3,7,2,7, 1,6,3,6,2,6, 1,8,3,8,2,8, 4,8,5,4,6,4, 7,0,7,4,7,8, 7,1,7,3,7,6, 7,2,7,7,7,5},
            {0,4,3,4,2,8, 1,0,1,4,1,8, 1,1,1,3,1,6, 1,2,1,7,1,5, 4,3,5,3,7,6, 4,4,5,4,7,8, 4,6,5,1,7,1, 4,7,5,2,7,5, 4,8,5,0,7,0},
            {0,0,2,4,3,0, 0,1,2,3,3,1, 0,5,2,2,3,5, 0,6,2,1,3,6, 0,8,2,0,3,8, 4,0,7,4,5,8, 6,0,6,8,6,4, 6,1,6,6,6,3, 6,2,6,5,6,7},
            {0,8,2,4,1,8, 3,0,3,8,3,4, 3,1,3,6,3,3, 3,2,3,5,3,7, 5,3,6,1,7,3, 5,4,6,0,7,4, 5,6,6,3,7,1, 5,7,6,2,7,2, 5,8,6,4,7,0},
            {0,0,1,0,3,8, 2,0,2,8,2,4, 2,1,2,6,2,3, 2,2,2,5,2,7, 4,0,7,8,6,4, 4,1,7,6,6,3, 4,5,7,7,6,7, 4,6,7,3,6,6, 4,8,7,4,6,8},
            {0,3,1,6,3,1, 0,4,1,8,3,0, 0,6,1,3,3,3, 0,7,1,7,3,2, 0,8,1,4,3,4, 4,4,7,0,6,0, 5,0,5,4,5,8, 5,1,5,3,5,6, 5,2,5,7,5,5},
            {0,0,2,8,1,4, 0,1,2,6,1,3, 0,2,2,5,1,2, 0,3,2,1,1,1, 0,4,2,0,1,0, 4,0,4,8,4,4, 4,1,4,6,4,3, 4,2,4,5,4,7, 5,0,6,8,7,8},
            {0,0,0,4,0,8, 0,1,0,3,0,6, 0,2,0,7,0,5, 1,4,3,0,2,0, 4,0,5,0,6,0, 4,1,5,1,6,1, 4,2,5,5,6,5, 4,3,5,6,6,6, 4,4,5,8,6,8},
            {1,0,2,4,3,4, 1,1,2,3,3,3, 1,5,2,7,3,7, 1,6,2,6,3,6, 1,8,2,8,3,8, 4,8,6,4,5,4, 7,0,7,8,7,4, 7,1,7,6,7,3, 7,2,7,5,7,7},
            {0,4,2,8,3,4, 1,0,1,8,1,4, 1,1,1,6,1,3, 1,2,1,5,1,7, 4,3,7,6,5,3, 4,4,7,8,5,4, 4,6,7,1,5,1, 4,7,7,5,5,2, 4,8,7,0,5,0},
            {0,0,3,0,2,4, 0,1,3,1,2,3, 0,5,3,5,2,2, 0,6,3,6,2,1, 0,8,3,8,2,0, 4,0,5,8,7,4, 6,0,6,4,6,8, 6,1,6,3,6,6, 6,2,6,7,6,5},
            {0,8,1,8,2,4, 3,0,3,4,3,8, 3,1,3,3,3,6, 3,2,3,7,3,5, 5,3,7,3,6,1, 5,4,7,4,6,0, 5,6,7,1,6,3, 5,7,7,2,6,2, 5,8,7,0,6,4},
            {0,0,3,8,1,0, 2,0,2,4,2,8, 2,1,2,3,2,6, 2,2,2,7,2,5, 4,0,6,4,7,8, 4,1,6,3,7,6, 4,5,6,7,7,7, 4,6,6,6,7,3, 4,8,6,8,7,4}
        };

        public void turn(Move move) {
            //Get all the sticker 3-cycles for this move
            int[] cycles = MOVE_CYCLES[move.ordinal()];

            //Loop over the 3-cycles one at a time and apply them
            for (int i = 0; i < 54; i += 6) {
                applyCycle(cycles, i);
            }
        }

    }
}
