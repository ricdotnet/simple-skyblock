package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "sales")
@NoArgsConstructor
@Getter
@Setter
public class Sale {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, canBeNull = false)
    private Balance user;

    @DatabaseField(canBeNull = false)
    private double value;

    @DatabaseField(canBeNull = false)
    private String item;

    @DatabaseField(canBeNull = false)
    private int quantity;

    @DatabaseField(canBeNull = false)
    private String type; // BOUGHT or SOLD
}