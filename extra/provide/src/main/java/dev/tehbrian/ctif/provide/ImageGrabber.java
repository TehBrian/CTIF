package dev.tehbrian.ctif.provide;

import com.github.kokorin.jaffree.ffmpeg.Frame;
import com.github.kokorin.jaffree.ffmpeg.FrameConsumer;
import com.github.kokorin.jaffree.ffmpeg.Stream;

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
    return frames.getFirst();
  }
}
