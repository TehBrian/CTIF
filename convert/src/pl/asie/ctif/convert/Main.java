package pl.asie.ctif.convert;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import pl.asie.ctif.convert.colorspace.AbstractColorspace;
import pl.asie.ctif.convert.colorspace.Colorspace;
import pl.asie.ctif.convert.platform.AbstractPlatform;
import pl.asie.ctif.convert.platform.Platform;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
  private static class Parameters {
    @Parameter(names = {"--palette-sampling-resolution"}, description = "The sampling resolution for palette generation. 0 = full image (1/4x1/4 image in -O3+)")
    private int paletteSamplingResolution = 0;

    @Parameter(names = {"--threads"}, description = "Amount of threads to create")
    private int threads = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"--palette-export"}, description = "File to export the palette to.")
    private String paletteExport;

    @Parameter(names = {"--palette"}, description = "File to load the palette from.")
    private String palette;

    @Parameter(names = {"-m", "--mode"}, description = "Target platform (cc, cc-paletted, oc-tier2, oc-tier3)")
    private Platform mode = Platform.OC_TIER_3;

    @Parameter(names = {"-O", "--optimization-level"}, description = "Optimization level [primarily 0-4]. Larger levels = less accurate but faster generated images. Default is 1.")
    private int optimizationLevel = 1;

    @Parameter(names = {"--colorspace"}, description = "Colorspace (rgb, yuv, yiq)")
    private Colorspace colorspace = Colorspace.YIQ;

    @Parameter(names = {"--dither-mode"}, description = "Dither mode (none, error, ordered)")
    private Converter.DitherMode ditherMode = Converter.DitherMode.ERROR;

    @Parameter(names = {"--dither-type"}, description = "Dither type (error: floyd-steinberg, sierra-lite; ordered: 2x2, 4x4, 8x8)")
    private String ditherType;

    @Parameter(names = {"--dither-level"}, description = "Dither level for error-type dither. 0 = off, 1 = full (default)")
    private float ditherLevel = 1.0f;

    @Parameter(names = {"-d", "--debug"}, description = "Enable debugging", hidden = true)
    private boolean debug = false;

    @Parameter(names = {"-W", "--width"}, description = "Output image width")
    private int w;

    @Parameter(names = {"-H", "--height"}, description = "Output image height")
    private int h;

    @Parameter(names = {"-N", "--no-aspect"}, description = "Ignore aspect ratio")
    private boolean ignoreAspectRatio = false;

    @Parameter(names = {"-o", "--output"}, description = "Output filename")
    private String outputFilename;

    @Parameter(names = {"-P", "--preview"}, description = "Preview image filename")
    private String previewFilename;

    @Parameter(description = "Input file")
    private List<String> files = new ArrayList<>();

    @Parameter(names = {"-h", "--help"}, description = "Print usage", help = true)
    private boolean help;

    @Parameter(names = {"--resize-mode"}, description = "Resize mode")
    private Resizer.Mode resizeMode;
  }

  private static int rCeil(int x, int y) {
    if (x % y > 0) {
      return x - (x % y) + y;
    } else {
      return x;
    }
  }

  public record Result(BufferedImage image, ByteArrayOutputStream data) {
  }

  public static Result convertImage(
      final AbstractPlatform platform,
      final AbstractColorspace colorspace,
      final int optimizationLevel,
      final boolean debug,
      final BufferedImage image,
      int w,
      int h,
      final boolean ignoreAspectRatio,
      final int threads,
      final Resizer.Mode resizeMode,
      final String palette,
      final int paletteSamplingResolution,
      final String paletteExport,
      String ditherType,
      final Float ditherLevel,
      Converter.DitherMode ditherMode,
      final String previewFilename
  ) {
    Color[] platformPalette = platform.getPalette();

    // adjusting dither type.
    if (ditherType == null) {
      if (ditherMode == Converter.DitherMode.ORDERED) {
        ditherType = "4x4";
      } else {
        ditherType = "floyd-steinberg";
      }
    }

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

    int width = w;
    int height = h;

    if (debug) {
      System.out.println("Using " + threads + " threads.");
    }

    System.out.println("Resizing image...");
    long timeR = System.currentTimeMillis();

    BufferedImage resizedImage;
    if (image.getWidth() == width && image.getHeight() == height) {
      resizedImage = image;
    } else if (resizeMode == Resizer.Mode.SPEED) {
      resizedImage = Resizer.speedyResize(image, width, height);
    } else {
      resizedImage = Resizer.qualityResize(image, width, height, resizeMode == Resizer.Mode.QUALITY_NATIVE);
    }

    timeR = System.currentTimeMillis() - timeR;
    if (debug) {
      System.out.println("Image resize time: " + timeR + " ms");
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

    float[] ditherArray = DitherArrays.get(ditherType.toLowerCase());
    if (ditherLevel == 0) {
      ditherMode = Converter.DitherMode.NONE;
    } else if (ditherLevel != 1) {
      ditherArray = Arrays.copyOf(ditherArray, ditherArray.length);

      switch (ditherMode) {
        case ERROR:
          for (int i = 0; i < ditherArray.length; i++) {
            ditherArray[i] *= ditherLevel;
          }
          break;
        case ORDERED:
          float newScale = ditherLevel;
          float newOffset = ditherArray.length * ((1 - ditherLevel) / 2.0f);
          for (int i = 0; i < ditherArray.length; i++) {
            if (ditherArray[i] > 0) {
              ditherArray[i] = (ditherArray[i] - 1) * newScale + newOffset;
            }
          }
          break;
      }
    }

    try {
      System.out.println("Converting image...");

      long time = System.currentTimeMillis();

      Converter writer = new Converter(
          resizedImage,
          platformPalette,
          ditherMode,
          ditherArray,
          platform,
          colorspace,
          optimizationLevel
      );

      ByteArrayOutputStream outputData = new ByteArrayOutputStream();
      BufferedImage outputImage = writer.write(outputData);

      time = System.currentTimeMillis() - time;
      if (debug) {
        System.out.println("Image conversion time: " + time + " ms");
      }

      if (previewFilename != null) {
        Util.saveImage(Resizer.speedyResize(outputImage, width * 2, height * 2), new File(previewFilename).getAbsolutePath());
      }

      return new Result(outputImage, outputData);
    } catch (Exception e) {
      e.printStackTrace();
      return new Result(null, null);
    }
  }

  public static void main(String[] args) {
    Parameters params = new Parameters();
    JCommander jCommander = new JCommander(params);
    jCommander.parse(args);

    if (params.help) {
      jCommander.usage();
      System.exit(0);
    }

    if (params.files.size() == 0) {
      System.err.println("You must specify an input file.");
      System.exit(1);
    }

    String inputPath = params.files.get(0);
    BufferedImage input = Util.loadImage(inputPath);
    if (input == null) {
      System.err.printf("Failed to read input image: %s%n", inputPath);
      System.exit(1);
    }

    Result result = convertImage(
        params.mode.get(),
        params.colorspace.get(),
        params.optimizationLevel,
        params.debug,
        input,
        params.w,
        params.h,
        params.ignoreAspectRatio,
        params.threads,
        params.resizeMode,
        params.palette,
        params.paletteSamplingResolution,
        params.paletteExport,
        params.ditherType,
        params.ditherLevel,
        params.ditherMode,
        params.previewFilename
    );

    String outputName = params.outputFilename != null ? params.outputFilename : params.files.get(0) + ".ctif";
    try {
      Files.write(Path.of(outputName), result.data().toByteArray());
    } catch (IOException e) {
      System.err.printf("Failed to write output image: %s%n", outputName);
      e.printStackTrace();
      System.exit(1);
    }
  }
}
