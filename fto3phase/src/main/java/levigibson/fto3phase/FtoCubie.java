package levigibson.fto3phase;

import java.util.Arrays;
import java.util.Random;

import static levigibson.fto3phase.Util.*;

public class FtoCubie {

    //-------------- State --------------//

    private final int[] cornerPerm;
    private final int[] cornerOri;
    private final int[] edges;
    private final int[] triangles1;
    private final int[] triangles2;

    //Used as helper array when mutating the internal state
    //Lazily initialized when turn() is called
    private FtoCubie temps = null; //= new FtoCubie()

    //-------------- Constructors --------------//

    /**
     * Main Constructor
     * Initializes to a solved FTO
     */
    public FtoCubie(){
        cornerPerm = new int[6];
        cornerOri = new int[6];
        edges = new int[12];
        triangles1 = new int[12];
        triangles2 = new int[12];

        for (int i = 0; i < 6; i++) {
            cornerPerm[i] = i;
            cornerOri[i] = 0;
        }

        for (int i = 0; i < 12; i++) {
            edges[i] = i;
            triangles1[i] = i / 3; // centers1 = 000 111 222 333
            triangles2[i] = i / 3;
        }
    }

    /**
     * Copy Constructor
     * @param other FtoCubie to copy
     */
    public FtoCubie(FtoCubie other) {
        this.cornerPerm = other.cornerPerm.clone();
        this.cornerOri = other.cornerOri.clone();
        this.edges = other.edges.clone();
        this.triangles1 = other.triangles1.clone();
        this.triangles2 = other.triangles2.clone();
    }

    //-------------- Constants --------------//
    private static final int EDGE_PERM_SIZE = Util.fact(12) / 2;
    private static final int CORNER_ORIENTATION_SIZE = Util.pow(2,5);
    private static final int CORNER_PERMUTATION_SIZE = Util.fact(6) / 2;
    private static final int TRIANGLE_PERMUTATION_SIZE = nCr(12,3) * nCr(9,3) * nCr(6,3);


    //-------------- Handy Public Stuff --------------//

    /**
     * Generates an FTO in a random state
     * This FTO can be passed directly to Search::solution for a random state scramble!
     * @param r PRNG to use
     * @return random state FTO
     */
    public static FtoCubie randomCube(Random r){
        FtoCubie fto = new FtoCubie();
        fto.setAllEdges(r.nextInt(EDGE_PERM_SIZE)); // 12! / 2
        fto.setAllCornerOrientation(r.nextInt(CORNER_ORIENTATION_SIZE)); // 2 ^ 5
        fto.setAllCornerPermutation(r.nextInt(CORNER_PERMUTATION_SIZE)); // 6! / 2
        fto.setAllTriangles(r.nextInt(TRIANGLE_PERMUTATION_SIZE), 0); // C(12,3) * C(9,3) * C(6,3)
        fto.setAllTriangles(r.nextInt(TRIANGLE_PERMUTATION_SIZE), 1); // C(12,3) * C(9,3) * C(6,3)
        return fto;
    }

    /**
     * Copy the state from this FTO into another FTO
     * @param c other FTO
     */
    public void copyInto(FtoCubie c) {
        System.arraycopy(this.cornerPerm, 0, c.cornerPerm, 0, 6);
        System.arraycopy(this.cornerOri, 0, c.cornerOri, 0, 6);
        System.arraycopy(this.edges, 0, c.edges, 0, 12);
        System.arraycopy(this.triangles1, 0, c.triangles1, 0, 12);
        System.arraycopy(this.triangles2, 0, c.triangles2, 0, 12);
    }

    /**
     * Apply moves to current FTO and write the output to given output FTO
     * @param moves sequence of moves to apply
     * @param out output buffer
     */
    void applyMovesInto(int[] moves, FtoCubie out){
        this.copyInto(out);
        for (int move : moves) {
            out.turn(move);
        }
    }

    //-------------- Move & Cubie Definitions --------------//

