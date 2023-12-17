package net.velinquish.cosmicmeteors.commands;

import net.velinquish.cosmicmeteors.CosmicMeteors;
import net.velinquish.cosmicmeteors.models.MeteorType;
import net.velinquish.cosmicmeteors.settings.Settings;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.settings.Lang;

import java.util.List;

public class SpawnCommand extends SimpleSubCommand {
    private CosmicMeteors plugin = CosmicMeteors.getInstance();

    public SpawnCommand() {
        super("spawn");
        setDescription("Spawns a meteor in the sky");
        setUsage("[type] [spawn] [destination]");
        setPermission("meteor.spawn");
        setMinArguments(0);
    }

    @Override
    protected void onCommand() {
        try {
            if (args.length == 0)
                plugin.spawnMeteor();
            else {
                MeteorType meteorInfo = Settings.MeteorTypes.get(args[0]);
                if (meteorInfo == null)
                    tellError(Lang.of("Commands.Spawn.Invalid_Type", args[0], Settings.MeteorTypes.getTypeNames()));
                else if (args.length == 1)
                    plugin.spawnMeteor(args[0]);
                else if (args.length == 2)
                    returnTell(getUsage());
                else
                    plugin.spawnMeteor(args[0], args[1], args[2]);
            }
        } catch (IndexOutOfBoundsException e) { // No locations set
            tellError(Lang.of("Commands.Spawn.Locations_Empty"));
        }
    }

    @Override
    protected List<String> tabComplete() {
        switch (args.length) {
            case 1:
                return completeLastWord(Settings.MeteorTypes.getTypeNames());
            case 2:
            case 3:
                return completeLastWord(plugin.getLocations().getLocationGroupNames());
        }
        return NO_COMPLETE;
    }
}
