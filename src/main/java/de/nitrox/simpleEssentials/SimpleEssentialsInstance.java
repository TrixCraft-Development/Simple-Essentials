package de.nitrox.simpleEssentials;

import org.bukkit.entity.Player;

public class SimpleEssentialsInstance {

    private final SimpleEssentials plugin;

    public SimpleEssentialsInstance(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the maximum ID from config
     */
    private int getMaxId() {
        return plugin.getConfig().getInt("enderchest.max_ids", 5);
    }

    public java.util.List<String> getEnderchestMax(Player player) {
        java.util.List<String> availableIds = new java.util.ArrayList<>();
        int maxId = getMaxId();

        for (int i = 1; i <= maxId; i++) {
            String id = String.valueOf(i);
            if (player.hasPermission("simpleessentials.enderchest." + id)) {
                availableIds.add(id);
            }
        }

        return availableIds;
    }
}
