package dev.ricr.skyblock.database;

public sealed interface DatabaseChange
        permits DatabaseChange.UserBalanceDelta, DatabaseChange.AuctionHouseItemAdd, DatabaseChange.TrustedPlayerAdd, DatabaseChange.TrustedPlayerRemove {

    record UserBalanceDelta(String playerUniqueId, double delta) implements DatabaseChange {
    }

    record AuctionHouseItemAdd() implements DatabaseChange {
    }

    record TrustedPlayerAdd(Island island, User trustedUser) implements DatabaseChange {
    }

    record TrustedPlayerRemove(String islandOwnerUniqueId, String trustedUserId) implements DatabaseChange {
    }
}
