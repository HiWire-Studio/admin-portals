package studio.hiwire.adminportals.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pure merge algorithm for translation files. Takes default and user translation lines and
 * produces merged output. No file I/O - use {@link TranslationFileManager} for file operations.
 */
public class TranslationMerger {

  private static final Pattern VALID_KEY_PATTERN = Pattern.compile("[\\w.]+");
  private static final String COMMENT_PREFIX = "# ";

  private sealed interface ParsedLine {
    String raw();

    record BlankLine(String raw) implements ParsedLine {}

    record CommentLine(String raw, String extractedKey, String extractedValue)
        implements ParsedLine {}

    record ActiveLine(String raw, String key, String value) implements ParsedLine {}
  }

  public record MergeOutput(List<String> lines, int addedCount, int updatedCount) {}

  /**
   * Generates an override file with all translations commented out.
   *
   * @param defaultLines lines from the default translation file
   * @return lines for the override file with all translations commented out
   */
  public List<String> generateOverride(List<String> defaultLines) {
    List<ParsedLine> parsed = parseLines(defaultLines);
    List<String> result = new ArrayList<>();
    for (ParsedLine line : parsed) {
      result.add(commentOutLine(line.raw()));
    }
    return normalizeEmptyLines(result);
  }

  /**
   * Merges default translations with user's override file.
   *
   * @param defaultLines lines from the default translation file
   * @param userLines lines from the user's override file
   * @return merge output containing the merged lines and statistics
   */
  public MergeOutput merge(List<String> defaultLines, List<String> userLines) {
    List<ParsedLine> defaultParsed = parseLines(defaultLines);
    List<ParsedLine> userParsed = parseLines(userLines);

    List<String> defaultKeyOrder = extractKeyOrder(defaultParsed);
    Map<String, Integer> userKeyPositions = buildKeyPositionMap(userParsed);
    Map<String, String> defaultValues = buildDefaultValuesMap(defaultParsed);
    Map<String, TranslationEntry> defaultEntries = buildEntryMap(defaultParsed);

    List<String> newKeys = new ArrayList<>();
    for (String key : defaultKeyOrder) {
      if (!userKeyPositions.containsKey(key)) {
        newKeys.add(key);
      }
    }

    List<String> result = new ArrayList<>();
    for (ParsedLine line : userParsed) {
      result.add(line.raw());
    }

    for (String newKey : newKeys) {
      TranslationEntry entry = defaultEntries.get(newKey);
      int insertAt = findInsertPosition(newKey, defaultKeyOrder, userParsed);

      List<String> linesToInsert = new ArrayList<>();
      for (String contextLine : entry.precedingLines()) {
        linesToInsert.add(commentOutLine(contextLine));
      }
      linesToInsert.add(commentOutLine(entry.line()));

      for (int i = 0; i < linesToInsert.size(); i++) {
        result.add(insertAt + i, linesToInsert.get(i));
      }

      userParsed = parseLines(result);
    }

    int updatedCount = updateOutdatedCommentedKeys(result, userParsed, defaultValues);

    List<String> normalized = normalizeEmptyLines(result);
    return new MergeOutput(normalized, newKeys.size(), updatedCount);
  }

  private List<ParsedLine> parseLines(List<String> lines) {
    List<ParsedLine> result = new ArrayList<>();
    for (String line : lines) {
      result.add(parseLine(line));
    }
    return result;
  }

  private ParsedLine parseLine(String line) {
    String trimmed = line.trim();

    if (trimmed.isEmpty()) {
      return new ParsedLine.BlankLine(line);
    }

    if (trimmed.startsWith("#")) {
      String[] keyValue = extractKeyValueFromComment(trimmed);
      return new ParsedLine.CommentLine(line, keyValue[0], keyValue[1]);
    }

    int eq = trimmed.indexOf('=');
    if (eq > 0) {
      String key = trimmed.substring(0, eq).trim();
      String value = trimmed.substring(eq + 1);
      return new ParsedLine.ActiveLine(line, key, value);
    }

    return new ParsedLine.CommentLine(line, null, null);
  }

  private String[] extractKeyValueFromComment(String line) {
    String stripped = line.replaceFirst("^#\\s*", "");
    int eq = stripped.indexOf('=');
    if (eq > 0) {
      String potentialKey = stripped.substring(0, eq).trim();
      if (VALID_KEY_PATTERN.matcher(potentialKey).matches()) {
        String value = stripped.substring(eq + 1);
        return new String[] {potentialKey, value};
      }
    }
    return new String[] {null, null};
  }

  private List<String> extractKeyOrder(List<ParsedLine> parsed) {
    List<String> keys = new ArrayList<>();
    for (ParsedLine line : parsed) {
      if (line instanceof ParsedLine.ActiveLine active) {
        keys.add(active.key());
      }
    }
    return keys;
  }

