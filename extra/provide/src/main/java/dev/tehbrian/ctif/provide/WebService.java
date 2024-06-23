package dev.tehbrian.ctif.provide;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FrameOutput;
import com.github.kokorin.jaffree.ffmpeg.PipeInput;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import pl.asie.ctif.convert.Stopwatch;
import pl.asie.ctif.convert.colorspace.Colorspace;
import pl.asie.ctif.convert.converter.Converter;
import pl.asie.ctif.convert.converter.UglyConverter;
import pl.asie.ctif.convert.platform.PlatformOpenComputers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Controls the web server.
 */
public final class WebService {
  private static final Logger LOGGER = LogManager.getLogger();

  private @Nullable Javalin javalin;

  /**
   * Starts the web server. Creates the conversion endpoint.
   */
  public void start() {
    this.javalin = Javalin.create();

    this.javalin.get("/*", this::convert);

    final var port = 59100;
    this.javalin.start(port);

    LOGGER.info("Started web server on port {}.", port);
  }

  /**
   * Stops the web server.
   */
  public void stop() {
    if (this.javalin != null) {
      this.javalin.stop();
    }
  }

  public String elapsed(final Stopwatch stopwatch) {
    final Duration elapsed = stopwatch.timeElapsed();
    return String.format("%s.%s", elapsed.toSeconds(), elapsed.toMillisPart());
  }

  public void convert(final Context ctx) {
    final var url = ctx.queryParam("url");
    if (url == null || url.isEmpty()) {
      ctx.result("You must provide a source URL as a query parameter with the key `url`.");
      ctx.status(HttpStatus.BAD_REQUEST);
      return;
    }

    final URI uri;
    try {
      uri = URI.create(url);
    } catch (final IllegalArgumentException e) {
      ctx.result("The provided source URL is not valid.");
      ctx.status(HttpStatus.BAD_REQUEST);
      return;
    }

    LOGGER.info("Received request for URL `{}`.", url);
    final String realIpHeader = ctx.header("X-Real-IP");
    if (realIpHeader != null) {
      // request was proxied.
      LOGGER.info("|  Real IP: `{}`", realIpHeader);
    } else {
      // request was not proxied.
      LOGGER.info("|  IP: `{}`", ctx.ip());
    }
    LOGGER.info("|  User Agent: `{}`", ctx.userAgent());

    LOGGER.info("Requesting source from URL.");
    final var toSource = Stopwatch.started();
    final InputStream source;
    final HttpClient client = HttpClient.newHttpClient();
    try {
      source = client.send(
          HttpRequest.newBuilder(uri).build(),
          HttpResponse.BodyHandlers.ofInputStream()
      ).body();
    } catch (final IOException | InterruptedException e) {
      LOGGER.info("Exception caught while requesting source.", e);
      ctx.result("Error while requesting source.");
      ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }
    LOGGER.info("Requested source from URL. Took {}.", elapsed(toSource));

    LOGGER.info("Converting source to frame with FFmpeg.");
    final var toFrame = Stopwatch.started();
    final var grabber = new ImageGrabber();
    FFmpeg.atPath()
        .addInput(PipeInput.pumpFrom(source))
        .addOutput(FrameOutput.withConsumer(grabber))
        .addArguments("-f", "image2")
        .addArguments("-c", "png")
        .execute();
    LOGGER.info("Converted source to frame. Took {}.", elapsed(toFrame));

    client.close();

    LOGGER.info("Converting frame to CTIF.");
    final var toCtif = Stopwatch.started();
    final Converter.Result result = Converter.convertImage(
        false,
        new PlatformOpenComputers(PlatformOpenComputers.Screen.TIER_3),
        1,
        Colorspace.YIQ.get(),
        Runtime.getRuntime().availableProcessors(),
        grabber.take().getImage(),
        320,
        200,
        false,
        null,
        UglyConverter.DitherMode.ERROR,
        null,
        1.0F,
        0,
        null,
        null
    );
    LOGGER.info("Converted frame to CTIF. Took {}.", elapsed(toCtif));

    ctx.header("Content-Disposition", "inline");
    ctx.contentType(ContentType.APPLICATION_OCTET_STREAM);
    ctx.result(result.data().toByteArray());
    ctx.status(HttpStatus.OK);
  }

}
