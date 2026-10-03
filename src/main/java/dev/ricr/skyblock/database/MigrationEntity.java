package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "migrations")
@NoArgsConstructor
@Getter
@Setter
public class MigrationEntity {
    @DatabaseField(id = true)
    private String id;

    @DatabaseField
    private String migration; // the filename

    @DatabaseField(columnName = "executed_at")
    private long executedAt = System.currentTimeMillis();
}
