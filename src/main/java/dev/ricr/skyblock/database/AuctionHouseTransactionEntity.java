package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "auction_house_transaction")
@NoArgsConstructor
@Getter
@Setter
public class AuctionHouseTransactionEntity {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, canBeNull = false)
    private PlayerEntity player;

    @DatabaseField(foreign = true, canBeNull = false)
    private PlayerEntity seller;

    @DatabaseField(canBeNull = false)
    private double price;

    @DatabaseField(canBeNull = false)
    private String item;

    @DatabaseField(columnName = "date_added", canBeNull = false)
    private long dateAdded = System.currentTimeMillis();
}