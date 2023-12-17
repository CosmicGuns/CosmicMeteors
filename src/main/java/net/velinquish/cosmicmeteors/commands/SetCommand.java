package net.velinquish.cosmicmeteors.commands;

import net.velinquish.cosmicmeteors.CosmicMeteors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleSubCommand;
import org.mineacademy.fo.settings.Lang;

import java.util.List;

public class SetCommand extends SimpleSubCommand {

    private final String CONSOLE_USAGE = "<spawn/destination> <x> <y> <z> <world>";

    private CosmicMeteors plugin = CosmicMeteors.getInstance();

    public SetCommand() {
        super("set");
        setDescription("Sets a meteor spawn origin/destination");
        setUsage("<location type> [<x> <y> <z> [world]]");
        setPermission("meteor.set");
        setMinArguments(1);
    }

    @Override
    protected void onCommand() {
        Location loc = parseLocation(1);
        plugin.getLocations().addLocation(args[0], loc);
        tellSuccess(Lang.of("Commands.Set.Success", args[0], Common.shortLocation(loc)));
    }

    @Override
    protected List<String> tabComplete() {
        switch (args.length) {
            case 1:
                return completeLastWord(plugin.getLocations().getLocationGroupNames());
            case 5:
                return completeLastWordWorldNames();
        }
        return NO_COMPLETE;
    }

    private Location parseLocation(int index) {
        if (args.length <= index)
            if (isPlayer())
                return ((Player) sender).getLocation();
            else
                returnTell(CONSOLE_USAGE);
        double x = 0, y = 0, z = 0;
        try {
            x = Double.parseDouble(args[index]);
            y = Double.parseDouble(args[index + 1]);
            z = Double.parseDouble(args[index + 2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            returnTell(getUsage());
        }
        if (args.length >= index + 4) {
            if (Bukkit.getWorld(args[index + 3]) == null)
                returnTell(Lang.of("Commands.Invalid_World").replace("{world}", args[index + 3])
                        .replace("{available}", Common.join(Bukkit.getWorlds(), ", ")));
            return new Location(Bukkit.getWorld(args[index + 3]), x, y, z);
        } else if (isPlayer())
            return new Location(((Player) sender).getWorld(), x, y, z);
        else
            returnTell(CONSOLE_USAGE);
        return null;
    }
}
