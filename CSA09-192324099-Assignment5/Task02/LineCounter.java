import java.io.*;
import java.nio.file.*;

public class LineCounter {
    static long countUnbuffered(Path p) throws IOException {
        long lines = 0;
        try (InputStream in = new FileInputStream(p.toFile())) {
            int value;
            while ((value = in.read()) != -1) if (value == '\n') lines++;
        }
        return lines;
    }

    static long countBuffered(Path p) throws IOException {
        long lines = 0;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream in = new BufferedInputStream(Files.newInputStream(p), buffer.length)) {
            int count;
            while ((count = in.read(buffer)) != -1)
                for (int i = 0; i < count; i++) if (buffer[i] == '\n') lines++;
        }
        return lines;
    }

    public static void main(String[] args) throws Exception {
        Path p = Files.createTempFile("attendance", ".txt");
        String row = "192311001,Present\n";
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            for (int i = 0; i < 100000; i++) w.write(row);
        }
        long start = System.nanoTime();
        long a = countUnbuffered(p);
        long t1 = System.nanoTime() - start;
        start = System.nanoTime();
        long b = countBuffered(p);
        long t2 = System.nanoTime() - start;
        System.out.println("Unbuffered lines: " + a + ", time ms: " + t1 / 1_000_000.0);
        System.out.println("Buffered lines: " + b + ", time ms: " + t2 / 1_000_000.0);
        Files.deleteIfExists(p);
    }
}
