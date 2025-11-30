package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "auction_house")
@NoArgsConstructor
@Getter
@Setter
public class AuctionHouse {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, canBeNull = false)
    private Balance user;

    @DatabaseField(columnName = "owner_name", canBeNull = false)
    private String ownerName;

    @DatabaseField(canBeNull = false)
    private double price;

    @DatabaseField(canBeNull = false)
    private String item;

    @DatabaseField(columnName = "date_added", canBeNull = false)
    private long dateAdded = System.currentTimeMillis();
}