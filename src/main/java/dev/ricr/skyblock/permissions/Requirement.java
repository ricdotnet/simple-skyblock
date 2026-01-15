package dev.ricr.skyblock.permissions;

@FunctionalInterface
public interface Requirement<C> {
    boolean test(C context);

    default Requirement<C> or(Requirement<C> other) {
        return ctx -> this.test(ctx) || other.test(ctx);
    }
}
