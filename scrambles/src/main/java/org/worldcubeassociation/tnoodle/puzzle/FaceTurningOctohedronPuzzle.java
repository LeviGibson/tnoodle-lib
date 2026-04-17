package org.worldcubeassociation.tnoodle.puzzle;

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

//    private static final int radius = 32;

//    private final ThreadLocal<Search> twoPhaseSearcher;

    public FaceTurningOctohedronPuzzle() {
        wcaMinScrambleDistance = 9;

//        twoPhaseSearcher = ThreadLocal.withInitial(Search::new);
    }

    //TODO
//    @Override
//    public PuzzleStateAndGenerator generateRandomMoves(Random r) {
//        FullCube randomState = FullCube.randomCube(r);
//
//        String scramble = twoPhaseSearcher.get().solution(randomState, Search.INVERSE_SOLUTION).trim();
//        PuzzleState state;
//        try {
//            state = getSolvedState().applyAlgorithm(scramble);
//        } catch (InvalidScrambleException e) {
//            throw new RuntimeException(e);
//        }
//        return new PuzzleStateAndGenerator(state, scramble);

//    }

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

    public class FaceTurningOctahedronState extends PuzzleState {

        public FaceTurningOctahedronState() {
        }

        public FaceTurningOctahedronState(boolean sliceSolved, int[] pieces) {
        }

        //TODO
        @Override
        public Map<String, FaceTurningOctahedronState> getScrambleSuccessors() {
            return null;
        }

        //TODO
        @Override
        public Map<String, FaceTurningOctahedronState> getSuccessorsByName() {
            return null;
        }

        //TODO
        @Override
        public String solveIn(int n) {
            return null;
        }

        //TODO
        @Override
        public boolean equals(Object other) {
//            SquareOneState o = ((SquareOneState) other);
//            return Arrays.equals(pieces, o.pieces) && sliceSolved == o.sliceSolved;
            return false;
        }

        //TODO
        @Override
        public int hashCode() {
//            return Arrays.hashCode(pieces) ^ (sliceSolved ? 1 : 0);
            return 0;
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

    }
}
