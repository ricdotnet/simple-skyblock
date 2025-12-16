package dev.ricr.skyblock.database;

public sealed interface DatabaseChange permits
        DatabaseChange.AuctionHouseItemAdd,
        DatabaseChange.AuctionHouseItemRemove,
        DatabaseChange.AuctionHouseTransactionAdd,
        DatabaseChange.GambleRecordAdd,
        DatabaseChange.SaleRecordAdd,
        DatabaseChange.TrustedPlayerAdd,
        DatabaseChange.TrustedPlayerRemove,
        DatabaseChange.PlayerCreateOrUpdate {

    record AuctionHouseItemAdd(AuctionHouseItemEntity auctionHouseItem) implements DatabaseChange {
    }

    record AuctionHouseItemRemove(AuctionHouseItemEntity auctionHouseItem) implements DatabaseChange {
    }

    record AuctionHouseTransactionAdd(AuctionHouseTransactionEntity auctionHouseTransaction) implements DatabaseChange {
    }

    record GambleRecordAdd(GambleEntity gamble) implements DatabaseChange {
    }

    record SaleRecordAdd(SaleEntity sale) implements DatabaseChange {
    }

    record TrustedPlayerAdd(IslandEntity island, PlayerEntity trustedPlayer) implements DatabaseChange {
    }

    record TrustedPlayerRemove(String islandOwnerUniqueId, String trustedPlayerId) implements DatabaseChange {
    }

    record PlayerCreateOrUpdate(PlayerEntity player) implements DatabaseChange {
    }
}
