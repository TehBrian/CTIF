package pl.asie.ctif.convert;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import pl.asie.ctif.convert.colorspace.Colorspace;
import pl.asie.ctif.convert.converter.Converter;
import pl.asie.ctif.convert.converter.Resizer;
import pl.asie.ctif.convert.converter.UglyConverter;
import pl.asie.ctif.convert.platform.Platform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  @SuppressWarnings("FieldMayBeFinal")
  private static class Parameters {
    /* meta. */
    @Parameter(names = {"-d", "--debug"}, description = "Enable debugging.", hidden = true)
    private boolean debug = false;

    @Parameter(names = {"-h", "--help"}, description = "Print usage.", help = true)
    private boolean help;

    /* miscellaneous. */
    @Parameter(names = {"-m", "--mode"}, description = "Target platform.")
    private Platform mode = Platform.OC_TIER_3;

    @Parameter(names = {"-O", "--optimization-level"}, description = "Optimization level. Primarily 0-4. Larger levels are less accurate but generate faster.")
    private int optimizationLevel = 1;

    @Parameter(names = {"--colorspace"}, description = "Image colorspace.")
    private Colorspace colorspace = Colorspace.YIQ;

    @Parameter(names = {"--threads"}, description = "Amount of threads to create.")
    private int threads = Runtime.getRuntime().availableProcessors();

    /* files. */
    @Parameter(names = {"-o", "--output"}, description = "Output image filename. The image will be in CTIF format.")
    private String outputFilename;

    @Parameter(names = {"-P", "--preview"}, description = "Preview image filename. The image will be in PNG format.")
    private String previewFilename;

    @Parameter(description = "Input image filename. The image must be in PNG format.")
    private String inputFilename;

    /* sizing. */
    @Parameter(names = {"-W", "--width"}, description = "Output image width.")
    private int w;

    @Parameter(names = {"-H", "--height"}, description = "Output image height.")
    private int h;

    @Parameter(names = {"-N", "--no-aspect"}, description = "Ignore aspect ratio.")
    private boolean ignoreAspectRatio = false;

    @Parameter(names = {"--resize-mode"}, description = "Resize mode.")
    private Resizer.Mode resizeMode;

    /* dither. */
    @Parameter(names = {"--dither-mode"}, description = "Dither mode.")
    private UglyConverter.DitherMode ditherMode = UglyConverter.DitherMode.ERROR;

    @Parameter(names = {"--dither-type"}, description = "Dither type. (error: floyd-steinberg, sierra-lite; ordered: 2x2, 4x4, 8x8)")
    private String ditherType;

    @Parameter(names = {"--dither-level"}, description = "Dither level for error-type dither. 0 = off, 1 = full.")
    private float ditherLevel = 1.0f;

    /* palette. */
    @Parameter(names = {"--palette-sampling-resolution"}, description = "The sampling resolution for palette generation. 0 means full image. (1/4x1/4 image in -O3+)")
    private int paletteSamplingResolution = 0;

    @Parameter(names = {"--palette-export"}, description = "File to export the palette to.")
    private String paletteExport;

    @Parameter(names = {"--palette"}, description = "File to load the palette from.")
    private String palette;
  }

  public static void main(String[] args) {
    Parameters params = new Parameters();
    JCommander jCommander = new JCommander(params);
    jCommander.parse(args);

    if (params.help) {
      jCommander.usage();
      System.exit(0);
      return;
    }

    if (params.inputFilename == null) {
      System.err.println("You must specify an input image.");
      System.exit(1);
      return;
    }

    BufferedImage input;
    try {
      if (params.inputFilename.equals("-")) {
        input = ImageIO.read(System.in);
      } else {
        input = ImageIO.read(Path.of(params.inputFilename).toFile());
      }
    } catch (Exception e) {
      System.err.printf("Failed to read input image: %s%n", params.inputFilename);
      e.printStackTrace();
      System.exit(1);
      return;
    }

    if (input == null) {
      System.err.printf("Failed to read input image: %s%n", params.inputFilename);
      System.exit(1);
      return;
    }

    Converter.Result result = Converter.convertImage(
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
        params.ditherMode
    );

    String outputFilename = params.outputFilename != null ? params.outputFilename : params.inputFilename + ".ctif";
    try {
      Files.write(Path.of(outputFilename), result.data().toByteArray());
    } catch (IOException e) {
      System.err.printf("Failed to write output image: %s%n", outputFilename);
      e.printStackTrace();
      System.exit(1);
      return;
    }

    if (params.previewFilename != null) {
      try {
        ImageIO.write(result.image(), "png", Path.of(params.previewFilename).toFile());
      } catch (IOException e) {
        System.err.printf("Failed to write preview image: %s%n", params.previewFilename);
        e.printStackTrace();
        System.exit(1);
        return;
      }
    }

    System.out.println("All done.");
  }
}
