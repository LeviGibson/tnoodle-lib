package levigibson.fto3phase;


import java.util.Arrays;
import java.util.Random;

public class FtoCubie {

    int[] cp;
    int[] co;
    int[] edges;
    int[] centers1;
    int[] centers2;

    public FtoCubie(){
        cp = new int[6];
        co = new int[6];
        edges = new int[12];
        centers1 = new int[12];
        centers2 = new int[12];

        for (int i = 0; i < 6; i++) {
            cp[i] = i;
            co[i] = 0;
        }

        for (int i = 0; i < 12; i++) {
            edges[i] = i;
            centers1[i] = i / 3; // centers1 = 000 111 222 333
            centers2[i] = i / 3;
        }
    }

    public FtoCubie(FtoCubie other) {
        this.cp = other.cp.clone();
        this.co = other.co.clone();
        this.edges = other.edges.clone();
        this.centers1 = other.centers1.clone();
        this.centers2 = other.centers2.clone();
    }


    //[0, 12!/2 - 1]
    public int packEdges(){
        return Util.packPerm(edges, true);
    }

    public void setEdges(int idx){
        Util.unpackPerm(edges, idx, true);
    }

    public int packCornerPermutation(){
        return Util.packPerm(cp, true);
    }

    public void setCornerPermutation(int idx){
        Util.unpackPerm(cp, idx, true);
    }

    public int packCornerOrientation(){
        int index = 0;
        for (int i = 0; i < 5; i++) {
            index |= co[i] << i;
        }
        return index;
    }

    public void setCornerOrientation(int idx){
        for (int i = 0; i < 5; i++) {
            co[i] = (idx >> i) & 1;
        }
        co[5] = Integer.bitCount(idx) % 2;
    }

    public int packPhaseOneEdges() {
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

        return Util.packSubset(loc) * 6 + Util.packPerm(perm, false);
    }

    public void setPhaseOneEdges(int idx){
        int locIdx = idx / 6;
        int permIdx = idx % 6;

        int[] loc = new int[3];
        int[] perm = new int[3];

        //Unpack loc
        Util.unpackSubset(loc, locIdx);

        //Unpack perm
        Util.unpackPerm(perm, permIdx, false);
        for (int i = 0; i < 3; i++) { perm[i] += 9; }

        //set the edges
        Arrays.fill(edges, -1);
        for (int i = 0; i < 3; i++) {
            edges[loc[i]] = perm[i];
        }
        int nonD = 0;
        for (int i = 0; i < 12; i++) {
            if (edges[i] == -1) {
                edges[i] = nonD++;
            }
        }

        //Parity fix
        //(This has nothing to do with the phase 1 edges)
        //(The other edges are filled with garbage)
        //(But it should be garbage with the correct parity)
        //(so it doesn't crash the program)
        if (Util.parity(edges)) {
            int i = 0;
            while (edges[i] >= 9) i++;
            int j = i + 1;
            while (edges[j] >= 9) j++;
            Util.swap(edges, i, j);
        }
    }

    public int packPhaseOneTriangles(){
        int[] idx = new int[3];
        int count = 0;

        for (int i = 0; i < 12; i++) {
            if (centers2[i] == XD) idx[count++] = i;
        }

        if (count != 3) throw new IllegalStateException("Less than 3 d-layer triangles");

        return Util.packSubset(idx);
    }

    public void setPhaseOneTriangles(int idx){
        //Unpack index
        int[] loc = new int[3];
        Util.unpackSubset(loc, idx);

        //Set the relevant centers
        for (int i = 0; i < 12; i++) {
            centers2[i] = -1;
        }
        for (int i = 0; i < 3; i++) {
            centers2[loc[i]] = XD;
        }

        //Fill in the rest with garbage
        //(But it has to not crash the other functions)
        int count = 0;
        for (int i = 0; i < 12; i++) {
            if (centers2[i] == -1){
                centers2[i] = count/3;
                count++;
            }
        }
    }

    public int packPhaseTwoEdges(){
        for (int i = 0; i < 9; i++) {
            if (edges[i] > 8) throw new IllegalStateException("Edges not in phase 1");
        }

        return Util.packPerm(edges, true, 9);
    }

    public void setPhaseTwoEdges(int idx){
        Util.unpackPerm(edges, idx, 9, true);
        for (int i = 9; i < 12; i++) {
            edges[i] = i;
        }
    }

