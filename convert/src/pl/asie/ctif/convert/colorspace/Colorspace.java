package pl.asie.ctif.convert.colorspace;

public enum Colorspace {
  RGB(new AbstractColorspace() {
    @Override
    public float[] fromRGB(float[] value) {
      return value;
    }

    @Override
    public float[] toRGBArray(float[] value) {
      return value;
    }
  }),
  YUV(new ColorspaceMatrix(
      new float[]{
          0.299f, 0.587f, 0.114f,
          -0.147f, -0.289f, 0.436f,
          0.615f, -0.515f, -0.100f
      }
  )),
  YIQ(new ColorspaceMatrix(
      new float[]{
          0.299f, 0.587f, 0.114f,
          0.595716f, -0.274453f, -0.321263f,
          0.211456f, -0.522591f, 0.311135f
      }
  ));

  final AbstractColorspace abstractColorspace;

  Colorspace(AbstractColorspace abstractColorspace) {
    this.abstractColorspace = abstractColorspace;
  }

  public AbstractColorspace get() {
    return abstractColorspace;
  }
}
