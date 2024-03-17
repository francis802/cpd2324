

#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <papi.h>
#include <omp.h>



using namespace std;

#define SYSTEMTIME clock_t
#define SIMPLE 1
#define LINE 2
#define BLOCK 3
#define PARALLEL_1 4
#define PARALLEL_2 5

void OnMult(int m_ar, int m_br)
{

    SYSTEMTIME Time1, Time2;

    char st[100];
    double temp;
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = (double)1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    Time1 = clock();

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

    Time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
    cout << st;

    // display 10 elements of the result matrix tto verify correctness
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

void loop(int option, int m_ar, int m_br, double *pha, double *phb, double *phc, int bkSize)
{
    int i, j, k, i2, j2, k2;

    switch (option)
    {
    case LINE:

        for (i = 0; i < m_ar; i++)
        {
            for (j = 0; j < m_br; j++)
            {
                for (k = 0; k < m_ar; k++)
                {
                    phc[i * m_ar + k] += pha[i * m_ar + j] * phb[j * m_br + k];
                }
            }
        }
        break;

    case BLOCK:

        for (i = 0; i < m_ar; i += bkSize)
        {
            for (j = 0; j < m_br; j += bkSize)
            {
                for (k = 0; k < m_ar; k += bkSize)
                {
                    for (i2 = i; i2 < min(i + bkSize, m_ar); i2++)
                    {
                        for (j2 = j; j2 < min(j + bkSize, m_br); j2++)
                        {
                            for (k2 = k; k2 < min(k + bkSize, m_ar); k2++)
                            {
                                phc[i2 * m_ar + k2] += pha[i2 * m_ar + j2] * phb[j2 * m_br + k2];
                            }
                        }
                    }
                }
            }
        }
        break;
    
    case PARALLEL_1:

        

        #pragma omp parallel for private(i, j, k)
        for (i = 0; i < m_ar; i++)
        {
            for (j = 0; j < m_br; j++)
            {
                for (k = 0; k < m_ar; k++)
                {
                    phc[i * m_ar + k] += pha[i * m_ar + j] * phb[j * m_br + k];
                }
            }
        }
        break;

    case PARALLEL_2:
        #pragma omp parallel private(i, j)
        for (i = 0; i < m_ar; i++)
        {
            for (j = 0; j < m_br; j++)
            {
                #pragma omp for
                for (k = 0; k < m_ar; k++)
                {
                    phc[i * m_ar + k] += pha[i * m_ar + j] * phb[j * m_br + k];
                }
            }
        }
        break;
    }
}

void OnMultLineNew(int m_ar, int m_br, int option, int bkSize = 0)
{
    SYSTEMTIME Time1, Time2;
    double OmpStart, OmpEnd;
    char st[100];
    int i, j, k;

    double *pha, *phb, *phc;

    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            pha[i * m_ar + j] = (double)1.0;

    for (i = 0; i < m_br; i++)
        for (j = 0; j < m_br; j++)
            phb[i * m_br + j] = (double)(i + 1);

    for (i = 0; i < m_ar; i++)
        for (j = 0; j < m_ar; j++)
            phc[i * m_ar + j] = (double)0.0;

    Time1 = clock();
    OmpStart = omp_get_wtime();

    loop(option, m_ar, m_br, pha, phb, phc, bkSize);

     if (option >= PARALLEL_1)
        Time2 = (double) omp_get_wtime();
    
        
    Time2 = clock();
    OmpEnd = omp_get_wtime();

    if (option >= PARALLEL_1)
        sprintf(st, "Time: %3.3f seconds\n", (double)(OmpEnd - OmpStart));
    else
        sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);


    cout << st;

    // display 10 elements of the result matrix tto verify correctness
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


void handle_error(int retval)
{
    printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
    exit(1);
}

void init_papi()
{
    int retval = PAPI_library_init(PAPI_VER_CURRENT);
    if (retval != PAPI_VER_CURRENT && retval < 0)
    {
        printf("PAPI library version mismatch!\n");
        exit(1);
    }
    if (retval < 0)
        handle_error(retval);

    std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
              << " MINOR: " << PAPI_VERSION_MINOR(retval)
              << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}

int main(int argc, char *argv[])
{

    char c;
    int lin, col, blockSize;
    int op;

    int EventSet = PAPI_NULL;
    long long values[2];
    int ret;

    ret = PAPI_library_init(PAPI_VER_CURRENT);
    if (ret != PAPI_VER_CURRENT)
        std::cout << "FAIL" << endl;

    ret = PAPI_create_eventset(&EventSet);
    if (ret != PAPI_OK)
        cout << "ERROR: create eventset" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L1_DCM" << endl;

    ret = PAPI_add_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        cout << "ERROR: PAPI_L2_DCM" << endl;

    op = 1;
    do
    {
        cout << endl
             << "1. Multiplication" << endl;
        cout << "2. Line Multiplication" << endl;
        cout << "3. Block Multiplication" << endl;
        cout << "4. Parallel Multiplication 1" << endl;
        cout << "5. Parallel Multiplication 2" << endl;
        cout << "Selection?: ";
        cin >> op;
        if (op == 0)
            break;
        printf("Dimensions: lins=cols ? ");
        cin >> lin;
        col = lin;

        // Start counting
        ret = PAPI_start(EventSet);
        if (ret != PAPI_OK)
            cout << "ERROR: Start PAPI" << endl;
        switch (op)
        {
        case SIMPLE:
            OnMult(lin, col);
            break;
        case BLOCK:
            cout << "Block Size? ";
            cin >> blockSize;
            OnMultLineNew(lin, col, op, blockSize);
            break;
        default:
            OnMultLineNew(lin, col, op);
            break;
        }

        ret = PAPI_stop(EventSet, values);
        if (ret != PAPI_OK)
            cout << "ERROR: Stop PAPI" << endl;
        printf("L1 DCM: %lld \n", values[0]);
        printf("L2 DCM: %lld \n", values[1]);

        ret = PAPI_reset(EventSet);
        if (ret != PAPI_OK)
            std::cout << "FAIL reset" << endl;

    } while (op != 0);

    ret = PAPI_remove_event(EventSet, PAPI_L1_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_remove_event(EventSet, PAPI_L2_DCM);
    if (ret != PAPI_OK)
        std::cout << "FAIL remove event" << endl;

    ret = PAPI_destroy_eventset(&EventSet);
    if (ret != PAPI_OK)
        std::cout << "FAIL destroy" << endl;
}