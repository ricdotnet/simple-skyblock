package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class PlayerFastBoard {
    private final SimpleSkyblock plugin;
    @Getter
    private final FastBoard fastBoard;

    public PlayerFastBoard(SimpleSkyblock plugin, Player player) {
        this.plugin = plugin;
        this.fastBoard = new FastBoard(player);

        var title = "<gold>Simple Skyblock";
        fastBoard.updateTitle(this.plugin.miniMessage.deserialize(title));

        this.updateMoney(player);
        this.updateDeaths(player);
        this.updatePing(player);
    }

    public void updateMoney(Player player) {
        var playerEntity = this.plugin.onlinePlayers.getOnlinePlayers().get(player.getUniqueId());
        var moneyLine = String.format("<green>\uD83D\uDCB2 <white>Money <green>%s", formatMoney(playerEntity.getBalance()));
        this.fastBoard.updateLine(1, this.plugin.miniMessage.deserialize(moneyLine));
    }

    public void updateMoney(PlayerEntity player) {
        var moneyLine = String.format("<green>\uD83D\uDCB2 <white>Money <green>%s", formatMoney(player.getBalance()));
        this.fastBoard.updateLine(1, this.plugin.miniMessage.deserialize(moneyLine));
    }

    public void updateDeaths(Player player) {
        var deaths = player.getStatistic(Statistic.DEATHS);
        var deathsLine = String.format("<red>\uD83D\uDC80 <white>Deaths <red>%d", deaths);
        this.fastBoard.updateLine(2, this.plugin.miniMessage.deserialize(deathsLine));
    }

    public void updatePing(Player player) {
        var pingLine = String.format("<gray>Ping (<blue>%dms</blue>)", player.getPing());
        this.fastBoard.updateLine(4, this.plugin.miniMessage.deserialize(pingLine));
    }

    private String formatMoney(double money) {
        return String.format("$%,.2f", money);
    }

    public void update(Player player) {
        this.updateMoney(player);
        this.updateDeaths(player);
        this.updatePing(player);
    }
}
