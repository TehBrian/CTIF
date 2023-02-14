package dev.tehbrian.ctif.provide;

public final class Main {
  private Main() {
  }

  public static void main(final String[] args) {
    final var webService = new WebService();
    webService.start();
    Runtime.getRuntime().addShutdownHook(new Thread(webService::stop));
  }
}
