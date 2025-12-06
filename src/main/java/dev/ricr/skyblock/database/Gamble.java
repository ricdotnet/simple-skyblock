package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "gambles")
@NoArgsConstructor
@Getter
@Setter
public class Gamble {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, canBeNull = false)
    private User user;

    @DatabaseField(canBeNull = false)
    private double amount;

    @DatabaseField(canBeNull = false)
    private String type; // Won or Lost
}