public final class LU {
    public static int luFactor(int M, int N, double[][] A, int[] pivot) {
        int minMN = (M < N) ? M : N;
        int j = 0;

        for (j = 0; j < minMN; j++) {
            int jp = j;
            double t = Math.abs(A[j][j]);

            for (int i = j + 1; i < M; i++) {
                double ab = Math.abs(A[i][j]);
                if (ab > t) {
                    jp = i;
                    t = ab;
                }
            }

            pivot[j] = jp;

            if (A[jp][j] == 0.0) {
                return 1;
            }

            if (jp != j) {
                double[] tA = A[j];
                A[j] = A[jp];
                A[jp] = tA;
            }

            if (j < M - 1) {
                double recp = 1.0 / A[j][j];
                for (int k = j + 1; k < M; k++) {
                    A[k][j] *= recp;
                }
            }

            if (j < minMN - 1) {
                for (int ii = j + 1; ii < M; ii++) {
                    double[] Aii = A[ii];
                    double[] Aj = A[j];
                    double AiiJ = Aii[j];
                    for (int jj = j + 1; jj < N; jj++) {
                        Aii[jj] -= AiiJ * Aj[jj];
                    }
                }
            }
        }

        return 0;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Main <size>");
            System.exit(1);
        }

        int size = Integer.parseInt(args[0]);

        double[][] A = new double[size][size];
        int[] pivot = new int[size];

        int res = luFactor(size, size, A, pivot);
    }
}