package dev.titantech.littlespore.trace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public final class TraceLog {
    private static final Path FILE = Path.of("logs", "littletiles-break-trace.log");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static long last = 0;
    private static int emitted = 0;
    private TraceLog() {}

    public static synchronized void record(String message) {
        try {
            Files.createDirectories(FILE.getParent());
            String line = LocalDateTime.now().format(CLOCK) + " " + message + "\n";
            Files.writeString(FILE, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("[LittleTilesBreakTracer] Failed writing trace: " + e);
        }
    }

    public static synchronized void change(String kind, Object pos, Object from, Object to) {
        long now = System.currentTimeMillis();
        if (now - last > 1000) { last = now; emitted = 0; }
        if (++emitted > 120) return;
        String stack = StackWalker.getInstance().walk(frames ->
            frames.limit(28).map(f -> "  at " + f.getClassName() + "." + f.getMethodName() +
                "(" + f.getFileName() + ":" + f.getLineNumber() + ")")
                .collect(Collectors.joining("\n")));
        record("CHANGE " + kind + " pos=" + pos + " old=" + from + " new=" + to +
            " thread=" + Thread.currentThread().getName() + "\n" + stack);
    }
}
