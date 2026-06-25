package levigibson.fto3phase;

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

            int idx = ftoCubie.idxEdges();
            FtoCubie testCube = new FtoCubie();

            testCube.setEdges(idx);
            assertEquals(idx, testCube.idxEdges());
            assertArrayEquals(ftoCubie.edges, testCube.edges);
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

            int idx = ftoCubie.idxCornerPermutation();
            FtoCubie testCube = new FtoCubie();

            testCube.setCornerPermutation(idx);
            assertEquals(idx, testCube.idxCornerPermutation());
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

            int idx = ftoCubie.idxCornerOrientation();
            FtoCubie testCube = new FtoCubie();

            testCube.setCornerOrientation(idx);
            assertEquals(idx, testCube.idxCornerOrientation());
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

            int loc = ftoCubie.idxPhaseOneEdgeLocations();
            int perm = ftoCubie.idxPhaseOneEdgePermutation();
            FtoCubie testCube = new FtoCubie();

            testCube.setG1Edges(loc, perm);
            System.out.println();
            System.out.println();

            assertEquals(loc, testCube.idxPhaseOneEdgeLocations());
            assertEquals(perm, testCube.idxPhaseOneEdgePermutation());
        }
    }
}
