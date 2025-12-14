package dev.ricr.skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
            player.sendMessage(Component.text(String.format("You already have an ongoing gamble session. Type /gamble %s to join",
                    player.getName()), NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        }

        var hostUser = this.plugin.onlinePlayers.getPlayer(player.getUniqueId());
        if (hostUser == null) {
            // somehow we broke the game
            return Command.SINGLE_SUCCESS;
        }

        if (hostUser.getBalance() < amount) {
            player.sendMessage(Component.text(String.format("Your balance is less than you tried to gamble for %s%s",
                    ServerUtils.COIN_SYMBOL, ServerUtils.formatMoneyValue(amount)), NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        var gambleSession = new GambleSessionGUI(this.plugin, player, amount);
        gambleSessions.put(player.getUniqueId(), gambleSession);

        player.openInventory(gambleSession.getInventory());
        var globalMessage = Component.text(String.format("%s started a gamble session with %s%s bets. Do /gamble %s to join",
                        player.getName(), ServerUtils.COIN_SYMBOL, amount, player.getName()),
                NamedTextColor.GREEN);

        this.plugin.getServer().broadcast(globalMessage);
        this.initiateGambleSessionCountdown(gambleSession);

        return Command.SINGLE_SUCCESS;
    }

    private int joinGambleSession(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var sender = ctx.getSource().getSender();
        var player = ServerUtils.ensureCommandSenderIsPlayer(sender);

        var hostPlayer = this.resolvePlayerFromCommandArgument(sender, ctx);

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
                var user = this.plugin.databaseManager.getUsersDao().queryForId(player.getUniqueId().toString());

                if (user.getBalance() < gambleSessionGUI.getOriginalAmount()) {
                    player.sendMessage(Component.text(String.format("You cannot join a gamble session with less than %s%s",
                                    ServerUtils.COIN_SYMBOL,
                                    ServerUtils.formatMoneyValue(gambleSessionGUI.getOriginalAmount())),
                            NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
            } catch (SQLException e) {
                // ignore for now
            }

            gambleSessionGUI.addPlayer(player);

            hostPlayer.sendMessage(Component.text(String.format("%s joined your gamble session",
                    player.getName()), NamedTextColor.AQUA));

            player.sendMessage(Component.text(String.format("You joined the gamble session of %s",
                            hostPlayer.getName()),
                    NamedTextColor.YELLOW));
        }

        player.openInventory(gambleSessionGUI.getInventory());

        return Command.SINGLE_SUCCESS;
    }

    private void initiateGambleSessionCountdown(GambleSessionGUI gambleSession) {
        Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, (task) -> {
                    long remaining = gambleSession.getCountdownClock()
                            .decrementAndGet();

                    if (remaining <= 0) {
                        task.cancel();

                        Bukkit.getGlobalRegionScheduler()
                                .execute(plugin, () -> {
                                    gambleSession.chooseWinner();
                                    gambleSessions.remove(gambleSession.getHost()
                                            .getUniqueId());
                                });
                        return;
                    }

                    Bukkit.getGlobalRegionScheduler()
                            .execute(plugin, () -> gambleSession.updateCountdownClock((int) remaining));
                }, 1, 1, TimeUnit.SECONDS);
    }

    private Player resolvePlayerFromCommandArgument(CommandSender sender, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        var players = resolver.resolve(ctx.getSource());
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return null;
        }

        return players.getFirst();
    }
}
