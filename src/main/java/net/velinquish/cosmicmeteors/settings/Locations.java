package net.velinquish.cosmicmeteors.settings;

import org.bukkit.Location;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.*;

public class Locations extends YamlConfig {

    private static final Map<String, List<Location>> locationGroups = new HashMap<>();

    @Override
    protected boolean saveComments() {
        return false;
    }

    @Override
    protected void onLoad() {
        Set<String> locationGroupNames = getKeys(false);
        for (String locationGroupName : locationGroupNames) {
            locationGroups.put(locationGroupName, getList(locationGroupName, Location.class));
        }
    }

    public Location get(String group) {
        List<Location> locations = locationGroups.get(group);
        if (locations == null)
            return null;
        return locations.get((int) (Math.random() * locations.size()));
    }

    public void addLocation(String group, Location location) {
        List<Location> locations = locationGroups.get(group);
        if (locations == null)
            locations = new ArrayList<>();
        locations.add(location);
        save(group, locations);
    }

    public List<String> getLocationGroupNames() {
        return new ArrayList<>(locationGroups.keySet());
    }
}
