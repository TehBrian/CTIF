package pl.asie.ctif.convert;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.jspecify.annotations.Nullable;
import pl.asie.ctif.convert.colorspace.Colorspace;
import pl.asie.ctif.convert.converter.Converter;
import pl.asie.ctif.convert.converter.Resizer;
import pl.asie.ctif.convert.converter.UglyConverter;
import pl.asie.ctif.convert.platform.Platform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
  @SuppressWarnings("FieldMayBeFinal")
  private static class Parameters {
    @Parameter(names = {"--palette-sampling-resolution"}, description = "The sampling resolution for palette generation. 0 means full image. (1/4x1/4 image in -O3+)")
    private int paletteSamplingResolution = 0;

    @Parameter(names = {"--threads"}, description = "Amount of threads to create.")
    private int threads = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"--palette-export"}, description = "File to export the palette to.")
    private String paletteExport;

    @Parameter(names = {"--palette"}, description = "File to load the palette from.")
    private String palette;

    @Parameter(names = {"-m", "--mode"}, description = "Target platform.")
    private Platform mode = Platform.OC_TIER_3;

    @Parameter(names = {"-O", "--optimization-level"}, description = "Optimization level. Primarily 0-4. Larger levels are less accurate but generate faster.")
    private int optimizationLevel = 1;

    @Parameter(names = {"--colorspace"}, description = "Image colorspace.")
    private Colorspace colorspace = Colorspace.YIQ;

    @Parameter(names = {"--dither-mode"}, description = "Dither mode.")
    private UglyConverter.DitherMode ditherMode = UglyConverter.DitherMode.ERROR;

    @Parameter(names = {"--dither-type"}, description = "Dither type. (error: floyd-steinberg, sierra-lite; ordered: 2x2, 4x4, 8x8)")
    private String ditherType;

    @Parameter(names = {"--dither-level"}, description = "Dither level for error-type dither. 0 = off, 1 = full.")
    private float ditherLevel = 1.0f;

    @Parameter(names = {"-d", "--debug"}, description = "Enable debugging.", hidden = true)
    private boolean debug = false;

    @Parameter(names = {"-W", "--width"}, description = "Output image width.")
    private int w;

    @Parameter(names = {"-H", "--height"}, description = "Output image height.")
    private int h;

    @Parameter(names = {"-N", "--no-aspect"}, description = "Ignore aspect ratio.")
    private boolean ignoreAspectRatio = false;

    @Parameter(names = {"-o", "--output"}, description = "Output filename.")
    private String outputFilename;

    @Parameter(names = {"-P", "--preview"}, description = "Preview image filename.")
    private String previewFilename;

    @Parameter(description = "Input file.")
    private List<String> files = new ArrayList<>();

    @Parameter(names = {"-h", "--help"}, description = "Print usage.", help = true)
    private boolean help;

    @Parameter(names = {"--resize-mode"}, description = "Resize mode.")
    private Resizer.Mode resizeMode;
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
    BufferedImage input = loadImage(inputPath);
    if (input == null) {
      System.err.printf("Failed to read input image: %s%n", inputPath);
      System.exit(1);
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

  public static @Nullable BufferedImage loadImage(String location) {
    try {
      if (location.equals("-")) {
        return ImageIO.read(System.in);
      } else {
        return ImageIO.read(new File(location));
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
