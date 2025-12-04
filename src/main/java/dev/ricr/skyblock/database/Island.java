package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    @DatabaseField
    private Set<UUID> trustedPlayers = new HashSet<>();

    @DatabaseField(canBeNull = false)
    private boolean isPrivate = false;

    @DatabaseField(canBeNull = false)
    private boolean isBorderVisible = false;
}
