package dev.tehbrian.ctif.converter;

import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameConsumer;
import com.github.kokorin.jaffree.ffmpeg.Stream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageGrabber implements FrameConsumer {

  final List<Frame> frames = new ArrayList<>();

  @Override
  public void consumeStreams(final List<Stream> streams) {
  }

  @Override
  public void consume(final Frame frame) {
    if (frame != null) {
      frames.add(frame);
    }
  }

  public Frame take() {
    if (frames.size() != 1) {
      System.err.println("Image grabber expects only one frame. Bad! >:(");
    }
    return frames.get(0);
  }

  public byte[] takeImageBytes(final String format) throws IOException {
    return toByteArray(take().getImage(), format);
  }

  private static byte[] toByteArray(final BufferedImage image, final String format) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, format, out);
    return out.toByteArray();
  }
}
