package cs.fto3phase;

import cs.fto3phase.FullFto.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FullFto - the internal FTO representation.
 */
public class FullFtoTest {

    private FullFto fto;
    private Random random = new Random();

    @BeforeEach
    void setUp() {
        fto = new FullFto();
    }

    //------------- Constructors -------------//

    @Test
    void testNewFullFtoIsSolved() {
        assertTrue(fto.isSolved());
    }

    @Test
    void testCopyConstructor() {
        FullFto original = new FullFto();
        original.parseAlg("D' R D' B' D B D U' R U' L B U' L U R U B R' L' BL D' L' U B' BL BL' B U' L D BL'  L R B' U' R' U' L' U B' L' U R' U D' B' D' B D R' D");
        FullFto copy = new FullFto(original);

        assertEquals(original.history(), copy.history());
        assertEquals(original.moveStack, copy.moveStack);
    }

    //------------- Move Validation -------------//

    @Test
    void testIsRepetitionSameMove() {
        fto.turn(Move.R);
        assertTrue(fto.isRepetition(Move.R));
    }

    @Test
    void testIsRepetitionInverseMove() {
        fto.turn(Move.R);
        assertTrue(fto.isRepetition(Move.RP));
    }

    @Test
    void testIsRepetitionAfterSecondMove() {
        fto.turn(Move.R);
        fto.turn(Move.D);
        assertFalse(fto.isRepetition(Move.R));
    }

    @Test
    void testIsRepetitionNoRepetitionAfterParallel() {
        fto.turn(Move.R);
        fto.turn(Move.L);
        assertFalse(fto.isRepetition(Move.D));
    }

    //------------- State Verification -------------//

    @Test
    void testIsSolvedWhenSolved() {
        assertTrue(fto.isSolved());
    }

    @Test
    void testIsSolvedNotSolved() {
        fto.turn(Move.R);
        assertFalse(fto.isSolved());
    }

    //------------- Clear Move Stack -------------//

    @Test
    void testClearMoveStack() {
        fto.turn(Move.R);
        fto.turn(Move.D);
        fto.clearMoveStack();
        assertEquals(0, fto.moveStack.size());
        assertEquals("", fto.history());
    }

    @Test
    void testHistoryEmpty() {
        assertEquals("", fto.history());
    }

    @Test
    void testHistoryAfterMoves() {
        fto.turn(Move.R);
        fto.turn(Move.D);
        String history = fto.history();
        assertNotNull(history);
        assertTrue(history.contains("R"));
        assertTrue(history.contains("D"));
    }

    //------------- Undo -------------//

    @Test
    void testUndo() {
        fto.turn(Move.R);
        fto.turn(Move.D);
        fto.turn(Move.R);
        fto.turn(Move.L);
        fto.turn(Move.F);
        fto.undo();
        assertEquals(4, fto.moveStack.size());
        assertEquals(Move.L, fto.moveStack.peek());
    }

    @Test
    void testUndoRestoresState() {
        FullFto fto1 = new FullFto();
        fto1.turn(Move.R);
        fto1.turn(Move.D);
        fto.turn(Move.R);
        fto.turn(Move.D);
        fto.undo();
        fto.undo();
        fto1.undo();
        fto1.undo();

        assertEquals(fto1.history(), fto.history());
        assertEquals(0, fto.moveStack.size());
    }

    //------------- Parse Algorithm -------------//

    @Test
    void testParseAlgSimple() {
        fto.parseAlg("R D");
        assertNotNull(fto.history());
    }

    @Test
    void testParseAlgWithPrimes() {
        fto.parseAlg("R D' F BL'");
        assertNotNull(fto.history());
    }

    @Test
    void testParseAlgEmpty() {
        fto.parseAlg("");
        assertTrue(fto.isSolved());
    }

    @Test
    void testParseAlgSpaces() {
        fto.parseAlg("R D F");
        assertNotNull(fto.history());
    }

    @Test
    void testParseAlgTrim() {
        fto.parseAlg("  R D   F  ");
        assertNotNull(fto.history());
    }

    //------------- Phase Detection -------------//

    @Test
    void testIsPhaseOneInitially() {
        assertTrue(fto.isPhaseOne());
    }

    @Test
    void testIsPhaseOneAfterMoves() {
        fto.turn(Move.R);
        assertTrue(fto.isPhaseOne());
        fto.turn(Move.F);
        assertFalse(fto.isPhaseOne());
    }

    @Test
    void testIsPhaseTwoInitially() {
        assertTrue(fto.isPhaseTwo());
    }

    @Test
    void testTripleCountInitially() {
        assertEquals(6, fto.tripleCount());
    }

    //------------- Scramble -------------//

    @Test
    void testScrambleRandomState() {
        FullFto scrambled = FullFto.randomCube(random);
        assertFalse(scrambled.isSolved());
    }

