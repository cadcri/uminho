#include <stdlib.h>

void sparseCompRow_matmult(int M, double *y, double *val, int *row, int *col, double *x, int NUM_ITERATIONS) {
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

int main(int argc, char *argv[]){
    int size = atoi(argv[1]);
    int num_iterations = 10;

    int nnz = size * 2;

    double *val = (double *)malloc(nnz * sizeof(double));
    int *col = (int *)malloc(nnz * sizeof(int));
    int *row = (int *)malloc((size + 1) * sizeof(int));
    double *x = (double *)malloc(size * sizeof(double));
    double *y = (double *)malloc(size * sizeof(double));

    sparseCompRow_matmult(size, y, val, row, col, x, num_iterations);

    return 0;
}