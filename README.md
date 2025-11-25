# SimpleSkyblock Plugin

A simple Paper plugin for Minecraft 1.21.10 that automatically generates a skyblock island for each new player that joins the server.

## Features

- Automatically generates a simple island for new players
- Islands are spaced 350 blocks apart in a spiral pattern
- Islands are generated from a custom structure (easily customizable)
- Saves island locations per player
- Teleports players to their island on join
- Randomly select coordinates for a custom stronghold structure (easily customizable)
  - Always generates at Y -40
  - Placed when any player first loads the chunks where the structure is located
  - Use an eye of ender to find the direction of the structure
    - Due to how minecraft works the eye of ender usage will throw a snowball instead of the actual eye of ender, keep an eye open for that
- Right-click on obsidian with a bucket to get your lava back

## Building

To build the plugin, you need Maven installed. Run:

```bash
cd plugin
./gradlew build
```

The compiled JAR file will be in `target/SimpleSkyblock-{version}.jar`

## Installation

1. Copy the JAR file to your Paper server's `plugins` folder
2. Restart your server
3. The plugin will automatically generate islands for new players

## Configuration

The plugin stores player island data in `plugins/SimpleSkyblock/` directory. Each player's island location is saved in a YAML file named after their UUID.

## Island Generation

- Islands are generated at Y={64:70} (standard sea level)
- Islands are spaced 350 blocks apart
- The first island is at (0, {64:70}, 0)
- Later islands follow a spiral pattern
- Due to how structures are generated, islands may not be perfectly centered (could offset a few blocks)

