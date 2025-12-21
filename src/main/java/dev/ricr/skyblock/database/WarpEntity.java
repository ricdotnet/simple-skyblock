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
    @DatabaseField(id = true, columnName = "warp_name")
    private String warpName;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private PlayerEntity player;

    @DatabaseField(columnName = "is_server")
    private boolean isServer = false;

    @DatabaseField(canBeNull = false)
    private String location;
}
