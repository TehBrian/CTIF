package pl.asie.ctif.convert.platform;

public enum Platform {
  CC(new PlatformComputerCraft(false)),
  CC_PALETTED(new PlatformComputerCraft(true)),
  OC_TIER_1(new PlatformOpenComputers(PlatformOpenComputers.Screen.TIER_1)),
  OC_TIER_2(new PlatformOpenComputers(PlatformOpenComputers.Screen.TIER_2)),
  OC_TIER_3(new PlatformOpenComputers(PlatformOpenComputers.Screen.TIER_3)),
  ZXSPECTRUM(new PlatformZXSpectrum(0)),
  ZXSPECTRUM_DARK(new PlatformZXSpectrum(1));

  final AbstractPlatform abstractPlatform;

  Platform(AbstractPlatform abstractPlatform) {
    this.abstractPlatform = abstractPlatform;
  }

  public AbstractPlatform get() {
    return abstractPlatform;
  }
}
