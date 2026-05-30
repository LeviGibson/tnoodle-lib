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

    private enum CenterOrd {
        U, F, BR, BL, L, R, B, D
    }

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
        defaultColorScheme.put("L", new Color(124, 2, 158));
        defaultColorScheme.put("R", Color.RED);
        defaultColorScheme.put("U", Color.WHITE);
        defaultColorScheme.put("BL", new Color(255, 128, 0));
        defaultColorScheme.put("BR", Color.GRAY);
    }

    @Override
    public Map<String, Color> getDefaultColorScheme() {
        return new HashMap<>(defaultColorScheme);
    }

    private static final int FACE_TURNING_OCTAHEDRON_UNIT_SIZE = 36;
    private static final int FACE_TURNING_OCTAHEDRON_MARGIN = 4;
    private static final double FACE_TURNING_OCTAHEDRON_FACE_GAP = 6.0;

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

        private int[][] image = new int[8][9];

        private final int[][][] C = {
            {{0,0},{6,8},{2,0},{4,0}},
            {{0,8},{5,8},{3,0},{6,0}},
            {{0,4},{4,4},{1,4},{5,0}},
            {{7,8},{1,0},{4,8},{2,8}},
            {{7,0},{3,4},{5,4},{1,8}},
            {{7,4},{2,4},{6,4},{3,8}}
        };

        private final int[][][] E_CELLS = {
            {{0,5},{6,5}},
            {{0,7},{5,5}},
            {{0,2},{4,2}},
            {{1,2},{4,7}},
            {{1,7},{5,2}},
            {{3,2},{5,7}},
            {{2,2},{6,7}},
            {{3,5},{6,2}},
            {{2,5},{4,5}},
            {{1,5},{7,5}},
            {{3,7},{7,2}},
            {{2,7},{7,7}}
        };

        private final int[][] CT = {
            {0,1},{0,6},{0,3},
            {1,3},{1,6},{1,1},
            {3,1},{3,6},{3,3},
            {2,1},{2,6},{2,3},
            {4,1},{4,3},{4,6},
            {5,1},{5,6},{5,3},
            {6,1},{6,6},{6,3},
            {7,6},{7,1},{7,3}
        };

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
                scheme[i] = colorSchemeMap.get(CenterOrd.values()[i].toString());
            }

            int unit = FACE_TURNING_OCTAHEDRON_UNIT_SIZE;
            int margin = FACE_TURNING_OCTAHEDRON_MARGIN;
            int m = margin;
            Svg svg = new Svg(getImageSize(unit));

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

        private void swap(int f0, int s0, int f1, int s1, int f2, int s2) {
            int tmp = image[f0][s0];
            image[f0][s0] = image[f2][s2];
            image[f2][s2] = image[f1][s1];
            image[f1][s1] = tmp;
        }

        private void edgeCycle(int eA, int eB, int eC) {
            swap(E_CELLS[eA][0][0], E_CELLS[eA][0][1],
                 E_CELLS[eB][0][0], E_CELLS[eB][0][1],
                 E_CELLS[eC][0][0], E_CELLS[eC][0][1]);
            swap(E_CELLS[eA][1][0], E_CELLS[eA][1][1],
                 E_CELLS[eB][1][0], E_CELLS[eB][1][1],
                 E_CELLS[eC][1][0], E_CELLS[eC][1][1]);
        }

        private void centerCycle(int cA, int cB, int cC) {
            swap(CT[cA][0], CT[cA][1],
                 CT[cB][0], CT[cB][1],
                 CT[cC][0], CT[cC][1]);
        }

        private void twistCornerCycle(int cA, int cB, int cC, int tA, int tB, int tC) {
            int[] vA = new int[4];
            int[] vB = new int[4];
            int[] vC = new int[4];
            for (int i = 0; i < 4; i++) {
                vA[i] = image[C[cA][i][0]][C[cA][i][1]];
                vB[i] = image[C[cB][i][0]][C[cB][i][1]];
                vC[i] = image[C[cC][i][0]][C[cC][i][1]];
            }
            for (int i = 0; i < 4; i++) {
                image[C[cB][i][0]][C[cB][i][1]] = vA[(i + tB) % 4];
                image[C[cC][i][0]][C[cC][i][1]] = vB[(i + tC) % 4];
                image[C[cA][i][0]][C[cA][i][1]] = vC[(i + tA) % 4];
            }
        }

        private void cornerCycle(int cA, int cB, int cC) {
            twistCornerCycle(cA, cB, cC, 0, 0, 0);
        }

        public void turn(Move move) {
            switch (move) {
                case R:
                    twistCornerCycle(2, 1, 4, 3, 2, 3);
                    edgeCycle(4, 1, 5);
                    centerCycle(15, 16, 17);
                    centerCycle(3, 1, 8);
                    centerCycle(4, 2, 6);
                    break;
                case L:
                    twistCornerCycle(0, 2, 3, 3, 2, 3);
                    edgeCycle(8, 2, 3);
                    centerCycle(12, 13, 14);
                    centerCycle(0, 3, 10);
                    centerCycle(2, 5, 9);
                    break;
                case U:
                    cornerCycle(0, 1, 2);
                    edgeCycle(2, 0, 1);
                    centerCycle(0, 1, 2);
                    centerCycle(15, 12, 18);
                    centerCycle(16, 13, 19);
                    break;
                case D:
                    cornerCycle(3, 4, 5);
                    edgeCycle(11, 9, 10);
                    centerCycle(21, 22, 23);
                    centerCycle(5, 8, 11);
                    centerCycle(4, 7, 10);
                    break;
                case F:
                    twistCornerCycle(2, 4, 3, 3, 3, 2);
                    edgeCycle(3, 4, 9);
                    centerCycle(3, 4, 5);
                    centerCycle(13, 17, 21);
                    centerCycle(15, 22, 14);
                    break;
                case B:
                    twistCornerCycle(1, 0, 5, 3, 2, 3);
                    edgeCycle(7, 0, 6);
                    centerCycle(18, 19, 20);
                    centerCycle(1, 9, 7);
                    centerCycle(0, 11, 6);
                    break;
                case BR:
                    twistCornerCycle(1, 5, 4, 3, 3, 2);
                    edgeCycle(10, 5, 7);
                    centerCycle(6, 7, 8);
                    centerCycle(16, 20, 22);
                    centerCycle(17, 18, 23);
                    break;
                case BL:
                    twistCornerCycle(0, 3, 5, 3, 3, 2);
                    edgeCycle(6, 8, 11);
                    centerCycle(9, 10, 11);
                    centerCycle(19, 14, 23);
                    centerCycle(12, 21, 20);
                    break;
                case RP:
                    twistCornerCycle(2, 4, 1, 2, 1, 1);
                    edgeCycle(5, 1, 4);
                    centerCycle(15, 17, 16);
                    centerCycle(3, 8, 1);
                    centerCycle(4, 6, 2);
                    break;
                case LP:
                    twistCornerCycle(0, 3, 2, 2, 1, 1);
                    edgeCycle(3, 2, 8);
                    centerCycle(12, 14, 13);
                    centerCycle(0, 10, 3);
                    centerCycle(2, 9, 5);
                    break;
                case UP:
                    cornerCycle(0, 2, 1);
                    edgeCycle(1, 0, 2);
                    centerCycle(0, 2, 1);
                    centerCycle(15, 18, 12);
                    centerCycle(16, 19, 13);
                    break;
                case DP:
                    cornerCycle(3, 5, 4);
                    edgeCycle(10, 9, 11);
                    centerCycle(21, 23, 22);
                    centerCycle(5, 11, 8);
                    centerCycle(4, 10, 7);
                    break;
                case FP:
                    twistCornerCycle(2, 3, 4, 1, 1, 2);
                    edgeCycle(9, 4, 3);
                    centerCycle(3, 5, 4);
                    centerCycle(13, 21, 17);
                    centerCycle(15, 14, 22);
                    break;
                case BP:
                    twistCornerCycle(1, 5, 0, 2, 1, 1);
                    edgeCycle(6, 0, 7);
                    centerCycle(18, 20, 19);
                    centerCycle(1, 7, 9);
                    centerCycle(0, 6, 11);
                    break;
                case BRP:
                    twistCornerCycle(1, 4, 5, 1, 1, 2);
                    edgeCycle(7, 5, 10);
                    centerCycle(6, 8, 7);
                    centerCycle(16, 22, 20);
                    centerCycle(17, 23, 18);
                    break;
                case BLP:
                    twistCornerCycle(0, 5, 3, 1, 1, 2);
                    edgeCycle(11, 8, 6);
                    centerCycle(9, 11, 10);
                    centerCycle(19, 23, 14);
                    centerCycle(12, 20, 21);
                    break;
                default:
                    throw new RuntimeException("Move not recognized");
            }
        }

    }
}
