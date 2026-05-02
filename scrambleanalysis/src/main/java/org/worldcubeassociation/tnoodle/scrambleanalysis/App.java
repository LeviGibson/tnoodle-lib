package org.worldcubeassociation.tnoodle.scrambleanalysis;

import org.worldcubeassociation.tnoodle.puzzle.FaceTurningOctahedronPuzzle;

public class App {

    public static void main(String[] args) {
        FaceTurningOctahedronPuzzle cube = new FaceTurningOctahedronPuzzle();
        String scramble = cube.generateScramble();
        System.out.println(scramble);

    }
}
