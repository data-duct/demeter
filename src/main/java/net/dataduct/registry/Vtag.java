package net.dataduct.registry;

import io.micrometer.core.instrument.Tag;
import java.util.List;
import java.util.stream.Collectors;

public class Vtag {

  public String key;
  public String value;

  private Vtag(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public static Vtag of(String key, String value) {
    return new Vtag(key, value);
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public static List<Tag> toTags(List<Vtag> vtags) {
    return vtags.stream().map((v) -> Tag.of(v.getKey(), v.getValue())).collect(Collectors.toList());
  }
}
