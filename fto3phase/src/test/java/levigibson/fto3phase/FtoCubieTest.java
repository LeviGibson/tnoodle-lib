package levigibson.fto3phase;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FtoCubieTest {

    private static final int[] CW_MOVES = {
        FtoCubie.R, FtoCubie.L, FtoCubie.B, FtoCubie.D,
        FtoCubie.U, FtoCubie.F, FtoCubie.BR, FtoCubie.BL
    };
    private static final int[] CCW_MOVES = {
        FtoCubie.RP, FtoCubie.LP, FtoCubie.BP, FtoCubie.DP,
        FtoCubie.UP, FtoCubie.FP, FtoCubie.BRP, FtoCubie.BLP
    };
    private static final String[] NAMES = {
        "R", "L", "B", "D", "U", "F", "BR", "BL"
    };

    @Test
    void testAllMoves3ReturnToSolved() {
        for (int i = 0; i < CW_MOVES.length; i++) {
            FtoCubie cube = new FtoCubie();
            FtoCubie tmp1 = new FtoCubie();
            FtoCubie tmp2 = new FtoCubie();
            cube.turn(CW_MOVES[i], tmp1);
            tmp1.turn(CW_MOVES[i], tmp2);
            tmp2.turn(CW_MOVES[i], cube);
            assertTrue(cube.isSolved(), NAMES[i] + "^3 should return to solved");
        }
    }

    @Test
    void testAllMovesFollowedByInverseReturnToSolved() {
        for (int i = 0; i < CW_MOVES.length; i++) {
            FtoCubie cube = new FtoCubie();
            FtoCubie mid = new FtoCubie();
            cube.turn(CW_MOVES[i], mid);
            mid.turn(CCW_MOVES[i], cube);
            assertTrue(cube.isSolved(), NAMES[i] + " followed by " + NAMES[i] + "' should return to solved");
        }
    }

    @Test
    void testEdgeIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packAllEdges();
            FtoCubie testCube = new FtoCubie();

            testCube.setAllEdges(idx);
            assertEquals(idx, testCube.packAllEdges());
            assertArrayEquals(ftoCubie.edges, testCube.edges);
        }
    }

    @Test
    void testTriangleIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packAllTriangles(0);
            FtoCubie testCube = new FtoCubie();

            testCube.setAllTriangles(idx, 0);
            assertEquals(idx, testCube.packAllTriangles(0));
            assertArrayEquals(ftoCubie.triangles1, testCube.triangles1);
        }
    }

    @Test
    void testCornerPermutationIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packAllCornerPermutation();
            FtoCubie testCube = new FtoCubie();

            testCube.setAllCornerPermutation(idx);
            assertEquals(idx, testCube.packAllCornerPermutation());
            assertArrayEquals(ftoCubie.cp, testCube.cp);
        }
    }

    @Test
    void testCornerOrientationIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packAllCornerOrientation();
            FtoCubie testCube = new FtoCubie();

            testCube.setAllCornerOrientation(idx);
            assertEquals(idx, testCube.packAllCornerOrientation());
            assertArrayEquals(ftoCubie.co, testCube.co);
        }
    }

    @Test
    void testG1EdgeIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packG1Edges();
            FtoCubie testCube = new FtoCubie();

            testCube.setG1Edges(idx);
            assertEquals(idx, testCube.packG1Edges());
        }
    }

    @Test
    void testPhaseOneTriangleIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packG1Triangles();
            FtoCubie testCube = new FtoCubie();

            testCube.setG1Triangles(idx);
            assertEquals(idx, testCube.packG1Triangles());
        }
    }

    @Test
    void testEveryMoveHasCorrectInverse() {
        for (int trial = 0; trial < 50; trial++) {
            // Generate a random non-solved state by applying 50 random moves
            FtoCubie state = new FtoCubie();
            FtoCubie tmp = new FtoCubie();
            Random r = new Random(trial);
            for (int i = 0; i < 50; i++) {
                state.turn(r.nextInt(16), tmp);
                FtoCubie swap = state;
                state = tmp;
                tmp = swap;
            }

            FtoCubie afterFirst = new FtoCubie();
            FtoCubie result = new FtoCubie();

            for (int move = 0; move < 16; move += 2) {
                int cw = move;
                int ccw = move + 1;

                // CW then CCW should return to original state
                state.turn(cw, afterFirst);
                afterFirst.turn(ccw, result);
                assertArrayEquals(state.cp, result.cp,
                    "trial " + trial + " move " + cw + "+" + ccw + ": cp mismatch");
                assertArrayEquals(state.co, result.co,
                    "trial " + trial + " move " + cw + "+" + ccw + ": co mismatch");
                assertArrayEquals(state.edges, result.edges,
                    "trial " + trial + " move " + cw + "+" + ccw + ": edges mismatch");
                assertArrayEquals(state.triangles1, result.triangles1,
                    "trial " + trial + " move " + cw + "+" + ccw + ": centers1 mismatch");
                assertArrayEquals(state.triangles2, result.triangles2,
                    "trial " + trial + " move " + cw + "+" + ccw + ": centers2 mismatch");

                // CCW then CW should also return to original state
                state.turn(ccw, afterFirst);
                afterFirst.turn(cw, result);
                assertArrayEquals(state.cp, result.cp,
                    "trial " + trial + " move " + ccw + "+" + cw + ": cp mismatch");
                assertArrayEquals(state.co, result.co,
                    "trial " + trial + " move " + ccw + "+" + cw + ": co mismatch");
                assertArrayEquals(state.edges, result.edges,
                    "trial " + trial + " move " + ccw + "+" + cw + ": edges mismatch");
                assertArrayEquals(state.triangles1, result.triangles1,
                    "trial " + trial + " move " + ccw + "+" + cw + ": centers1 mismatch");
                assertArrayEquals(state.triangles2, result.triangles2,
                    "trial " + trial + " move " + ccw + "+" + cw + ": centers2 mismatch");
            }
        }
    }

    @Test
    void testPhaseTwoTrisIndex(){
        Random r = new Random(42);
        int[] safeMoves = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; // R,RP,L,LP,B,BP,D,DP,U,UP
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(safeMoves[r.nextInt(safeMoves.length)], out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packG2Tris();
            FtoCubie testCube = new FtoCubie();

            testCube.setG2Triangles(idx);
            assertEquals(idx, testCube.packG2Tris());
            assertArrayEquals(ftoCubie.triangles2, testCube.triangles2);
        }
    }

    @Test
    void testPhaseTwoEdgesIndex(){
        Random r = new Random(42);
        int[] safeMoves = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; // R,RP,L,LP,B,BP,D,DP,U,UP
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(safeMoves[r.nextInt(safeMoves.length)], out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packG2Edges();
            FtoCubie testCube = new FtoCubie();

            testCube.setG2Edges(idx);
            assertEquals(idx, testCube.packG2Edges());
//            assertArrayEquals(Arrays.copyOf(ftoCubie.edges, 9), Arrays.copyOf(testCube.edges, 9));
        }
    }

    @Test
    void testPhaseTwoTriples(){
        Random r = new Random(42);
        int[] safeMoves = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; // R,RP,L,LP,B,BP,D,DP,U,UP
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(safeMoves[r.nextInt(safeMoves.length)], out);
                ftoCubie = out;
            }

            for (int color = 0; color < 4; color++) {
                int idx = ftoCubie.packG2Triples(color);
                FtoCubie testCube = new FtoCubie();

                testCube.setG2Triples(idx, color);
                assertEquals(idx, testCube.packG2Triples(color));
            }

        }
    }

    @Test
    void testPhaseThreeCorners(){
        Random r = new Random(42);
        int[] safeMoves = {0, 1, 2, 3, 4, 5, 6, 7}; // R,RP,L,LP,B,BP,D,DP,U,UP
        for (int i = 0; i < 10000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(safeMoves[r.nextInt(safeMoves.length)], out);
                ftoCubie = out;
            }

            int idx = ftoCubie.packG3Corners();
            FtoCubie testCube = new FtoCubie();

            testCube.setG3Corners(idx);
            assertEquals(idx, testCube.packG3Corners());

        }
    }
}
