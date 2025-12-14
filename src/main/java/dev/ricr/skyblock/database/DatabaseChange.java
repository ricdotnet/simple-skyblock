package dev.ricr.skyblock.database;

public sealed interface DatabaseChange permits
        DatabaseChange.AuctionHouseItemAdd,
        DatabaseChange.GambleRecordAdd,
        DatabaseChange.TrustedPlayerAdd,
        DatabaseChange.TrustedPlayerRemove,
        DatabaseChange.UserCreateOrUpdate {

    record AuctionHouseItemAdd() implements DatabaseChange {
    }

    record GambleRecordAdd(Gamble gamble) implements DatabaseChange {
    }

    record TrustedPlayerAdd(Island island, User trustedUser) implements DatabaseChange {
    }

    record TrustedPlayerRemove(String islandOwnerUniqueId, String trustedUserId) implements DatabaseChange {
    }

    record UserCreateOrUpdate(User player) implements DatabaseChange {
    }
}
