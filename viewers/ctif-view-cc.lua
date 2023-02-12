--[[
ctif-cc: ComputerCraft viewer for CTIF image files
Copyright (c) 2016, 2017, 2018 asie

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
]]

local pal = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"}
local oldCols = {}
local colsChanged = false
local ctable = {}
local hdr = {67,84,73,70}
local args = {...}

-- No Lua 5.3? Oh well.
for a=0,1 do
for b=0,1 do
for c=0,1 do
for d=0,1 do
for e=0,1 do
for f=0,1 do
  ctable[1 + a*32 + b*16 + c*8 + d*4 + e*2 + f] = e*16 + d*8 + c*4 + b*2 + a
end
end
end
end
end
end

local file = fs.open(args[1], "rb")
local t = term
local isMonitor = false
if #args >= 2 then
  t = peripheral.wrap(args[2])
  isMonitor = true
end

function readShort()
  local x = file:read()
  return x + (file:read()) * 256
end

-- Verify header
for i=1,4 do
  if file:read() ~= hdr[i] then
    error("Invalid header!")
  end
end

local hdrVersion = file:read()
if hdrVersion > 1 then
  error("Unknown header version: " .. hdrVersion)
end

local platformVariant = file:read()
local platformId = readShort()
if platformId ~= 2 or platformVariant ~= 0 then
  error("Unsupported platform ID: " .. platformId .. ":" .. platformVariant)
end

local width = readShort()
local height = readShort()

local pw = file:read()
local ph = file:read()
if not (pw == 2 and ph == 3) and not (pw == 1 and ph == 1) then
  error("Unsupported character width: " .. pw .. "x" .. ph)
end

local bpp = file:read()
if bpp ~= 4 then
  error("Unsupported bit depth: " .. bpp)
end

local ccEntrySize = file:read()
local ccArraySize = readShort()
if ccArraySize > 0 then
  if t.setPaletteColour == nil then
    error("Custom colors are not supported on this version of ComputerCraft! Please upgrade.")
  else
    for i=0,ccArraySize-1 do
      local oldColR, oldColG, oldColB = t.getPaletteColour(bit.blshift(1, i))
      oldCols[i*3 + 1] = oldColR
      oldCols[i*3 + 2] = oldColG
      oldCols[i*3 + 3] = oldColB
      local colB = file:read() / 255
      local colG = file:read() / 255
      local colR = file:read() / 255
      t.setPaletteColour(bit.blshift(1, i), colR, colG, colB)
    end
    colsChanged = true
  end
end

-- Prepare ideal terminal size
t.clear()
if isMonitor then
  t.setTextScale(0.5)
end
local xoff = -1
local yoff = -1
local xbase, ybase = t.getSize()
if isMonitor then
  for xdiv=5,0.5,-0.5 do
    local xsize = math.floor(xbase / (xdiv * 2))
    local ysize = math.floor(ybase / (xdiv * 2))
    if xsize >= width and ysize >= height then
      t.setTextScale(xdiv)
      xoff = math.floor((xsize - width) / 2) + 1
      yoff = math.floor((ysize - height) / 2) + 1
      break
    end
  end
else
  if xbase >= width and ybase >= height then
    xoff = math.floor((xbase - width) / 2) + 1
    yoff = math.floor((ybase - height) / 2) + 1
  end
end

if (xoff < 0) or (yoff < 0) then
  error("Image too large for ComputerCraft: " .. width .. "x" .. height .. " (max: " .. xbase .. "x" .. ybase .. ")")
end

-- Draw
for y=0,height-1 do 
local bgs = ""
local fgs = ""
local chs = ""
for x=0,width-1 do 
  if pw*ph == 1 then
    bgs = bgs .. pal[1 + file:read()]
    fgs = fgs .. "0"
    chs = chs .. " "
  else
    local col = file:read()
    local ch = file:read()
    local fg = col % 16
    local bg = (col - fg) / 16
    if ch % 2 == 1 then
      ch = 63 - ch
      local t = fg
      fg = bg
      bg = t
    end
    bgs = bgs .. pal[1 + bg]
    fgs = fgs .. pal[1 + fg]
    chs = chs .. string.char(128 + ctable[1 + ch])
  end
end
t.setCursorPos(xoff, yoff + y)
t.blit(chs, fgs, bgs)
end

local event, key = os.pullEvent( "key" )

if colsChanged then
  for i=0,ccArraySize-1 do
    t.setPaletteColour(bit.blshift(1, i), oldCols[i*3 + 1], oldCols[i*3 + 2], oldCols[i*3 + 3])
  end
end
