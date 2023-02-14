local ctif = require("ctif-lib-oc")
local event = require("event")
local fs = require("filesystem")
local os = require("os")
local shell = require("shell")

local args = {...}

function abort(message)
  print("Error: " .. message)
  os.exit(1)
end

-- return iterator values as an array.
function iteratorToArray(...)
  local array = {}
  for x in ... do
    array[#array + 1] = x
  end
  return array
end

-- go to the next index. supports overflow.
function next(cur, max)
  if cur >= max then
    return 1
  end
  return cur + 1
end

-- go to the previous index. supports underflow.
function prev(cur, max)
  if cur <= 1 then
    return max
  end
  return cur - 1
end

function main()
  if #args < 1 then
    abort("You must provide the source directory as an argument.")
  end

  local src = shell.getWorkingDirectory() .. "/" .. args[1]

  if not fs.isDirectory(src) then
    abort("The provided argument must be a directory.")
  end

  print("Using " .. src .. " as the source directory.")

  local images = iteratorToArray(fs.list(src))
  print(#images .. " images loaded.")
  print()
  print("Use the left and right arrow keys to navigate between images.")
  print("Press enter to exit the slideshow.")

  -- by starting at 0, the first image to the right will
  -- be 1, and the first image to the left will be max.
  local current = 0

  while true do
    local _, _, ascii, _, _ = event.pull("key_down")
    if ascii == 63235 then
      -- right arrow key.
      current = next(current, #images)
      ctif.clearScreen()
      ctif.displayImage(src .. "/" .. images[current])
    elseif ascii == 63234 then
      -- left arrow key.
      current = prev(current, #images)
      ctif.clearScreen()
      ctif.displayImage(src .. "/" .. images[current])
    elseif ascii == 13 then
      -- enter.
      ctif.resetResolution()
      ctif.clearScreen()
      os.exit()
    end
  end
end

main()
