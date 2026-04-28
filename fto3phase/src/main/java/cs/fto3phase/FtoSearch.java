package cs.fto3phase;

import  java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static cs.fto3phase.FullFto.Move;

public class FtoSearch {
    private String[] solution;

    private static final int PHASE_ONE_PRUNING_DEPTH = 4;
    private static final int PHASE_TWO_PRUNING_DEPTH = 7;
    private static final int PHASE_THREE_PRUNING_DEPTH = 5;
    private static HashMap<Long, Integer> phaseOnePruningTable;
    private static HashMap<Long, ArrayList<PhaseTwoPruningEntry>> phaseTwoPruningTable;



    private static class PhaseTwoPruningEntry{
        public int distanceToSolved;
        public long triples;
        public PhaseTwoPruningEntry(int distanceToSolved, long triples){
            this.distanceToSolved = distanceToSolved;
            this.triples = triples;
        }
    }

    public FtoSearch(){
        solution = new String[3];
    }

    //--------------- Static Pruning Table Generation ---------------//

    private static final double[][] BETAS = {
        {0, 0, 0}, // 0
        {0, 0, 0}, // 1
        {0, 0, 0}, // 2
        {0, 0, 0}, // 3
        {0, 0, 0}, // 4
        {0, 0, 0}, // 5
        {0, 0, 0}, // 6
        {0, 0, 0}, // 7
        {-3.0228, 1.6976, -1.1361}, // 8
        {-2.8937, 1.5726, -0.9234}, // 9
        {-3.05216, 1.51510, -0.79536}, // 10
        {-2.14743, 1.40091, -0.81680}, // 11
        {-3.02457, 1.22228, -0.52262}, // 12
        {-1.91284, 1.23967, -0.67062}, // 13
        {-2.59936, 1.25072, -0.52469}, // 14
        {-2.26634, 1.11097, -0.44942}, // 15
        {-2.40757, 1.14014, -0.42935}, // 16
        {-2.12963, 1.06413, -0.41900}, // 17
        {-2.19083, 1.03216, -0.35640}, // 18
        {-1.91584, 0.95119, -0.33685}, // 19
    };

    public static final Move[] PHASE_TWO_MOVES = {Move.U, Move.R, Move.L, Move.D, Move.B, Move.UP, Move.RP, Move.LP, Move.DP, Move.BP};
    public static final Move[] PHASE_THREE_MOVES = {Move.R, Move.L, Move.D, Move.B, Move.RP, Move.LP, Move.DP, Move.BP};

    private static byte[] phaseTwoEdgePruningTable = new byte[362880/2];

