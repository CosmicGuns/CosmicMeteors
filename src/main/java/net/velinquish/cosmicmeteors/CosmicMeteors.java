package net.velinquish.cosmicmeteors;

import lombok.Getter;
import net.velinquish.cosmicmeteors.models.Meteor;
import net.velinquish.cosmicmeteors.models.MeteorType;
import net.velinquish.cosmicmeteors.settings.Locations;
import net.velinquish.cosmicmeteors.settings.Settings;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Vector;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.nbt.NBT;

import java.util.*;


public final class CosmicMeteors extends SimplePlugin {

    @Getter
    private Locations locations;
    private final Map<UUID, Meteor> meteors = new HashMap<>();

    /**
     * Automatically perform login ONCE when the plugin starts.
     */
    @Override
    protected void onPluginStart() {
    }

    @Override
    protected void onReloadablesStart() {
        locations = new Locations();
        locations.loadConfiguration("locations.yml");
    }

    @Override
    protected void onPluginStop() {
        for (Map.Entry<UUID, Meteor> pair : meteors.entrySet()) {
            pair.getValue().cancel();
            Entity e = Bukkit.getEntity(pair.getKey());
            if (e != null)
                e.remove();
        }
    }

    @Override
    public boolean areToolsEnabled() {
        return false;
    }

