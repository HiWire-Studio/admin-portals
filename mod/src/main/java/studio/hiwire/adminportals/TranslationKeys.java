package studio.hiwire.adminportals;

/**
 * Contains translation key prefixes and common parameter names for the AdminPortals plugin.
 */
public final class TranslationKeys {

  private TranslationKeys() {}

  // Translation key prefixes
  public static final String CHAT_MESSAGES = "HiWire.AdminPortals.ChatMessages";
  public static final String UI = "HiWire.AdminPortals.UI";
  public static final String ITEMS = "HiWire.AdminPortals.Items";

  /** Common parameter keys used in translations. */
  public static final class Params {
    private Params() {}

    public static final String MOD_PREFIX = "ModPrefix";
    public static final String DETAIL = "Detail";
    public static final String PLACEHOLDER_LIST = "PlaceholderList";
    public static final String PLACEHOLDER_AMOUNT = "PlaceholderAmount";
    public static final String PERMISSION = "Permission";
  }
}
