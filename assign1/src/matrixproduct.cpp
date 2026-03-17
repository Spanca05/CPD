#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <omp.h>

using namespace std;

#define SYSTEMTIME clock_t


void OnMult(int m_ar, int m_br)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);


    Time1 = omp_get_wtime();

    for (i = 0; i < m_ar; i++)
    {
        for (j = 0; j < m_br; j++)
        {
            temp = 0;
            for (k = 0; k < m_ar; k++)
            {
                temp += pha[i * m_ar + k] * phb[k * m_br + j];
            }
            phc[i * m_ar + j] = temp;
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}


void OnMultParallel_1(int m_ar, int m_br, int threads)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

                
    omp_set_num_threads(threads);

    Time1 = omp_get_wtime();

    
    #pragma omp parallel for private(j, k, temp) schedule(static)
    for (i = 0; i < m_ar; i++)
    {
        for (j = 0; j < m_br; j++)
        {
            temp = 0.0;
            for (k = 0; k < m_ar; k++)
            {
                temp += pha[i * m_ar + k] * phb[k * m_br + j];
            }
            phc[i * m_ar + j] = temp;
        }
    }


    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}


void OnMultParallel_2(int m_ar, int m_br, int threads)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);


    omp_set_num_threads(threads);

    Time1 = omp_get_wtime();

    #pragma omp parallel for collapse(2) private(j, k, temp) schedule(static)
    for (i = 0; i < m_ar; i++)
    {
        for (j = 0; j < m_br; j++)
        {
            temp = 0.0;
            #pragma omp simd reduction(+ : temp)
            for (k = 0; k < m_ar; k++)
            {
                temp += pha[i * m_ar + k] * phb[k * m_br + j];
            }
            phc[i * m_br + j] = temp;
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}

// Line-by-line matrix multiplication
void OnMultLine(int m_ar, int m_br)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    // Inicialize matrix C so that we can add to each element
    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phc[i * m_br + j] = 0.0;


    Time1 = omp_get_wtime();

    // Alterar loop para percorrer linha de A e de B em vez de linha de A e coluna de B
    // Cada elemento A[i,k] aponta para a linha k de B
    for (i = 0; i < m_ar; i++)
    {
        for (k = 0; k < m_ar; k++)
        {
            temp = pha[i * m_ar + k];
            for (j = 0; j < m_br; j++)
            {
                phc[i * m_ar + j] += temp * phb[k * m_br + j];
            }
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}


void OnMultLineParallel_1(int m_ar, int m_br, int threads)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phc[i * m_br + j] = 0.0;


    omp_set_num_threads(threads);

    Time1 = omp_get_wtime();

    #pragma omp parallel for private(j, k, temp) schedule(static)
    for (i = 0; i < m_ar; i++)
    {
        for (k = 0; k < m_ar; k++)
        {
            temp = pha[i * m_ar + k];
            for (j = 0; j < m_br; j++)
            {
                phc[i * m_ar + j] += temp * phb[k * m_br + j];
            }
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}


void OnMultLineParallel_2(int m_ar, int m_br, int threads)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phc[i * m_br + j] = 0.0;


    omp_set_num_threads(threads);

    Time1 = omp_get_wtime();

    #pragma omp parallel private (i, k, temp)
    for (i = 0; i < m_ar; i++) {
        for (k = 0; k < m_ar; k++) 
        {
            temp = pha[i * m_ar + k];
            #pragma omp for
            for (j = 0; j < m_br; j++) 
            {
                phc[i * m_br + j] += temp * phb[k * m_br + j];
            }
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}


void OnMultLineParallel_3(int m_ar, int m_br, int threads)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phc[i * m_br + j] = 0.0;


    omp_set_num_threads(threads);

    Time1 = omp_get_wtime();

    #pragma omp parallel for private(k, j, temp) schedule(static)
    for (i = 0; i < m_ar; i++) {
        for (k = 0; k < m_ar; k++) 
        {
            temp = pha[i * m_ar + k];
            #pragma omp simd
            for (j = 0; j < m_br; j++) 
            {
                phc[i * m_br + j] += temp * phb[k * m_br + j];
            }
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}


// Block matrix multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize)
{
    double Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = 1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    // Inicialize matrix C so that we can add to each element
    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phc[i * m_br + j] = 0.0;


    Time1 = omp_get_wtime();

    for (int ii = 0; ii < m_ar; ii += bkSize)
    {
        for (int jj = 0; jj < m_br; jj += bkSize)
        {
            for (int kk = 0; kk < m_ar; kk += bkSize)
            {
                for (int i = ii; i < min(ii + bkSize, m_ar); i++)
                {
                    for (int k = kk; k < min(kk + bkSize, m_ar); k++)
                    {
                        for (int j = jj; j < min(jj + bkSize, m_br); j++)
                        {
                            phc[i * m_br + j] += pha[i * m_ar + k] * phb[k * m_br + j];
                        }
                    }
                }
            }
        }
    }

    Time2 = omp_get_wtime();

    snprintf(st, sizeof(st), "Time: %3.3f seconds\n", Time2 - Time1);
    cout << st;

    cout << "Result matrix: " << endl;
    for (i = 0; i < 1; i++)
    {
        for (j = 0; j < min(10, m_br); j++)
            cout << phc[j] << " ";
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}

int main(int argc, char *argv[])
{
    int lin, col, blockSize, threads;
    int op;

    do
    {
        cout << endl
             << "1. Multiplication" << endl;
        cout << "2. Line Multiplication" << endl;
        cout << "3. Block Multiplication" << endl;
        cout << "4. Multiplication Parallel 1" << endl;
        cout << "5. Multiplication Parallel 2" << endl;
        cout << "6. Line Multiplication Parallel 1" << endl;
        cout << "7. Line Multiplication Parallel 2" << endl;
        cout << "8. Line Multiplication Parallel 3" << endl;
        cout << "0. Exit" << endl;
        cout << "Selection?: ";
        cin >> op;

        if (op == 0)
            break;

        cout << "Dimensions: lins=cols ? ";
        cin >> lin;
        col = lin;

        switch (op)
        {
        case 1:
            OnMult(lin, col);
            break;
        case 2:
            OnMultLine(lin, col);
            break;
        case 3:
            cout << "Block Size? ";
            cin >> blockSize;
            OnMultBlock(lin, col, blockSize);
            break;
        case 4:
            cout << "Num of threads? ";
            cin >> threads;
            OnMultParallel_1(lin, col, threads);
            break;
        case 5:
            cout << "Num of threads? ";
            cin >> threads;
            OnMultParallel_2(lin, col, threads);
            break;
        case 6:
            cout << "Num of threads? ";
            cin >> threads;
            OnMultLineParallel_1(lin, col, threads);
            break;
        case 7:
            cout << "Num of threads? ";
            cin >> threads;
            OnMultLineParallel_2(lin, col, threads);
            break;
        case 8:
            cout << "Num of threads? ";
            cin >> threads;
            OnMultLineParallel_3(lin, col, threads);
            break;
        }

    } while (op != 0);

    return 0;
}
