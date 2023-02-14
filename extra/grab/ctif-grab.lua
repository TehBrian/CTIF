local server = "https://thbn.me/ctif-provide"

local shell = require("shell")
local os = require("os")

local args = {...}

if #args < 2 then
  print("Error: You must provide the source URL and the save location as arguments.")
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

local view = false
if #args > 2 and (args[3] == "view" or args[3] == "open") then
  view = true
end

print()
print("Grabbing: " .. url)
print("Saving at: " .. saveLoc)
print("Will view: " .. tostring(view))
print()

shell.execute("wget " .. server .. "?url=" .. url .. " " .. saveLoc)

if view then
  shell.execute("ctif-view-oc " .. saveLoc)
end