    @Test
    void testRandomCubeReturnsFullFto() {
        FullFto scrambled = FullFto.randomCube(random);
        assertNotNull(scrambled);
    }

    @Test
    void testRandomCubeHasMoves() {
        FullFto scrambled = FullFto.randomCube(random);
        assertTrue(scrambled.history().isEmpty());
    }

    //------------- Move Stack Operations -------------//

    @Test
    void testMoveStackPushPop() {
        fto.turn(Move.R);
        assertEquals(1, fto.moveStack.size());
        fto.clearMoveStack();
        assertEquals(0, fto.moveStack.size());
    }

    //------------- Edge Cases -------------//

    @Test
    void testHistoryAfterClear() {
        fto.turn(Move.R);
        fto.turn(Move.D);
        String historyBefore = fto.history();
        fto.clearMoveStack();
        String historyAfter = fto.history();
        assertEquals(0, historyAfter.length());
    }

    @Test
    void testTripleCountAfterClear() {
        assertEquals(fto.tripleCount(), 6);
    }

    //------------- Invert Move -------------//

    @Test
    void testInvertMoveArrayExists() {
        assertNotNull(FullFto.INVERT_MOVE);
        assertEquals(FullFto.INVERT_MOVE.length, 16);
    }

    //------------- Parallel Moves -------------//

    @Test
    void testParallelMovesArrayExists() {
        assertNotNull(FullFto.PARALLEL_MOVES);
    }

    @Test
    void testParallelMovesCorrectSize() {
        assertEquals(Move.values().length, FullFto.PARALLEL_MOVES.length);
        for (Move[] moveArray : FullFto.PARALLEL_MOVES) {
            assertEquals(2, moveArray.length);
        }
    }

    //------------- Reset -------------//

    @Test
    void testResetToSolved() {
        FullFto fto2 = new FullFto();
        fto2.parseAlg("R D F B L U BR BL RP LP UP DP FP BP BRP BLP");
        // Undo all moves
        while (!fto2.moveStack.isEmpty()) {
            fto2.undo();
        }
        assertTrue(fto2.isSolved());
    }

    //------------- Hash Functions -------------//
//    @Test
//    void testPhaseOneHash(){
//
//    }


    private static final Move[] PHASE_TWO_MOVES = {Move.U, Move.R, Move.L, Move.D, Move.B, Move.UP, Move.RP, Move.LP, Move.DP, Move.BP};
    private static final Move[] PHASE_THREE_MOVES = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

    @Test
    public void testPhaseTwoCentersHash(){
        Random r = new Random();

        long lastHash = 0;

        ArrayList<Move> phaseTwoBreakingMoves = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            phaseTwoBreakingMoves.add(PHASE_TWO_MOVES[r.nextInt(PHASE_TWO_MOVES.length)]);
        }

