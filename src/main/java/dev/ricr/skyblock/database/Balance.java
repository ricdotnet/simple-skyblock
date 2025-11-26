package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "balance")
@NoArgsConstructor
@Getter
@Setter
public class Balance {
    @DatabaseField(id = true)
    private String userId;

    @DatabaseField(defaultValue = "0.0")
    private double value;
}