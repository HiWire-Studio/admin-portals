package studio.hiwire.adminportals.placeholder;

@FunctionalInterface
public interface PlaceholderProvider {

  String resolve(PlaceholderContext context);
}