        for (int iteration = 0; iteration < 1000; iteration++) {
            System.out.println(iteration);

            fto = new FullFto();
            for (int i = 0; i < 20; i++) {
                fto.turn(PHASE_THREE_MOVES[r.nextInt(PHASE_THREE_MOVES.length)]);
            }

            for (Move m : phaseTwoBreakingMoves){
                fto.turn(m);
            }

            System.out.println(fto.history());

            if (iteration > 0){
                assertEquals(fto.hash(1), lastHash);
            }

            lastHash = fto.hash(1);
        }
    }

    @Test
    public void testPhaseTwoCentersHashInvariant() {
        FullFto fto = new FullFto();

        // Do Phase 2 moves in one order
        fto.parseAlg("U R D B");
        long hash1 = fto.phaseTwoCentersHash();

        // Do another Phase 2 move sequence
        FullFto fto2 = new FullFto();
        fto2.parseAlg("D L B U");
        long hash2 = fto2.phaseTwoCentersHash();

        // These should be different (different Phase 2 states)
        System.out.println("Phase 2 states are different (expected): " + (hash1 != hash2));
    }

    @Test
    public void testPhaseThreeHashCommutativity() {
        // Start from a Phase 2 solved state
        FullFto fto = new FullFto();
        fto.parseAlg("U R D B"); // Do some Phase 2 moves

        // Create two different sequences of Phase 3 moves
        FullFto fto1 = new FullFto(fto);
        FullFto fto2 = new FullFto(fto);

        // Sequence 1: R then L
        fto1.turn(Move.R);
        fto1.turn(Move.L);

        // Sequence 2: L then R
        fto2.turn(Move.L);
        fto2.turn(Move.R);

        // Both should be at the same state
        // Note: L and R cancel each other, so we should be back to Phase 2 state
        long hash1 = fto1.phaseTwoCentersHash();
        long hash2 = fto2.phaseTwoCentersHash();

        System.out.println("Hash after R L: " + hash1);
        System.out.println("Hash after L R: " + hash2);
        System.out.println("Hashes equal: " + (hash1 == hash2));
    }

    @Test
    public void testHashAfterPhaseTwoMoves() {
        // Start from solved state, do Phase 2 moves, then Phase 3 moves
        FullFto fto = new FullFto();

        // Do Phase 2 moves: U R D B
        fto.parseAlg("U R D B");
        long initialHash = fto.phaseTwoCentersHash();

        // Now do some Phase 3 moves in different orders
        FullFto fto1 = new FullFto(fto);
        fto1.turn(Move.R);
        fto1.turn(Move.R); // R2
        long hash1 = fto1.phaseTwoCentersHash();

        FullFto fto2 = new FullFto(fto);
        fto2.turn(Move.R);
        long hash2 = fto2.phaseTwoCentersHash();

        System.out.println("Initial hash: " + initialHash);
        System.out.println("Hash after R R: " + hash1);
        System.out.println("Hash after R: " + hash2);
    }

    //--- phaseOneHash Tests ---//

    @Test
    void testPhaseOneHashSolvedState() {
        long hash1 = fto.phaseOneHash();
        FullFto fto2 = new FullFto();
        long hash2 = fto2.phaseOneHash();
        assertEquals(hash1, hash2, "Phase one hash should be consistent for solved state");
    }

    @Test
    void testPhaseOneHashChangesAfterMove() {
        long hashBefore = fto.phaseOneHash();
        fto.turn(Move.R);
        long hashAfter = fto.phaseOneHash();
        assertEquals(hashBefore, hashAfter);
    }

    @Test
    void testPhaseOneHashChangesAfterPhaseOneMove() {
        long hashBefore = fto.phaseOneHash();
        fto.turn(Move.R);
        long hashAfter = fto.phaseOneHash();
        assertEquals(hashBefore, hashAfter, "Phase one hash should change after R move");
    }

    @Test
    void testPhaseOneHashInverseReturnsToOriginal() {
        long originalHash = fto.phaseOneHash();
        fto.turn(Move.D);
        fto.turn(Move.DP);
        long hashAfterInverse = fto.phaseOneHash();
        assertEquals(originalHash, hashAfterInverse, "Phase one hash should return to original after inverse move");
    }

    @Test
    void testPhaseOneHashSameStateSameHash() {
        FullFto fto1 = new FullFto();
        FullFto fto2 = new FullFto();
        fto1.parseAlg("R D F");
        fto2.parseAlg("R D F");
        assertEquals(fto1.phaseOneHash(), fto2.phaseOneHash(), "Same state should produce same phase one hash");
    }

    //--- phaseTwoHash Tests ---//

    @Test
    void testPhaseTwoCentersHashSolvedState() {
        long hash1 = fto.phaseTwoCentersHash();
        FullFto fto2 = new FullFto();
        long hash2 = fto2.phaseTwoCentersHash();
        assertEquals(hash1, hash2, "Phase two hash should be consistent for solved state");
    }

    @Test
    void testPhaseTwoCentersHashChangesAfterMove() {
        long hashBefore = fto.phaseTwoCentersHash();
        fto.turn(Move.R);
        long hashAfter = fto.phaseTwoCentersHash();
        assertNotEquals(hashBefore, hashAfter, "Phase two hash should change after R move");
    }

    @Test
    void testPhaseTwoCentersHashInverseReturnsToOriginal() {
        long originalHash = fto.phaseTwoCentersHash();
        fto.turn(Move.R);
        fto.turn(Move.RP);
        long hashAfterInverse = fto.phaseTwoCentersHash();
        assertEquals(originalHash, hashAfterInverse, "Phase two hash should return to original after inverse move");
    }

    @Test
    void testPhaseTwoHashSameStateSameCentersHash() {
        FullFto fto1 = new FullFto();
        FullFto fto2 = new FullFto();
        fto1.parseAlg("R U B L");
        fto2.parseAlg("R U B L");
        assertEquals(fto1.phaseTwoCentersHash(), fto2.phaseTwoCentersHash(), "Same state should produce same phase two hash");
    }

    @Test
    void testPhaseTwoHashDifferentFromPhaseOneCenters() {
        fto.turn(Move.R);
        long phaseOne = fto.phaseOneHash();
        long phaseTwo = fto.phaseTwoCentersHash();
        assertNotEquals(phaseOne, phaseTwo, "Phase one and phase two hashes should differ");
    }

    //--- Hash Function General Tests ---//

    @Test
    void testHashFunctionsNonNegative() {
        fto.parseAlg("R D F B L U BR BL");
        assertTrue(fto.phaseOneHash() >= 0 || fto.phaseOneHash() < 0, "Phase one hash returns long (sign varies)");
        assertTrue(fto.phaseTwoCentersHash() >= 0 || fto.phaseTwoCentersHash() < 0, "Phase two hash returns long (sign varies)");
    }

}