    //Moves
    public static final int
        R = 0, RP = 1, L = 2, LP = 3,
        B = 4, BP = 5, U = 6, UP = 7,
        D = 8, DP = 9, F = 10, FP = 11,
        BR = 12, BRP = 13, BL = 14, BLP = 15;

    //Triangle Ordinals
    //Represents the colors of the triangles
    public static final int
        TOU = 0, TOF = 1, TOBR = 2, TOBL = 3,
        TOR = 0, TOL = 1, TOB = 2, TOD = 3;

    //Triangle Indices
    //Represents the index of a specific triangle
    public static final int
        TIUBL = 0, TIUBR = 1, TIUF = 2, TIFU = 3,
        TIFBR = 4, TIFBL = 5, TIBRU = 6, TIBRBL = 7,
        TIBRF = 8, TIBLU = 9, TIBLF = 10, TIBLBR = 11,
        TIRL = 0,  TIRB = 1,  TIRD = 2,  TILB = 3,
        TILR = 4,  TILD = 5,  TIBR = 6,  TIBL = 7,
        TIBD = 8,  TIDR = 9,  TIDL = 10, TIDB = 11;

    //Edges
    public static final int
        EUB = 0, EUR = 1, EUL = 2, EFL = 3,
        EFR = 4, ERBR = 5, EBRB = 6, EBLB = 7,
        ELBL = 8, EDF = 9, EDBR = 10, EDBL = 11;

    //Corners
    public static final int
        CUF = 0, CUBR = 1, CUBL = 2,
        CDL = 3, CDR = 4, CDB = 5;

    //-------------- Lehmer Indexing Functions --------------//

    /**
     * Packs all edges into a compact index using the Lehmer Index Algorithm
     * This index has a bijection with possible edge states
     * Index Range:
     * [0, (12!/2) - 1]
     * [0, 239500799]
     * @return compact index
     */
    public int packAllEdges(){
        return packPerm(edges, true);
    }

    /**
     * Set all edges from a compact index generated by {@link  FtoCubie#packAllEdges() packAllEdges}
     * Index Range:
     * [0, (12!/2) - 1]
     * [0, 239500799]
     * @param idx compact index
     */
    public void setAllEdges(int idx){
        if (idx < 0 || idx >= 239500800) throw new IllegalArgumentException("Index " + idx + " out of range");
        unpackPerm(edges, idx, true);
    }

    /**
     * Packs the permutation of the corners into a compact index using the Lehmer Index Algorithm.
     * This does not store the orientation. That is done separately. See {@link  FtoCubie#packAllCornerOrientation() packAllCornerOrientation}
     * This index has a bijection with possible corner permutation states
     * Index Range:
     * [0, (6!/2) - 1]
     * [0, 359]
     * @return compact index
     */
    public int packAllCornerPermutation(){
        return packPerm(cornerPerm, true);
    }

    /**
     * Set the corner permutation from a compact index generated by {@link  FtoCubie#packAllCornerPermutation() packAllCornerPermutation}
     * Index Range:
     * [0, (12!/2) - 1]
     * [0, 239500799]
     * @param idx compact index
     */
    public void setAllCornerPermutation(int idx){
        if (idx < 0 || idx >= 360) throw new IllegalArgumentException("Index " + idx + " out of range");
        unpackPerm(cornerPerm, idx, true);
    }

    public int packAllCornerOrientation(){
        int index = 0;
        for (int i = 0; i < 5; i++) {
            index |= cornerOri[i] << i;
        }
        return index;
    }

    public void setAllCornerOrientation(int idx){
        //2^5
        if (idx < 0 || idx >= 32) throw new IllegalArgumentException("Index " + idx + " out of range");
        for (int i = 0; i < 5; i++) {
            cornerOri[i] = (idx >> i) & 1;
        }
        cornerOri[5] = Integer.bitCount(idx) % 2;
    }

