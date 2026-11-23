/** Shared, read-only map data. Cell index: (z * Y + y) * X + x. */
final class Model {
    final int X, Y, Z, PX, PY, PZ;
    final byte[] cell;
    final String[] names;

    Model(int X, int Y, int Z, int PX, int PY, int PZ, byte[] cell, String[] names) {
        this.X = X;
        this.Y = Y;
        this.Z = Z;
        this.PX = PX;
        this.PY = PY;
        this.PZ = PZ;
        this.cell = cell;
        this.names = names;
    }

    int idx(int x, int y, int z) {
        return (z * Y + y) * X + x;
    }

    boolean is2D() {
        return Z == 1;
    }

    int cellCount() {
        return cell.length;
    }
}
