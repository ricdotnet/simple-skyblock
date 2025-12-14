package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "players")
@NoArgsConstructor
@Getter
@Setter
public class PlayerEntity {
    @DatabaseField(id = true)
    private String playerId;

    @DatabaseField(unique = true)
    private String username;

    @DatabaseField(defaultValue = "0.0")
    private double balance;

    // We should keep track of this here for keeping how big a player's island was in case they decide to delete it and recreate
    @DatabaseField
    private int expansionSize = 0;
}