# SimpleSkyblock Plugin

A simple Paper plugin for Minecraft `>1.21.10` that every player to have their own island.

## Features

- Void World generator
- Automatically generates an island for new players when they use `/island create`
- Nether worlds are generated for each island, with a custom nether island structure
  - Each player has their own nether portal to the island's nether world
- Each island is set in its own world
  - With plans to add support for a single world with multiple islands
- Islands are generated from a custom structure
  - With plans to add support for structures added in `/resources`
- Teleports players to the lobby / spawn world on join, always
- Right-click on obsidian with a bucket to get a lava bucket
- The End is created based on the plugin's host preferences
  - The plugin supports an End Portal at the lobby / spawn world

### Other features

- Economy
  - Balance leaderboard
  - Pay command
- Auction house (works more like a marketplace than a traditional auction house)
  - Auction house history (player's own)
- Shop system (customizable via shop.yml)
- Gamble system
- Island trust and protection system
- Island border and expansion system
- Sign Trade shops (needs a tutorial)

[//]: # (- Randomly select coordinates for a custom stronghold structure &#40;easily customizable&#41;)
[//]: # (  - Always generates at Y -40)
[//]: # (  - Placed when any player first loads the chunks where the structure is located)
[//]: # (  - Use an eye of ender to find the direction of the structure)
[//]: # (    - Due to how minecraft works the eye of ender usage will throw a snowball instead of the actual eye of ender, keep an eye open for that)

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

For having the plugin generate the default lobby / spawn world as a void world, set the following in your `bukkit.yml` file:

```yaml
worlds:
  void_skyblock:
    generator: SimpleSkyblock
```

## Island Generation

### For multi-world support
- Islands are generated at Y={64} (standard sea level)
- Islands generate at X={0} and Z={0}
- The nether island structure is placed at X={0}, Y={64} and Z={0}

### For single-world support
- Coming soon

