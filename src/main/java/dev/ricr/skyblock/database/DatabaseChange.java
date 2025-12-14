package dev.ricr.skyblock.database;

public sealed interface DatabaseChange permits
        DatabaseChange.AuctionHouseItemAdd,
        DatabaseChange.AuctionHouseItemRemove,
        DatabaseChange.AuctionHouseTransactionAdd,
        DatabaseChange.GambleRecordAdd,
        DatabaseChange.SaleRecordAdd,
        DatabaseChange.TrustedPlayerAdd,
        DatabaseChange.TrustedPlayerRemove,
        DatabaseChange.UserCreateOrUpdate {

    record AuctionHouseItemAdd(AuctionHouse auctionHouse) implements DatabaseChange {
    }

    record AuctionHouseItemRemove(AuctionHouse auctionHouse) implements DatabaseChange {
    }

    record AuctionHouseTransactionAdd(AuctionHouseTransaction auctionHouseTransaction) implements DatabaseChange {
    }

    record GambleRecordAdd(Gamble gamble) implements DatabaseChange {
    }

    record SaleRecordAdd(Sale sale) implements DatabaseChange {
    }

    record TrustedPlayerAdd(Island island, User trustedUser) implements DatabaseChange {
    }

    record TrustedPlayerRemove(String islandOwnerUniqueId, String trustedUserId) implements DatabaseChange {
    }

    record UserCreateOrUpdate(User player) implements DatabaseChange {
    }
}
