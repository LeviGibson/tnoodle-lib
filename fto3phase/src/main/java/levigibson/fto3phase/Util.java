package levigibson.fto3phase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Util {

    public static final int[] FACTORIAL = new int[] {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600};

    public static int nCr(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        if (k > n - k) k = n - k;

        long result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
        }
        assert result <= Integer.MAX_VALUE
            : "choose(" + n + ", " + k + ") = " + result + " overflows int";
        return (int) result;
    }

    public static void swap(int[] arr, int i1, int i2){
        int tmp = arr[i2];
        arr[i2] = arr[i1];
        arr[i1] = tmp;
    }

    public static boolean parity(int[] perm) {
        perm = Arrays.copyOf(perm, perm.length);

        int n = perm.length;
        int swaps = 0;
        for (int i = 0; i < n; i++) {
            while (perm[i] != i) {
                int target = perm[i];
                swap(perm, i, target);
                swaps++;
            }
        }
        return swaps % 2 == 1;
    }

    public static int packPerm(int[] arr, boolean parity){
        return packPerm(arr, parity, arr.length);
    }

    //https://medium.com/@benjamin.botto/sequentially-indexing-permutations-a-linear-algorithm-for-computing-lexicographic-rank-a22220ffd6e3
    public static int packPerm(int[] arr, boolean parity, int size) {
        int index = 0;
        int seen = 0;

        for (int i = 0; i < size; i++) {
            int e = arr[i];
            seen |= (1 << ((size-1) - e));

            int lehmerDigit = e - Integer.bitCount(seen >> (size - e));
            index += Util.FACTORIAL[(size-1)-i] * lehmerDigit;
        }

        return parity ? index / 2 : index;
    }

    public static void unpackPerm(int[] arr, int idx, boolean parity) {
        unpackPerm(arr, idx, arr.length, parity);
    }

    public static void unpackPerm(int[] arr, int idx, int size, boolean parity){
        if (parity)
            idx *= 2;

        boolean[] used = new boolean[size];
        for (int i = 0; i < size; i++) {
            int lehmerDigit = idx / Util.FACTORIAL[(size-1) - i];
            idx %= Util.FACTORIAL[(size-1) - i];

            int count = 0;
            int e = -1;
            for (int v = 0; v < size; v++) {
                if (!used[v]) {
                    if (count == lehmerDigit) {
                        e = v;
                        break;
                    }
                    count++;
                }
            }

            arr[i] = e;
            used[e] = true;
        }

        if (parity && Util.parity(arr)){
            Util.swap(arr, size-2, size-1);
        }
    }

    public static int packSubset(int[] idx){

        for (int i = 0; i < idx.length-1; i++) {
            if (idx[i] >= idx[i+1]){
                throw new IllegalArgumentException("idx must be in ascending order");
            }
        }

        int index = 0;
        for (int i = idx.length-1; i >= 0; i--) {
            index += nCr(idx[i], i+1);
        }
        return index;
    }

    public static void unpackSubset(int[] arr, int idx){
        int subsetSize = arr.length;

        int k = subsetSize;
        int remaining = idx;

        for (int pos = 0; pos < subsetSize; pos++) {
            int c = k - 1;
            while (nCr(c + 1, k) <= remaining) {
                c++;
            }
            arr[subsetSize - 1 - pos] = c;
            remaining -= nCr(c, k);
            k--;
        }
    }

    private static final Map<String, Integer> MOVE_MAP = new HashMap<>();
    static {
        String[] names = {"R", "L", "B", "U", "D", "F", "BR", "BL"};
        int[] cw  = {FtoCubie.R,  FtoCubie.L,  FtoCubie.B,  FtoCubie.U,
            FtoCubie.D,  FtoCubie.F,  FtoCubie.BR, FtoCubie.BL};
        int[] ccw = {FtoCubie.RP, FtoCubie.LP, FtoCubie.BP, FtoCubie.UP,
            FtoCubie.DP, FtoCubie.FP, FtoCubie.BRP, FtoCubie.BLP};
        for (int i = 0; i < 8; i++) {
            MOVE_MAP.put(names[i], cw[i]);
            MOVE_MAP.put(names[i] + "'", ccw[i]);
        }
    }

    public static int parseMove(String s) {
        Integer move = MOVE_MAP.get(s);
        if (move == null) {
            throw new IllegalArgumentException("Unrecognized move: " + s);
        }
        return move;
    }

    public static FtoCubie applyAlg(String alg) {
        FtoCubie a = new FtoCubie();   // solved state
        FtoCubie b = new FtoCubie();   // temp buffer
        for (String token : alg.trim().split("\\s+")) {
            a.turn(parseMove(token), b);
            FtoCubie tmp = a;
            a = b;
            b = tmp;
        }
        return a;
    }

    private static final String[] MOVE_NAMES = {"R", "R'", "L", "L'", "B", "B'", "U", "U'", "D", "D'", "F", "F'", "BR", "BR'", "BL", "BL'"};

    public static String moveArrayToString(int[] moves, int length) {
        if (length > moves.length)
            throw new IllegalArgumentException("length must be less than or equal to the size of moves[]");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(MOVE_NAMES[moves[i]]);
        }
        return sb.toString();
    }

    public static int pow(int a, int b){
        int c = 1;
        for (int i = 0; i < b; i++) {
            c *= a;
        }
        return c;
    }

    public static String moveArrayToString(int[] moves) {
        return moveArrayToString(moves, moves.length);
    }
}
