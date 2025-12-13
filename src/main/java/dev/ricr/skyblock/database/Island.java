package dev.ricr.skyblock.database;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "player_islands")
@NoArgsConstructor
@Getter
@Setter
public class Island {
    @DatabaseField(id = true)
    private String id; // the UUID of the player

    @DatabaseField(foreign = true, canBeNull = false)
    private User user;

    @DatabaseField
    private String islandName;

    @DatabaseField(canBeNull = false)
    private double positionX;

    @DatabaseField(canBeNull = false)
    private double positionZ;

    @ForeignCollectionField
    private ForeignCollection<IslandUserTrustLink> trustedPlayers;

    @DatabaseField(canBeNull = false)
    private boolean isPrivate = false;

    @DatabaseField(canBeNull = false)
    private int islandRadius;

    @DatabaseField(canBeNull = false)
    private boolean hasNether = false;

    @DatabaseField(canBeNull = false)
    private boolean allowNetherTeleport = false;

    @DatabaseField(canBeNull = false)
    private long seed;
}
