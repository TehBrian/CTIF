package pl.asie.ctif.convert;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.Stream2BufferedImage;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Util {

  private static int imMode = -1;

  public static int[] getRGB(BufferedImage image) {
    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
  }

  public static double getColorDistanceSq(float[] f1, float[] f2) {
    return (f1[0] - f2[0]) * (f1[0] - f2[0]) +
        (f1[1] - f2[1]) * (f1[1] - f2[1]) +
        (f1[2] - f2[2]) * (f1[2] - f2[2]);
  }

  private static ConvertCmd createConvertCmd() {
    if (imMode < 0) {
      ConvertCmd cmd = new ConvertCmd();
      imMode = 1;

      try {
        cmd.setOutputConsumer(inputStream -> {

        });
        cmd.run(new IMOperation());
      } catch (Exception e) {
        if (e.getCause() instanceof IOException) {
          System.out.println("Warning: ImageMagick not found! Please install ImageMagick for improved scaling quality.");
          imMode = 0;
        }
      }

      if (imMode == 1) {
        System.out.println("ImageMagick found; using ImageMagick for resizing.");
      }
    }

    return switch (imMode) {
      case 1 -> new ConvertCmd();
      case 2 -> new ConvertCmd(true);
      default -> null;
    };
  }

  public static BufferedImage resize(BufferedImage image, int width, int height, boolean forceNoImagemagick) {
    ConvertCmd cmd = forceNoImagemagick ? null : createConvertCmd();
    if (cmd == null) {
      BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
      Graphics2D g = resizedImage.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.drawImage(image, 0, 0, width, height, null);
      g.dispose();
      return resizedImage;
    }
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
      e.printStackTrace();
    }
    return s2b.getImage();
  }

  public static BufferedImage resizeBox(BufferedImage image, int width, int height) {
    BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
    Graphics2D g = resizedImage.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(image, 0, 0, width, height, null);
    g.dispose();
    return resizedImage;
  }

  public static void saveImage(BufferedImage image, String location) {
    try {
      ImageIO.write(image, "png", new File(location));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static BufferedImage loadImage(String location) {
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