    private static void savePruningTable(byte[] table, String filename) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filename);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(table);
        }
    }

    private static byte[] loadPruningTable(String filename, int size) throws IOException {
        byte[] table = new byte[size];
        try (FileInputStream fis = new FileInputStream(filename);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            bis.read(table);
        }
        return table;
    }


    private static void phaseTwoEdgePruningSearch(int depth, FullFto fto){

        if (depth == 0)
            return;

        int edgeIndex = fto.phaseTwoEdgeIndex();
        int ply = fto.historyLength();

        if (phaseTwoEdgePruningTable[edgeIndex] > ply){
            phaseTwoEdgePruningTable[edgeIndex] = (byte)ply;
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            if (move == Move.D || move == Move.DP)
                continue;

            if (ply == 0 && (move == Move.R || move == Move.RP || move == Move.L || move == Move.LP || move == Move.B || move == Move.BP))
                continue;

            fto.turn(move);
            phaseTwoEdgePruningSearch(depth-1, fto);
            fto.undo();
        }

    }

    private static void generateEdgePruning(){

        for (int i = 0; i < 362880/2; i++) {
            phaseTwoEdgePruningTable[i] = 25;
        }

        FullFto[] angles = new FullFto[3*3*3];
        int anglesFound = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    angles[anglesFound] = new FullFto();

                    for (int l = 0; l < i; l++) {
                        angles[anglesFound].turn(Move.R);
                    }
                    for (int l = 0; l < j; l++) {
                        angles[anglesFound].turn(Move.L);
                    }
                    for (int l = 0; l < k; l++) {
                        angles[anglesFound].turn(Move.B);
                    }

                    anglesFound++;
                }
            }
        }

        for (int depth = 0; depth < 20; depth++) {
            System.out.println("Searching depth " + Integer.toString(depth));

            for (int i = 0; i < 3*3*3; i++) {
                angles[i].clearMoveStack();
                phaseTwoEdgePruningSearch(depth, angles[i]);
            }

            int capacity = 362880/2;
            for (int i = 0; i < 362880/2; i++) {
                if (phaseTwoEdgePruningTable[i] != 25){
                    capacity--;
                }
            }

            System.out.println("Left: " + Integer.toString(capacity));

            try {
                savePruningTable(phaseTwoEdgePruningTable, "edgeprun.dat");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static{
        try {
            phaseTwoEdgePruningTable = loadPruningTable("edgeprun.dat", 362880/2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 362880/2; i++) {
            if (phaseTwoEdgePruningTable[i] == 24){
                phaseTwoEdgePruningTable[i] = 11;
            }
        }
    }

    private static void phaseOnePruningSearch(int depth, FullFto fto){

        long hash = fto.phaseOneHash();
        Integer lookup = phaseOnePruningTable.get(hash);

        if (lookup == null || fto.historyLength() < lookup){
            phaseOnePruningTable.put(hash, fto.historyLength());
        }

        if (depth == 0){
            return;
        }

        for (Move move : Move.values()){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            phaseOnePruningSearch(depth-1, fto);
            fto.undo();
        }
    }

    private static void phaseTwoPruningSearch(int depth, FullFto fto){
        long centerHash = fto.phaseTwoCentersHash();
        long triples = fto.packPhaseTwoTripleData();
        ArrayList<PhaseTwoPruningEntry> lookup = phaseTwoPruningTable.get(centerHash);

        if (lookup == null){
            ArrayList<PhaseTwoPruningEntry> entry = new ArrayList<>();
            entry.add(new PhaseTwoPruningEntry(fto.historyLength(), triples));
            phaseTwoPruningTable.put(centerHash, entry);
        } else {
            boolean foundMatchingEntry = false;

            for (PhaseTwoPruningEntry e : lookup){
                if (e.triples == triples){
                    foundMatchingEntry = true;
                    if (e.distanceToSolved > fto.historyLength()){
                        e.distanceToSolved = fto.historyLength();
                    }
                }
            }

            if (!foundMatchingEntry){
                lookup.add(new PhaseTwoPruningEntry(fto.historyLength(), triples));
            }
        }

        if (depth == 0){
            return;
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;

            //Don't look at moves that don't break phase 2 as a first move
            if (fto.historyLength() == 0 && move != Move.U && move != Move.UP){
                continue;
            }

            fto.turn(move);
            phaseTwoPruningSearch(depth-1, fto);
            fto.undo();
        }
    }

    static{
        phaseOnePruningTable = new HashMap<Long, Integer>();
        phaseTwoPruningTable = new HashMap<Long, ArrayList<PhaseTwoPruningEntry>>();

        long startTime = System.nanoTime();

        phaseOnePruningSearch(PHASE_ONE_PRUNING_DEPTH, new FullFto());
        phaseTwoPruningSearch(PHASE_TWO_PRUNING_DEPTH, new FullFto());
//        pruningSearch(PHASE_THREE_PRUNING_DEPTH, new FullFto(), 2, PHASE_THREE_MOVES);

        long endTime = System.nanoTime();
        long duration = (endTime - startTime); // total time in nanoseconds

        System.out.println("Pruning table generation time: " + (duration / 1_000_000) + " ms");

    }


    private boolean searchPhaseOne(int depth, FullFto fto){
        if (fto.isPhaseOne()){
            System.out.print("Found Phase 1 Solution: ");
            System.out.println(fto.history());
            solution[0] = fto.history();
            return true;
        }

        if (depth == 0){
            return false;
        }

        if (depth <= PHASE_ONE_PRUNING_DEPTH){
            Integer lookup = phaseOnePruningTable.get(fto.phaseOneHash());
            if (lookup == null || lookup > depth){
                return false;
            }
        }

        for (FullFto.Move move : FullFto.Move.values()){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseOne(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    int nodes;

    private boolean searchPhaseTwo(int depth, FullFto fto){
        nodes++;
        if (fto.isPhaseTwo()){
            System.out.print("Found Phase 2 Solution: ");
            System.out.println(fto.history());
            solution[1] = fto.history();

            return true;
        }

        if (depth == 0){
            return false;
        }

        int edgeLookup = (int)(phaseTwoEdgePruningTable[fto.phaseTwoEdgeIndex()]);
        if (edgeLookup > depth) {
            return false;
        }

        int ply = fto.historyLength();

        if (depth > 7 && depth < 20 && ply > 0){
            double logOdds = BETAS[depth][0] + BETAS[depth][1] * (double)fto.triplePairCount() + BETAS[depth][2] * (float)edgeLookup;

            double odds = Math.pow(2.71828182846, logOdds);

            double p = odds/(1+odds);

//            System.out.println(depth);
//            System.out.println(1/(1+Math.pow(1.5, (double)(-depth))));

            if (p < 1/(1+Math.pow(1.6, (double)(depth-8))))
                return false;
        }

        if (depth <= PHASE_TWO_PRUNING_DEPTH) {
            ArrayList<PhaseTwoPruningEntry> lookup = phaseTwoPruningTable.get(fto.phaseTwoCentersHash());
            if (lookup == null) {
                return false;
            } else {
                boolean hashHit = false;

                for (PhaseTwoPruningEntry e : lookup) {
                    if (fto.checkPhaseTwoTripleData(e.triples) && e.distanceToSolved <= depth) {
                        hashHit = true;
                        break;
                    }
                }

                if (!hashHit) {
                    return false;
                }
            }
        }

        for (Move move : PHASE_TWO_MOVES){
            if (fto.isRepetition(move))
                continue;
//
//            if (move == Move.D || move == Move.DP)
//                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseTwo(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }

    private boolean searchPhaseThree(int depth, FullFto fto){
        if (fto.isSolved()){
            System.out.print("Found Phase 3 Solution: ");
            solution[2] = fto.history();
            System.out.println(fto.history());
            return true;
        }

        if (depth == 0){
            return false;
        }

        for (Move move : PHASE_THREE_MOVES){
            if (fto.isRepetition(move))
                continue;

            fto.turn(move);
            boolean foundSolution = searchPhaseThree(depth-1, fto);
            fto.undo();

            if (foundSolution)
                return true;
        }

        return false;
    }


    public String solution(FullFto fto){
        nodes = 0;

        fto.clearMoveStack();
        solution = new String[3];

        long startTime = System.nanoTime();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseOne(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 1 solution");
            }
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime); // total time in nanoseconds

        System.out.println("Phase 1 search time: " + (duration / 1_000_000) + " ms");

        fto.parseAlg(solution[0]);
        fto.clearMoveStack();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseTwo(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 2 solution");
            }
        }

        fto.parseAlg(solution[1]);
        fto.clearMoveStack();

        for (int depth = 0; depth < 100; depth++) {

            System.out.println("Searching depth " + Integer.toString(depth));
            boolean foundSolution = searchPhaseThree(depth, fto);

            if (foundSolution)
                break;

            if (depth == 99){
                throw new RuntimeException("Could not find FTO Phase 2 solution");
            }
        }

        System.out.print("Nodes: ");
        System.out.println(nodes);

        return solution[0] + solution[1] + solution[2];
    }

    private void write(FullFto fto, int depth, boolean label){
        System.out.print(fto.tripleCount());
        System.out.print(",");
        System.out.print(fto.triplePairCount());
        System.out.print(",");
        System.out.print((int)(phaseTwoEdgePruningTable[fto.phaseTwoEdgeIndex()]));
        System.out.print(",");
        System.out.print(depth);
        System.out.print(",");
        System.out.print(label);
        System.out.println();
    }

    public void genData(){

        Random r = new Random();

        for (int depth = 8; depth < 20; depth++) {
            for (int iter = 0; iter < 1000; iter++) {
                FullFto randomFto = new FullFto();
                FullFto closeFto = new FullFto();

                randomFto.scrambleRandomG2State(r);
                closeFto.scrambleRandomG2State(r, depth);

                write(randomFto, depth, false);
                write(closeFto, depth, true);
            }
        }

    }

    public static void main(String[] args) {
//        FullFto fto = new FullFto();
//        fto.parseAlg("R' B' D' B L D B L BR R BR D BR R' BR' R D L D B U' R L' U' BR D' BL");
//        fto.clearMoveStack();
//        FtoSearch search = new FtoSearch();
//        search.solution(fto);

         FullFto fto = new FullFto();
        //        fto.parseAlg("R' B' D' B L D B L BR R BR D BR R' BR' R D L D B U' R L' U' BR D' BL");
        //        fto.clearMoveStack();
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            fto.scrambleRandomState(r);
            FtoSearch search = new FtoSearch();
            search.solution(fto);
        }
    }
}
