package pl.asie.ctif.convert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Util {

  public static int[] getRGB(BufferedImage image) {
    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
  }

  public static double getColorDistanceSq(float[] f1, float[] f2) {
    return (f1[0] - f2[0]) * (f1[0] - f2[0]) +
        (f1[1] - f2[1]) * (f1[1] - f2[1]) +
        (f1[2] - f2[2]) * (f1[2] - f2[2]);
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
