package pl.asie.ctif.convert;

import java.time.Duration;
import java.time.Instant;

public interface Stopwatch {

  static Stopwatch started() {
    final StopwatchImpl stopwatch = new StopwatchImpl();
    stopwatch.start();
    return stopwatch;
  }

  Duration timeElapsed();

  class StopwatchImpl implements Stopwatch {
    private Instant beginning;
    private Instant end;

    public void start() {
      this.beginning = Instant.now();
    }

    public void stop() {
      this.end = Instant.now();
    }

    @Override
    public Duration timeElapsed() {
      if (this.beginning == null) {
        this.start();
      }
      if (this.end == null) {
        this.stop();
      }

      return Duration.between(this.beginning, this.end);
    }
  }
}
