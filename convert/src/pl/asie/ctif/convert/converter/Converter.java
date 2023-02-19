package pl.asie.ctif.convert.converter;

import pl.asie.ctif.convert.Stopwatch;
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

  private record Size(int width, int height) {
  }

  private record Dither(UglyConverter.DitherMode mode, String type, float level, float[] array) {
  }

  public static Result convertImage(
      final boolean debug,
      final AbstractPlatform platform,
      final int optimizationLevel,
      final AbstractColorspace colorspace,
      final int threads,
      final BufferedImage image,
      final int width,
      final int height,
      final boolean ignoreAspectRatio,
      final Resizer.Mode resizeMode,
      final UglyConverter.DitherMode ditherMode,
      final String ditherType,
      final Float ditherLevel,
      final int paletteSamplingResolution,
      final String paletteExport,
      final String palette
  ) {
    if (debug) {
      System.out.println("Using " + threads + " threads.");
    }

    final Size size = determineSize(
        width,
        height,
        platform,
        ignoreAspectRatio,
        image
    );
    checkSize(size, platform);

    System.out.println("Resizing image...");
    final var resizeStopwatch = Stopwatch.started();
    final BufferedImage resizedImage = resizeImage(
        image,
        size.width(),
        size.height(),
        resizeMode
    );
    if (debug) {
      System.out.println("Image resize time: " + resizeStopwatch.timeElapsed().toMillis() + "ms");
    }

    final Color[] customPalette = determinePalette(
        debug,
        platform,
        optimizationLevel,
        colorspace,
        threads,
        resizedImage,
        paletteSamplingResolution,
        paletteExport,
        palette
    );

    final Dither dither = determineDither(
        ditherMode,
        ditherType,
        ditherLevel
    );

    System.out.println("Converting image...");
    final var convertStopwatch = Stopwatch.started();
    final Result result = doUglyConversion(
        resizedImage,
        customPalette,
        dither.mode(),
        dither.array(),
        platform,
        colorspace,
        optimizationLevel
    );
    if (debug) {
      System.out.println("Image conversion time: " + convertStopwatch.timeElapsed().toMillis() + " ms");
    }
    return result;
  }

  private static int rCeil(int x, int y) {
    if (x % y > 0) {
      return x - (x % y) + y;
    } else {
      return x;
    }
  }

  private static Size determineSize(
      final int width,
      final int height,
      final AbstractPlatform platform,
      final boolean ignoreAspectRatio,
      final BufferedImage image
  ) {
    int w = width;
    int h = height;

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

    return new Size(w, h);
  }

  private static void checkSize(
      final Size size,
      final AbstractPlatform platform
  ) {
    final int w = size.width();
    final int h = size.height();
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
  }

  private static BufferedImage resizeImage(
      final BufferedImage image,
      final int width,
      final int height,
      final Resizer.Mode resizeMode
  ) {
    final BufferedImage result;

    if (image.getWidth() == width && image.getHeight() == height) {
      result = image;
    } else if (resizeMode == Resizer.Mode.SPEED) {
      result = Resizer.speedyResize(image, width, height);
    } else {
      result = Resizer.qualityResize(image, width, height, resizeMode == Resizer.Mode.QUALITY_NATIVE);
    }

    return result;
  }

  private static Color[] determinePalette(
      final boolean debug,
      final AbstractPlatform platform,
      final int optimizationLevel,
      final AbstractColorspace colorspace,
      final int threads,
      final BufferedImage resizedImage,
      final int paletteSamplingResolution,
      final String paletteExport,
      final String palette
  ) {
    Color[] result = new Color[platform.getCustomColorCount()];

    if (platform.getCustomColorCount() > 0) {
      // hooray! we get to use custom colors.
      if (palette != null) {
        // user-provided palette.
        System.out.println("Reading palette...");
        try (FileInputStream inputStream = new FileInputStream(palette)) {
          for (int i = 0; i < platform.getCustomColorCount(); i++) {
            int red = inputStream.read();
            int green = inputStream.read();
            int blue = inputStream.read();
            result[i] = new Color(red, green, blue);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        // generate our own.
        System.out.println("Generating palette...");
        final var stopwatch = Stopwatch.started();

        PaletteGeneratorKMeans generator = new PaletteGeneratorKMeans(resizedImage, platform.getPalette(), platform.getCustomColorCount(), paletteSamplingResolution, colorspace, optimizationLevel, debug);
        result = generator.generate(threads);

        if (debug) {
          System.out.println("Palette generation time: " + stopwatch.timeElapsed().toMillis() + " ms");
        }
      }

      if (paletteExport != null) {
        System.out.println("Saving palette...");
        try (FileOutputStream outputStream = new FileOutputStream(paletteExport)) {
          for (final Color color : result) {
            outputStream.write(color.getRed());
            outputStream.write(color.getGreen());
            outputStream.write(color.getBlue());
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      result = platform.getPalette();
    }

    return result;
  }

  private static Dither determineDither(
      UglyConverter.DitherMode ditherMode,
      String ditherType,
      final float ditherLevel
  ) {
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
          float newOffset = ditherArray.length * ((1 - ditherLevel) / 2.0f);
          for (int i = 0; i < ditherArray.length; i++) {
            if (ditherArray[i] > 0) {
              ditherArray[i] = (ditherArray[i] - 1) * ditherLevel + newOffset;
            }
          }
        }
      }
    }

    return new Dither(ditherMode, ditherType, ditherLevel, ditherArray);
  }

  private static Result doUglyConversion(
      final BufferedImage resizedImage,
      final Color[] customPalette,
      final UglyConverter.DitherMode ditherMode,
      final float[] ditherArray,
      final AbstractPlatform platform,
      final AbstractColorspace colorspace,
      final int optimizationLevel
  ) {
    ByteArrayOutputStream outputData = new ByteArrayOutputStream();
    BufferedImage outputImage;

    try {
      final var uglyConverter = new UglyConverter(
          resizedImage,
          customPalette,
          ditherMode,
          ditherArray,
          platform,
          colorspace,
          optimizationLevel
      );
      outputImage = uglyConverter.write(outputData);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return new Result(outputImage, outputData);
  }
}
