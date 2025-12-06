package dev.ricr.skyblock.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DatabaseTable(tableName = "users")
@NoArgsConstructor
@Getter
@Setter
public class User {
    @DatabaseField(id = true)
    private String userId;

    @DatabaseField(unique = true)
    private String username;

    @DatabaseField(defaultValue = "0.0")
    private double balance;
}