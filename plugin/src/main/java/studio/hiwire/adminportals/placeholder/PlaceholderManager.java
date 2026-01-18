package studio.hiwire.adminportals.placeholder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PlaceholderManager {

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

  private final Map<String, PlaceholderProvider> placeholders = new HashMap<>();

  public PlaceholderManager() {
    registerDefaults();
  }

  public void register(@NonNullDecl String name, @NonNullDecl PlaceholderProvider provider) {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Placeholder name cannot be blank");
    }
    if (placeholders.containsKey(name)) {
      throw new IllegalArgumentException("Placeholder '" + name + "' is already registered");
    }
    placeholders.put(name, provider);
  }

  public boolean unregister(@NonNullDecl String name) {
    if (name.isBlank()) {
      throw new IllegalArgumentException("Placeholder name cannot be blank");
    }
    return placeholders.remove(name) != null;
  }

  public boolean isRegistered(@NonNullDecl String name) {
    return placeholders.containsKey(name);
  }

  public Set<String> getRegisteredNames() {
    return Collections.unmodifiableSet(placeholders.keySet());
  }

  public String process(@NonNullDecl String input, @NonNullDecl PlaceholderContext context) {
    if (input.isEmpty()) {
      return input;
    }

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String placeholderName = matcher.group(1);
      PlaceholderProvider provider = placeholders.get(placeholderName);

      String replacement =
          provider != null
              ? provider.resolve(context)
              : matcher.group(0); // Keep original if not found

      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * Validates if all placeholders in the input string are registered.
   *
   * @param input the string containing placeholders to validate
   * @return a set of placeholder names that are used but not registered, empty if all are valid
   */
  public Set<String> findMissingPlaceholders(@NonNullDecl String input) {
    if (input.isEmpty()) {
      return Collections.emptySet();
    }

    Set<String> missing = new HashSet<>();
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);

    while (matcher.find()) {
      String placeholderName = matcher.group(1);
      if (!placeholders.containsKey(placeholderName)) {
        missing.add(placeholderName);
      }
    }

    return missing;
  }

  private void registerDefaults() {
    register("PlayerUsername", ctx -> ctx.playerRef().getUsername());
    register("PlayerUuid", ctx -> ctx.playerRef().getUuid().toString());

    register("PosX", ctx -> String.valueOf(ctx.pos().x));
    register("PosY", ctx -> String.valueOf(ctx.pos().y));
    register("PosZ", ctx -> String.valueOf(ctx.pos().z));

    register("WorldName", ctx -> ctx.world().getName());
  }
}
