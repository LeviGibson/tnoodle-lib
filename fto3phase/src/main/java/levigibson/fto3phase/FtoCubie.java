package levigibson.fto3phase;


import java.util.Arrays;

public class FtoCubie {

    public int[] cp;
    public int[] co;
    public int[] edges;
    public int[] centers1;
    public int[] centers2;


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


    //[0, 12!/2 - 1]
    public int idxEdges(){
        return Util.packPerm(edges, true);
    }

    public void setEdges(int idx){
        Util.unpackPerm(edges, idx, true);
    }

    public int idxCornerPermutation(){
        return Util.packPerm(cp, true);
    }

    public void setCornerPermutation(int idx){
        Util.unpackPerm(cp, idx, true);
    }

    public int idxCornerOrientation(){
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

    //[0,219]
    public int idxPhaseOneEdgeLocations(){
        int[] idx = new int[3];
        int count = 0;

        for (int i = 0; i < 12; i++) {
            //If the edge is a D-face edge
            if (edges[i] >= EDF){
                idx[count++] = i;
            }
        }

        if (count != 3) {
            throw new IllegalStateException("Expected 3 D-face edges. This is not a possible FTO state.");
        }

        return Util.packSubset(idx);
    }

    public int idxPhaseOneEdgePermutation(){
        int numSeen = 0;

        int[] perm = {-1, -1, -1};

        for (int i = 0; i < 12; i++) {
            int e = edges[i] - 9;
            if (e >= 0){
                perm[numSeen++] = e;
            }
        }

        return Util.packPerm(perm, false);
    }

    public void setG1Edges(int locIdx, int permIdx) {
        int[] loc = new int[3];
        int[] perm = new int[3];

        //Unpack loc
        int k = 3;
        int remaining = locIdx;

        for (int pos = 0; pos < 3; pos++) {
            int c = k - 1;
            while (Util.choose(c + 1, k) <= remaining) {
                c++;
            }
            loc[pos] = c;
            remaining -= Util.choose(c, k);
            k--;
        }

        //Unpack perm
        Util.unpackPerm(perm, permIdx, false);
        for (int i = 0; i < 3; i++) { perm[i] += 9; }

        //set the edges
        Arrays.fill(edges, -1);
        for (int i = 0; i < 3; i++) {
            edges[loc[i]] = perm[2-i];
        }
        int nonD = 0;
        for (int i = 0; i < 12; i++) {
            if (edges[i] == -1) {
                edges[i] = nonD++;
            }
        }

        //Parity fix
        if (Util.parity(edges)) {
            int i = 0;
            while (edges[i] >= 9) i++;
            int j = i + 1;
            while (edges[j] >= 9) j++;
            Util.swap(edges, i, j);
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
        B = 4, BP = 5, D = 6, DP = 7,
        U = 8, UP = 9, F = 10, FP = 11,
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

    private static MoveEffect[] moveEffects;

    //Hard-coded data about what cycles the different moves make
    static {
        moveEffects = new MoveEffect[16];
        for (int i = 0; i < 16; i++) { moveEffects[i] = new MoveEffect(); }

        //R
        moveEffects[0].cp = new int[]{CDR, CUF, CUBL, CDL, CUBR, CDB};
        moveEffects[0].co = new int[]{1, 0, 0, 0, 1, 0};
        moveEffects[0].ep = new int[]{EUB, EFR, EUL, EFL, ERBR, EUR, EBRB, EBLB, ELBL, EDF, EDBR, EDBL};
        moveEffects[0].xp1 = new int[]{XIUBL, XIFU, XIFBR, XIBRF, XIBRU, XIFBL, XIUF, XIBRBL, XIUBR, XIBLU, XIBLF, XIBLBR};
        moveEffects[0].xp2 = new int[]{XIRD, XIRL, XIRB, XILB, XILR, XILD, XIBR, XIBL, XIBD, XIDR, XIDL, XIDB};
        //L
        moveEffects[2].cp = new int[]{CUBL,  CUBR, CDL, CUF, CDR, CDB};
        moveEffects[2].co = new int[]{0, 0, 1, 1, 0, 0};
        moveEffects[2].ep = new int[]{EUB, EUR, ELBL, EUL, EFR, ERBR, EBRB, EBLB, EFL, EDF, EDBR, EDBL};
        moveEffects[2].xp1 = new int[]{XIBLF, XIUBR, XIBLU, XIUBL, XIFBR, XIUF, XIBRU, XIBRBL, XIBRF, XIFBL, XIFU, XIBLBR};
        moveEffects[2].xp2 = new int[]{XIRL, XIRB, XIRD, XILD, XILB, XILR, XIBR, XIBL, XIBD, XIDR, XIDL, XIDB};
        //B
        moveEffects[4].cp = new int[]{CUF, CDB, CUBR, CDL, CDR, CUBL};
        moveEffects[4].co = new int[]{0, 1, 0, 0, 0, 1};
        moveEffects[4].ep = new int[]{EBRB, EUR, EUL, EFL, EFR, ERBR, EBLB, EUB, ELBL, EDF, EDBR, EDBL};
        moveEffects[4].xp1 = new int[]{XIBRU, XIBRBL, XIUF, XIFU, XIFBR, XIFBL, XIBLBR, XIBLU, XIBRF, XIUBR, XIBLF, XIUBL};
        moveEffects[4].xp2 = new int[]{XIRL, XIRB, XIRD, XILB, XILR, XILD, XIBD, XIBR, XIBL, XIDR, XIDL, XIDB};
        //D
        moveEffects[6].cp = new int[]{CUF, CUBR, CUBL, CDB, CDL, CDR};
        moveEffects[6].co = new int[]{0, 0, 0, 0, 0, 0};
        moveEffects[6].ep = new int[]{EUB, EUR, EUL, EFL, EFR, ERBR, EBRB, EBLB, ELBL, EDBL, EDF, EDBR};
        moveEffects[6].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFU, XIBLF, XIBLBR, XIBRU, XIFBR, XIFBL, XIBLU, XIBRBL, XIBRF};
        moveEffects[6].xp2 = new int[]{XIRL, XIRB, XIRD, XILB, XILR, XILD, XIBR, XIBL, XIBD, XIDL, XIDB, XIDR};
        //U
        moveEffects[8].cp = new int[]{CUBR, CUBL, CUF, CDL, CDR, CDB};
        moveEffects[8].co = new int[]{0, 0, 0, 0, 0, 0};
        moveEffects[8].ep = new int[]{EUL, EUB, EUR, EFL, EFR, ERBR, EBRB, EBLB, ELBL, EDF, EDBR, EDBL};
        moveEffects[8].xp1 = new int[]{XIUF, XIUBL, XIUBR, XIFU, XIFBR, XIFBL, XIBRU, XIBRBL, XIBRF, XIBLU, XIBLF, XIBLBR};
        moveEffects[8].xp2 = new int[]{XIBR, XIBL, XIRD, XIRL, XIRB, XILD, XILB, XILR, XIBD, XIDR, XIDL, XIDB};
        //F
        moveEffects[10].cp = new int[]{CDL, CUBR, CUBL, CDR, CUF, CDB};
        moveEffects[10].co = new int[]{1, 0, 0, 1, 0, 0};
        moveEffects[10].ep = new int[]{EUB, EUR, EUL, EDF, EFL, ERBR, EBRB, EBLB, ELBL, EFR, EDBR, EDBL};
        moveEffects[10].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFBL, XIFU, XIFBR, XIBRU, XIBRBL, XIBRF, XIBLU, XIBLF, XIBLBR};
        moveEffects[10].xp2 = new int[]{XILD, XIRB, XILR, XILB, XIDL, XIDR, XIBR, XIBL, XIBD, XIRL, XIRD, XIDB};
        //BR
        moveEffects[12].cp = new int[]{CUF, CDR, CUBL, CDL, CDB, CUBR};
        moveEffects[12].co = new int[]{0, 1, 0, 0, 0, 1};
        moveEffects[12].ep = new int[]{EUB, EUR, EUL, EFL, EFR, EDBR, ERBR, EBLB, ELBL, EDF, EBRB, EDBL};
        moveEffects[12].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFU, XIFBR, XIFBL, XIBRF, XIBRU, XIBRBL, XIBLU, XIBLF, XIBLBR};
        moveEffects[12].xp2 = new int[]{XIRL, XIDR, XIDB, XILB, XILR, XILD, XIRD, XIBL, XIRB, XIBD, XIDL, XIBR};
        //BL
        moveEffects[14].cp = new int[]{CUF, CUBR, CDB, CUBL, CDR, CDL};
        moveEffects[14].co = new int[]{0, 0, 1, 1, 0, 0};
        moveEffects[14].ep = new int[]{EUB, EUR, EUL, EFL, EFR, ERBR, EBRB, EDBL, EBLB, EDF, EDBR, ELBL};
        moveEffects[14].xp1 = new int[]{XIUBL, XIUBR, XIUF, XIFU, XIFBR, XIFBL, XIBRU, XIBRBL, XIBRF, XIBLBR, XIBLU, XIBLF};
        moveEffects[14].xp2 = new int[]{XIRL, XIRB, XIRD, XIBD, XILR, XIBL, XIBR, XIDB, XIDL, XIDR, XILB, XILD};

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