    public int packPhaseTwoTris(){
        for (int i = 9; i < 12; i++) {
            if (centers2[i] != XD)
                throw new IllegalStateException("Tris must be in phase 1");
        }

        boolean[] used = new boolean[9];
        int[][] loc = new int[2][3];

        for (int xo = 0; xo < 2; xo++) {
            int found = 0;
            int passed = 0;
            for (int i = 0; i < 9; i++) {
                if (centers2[i] == xo){
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
        return Util.packSubset(loc[1]) +
            Util.packSubset(loc[0]) * Util.choose(6, 3);
    }

    public void setPhaseTwoTris(int idx){
        int[] loc0 = new int[3];
        int[] loc1 = new int[3];
        Util.unpackSubset(loc0, idx / Util.choose(6, 3));
        Util.unpackSubset(loc1, idx % Util.choose(6, 3));
        Arrays.sort(loc1);

        Arrays.fill(centers2, 0, 9, 2);
        for (int v : loc0) centers2[v] = 0;

        int nz = 0;
        int li = 0;
        for (int i = 0; i < 9; i++) {
            if (centers2[i] == 0) continue;
            if (li < 3 && nz == loc1[li]) {
                centers2[i] = 1;
                li++;
            }
            nz++;
        }
        for (int i = 9; i < 12; i++) centers2[i] = XD;
    }

    private static int[][] CORNER_PARITY = {
        {0,0,0,-1,-1,-1},
        {1,-1,-1,0,1,-1},
        {-1,1,-1,-1,0,1},
        {-1,-1,1,1,-1,0}};

    //[0, 159]
    private int packTripleCorners(int color){
        int[] idx = new int[3];
        int orientation = 0;

        int found = 0;
        for (int i = 0; i < 6; i++) {
            int corner = cp[i];
            int ori = co[i];

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

        return Util.packSubset(idx) * 8 + orientation;
    }

    private int packTripleTris(int color){
        int[] idx = new int[3];

        int found = 0;
        for (int i = 0; i < 12; i++) {
            if (centers1[i] == color){
                idx[found++] = i;
            }
        }

        return Util.packSubset(idx);
    }

    public int packTriples(int color){
        if (color > 3 || color < 0){
            throw new IllegalArgumentException("color must be U, F, BR, or BL");
        }

        return (160 * packTripleTris(color)) + packTripleCorners(color);
    }

    private void setTripleTris(int idx, int color){
        int[] loc = new int[3];
        Util.unpackSubset(loc, idx);
        Arrays.fill(centers1, -1);

        for (int i = 0; i < 3; i++) {
            centers1[loc[i]] = color;
        }
    }

    private void setTripleCorners(int idx, int color){
        int[] loc = new int[3];
        Util.unpackSubset(loc, idx / 8);

        int orientation = idx % 8;

        int[] relevantCorners = new int[3];
        int found = 0;
        for (int i = 0; i < 6; i++) {
            if (CORNER_PARITY[color][i] != -1){
                relevantCorners[found++] = i;
            }
        }

        Arrays.fill(cp, -1);
        Arrays.fill(co, -1);

        for (int i = 0; i < 3; i++) {
            int perm = loc[i];
            int ori = (orientation >> i) & 1;

            cp[perm] = relevantCorners[i];
            co[perm] = ori ^ CORNER_PARITY[color][relevantCorners[i]];
        }

    }

    public void setTriples(int idx, int color){
        int tris = idx / 160;
        int corners = idx % 160;

        setTripleTris(tris, color);
        setTripleCorners(corners, color);
    }

    public int packPhaseThreeCorners(){
        return (packCornerPermutation() * 32) + packCornerOrientation();
    }

    public void setPhaseThreeCorners(int idx){
        setCornerPermutation(idx/32);
        setCornerOrientation(idx%32);
    }

    public int packPhaseThreeEdges(){
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

    public void setPhaseThreeEdges(int idx){
        for (int i = 0; i < 12; i++) {
            edges[i] = i;
        }

        int remaining = idx;
        for (int axis = 0; axis < 4; axis++) {
            int coefficient = Util.pow(3, 3-axis);
            int digit = remaining / coefficient;

            while (edges[G3_EDGES[axis][0]] != G3_EDGES[axis][digit]){
                for (int i = 0; i < 2; i++) {
                    Util.swap(edges, G3_EDGES[axis][0], G3_EDGES[axis][i + 1]);
                }
            }

            remaining -= coefficient * digit;
        }

    }


    public boolean isSolved(){
        for (int i = 0; i < 6; i++) {
            if (co[i] != 0)
                return false;
            if (cp[i] != i)
                return false;
        }

        for (int i = 0; i < 12; i++) {
            if (centers1[i] != i / 3)
                return false;
            if (centers2[i] != i / 3)
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
            out.cp[i] = this.cp[cycles.cp[i]];
            out.co[i] = this.co[cycles.cp[i]] ^ cycles.co[i];
        }

        //Edges + Triangles
        for (int i = 0; i < 12; i++) {
            out.edges[i] = this.edges[cycles.ep[i]];
            out.centers1[i] = this.centers1[cycles.xp1[i]];
            out.centers2[i] = this.centers2[cycles.xp2[i]];
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
            Arrays.equals(fto.centers1, this.centers1) &&
            Arrays.equals(fto.centers2, this.centers2) &&
            Arrays.equals(fto.co, this.co) &&
            Arrays.equals(fto.cp, this.cp);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.edges) ^
            Arrays.hashCode(this.centers1) ^
            Arrays.hashCode(this.centers2) ^
            Arrays.hashCode(this.co) ^
            Arrays.hashCode(this.cp);
    }

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

    public static void main(String[] args) {
        for (int i = 0; i < 6; i++) {
            System.out.println(FtoCubie.moveEffects[LP].co[i]);
        }
    }

}
