package pl.asie.ctif.convert.colorspace;

public enum Colorspace {
  RGB(AbstractColorspace.RGB),
  YUV(AbstractColorspace.YUV),
  YIQ(AbstractColorspace.YIQ);

  final AbstractColorspace abstractColorspace;

  Colorspace(AbstractColorspace abstractColorspace) {
    this.abstractColorspace = abstractColorspace;
  }

  public AbstractColorspace get() {
    return abstractColorspace;
  }
}
