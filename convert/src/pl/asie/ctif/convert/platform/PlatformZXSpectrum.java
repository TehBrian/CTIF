package pl.asie.ctif.convert.platform;

import java.awt.Color;

public class PlatformZXSpectrum extends AbstractPlatform {
  private final int paletteMode;

  public PlatformZXSpectrum(int paletteMode) {
    super(3, 8, 8, 256 / 8, 192 / 8, 0);
    this.paletteMode = paletteMode;
  }

  @Override
  public Color[] generatePalette() {
    Color[] colors = new Color[paletteMode == 2 ? 16 : 8];
    colors[0] = new Color(0x000000);
    colors[1] = new Color(0x0000ff);
    colors[2] = new Color(0xff0000);
    colors[3] = new Color(0xff00ff);
    colors[4] = new Color(0x00ff00);
    colors[5] = new Color(0x00ffff);
    colors[6] = new Color(0xffff00);
    colors[7] = new Color(0xffffff);
    if (paletteMode > 0) {
      for (int i = 0; i < 8; i++) {
        Color c = colors[i];
        colors[i] = new Color(c.getRGB() & 0x00d7d7d7);
        if (paletteMode == 2) colors[8 + i] = c;
      }
    }
    return colors;
  }
}