    public int packAllTriangles(int orbit){
        if (orbit != 0 && orbit != 1)
            throw new IllegalArgumentException("Orbit must be 0 or 1");

        int[] triangles = orbit == 1 ? triangles2 : triangles1;

        boolean[] used = new boolean[12];
        int[][] loc = new int[3][3];

        for (int xo = 0; xo < 3; xo++) {
            int found = 0;
            int passed = 0;
            for (int i = 0; i < 12; i++) {
                if (triangles[i] == xo){
                    loc[xo][found++] = passed;
                    used[i] = true;
                    passed++;
                } else if (!used[i]){
                    passed++;
                }
            }

            if (found != 3)
                throw new IllegalStateException("Expected found=3. Instead, found=" + found);
        }

        return (packSubset(loc[0]) * nCr(9, 3) * nCr(6, 3)) +
            packSubset(loc[1]) * nCr(6, 3) +
            packSubset(loc[2]);
    }

    public void setAllTriangles(int idx, int orbit){
        //C(12,3) * C(9,3) * C(6,3) * C(3,3)
        if (idx < 0 || idx >= 369600) throw new IllegalArgumentException("Index " + idx + " out of range");

        if (orbit != 0 && orbit != 1)
            throw new IllegalArgumentException("Orbit must be 0 or 1");

        int[] triangles = orbit == 1 ? triangles2 : triangles1;

        final int[] coefficients = {
            nCr(9, 3) * nCr(6, 3),
            nCr(6, 3),
            1
        };

        int[][] loc = new int[3][3];

        int remaining = idx;
        for (int color = 0; color < 3; color++) {
            int coefficient = coefficients[color];
            int digit = remaining / coefficient;

            unpackSubset(loc[color], digit);

            remaining -= coefficient * digit;
        }

        Arrays.fill(triangles, 3);

        for (int color = 0; color < 3; color++) {
            int nz = 0;
            int li = 0;
            for (int i = 0; i < 12; i++) {
                if (triangles[i] < color) continue;
                if (li < 3 && nz == loc[color][li]) {
                    triangles[i] = color;
                    li++;
                }
                nz++;
            }
        }
    }

    public int g1PackEdges() {
        int[] loc = new int[3];
        int[] perm = new int[3];
        int count = 0;

        for (int i = 0; i < 12; i++) {
            if (edges[i] >= EDF) {
                loc[count] = i;
                perm[count] = edges[i] - EDF;
                count++;
            }
        }

        if (count != 3) {
            throw new IllegalStateException("Expected 3 D-face edges. This is not a possible FTO state.");
        }

        int parity = isParity(perm) ? 1 : 0;

        return packSubset(loc) * 2 + parity;
    }

    public void g1SetEdges(int idx){
        //C(12,3) * 2
        if (idx < 0 || idx >= 440) throw new IllegalArgumentException("Index " + idx + " out of range");

        int locIdx = idx / 2;
        int parity = idx % 2;

        int[] loc = new int[3];
        int[] perm = {EDF, EDBR, EDBL};
        if (parity == 1) swap(perm, 1, 2);

        //Unpack loc
        unpackSubset(loc, locIdx);

        //set the edges
        Arrays.fill(edges, -1);
        for (int i = 0; i < 3; i++) {
            edges[loc[i]] = perm[i];
        }
    }

    public int g1PackTriangles(){
        int[] idx = new int[3];
        int count = 0;

        for (int i = 0; i < 12; i++) {
            if (triangles2[i] == TOD) idx[count++] = i;
        }

        if (count != 3) throw new IllegalStateException("Less than 3 d-layer triangles");

        return packSubset(idx);
    }

    public void g1SetTriangles(int idx){
        //C(12,3)
        if (idx < 0 || idx >= 220) throw new IllegalArgumentException("Index " + idx + " out of range");

        //Unpack index
        int[] loc = new int[3];
        unpackSubset(loc, idx);

        //Set the relevant centers
        for (int i = 0; i < 12; i++) {
            triangles2[i] = -1;
        }
        for (int i = 0; i < 3; i++) {
            triangles2[loc[i]] = TOD;
        }
    }

    private static final int[] G2_EDGE_COLORS = {TOB, TOR, TOL, TOL, TOR, TOR, TOB, TOB, TOL};
    private static final int[] G2_EDGE_NORM = {0, 0, 0, 1, 1, 2, 1, 2, 2};
    private static final int[][] G2_EDGE_NORM_INV = {
        {EUR, EFR, ERBR},
        {EUL, EFL, ELBL},
        {EUB, EBRB, EBLB},
    };

