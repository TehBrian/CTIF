local shell = require("shell")
local os = require("os")

local args = {...}

if #args < 2 then
  print("You must provide the source URL and the save location as arguments.")
  os.exit(1)
end

local url = args[1]

local saveLoc = args[2]

-- auto-apply .ctif
local suffix = ".ctif"
local hasSuffix = url:sub(-string.len(suffix)) == suffix
if not hasSuffix then
  saveLoc = saveLoc .. ".ctif"
end

shell.execute("wget <server>?url=" .. url .. " " .. saveLoc)

if #args > 2 and args[3] == "view" then
  shell.execute("ctif-view-oc " .. saveLoc)
end
