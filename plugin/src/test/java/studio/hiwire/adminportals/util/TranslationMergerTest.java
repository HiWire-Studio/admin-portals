package studio.hiwire.adminportals.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TranslationMergerTest {

  private TranslationMerger merger;

  @BeforeEach
  void setUp() {
    merger = new TranslationMerger();
  }

  @Nested
  class GenerateOverride {

    @Test
    void shouldCommentOutAllTranslationLines() {
      List<String> defaults = List.of("Key.One=Value one", "Key.Two=Value two");

      List<String> result = merger.generateOverride(defaults);

      assertEquals(List.of("# Key.One=Value one", "# Key.Two=Value two"), result);
    }

    @Test
    void shouldPreserveBlankLines() {
      List<String> defaults = List.of("Key.One=Value one", "", "Key.Two=Value two");

      List<String> result = merger.generateOverride(defaults);

      assertEquals(List.of("# Key.One=Value one", "", "# Key.Two=Value two"), result);
    }

    @Test
    void shouldPreserveExistingComments() {
      List<String> defaults = List.of("# Section header", "Key.One=Value one");

      List<String> result = merger.generateOverride(defaults);

      assertEquals(List.of("# Section header", "# Key.One=Value one"), result);
    }

    @Test
    void shouldNormalizeConsecutiveEmptyLines() {
      List<String> defaults = List.of("Key.One=Value one", "", "", "", "Key.Two=Value two");

      List<String> result = merger.generateOverride(defaults);

      assertEquals(List.of("# Key.One=Value one", "", "# Key.Two=Value two"), result);
    }

    @Test
    void shouldRemoveTrailingEmptyLines() {
      List<String> defaults = List.of("Key.One=Value one", "");

      List<String> result = merger.generateOverride(defaults);

      assertEquals(List.of("# Key.One=Value one"), result);
    }

    @Test
    void shouldHandleEmptyInput() {
      List<String> defaults = List.of();

      List<String> result = merger.generateOverride(defaults);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class Merge {

    @Test
    void shouldReturnNoChangesWhenUpToDate() {
      List<String> defaults = List.of("Key.One=Value one", "Key.Two=Value two");
      List<String> user = List.of("# Key.One=Value one", "# Key.Two=Value two");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(user, output.lines());
    }

    @Test
    void shouldAddNewKeyAfterPreviousSibling() {
      List<String> defaults =
          List.of("Key.One=Value one", "Key.Two=Value two", "Key.Three=Value three");
      List<String> user = List.of("# Key.One=Value one", "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(1, output.addedCount());
      assertEquals(
          List.of("# Key.One=Value one", "# Key.Two=Value two", "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldAddNewKeyBeforeNextSiblingWhenNoPreviousFound() {
      List<String> defaults =
          List.of("Key.One=Value one", "Key.Two=Value two", "Key.Three=Value three");
      List<String> user = List.of("# Key.Two=Value two", "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(1, output.addedCount());
      assertEquals(
          List.of("# Key.One=Value one", "# Key.Two=Value two", "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldAddNewKeyAtEndWhenNoSiblingsFound() {
      List<String> defaults = List.of("Key.One=Value one", "Key.Two=Value two");
      List<String> user = List.of("# Custom.Key=Custom value");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(2, output.addedCount());
      assertEquals(
          List.of("# Custom.Key=Custom value", "# Key.One=Value one", "# Key.Two=Value two"),
          output.lines());
    }

    @Test
    void shouldAddNewKeyWithContext() {
      List<String> defaults =
          List.of(
              "Key.One=Value one",
              "",
              "# Description for key two",
              "Key.Two=Value two",
              "",
              "Key.Three=Value three");
      List<String> user = List.of("# Key.One=Value one", "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(1, output.addedCount());
      assertEquals(
          List.of(
              "# Key.One=Value one",
              "",
              "# Description for key two",
              "# Key.Two=Value two",
              "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldUpdateOutdatedCommentedKey() {
      List<String> defaults = List.of("Key.One=New value");
      List<String> user = List.of("# Key.One=Old value");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(1, output.updatedCount());
      assertEquals(List.of("# Key.One=New value"), output.lines());
    }

    @Test
    void shouldNotUpdateActiveUserTranslation() {
      List<String> defaults = List.of("Key.One=Default value");
      List<String> user = List.of("Key.One=User custom value");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(List.of("Key.One=User custom value"), output.lines());
    }

    @Test
    void shouldPreserveUserCustomKeys() {
      List<String> defaults = List.of("Key.One=Value one");
      List<String> user = List.of("# Key.One=Value one", "Custom.Key=Custom value");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(List.of("# Key.One=Value one", "Custom.Key=Custom value"), output.lines());
    }

    @Test
    void shouldHandleMixedActiveAndCommentedKeys() {
      List<String> defaults =
          List.of("Key.One=Value one", "Key.Two=Value two", "Key.Three=Value three");
      List<String> user =
          List.of("# Key.One=Value one", "Key.Two=User override", "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(
          List.of("# Key.One=Value one", "Key.Two=User override", "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldNormalizeEmptyLinesInResult() {
      List<String> defaults = List.of("Key.One=Value one", "Key.Two=Value two");
      List<String> user = List.of("# Key.One=Value one", "", "", "# Key.Two=Value two");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(List.of("# Key.One=Value one", "", "# Key.Two=Value two"), output.lines());
    }

    @Test
    void shouldInsertBeforeNextKeyContext() {
      List<String> defaults =
          List.of(
              "Key.One=Value one",
              "Key.Two=Value two",
              "",
              "# Section: Three",
              "Key.Three=Value three");
      List<String> user =
          List.of("# Key.One=Value one", "", "# Section: Three", "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(1, output.addedCount());
      assertEquals(
          List.of(
              "# Key.One=Value one",
              "# Key.Two=Value two",
              "",
              "# Section: Three",
              "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldHandleMultipleNewKeys() {
      List<String> defaults =
          List.of(
              "Key.One=Value one",
              "Key.Two=Value two",
              "Key.Three=Value three",
              "Key.Four=Value four");
      List<String> user = List.of("# Key.One=Value one", "# Key.Four=Value four");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(2, output.addedCount());
      assertEquals(
          List.of(
              "# Key.One=Value one",
              "# Key.Two=Value two",
              "# Key.Three=Value three",
              "# Key.Four=Value four"),
          output.lines());
    }

    @Test
    void shouldHandleEmptyUserFile() {
      List<String> defaults = List.of("Key.One=Value one", "Key.Two=Value two");
      List<String> user = List.of();

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(2, output.addedCount());
      assertEquals(List.of("# Key.One=Value one", "# Key.Two=Value two"), output.lines());
    }

    @Test
    void shouldHandleEmptyDefaultFile() {
      List<String> defaults = List.of();
      List<String> user = List.of("# Key.One=Value one");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(List.of("# Key.One=Value one"), output.lines());
    }

    @Test
    void shouldUpdateMultipleOutdatedCommentedKeys() {
      List<String> defaults = List.of("Key.One=New one", "Key.Two=New two");
      List<String> user = List.of("# Key.One=Old one", "# Key.Two=Old two");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(2, output.updatedCount());
      assertEquals(List.of("# Key.One=New one", "# Key.Two=New two"), output.lines());
    }

    @Test
    void shouldHandleKeysWithDotsAndUnderscores() {
      List<String> defaults = List.of("Command.Portal_Config.Save=Save portal");
      List<String> user = List.of("# Command.Portal_Config.Save=Old save");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(1, output.updatedCount());
      assertEquals(List.of("# Command.Portal_Config.Save=Save portal"), output.lines());
    }

    @Test
    void shouldNotTreatProseCommentsAsKeys() {
      List<String> defaults = List.of("Key.One=Value one");
      List<String> user = List.of("# This is a description comment", "# Key.One=Value one");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(
          List.of("# This is a description comment", "# Key.One=Value one"), output.lines());
    }

    @Test
    void shouldHandleValuesWithSpecialCharacters() {
      List<String> defaults = List.of("Key.One=Value with = sign and {placeholder}");
      List<String> user = List.of("# Key.One=Old value");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(1, output.updatedCount());
      assertEquals(List.of("# Key.One=Value with = sign and {placeholder}"), output.lines());
    }

    @Test
    void shouldPreserveUserAddedComments() {
      List<String> defaults =
          List.of("Key.One=Value one", "Key.Two=Value two", "Key.Three=Value three");
      List<String> user =
          List.of(
              "# === My Custom Section ===",
              "# Key.One=Value one",
              "",
              "# User's note: I customized this one",
              "Key.Two=My custom value",
              "",
              "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(0, output.addedCount());
      assertEquals(0, output.updatedCount());
      assertEquals(
          List.of(
              "# === My Custom Section ===",
              "# Key.One=Value one",
              "",
              "# User's note: I customized this one",
              "Key.Two=My custom value",
              "",
              "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldMergeDefaultCommentsWithUserStructure() {
      List<String> defaults =
          List.of(
              "Key.One=Value one",
              "",
              "# Important: This is a new feature",
              "# Use with caution",
              "Key.Two=Value two",
              "",
              "Key.Three=Value three");
      List<String> user =
          List.of(
              "# === User's Custom Header ===",
              "# Key.One=Value one",
              "",
              "# === User's Section for Three ===",
              "# Key.Three=Value three");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(1, output.addedCount());
      assertEquals(
          List.of(
              "# === User's Custom Header ===",
              "# Key.One=Value one",
              "",
              "# Important: This is a new feature",
              "# Use with caution",
              "# Key.Two=Value two",
              "",
              "# === User's Section for Three ===",
              "# Key.Three=Value three"),
          output.lines());
    }

    @Test
    void shouldInsertNewKeyWithDefaultContextBeforeUserContext() {
      List<String> defaults =
          List.of(
              "# Section A",
              "Key.A=Value A",
              "",
              "# Section B - New",
              "Key.B=Value B",
              "",
              "# Section C",
              "Key.C=Value C");
      List<String> user =
          List.of(
              "# Section A",
              "# Key.A=Value A",
              "",
              "# User added this section header for C",
              "# Section C",
              "# Key.C=Value C");

      TranslationMerger.MergeOutput output = merger.merge(defaults, user);

      assertEquals(1, output.addedCount());
      assertEquals(
          List.of(
              "# Section A",
              "# Key.A=Value A",
              "",
              "# Section B - New",
              "# Key.B=Value B",
              "",
              "# User added this section header for C",
              "# Section C",
              "# Key.C=Value C"),
          output.lines());
    }
  }
}
