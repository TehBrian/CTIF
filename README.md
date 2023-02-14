# CTIF

An image format for use with OpenComputers and ComputerCraft. Stands for ChenThread Image Format.

* [OpenComputers Thread](https://oc.cil.li/topic/864-chenthread-image-format-high-quality-images-on-opencomputers/)
* [ComputerCraft Thread](http://www.computercraft.info/forums2/index.php?/topic/26186-chenthread-image-format-quality-images-on-18-computercraft)

## Overview

CTIF allows you to display high-resolution images on an OpenComputers or ComputerCraft screen.

It accomplishes this with a two-step process:

1. *Converting* PNG images to the CTIF format. This step is done outside the game.
2. *Viewing* CTIF images in-game.

This repository, therefore, contains two important segments: the **converter** and the **viewers**.

The **converter** is a standalone Java CLI app that transforms PNG images to CTIF images.

As for the **viewers**, there are two: one for OpenComputers and one for ComputerCraft. These are Lua programs that
display CTIF images onto the screen.

## Converter

The converter's code can be found in the `converter` directory. You can either build
it yourself or grab a CTIFConverter JAR at the GitHub [Releases](https://github.com/TehBrian/CTIF/releases/latest).

Then, simply run the JAR as a Java application through the terminal.

### Notes
The application requires a runtime of at minimum Java 17.

Additionally, if you're using Windows, download [im4java](https://im4java.sourceforge.net/) and add the path to an ENV
variable called `IM4JAVA_TOOLPATH`.

### Example Usage

`java -jar CTIFConverter.jar -h` will show the help menu.

`java -jar CTIFConverter.jar -m oc-tier3 -o out.ctif in.png` will convert `in.png` to `out.ctif`, targeting an
OpenComputers tier 3 screen.

`java -jar CTIFConverter.jar -m oc-tier3 -P preview.png -o out.ctif in.png` will, in addition to converting the image as
before, generate `preview.png` as a preview of what the CTIF image will look like when displayed by a viewer.

`java -jar CTIFConverter.jar -m cc -W 102 -H 57 -o out.ctif in.png` will convert `in.png` to `out.ctif`, targeting a
ComputerCraft screen, and scale the output to a resolution of *at most* 102x57. By default, this will retain the aspect
ratio; to ignore the aspect ratio and force the image to be ***exactly*** 102x57, add the `-N` flag.

`java -jar CTIFConverter.jar -m oc-tier3 -W 320 -H 200 -o out.ctif in.png` is what I find works best to get as big of
a picture as possible onto an OpenComputers tier 3 screen. (It's simply the tier 3 screen resolution of `160x50`, with
the width `*2` and the height `*4`.)

## Viewers

The viewers' code can be found in the `viewers` directory. In it, there are three files.

* `ctif-view-oc.lua` - CTIF viewer for OpenComputers. Usage: `ctif-view-oc <file>`. Requires the CPU's architecture to
  be set to **Lua 5.3**. (To change the architecture, shift-click the CPU while holding it.)
* `ctif-view-cc.lua` - CTIF viewer for ComputerCraft 1.79-. Usage: `ctif-view-cc <file> [monitor side]`. If you see
  errors about the image size being too large, keep in mind that the viewer operates on *characters* while the converter
  operates on *pixels*. To convert from characters to pixels, multiply the width by 2 and the height by 4.
* `ctif-view-cc-1.8.lua` - CTIF viewer for ComputerCraft 1.80+.