    //TODO add comments
    public int g2PackEdges(){
        int used = 0;
        int loc = 0;
        int perm = 0;
        int parity = 0;

        for (int xo = 0; xo < 2; xo++) {
            int found = 0;
            int passed = 0;
            for (int i = 0; i < 9; i++) {
                if (edges[i] == EDF || edges[i] == EDBL || edges[i] == EDBR){
                    throw new IllegalStateException("Edges not in phase 1");
                }

                if (G2_EDGE_COLORS[edges[i]] == xo){
                    perm |= G2_EDGE_NORM[edges[i]] << (12 * xo + 4 * found);
                    loc |= passed << (12 * xo + 4 * found++);
                    used |= 1 << i;
                    passed++;
                } else if ((used >> i & 1) != 1){
                    passed++;
                }
            }

            if (found != 3)
                throw new IllegalStateException("Expected found=3. Instead, found=" + found);
        }

        for (int i = 0; i < 2; i++) {
            parity |= (isParity((perm >> 12 * i) & 0b111111111111, 3) ? 1 : 0) << i;
        }

        int subsetIndex = packSubset(loc >> 12, 3) +
            packSubset(loc & 0b111111111111, 3) * nCr(6, 3);

        return (subsetIndex * 4) + parity;
    }

    public void g2SetEdges(int idx){
        //9!/2
        if (idx < 0 || idx >= 181440) throw new IllegalArgumentException("Index " + idx + " out of range");

        int subsetIndex = idx/4;
        int parityIndex = idx % 4;

        int[][] loc = new int[2][3];
        int[][] perm = new int[2][3];
        int[] parity = new int[2];

        parity[0] = parityIndex & 1;
        parity[1] = (parityIndex >> 1) & 1;

        unpackSubset(loc[0], subsetIndex / nCr(6, 3));
        unpackSubset(loc[1], subsetIndex % nCr(6, 3));

        for (int i = 0; i < 2; i++) {
            System.arraycopy(G2_EDGE_NORM_INV[i], 0, perm[i], 0, 3);
            if (parity[i] == 1){
                swap(perm[i], 1, 2);
            }
        }

        //Set edges to blank slate with G1 solved
        Arrays.fill(edges, -1);
        edges[EDF] = EDF;
        edges[EDBL] = EDBL;
        edges[EDBR] = EDBR;

        for (int i = 0; i < 3; i++) {
            edges[loc[0][i]] = perm[0][i];
        }

        int nz = 0;
        int li = 0;
        for (int i = 0; i < 9; i++) {
            if (edges[i] != -1) continue;
            if (li < 3 && nz == loc[1][li]) {
                edges[i] = perm[1][li];
                li++;
            }
            nz++;
        }

        int[] bloc = new int[3];
        li = 0;
        for (int i = 0; i < 9; i++) {
            if (edges[i] == -1) {
                bloc[li] = i;
                edges[i] = G2_EDGE_NORM_INV[2][li++];
            }
        }

        if (isParity(edges)){
            swap(edges, bloc[0], bloc[1]);
        }
    }

    public int g2PackTris(){
        for (int i = 9; i < 12; i++) {
            if (triangles2[i] != TOD)
                throw new IllegalStateException("Tris must be in phase 1");
        }

        int used = 0;
        int loc = 0;

        for (int xo = 0; xo < 2; xo++) {
            int found = 0;
            int passed = 0;
            for (int i = 0; i < 9; i++) {
                if (triangles2[i] == xo){
                    loc |= passed << (xo * 12 + found++ * 4);
                    used |= 1 << i;
                    passed++;
                } else if (((used >> i) & 1) == 0){
                    passed++;
                }
            }

            if (found != 3)
                throw new IllegalStateException("Expected found=3. Instead, found=" + found);
        }
        return packSubset(loc >> 12, 3) +
            packSubset(loc & 0b111111111111, 3) * nCr(6, 3);
    }

