import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

public final class Main {
    /** After this much time, remaining blocks use the fallback. Re-measure once C's code is in. */
    private static final long OPTIMIZER_BUDGET_NANOS = 3_500_000_000L;

    public static void main(String[] args) throws IOException {
        final long start = System.nanoTime();
        Model model = new FastReader(System.in).readModel();

        // Integration point: replace this with C's algorithm.
        BlockCompressor optimizer = new GreedyCompressor();
        BlockCompressor fallback = new GreedyCompressor();

        RegionList regions = new RegionList(Math.min(1 << 16, model.cellCount()));
        for (int bz = 0; bz < model.Z; bz += model.PZ) {
            for (int by = 0; by < model.Y; by += model.PY) {
                for (int bx = 0; bx < model.X; bx += model.PX) {
                    compressBlock(model, bx, by, bz, optimizer, fallback, regions, start);
                }
            }
        }

        try (FileOutputStream out = new FileOutputStream(FileDescriptor.out)) {
            new RegionWriter(out).write(model, regions);
        }
    }

    private static void compressBlock(Model m, int bx, int by, int bz, BlockCompressor optimizer,
                                      BlockCompressor fallback, RegionList out, long start) {
        if (System.nanoTime() - start > OPTIMIZER_BUDGET_NANOS) {
            fallback.compress(m, bx, by, bz, out);
            return;
        }
        int mark = out.size();
        try {
            optimizer.compress(m, bx, by, bz, out);
        } catch (Throwable t) {
            out.truncate(mark); // discard the failed block's partial result
            fallback.compress(m, bx, by, bz, out);
        }
    }
}
