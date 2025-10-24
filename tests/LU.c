#include <stdlib.h>

int LU_factor(int M, int N, double **A, int *pivot) {
    int minMN = M < N ? M : N;
    int j = 0;

    for (j = 0; j < minMN; j++) {
        int jp = j;
        double t = A[j][j] >= 0 ? A[j][j] : -A[j][j];
        for (int i = j + 1; i < M; i++) {
            double ab = A[i][j] >= 0 ? A[i][j] : -A[i][j];
            if (ab > t) {
                jp = i;
                t = ab;
            }
        }

        pivot[j] = jp;

        if (A[jp][j] == 0){
        	 return 1;
        }

        if (jp != j) {
            double *tA = A[j];
            A[j] = A[jp];
            A[jp] = tA;
        }

        if (j < M - 1) {
            double recp = 1.0 / A[j][j];
            int k;
            for (k = j + 1; k < M; k++) A[k][j] *= recp;
        }

        if (j < minMN - 1) {
            int ii;
            for (ii = j + 1; ii < M; ii++) {
                double *Aii = A[ii];
                double *Aj = A[j];
                double AiiJ = Aii[j];
                int jj;
                for (jj = j + 1; jj < N; jj++){
                	Aii[jj] -= AiiJ * Aj[jj];
                }
            }
        }
    }

    return 0;
}


int main(int argc, char *argv[]) {
    int size = atoi(argv[1]);

    double **A = malloc(size * sizeof(double *));
    for (int i = 0; i < size; i++){
    	A[i] = malloc(size * sizeof(double));
    }

    int *pivot = malloc(size * sizeof(int));

    LU_factor(size, size, A, pivot);

    return 0;
}