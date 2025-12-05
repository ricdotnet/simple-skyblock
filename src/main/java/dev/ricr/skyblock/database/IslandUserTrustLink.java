package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "island_user_trust_link")
@NoArgsConstructor
@Getter
@Setter
public class IslandUserTrustLink {
    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private Island island;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, canBeNull = false)
    private User user;
}
