package dev.titantech.ltrdiag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
public final class TraceLog {
  private static final Path FILE = Path.of("logs","littletiles-render-diagnostics.log");
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
  private static final AtomicInteger COUNT = new AtomicInteger();
  private TraceLog() {}
  public static synchronized void record(String event) {
    if (COUNT.incrementAndGet() > 30000) return;
    try {
      Files.createDirectories(FILE.getParent());
      Files.writeString(FILE,LocalDateTime.now().format(FMT)+" "+event+"\n",
          StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    } catch(IOException e) { System.err.println("[LittleTilesRenderDiagnostics] "+e); }
  }
}
