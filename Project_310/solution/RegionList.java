import java.util.Arrays;

/**
 * Growable list of regions, each packed into one long (63 bits):
 * x:12 | y:12 | z:6 | (w-1):12 | (h-1):12 | (d-1):6 | label:3.
 * A checkerboard of 4,194,304 cells therefore needs about 34 MB of payload.
 */
final class RegionList {
    private long[] data;
    private int size;

    RegionList(int initialCapacity) {
        data = new long[Math.max(16, initialCapacity)];
    }

    void add(int x, int y, int z, int w, int h, int d, int label) {
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
        data[size++] = ((long) x << 51) | ((long) y << 39) | ((long) z << 33)
                | ((long) (w - 1) << 21) | ((long) (h - 1) << 9) | ((long) (d - 1) << 3) | label;
    }

    int size() {
        return size;
    }

    /** Drops every region added after the list had {@code newSize} entries. */
    void truncate(int newSize) {
        size = newSize;
    }

    long raw(int i) {
        return data[i];
    }

    static int x(long r) { return (int) (r >>> 51) & 0xFFF; }
    static int y(long r) { return (int) (r >>> 39) & 0xFFF; }
    static int z(long r) { return (int) (r >>> 33) & 0x3F; }
    static int w(long r) { return ((int) (r >>> 21) & 0xFFF) + 1; }
    static int h(long r) { return ((int) (r >>> 9) & 0xFFF) + 1; }
    static int d(long r) { return ((int) (r >>> 3) & 0x3F) + 1; }
    static int label(long r) { return (int) r & 0x7; }
}
