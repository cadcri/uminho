public class SPARSE {

    static void sparseCompRow_matmult(int M, double[] y, double[] val, int[] row, int[] col, double[] x, int NUM_ITERATIONS) {
        for (int reps = 0; reps < NUM_ITERATIONS; reps++) {
            for (int r = 0; r < M; r++) {
                double sum = 0.0;
                int rowR = row[r];
                int rowRp1 = row[r + 1];
                for (int i = rowR; i < rowRp1; i++) {
                    sum += x[col[i]] * val[i];
                }
                y[r] += sum;
            }
        }
    }

    public static void main(String[] argv) {
        int size = Integer.parseInt(argv[0]);
        int num_iterations = 10;

        int nnz = size * 2;

        double[] val = new double[nnz];
        int[] col = new int[nnz];
        int[] row = new int[size + 1];
        double[] x = new double[size];
        double[] y = new double[size];

        sparseCompRow_matmult(size, y, val, row, col, x, num_iterations);
    }
}