package dev.ricr.skyblock.permissions;

import dev.ricr.skyblock.utils.ServerUtils;

import java.util.UUID;

public final class Requirements {

    public static Requirement<ActionContext> permission(String permission) {
        return actionContext -> {
            if (actionContext.player().isOp() && ServerUtils.isOpOverride()) {
                return true;
            }

            if (Requirements.hasPermission(actionContext, permission)) {
                if (actionContext.cancellable() != null) {
                    actionContext.cancellable().setCancelled(false);
                }
                return true;
            }

            if (actionContext.cancellable() != null) {
                actionContext.cancellable().setCancelled(true);
            }

            return false;
        };
    }

    private static boolean hasPermission(ActionContext actionContext, String permission) {
        var player = actionContext.player();
        var world = player.getWorld();

        if (world.getName().equals("lobby")) {
            return false;
        }

        if (world.getName().contains(player.getUniqueId().toString())) {
            return true;
        }

        var islandUniqueId = world.getName()
                .replace("islands/", "")
                .replace("_nether", "");
        var islandRecord = actionContext.plugin().islandManager.getIslandRecord(UUID.fromString(islandUniqueId));
        var permissionEnumValue = Policies.PoliciesEnum.getByLabel(permission);

        return islandRecord.islandPermissions().getPermissions().get(permissionEnumValue);
    }

}
