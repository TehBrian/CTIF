package pl.asie.ctif.convert.colorspace;

public abstract class AbstractColorspace {
  public abstract float[] fromRGB(float[] value);

  public abstract float[] toRGBArray(float[] value);

  public int toRGB(float[] value) {
    float[] rgb = toRGBArray(value);
    if (rgb[0] < 0) rgb[0] = 0;
    else if (rgb[0] > 1) rgb[0] = 1;
    if (rgb[1] < 0) rgb[1] = 0;
    else if (rgb[1] > 1) rgb[1] = 1;
    if (rgb[2] < 0) rgb[2] = 0;
    else if (rgb[2] > 1) rgb[2] = 1;
    return (Math.round(rgb[0] * 255.0f) << 16) | (Math.round(rgb[1] * 255.0f) << 8) | Math.round(rgb[2] * 255.0f);
  }

  public float[] fromRGB(int value) {
    return fromRGB(new float[]{
        (float) ((value >> 16) & 0xFF) / 255.0f,
        (float) ((value >> 8) & 0xFF) / 255.0f,
        (float) (value & 0xFF) / 255.0f
    });
  }
}
