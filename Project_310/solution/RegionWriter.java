import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Writes region records without creating a String per record. */
final class RegionWriter {
    private final OutputStream out;
    private final byte[] buf = new byte[1 << 16];
    private int n;

    RegionWriter(OutputStream out) {
        this.out = out;
    }

    void write(Model m, RegionList regions) throws IOException {
        byte[][] labels = new byte[m.names.length][];
        int maxLabel = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = m.names[i].getBytes(StandardCharsets.UTF_8);
            maxLabel = Math.max(maxLabel, labels[i].length);
        }
        final int maxRecord = 6 * 5 + maxLabel + 1; // six numbers with commas, label, newline
        final boolean is2D = m.is2D();

        for (int i = 0, size = regions.size(); i < size; i++) {
            if (n + maxRecord > buf.length) {
                flushBuffer();
            }
            long r = regions.raw(i);
            putInt(RegionList.x(r));
            putInt(RegionList.y(r));
            if (!is2D) {
                putInt(RegionList.z(r));
            }
            putInt(RegionList.w(r));
            putInt(RegionList.h(r));
            if (!is2D) {
                putInt(RegionList.d(r));
            }
            byte[] label = labels[RegionList.label(r)];
            System.arraycopy(label, 0, buf, n, label.length);
            n += label.length;
            buf[n++] = '\n';
        }
        flushBuffer();
        out.flush();
    }

    /** Appends a value in 0..4096 followed by a comma. */
    private void putInt(int v) {
        if (v >= 1000) {
            buf[n++] = (byte) ('0' + v / 1000);
        }
        if (v >= 100) {
            buf[n++] = (byte) ('0' + v / 100 % 10);
        }
        if (v >= 10) {
            buf[n++] = (byte) ('0' + v / 10 % 10);
        }
        buf[n++] = (byte) ('0' + v % 10);
        buf[n++] = ',';
    }

    private void flushBuffer() throws IOException {
        out.write(buf, 0, n);
        n = 0;
    }
}
