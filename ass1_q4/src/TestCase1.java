void testcase2() {
    int cachePower = 16; // cache size = 2^16B
    int blockPower = 5; // block size = 2^5B
    int N = 256;
    int[][] Z = new int[N][N];
    String cacheType = "DirectMapped";
    for (int i = 0; i < N; i += 1) {
        for (int j = 0; j < N; j += 1) {
            Z[i][j] = 0;
        }
    }
}
