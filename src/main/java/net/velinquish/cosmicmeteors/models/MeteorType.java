package net.velinquish.cosmicmeteors.models;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;

public class MeteorType {
    @Getter
    private final CompMaterial material;

    @Getter
    private final String defaultSpawns;

    @Getter
    private final String defaultDestinations;

    @Getter
    private final @Nullable Double spawnHeight;

    @Getter
    private final @Nullable Integer despawnTicks;

    @Getter
    private final double speed;

    @Getter
    private final float explosionPower;

    @Getter
    private final String spawnAnnouncement;

    @Getter
    private final String collectAnnouncement;

    @Getter
    private final String despawnAnnouncement;

    @Getter
    private final String lootTableName;

    @Getter
    private SerializedMap particles;

    @Setter
    private static MeteorType defaultMeteor = new MeteorType();

    private MeteorType() {
        material = CompMaterial.MAGMA_BLOCK;
        defaultSpawns = "spawn";
        defaultDestinations = "destination";
        spawnHeight = null;
        despawnTicks = null;
        speed = 0.05;
        explosionPower = 4F;
        spawnAnnouncement = "none";
        collectAnnouncement = "none";
        despawnAnnouncement = "none";
        lootTableName = null;
        particles = SerializedMap.of("Enabled", false);
    }

    public MeteorType(SerializedMap map) {
        material = map.getMaterial("Material", defaultMeteor.getMaterial());
        defaultSpawns = map.getString("Default_Spawns", defaultMeteor.getDefaultSpawns());
        defaultDestinations = map.getString("Default_Destinations", defaultMeteor.getDefaultDestinations());
        if (map.containsKey("Despawn_Ticks") && map.getString("Spawn_Height").equals("none"))
            spawnHeight = null;
        else
            spawnHeight = map.getDouble("Spawn_Height", defaultMeteor.getSpawnHeight());
        if (map.containsKey("Despawn_Ticks") && map.getInteger("Despawn_Ticks") == -1)
            despawnTicks = null;
        else
            despawnTicks = map.getInteger("Despawn_Ticks", defaultMeteor.getDespawnTicks());
        speed = map.getDouble("Speed", defaultMeteor.getSpeed());
        explosionPower = map.getFloat("Explosion_Power", defaultMeteor.getExplosionPower());
        spawnAnnouncement = map.getString("Spawn_Announcement", defaultMeteor.getSpawnAnnouncement());
        collectAnnouncement = map.getString("Collect_Announcement", defaultMeteor.getCollectAnnouncement());
        despawnAnnouncement = map.getString("Despawn_Announcement", defaultMeteor.getDespawnAnnouncement());
        lootTableName = map.getString("Loot_Table", defaultMeteor.getLootTableName());
        particles = map.getMap("Particles");
        if (particles == null)
            particles = defaultMeteor.getParticles();
    }
}
