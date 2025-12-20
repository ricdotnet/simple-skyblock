package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "warps")
@NoArgsConstructor
@Getter
@Setter
public class WarpEntity {
    @DatabaseField(id = true)
    private String warpName;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private PlayerEntity owner;

    @DatabaseField
    private boolean isServer = false;
}
