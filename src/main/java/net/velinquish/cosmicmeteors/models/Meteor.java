package net.velinquish.cosmicmeteors.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public class Meteor {
    @Getter
    private final MeteorType type;
    @Setter
    private BukkitTask physicsTask;
    @Setter
    private BukkitTask particleTask;
    @Getter
    private final Entity displayEntity;

    public Meteor(MeteorType type, Entity displayEntity) {
        this.type = type;
        this.displayEntity = displayEntity;
    }

    public void cancelPhysicsTask() {
        if (physicsTask != null)
            physicsTask.cancel();
    }

    public void remove() {
        cancelPhysicsTask();
        if (particleTask != null)
            particleTask.cancel();
        if (displayEntity != null)
            displayEntity.remove();
    }
}
