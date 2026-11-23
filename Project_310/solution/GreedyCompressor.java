import java.util.Arrays;

/** Baseline and safety net: grows each region along x, then y, then z. Not thread-safe. */
final class GreedyCompressor implements BlockCompressor {
    private boolean[] covered = new boolean[0];

    @Override
    public void compress(Model m, int bx, int by, int bz, RegionList out) {
        final int PX = m.PX, PY = m.PY, PZ = m.PZ, X = m.X, XY = m.X * m.Y;
        final byte[] cell = m.cell;
        final int blockSize = PX * PY * PZ;
        if (covered.length < blockSize) {
            covered = new boolean[blockSize];
        } else {
            Arrays.fill(covered, 0, blockSize, false);
        }
        final boolean[] done = covered;
        final int PXY = PX * PY;

        for (int lz = 0; lz < PZ; lz++) {
            for (int ly = 0; ly < PY; ly++) {
                int lx = 0;
                while (lx < PX) {
                    int v = (lz * PY + ly) * PX + lx;
                    if (done[v]) {
                        lx++;
                        continue;
                    }
                    int i = m.idx(bx + lx, by + ly, bz + lz);
                    byte label = cell[i];

                    int w = 1;
                    while (lx + w < PX && !done[v + w] && cell[i + w] == label) {
                        w++;
                    }

                    int h = 1;
                    while (ly + h < PY && rowMatches(cell, done, i + h * X, v + h * PX, w, label)) {
                        h++;
                    }

                    int d = 1;
                    while (lz + d < PZ && layerMatches(cell, done,
                            i + d * XY, v + d * PXY, w, h, X, PX, label)) {
                        d++;
                    }

                    for (int c = 0; c < d; c++) {
                        for (int b = 0; b < h; b++) {
                            int start = v + c * PXY + b * PX;
                            Arrays.fill(done, start, start + w, true);
                        }
                    }
                    out.add(bx + lx, by + ly, bz + lz, w, h, d, label);
                    lx += w;
                }
            }
        }
    }

    private static boolean rowMatches(byte[] cell, boolean[] done, int i, int v, int w, byte label) {
        for (int k = 0; k < w; k++) {
            if (done[v + k] || cell[i + k] != label) {
                return false;
            }
        }
        return true;
    }

    private static boolean layerMatches(byte[] cell, boolean[] done, int i, int v,
                                        int w, int h, int X, int PX, byte label) {
        for (int b = 0; b < h; b++) {
            if (!rowMatches(cell, done, i + b * X, v + b * PX, w, label)) {
                return false;
            }
        }
        return true;
    }
}