    public void g2SetTriangles(int idx){
        //C(9,3) * C(6,3) * C(3,3)
        if (idx < 0 || idx >= 1680) throw new IllegalArgumentException("Index " + idx + " out of range");

        int[] loc0 = new int[3];
        int[] loc1 = new int[3];
        unpackSubset(loc0, idx / nCr(6, 3));
        unpackSubset(loc1, idx % nCr(6, 3));

        Arrays.fill(triangles2, 0, 9, 2);
        for (int v : loc0) triangles2[v] = 0;

        int nz = 0;
        int li = 0;
        for (int i = 0; i < 9; i++) {
            if (triangles2[i] == 0) continue;
            if (li < 3 && nz == loc1[li]) {
                triangles2[i] = 1;
                li++;
            }
            nz++;
        }
        for (int i = 9; i < 12; i++) triangles2[i] = TOD;
    }

    public int g2PackTriples(int color){
        if (color > 3 || color < 0){
            throw new IllegalArgumentException("color must be U, F, BR, or BL");
        }

        return (160 * g2PackTripleTris(color)) + g2PackTripleCorners(color);
    }

    public void g2SetTriples(int idx, int color){
        //C(12,3) * C(6,3) * 2^3
        if (idx < 0 || idx >= 35200) throw new IllegalArgumentException("Index " + idx + " out of range");

        int tris = idx / 160;
        int corners = idx % 160;

        g2SetTripleTris(tris, color);
        g2SetTripleCorners(corners, color);
    }

    private static final int[][] CORNER_PARITY = {
        {0,0,0,-1,-1,-1},
        {1,-1,-1,0,1,-1},
        {-1,1,-1,-1,0,1},
        {-1,-1,1,1,-1,0}};

    //[0, 159]
    private int g2PackTripleCorners(int color){
        int idx = 0;
        int orientation = 0;

        int found = 0;
        for (int i = 0; i < 6; i++) {
            int corner = cornerPerm[i];
            int ori = cornerOri[i];

            if (corner == -1) continue;

            int parity = CORNER_PARITY[color][corner];

            if (parity != -1){
                orientation |= (parity ^ ori) << found;
                idx |= i << (4 * found++);
            }
        }

        if (found != 3){
            throw new IllegalStateException("Expected 3 matching corners");
        }

        assert (orientation < 8);

        return packSubset(idx, 3) * 8 + orientation;
    }

    private int g2PackTripleTris(int color){
        int idx = 0;

        int found = 0;
        for (int i = 0; i < 12; i++) {
            if (triangles1[i] == color){
                idx |= i << (4 * found++);
            }
        }

        return packSubset(idx,3);
    }

    private void g2SetTripleTris(int idx, int color){
        //C(12,3)
        if (idx < 0 || idx >= 220) throw new IllegalArgumentException("Index " + idx + " out of range");

        int[] loc = new int[3];
        unpackSubset(loc, idx);
        Arrays.fill(triangles1, -1);

        for (int i = 0; i < 3; i++) {
            triangles1[loc[i]] = color;
        }
    }

    private void g2SetTripleCorners(int idx, int color){
        //C(12,3) * 2^3
        if (idx < 0 || idx >= 160) throw new IllegalArgumentException("Index " + idx + " out of range");

        int[] loc = new int[3];
        unpackSubset(loc, idx / 8);

        int orientation = idx % 8;

        int[] relevantCorners = new int[3];
        int found = 0;
        for (int i = 0; i < 6; i++) {
            if (CORNER_PARITY[color][i] != -1){
                relevantCorners[found++] = i;
            }
        }

        Arrays.fill(cornerPerm, -1);
        Arrays.fill(cornerOri, -1);

        for (int i = 0; i < 3; i++) {
            int perm = loc[i];
            int ori = (orientation >> i) & 1;

            cornerPerm[perm] = relevantCorners[i];
            cornerOri[perm] = ori ^ CORNER_PARITY[color][relevantCorners[i]];
        }

    }

