package levigibson.fto3phase;


import java.util.Arrays;
import java.util.Random;

import static levigibson.fto3phase.Util.*;

public class FtoCubie {

    private int[] cornerPerm;
    private int[] cornerOri;
    private int[] edges;
    private int[] triangles1;
    private int[] triangles2;

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

    public FtoCubie(FtoCubie other) {
        this.cornerPerm = other.cornerPerm.clone();
        this.cornerOri = other.cornerOri.clone();
        this.edges = other.edges.clone();
        this.triangles1 = other.triangles1.clone();
        this.triangles2 = other.triangles2.clone();
    }

    public static FtoCubie randomCube(Random r){
        FtoCubie fto = new FtoCubie();
        fto.setAllEdges(r.nextInt(239500800)); //8481 12! / 2
        fto.setAllCornerOrientation(r.nextInt(32)); // 2 ^ 5
        fto.setAllCornerPermutation(r.nextInt(360)); // 6! / 2
        fto.setAllTriangles(r.nextInt(369600), 0); // C(12,3) * C(9,3) * C(6,3)
        fto.setAllTriangles(r.nextInt(369600), 1); // C(12,3) * C(9,3) * C(6,3)
        return fto;
    }

    public FtoCubie applyMoves(int[] moves){
        FtoCubie[] ftos = new FtoCubie[2];
        ftos[0] = new FtoCubie(this);
        ftos[1] = new FtoCubie();

        FtoCubie source;
        FtoCubie target = ftos[1]; // makes the compiler happy

        for (int i = 0; i < moves.length; i++) {
            source = ftos[i % 2];
            target = ftos[(i+1) % 2];

            source.turn(moves[i], target);
        }

        return target;
    }

    //[0, 12!/2 - 1]
    public int packAllEdges(){
        return packPerm(edges, true);
    }

    public void setAllEdges(int idx){
        unpackPerm(edges, idx, true);
    }

    public int packAllCornerPermutation(){
        return packPerm(cornerPerm, true);
    }

