#include <stdlib.h>

    void SOR_execute(int M, int N, double omega, double **G, int num_iterations) {
        double omega_over_four = omega * 0.25;
        double one_minus_omega = 1.0 - omega;

        int Mm1 = M-1;
        int Nm1 = N-1; 
        int p;
        int i;
        int j;
        double *Gi;
        double *Gim1;
        double *Gip1;

        for (p=0; p<num_iterations; p++) {
            for (i=1; i<Mm1; i++) {
                Gi = G[i];
                Gim1 = G[i-1];
                Gip1 = G[i+1];
                for (j=1; j<Nm1; j++) {
                    Gi[j] = omega_over_four * (Gim1[j] + Gip1[j] + Gi[j-1] + Gi[j+1]) + one_minus_omega * Gi[j];
                }
            }
        }
    }


    int main(int argc, char *argv[]) {
        int size = atoi(argv[1]);
        double omega = 1.25;
        int num_iterations = 10;

        double **G = (double **)malloc(size * sizeof(double *));
        for (int i = 0; i < size; i++) {
            G[i] = (double *)malloc(size * sizeof(double));
        }

        SOR_execute(size, size, omega, G, num_iterations);

        return 0;
    }
            