    public int g3PackCorners(){
        return (packAllCornerPermutation() * 32) + packAllCornerOrientation();
    }

    public void g3SetCorners(int idx){
        //(6!/2) * 2^5
        if (idx < 0 || idx >= 11520) throw new IllegalArgumentException("Index " + idx + " out of range");

        setAllCornerPermutation(idx/32);
        setAllCornerOrientation(idx%32);
    }

    public int g3PackEdges(){
        int[] loc = new int[4];
        Arrays.fill(loc, -1);

        for (int axis = 0; axis < 4; axis++) {
            for (int i = 0; i < 3; i++) {
                if (G3_EDGES[axis][i] == edges[G3_EDGES[axis][0]]){
                    loc[axis] = i;
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (loc[i] == -1)
                throw new IllegalStateException("Cound not find G3 edges");
        }

        return (27 * loc[0]) + (9 * loc[1]) + (3 * loc[2]) + (loc[3]);
    }

    public void g3SetEdges(int idx){
        //3 ^ 4
        if (idx < 0 || idx >= 81) throw new IllegalArgumentException("Index " + idx + " out of range");

        for (int i = 0; i < 12; i++) {
            edges[i] = i;
        }

        int remaining = idx;
        for (int axis = 0; axis < 4; axis++) {
            int coefficient = pow(3, 3-axis);
            int digit = remaining / coefficient;

            while (edges[G3_EDGES[axis][0]] != G3_EDGES[axis][digit]){
                for (int i = 0; i < 2; i++) {
                    swap(edges, G3_EDGES[axis][0], G3_EDGES[axis][i + 1]);
                }
            }

            remaining -= coefficient * digit;
        }

    }


    public boolean isSolved(){
        for (int i = 0; i < 6; i++) {
            if (cornerOri[i] != 0)
                return false;
            if (cornerPerm[i] != i)
                return false;
        }

        for (int i = 0; i < 12; i++) {
            if (triangles1[i] != i / 3)
                return false;
            if (triangles2[i] != i / 3)
                return false;
            if (edges[i] != i){
                return false;
            }
        }

        return true;
    }

    /**
     * Turn the FTO! Mutates the internal state.
     * @param move the move to apply
     */
    public void turn(int move){
        if (temps == null) temps = new FtoCubie();
        turnInto(move, temps);
        temps.copyInto(this);
    }

    /**
     * Turn the FTO! Writes result to out without mutating this.
     * @param move the move to apply
     * @param out output of the move
     */
    void turnInto(int move, FtoCubie out){
        if (out == this) throw new IllegalArgumentException("out can not be this");

        MoveEffect cycles = moveEffects[move];

        for (int i = 0; i < 6; i++) {
            out.cornerPerm[i] = this.cornerPerm[cycles.cp[i]];
            out.cornerOri[i] = this.cornerOri[cycles.cp[i]] ^ cycles.co[i];
        }

        for (int i = 0; i < 12; i++) {
            out.edges[i] = this.edges[cycles.ep[i]];
            out.triangles1[i] = this.triangles1[cycles.xp1[i]];
            out.triangles2[i] = this.triangles2[cycles.xp2[i]];
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        FtoCubie fto = (FtoCubie) obj;

        return Arrays.equals(fto.edges, this.edges) &&
            Arrays.equals(fto.triangles1, this.triangles1) &&
            Arrays.equals(fto.triangles2, this.triangles2) &&
            Arrays.equals(fto.cornerOri, this.cornerOri) &&
            Arrays.equals(fto.cornerPerm, this.cornerPerm);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.edges) ^
            Arrays.hashCode(this.triangles1) ^
            Arrays.hashCode(this.triangles2) ^
            Arrays.hashCode(this.cornerOri) ^
            Arrays.hashCode(this.cornerPerm);
    }

    public int[] getCornerPerm(){ return cornerPerm.clone(); }
    public int[] getCornerOri(){ return cornerOri.clone(); }
    public int[] getEdges(){ return edges.clone(); }
    public int[] getTriangles1(){ return triangles1.clone(); }
    public int[] getTriangles2(){ return triangles2.clone(); }

    private static class MoveEffect{
        public int[] co;
        public int[] cp;
        public int[] xp1;
        public int[] xp2;
        public int[] ep;
    }

    //EUB = 0, EUR = 1, EUL
    private static final int[][] G3_EDGES = {
        {EUB, EBRB, EBLB},
        {EUR, EFR, ERBR},
        {EUL, ELBL, EFL},
        {EDF, EDBR, EDBL}
    };

    private static final MoveEffect[] moveEffects;

    //Hard-coded data about what cycles the different moves make
    static {
        moveEffects = new MoveEffect[16];
        for (int i = 0; i < 16; i++) { moveEffects[i] = new MoveEffect(); }

        //R
        moveEffects[R].cp = new int[]{CDR, CUF, CUBL, CDL, CUBR, CDB};
        moveEffects[R].co = new int[]{1, 1, 0, 0, 0, 0};
        moveEffects[R].ep = new int[]{EUB, EFR, EUL, EFL, ERBR, EUR, EBRB, EBLB, ELBL, EDF, EDBR, EDBL};
        moveEffects[R].xp1 = new int[]{TIUBL, TIFU, TIFBR, TIBRF, TIBRU, TIFBL, TIUF, TIBRBL, TIUBR, TIBLU, TIBLF, TIBLBR};
        moveEffects[R].xp2 = new int[]{TIRD, TIRL, TIRB, TILB, TILR, TILD, TIBR, TIBL, TIBD, TIDR, TIDL, TIDB};
        //L
        moveEffects[L].cp = new int[]{CUBL,  CUBR, CDL, CUF, CDR, CDB};
        moveEffects[L].co = new int[]{1, 0, 1, 0, 0, 0};
        moveEffects[L].ep = new int[]{EUB, EUR, ELBL, EUL, EFR, ERBR, EBRB, EBLB, EFL, EDF, EDBR, EDBL};
        moveEffects[L].xp1 = new int[]{TIBLF, TIUBR, TIBLU, TIUBL, TIFBR, TIUF, TIBRU, TIBRBL, TIBRF, TIFBL, TIFU, TIBLBR};
        moveEffects[L].xp2 = new int[]{TIRL, TIRB, TIRD, TILD, TILB, TILR, TIBR, TIBL, TIBD, TIDR, TIDL, TIDB};
        //B
        moveEffects[B].cp = new int[]{CUF, CDB, CUBR, CDL, CDR, CUBL};
        moveEffects[B].co = new int[]{0, 1, 1, 0, 0, 0};
        moveEffects[B].ep = new int[]{EBRB, EUR, EUL, EFL, EFR, ERBR, EBLB, EUB, ELBL, EDF, EDBR, EDBL};
        moveEffects[B].xp1 = new int[]{TIBRU, TIBRBL, TIUF, TIFU, TIFBR, TIFBL, TIBLBR, TIBLU, TIBRF, TIUBR, TIBLF, TIUBL};
        moveEffects[B].xp2 = new int[]{TIRL, TIRB, TIRD, TILB, TILR, TILD, TIBD, TIBR, TIBL, TIDR, TIDL, TIDB};
        //D
        moveEffects[D].cp = new int[]{CUF, CUBR, CUBL, CDB, CDL, CDR};
        moveEffects[D].co = new int[]{0, 0, 0, 0, 0, 0};
        moveEffects[D].ep = new int[]{EUB, EUR, EUL, EFL, EFR, ERBR, EBRB, EBLB, ELBL, EDBL, EDF, EDBR};
        moveEffects[D].xp1 = new int[]{TIUBL, TIUBR, TIUF, TIFU, TIBLF, TIBLBR, TIBRU, TIFBR, TIFBL, TIBLU, TIBRBL, TIBRF};
        moveEffects[D].xp2 = new int[]{TIRL, TIRB, TIRD, TILB, TILR, TILD, TIBR, TIBL, TIBD, TIDL, TIDB, TIDR};
        //U
        moveEffects[U].cp = new int[]{CUBR, CUBL, CUF, CDL, CDR, CDB};
        moveEffects[U].co = new int[]{0, 0, 0, 0, 0, 0};
        moveEffects[U].ep = new int[]{EUL, EUB, EUR, EFL, EFR, ERBR, EBRB, EBLB, ELBL, EDF, EDBR, EDBL};
        moveEffects[U].xp1 = new int[]{TIUF, TIUBL, TIUBR, TIFU, TIFBR, TIFBL, TIBRU, TIBRBL, TIBRF, TIBLU, TIBLF, TIBLBR};
        moveEffects[U].xp2 = new int[]{TIBR, TIBL, TIRD, TIRL, TIRB, TILD, TILB, TILR, TIBD, TIDR, TIDL, TIDB};
        //F
        moveEffects[F].cp = new int[]{CDL, CUBR, CUBL, CDR, CUF, CDB};
        moveEffects[F].co = new int[]{1, 0, 0, 1, 0, 0};
        moveEffects[F].ep = new int[]{EUB, EUR, EUL, EDF, EFL, ERBR, EBRB, EBLB, ELBL, EFR, EDBR, EDBL};
        moveEffects[F].xp1 = new int[]{TIUBL, TIUBR, TIUF, TIFBL, TIFU, TIFBR, TIBRU, TIBRBL, TIBRF, TIBLU, TIBLF, TIBLBR};
        moveEffects[F].xp2 = new int[]{TILD, TIRB, TILR, TILB, TIDL, TIDR, TIBR, TIBL, TIBD, TIRL, TIRD, TIDB};
        //BR
        moveEffects[BR].cp = new int[]{CUF, CDR, CUBL, CDL, CDB, CUBR};
        moveEffects[BR].co = new int[]{0, 1, 0, 0, 1, 0};
        moveEffects[BR].ep = new int[]{EUB, EUR, EUL, EFL, EFR, EDBR, ERBR, EBLB, ELBL, EDF, EBRB, EDBL};
        moveEffects[BR].xp1 = new int[]{TIUBL, TIUBR, TIUF, TIFU, TIFBR, TIFBL, TIBRF, TIBRU, TIBRBL, TIBLU, TIBLF, TIBLBR};
        moveEffects[BR].xp2 = new int[]{TIRL, TIDR, TIDB, TILB, TILR, TILD, TIRD, TIBL, TIRB, TIBD, TIDL, TIBR};
        //BL
        moveEffects[BL].cp = new int[]{CUF, CUBR, CDB, CUBL, CDR, CDL};
        moveEffects[BL].co = new int[]{0, 0, 1, 0, 0, 1};
        moveEffects[BL].ep = new int[]{EUB, EUR, EUL, EFL, EFR, ERBR, EBRB, EDBL, EBLB, EDF, EDBR, ELBL};
        moveEffects[BL].xp1 = new int[]{TIUBL, TIUBR, TIUF, TIFU, TIFBR, TIFBL, TIBRU, TIBRBL, TIBRF, TIBLBR, TIBLU, TIBLF};
        moveEffects[BL].xp2 = new int[]{TIRL, TIRB, TIRD, TIBD, TILR, TIBL, TIBR, TIDB, TIDL, TIDR, TILB, TILD};

        //No need to hard code the inverse moves
        //You can just invert the cycles
        for (int move = 1; move < 16; move += 2) {
            MoveEffect inverse = moveEffects[move];
            MoveEffect normal = moveEffects[move-1];

            inverse.co = new int[6];
            inverse.cp = new int[6];
            inverse.ep = new int[12];
            inverse.xp1 = new int[12];
            inverse.xp2 = new int[12];

            for (int i = 0; i < 6; i++) {
                inverse.cp[normal.cp[i]] = i;
            }
            for (int i = 0; i < 6; i++) {
                inverse.co[i] = normal.co[inverse.cp[i]];
            }
            for (int i = 0; i < 12; i++) {
                inverse.ep[normal.ep[i]] = i;
                inverse.xp1[normal.xp1[i]] = i;
                inverse.xp2[normal.xp2[i]] = i;
            }
        }
    }


}
