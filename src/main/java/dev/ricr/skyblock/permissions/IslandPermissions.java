package dev.ricr.skyblock.permissions;

import dev.ricr.skyblock.SimpleSkyblock;
import dev.ricr.skyblock.database.DatabaseChange;
import dev.ricr.skyblock.database.IslandEntity;
import dev.ricr.skyblock.enums.SoundType;
import dev.ricr.skyblock.utils.PlayerUtils;
import lombok.Getter;
import lombok.Setter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class IslandPermissions {
    private final SimpleSkyblock plugin;
    private final UUID islandUniqueId;
    private Map<Policies.PoliciesEnum, Boolean> permissions = new HashMap<>();

    public IslandPermissions(SimpleSkyblock plugin, UUID islandUniqueId) {
        this.plugin = plugin;
        this.islandUniqueId = islandUniqueId;
        // used when the permissions are first added to a player
        this.deserialize("all=false:villager_trading=false:open_inventories=false:kill_mobs=false:portal_travel=false:place_blocks=false:break_blocks=false:interact_with_mobs=false:open_doors=false");
    }

    public IslandPermissions(SimpleSkyblock plugin, UUID islandUniqueId, String permissions) {
        this.plugin = plugin;
        this.islandUniqueId = islandUniqueId;
        this.deserialize(permissions);
    }

    public Boolean getPermissionValue(Policies.PoliciesEnum permission) {
        return this.permissions.get(permission);
    }

    public void switchPermission(Policies.PoliciesEnum permission) {
        this.permissions.compute(permission, (policyEnum, permissionValue) -> Boolean.FALSE.equals(permissionValue));
        var player = this.plugin.getServer().getPlayer(this.islandUniqueId);

        IslandEntity islandEntity;
        try {
            islandEntity = this.plugin.databaseManager.getIslandsDao().queryForId(this.islandUniqueId.toString());
        } catch (SQLException e) {
            this.permissions.compute(permission, (policyEnum, permissionValue) -> Boolean.FALSE.equals(permissionValue));
            this.plugin.getLogger().severe(String.format("Failed when trying to switch a permission for island id %s", islandUniqueId));
            PlayerUtils.playSound(player, SoundType.NEGATIVE);
            return;
        }

        islandEntity.setPermissions(this.toString());

        var islandRecordUpdate = new DatabaseChange.IslandRecordUpdate(islandEntity);
        this.plugin.databaseChangesAccumulator.add(islandRecordUpdate);

        PlayerUtils.playSound(player, SoundType.POSITIVE);
    }

    public void deserialize(String permissions) {
        for (String permission : permissions.split(":")) {
            var keyValue = permission.split("=");
            this.permissions.put(Policies.PoliciesEnum.getByLabel(keyValue[0]), keyValue[1].equals("true"));
        }
    }

    @Override
    public String toString() {
        return this.permissions.entrySet()
                .stream()
                .map(entry -> String.format("%s=%s", entry.getKey().getLabel(), entry.getValue()))
                .collect(java.util.stream.Collectors.joining(":"));
    }

}
