# SimpleSkyblock Plugin

A simple Paper plugin for Minecraft 1.21.10 that automatically generates a skyblock island for each new player that joins the server.

## Features

- Automatically generates a simple island for new players
- Islands are spaced 200 blocks apart in a spiral pattern
- Each island includes:
  - A 7x7 grass platform (stone base, dirt layer, grass top)
  - An oak tree in the center
  - A chest for starter items
- Saves island locations per player
- Teleports players to their island on join

## Building

To build the plugin, you need Maven installed. Run:

```bash
cd plugin
./gradlew build
```

The compiled JAR file will be in `target/SimpleSkyblock-1.0.0.jar`

## Installation

1. Copy the JAR file to your Paper server's `plugins` folder
2. Restart your server
3. The plugin will automatically generate islands for new players

## Configuration

The plugin stores player island data in `plugins/SimpleSkyblock/` directory. Each player's island location is saved in a YAML file named after their UUID.

## Island Generation

- Islands are generated at Y=64 (standard sea level)
- Islands are spaced 200 blocks apart
- The first island is at (0, 64, 0)
- Subsequent islands follow a spiral pattern

