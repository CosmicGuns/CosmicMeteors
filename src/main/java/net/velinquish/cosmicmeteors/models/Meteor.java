package net.velinquish.cosmicmeteors.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;

public class Meteor {
    @Getter
    private MeteorType type;
    @Setter
    private BukkitTask physicsTask;
    @Setter
    private BukkitTask particleTask;

    public Meteor(MeteorType type) {
        this.type = type;
    }

    public void cancelPhysicsTask() {
        if (physicsTask != null)
            physicsTask.cancel();
    }

    public void cancel() {
        cancelPhysicsTask();
        if (particleTask != null)
            particleTask.cancel();
    }
}
