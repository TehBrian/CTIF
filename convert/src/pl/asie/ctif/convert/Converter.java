package pl.asie.ctif.convert;

import pl.asie.ctif.convert.colorspace.AbstractColorspace;
import pl.asie.ctif.convert.platform.AbstractPlatform;
import pl.asie.ctif.convert.platform.PlatformComputerCraft;
import pl.asie.ctif.convert.platform.PlatformOpenComputers;
import pl.asie.ctif.convert.platform.PlatformZXSpectrum;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class Converter {
  public enum DitherMode {
    NONE,
    ERROR,
    ORDERED
  }

  private final BufferedImage image;
  private final Color[] palette;
  private final DitherMode ditherMode;
  private final float[] ditherMatrix;
  private final AbstractPlatform platform;
  private final AbstractColorspace colorspace;
  private final int optimizationLevel;

  private final int ditherMatrixSize, ditherMatrixOffset, ditherMax;
  private final float[][] img;
  private final float[][] pal;
  private final int pw, ph, cw, ch;

  public Converter(
      BufferedImage image,
      Color[] palette,
      DitherMode ditherMode,
      float[] ditherMatrix,
      AbstractPlatform platform,
      AbstractColorspace colorspace,
      int optimizationLevel
  ) {
    this.image = image;
    this.palette = palette;
    this.ditherMode = ditherMode;
    this.ditherMatrix = ditherMatrix;
    this.platform = platform;
    this.colorspace = colorspace;
    this.optimizationLevel = optimizationLevel;

    if (ditherMode == DitherMode.ORDERED) {
      assert ditherMatrix != null;
      this.ditherMatrixSize = (int) Math.sqrt(ditherMatrix.length - 1);
      this.ditherMatrixOffset = 0;
      this.ditherMax = (int) ditherMatrix[ditherMatrix.length - 1];
    } else {
      this.ditherMatrixSize = ditherMatrix != null ? (int) Math.sqrt(ditherMatrix.length) : 0;
      this.ditherMatrixOffset = (ditherMatrixSize - 1) / 2;
      this.ditherMax = 0;
    }

    this.img = new float[image.getWidth() * image.getHeight()][3];
    this.pal = new float[palette.length][3];

    this.pw = this.platform.getCharWidth();
    this.ph = this.platform.getCharHeight();
    this.cw = image.getWidth() / pw;
    this.ch = image.getHeight() / ph;

    for (int i = 0; i < img.length; i++) {
      img[i] = this.colorspace.fromRGB(image.getRGB(i % image.getWidth(), i / image.getWidth()));
    }

    for (int i = 0; i < palette.length; i++) {
      pal[i] = this.colorspace.fromRGB(palette[i].getRGB());
    }
  }

  public BufferedImage write(OutputStream stream) throws IOException {
    BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

    stream.write('C');
    stream.write('T');
    stream.write('I');
    stream.write('F');

    stream.write(1); // Header version
    stream.write(0); // Platform variant (0 - default)
    stream.write(this.platform.getPlatformId());
    stream.write(this.platform.getPlatformId() >> 8); // Platform ID
    stream.write(cw & 0xFF);
    stream.write(cw >> 8); // Width in chars
    stream.write(ch & 0xFF);
    stream.write(ch >> 8); // Height in chars
    stream.write(pw); // Char width
    stream.write(ph); // Char height

    stream.write(palette.length > 16 ? 8 : 4); // BPP (byte)

    if (this.platform.getCustomColorCount() > 0) {
      stream.write(3); // Palette entry size
      stream.write(16);
      stream.write(0); // Palette array size
      for (int i = 0; i < 16; i++) {
        stream.write(palette[i].getRGB() & 0xFF);
        stream.write((palette[i].getRGB() >> 8) & 0xFF);
        stream.write((palette[i].getRGB() >> 16) & 0xFF);
      }
    } else {
      stream.write(0); // Palette array size
      stream.write(0);
      stream.write(0); // Palette entry size
    }

    writePixelData(stream, output);
    stream.close();
    return output;
  }

  private void addQuantError(float[][] pixelArray, int x, int y, int w, int h, float[] expected, float[] received, float mul) {
    if (x >= 0 && y >= 0 && x < w && y < h) {
      addQuantError(pixelArray[y * w + x], expected, received, mul);
    }
  }

  private static void addQuantError(float[] target, float[] expected, float[] received, float mul) {
    if (mul != 0.0f) {
      for (int i = 0; i < target.length; i++) {
        target[i] += (expected[i] - received[i]) * mul;
      }
    }
  }

  private void writePixelData(OutputStream stream, BufferedImage output) throws IOException {
    int ew = (pw + ditherMatrixOffset * 2);
    int eh = (ph + ditherMatrixOffset * 2);

    int quadrantLen = (pw * ph + 7) / 8;

    float[][] pixels = new float[pw * ph][];
    float[][] bcea = new float[ew * eh][3];
    float[][] tPixels = new float[pixels.length][3];
    float[][] errors = new float[ew * eh][3];
    int[] bcq = new int[quadrantLen];
    int[] cq = new int[quadrantLen];

    float[] colA = new float[3];

    boolean usePalMap = this.optimizationLevel > 0
        && this.platform instanceof PlatformOpenComputers
        && ((PlatformOpenComputers) this.platform).getScreen() == PlatformOpenComputers.Screen.TIER_3;

    int[] palMap = new int[palette.length];
    int palMapLength;
    for (int i = 0; i < 16; i++)
      palMap[i] = i;
    int t3OffRed = this.optimizationLevel <= 1 ? 3 : (this.optimizationLevel == 2 ? 2 : 1);
    int t3OffGreen = this.optimizationLevel <= 1 ? 3 : (this.optimizationLevel <= 3 ? 2 : 1);
    int t3OffBlue = this.optimizationLevel <= 1 ? 2 : 1;

    for (int cy = 0; cy < ch; cy++) {
      for (int cx = 0; cx < cw; cx++) {
        for (int py = 0; py < ph; py++) {
          for (int px = 0; px < pw; px++) {
            pixels[py * pw + px] = img[(cy * ph + py) * image.getWidth() + cx * pw + px];
          }
        }

        int bci1 = 0, bci2 = 0;
        double bcerr = Double.MAX_VALUE;

        if (usePalMap) {
          palMapLength = 16;
          int[] colorsUsed = new int[palette.length];
          for (int py = 0; py < ph; py++) {
            for (int px = 0; px < pw; px++) {
              int rgb = image.getRGB(cx * pw + px, cy * ph + py);
              int red = (((rgb >> 16) & 0xFF) * 6 / 256);
              int green = (((rgb >> 8) & 0xFF) * 8 / 256);
              int blue = ((rgb & 0xFF) * 5 / 256);
              for (int rr = red - t3OffRed; rr <= red + t3OffRed; rr++)
                for (int rg = green - t3OffGreen; rg <= green + t3OffGreen; rg++)
                  for (int rb = blue - t3OffBlue; rb <= blue + t3OffBlue; rb++)
                    if (rr >= 0 && rg >= 0 && rb >= 0 && rr < 6 && rg < 8 && rb < 5) {
                      int col = 16 + rr * 40 + rg * 5 + rb;
                      if (colorsUsed[col] == 0) {
                        palMap[palMapLength++] = col;
                        colorsUsed[col] = 1;
                      }
                    }
            }
          }
        } else {
          palMapLength = palette.length;
        }

        boolean bcqFound = false;

        if (ditherMode == DitherMode.NONE && this.optimizationLevel >= 3) {
          int[] colors = new int[pixels.length];
          int colorCount = 0;
          boolean[] uColors = new boolean[palette.length];

          for (float[] pixel : pixels) {
            double bestDist = Double.MAX_VALUE;
            int bestCol = 0;

            for (int cim1 = 0; cim1 < palMapLength; cim1++) {
              int ci1 = usePalMap ? palMap[cim1] : cim1;
              float[] col1 = pal[ci1];
              double dist = Util.getColorDistanceSq(col1, pixel);
              if (dist < bestDist) {
                bestCol = ci1;
                bestDist = dist;
              }
            }

            if (!uColors[bestCol]) {
              uColors[bestCol] = true;
              colors[colorCount++] = bestCol;
            }
          }

          if (colorCount <= 2) {
            bci1 = colors[0];
            bci2 = colors[1];

            Arrays.fill(bcq, 0);

            for (int i = 0; i < pixels.length; i++) {
              int pos = (pw * ph - 1 - i);
              double dist0 = Util.getColorDistanceSq(pal[bci1], pixels[i]);
              double dist1 = Util.getColorDistanceSq(pal[bci2], pixels[i]);
              if (dist1 < dist0) {
                bcq[pos >> 3] |= (1 << (pos & 7));
              }
            }

            bcqFound = true;
          }
        }

        if (!bcqFound) {
          for (int cim1 = 1; cim1 < palMapLength; cim1++) {
            if (bcerr == 0) break;
            int ci1 = usePalMap ? palMap[cim1] : cim1;
            float[] col1 = pal[ci1];

            for (int cim2 = (this.platform instanceof PlatformZXSpectrum) ? (cim1 >= 8 ? 8 : 0) : 0; cim2 < cim1; cim2++) {
              if (bcerr == 0) break;
              int ci2 = usePalMap ? palMap[cim2] : cim2;
              float[] col2 = pal[ci2];
              double cerr = 0;

              for (int i = 0; i < quadrantLen; i++) {
                cq[i] = 0;
              }

              if (ditherMode == DitherMode.NONE) {
                for (int i = 0; i < pixels.length; i++) {
                  float[] col = pixels[i];
                  double cerr1 = Util.getColorDistanceSq(col, col1);
                  double cerr2 = Util.getColorDistanceSq(col, col2);
                  if (cerr2 < cerr1) {
                    int pos = (pw * ph - 1 - i);
                    cq[pos >> 3] |= (1 << (pos & 7));
                    cerr += cerr2;
                  } else {
                    cerr += cerr1;
                  }

                  if (cerr >= bcerr)
                    break;
                }
              } else if (ditherMode == DitherMode.ERROR) {
                for (int i = 0; i < pixels.length; i++) {
                  tPixels[i][0] = pixels[i][0];
                  tPixels[i][1] = pixels[i][1];
                  tPixels[i][2] = pixels[i][2];
                }

                for (int i = 0; i < errors.length; i++) {
                  errors[i][0] = 0;
                  errors[i][1] = 0;
                  errors[i][2] = 0;
                }

                for (int i = 0; i < tPixels.length; i++) {
                  float[] col = tPixels[i];
                  float[] colR;
                  double cerr1 = Util.getColorDistanceSq(col, col1);
                  double cerr2 = Util.getColorDistanceSq(col, col2);
                  if (cerr2 < cerr1) {
                    int pos = (pw * ph - 1 - i);
                    cq[pos >> 3] |= (1 << (pos & 7));
                    cerr += cerr2;
                    colR = col2;
                  } else {
                    cerr += cerr1;
                    colR = col1;
                  }

                  if (cerr >= bcerr)
                    break;

                  int qx = (i % pw);
                  int qy = (i / pw);

                  int ip = ditherMatrixSize * ditherMatrixOffset;
                  for (int iy = 0; iy < ditherMatrixSize - ditherMatrixOffset; iy++) {
                    for (int ix = -ditherMatrixOffset; ix < ditherMatrixSize - ditherMatrixOffset; ix++) {
                      addQuantError(tPixels, qx + ix, qy + iy, pw, ph, col, colR, ditherMatrix[ip]);
                      addQuantError(errors, qx + ix + ditherMatrixOffset, qy + iy + ditherMatrixOffset, ew, eh, col, colR, ditherMatrix[ip]);
                      ip++;
                    }
                  }
                }
              } else {
                // http://bisqwit.iki.fi/story/howto/dither/jy/

                cerr += Util.getColorDistanceSq(col1, col2) * 0.1 * pixels.length;

                for (int i = 0; i < pixels.length; i++) {
                  float[] col = pixels[i];
                  int qx = (i % pw);
                  int qy = (i / pw);

                  float jf =
                      (col[0] * col1[0] - col[0] * col2[0] - col1[0] * col2[0] + col2[0] * col2[0] +
                          col[1] * col1[1] - col[1] * col2[1] - col1[1] * col2[1] + col2[1] * col2[1] +
                          col[2] * col1[2] - col[2] * col2[2] - col1[2] * col2[2] + col2[2] * col2[2]) /
                          ((col1[0] - col2[0]) * (col1[0] - col2[0]) +
                              (col1[1] - col2[1]) * (col1[1] - col2[1]) +
                              (col1[2] - col2[2]) * (col1[2] - col2[2]));
                  int birat = ditherMax - Math.round(jf * ditherMax);
                  if (birat < 0) birat = 0;
                  else if (birat > ditherMax) birat = ditherMax;

                  colA[0] = (col2[0] * birat + col1[0] * (ditherMax - birat)) / ditherMax;
                  colA[1] = (col2[1] * birat + col1[1] * (ditherMax - birat)) / ditherMax;
                  colA[2] = (col2[2] * birat + col1[2] * (ditherMax - birat)) / ditherMax;
                  cerr += Util.getColorDistanceSq(col, colA);

                  if (cerr >= bcerr)
                    break;

                  int threshold = (int) ditherMatrix[((cy * ph + qy) % ditherMatrixSize) * ditherMatrixSize + ((cx * pw + qx) % ditherMatrixSize)];
                  if (threshold < birat) {
                    int pos = (pw * ph - 1 - i);
                    cq[pos >> 3] |= (1 << (pos & 7));
                  }
                }
              }

              if (cerr < bcerr) {
                bci1 = ci1;
                bci2 = ci2;
                bcerr = cerr;
                if (ditherMode == DitherMode.ERROR) {
                  for (int i = 0; i < errors.length; i++) {
                    bcea[i][0] = errors[i][0];
                    bcea[i][1] = errors[i][1];
                    bcea[i][2] = errors[i][2];
                  }
                }
                System.arraycopy(cq, 0, bcq, 0, quadrantLen);
              }
            }
          }

          if (ditherMode == DitherMode.ERROR) {
            for (int iy = 0; iy < eh; iy++) {
              int ry = cy * ph + iy - ditherMatrixOffset;
              if (ry >= 0 && ry < ch * ph) {
                for (int ix = 0; ix < ew; ix++) {
                  int rx = cx * pw + ix - ditherMatrixOffset;
                  if (rx >= 0 && rx < cw * pw) {
                    for (int i = 0; i < 3; i++) {
                      img[ry * cw * pw + rx][i] += bcea[iy * ew + ix][i];
                    }
                  }
                }
              }
            }
          }
        }

        int[] quadrant = bcq;
        int bgIndex = bci1;
        int fgIndex = bci2;
        if (bgIndex == fgIndex) {
          for (int i = 0; i < quadrantLen; i++) quadrant[i] = 0;
        }

        if (this.platform instanceof PlatformComputerCraft) {
          if ((quadrant[0] & 0x01) != 0) {
            int t = fgIndex;
            fgIndex = bgIndex;
            bgIndex = t;
            quadrant[0] ^= 0x3F;
          }
        } else if (this.platform instanceof PlatformOpenComputers && pw * ph > 2) {
          if (bgIndex > fgIndex) {
            int t = fgIndex;
            fgIndex = bgIndex;
            bgIndex = t;
            quadrant[0] ^= (1 << (pw * ph)) - 1;
          }
        }

        if (pw * ph == 2 && quadrant[0] == 1) {
          int t = fgIndex;
          fgIndex = bgIndex;
          bgIndex = t;
          quadrant[0] = 0;
        }

        if (palette.length > 2) {
          if (pw * ph == 1) {
            stream.write(fgIndex);
          } else {
            if (palette.length > 16) {
              stream.write(bgIndex);
              stream.write(fgIndex);
            } else {
              stream.write(bgIndex << 4 | fgIndex);
            }
          }

          if (pw * ph > 2) {
            for (int i = 0; i < quadrantLen; i++)
              stream.write(quadrant[i]);
          }
        } else {
          for (int i = 0; i < quadrantLen; i++)
            stream.write(quadrant[i]);
        }

        for (int py = 0; py < ph; py++) {
          for (int px = 0; px < pw; px++) {
            int i = (pw * ph - 1) - (py * pw + px);

            if ((quadrant[i >> 3] & (1 << (i & 7))) != 0) {
              output.setRGB(cx * pw + px, cy * ph + py, palette[fgIndex].getRGB());
            } else {
              output.setRGB(cx * pw + px, cy * ph + py, palette[bgIndex].getRGB());
            }
          }
        }
      }
    }
  }
}
