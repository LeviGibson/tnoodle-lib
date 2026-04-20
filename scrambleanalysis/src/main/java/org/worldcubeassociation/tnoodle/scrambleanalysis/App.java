package org.worldcubeassociation.tnoodle.scrambleanalysis;

import org.worldcubeassociation.tnoodle.puzzle.CubePuzzle;
import org.worldcubeassociation.tnoodle.puzzle.SquareOnePuzzle;
import org.worldcubeassociation.tnoodle.puzzle.ThreeByThreeCubePuzzle;
import org.worldcubeassociation.tnoodle.scrambles.InvalidScrambleException;

import java.util.List;

public class App {

    public static void main(String[] args) {
        SquareOnePuzzle cube = new SquareOnePuzzle();
        String scramble = cube.generateScramble();
        System.out.println(scramble);

    }
}
