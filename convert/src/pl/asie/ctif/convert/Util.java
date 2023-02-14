package pl.asie.ctif.convert;

public class Util {

  public static double getColorDistanceSq(float[] f1, float[] f2) {
    return (f1[0] - f2[0]) * (f1[0] - f2[0]) +
        (f1[1] - f2[1]) * (f1[1] - f2[1]) +
        (f1[2] - f2[2]) * (f1[2] - f2[2]);
  }
}
