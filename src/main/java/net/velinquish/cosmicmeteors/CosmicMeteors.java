package net.velinquish.cosmicmeteors;

import lombok.Getter;
import net.velinquish.cosmicmeteors.models.Meteor;
import net.velinquish.cosmicmeteors.models.MeteorType;
import net.velinquish.cosmicmeteors.settings.Locations;
import net.velinquish.cosmicmeteors.settings.Settings;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.plugin.SimplePlugin;

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
            pair.getValue().remove();
            Entity e = Bukkit.getEntity(pair.getKey());
            if (e != null)
                e.remove();
        }
    }

    @Override
    public boolean areToolsEnabled() {
        return false;
    }

    public void spawnMeteor(String meteorType, Location spawn, Location destination) {
        MeteorType meteorInfo = Settings.MeteorTypes.get(meteorType);
        Material material = meteorInfo.getMaterial().toMaterial();
        Vector3f scale = new Vector3f(meteorInfo.getScaleX(), meteorInfo.getScaleY(), meteorInfo.getScaleZ());
        World world = Objects.requireNonNull(spawn.getWorld());
        Vector displacement = destination.toVector().subtract(spawn.toVector());
        float numTicks = (float) (displacement.length() / meteorInfo.getSpeed());

        spawn.setDirection(new Vector(0, 0, 1));
        ItemDisplay display = world.spawn(spawn, ItemDisplay.class, (ent) -> {
            ent.setItemStack(new ItemStack(material));
            ent.setInvulnerable(true);
            ent.setCustomName("meteor");
            ent.setViewRange(5f); // Ensure it doesn't fall out of view range and disappear
            float startRotation = (float) Math.toRadians(meteorInfo.getStartRotation());
            ent.setTransformation(new Transformation(new Vector3f(),
                    new AxisAngle4f(startRotation, 1, 0, 0), scale,
                    new AxisAngle4f(startRotation, 0, 0, 1)));
            ent.setInterpolationDuration((int) numTicks);
        });
        Meteor met = new Meteor(meteorInfo, display);
        float deltaRadians = (float) Math.toRadians(meteorInfo.getStartRotation() + meteorInfo.getRotationChange());
        Transformation transformation = new Transformation(displacement.toVector3f(),
                new AxisAngle4f(deltaRadians, 1, 0, 1), scale,
                new AxisAngle4f());
        Bukkit.getScheduler().runTaskLater(this, () -> {
            display.setInterpolationDelay(0);
            display.setTransformation(transformation);
        }, 5); // Starts a short delay after creation to avoid occasional instant teleporting

        Interaction interaction = world.spawn(spawn, Interaction.class, (ent) -> {
            ent.setInteractionHeight(scale.get(1));
            ent.setInteractionWidth(Math.max(scale.get(0), scale.get(2)));
        });
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!meteors.containsKey(interaction.getUniqueId()))
                return;
            landMeteor(met, interaction);
        }, (long) numTicks + 5);

        long spawnTime = world.getGameTime() + 5;
        Vector3fc spawnVector = spawn.toVector().toVector3f();
        Vector3fc destinationVector = destination.toVector().toVector3f();

        met.setPhysicsTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            float alpha = Math.min((float) (world.getGameTime() - spawnTime) / numTicks, 1f);
            Vector3f position = spawnVector.lerp(destinationVector, alpha, new Vector3f())
                    .sub(new Vector3f(0, scale.y / 2f, 0)); // Offset by half the height
            interaction.teleport(new Location(world, position.x, position.y, position.z));
            if (alpha >= 1f)
                met.cancelPhysicsTask();
        }, 5, 1));

        meteors.put(interaction.getUniqueId(), met);

        String spawnAnnouncement = meteorInfo.getSpawnAnnouncement();
        if (spawnAnnouncement != null && !spawnAnnouncement.equalsIgnoreCase("none")) {
            Replacer.replaceArray(spawnAnnouncement, "prefix", Settings.PLUGIN_PREFIX, "location",
                    Common.shortLocation(spawn), "destination", Common.shortLocation(destination));
            Common.broadcast(spawnAnnouncement);
        }

        SerializedMap particles = meteorInfo.getParticles();
        if (!particles.getBoolean("Enabled", true))
            return;
        List<SerializedMap> effects = particles.getMapList("Effects");
        met.setParticleTask(Bukkit.getScheduler().runTaskTimer(this, () -> {
            float alpha = Math.min((float) (world.getGameTime() - spawnTime) / numTicks, 1f);
            Vector3f position = spawnVector.lerp(destinationVector, alpha, new Vector3f());
            for (SerializedMap effect : effects) {
                List<Double> spread = effect.getList("Spread", Double.class);
                world.spawnParticle(
                        effect.get("Type", Particle.class),
                        position.x, position.y, position.z,
                        effect.getInteger("Amount", 1),
                        spread.get(0), spread.get(1), spread.get(2),
                        effect.getDouble("Speed", 0.0),
                        null,  // Data
                        effect.getBoolean("Force_Visibility", false)
                );
            }
        }, particles.getInteger("Delay", 0), particles.getInteger("Interval", 5)));
    }

    public void spawnMeteor(Location spawn, Location destination) {
        spawnMeteor("default", spawn, destination);
    }

    public void spawnMeteor(String meteorType, String spawnLocationGroup, String destinationLocationGroup) {
        MeteorType meteorInfo = Settings.MeteorTypes.get(meteorType);
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

    private void landMeteor(Meteor met, Entity interaction) {
        Location loc = interaction.getLocation();
        Objects.requireNonNull(loc.getWorld()).createExplosion(loc, met.getType().getExplosionPower(),
                false, false, interaction);

        Integer despawnTicks = met.getType().getDespawnTicks();
        if (despawnTicks != null) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!meteors.containsKey(interaction.getUniqueId()))
                    return;
                meteors.remove(interaction.getUniqueId()).remove();
                interaction.remove();
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
            met.remove();
            event.getRightClicked().remove();
            meteors.remove(event.getRightClicked().getUniqueId());
            rewardItems(event.getPlayer(), met.getType());
        }
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
