package dev.ricr.skyblock.permissions;

import dev.ricr.skyblock.utils.Messages;
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

            if (actionContext.shouldNotify()) {
                actionContext.player().sendMessage(Messages.CANNOT_DO_THAT_HERE.component(actionContext.plugin()));
            }

            return false;
        };
    }

    private static boolean hasPermission(ActionContext actionContext, String permission) {
        return false;
    }

}
