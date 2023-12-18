package net.velinquish.cosmicmeteors.models;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompMaterial;

@Getter
public class MeteorType {
    private final CompMaterial material;

    private final String defaultSpawns;

    private final String defaultDestinations;

    private final @Nullable Double spawnHeight;

    private final @Nullable Integer despawnTicks;

    private final double speed;

    private final float startRotation; // In degrees

    private final float rotationChange; // In degrees

    private final float scaleX;

    private final float scaleY;

    private final float scaleZ;

    private final float explosionPower;

    private final String spawnAnnouncement;

    private final String collectAnnouncement;

    private final String despawnAnnouncement;

    private final String lootTableName;


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
        startRotation = 45;
        rotationChange = 180;
        scaleX = 1f;
        scaleY = 1f;
        scaleZ = 1f;
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
        if (map.containsKey("Spawn_Height") && map.getString("Spawn_Height").equals("none"))
            spawnHeight = null;
        else
            spawnHeight = map.getDouble("Spawn_Height", defaultMeteor.getSpawnHeight());
        if (map.containsKey("Despawn_Ticks") && map.getInteger("Despawn_Ticks") == -1)
            despawnTicks = null;
        else
            despawnTicks = map.getInteger("Despawn_Ticks", defaultMeteor.getDespawnTicks());
        speed = map.getDouble("Speed", defaultMeteor.getSpeed());
        scaleX = map.getFloat("Scale_X", defaultMeteor.getScaleX());
        scaleY = map.getFloat("Scale_Y", defaultMeteor.getScaleY());
        scaleZ = map.getFloat("Scale_Z", defaultMeteor.getScaleZ());
        startRotation = map.getFloat("Rotation_Start", defaultMeteor.getStartRotation());
        rotationChange = map.getFloat("Rotation_Speed", defaultMeteor.getRotationChange());
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
