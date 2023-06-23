package com.example;

import com.example.island.IslandCommand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save the default config.yml if it doesn't exist in the plugin's data folder
        this.saveDefaultConfig();

        // Load the merged config (user's config with missing values filled from default config)
        FileConfiguration config = this.getConfig();

        getCommand("island").setExecutor(new IslandCommand());

        // Register the event listener in the plugin manager
        getServer().getPluginManager().registerEvents(new EnderPearlRide(this, config), this);

        // This method is called when the plugin is enabled
        getLogger().info("MyPlugin is enabled!");
    }

    @Override
    public void onDisable() {
        // This method is called when the plugin is disabled
        getLogger().info("MyPlugin is disabled!");
    }
}