    public void setAllCornerPermutation(int idx){
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

    public int packG1Edges() {
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

    public void setG1Edges(int idx){
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

    public int packG1Triangles(){
        int[] idx = new int[3];
        int count = 0;

        for (int i = 0; i < 12; i++) {
            if (triangles2[i] == XD) idx[count++] = i;
        }

        if (count != 3) throw new IllegalStateException("Less than 3 d-layer triangles");

        return packSubset(idx);
    }

    public void setG1Triangles(int idx){
        //Unpack index
        int[] loc = new int[3];
        unpackSubset(loc, idx);

        //Set the relevant centers
        for (int i = 0; i < 12; i++) {
            triangles2[i] = -1;
        }
        for (int i = 0; i < 3; i++) {
            triangles2[loc[i]] = XD;
        }
    }

    private final int[] G2_EDGE_COLORS = {XB, XR, XL, XL, XR, XR, XB, XB, XL};
    private final int[] G2_EDGE_NORM = {0, 0, 0, 1, 1, 2, 1, 2, 2};
    private final int[][] G2_EDGE_NORM_INV = {
        {EUR, EFR, ERBR},
        {EUL, EFL, ELBL},
        {EUB, EBRB, EBLB},
    };

    public int packG2Edges(){
        boolean[] used = new boolean[9];
        int[][] loc = new int[2][3];
        int[][] perm = new int[2][3];
        int[] parity = new int[2];

        for (int xo = 0; xo < 2; xo++) {
            int found = 0;
            int passed = 0;
            for (int i = 0; i < 9; i++) {
                if (edges[i] == EDF || edges[i] == EDBL || edges[i] == EDBR){
                    throw new IllegalStateException("Edges not in phase 1");
                }

                if (G2_EDGE_COLORS[edges[i]] == xo){
                    perm[xo][found] = G2_EDGE_NORM[edges[i]];
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

        for (int i = 0; i < 2; i++) {
            parity[i] = isParity(perm[i]) ? 1 : 0;
        }

        int subsetIndex = packSubset(loc[1]) +
            packSubset(loc[0]) * nCr(6, 3);

        int parityIndex = parity[1] * 2 + parity[0];

        return (subsetIndex * 4) + parityIndex;
    }

    public void setG2Edges(int idx){
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
            for (int j = 0; j < 3; j++) {
                perm[i][j] = G2_EDGE_NORM_INV[i][j];
            }
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

    public int packG2Tris(){
        for (int i = 9; i < 12; i++) {
            if (triangles2[i] != XD)
                throw new IllegalStateException("Tris must be in phase 1");
        }

        boolean[] used = new boolean[9];
        int[][] loc = new int[2][3];

        for (int xo = 0; xo < 2; xo++) {
            int found = 0;
            int passed = 0;
            for (int i = 0; i < 9; i++) {
                if (triangles2[i] == xo){
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
        return packSubset(loc[1]) +
            packSubset(loc[0]) * nCr(6, 3);
    }

    public void setG2Triangles(int idx){
        int[] loc0 = new int[3];
        int[] loc1 = new int[3];
        unpackSubset(loc0, idx / nCr(6, 3));
        unpackSubset(loc1, idx % nCr(6, 3));
        Arrays.sort(loc1);

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
        for (int i = 9; i < 12; i++) triangles2[i] = XD;
    }

    private static int[][] CORNER_PARITY = {
        {0,0,0,-1,-1,-1},
        {1,-1,-1,0,1,-1},
        {-1,1,-1,-1,0,1},
        {-1,-1,1,1,-1,0}};

    //[0, 159]
    private int packG2TripleCorners(int color){
        int[] idx = new int[3];
        int orientation = 0;

        int found = 0;
        for (int i = 0; i < 6; i++) {
            int corner = cornerPerm[i];
            int ori = cornerOri[i];

            if (corner == -1) continue;

            int parity = CORNER_PARITY[color][corner];

            if (parity != -1){
                orientation |= (parity ^ ori) << found;
                idx[found++] = i;
            }
        }

        if (found != 3){
            throw new IllegalStateException("Expected 3 matching corners");
        }

        assert (orientation < 8);

        return packSubset(idx) * 8 + orientation;
    }

    private int packG2TripleTris(int color){
        int[] idx = new int[3];

        int found = 0;
        for (int i = 0; i < 12; i++) {
            if (triangles1[i] == color){
                idx[found++] = i;
            }
        }

        return packSubset(idx);
    }

    public int packG2Triples(int color){
        if (color > 3 || color < 0){
            throw new IllegalArgumentException("color must be U, F, BR, or BL");
        }

        return (160 * packG2TripleTris(color)) + packG2TripleCorners(color);
    }

    private void setG2TripleTris(int idx, int color){
        int[] loc = new int[3];
        unpackSubset(loc, idx);
        Arrays.fill(triangles1, -1);

        for (int i = 0; i < 3; i++) {
            triangles1[loc[i]] = color;
        }
    }

    private void setG2TripleCorners(int idx, int color){
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

    public void setG2Triples(int idx, int color){
        int tris = idx / 160;
        int corners = idx % 160;

        setG2TripleTris(tris, color);
        setG2TripleCorners(corners, color);
    }

    public int packG3Corners(){
        return (packAllCornerPermutation() * 32) + packAllCornerOrientation();
    }

    public void setG3Corners(int idx){
        setAllCornerPermutation(idx/32);
        setAllCornerOrientation(idx%32);
    }

    public int packG3Edges(){
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

    public void setG3Edges(int idx){
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
     * Turn the FTO!
     * This function does not mutate the internal state.
     * It takes the internal state, turns it, then assigns
     * the internal state of "out" to the result
     * @param move the move to apply
     * @param out output of the move
     */
    public void turn(int move, FtoCubie out){
        if (out == this) throw new IllegalArgumentException("out can not be this");

        MoveEffect cycles = moveEffects[move];

        //Corners
        for (int i = 0; i < 6; i++) {
            out.cornerPerm[i] = this.cornerPerm[cycles.cp[i]];
            out.cornerOri[i] = this.cornerOri[cycles.cp[i]] ^ cycles.co[i];
        }

        //Edges + Triangles
        for (int i = 0; i < 12; i++) {
            out.edges[i] = this.edges[cycles.ep[i]];
            out.triangles1[i] = this.triangles1[cycles.xp1[i]];
            out.triangles2[i] = this.triangles2[cycles.xp2[i]];
        }
    }

    public FtoCubie turn(int move){
        FtoCubie fto = new FtoCubie();
        this.turn(move, fto);
        return fto;
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

    //Moves
    public static final int
        R = 0, RP = 1, L = 2, LP = 3,
        B = 4, BP = 5, U = 6, UP = 7,
        D = 8, DP = 9, F = 10, FP = 11,
        BR = 12, BRP = 13, BL = 14, BLP = 15;

    //Center Ordinals
    public static final int
        XU = 0, XF = 1, XBR = 2, XBL = 3,
        XR = 0, XL = 1, XB = 2, XD = 3;

    //Center Indices
    public static final int
        XIUBL = 0, XIUBR = 1, XIUF = 2, XIFU = 3,
        XIFBR = 4, XIFBL = 5, XIBRU = 6, XIBRBL = 7,
        XIBRF = 8, XIBLU = 9, XIBLF = 10, XIBLBR = 11,
        XIRL = 0,  XIRB = 1,  XIRD = 2,  XILB = 3,
        XILR = 4,  XILD = 5,  XIBR = 6,  XIBL = 7,
        XIBD = 8,  XIDR = 9,  XIDL = 10, XIDB = 11;

    //Edges
    public static final int
        EUB = 0, EUR = 1, EUL = 2, EFL = 3,
        EFR = 4, ERBR = 5, EBRB = 6, EBLB = 7,
        ELBL = 8, EDF = 9, EDBR = 10, EDBL = 11;

    //Corners
    public static final int
        CUF = 0, CUBR = 1, CUBL = 2,
        CDL = 3, CDR = 4, CDB = 5;

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
        moveEffects[R].xp1 = new int[]{XIUBL, XIFU, XIFBR, XIBRF, XIBRU, XIFBL, XIUF, XIBRBL, XIUBR, XIBLU, XIBLF, XIBLBR};
        moveEffects[R].xp2 = new int[]{XIRD, XIRL, XIRB, XILB, XILR, XILD, XIBR, XIBL, XIBD, XIDR, XIDL, XIDB};
        //L
        moveEffects[L].cp = new int[]{CUBL,  CUBR, CDL, CUF, CDR, CDB};
        moveEffects[L].co = new int[]{1, 0, 1, 0, 0, 0};
        moveEffects[L].ep = new int[]{EUB, EUR, ELBL, EUL, EFR, ERBR, EBRB, EBLB, EFL, EDF, EDBR, EDBL};
        moveEffects[L].xp1 = new int[]{XIBLF, XIUBR, XIBLU, XIUBL, XIFBR, XIUF, XIBRU, XIBRBL, XIBRF, XIFBL, XIFU, XIBLBR};
        moveEffects[L].xp2 = new int[]{XIRL, XIRB, XIRD, XILD, XILB, XILR, XIBR, XIBL, XIBD, XIDR, XIDL, XIDB};
        //B
        moveEffects[B].cp = new int[]{CUF, CDB, CUBR, CDL, CDR, CUBL};
        moveEffects[B].co = new int[]{0, 1, 1, 0, 0, 0};
        moveEffects[B].ep = new int[]{EBRB, EUR, EUL, EFL, EFR, ERBR, EBLB, EUB, ELBL, EDF, EDBR, EDBL};
        moveEffects[B].xp1 = new int[]{XIBRU, XIBRBL, XIUF, XIFU, XIFBR, XIFBL, XIBLBR, XIBLU, XIBRF, XIUBR, XIBLF, XIUBL};
        moveEffects[B].xp2 = new int[]{XIRL, XIRB, XIRD, XILB, XILR, XILD, XIBD, XIBR, XIBL, XIDR, XIDL, XIDB};
        //D
        moveEffects[D].cp = new int[]{CUF, CUBR, CUBL, CDB, CDL, CDR};
        moveEffects[D].co = new int[]{0, 0, 0, 0, 0, 0};
        moveEffects[D].ep = new int[]{EUB, EUR, EUL, EFL, EFR, ERBR, EBRB, EBLB, ELBL, EDBL, EDF, EDBR};
        moveEffects[D].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFU, XIBLF, XIBLBR, XIBRU, XIFBR, XIFBL, XIBLU, XIBRBL, XIBRF};
        moveEffects[D].xp2 = new int[]{XIRL, XIRB, XIRD, XILB, XILR, XILD, XIBR, XIBL, XIBD, XIDL, XIDB, XIDR};
        //U
        moveEffects[U].cp = new int[]{CUBR, CUBL, CUF, CDL, CDR, CDB};
        moveEffects[U].co = new int[]{0, 0, 0, 0, 0, 0};
        moveEffects[U].ep = new int[]{EUL, EUB, EUR, EFL, EFR, ERBR, EBRB, EBLB, ELBL, EDF, EDBR, EDBL};
        moveEffects[U].xp1 = new int[]{XIUF, XIUBL, XIUBR, XIFU, XIFBR, XIFBL, XIBRU, XIBRBL, XIBRF, XIBLU, XIBLF, XIBLBR};
        moveEffects[U].xp2 = new int[]{XIBR, XIBL, XIRD, XIRL, XIRB, XILD, XILB, XILR, XIBD, XIDR, XIDL, XIDB};
        //F
        moveEffects[F].cp = new int[]{CDL, CUBR, CUBL, CDR, CUF, CDB};
        moveEffects[F].co = new int[]{1, 0, 0, 1, 0, 0};
        moveEffects[F].ep = new int[]{EUB, EUR, EUL, EDF, EFL, ERBR, EBRB, EBLB, ELBL, EFR, EDBR, EDBL};
        moveEffects[F].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFBL, XIFU, XIFBR, XIBRU, XIBRBL, XIBRF, XIBLU, XIBLF, XIBLBR};
        moveEffects[F].xp2 = new int[]{XILD, XIRB, XILR, XILB, XIDL, XIDR, XIBR, XIBL, XIBD, XIRL, XIRD, XIDB};
        //BR
        moveEffects[BR].cp = new int[]{CUF, CDR, CUBL, CDL, CDB, CUBR};
        moveEffects[BR].co = new int[]{0, 1, 0, 0, 1, 0};
        moveEffects[BR].ep = new int[]{EUB, EUR, EUL, EFL, EFR, EDBR, ERBR, EBLB, ELBL, EDF, EBRB, EDBL};
        moveEffects[BR].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFU, XIFBR, XIFBL, XIBRF, XIBRU, XIBRBL, XIBLU, XIBLF, XIBLBR};
        moveEffects[BR].xp2 = new int[]{XIRL, XIDR, XIDB, XILB, XILR, XILD, XIRD, XIBL, XIRB, XIBD, XIDL, XIBR};
        //BL
        moveEffects[BL].cp = new int[]{CUF, CUBR, CDB, CUBL, CDR, CDL};
        moveEffects[BL].co = new int[]{0, 0, 1, 0, 0, 1};
        moveEffects[BL].ep = new int[]{EUB, EUR, EUL, EFL, EFR, ERBR, EBRB, EDBL, EBLB, EDF, EDBR, ELBL};
        moveEffects[BL].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFU, XIFBR, XIFBL, XIBRU, XIBRBL, XIBRF, XIBLBR, XIBLU, XIBLF};
        moveEffects[BL].xp2 = new int[]{XIRL, XIRB, XIRD, XIBD, XILR, XIBL, XIBR, XIDB, XIDL, XIDR, XILB, XILD};

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
