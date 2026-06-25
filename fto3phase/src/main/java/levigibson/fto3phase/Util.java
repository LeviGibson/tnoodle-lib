package levigibson.fto3phase;

import java.util.Arrays;

public class Util {

    public static int[] FACTORIAL = new int[] {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800, 39916800, 479001600};

    public static int choose(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;

        if (k > n - k) {
            k = n - k;
        }

        int result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
        }
        return result;
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
    
}
