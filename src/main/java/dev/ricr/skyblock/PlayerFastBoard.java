package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class PlayerFastBoard {
    private final SimpleSkyblock plugin;
    @Getter
    private final FastBoard fastBoard;

    public PlayerFastBoard(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;
        this.fastBoard = new FastBoard(player);

        var title = "<gold>Simple Skyblock";
        fastBoard.updateTitle(this.plugin.miniMessage.deserialize(title));

        this.updateMoney();
        this.updateDeaths();
        this.updateGamble(null);
        this.updatePing();

        Bukkit.getScheduler().runTaskTimer(this.plugin, this::updatePing, 0, 20);
    }

    public void updateMoney() {
        var player = this.fastBoard.getPlayer();
        var playerEntity = this.plugin.onlinePlayers.getOnlinePlayers().get(player.getUniqueId());
        var moneyLine = String.format("<green>\uD83D\uDCB2 <white>Money <green>%s", ServerUtils.formatMoneyValue(playerEntity.getBalance()));
        this.fastBoard.updateLine(1, this.plugin.miniMessage.deserialize(moneyLine));
    }

    public void updateMoney(PlayerEntity player) {
        var moneyLine = String.format("<green>\uD83D\uDCB2 <white>Money <green>%s", ServerUtils.formatMoneyValue(player.getBalance()));
        this.fastBoard.updateLine(1, this.plugin.miniMessage.deserialize(moneyLine));
    }

    public void updateDeaths() {
        var player = this.fastBoard.getPlayer();
        var deaths = player.getStatistic(Statistic.DEATHS);
        var deathsLine = String.format("<red>\uD83D\uDC80 <white>Deaths <red>%d", deaths);
        this.fastBoard.updateLine(2, this.plugin.miniMessage.deserialize(deathsLine));
    }

    public void updatePing() {
        var player = this.fastBoard.getPlayer();
        var pingLine = String.format("<gray>Ping (<blue>%dms</blue>)", player.getPing());
        this.fastBoard.updateLine(7, this.plugin.miniMessage.deserialize(pingLine));
    }

    public void updateGamble(@Nullable GambleSessionGUI gambleSessionGUI) {
        if (gambleSessionGUI == null) {
            var gambleLine = String.format("<blue>\uD83C\uDFB2 <white>Gamble: <yellow>%s", ServerUtils.formatMoneyValue(0));
            var gambleInfoLine = "<blue>\uD83D\uDD51 <red>not gambling";
            this.fastBoard.updateLine(4, this.plugin.miniMessage.deserialize(gambleLine));
            this.fastBoard.updateLine(5, this.plugin.miniMessage.deserialize(gambleInfoLine));
        } else {
            var countdown = gambleSessionGUI.getCountdownClock().get();
            var amount = gambleSessionGUI.getAmount();
            var gambleLine = String.format("<blue>\uD83C\uDFB2 <white>Gamble: <yellow>%s", ServerUtils.formatMoneyValue(amount));
            var gambleInfoLine = String.format("<blue>\uD83D\uDD51 <white>Countdown: <yellow>%s", countdown + "s");
            this.fastBoard.updateLine(4, this.plugin.miniMessage.deserialize(gambleLine));
            this.fastBoard.updateLine(5, this.plugin.miniMessage.deserialize(gambleInfoLine));
        }
    }
}
