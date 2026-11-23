/**
 * Common interface for every compression algorithm.
 * Covers exactly the parent block whose minimum corner is (bx, by, bz)
 * and whose size is (m.PX, m.PY, m.PZ), appending the regions to {@code out}.
 */
interface BlockCompressor {
    void compress(Model m, int bx, int by, int bz, RegionList out);
}
