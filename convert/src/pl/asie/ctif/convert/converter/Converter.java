package pl.asie.ctif.convert.converter;

import pl.asie.ctif.convert.colorspace.AbstractColorspace;
import pl.asie.ctif.convert.platform.AbstractPlatform;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Converter {
  public record Result(BufferedImage image, ByteArrayOutputStream data) {
  }

  public static Result convertImage(
      final boolean debug,
      final AbstractPlatform platform,
      final int optimizationLevel,
      final AbstractColorspace colorspace,
      final int threads,
      final BufferedImage image,
      int w,
      int h,
      final boolean ignoreAspectRatio,
      final Resizer.Mode resizeMode,
      UglyConverter.DitherMode ditherMode,
      String ditherType,
      final Float ditherLevel,
      final int paletteSamplingResolution,
      final String paletteExport,
      final String palette
  ) throws IOException {
    Color[] platformPalette = platform.getPalette();

    // if width/height was set, max at platform char width/height.
    w = (w > 0) ? rCeil(w, platform.getCharWidth()) : 0;
    h = (h > 0) ? rCeil(h, platform.getCharHeight()) : 0;

    // default to platform width/height.
    if (w == 0) w = platform.getWidthPx();
    if (h == 0) h = platform.getHeightPx();

    if (!ignoreAspectRatio) {
      float x = (float) image.getWidth() / image.getHeight();
      float y = 1.0f;
      float a = Math.min(Math.min(
              (float) w / x,
              (float) h / y
          ),
          (float) Math.sqrt((float) platform.getCharsPx() / (x * y)));
      w = rCeil((int) Math.floor(x * a), platform.getCharWidth());
      h = rCeil((int) Math.floor(y * a), platform.getCharHeight());
    }

    if (w * h > platform.getCharsPx()) {
      System.err.printf("Size too large: %dx%d (maximum size: %d pixels)%n", w, h, platform.getCharsPx());
      System.exit(1);
    } else if (w > platform.getWidthPx()) {
      System.err.printf("Width too large: %d (maximum width: %d)%n", w, platform.getWidthPx());
      System.exit(1);
    } else if (h > platform.getHeightPx()) {
      System.err.printf("Height too large: %d (maximum height: %d)%n", h, platform.getHeightPx());
      System.exit(1);
    }

    if (debug) {
      System.out.println("Using " + threads + " threads.");
    }

    System.out.println("Resizing image...");
    long resizeTime = System.currentTimeMillis();

    BufferedImage resizedImage;
    if (image.getWidth() == w && image.getHeight() == h) {
      resizedImage = image;
    } else if (resizeMode == Resizer.Mode.SPEED) {
      resizedImage = Resizer.speedyResize(image, w, h);
    } else {
      resizedImage = Resizer.qualityResize(image, w, h, resizeMode == Resizer.Mode.QUALITY_NATIVE);
    }

    resizeTime = System.currentTimeMillis() - resizeTime;
    if (debug) {
      System.out.println("Image resize time: " + resizeTime + "ms");
    }

    if (platform.getCustomColorCount() > 0) {
      if (palette != null) {
        System.out.println("Reading palette...");
        try {
          FileInputStream inputStream = new FileInputStream(palette);
          for (int i = 0; i < platform.getCustomColorCount(); i++) {
            int red = inputStream.read();
            int green = inputStream.read();
            int blue = inputStream.read();
            platformPalette[i] = new Color(red, green, blue);
          }
          inputStream.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        long time = System.currentTimeMillis();
        System.out.println("Generating palette...");
        PaletteGeneratorKMeans generator = new PaletteGeneratorKMeans(resizedImage, platformPalette, platform.getCustomColorCount(), paletteSamplingResolution, colorspace, optimizationLevel, debug);
        platformPalette = generator.generate(threads);
        time = System.currentTimeMillis() - time;
        if (debug) {
          System.out.println("Palette generation time: " + time + " ms");
        }
      }

      if (paletteExport != null) {
        System.out.println("Saving palette...");
        try {
          FileOutputStream outputStream = new FileOutputStream(paletteExport);
          for (final Color color : platformPalette) {
            outputStream.write(color.getRed());
            outputStream.write(color.getGreen());
            outputStream.write(color.getBlue());
          }
          outputStream.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    // default dither type.
    if (ditherType == null) {
      if (ditherMode == UglyConverter.DitherMode.ORDERED) {
        ditherType = "4x4";
      } else {
        ditherType = "floyd-steinberg";
      }
    }

    float[] ditherArray = DitherArrays.get(ditherType.toLowerCase());
    if (ditherLevel == 0) {
      ditherMode = UglyConverter.DitherMode.NONE;
    } else if (ditherLevel != 1) {
      ditherArray = Arrays.copyOf(ditherArray, ditherArray.length);

      switch (ditherMode) {
        case ERROR -> {
          for (int i = 0; i < ditherArray.length; i++) {
            ditherArray[i] *= ditherLevel;
          }
        }
        case ORDERED -> {
          float newScale = ditherLevel;
          float newOffset = ditherArray.length * ((1 - ditherLevel) / 2.0f);
          for (int i = 0; i < ditherArray.length; i++) {
            if (ditherArray[i] > 0) {
              ditherArray[i] = (ditherArray[i] - 1) * newScale + newOffset;
            }
          }
        }
      }
    }

    ByteArrayOutputStream outputData = new ByteArrayOutputStream();
    BufferedImage outputImage;

    System.out.println("Converting image...");
    final long startTime = System.currentTimeMillis();

    try {
      UglyConverter writer = new UglyConverter(
          resizedImage,
          platformPalette,
          ditherMode,
          ditherArray,
          platform,
          colorspace,
          optimizationLevel
      );

      outputImage = writer.write(outputData);
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }

    final long timeSpent = System.currentTimeMillis() - startTime;
    if (debug) {
      System.out.println("Image conversion time: " + timeSpent + " ms");
    }

    return new Result(outputImage, outputData);
  }

  private static int rCeil(int x, int y) {
    if (x % y > 0) {
      return x - (x % y) + y;
    } else {
      return x;
    }
  }
}
