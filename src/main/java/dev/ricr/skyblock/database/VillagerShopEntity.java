package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "villager_shops")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VillagerShopEntity {
    @DatabaseField(id = true)
    private String name;

    @DatabaseField(columnName = "villager_shop_id", unique = true)
    private String villagerShopId;
}
