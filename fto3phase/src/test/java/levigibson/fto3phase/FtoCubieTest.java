package levigibson.fto3phase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
            assertArrayEquals(ftoCubie.getEdges(), testCube.getEdges());
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
            assertArrayEquals(ftoCubie.getTriangles1(), testCube.getTriangles1());
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
            assertArrayEquals(ftoCubie.getCornerPerm(), testCube.getCornerPerm());
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
            assertArrayEquals(ftoCubie.getCornerOri(), testCube.getCornerOri());
        }
    }

    @Test
    void testG1EdgeIndex(){
        Random r = new Random(42);
        for (int i = 0; i < 100000; i++) {
            FtoCubie ftoCubie = new FtoCubie();
            for (int j = 0; j < 100; j++) {
                FtoCubie out = new FtoCubie();
                ftoCubie.turn(r.nextInt(16), out);
                ftoCubie = out;
            }

            int idx = ftoCubie.g1PackEdges();
            FtoCubie testCube = new FtoCubie();

            testCube.g1SetEdges(idx);
            assertEquals(idx, testCube.g1PackEdges());
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

            int idx = ftoCubie.g1PackTriangles();
            FtoCubie testCube = new FtoCubie();

            testCube.g1SetTriangles(idx);
            assertEquals(idx, testCube.g1PackTriangles());
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
                assertArrayEquals(state.getCornerPerm(), result.getCornerPerm(),
                    "trial " + trial + " move " + cw + "+" + ccw + ": cp mismatch");
                assertArrayEquals(state.getCornerOri(), result.getCornerOri(),
                    "trial " + trial + " move " + cw + "+" + ccw + ": co mismatch");
                assertArrayEquals(state.getEdges(), result.getEdges(),
                    "trial " + trial + " move " + cw + "+" + ccw + ": edges mismatch");
                assertArrayEquals(state.getTriangles1(), result.getTriangles1(),
                    "trial " + trial + " move " + cw + "+" + ccw + ": centers1 mismatch");
                assertArrayEquals(state.getTriangles2(), result.getTriangles2(),
                    "trial " + trial + " move " + cw + "+" + ccw + ": centers2 mismatch");

                // CCW then CW should also return to original state
                state.turn(ccw, afterFirst);
                afterFirst.turn(cw, result);
                assertArrayEquals(state.getCornerPerm(), result.getCornerPerm(),
                    "trial " + trial + " move " + ccw + "+" + cw + ": cp mismatch");
                assertArrayEquals(state.getCornerOri(), result.getCornerOri(),
                    "trial " + trial + " move " + ccw + "+" + cw + ": co mismatch");
                assertArrayEquals(state.getEdges(), result.getEdges(),
                    "trial " + trial + " move " + ccw + "+" + cw + ": edges mismatch");
                assertArrayEquals(state.getTriangles1(), result.getTriangles1(),
                    "trial " + trial + " move " + ccw + "+" + cw + ": centers1 mismatch");
                assertArrayEquals(state.getTriangles2(), result.getTriangles2(),
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

            int idx = ftoCubie.g2PackTris();
            FtoCubie testCube = new FtoCubie();

            testCube.g2SetTriangles(idx);
            assertEquals(idx, testCube.g2PackTris());
            assertArrayEquals(ftoCubie.getTriangles2(), testCube.getTriangles2());
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

            int idx = ftoCubie.g2PackEdges();
            FtoCubie testCube = new FtoCubie();

            testCube.g2SetEdges(idx);
            assertEquals(idx, testCube.g2PackEdges());
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
                int idx = ftoCubie.g2PackTriples(color);
                FtoCubie testCube = new FtoCubie();

                testCube.g2SetTriples(idx, color);
                assertEquals(idx, testCube.g2PackTriples(color));
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

            int idx = ftoCubie.g3PackCorners();
            FtoCubie testCube = new FtoCubie();

            testCube.g3SetCorners(idx);
            assertEquals(idx, testCube.g3PackCorners());

        }
    }

    @Disabled("Don't include in the test suite")
    @Test
    void performanceTest(){
        int n = 500;

        Random r = new Random(42);
        long start = System.currentTimeMillis();
        int totalMoves = 0;

        Search search = new Search();
        for (int i = 0; i < n; i++) {
            FtoCubie rs = FtoCubie.randomCube(r);
            String solution = search.solution(rs);
            int sollen = solution.split(" ").length;
            System.out.print(solution);
            System.out.println("(" + sollen + ")");

            totalMoves += sollen;
        }

        System.out.println("Average Time per Scramble: " + ((System.currentTimeMillis() - start) / n) + "ms");
        System.out.println("Average Moves per Scramble: " + (float)(totalMoves) / (float)n);
    }
}
