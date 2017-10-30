package com.raffler.app.models;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by Ghost on 10/27/2017.
 */

public class RealmContactMigration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        // DynamicRealm exposes an editable schema
        RealmSchema schema = realm.getSchema();

        // Migrate to version 1: Add a new class.
        // Example:
        // public Person extends RealmObject {
        //     private String name;
        //     private int age;
        //     // getters and setters left out for brevity
        // }
        if (oldVersion == 0) {
            schema.get("RealmContact")
                    .addPrimaryKey("idx");
//                    .addField("idx", String.class, FieldAttribute.PRIMARY_KEY);
            oldVersion++;
        }

        // Migrate to version 2: Add a primary key + object references
        // Example:
        // public Person extends RealmObject {
        //     private String name;
        //     @PrimaryKey
        //     private int age;
        //     private Dog favoriteDog;
        //     private RealmList<Dog> dogs;
        //     // getters and setters left out for brevity
        // }
        if (oldVersion == 1) {
            /*schema.get("RealmContact")
                    .addField("idx", String.class, FieldAttribute.PRIMARY_KEY)
                    .addField("name", String.class)
                    .addField("phone", String.class);
            oldVersion++;*/
        }
    }
}
