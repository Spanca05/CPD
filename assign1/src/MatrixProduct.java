import java.util.Scanner;

public class MatrixProduct {

    // ==========================
    // Standard Multiplication
    // ==========================
    public static void onMult(int m_ar, int m_br) {

        long startTime, endTime;
        double[] A = new double[m_ar * m_ar];
        double[] B = new double[m_ar * m_ar];
        double[] C = new double[m_ar * m_ar];
        double temp;

        // Initialize matrix A
        for (int i = 0; i < m_ar; i++)
            for (int j = 0; j < m_ar; j++)
                A[i * m_ar + j] = 1.0;

        // Initialize matrix B
        for (int i = 0; i < m_br; i++)
            for (int j = 0; j < m_br; j++)
                B[i * m_br + j] = (double) (i + 1);

        startTime = System.nanoTime();

        for (int i = 0; i < m_ar; i++)
        {
            for (int j = 0; j < m_br; j++)
            {
                temp = 0;
                for (int k = 0; k < m_ar; k++)
                {
                    temp += A[i * m_ar + k] * B[k * m_br + j];
                }
                C[i * m_ar + j] = temp;
            }
        }


        endTime = System.nanoTime();

        double seconds = (endTime - startTime) / 1e9;
        System.out.printf("Time: %.3f seconds%n", seconds);

        System.out.println("Result matrix:");
        for (int j = 0; j < Math.min(10, m_br); j++)
            System.out.print(C[j] + " ");
        System.out.println();
    }

    // ==========================
    // Line-by-line Multiplication
    // ==========================
    public static void onMultLine(int m_ar, int m_br) {

        long startTime, endTime;
        double[] A = new double[m_ar * m_ar];
        double[] B = new double[m_ar * m_ar];
        double[] C = new double[m_ar * m_ar];
        double temp;

        // Initialize matrices (same as above)
        for (int i = 0; i < m_ar; i++)
            for (int j = 0; j < m_ar; j++)
                A[i * m_ar + j] = 1.0;

        for (int i = 0; i < m_br; i++)
            for (int j = 0; j < m_br; j++)
                B[i * m_br + j] = (double) (i + 1);

        // Initialize C to zero
        for (int i = 0; i < m_ar * m_br; i++)
            C[i] = 0.0;

        startTime = System.nanoTime();

        for (int i = 0; i < m_ar; i++) {
            for (int k = 0; k < m_ar; k++) {
                temp = 0;
                for (int j = 0; j < m_br; j++) {
                    temp = A[i * m_ar + k] * B[k * m_br + j];
                    C[i * m_ar + j] += temp;
                }
            }
        }

        endTime = System.nanoTime();

        double seconds = (endTime - startTime) / 1e9;
        System.out.printf("Time: %.3f seconds%n", seconds);

        System.out.println("Result matrix:");
        for (int j = 0; j < Math.min(10, m_br); j++)
            System.out.print(C[j] + " ");
        System.out.println();
    }

    // ==========================
    // Block Multiplication
    // ==========================
    public static void onMultBlock(int m_ar, int m_br, int blockSize) {

        long startTime, endTime;
        double[] A = new double[m_ar * m_ar];
        double[] B = new double[m_ar * m_ar];
        double[] C = new double[m_ar * m_ar];

        // Initialize matrices
        for (int i = 0; i < m_ar; i++)
            for (int j = 0; j < m_ar; j++)
                A[i * m_ar + j] = 1.0;

        for (int i = 0; i < m_br; i++)
            for (int j = 0; j < m_br; j++)
                B[i * m_br + j] = (double) (i + 1);

        // Initialize C to zero
        for (int i = 0; i < m_ar * m_br; i++)
            C[i] = 0.0;

        startTime = System.nanoTime();

        // TODO: Implement block multiplication here

        for (int ii = 0; ii < m_ar; ii += blockSize)
        {
            for (int jj = 0; jj < m_br; jj += blockSize)
            {
                for (int kk = 0; kk < m_ar; kk += blockSize)
                {
                    for (int i = ii; i < Math.min(ii + blockSize, m_ar); i++)
                    {
                        for (int j = jj; j < Math.min(jj + blockSize, m_br); j++)
                        {
                            double sum = 0.0;
                            for (int k = kk; k < Math.min(kk + blockSize, m_ar); k++)
                            {
                                sum += A[i * m_ar + k] * B[k * m_br + j];
                            }
                            C[i * m_br + j] += sum;
                        }
                    }
                }
            }
        }

        endTime = System.nanoTime();

        double seconds = (endTime - startTime) / 1e9;
        System.out.printf("Time: %.3f seconds%n", seconds);

        System.out.println("Result matrix:");
        for (int j = 0; j < Math.min(10, m_br); j++)
            System.out.print(C[j] + " ");
        System.out.println();
    }

    // ==========================
    // Main Menu
    // ==========================
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        int lin, col, blockSize;
        int op;

        do {
            System.out.println("\n1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.println("3. Block Multiplication");
            System.out.println("0. Exit");
            System.out.print("Selection?: ");

            op = scanner.nextInt();

            if (op == 0)
                break;

            System.out.print("Dimensions: lins=cols ? ");
            lin = scanner.nextInt();
            col = lin;

            switch (op) {
                case 1:
                    onMult(lin, col);
                    break;
                case 2:
                    onMultLine(lin, col);
                    break;
                case 3:
                    System.out.print("Block Size? ");
                    blockSize = scanner.nextInt();
                    onMultBlock(lin, col, blockSize);
                    break;
            }

        } while (op != 0);

        scanner.close();
    }
}