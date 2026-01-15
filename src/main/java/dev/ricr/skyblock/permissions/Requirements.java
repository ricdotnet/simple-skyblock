package dev.ricr.skyblock.permissions;

import dev.ricr.skyblock.utils.ServerUtils;

public final class Requirements {

    public static Requirement<ActionContext> permission(String permission) {
        return actionContext -> {
            if (actionContext.player().isOp() && ServerUtils.isOpOverride()) {
                return true;
            }

            if (Requirements.hasPermission(actionContext, permission)) {
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

        if (world.getName().contains(player.getUniqueId().toString())) {
            return true;
        }

        // TODO: check for permissions here

        return false;
    }

}
