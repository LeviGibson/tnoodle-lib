package levigibson.fto3phase;

import java.util.Arrays;

public class Util {

    public static final int[] FACTORIAL = new int[] {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600};

    public static int choose(int n, int k) {
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

    //https://medium.com/@benjamin.botto/sequentially-indexing-permutations-a-linear-algorithm-for-computing-lexicographic-rank-a22220ffd6e3
    public static int packPerm(int[] arr, boolean parity){
        int size = arr.length;

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

    public static void unpackPerm(int[] arr, int idx, boolean parity){
        if (parity)
            idx *= 2;

        int size = arr.length;

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
            index += choose(idx[i], i+1);
        }
        return index;
    }

    public static void unpackSubset(int[] arr, int idx){
        int subsetSize = arr.length;

        int k = subsetSize;
        int remaining = idx;

        for (int pos = 0; pos < subsetSize; pos++) {
            int c = k - 1;
            while (Util.choose(c + 1, k) <= remaining) {
                c++;
            }
            arr[subsetSize - 1 - pos] = c;
            remaining -= Util.choose(c, k);
            k--;
        }
    }
}
