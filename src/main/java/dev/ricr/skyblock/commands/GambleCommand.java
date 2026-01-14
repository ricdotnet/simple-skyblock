package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.utils.PlayerUtils;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GambleCommand implements ICommand {

    private final SimpleSkyblock plugin;
    private final Map<UUID, GambleSessionGUI> gambleSessions;

    public GambleCommand(SimpleSkyblock plugin) {
        this.plugin = plugin;
        this.gambleSessions = new HashMap<>();
    }

    public void register() {
        this.plugin.getLifecycleManager()
                .registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                    LiteralCommandNode<CommandSourceStack> gamble = this.command();
                    commands.registrar().register(gamble);
                });
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("gamble")
                .then(Commands.literal("start")
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(ServerUtils.GAMBLE_MINIMUM_BALANCE, ServerUtils.GAMBLE_MAXIMUM_BALANCE))
                                .executes(this::startGambleSession)
                        )
                )
                .then(Commands.literal("join")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(this::joinGambleSession)
                        )
                )
                .build();
    }

    private int startGambleSession(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var amount = ctx.getArgument("amount", Double.class);

        if (gambleSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an ongoing gamble session", NamedTextColor.GREEN)
                    .appendNewline()
                    .append(Component.text(String.format("Type /gamble join %s to see who joined", player.getName()), NamedTextColor.GREEN))
            );
            return Command.SINGLE_SUCCESS;
        }

        var hostPlayer = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());
        if (hostPlayer == null) {
            // somehow we broke the game
            return Command.SINGLE_SUCCESS;
        }

        var hostPlayerEntity = hostPlayer.getPlayerEntity();
        if (hostPlayerEntity.getBalance() < amount) {
            player.sendMessage(Component.text(String.format("Your balance is less than you tried to gamble for %s",
                    ServerUtils.formatMoneyValue(amount)), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var gambleSession = new GambleSessionGUI(this.plugin, player, amount);
        gambleSessions.put(player.getUniqueId(), gambleSession);

        player.openInventory(gambleSession.getInventory());
        var globalMessage = "<green>" + player.getName() + "</green><head:" + player.getUniqueId() + "> <white>started a gambling session with " +
                "<gold>" + ServerUtils.formatMoneyValue(amount) + "</gold> bets" +
                "<newline>To join <click:run_command:gamble join " + player.getName() + "><aqua>click here<aqua></click> or type <green>/gamble join <name></green>";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId() == player.getUniqueId()) {
                continue;
            }
            onlinePlayer.sendMessage(this.plugin.miniMessage.deserialize(globalMessage));
        }

        this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getFastBoard().updateGamble(gambleSession);
        this.initiateGambleSessionCountdown(gambleSession);

        return Command.SINGLE_SUCCESS;
    }

    private int joinGambleSession(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var hostPlayer = ServerUtils.resolvePlayerFromCommandArgument(sender, ctx);

        if (hostPlayer == null) {
            player.sendMessage(Component.text("No gamble session found for that player",
                    NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var gambleSessionGUI = gambleSessions.get(hostPlayer.getUniqueId());
        if (gambleSessionGUI == null) {
            player.sendMessage(Component.text(String.format("No gamble session found for player %s", hostPlayer.getName()),
                    NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (gambleSessionGUI.getPlayers().size() == 5) {
            player.sendMessage(Component.text("The gamble session is full", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        if (gambleSessionGUI.getHost()
                .getUniqueId() == player.getUniqueId()) {
            player.sendMessage(Component.text("You rejoined your own gamble session", NamedTextColor.YELLOW));
        } else {

            try {
                var playerEntity = this.plugin.databaseManager.getPlayersDao().queryForId(player.getUniqueId().toString());

                if (playerEntity.getBalance() < gambleSessionGUI.getOriginalAmount()) {
                    player.sendMessage(Component.text(String.format("You cannot join a gamble session with less than %s",
                                    ServerUtils.formatMoneyValue(gambleSessionGUI.getOriginalAmount())),
                            NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
            } catch (SQLException e) {
                // ignore for now
            }

            gambleSessionGUI.addPlayer(player);

            var playerJoinedMessage = "<green>" + player.getName() + "<white><head:" + player.getUniqueId() + "> joined your gamble session";
            hostPlayer.sendMessage(this.plugin.miniMessage.deserialize(playerJoinedMessage));

            var youJoinedMessage = "You joined the gamble session of <green>" + hostPlayer.getName() + "</green><head:" + hostPlayer.getUniqueId() + ">";
            player.sendMessage(this.plugin.miniMessage.deserialize(youJoinedMessage));
        }

        player.openInventory(gambleSessionGUI.getInventory());

        return Command.SINGLE_SUCCESS;
    }

    private void initiateGambleSessionCountdown(GambleSessionGUI gambleSession) {
        Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, (task) -> {
                    long remaining = gambleSession.getCountdownClock()
                            .decrementAndGet();

                    for (Player player : gambleSession.getPlayers()) {
                        if (remaining > 0 && remaining <= 3) {
                            var message = Component.text(String.format("Gamble ends in: %s", remaining), NamedTextColor.RED);
                            PlayerUtils.showTitleMessage(plugin, player, message);
                        }

                        this.plugin.onlinePlayers.getOnlinePlayers()
                                .get(player.getUniqueId())
                                .getFastBoard()
                                .updateGamble(gambleSession);
                    }

                    if (remaining <= 0) {
                        task.cancel();

                        Bukkit.getGlobalRegionScheduler()
                                .execute(plugin, () -> {
                                    gambleSession.chooseWinner();
                                    gambleSessions.remove(gambleSession.getHost().getUniqueId());

                                    for (Player player : gambleSession.getPlayers()) {
                                        // reset fastboard gamble lines
                                        this.plugin.onlinePlayers.getOnlinePlayers()
                                                .get(player.getUniqueId())
                                                .getFastBoard()
                                                .updateGamble(null);
                                    }
                                });
                        return;
                    }
                }, 1, 1, TimeUnit.SECONDS);
    }
}
