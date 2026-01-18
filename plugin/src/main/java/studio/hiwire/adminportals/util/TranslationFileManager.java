package studio.hiwire.adminportals.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles file I/O for translation merging operations. Loads default translations from resources,
 * reads/writes user override files, and delegates the actual merging to {@link TranslationMerger}.
 */
public class TranslationFileManager {

  private final ClassLoader classLoader;
  private final TranslationMerger merger;

  public TranslationFileManager(ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.merger = new TranslationMerger();
  }

  /**
   * Merges a default translation file with the user's override file.
   *
   * @param resourcePath path to the resource in the classpath
   * @param targetPath path to the user's override file
   * @return merge result
   */
  public MergeResult merge(String resourcePath, Path targetPath) {
    List<String> defaultLines;
    try (InputStream in = classLoader.getResourceAsStream(resourcePath)) {
      if (in == null) {
        return MergeResult.resourceNotFound(resourcePath);
      }
      defaultLines = readAllLines(in);
    } catch (IOException e) {
      return MergeResult.error(e.getMessage());
    }

    try {
      if (!Files.exists(targetPath)) {
        Files.createDirectories(targetPath.getParent());
        List<String> result = merger.generateOverride(defaultLines);
        Files.write(targetPath, result, StandardCharsets.UTF_8);
        return MergeResult.createdOverride(targetPath, countKeys(defaultLines));
      }

      List<String> userLines = Files.readAllLines(targetPath, StandardCharsets.UTF_8);
      TranslationMerger.MergeOutput output = merger.merge(defaultLines, userLines);

      if (output.addedCount() == 0 && output.updatedCount() == 0) {
        return MergeResult.noChanges();
      }

      Files.write(targetPath, output.lines(), StandardCharsets.UTF_8);
      return MergeResult.merged(output.addedCount(), output.updatedCount());

    } catch (IOException e) {
      return MergeResult.error(e.getMessage());
    }
  }

  private List<String> readAllLines(InputStream in) throws IOException {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    return lines;
  }

  private int countKeys(List<String> lines) {
    int count = 0;
    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
        count++;
      }
    }
    return count;
  }

  public record MergeResult(Status status, String message, int count) {
    public enum Status {
      CREATED_OVERRIDE,
      MERGED,
      NO_CHANGES,
      RESOURCE_NOT_FOUND,
      ERROR
    }

    public static MergeResult createdOverride(Path path, int count) {
      return new MergeResult(
          Status.CREATED_OVERRIDE,
          "Created override file " + path + " (all translations commented out)",
          count);
    }

    public static MergeResult merged(int addedCount, int updatedCount) {
      int total = addedCount + updatedCount;
      String message = buildMergeMessage(addedCount, updatedCount);
      return new MergeResult(Status.MERGED, message, total);
    }

    private static String buildMergeMessage(int addedCount, int updatedCount) {
      List<String> parts = new ArrayList<>();
      if (addedCount > 0) {
        parts.add("Added " + addedCount + " new translation(s)");
      }
      if (updatedCount > 0) {
        parts.add("Updated " + updatedCount + " commented translation(s)");
      }
      return String.join(", ", parts);
    }

    public static MergeResult noChanges() {
      return new MergeResult(Status.NO_CHANGES, "No new translations", 0);
    }

    public static MergeResult resourceNotFound(String path) {
      return new MergeResult(Status.RESOURCE_NOT_FOUND, "Resource not found: " + path, 0);
    }

    public static MergeResult error(String message) {
      return new MergeResult(Status.ERROR, message, 0);
    }

    public boolean isSuccess() {
      return status == Status.CREATED_OVERRIDE
          || status == Status.MERGED
          || status == Status.NO_CHANGES;
    }
  }
}
