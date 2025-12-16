package dev.ricr.skyblock.utils;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ricr.skyblock.SimpleSkyblock;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class ServerUtils {
    private static final Logger logger = Logger.getLogger("SimpleSkyblock");

    public static final int MIN_STRONGHOLD_LOCATION = -3500;
    public static final int MAX_STRONGHOLD_LOCATION = 3500;

    public static final String COIN_SYMBOL = "$";

    public static final int GAMBLE_MINIMUM_BALANCE = 100;
    public static final int GAMBLE_MAXIMUM_BALANCE = 5000;
    public static final int GAMBLE_COUNTDOWN = 30;

    public static final int AUCTION_HOUSE_MAX_LISTINGS = 10;

    // auction house GUI
    public static NamespacedKey AUCTION_HOUSE_ITEM_ID;
    public static final String AUCTION_NEXT_PAGE = "auction_next_page";
    public static final String AUCTION_PREVIOUS_PAGE = "auction_previous_page";
    public static final String AUCTION_REFRESH_BUTTON = "auction_refresh_button";

    // other GUI
    public static NamespacedKey GUI_BUTTON_TYPE;

    // custom display entities
    public static TextDisplay END_PORTAL_TEXT_DISPLAY;

    // sign shop
    public static NamespacedKey SIGN_SHOP_TYPE;
    public static NamespacedKey SIGN_SHOP_OUT_ITEM;
    public static NamespacedKey SIGN_SHOP_IN_ITEM;
    public static NamespacedKey SIGN_SHOP_OWNER;

    public static FileConfiguration loadConfig(SimpleSkyblock plugin) {
        File serverConfig = new File(plugin.getDataFolder(), "config.yml");

        return YamlConfiguration.loadConfiguration(serverConfig);
    }

    public static void saveConfig(FileConfiguration config, File dataFolder) {
        File serverConfig = new File(dataFolder, "config.yml");

        try {
            config.save(serverConfig);
        } catch (Exception e) {
            logger.severe("Failed to save config: " + e.getMessage());
        }
    }

    public static List<Component> getLoreOrEmptyComponentList(ItemMeta meta) {
        return meta.lore() != null ? meta.lore() : new ArrayList<>();
    }

    public static String getTextFromComponent(Component component) {
        return PlainTextComponentSerializer.plainText()
                .serialize(component);
    }

    public static String formatMoneyValue(double value) {
        return String.format("%s%s", ServerUtils.COIN_SYMBOL, Math.round(value * 100.00) / 100.00);
    }

    public static String base64FromBytes(byte[] bytes) {
        return java.util.Base64.getEncoder()
                .encodeToString(bytes);
    }

    public static byte[] bytesFromBase64(String base64) {
        return java.util.Base64.getDecoder()
                .decode(base64);
    }

    public static void initiateNamespacedKeys(SimpleSkyblock plugin) {
        AUCTION_HOUSE_ITEM_ID = new NamespacedKey(plugin, "auction_house_item");
        GUI_BUTTON_TYPE = new NamespacedKey(plugin, "gui_button_type");
        SIGN_SHOP_TYPE = new NamespacedKey(plugin, "sign_shop_type");
        SIGN_SHOP_OUT_ITEM = new NamespacedKey(plugin, "sign_shop_out_item");
        SIGN_SHOP_IN_ITEM = new NamespacedKey(plugin, "sign_shop_in_item");
        SIGN_SHOP_OWNER = new NamespacedKey(plugin, "sign_shop_owner");
    }

    public static World loadOrCreateLobby() {
        var lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            var lobbyWorldCreator = new WorldCreator("lobby");
            lobbyWorldCreator.generator("SimpleSkyblock");
            lobbyWorld = lobbyWorldCreator.createWorld();
        }

        return lobbyWorld;
    }

    public static World loadOrCreateWorld(UUID playerUniqueId, World.Environment environment, Long seed) {
        var suffix = playerUniqueId.toString() + (environment == World.Environment.NETHER ? "_nether" : "");
        var islandName = String.format("islands/%s", suffix);

        var islandWorld = Bukkit.getWorld(islandName);
        if (islandWorld == null) {
            var worldCreator = new WorldCreator(islandName);
            if (environment != null) {
                worldCreator.environment(environment);
            }
            if (seed != null) {
                worldCreator.seed(seed);
            }
            worldCreator.generator("SimpleSkyblock");
            islandWorld = worldCreator.createWorld();
        }

        return islandWorld;
    }

    public static Player ensureCommandSenderIsPlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new CommandException("This command can only be executed by players");
        }

        return player;
    }

    public static void setEndPortalTextDisplay(SimpleSkyblock plugin) {
        var lobbyWorld = ServerUtils.loadOrCreateLobby();
        var textDisplayLocation = new Location(lobbyWorld, 0.5, 67.5, -9.5);

        var theEndPortalPrice = plugin.serverConfig.getDouble("end_portal_price", 100000);
        var message = Component.text("To jump in", NamedTextColor.GREEN)
                .appendSpace()
                .append(Component.text("The End", NamedTextColor.DARK_PURPLE))
                .appendSpace()
                .append(Component.text(String.format("you need %s", ServerUtils.formatMoneyValue(theEndPortalPrice)), NamedTextColor.GREEN));

        var textDisplay = lobbyWorld.spawn(textDisplayLocation, TextDisplay.class, entity -> {
            entity.text(message);
            entity.setVisibleByDefault(true);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // transparent
            entity.setShadowed(true);
        });

        textDisplay.setTransformation(
                new Transformation(
                        new Vector3f(0, 0, 0),           // translation
                        new AxisAngle4f(0, 0, 0, 1),     // left rotation
                        new Vector3f(1.5f, 1.5f, 1.5f),  // scale (THIS controls size)
                        new AxisAngle4f(0, 0, 0, 1)      // right rotation
                )
        );

        ServerUtils.END_PORTAL_TEXT_DISPLAY = textDisplay;
    }

    public static void cleanUpTextDisplays(SimpleSkyblock plugin) {
        plugin.getServer().getLogger().info("Cleaning up end portal text displays...");

        // TODO: refactor later with a list of text displays with ephemeral and dynamic displays if needed
        plugin.getServer().getWorlds().forEach(world -> world.getEntitiesByClass(TextDisplay.class).forEach(Display::remove));
    }

    public static Player resolvePlayerFromCommandArgument(CommandSender sender, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        var players = resolver.resolve(ctx.getSource());
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return null;
        }

        return players.getFirst();
    }

    public static List<Component> wrapLore(
            String text,
            int maxLineLength,
            TextColor color
    ) {
        List<Component> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : text.split(" ")) {
            if (current.length() + word.length() + 1 > maxLineLength) {
                lines.add(Component.text(current.toString(), color));
                current.setLength(0);
            }

            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }

        if (!current.isEmpty()) {
            lines.add(Component.text(current.toString(), color));
        }

        return lines;
    }

    public static ArmorStand armorStandText(World world, Location textDisplayLocation, Component message) {
        return world.spawn(textDisplayLocation, ArmorStand.class, stand -> {
            stand.setMarker(true);
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.customName(message);
        });
    }
}
