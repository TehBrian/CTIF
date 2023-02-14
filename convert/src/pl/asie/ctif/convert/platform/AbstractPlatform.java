package pl.asie.ctif.convert.platform;

import java.awt.Color;

public abstract class AbstractPlatform {
  private final int platformId, charWidth, charHeight, width, height, customColorCount;
  private Color[] palette = null;

  protected AbstractPlatform(int platformId, int charWidth, int charHeight, int width, int height, int customColorCount) {
    this.platformId = platformId;
    this.charWidth = charWidth;
    this.charHeight = charHeight;
    this.width = width;
    this.height = height;
    this.customColorCount = customColorCount;
  }

  public int getPlatformId() {
    return platformId;
  }

  public int getCharWidth() {
    return charWidth;
  }

  public int getCharHeight() {
    return charHeight;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getChars() {
    return getWidth() * getHeight();
  }

  public final int getWidthPx() {
    return getWidth() * getCharWidth();
  }

  public final int getHeightPx() {
    return getHeight() * getCharHeight();
  }

  public final int getCharsPx() {
    return getChars() * getCharWidth() * getCharHeight();
  }

  public int getCustomColorCount() {
    return customColorCount;
  }

  public Color[] getPalette() {
    if (palette == null) {
      palette = generatePalette();
    }
    return palette.clone();
  }

  protected abstract Color[] generatePalette();
}
