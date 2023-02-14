package pl.asie.ctif.convert;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Stream2BufferedImage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Resizer {
  public static BufferedImage qualityResize(BufferedImage image, int width, int height, boolean forceNoImageMagick) {
    if (forceNoImageMagick || !hasImageMagick()) {
      BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
      Graphics2D g = resizedImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.drawImage(image, 0, 0, width, height, null);
      g.dispose();
      return resizedImage;
    } else {
      ConvertCmd cmd = new ConvertCmd();
      IMOperation op = new IMOperation();
      op.addImage();
      op.colorspace("RGB");
      op.filter("LanczosRadius");
      op.resize(width, height, '!');
      op.colorspace("sRGB");
      op.addImage("png:-");
      Stream2BufferedImage s2b = new Stream2BufferedImage();
      cmd.setOutputConsumer(s2b);

      try {
        cmd.run(op, image);
      } catch (Exception e) {
        System.err.println("Failed to resize image using ImageMagick.");
        e.printStackTrace();
      }

      return s2b.getImage();
    }
  }

  public static BufferedImage speedyResize(BufferedImage image, int width, int height) {
    BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
    Graphics2D g = resizedImage.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(image, 0, 0, width, height, null);
    g.dispose();
    return resizedImage;
  }

  public enum Mode {
    SPEED,
    QUALITY_NATIVE,
    QUALITY
  }

  private static boolean hasImageMagick() {
    boolean hasImageMagick = true;

    try {
      ConvertCmd cmd = new ConvertCmd();
      cmd.setOutputConsumer(inputStream -> {
      });
      cmd.run(new IMOperation());
    } catch (IOException | IM4JavaException | InterruptedException e) {
      if (e.getCause() instanceof IOException) {
        hasImageMagick = false;
      }
    }

    if (hasImageMagick) {
      System.out.println("ImageMagick found; using ImageMagick for resizing.");
      return true;
    } else {
      System.out.println("Warning: ImageMagick not found! Please install ImageMagick for improved scaling quality.");
      return false;
    }
  }
}
