package pl.asie.ctif.convert.colorspace;

public class ColorspaceMatrix extends Colorspace {
  private final float[] matrixFromRGB;
  private final float[] matrixToRGB;

  protected ColorspaceMatrix(float[] matrix) {
    this.matrixFromRGB = matrix;
    this.matrixToRGB = new float[9];

    // @formatter:off
    float detInv = 1f / (
        ((matrix[0] * matrix[4] * matrix[8]) + (matrix[1] * matrix[5] * matrix[6]) + (matrix[2] * matrix[3] * matrix[7]))
      - ((matrix[2] * matrix[4] * matrix[6]) + (matrix[0] * matrix[5] * matrix[7]) + (matrix[1] * matrix[3] * matrix[8]))
    );

    matrixToRGB[0] =  detInv * (matrix[3 + 1] * matrix[2 * 3 + 2] - matrix[3 + 2] * matrix[2 * 3 + 1]);
    matrixToRGB[3] = -detInv * (matrix[3    ] * matrix[2 * 3 + 2] - matrix[3 + 2] * matrix[2 * 3    ]);
    matrixToRGB[6] = -detInv * (matrix[3 + 1] * matrix[2 * 3    ] - matrix[3    ] * matrix[2 * 3 + 1]);
    matrixToRGB[1] = -detInv * (matrix[1    ] * matrix[2 * 3 + 2] - matrix[2    ] * matrix[2 * 3 + 1]);
    matrixToRGB[4] =  detInv * (matrix[0    ] * matrix[2 * 3 + 2] - matrix[2    ] * matrix[2 * 3    ]);
    matrixToRGB[7] =  detInv * (matrix[1    ] * matrix[2 * 3    ] - matrix[0    ] * matrix[2 * 3 + 1]);
    matrixToRGB[2] =  detInv * (matrix[1    ] * matrix[3 + 2    ] - matrix[2    ] * matrix[3 + 1    ]);
    matrixToRGB[5] = -detInv * (matrix[0    ] * matrix[3 + 2    ] - matrix[2    ] * matrix[3        ]);
    matrixToRGB[8] = -detInv * (matrix[1    ] * matrix[3        ] - matrix[0    ] * matrix[3 + 1    ]);
    // @formatter:on
  }

  protected final float[] mmul3(float[] a, float[] b) {
    return new float[]{
        a[0] * b[0] + a[1] * b[1] + a[2] * b[2],
        a[0] * b[3] + a[1] * b[4] + a[2] * b[5],
        a[0] * b[6] + a[1] * b[7] + a[2] * b[8]
    };
  }

  @Override
  public float[] fromRGB(float[] value) {
    return mmul3(value, matrixFromRGB);
  }

  @Override
  public float[] toRGBArray(float[] value) {
    return mmul3(value, matrixToRGB);
  }
}
