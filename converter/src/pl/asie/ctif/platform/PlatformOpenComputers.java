package pl.asie.ctif.platform;

import java.awt.*;

public class PlatformOpenComputers extends Platform {
    public final int tier;

    public PlatformOpenComputers(int tier) {
        super(1, 2, 4, 0, 0, 0);
        this.tier = tier;
    }

    @Override
    public int getCustomColorCount() {
        return switch (tier) {
            case 1 -> 0;
            case 2, 3 -> 16;
            default -> 0;
        };
    }

    @Override
    public int getWidth() {
        return switch (tier) {
            case 1 -> 40;
            case 2 -> 80;
            case 3 -> 160;
            default -> 0;
        };
    }

    protected int getRealHeight() {
        return switch (tier) {
            case 1, 2 -> 25;
            case 3 -> 50;
            default -> 0;
        };
    }

    @Override
    public int getHeight() {
        return getWidth();
    }

    @Override
    public float getDefaultAspectRatio() {
        return (float) getWidth() / getRealHeight();
    }

    @Override
    public int getChars() {
        return getWidth() * getRealHeight();
    }

    @Override
    Color[] generatePalette() {
        Color[] colors = new Color[getColorCount()];
        if (tier == 1) {
            colors[0] = new Color(0, 0, 0);
            colors[1] = new Color(255, 255, 255);
        } else {
            for (int i = 0; i < 16; i++) {
                colors[i] = new Color(17 * i, 17 * i, 17 * i);
            }

            if (tier == 3) {
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

    protected int getColorCount() {
        return switch (tier) {
            case 1 -> 2;
            case 2 -> 16;
            case 3 -> 256;
            default -> 0;
        };
    }
}
