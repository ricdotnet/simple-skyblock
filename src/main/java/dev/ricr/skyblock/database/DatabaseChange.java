package dev.ricr.skyblock.database;

public sealed interface DatabaseChange permits
        DatabaseChange.AuctionHouseItemAdd,
        DatabaseChange.AuctionHouseItemRemove,
        DatabaseChange.BlockedPlayerAdd,
        DatabaseChange.BlockedPlayerRemove,
        DatabaseChange.GambleRecordAdd,
        DatabaseChange.IslandRecordUpdate,
        DatabaseChange.PlayerCreateOrUpdate,
        DatabaseChange.TransactionAdd,
        DatabaseChange.TrustedPlayerAdd,
        DatabaseChange.TrustedPlayerRemove,
        DatabaseChange.WarpEntityCreateOrUpdate {

    record AuctionHouseItemAdd(AuctionHouseItemEntity auctionHouseItem) implements DatabaseChange {
    }

    record AuctionHouseItemRemove(AuctionHouseItemEntity auctionHouseItem) implements DatabaseChange {
    }

    record BlockedPlayerAdd(IslandEntity island, PlayerEntity trustedPlayer) implements DatabaseChange {
    }

    record BlockedPlayerRemove(String islandOwnerUniqueId, String trustedPlayerId) implements DatabaseChange {
    }

    record GambleRecordAdd(GambleEntity gamble) implements DatabaseChange {
    }

    record IslandRecordUpdate(IslandEntity islandEntity) implements DatabaseChange {
    }

    record TransactionAdd(TransactionEntity transaction) implements DatabaseChange {
    }

    record TrustedPlayerAdd(IslandEntity island, PlayerEntity trustedPlayer) implements DatabaseChange {
    }

    record TrustedPlayerRemove(String islandOwnerUniqueId, String trustedPlayerId) implements DatabaseChange {
    }

    record PlayerCreateOrUpdate(PlayerEntity player) implements DatabaseChange {
    }

    record WarpEntityCreateOrUpdate(WarpEntity warpEntity) implements DatabaseChange {
    }
}
