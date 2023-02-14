package pl.asie.ctif.platform;

import java.awt.Color;

public class PlatformComputerCraft extends Platform {
  public PlatformComputerCraft(boolean customPaletteAllowed) {
    super(2, 2, 3, 164, 81, customPaletteAllowed ? 16 : 0);
  }

  @Override
  public Color[] generatePalette() {
    Color[] colors = new Color[16];
    colors[0] = new Color(0xf0f0f0);
    colors[1] = new Color(0xf2b233);
    colors[2] = new Color(0xe57fd8);
    colors[3] = new Color(0x99b2f2);
    colors[4] = new Color(0xdede6c);
    colors[5] = new Color(0x7fcc19);
    colors[6] = new Color(0xf2b2cc);
    colors[7] = new Color(0x4c4c4c);
    colors[8] = new Color(0x999999);
    colors[9] = new Color(0x4c99b2);
    colors[10] = new Color(0xb266e5);
    colors[11] = new Color(0x3366cc);
    colors[12] = new Color(0x7f664c);
    colors[13] = new Color(0x57a64e);
    colors[14] = new Color(0xcc4c4c);
    colors[15] = new Color(0x0c0c0c);
    return colors;
  }
}
