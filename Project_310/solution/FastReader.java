import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Streaming byte parser for the map format; tolerates CRLF, extra spaces and separators. */
final class FastReader {
    private static final byte UNKNOWN = -1;
    private static final byte SKIP = -2;

    private final InputStream in;
    private final byte[] buf = new byte[1 << 16];
    private int pos, len;

    FastReader(InputStream in) {
        this.in = in;
    }

    Model readModel() throws IOException {
        String header = nextNonBlankLine();
        if (header == null) {
            throw new IOException("Empty input");
        }
        String[] parts = header.split(",");
        if (parts.length != 6) {
            throw new IOException("Header must have 6 fields: " + header);
        }
        int[] dim = new int[6];
        for (int i = 0; i < 6; i++) {
            dim[i] = Integer.parseInt(parts[i].trim());
        }

        byte[] lookup = new byte[256];
        Arrays.fill(lookup, UNKNOWN);
        lookup[' '] = lookup['\t'] = lookup['\r'] = lookup['\n'] = lookup[','] = SKIP;

        List<String> names = new ArrayList<>();
        String line = nextNonBlankLine();
        while (line != null && !line.isEmpty()) {
            int comma = line.indexOf(',');
            if (comma != 1) {
                throw new IOException("Bad tag line: " + line);
            }
            lookup[line.charAt(0) & 0xFF] = (byte) names.size();
            names.add(line.substring(comma + 1).trim());
            line = readTrimmedLine();
        }
        if (names.isEmpty()) {
            throw new IOException("Missing tag mapping");
        }

        byte[] cell = new byte[dim[0] * dim[1] * dim[2]];
        readCells(cell, lookup);
        return new Model(dim[0], dim[1], dim[2], dim[3], dim[4], dim[5],
                cell, names.toArray(new String[0]));
    }

    private void readCells(byte[] cell, byte[] lookup) throws IOException {
        int n = 0;
        while (n < cell.length) {
            if (pos == len && !fill()) {
                throw new IOException("Expected " + cell.length + " cells, found " + n);
            }
            final byte[] b = buf;
            final int end = len;
            int p = pos;
            while (p < end && n < cell.length) {
                byte value = lookup[b[p++] & 0xFF];
                if (value >= 0) {
                    cell[n++] = value;
                } else if (value == UNKNOWN) {
                    throw new IOException("Unknown tag at cell " + n);
                }
            }
            pos = p;
        }
    }

    private String nextNonBlankLine() throws IOException {
        String line;
        do {
            line = readTrimmedLine();
        } while (line != null && line.isEmpty());
        return line;
    }

    /** Reads one header-section line; returns null at end of input. */
    private String readTrimmedLine() throws IOException {
        byte[] tmp = new byte[64];
        int n = 0;
        while (true) {
            if (pos == len && !fill()) {
                return n == 0 ? null : new String(tmp, 0, n, StandardCharsets.UTF_8).trim();
            }
            byte c = buf[pos++];
            if (c == '\n') {
                return new String(tmp, 0, n, StandardCharsets.UTF_8).trim();
            }
            if (n == tmp.length) {
                tmp = Arrays.copyOf(tmp, n * 2);
            }
            tmp[n++] = c;
        }
    }

    private boolean fill() throws IOException {
        len = in.read(buf, 0, buf.length);
        pos = 0;
        if (len <= 0) {
            len = 0;
            return false;
        }
        return true;
    }
}
