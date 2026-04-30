package org.worldcubeassociation.tnoodle.scrambleanalysis;

import org.worldcubeassociation.tnoodle.puzzle.CubePuzzle;
import org.worldcubeassociation.tnoodle.puzzle.FaceTurningOctohedronPuzzle;
import org.worldcubeassociation.tnoodle.puzzle.SquareOnePuzzle;
import org.worldcubeassociation.tnoodle.puzzle.ThreeByThreeCubePuzzle;
import org.worldcubeassociation.tnoodle.scrambles.InvalidScrambleException;

import java.util.List;

public class App {

    public static void main(String[] args) {
        FaceTurningOctohedronPuzzle cube = new FaceTurningOctohedronPuzzle();
        String scramble = cube.generateScramble();
        System.out.println(scramble);

    }
}
