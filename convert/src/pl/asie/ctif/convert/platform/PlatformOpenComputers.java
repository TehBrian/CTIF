package pl.asie.ctif.convert.platform;

import java.awt.Color;

public class PlatformOpenComputers extends AbstractPlatform {
  public enum Screen {
    TIER_1,
    TIER_2,
    TIER_3
  }

  private final Screen screen;

  public PlatformOpenComputers(Screen screen) {
    super(1, 2, 4, 0, 0, 0);
    this.screen = screen;
  }

  @Override
  public int getWidth() {
    return switch (screen) {
      case TIER_1 -> 50;
      case TIER_2 -> 80;
      case TIER_3 -> 160;
    };
  }

  @Override
  public int getHeight() {
    return getWidth();
  }

  @Override
  public int getChars() {
    return getWidth() * getRealHeight();
  }

  @Override
  public int getCustomColorCount() {
    return switch (screen) {
      case TIER_1 -> 0;
      case TIER_2, TIER_3 -> 16;
    };
  }

  @Override
  protected Color[] generatePalette() {
    Color[] colors = new Color[getColorCount()];
    if (screen == Screen.TIER_1) {
      colors[0] = new Color(0, 0, 0);
      colors[1] = new Color(255, 255, 255);
    } else {
      for (int i = 0; i < 16; i++) {
        colors[i] = new Color(17 * i, 17 * i, 17 * i);
      }

      if (screen == Screen.TIER_3) {
        for (int i = 0; i < 240; i++) {
          colors[i + 16] = new Color(
              ((i / 40) % 6) * 255 / 5,
              ((i / 5) % 8) * 255 / 7,
              (i % 5) * 255 / 4
          );
        }
      }
    }
    return colors;
  }

  public Screen getScreen() {
    return screen;
  }

  private int getColorCount() {
    return switch (screen) {
      case TIER_1 -> 2;
      case TIER_2 -> 16;
      case TIER_3 -> 256;
    };
  }

  private int getRealHeight() {
    return switch (screen) {
      case TIER_1 -> 16;
      case TIER_2 -> 25;
      case TIER_3 -> 50;
    };
  }
}