    public FallingBlock spawnMeteor(Meteor met, Location spawn, Location destination) {
        MeteorType meteorInfo = met.getType();
        Material material = meteorInfo.getMaterial().toMaterial();

        FallingBlock block = Objects.requireNonNull(spawn.getWorld())
                .spawnFallingBlock(spawn, material.createBlockData());
        NBT.modify(block, (nbtCompound) -> {
            nbtCompound.setInteger("Time", -2147483648);
        });
        block.setDropItem(false);
        block.setCustomName("meteor");
        block.setGravity(false);
        Vector displacement = destination.toVector().subtract(spawn.toVector());
        Vector vel = displacement.lengthSquared() == 0 ? new Vector(0, 0, 0) :
                displacement.normalize().multiply(meteorInfo.getSpeed());
        block.setVelocity(vel);

        meteors.put(block.getUniqueId(), met);

        met.setPhysicsTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!block.isValid()) { // Meteor broke on slabs or something, doesn't trigger on regular despawn
                landMeteor(met, block);
                return;
            }
            block.setVelocity(vel);
        }, 1, 1));

        if (displacement.lengthSquared() == 0) // Stationary landed meteor
            return block;

        String spawnAnnouncement = meteorInfo.getSpawnAnnouncement();
        if (spawnAnnouncement != null && !spawnAnnouncement.equalsIgnoreCase("none")) {
            Replacer.replaceArray(spawnAnnouncement, "prefix", Settings.PLUGIN_PREFIX, "location",
                    Common.shortLocation(spawn), "destination", Common.shortLocation(destination));
            Common.broadcast(spawnAnnouncement);
        }

        SerializedMap particles = meteorInfo.getParticles();
        if (!particles.getBoolean("Enabled", true))
            return block;
        List<SerializedMap> effects = particles.getMapList("Effects");
        met.setParticleTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            Location location = block.getLocation();
            for (SerializedMap effect : effects) {
                List<Double> spread = effect.getList("Spread", Double.class);
                Objects.requireNonNull(location.getWorld()).spawnParticle(
                        effect.get("Type", Particle.class),
                        location.getX(), location.getY(), location.getZ(),
                        effect.getInteger("Amount", 1),
                        spread.get(0), spread.get(1), spread.get(2),
                        effect.getDouble("Speed", 0.0),
                        null,  // Data
                        effect.getBoolean("Force_Visibility", false)
                );
            }
        }, particles.getInteger("Delay", 0), particles.getInteger("Interval", 5)));
        return block;
    }

    public void spawnMeteor(String meteorType, Location spawn, Location destination) {
        MeteorType meteorInfo = Settings.MeteorTypes.get(meteorType);
        spawnMeteor(new Meteor(meteorInfo), spawn, destination);
    }

    public void spawnMeteor(Location spawn, Location destination) {
        spawnMeteor("default", spawn, destination);
    }

    public void spawnMeteor(String meteorType, String spawnLocationGroup, String destinationLocationGroup) {
        MeteorType meteorInfo = Settings.MeteorTypes.get(meteorType);
        if (meteorInfo == null)
            throw new IllegalArgumentException("Invalid meteor type: " + meteorType);
        Location spawn = locations.get(spawnLocationGroup);
        Location destination = locations.get(destinationLocationGroup);
        Double height = meteorInfo.getSpawnHeight();
        if (height != null)
            spawn.setY(height);
        spawnMeteor(meteorType, spawn, destination);
    }

    public void spawnMeteor(String meteorType) {
        String spawnLocationGroup = Settings.MeteorTypes.get(meteorType).getDefaultSpawns();
        String destinationLocationGroup = Settings.MeteorTypes.get(meteorType).getDefaultDestinations();
        spawnMeteor(meteorType, spawnLocationGroup, destinationLocationGroup);
    }

    public void spawnMeteor() {
        spawnMeteor("default");
    }

    private void landMeteor(Meteor met, Entity ent) {
        met.cancelPhysicsTask();
        meteors.remove(ent.getUniqueId());
        Location loc = ent.getLocation();
        Objects.requireNonNull(loc.getWorld()).createExplosion(loc, met.getType().getExplosionPower(),
                false, false, ent);
        final Entity newEnt = spawnMeteor(met, loc, loc); // Spawn a stationary replacement meteor

        Integer despawnTicks = met.getType().getDespawnTicks();
        if (despawnTicks != null) {
            NBT.modify(newEnt, (nbtCompound) -> {
                nbtCompound.setInteger("Time", 600 - despawnTicks);
            });
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!meteors.containsKey(newEnt.getUniqueId()))
                    return;
                meteors.remove(newEnt.getUniqueId()).cancel();
                newEnt.remove();
                String despawnAnnouncement = met.getType().getDespawnAnnouncement();
                if (despawnAnnouncement != null && !despawnAnnouncement.equalsIgnoreCase("none")) {
                    Replacer.replaceArray(despawnAnnouncement, "prefix", Settings.PLUGIN_PREFIX, "location",
                            Common.shortLocation(loc));
                    Common.broadcast(despawnAnnouncement);
                }
            }, despawnTicks);
        }
    }

    private void rewardItems(Player player, MeteorType meteorType) {
        String broadcast = meteorType.getCollectAnnouncement();
        if (broadcast == null || broadcast.equalsIgnoreCase("none"))
            return;
        Replacer.replaceArray(broadcast, "player", player.getName(), "prefix", Settings.PLUGIN_PREFIX);
        Common.broadcast(broadcast);

        String lootTableName = meteorType.getLootTableName();
        if (lootTableName == null)
            return;
        LootTable loot = Bukkit.getLootTable(Objects.requireNonNull(NamespacedKey.fromString(lootTableName)));
        if (loot == null)
            throw new IllegalArgumentException("Loot table not found: " + lootTableName);
        LootContext lootContext = new LootContext.Builder(player.getLocation()).killer(player)
                .lootedEntity(player).luck(0).lootingModifier(0).build();
        Collection<ItemStack> items = loot.populateLoot(new Random(), lootContext);
        for (ItemStack item : items)
            for (ItemStack drop : player.getInventory().addItem(item).values())
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
    }


    /* ------------------------------------------------------------------------------- */
    /* Events */
    /* ------------------------------------------------------------------------------- */

    @EventHandler
    public void onRightClickMeteorEntity(PlayerInteractEntityEvent event) {
        Meteor met = meteors.get(event.getRightClicked().getUniqueId());
        if (met != null) {
            met.cancel();
            event.getRightClicked().remove();
            meteors.remove(event.getRightClicked().getUniqueId());
            rewardItems(event.getPlayer(), met.getType());
        }
    }

    @EventHandler
    public void onMeteorLand(EntityChangeBlockEvent event) {
        Meteor met = meteors.get(event.getEntity().getUniqueId());
        if (met == null)
            return;
        event.setCancelled(true);
        landMeteor(met, event.getEntity());
    }

    /* ------------------------------------------------------------------------------- */
    /* Static */
    /* ------------------------------------------------------------------------------- */

    /**
     * Return the instance of this plugin, which simply refers to a static
     * field already created for you in SimplePlugin but casts it to your
     * specific plugin instance for your convenience.
     *
     * @return
     */
    public static CosmicMeteors getInstance() {
        return (CosmicMeteors) SimplePlugin.getInstance();
    }
}
