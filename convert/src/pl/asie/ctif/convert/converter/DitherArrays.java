package pl.asie.ctif.convert.converter;

import java.util.HashMap;
import java.util.Map;

public class DitherArrays {
  private static final Map<String, float[]> DITHER_ARRAYS = new HashMap<>();

  static {
    DITHER_ARRAYS.put("floyd-steinberg", new float[]{
        0, 0, 0,
        0, 0, 7f / 16f,
        3f / 16f, 5f / 16f, 1f / 16f
    });
    DITHER_ARRAYS.put("sierra-lite", new float[]{
        0, 0, 0,
        0, 0, 0.5f,
        0.25f, 0.25f, 0
    });
    DITHER_ARRAYS.put("checks", new float[]{
        0, 1,
        1, 0,
        2
    });
    DITHER_ARRAYS.put("2x2", new float[]{
        0, 2,
        3, 1,
        4
    });
    DITHER_ARRAYS.put("3x3", new float[]{
        0, 7, 3,
        6, 5, 2,
        4, 1, 8,
        9
    });
    DITHER_ARRAYS.put("4x4", new float[]{
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5,
        16
    });
    DITHER_ARRAYS.put("8x8", new float[]{
        0, 48, 12, 60, 3, 51, 15, 63,
        32, 16, 44, 28, 35, 19, 47, 31,
        8, 56, 4, 52, 11, 59, 7, 55,
        40, 24, 36, 20, 43, 27, 39, 23,
        2, 50, 14, 62, 1, 49, 13, 61,
        34, 18, 46, 30, 33, 17, 45, 29,
        10, 58, 6, 54, 9, 57, 5, 53,
        42, 26, 38, 22, 41, 25, 37, 21,
        64
    });

    for (int i = 3; i <= 8; i++) {
      float[] arrL = new float[i * i + 1];
      float[] arrR = new float[i * i + 1];
      float[] arrS = new float[i * i + 1];
      arrL[i * i] = arrR[i * i] = i;
      arrS[i * i] = i * i;
      for (int j = 0; j < i; j++) {
        for (int k = 0; k < i; k++) {
          arrL[k * i + j] = ((i - 1 - j) + (i - k)) % i;
          arrR[k * i + j] = (j + (i - k)) % i;
          arrS[k * i + j] = Math.max(k, j) * Math.max(k, j);
        }
      }

      DITHER_ARRAYS.put("diag-l-" + i + "x" + i, arrL);
      DITHER_ARRAYS.put("diag-r-" + i + "x" + i, arrR);
      DITHER_ARRAYS.put("square-" + i + "x" + i, arrS);
    }

    for (int i = 3; i <= 8; i += 2) {
      float[] arrD = new float[i * i + 1];
      arrD[i * i] = i * i;
      int center = i / 2;
      for (int j = 0; j < i; j++) {
        for (int k = 0; k < i; k++) {
          arrD[k * i + j] = Math.abs(j - center) + Math.abs(k - center);
          arrD[k * i + j] *= arrD[k * i + j];
        }
      }
      DITHER_ARRAYS.put("diamond-" + i + "x" + i, arrD);
    }

    DITHER_ARRAYS.put("diagl-4x4", new float[]{
        3, 2, 1, 0,
        2, 1, 0, 3,
        1, 0, 3, 2,
        0, 3, 2, 1,
        4
    });
    DITHER_ARRAYS.put("diagr-4x4", new float[]{
        0, 1, 2, 3,
        3, 0, 1, 2,
        2, 3, 0, 1,
        1, 2, 3, 0,
        4
    });
  }

  public static float[] get(String name) {
    return DITHER_ARRAYS.get(name);
  }
}
