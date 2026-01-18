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
                // There is a small issue with the nether portal travel, we want to always leave it in a canceled state
                // but other policies, because ALL will cancel them by default (unless ALL has been enabled)
                // we want to revert back from a canceled state
                if (actionContext.cancellable() != null) {
                    actionContext.cancellable().setCancelled(Policies.PoliciesEnum.PORTAL_TRAVEL.getLabel().equals(permission));
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

        // I will deal with ALL permissions later
        if (Policies.PoliciesEnum.ALL.getLabel().equals(permission)) {
            return false;
        }

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

        // TODO: update how we do blocked player checks but for now we just reject all that they do
        for (var blockedPlayer : islandRecord.blockedPlayers()) {
            if (blockedPlayer.getFirst().equals(player.getUniqueId().toString())) {
                return false;
            }
        }

        // If the player is trusted ignore the permission check
        for (var trustedPlayer : islandRecord.trustedPlayers()) {
            if (trustedPlayer.getFirst().equals(player.getUniqueId().toString())) {
                return true;
            }
        }

        var permissionEnumValue = Policies.PoliciesEnum.getByLabel(permission);
        return islandRecord.islandPermissions().getPermissions().get(permissionEnumValue);
    }

}
