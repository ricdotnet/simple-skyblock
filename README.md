# SimpleSkyblock Plugin

A simple Paper plugin for Minecraft `26.3` that allows every player to have their own island.

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

### config.yml

The main configuration lives in `plugins/SimpleSkyblock/config.yml`. A sample configuration looks like this:

```yaml
# should the server owner want a lobby world to be the initial spawning world
lobby: true
end_portal_price: 100000
max_player_warps: 3
play_time_key_countdown: 14400 # this is in seconds
island:
  starting_border_radius: 60
  show_seed_price: 25000
  expand_price: 2000 # price per block - 10 blocks means 10 * price
warps:
  server:
    - name: "keys"
      world: "lobby"
      x: 0.5
      y: 44
      z: 0.5
  reserved_names:
    - "warp"
    - "spawn"
    - "shop"
    # ...add as many reserved names as needed
key_chests:
  - name: "PLAYTIME_KEY"
    display_name: "Playtime Key"
    world: "lobby"
    x: 0
    y: 44
    z: 6
```

| Option                          | Description                                                                                      |
|---------------------------------|--------------------------------------------------------------------------------------------------|
| `lobby`                         | Use a lobby / spawn world as the world players join in. Defaults to `true`                       |
| `end_portal_price`              | Price to unlock the End Portal at the lobby / spawn world                                        |
| `max_player_warps`              | Maximum number of warps each player can create. Defaults to `3`                                  |
| `play_time_key_countdown`       | Play time (in seconds) a player needs to earn a playtime key. Defaults to `14400` (4 hours)      |
| `island.starting_border_radius` | Initial world border radius of a newly created island                                            |
| `island.show_seed_price`        | Price to reveal the island world seed                                                            |
| `island.expand_price`           | Price per block when expanding the island border                                                 |
| `warps.server`                  | List of server warps (`name`, `world`, `x`, `y`, `z`) registered on startup                      |
| `warps.reserved_names`          | List of warp names players cannot use                                                            |
| `key_chests`                    | List of key chests (`name`, `display_name`, `world`, `x`, `y`, `z`) placed when the server loads |

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

