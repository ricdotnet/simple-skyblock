package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class PlayerScoreboard {
    private final SimpleSkyblock plugin;
    private final Scoreboard scoreboard;

    public PlayerScoreboard(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                "skyblockSidebar",
                Criteria.DUMMY,
                Component.text("Simple Skyblock", NamedTextColor.GOLD)
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        var playerBalance = this.plugin.onlinePlayers.getPlayer(player.getUniqueId()).getBalance();

        // --- Money line ---
        var moneyTeam = scoreboard.registerNewTeam("money");
        moneyTeam.addEntry(ChatColor.GREEN.toString());
        moneyTeam.prefix(Component.text("Money: ", NamedTextColor.GREEN));
        moneyTeam.suffix(Component.text(formatMoney(playerBalance), NamedTextColor.WHITE));
        objective.getScore(ChatColor.GREEN.toString()).setScore(2);

        // --- Deaths line ---
        var deathsTeam = scoreboard.registerNewTeam("deaths");
        deathsTeam.addEntry(ChatColor.RED.toString());
        deathsTeam.prefix(Component.text("Deaths: ", NamedTextColor.RED));
        deathsTeam.suffix(Component.text(String.valueOf(player.getStatistic(Statistic.DEATHS)), NamedTextColor.WHITE));
        objective.getScore(ChatColor.RED.toString()).setScore(1);

        player.setScoreboard(scoreboard);
    }

    public void setMoneyObjective(UUID playerId) {
        var moneyTeam = scoreboard.getTeam("money");

        assert moneyTeam != null;
        var playerBalance = this.plugin.onlinePlayers.getPlayer(playerId).getBalance();

        moneyTeam.suffix(Component.text(formatMoney(playerBalance), NamedTextColor.WHITE));
    }

    public void setDeathsObjective(Player player) {
        var deathsTeam = scoreboard.getTeam("deaths");
        var deaths = player.getStatistic(Statistic.DEATHS);

        assert deathsTeam != null;
        deathsTeam.suffix(Component.text(String.valueOf(deaths), NamedTextColor.WHITE));
    }

    private String formatMoney(double money) {
        return String.format("$%,.2f", money);
    }
}
