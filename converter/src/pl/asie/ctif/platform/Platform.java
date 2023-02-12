package pl.asie.ctif.platform;

import java.awt.Color;

public abstract class Platform {
    private final int platformId, charWidth, charHeight, width, height, customColorCount;
    private Color[] palette = null;

    protected Platform(int platformId, int charWidth, int charHeight, int width, int height, int customColorCount) {
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

    public float getDefaultAspectRatio() {
        return (float) getWidth() / getHeight();
    }

    public Color[] getPalette() {
        if (palette == null) {
            palette = generatePalette();
        }
        return palette.clone();
    }

    abstract Color[] generatePalette();
}
