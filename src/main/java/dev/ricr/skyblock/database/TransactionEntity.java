package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "transactions")
@NoArgsConstructor
@Getter
@Setter
public class TransactionEntity {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, canBeNull = false)
    private PlayerEntity player;

    @DatabaseField(foreign = true)
    private PlayerEntity seller;

    @DatabaseField(canBeNull = false)
    private double price;

    @DatabaseField(canBeNull = false)
    private int quantity;

    @DatabaseField(canBeNull = false)
    private String type; // Buy or Sell

    @DatabaseField(canBeNull = false)
    private String item;

    @DatabaseField(columnName = "date_added", canBeNull = false)
    private long dateAdded = System.currentTimeMillis();
}