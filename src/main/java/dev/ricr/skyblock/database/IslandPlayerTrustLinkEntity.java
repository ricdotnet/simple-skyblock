package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "island_player_trust_link")
@NoArgsConstructor
@Getter
@Setter
public class IslandPlayerTrustLinkEntity {
    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private IslandEntity island;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private PlayerEntity player;
}
