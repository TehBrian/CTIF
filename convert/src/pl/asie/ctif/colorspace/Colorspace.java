package pl.asie.ctif.colorspace;

public abstract class Colorspace {
    public static final Colorspace YIQ;
    public static final Colorspace YUV;
    public static final Colorspace RGB;

    static {
        YUV = new ColorspaceMatrix(
                new float[]{
                        0.299f, 0.587f, 0.114f,
                        -0.147f, -0.289f, 0.436f,
                        0.615f, -0.515f, -0.100f
                }
        );
        YIQ = new ColorspaceMatrix(
                new float[]{
                        0.299f, 0.587f, 0.114f,
                        0.595716f, -0.274453f, -0.321263f,
                        0.211456f, -0.522591f, 0.311135f
                }
        );
        RGB = new Colorspace() {
            @Override
            public float[] fromRGB(float[] value) {
                return value;
            }

            @Override
            public float[] toRGBArray(float[] value) {
                return value;
            }
        };
    }

    public int toRGB(float[] value) {
        float[] rgb = toRGBArray(value);
        if (rgb[0] < 0) rgb[0] = 0; else if (rgb[0] > 1) rgb[0] = 1;
        if (rgb[1] < 0) rgb[1] = 0; else if (rgb[1] > 1) rgb[1] = 1;
        if (rgb[2] < 0) rgb[2] = 0; else if (rgb[2] > 1) rgb[2] = 1;
        return (Math.round(rgb[0] * 255.0f) << 16) | (Math.round(rgb[1] * 255.0f) << 8) | Math.round(rgb[2] * 255.0f);
    }

    public float[] fromRGB(int value) {
        return fromRGB(new float[] {
                (float) ((value >> 16) & 0xFF) / 255.0f,
                (float) ((value >> 8) & 0xFF) / 255.0f,
                (float) (value & 0xFF) / 255.0f
        });
    }

    public abstract float[] fromRGB(float[] value);

    public abstract float[] toRGBArray(float[] value);
}