  private Map<String, Integer> buildKeyPositionMap(List<ParsedLine> parsed) {
    Map<String, Integer> positions = new LinkedHashMap<>();
    for (int i = 0; i < parsed.size(); i++) {
      ParsedLine line = parsed.get(i);
      String key = getKeyFromLine(line);
      if (key != null && !positions.containsKey(key)) {
        positions.put(key, i);
      }
    }
    return positions;
  }

  private String getKeyFromLine(ParsedLine line) {
    return switch (line) {
      case ParsedLine.ActiveLine active -> active.key();
      case ParsedLine.CommentLine comment -> comment.extractedKey();
      case ParsedLine.BlankLine ignored -> null;
    };
  }

  private record TranslationEntry(String line, List<String> precedingLines) {}

  private Map<String, TranslationEntry> buildEntryMap(List<ParsedLine> parsed) {
    Map<String, TranslationEntry> entries = new LinkedHashMap<>();
    List<String> context = new ArrayList<>();

    for (ParsedLine line : parsed) {
      if (line instanceof ParsedLine.BlankLine || line instanceof ParsedLine.CommentLine) {
        context.add(line.raw());
      } else if (line instanceof ParsedLine.ActiveLine active) {
        entries.put(active.key(), new TranslationEntry(active.raw(), new ArrayList<>(context)));
        context.clear();
      }
    }
    return entries;
  }

  private Map<String, String> buildDefaultValuesMap(List<ParsedLine> parsed) {
    Map<String, String> values = new LinkedHashMap<>();
    for (ParsedLine line : parsed) {
      if (line instanceof ParsedLine.ActiveLine active) {
        values.put(active.key(), active.value());
      }
    }
    return values;
  }

  private int updateOutdatedCommentedKeys(
      List<String> result, List<ParsedLine> parsed, Map<String, String> defaultValues) {
    int updatedCount = 0;

    for (int i = 0; i < parsed.size(); i++) {
      ParsedLine line = parsed.get(i);
      if (line instanceof ParsedLine.CommentLine comment
          && comment.extractedKey() != null
          && comment.extractedValue() != null) {
        String key = comment.extractedKey();
        String defaultValue = defaultValues.get(key);

        if (defaultValue != null && !defaultValue.equals(comment.extractedValue())) {
          String newLine = COMMENT_PREFIX + key + "=" + defaultValue;
          result.set(i, newLine);
          updatedCount++;
        }
      }
    }

    return updatedCount;
  }

  private String commentOutLine(String line) {
    if (line.trim().isEmpty()) {
      return line;
    }
    if (line.trim().startsWith("#")) {
      return line;
    }
    return COMMENT_PREFIX + line;
  }

  private int findInsertPosition(
      String newKey, List<String> defaultKeyOrder, List<ParsedLine> userParsed) {
    int keyIndex = defaultKeyOrder.indexOf(newKey);

    String prevKey = keyIndex > 0 ? defaultKeyOrder.get(keyIndex - 1) : null;
    String nextKey =
        keyIndex < defaultKeyOrder.size() - 1 ? defaultKeyOrder.get(keyIndex + 1) : null;

    if (prevKey != null) {
      for (int i = 0; i < userParsed.size(); i++) {
        String lineKey = getKeyFromLine(userParsed.get(i));
        if (prevKey.equals(lineKey)) {
          return i + 1;
        }
      }
    }

    if (nextKey != null) {
      for (int i = 0; i < userParsed.size(); i++) {
        String lineKey = getKeyFromLine(userParsed.get(i));
        if (nextKey.equals(lineKey)) {
          return findContextStart(i, userParsed);
        }
      }
    }

    return userParsed.size();
  }

  private int findContextStart(int keyLineIndex, List<ParsedLine> lines) {
    int start = keyLineIndex;
    while (start > 0) {
      ParsedLine prevLine = lines.get(start - 1);
      if (prevLine instanceof ParsedLine.BlankLine || prevLine instanceof ParsedLine.CommentLine) {
        start--;
      } else {
        break;
      }
    }
    return start;
  }

  private List<String> normalizeEmptyLines(List<String> lines) {
    List<String> result = new ArrayList<>();
    boolean lastWasEmpty = false;

    for (String line : lines) {
      boolean isEmpty = line.trim().isEmpty();
      if (isEmpty) {
        if (!lastWasEmpty) {
          result.add(line);
        }
        lastWasEmpty = true;
      } else {
        result.add(line);
        lastWasEmpty = false;
      }
    }

    while (!result.isEmpty() && result.getLast().trim().isEmpty()) {
      result.removeLast();
    }

    return result;
  }
}
