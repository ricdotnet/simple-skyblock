package dev.ricr.skyblock;

import dev.ricr.skyblock.database.PlayerEntity;
import dev.ricr.skyblock.gui.GambleSessionGUI;
import dev.ricr.skyblock.utils.ServerUtils;
import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class PlayerFastBoard {
    private final SimpleSkyblock plugin;
    @Getter
    private final FastBoard fastBoard;

    public PlayerFastBoard(SimpleSkyblock plugin, Player player, PlayerEntity playerEntity) {
        this.plugin = plugin;
        this.fastBoard = new FastBoard(player);

        var title = "<gold>ꜱɪᴍᴘʟᴇ ꜱᴋʏʙʟᴏᴄᴋ";
        fastBoard.updateTitle(this.plugin.miniMessage.deserialize(title));

        this.updateMoney(playerEntity);
        this.updateDeaths();
        this.updateGamble(null);
        this.updatePing();
    }

    public void updateWorldTime() {
        var player = this.fastBoard.getPlayer();
        var currentWorld = player.getWorld();
        var currentTime = currentWorld.getTime();
        var currentTimeLine = String.format("<green>⌚ <white>ᴛɪᴍᴇ <green>%s", ServerUtils.longToTime(currentTime));
        this.fastBoard.updateLine(2, this.plugin.miniMessage.deserialize(currentTimeLine));
    }

    public void updateMoney() {
        var player = this.fastBoard.getPlayer();
        var playerEntity = this.plugin.onlinePlayers.getOnlinePlayers().get(player.getUniqueId()).getPlayerEntity();
        var moneyLine = String.format("<gold>\uD83D\uDCB2 <white>ᴍᴏɴᴇʏ <gold>%s", ServerUtils.formatMoneyValue(playerEntity.getBalance()));
        this.fastBoard.updateLine(4, this.plugin.miniMessage.deserialize(moneyLine));
    }

    public void updateMoney(PlayerEntity player) {
        var moneyLine = String.format("<gold>\uD83D\uDCB2 <white>ᴍᴏɴᴇʏ <gold>%s", ServerUtils.formatMoneyValue(player.getBalance()));
        this.fastBoard.updateLine(4, this.plugin.miniMessage.deserialize(moneyLine));
    }

    public void updateDeaths() {
        var player = this.fastBoard.getPlayer();
        var deaths = player.getStatistic(Statistic.DEATHS);
        var deathsLine = String.format("<red>\uD83D\uDC80 <white>ᴅᴇᴀᴛʜꜱ <red>%d", deaths);
        this.fastBoard.updateLine(5, this.plugin.miniMessage.deserialize(deathsLine));
    }

    public void updatePing() {
        var player = this.fastBoard.getPlayer();
        var pingLine = String.format("<gray>ᴘɪɴɢ (<blue>%dms</blue>)", player.getPing());
        this.fastBoard.updateLine(12, this.plugin.miniMessage.deserialize(pingLine));
    }

    public void updateWorld(String worldName) {
        var worldLine = String.format("<aqua>\uD83C\uDF0D <white>ᴡᴏʀʟᴅ <aqua>%s", worldName);
        this.fastBoard.updateLine(1, this.plugin.miniMessage.deserialize(worldLine));
    }

    public void updatePlayTime(int playTime) {
        int minutes = playTime / 60;
        int seconds = playTime % 60;

        var timeString = String.format("%02dm %02ds", minutes, seconds);

        var worldLine = String.format("<dark_purple>\uD83D\uDD11 <white>ᴋᴇʏ <dark_purple>%s", timeString);
        this.fastBoard.updateLine(7, this.plugin.miniMessage.deserialize(worldLine));
    }

    public void updateGamble(@Nullable GambleSessionGUI gambleSessionGUI) {
        if (gambleSessionGUI == null) {
            var gambleLine = String.format("<blue>\uD83C\uDFB2 <white>ɢᴀᴍʙʟᴇ: <blue>%s", ServerUtils.formatMoneyValue(0));
            var gambleInfoLine = "<blue>\uD83D\uDD51 <white>ɴᴏᴛ ɢᴀᴍʙʟɪɴɢ";
            this.fastBoard.updateLine(9, this.plugin.miniMessage.deserialize(gambleLine));
            this.fastBoard.updateLine(10, this.plugin.miniMessage.deserialize(gambleInfoLine));
        } else {
            var countdown = gambleSessionGUI.getCountdownClock().get();
            var amount = gambleSessionGUI.getAmount();
            var gambleLine = String.format("<blue>\uD83C\uDFB2 <white>ɢᴀᴍʙʟᴇ: <blue>%s", ServerUtils.formatMoneyValue(amount));
            var gambleInfoLine = String.format("<blue>\uD83D\uDD51 <white>ᴄᴏᴜɴᴛᴅᴏᴡɴ: <blue>%s", countdown + "s");
            this.fastBoard.updateLine(9, this.plugin.miniMessage.deserialize(gambleLine));
            this.fastBoard.updateLine(10, this.plugin.miniMessage.deserialize(gambleInfoLine));
        }
    }
}
