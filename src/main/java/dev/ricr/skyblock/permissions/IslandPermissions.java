package dev.ricr.skyblock.permissions;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class IslandPermissions {
    private Map<Policies.PoliciesEnum, Boolean> permissions = new HashMap<>();

    public IslandPermissions() {
        // used when the permissions are first added to a player
        this.deserialize("all=false:villager_trading=false:open_inventories=false:kill_mobs=false:portal_travel=false:place_blocks=false:break_blocks=false:interact_with_mobs=false:open_doors=false");
    }

    public IslandPermissions(String permissions) {
        this.deserialize(permissions);
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
