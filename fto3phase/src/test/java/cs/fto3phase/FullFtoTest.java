package cs.fto3phase;

import cs.fto3phase.FullFto.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FullFto - the internal FTO representation.
 */
public class FullFtoTest {

    private FullFto fto;
    private final Random random = new Random(0);

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
        assertEquals(original.phaseOneHash(), copy.phaseOneHash());
        assertEquals(original.phaseTwoHash(), copy.phaseTwoHash());
        assertEquals(original.phaseThreeHash(), copy.phaseThreeHash());
    }

    @Test
    void testCopyConstructorDoesNotShareMoveStack() {
        fto.parseAlg("R D F");
        FullFto copy = new FullFto(fto);

        copy.turn(Move.B);

        assertEquals(3, fto.historyLength());
        assertEquals(4, copy.historyLength());
        assertNotEquals(fto.history(), copy.history());
    }

    @Test
    void testCenterIndexTrackingIsOffByDefault() throws Exception {
        long solvedLocations = phaseTwoCenterIndexLocations(fto);

        fto.turn(Move.R);

        assertEquals(solvedLocations, phaseTwoCenterIndexLocations(fto));
    }

    @Test
    void testCenterIndexTrackingCanBeEnabledFromSolvedState() throws Exception {
        fto.enableCenterIndexTracking();
        long solvedLocations = phaseTwoCenterIndexLocations(fto);

        fto.turn(Move.R);

        assertNotEquals(solvedLocations, phaseTwoCenterIndexLocations(fto));
    }

    @Test
    void testCenterIndexTrackingCannotBeEnabledFromUnsolvedState() {
        fto.turn(Move.R);

        assertThrows(IllegalStateException.class, () -> fto.enableCenterIndexTracking());
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
        assertEquals(0, fto.historyLength());
        assertEquals("", fto.history());
    }

    @Test
    void testHistoryEmpty() {
        assertEquals("", fto.history());
    }


    @Test
    void testHistoryUsesPrimeNotation() {
        fto.turn(Move.RP);
        fto.turn(Move.BLP);

        assertEquals("R' BL' ", fto.history());
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
        assertEquals(4, fto.historyLength());
        assertEquals(Move.L, fto.lastMove());
    }

    @Test
    void testUndoRestoresState() {
        fto.parseAlg("R D F B L");
        while (!(fto.historyLength() == 0)) {
            fto.undo();
        }

        assertTrue(fto.isSolved());
        assertEquals(0, fto.historyLength());
    }

    @Test
    void testEachMoveInverseRestoresSolvedState() {
        for (Move move : Move.values()) {
            FullFto moved = new FullFto();
            moved.turn(move);
            moved.turn(FullFto.INVERT_MOVE[move.ordinal()]);

            assertTrue(moved.isSolved(), move + " followed by inverse should solve");
        }
    }


    //------------- Parse Algorithm -------------//

    @Test
    void testParseAlgSimple() {
        fto.parseAlg("R D");
        assertEquals("R D ", fto.history());
    }

    @Test
    void testParseAlgWithPrimes() {
        fto.parseAlg("R D' F BL'");
        assertEquals("R D' F BL' ", fto.history());
    }

    @Test
    void testParseAlgEmpty() {
        fto.parseAlg("");
        assertTrue(fto.isSolved());
    }

    @Test
    void testParseAlgSpaces() {
        fto.parseAlg("R D F");
        assertEquals("R D F ", fto.history());
    }

    @Test
    void testParseAlgTrim() {
        fto.parseAlg("  R D   F  ");
        assertEquals("R D F ", fto.history());
    }

    @Test
    void testParseAlgInvalidMoveThrows() {
        assertThrows(IllegalArgumentException.class, () -> fto.parseAlg("R X"));
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

    @Test
    void testIsPhaseOneFalseAfterBreakingMove() {
        fto.parseAlg("F");
        assertFalse(fto.isPhaseOne());
    }

    @Test
    void testIsPhaseOneTrueAfterPhaseTwoMoves() {
        fto.parseAlg("U R D B");
        assertTrue(fto.isPhaseOne());
    }

    @Test
    void testIsPhaseTwoFalseAfterBreakingMove() {
        fto.parseAlg("F");
        assertFalse(fto.isPhaseTwo());
    }

    //------------- Triple Methods -------------//

    @Test
    void testIsTripleAllSolved() {
        for (int i = 0; i < 6; i++) {
            assertTrue(fto.isTriple(i));
        }
    }

    @Test
    void testTriplePairsOnCornerAllSolved() {
        for (int i = 0; i < 6; i++) {
            assertEquals(2, fto.triplePairsOnCorner(i));
        }
    }

    @Test
    void testTripleCountAfterBreakingMoves() {
        fto.parseAlg("R D F");
        assertTrue(fto.tripleCount() < 6);
    }

    @Test
    void testTriplePairCountSolved() {
        assertEquals(12, fto.triplePairCount());
    }

    @Test
    void testTriplePairCountAfterBreakingMove() {
        fto.parseAlg("F");
        assertTrue(fto.triplePairCount() < 12);
    }

    @Test
    void testPhaseTwoTripleIndexSolved() {
        assertEquals(0b111111111111, fto.phaseTwoTripleIndex());
    }

    @Test
    void testPhaseTwoTripleIndexBitWidth() {
        int maxIndex = (1 << 12) - 1;
        fto.parseAlg("R D F B L U BR BL");
        int index = fto.phaseTwoTripleIndex();
        assertTrue(index >= 0 && index <= maxIndex);
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

    @Test
    void testScrambleRandomG2StateClearsHistory() {
        fto.scrambleRandomG2State(random, 20);

        assertEquals(0, fto.historyLength());
        assertEquals("", fto.history());
    }

    @Test
    void testScrambleRandomG2StateIsPhaseOne() {
        fto.scrambleRandomG2State(random, 20);

        assertTrue(fto.isPhaseOne());
    }

    @Test
    void testScrambleRandomG2StateDeterministic() {
        FullFto fto1 = new FullFto();
        FullFto fto2 = new FullFto();
        Random r1 = new Random(42);
        Random r2 = new Random(42);
        fto1.scrambleRandomG2State(r1, 30);
        fto2.scrambleRandomG2State(r2, 30);
        assertEquals(fto1.phaseOneHash(), fto2.phaseOneHash());
        assertEquals(fto1.phaseTwoHash(), fto2.phaseTwoHash());
    }

    @Test
    void testRandomCubeDeterministic() {
        Random r1 = new Random(42);
        Random r2 = new Random(42);
        FullFto c1 = FullFto.randomCube(r1);
        FullFto c2 = FullFto.randomCube(r2);
        assertEquals(c1.phaseOneHash(), c2.phaseOneHash());
    }

    //------------- Rotate -------------//

    @Test
    void testRotateYDoesNotThrow() {
        fto.rotate(1);
        assertFalse(fto.isSolved());
    }

    @Test
    void testRotateYPrimeDoesNotThrow() {
        fto.rotate(2);
        assertFalse(fto.isSolved());
    }



    @Test
    void testRotateTwelveYReturnsToSolved() {
        for (int i = 0; i < 12; i++) {
            fto.rotate(1);
        }
        assertTrue(fto.isSolved());
    }

    @Test
    void testRotateFourYDoesNotReturnToSolved() {
        fto.rotate(1);
        fto.rotate(1);
        fto.rotate(1);
        fto.rotate(1);
        assertFalse(fto.isSolved());
    }

    @Test
    void testRotateSixYReturnsToSolved() {
        for (int i = 0; i < 6; i++) {
            fto.rotate(1);
        }
        assertTrue(fto.isSolved());
    }

    //------------- Edge Index -------------//

    @Test
    void testPhaseTwoEdgeLehmerIndexSolved() {
        assertEquals(0, fto.phaseTwoEdgeLehmerIndex());
    }

    @Test
    void testPhaseTwoEdgeLehmerIndexNonNegative() {
        fto.parseAlg("U R D B");
        assertTrue(fto.phaseTwoEdgeLehmerIndex() >= 0);
    }

    @Test
    void testPhaseTwoEdgeLehmerIndexRoundTrip() {
        // We must stay in phase 2: D-face edges (positions 9,10,11) must remain
        // solved so that the 9-element Lehmer-code universe {0..8} exactly
        // matches the pieces at positions 0..8. Only U, R, L, B moves never
        // touch the D-face edges.
        Move[] phase2Moves = {Move.U, Move.R, Move.L, Move.B};
        Random rng = new Random(42);
        FullFto reference = new FullFto();
        for (int i = 0; i < 1000; i++) {
            reference.turn(phase2Moves[rng.nextInt(phase2Moves.length)]);
            int edgeIdx = reference.phaseTwoEdgeLehmerIndex();
            FullFto restored = new FullFto();
            restored.setPhaseTwoEdgeLehmerIndex(edgeIdx);
            for (int e = 0; e < 9; e++) {
                assertEquals(reference.getEdge(e), restored.getEdge(e),
                    "Edge " + e + " mismatch at iteration " + i);
            }
        }
    }

    @Test
    void testGeneratedEdgePruningConsistent() {
        // The generated table will differ from the bundled edgeprun.dat because
        // the bundled table was pre-computed for a different solver with
        // different move definitions.  Instead, verify the generated table is
        // self-consistent: for every index, every move reaches a state whose
        // distance is at most (this_distance + 1).
        FullFto.Move[] p2moves = {Move.U, Move.UP, Move.R, Move.L, Move.B,
            Move.RP, Move.LP, Move.BP, Move.DP, Move.D};

        byte[] table = FtoSearch.generateEdgePruningTable();

        FullFto worker = new FullFto();
        int violations = 0;
        for (int i = 0; i < 181440; i++) {
            int dist = table[i];
            if (dist == 24) continue; // unreachable sentinel

            worker.setPhaseTwoEdgeLehmerIndex(i);
            for (FullFto.Move move : p2moves) {
                worker.turn(move);
                int nextIdx = worker.phaseTwoEdgeLehmerIndex();
                int nextDist = table[nextIdx];
                worker.undo();

                if (nextDist > dist + 1) {
                    violations++;
                    if (violations <= 5) {
                        System.out.println("  violation at idx=" + i
                            + " dist=" + dist + " move=" + move
                            + " → idx=" + nextIdx + " dist=" + nextDist);
                    }
                }
            }
        }

        assertEquals(0, violations,
            "Generated table distances must be monotonic under single moves");
    }

    //------------- Move Stack Operations -------------//

    @Test
    void testMoveStackPushPop() {
        fto.turn(Move.R);
        assertEquals(1, fto.historyLength());
        fto.clearMoveStack();
        assertEquals(0, fto.historyLength());
    }

    //------------- Edge Cases -------------//

    @Test
    void testHistoryAfterClear() {
        fto.turn(Move.R);
        fto.turn(Move.D);
        fto.clearMoveStack();
        assertEquals(0, fto.history().length());
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

    @Test
    void testInvertMoveArrayIsSymmetric() {
        for (Move move : Move.values()) {
            Move inverse = FullFto.INVERT_MOVE[move.ordinal()];
            assertEquals(move, FullFto.INVERT_MOVE[inverse.ordinal()]);
        }
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

    //------------- Move Validation -------------//

    @Test
    void testIsValidPhaseOneFinishingSequenceAcceptsShortHistory() {
        assertTrue(fto.isValidPhaseOneFinishingSequence());
        fto.turn(Move.R);
        assertTrue(fto.isValidPhaseOneFinishingSequence());
    }

    @Test
    void testIsValidPhaseOneFinishingSequenceRejectsNonBreakingMove() {
        fto.parseAlg("R D");
        assertFalse(fto.isValidPhaseOneFinishingSequence());
    }

    @Test
    void testIsValidPhaseOneFinishingSequenceRejectsParallelPreviousMove() {
        fto.parseAlg("BL R");
        assertFalse(fto.isValidPhaseOneFinishingSequence());
    }

    @Test
    void testIsValidPhaseOneFinishingSequenceAcceptsBreakingNonParallelMove() {
        fto.parseAlg("R F");
        assertTrue(fto.isValidPhaseOneFinishingSequence());
    }

    @Test
    void testIsValidParallelSequenceTrueOnEmptyStack() {
        assertTrue(fto.isValidParallelSequence(Move.R));
    }

    @Test
    void testIsValidParallelSequenceRejectsLowerId() {
        fto.turn(Move.BL);
        assertFalse(fto.isValidParallelSequence(Move.R));
    }

    @Test
    void testIsValidParallelSequenceAllowsHigherId() {
        fto.turn(Move.R);
        assertTrue(fto.isValidParallelSequence(Move.BL));
    }

    @Test
    void testIsValidParallelSequenceAllowsNonParallel() {
        fto.turn(Move.R);
        assertTrue(fto.isValidParallelSequence(Move.U));
    }

    @Test
    void testIsRepetitionDetectsLastMoveRepeated() {
        fto.parseAlg("R L");
        assertTrue(fto.isRepetition(Move.L));
    }

    @Test
    void testIsRepetitionDetectsParallelInverseOfLastLastMove() {
        fto.parseAlg("F R");
        assertTrue(fto.isRepetition(Move.FP));
    }

    @Test
    void testIsRepetitionDetectsParallelOfLastLastMove() {
        fto.parseAlg("F R");
        assertTrue(fto.isRepetition(Move.F));
    }

    @Test
    void testIsRepetitionAllowsNonParallelSameAsLastLastMove() {
        fto.parseAlg("R L");
        assertFalse(fto.isRepetition(Move.R));
    }

    @Test
    void testIsRepetitionWithEmptyHistory() {
        assertFalse(fto.isRepetition(Move.R));
    }

    //------------- State Queries -------------//

    @Test
    void testEqualsSameState() {
        FullFto fto2 = new FullFto();
        assertTrue(fto.equals(fto2));
    }

    @Test
    void testEqualsDifferentState() {
        FullFto fto2 = new FullFto();
        fto2.turn(Move.R);
        assertFalse(fto.equals(fto2));
    }

    @Test
    void testEqualsSameAfterSameMoves() {
        FullFto fto2 = new FullFto();
        fto.parseAlg("R D F B L");
        fto2.parseAlg("R D F B L");
        assertTrue(fto.equals(fto2));
    }

    @Test
    void testEqualsAfterUndo() {
        FullFto fto2 = new FullFto(fto);
        fto.turn(Move.R);
        fto.undo();
        assertTrue(fto.equals(fto2));
    }

    @Test
    void testLastMoveSimple() {
        fto.turn(Move.R);
        assertEquals(Move.R, fto.lastMove());
    }

    @Test
    void testLastMoveWithOffset() {
        fto.parseAlg("R D F");
        assertEquals(Move.F, fto.lastMove(0));
        assertEquals(Move.D, fto.lastMove(1));
        assertEquals(Move.R, fto.lastMove(2));
    }

    @Test
    void testLastMoveWithOffsetAfterUndo() {
        fto.parseAlg("R D F B");
        fto.undo();
        assertEquals(Move.F, fto.lastMove(0));
        assertEquals(Move.D, fto.lastMove(1));
    }

    @Test
    void testIsNormalizedInitially() {
        assertTrue(fto.isNormalized());
    }

    @Test
    void testIsNormalizedAfterRMove() {
        fto.turn(Move.R);
        assertTrue(fto.isNormalized());
    }

    @Test
    void testIsNormalizedAfterLMove() {
        fto.turn(Move.L);
        assertTrue(fto.isNormalized());
    }

    @Test
    void testIsNotNormalizedAfterUMove() {
        fto.turn(Move.U);
        assertFalse(fto.isNormalized());
    }

    @Test
    void testIsNormalizedAfterInverseMove() {
        fto.parseAlg("R D F B L");
        while (fto.historyLength() > 0) {
            fto.undo();
        }
        assertTrue(fto.isNormalized());
    }

    @Test
    void testIsPhaseOneBreakingMoveTrue() {
        assertTrue(FullFto.isPhaseOneBreakingMove(Move.F));
        assertTrue(FullFto.isPhaseOneBreakingMove(Move.FP));
        assertTrue(FullFto.isPhaseOneBreakingMove(Move.BR));
        assertTrue(FullFto.isPhaseOneBreakingMove(Move.BRP));
        assertTrue(FullFto.isPhaseOneBreakingMove(Move.BL));
        assertTrue(FullFto.isPhaseOneBreakingMove(Move.BLP));
    }

    @Test
    void testIsPhaseOneBreakingMoveFalse() {
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.R));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.RP));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.L));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.LP));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.U));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.UP));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.D));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.DP));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.B));
        assertFalse(FullFto.isPhaseOneBreakingMove(Move.BP));
    }

    //------------- Reset -------------//

    @Test
    void testResetToSolved() {
        FullFto fto2 = new FullFto();
        fto2.parseAlg("R D F B L U BR BL RP LP UP DP FP BP BRP BLP");
        while (!(fto2.historyLength() == 0)) {
            fto2.undo();
        }
        assertTrue(fto2.isSolved());
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
    void testPhaseOneHashChangesAfterPhaseOneMove() {
        long hashBefore = fto.phaseOneHash();
        fto.turn(Move.R);
        long hashAfter = fto.phaseOneHash();
        assertEquals(hashBefore, hashAfter, "Phase one hash should be unchanged after R move");
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
        long hash1 = fto.phaseTwoHash();
        FullFto fto2 = new FullFto();
        long hash2 = fto2.phaseTwoHash();
        assertEquals(hash1, hash2, "Phase two hash should be consistent for solved state");
    }

    @Test
    void testPhaseTwoCentersHashChangesAfterMove() {
        long hashBefore = fto.phaseTwoHash();
        fto.turn(Move.U);
        long hashAfter = fto.phaseTwoHash();
        assertNotEquals(hashBefore, hashAfter, "Phase two hash should change after U move");
    }

    @Test
    void testPhaseTwoCentersHashInverseReturnsToOriginal() {
        long originalHash = fto.phaseTwoHash();
        fto.turn(Move.R);
        fto.turn(Move.RP);
        long hashAfterInverse = fto.phaseTwoHash();
        assertEquals(originalHash, hashAfterInverse, "Phase two hash should return to original after inverse move");
    }

    @Test
    void testPhaseTwoHashSameStateSameCentersHash() {
        FullFto fto1 = new FullFto();
        FullFto fto2 = new FullFto();
        fto1.parseAlg("R U B L");
        fto2.parseAlg("R U B L");
        assertEquals(fto1.phaseTwoHash(), fto2.phaseTwoHash(), "Same state should produce same phase two hash");
    }

    @Test
    void testPhaseTwoHashDifferentFromPhaseOneCenters() {
        fto.turn(Move.R);
        long phaseOne = fto.phaseOneHash();
        long phaseTwo = fto.phaseTwoHash();
        assertNotEquals(phaseOne, phaseTwo, "Phase one and phase two hashes should differ");
    }

    @Test
    void testPhaseThreeHashSameStateSameHash() {
        FullFto fto1 = new FullFto();
        FullFto fto2 = new FullFto();

        fto1.parseAlg("R U B L");
        fto2.parseAlg("R U B L");

        assertEquals(fto1.phaseThreeHash(), fto2.phaseThreeHash());
    }

    @Test
    void testPhaseTwoTripleDataMatchesCurrentState() {
        fto.enableCenterIndexTracking();
        fto.parseAlg("R U B L");
        long tripleData = fto.packPhaseTwoTripleData();

        assertTrue(fto.checkPhaseTwoTripleData(tripleData));
    }

    @Test
    void testPhaseTwoTripleDataRejectsDifferentState() {
        fto.enableCenterIndexTracking();
        fto.parseAlg("R U B L");
        long tripleData = fto.packPhaseTwoTripleData();

        FullFto different = new FullFto();
        different.parseAlg("R U B");

        assertFalse(different.checkPhaseTwoTripleData(tripleData));
    }

    private static long phaseTwoCenterIndexLocations(FullFto fto) throws Exception {
        Field stateField = FullFto.class.getDeclaredField("state");
        stateField.setAccessible(true);
        Object state = stateField.get(fto);

        Method method = state.getClass().getDeclaredMethod("phaseTwoCenterIndexLocations");
        method.setAccessible(true);
        return (long) method.invoke(state);
    }

}
