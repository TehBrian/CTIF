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
import pl.asie.ctif.convert.colorspace.Colorspace;
import pl.asie.ctif.convert.converter.Converter;
import pl.asie.ctif.convert.converter.UglyConverter;
import pl.asie.ctif.convert.platform.PlatformOpenComputers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Controls the web server.
 */
public final class WebService {
  private static final Logger LOGGER = LogManager.getLogger();

  private @Nullable Javalin javalin;

  /**
   * Starts the web server and sets up the appropriate endpoints.
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

    final var source = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(uri).build(),
        HttpResponse.BodyHandlers.ofInputStream()
    ).body();

    final var grabber = new ImageGrabber();
    FFmpeg.atPath()
        .addInput(PipeInput.pumpFrom(source))
        .addOutput(FrameOutput.withConsumer(grabber))
        .addArguments("-f", "image2")
        .addArguments("-c", "png")
        .execute();

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

    ctx.header("Content-Disposition", "inline");
    ctx.contentType(ContentType.APPLICATION_OCTET_STREAM);
    ctx.result(result.data().toByteArray());
    ctx.status(HttpStatus.OK);
  }

}
