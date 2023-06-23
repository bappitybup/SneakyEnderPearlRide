package com.example;

import com.example.island.IslandCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("island").setExecutor(new IslandCommand());

        // Register the event listener in the plugin manager
        getServer().getPluginManager().registerEvents(new EnderPearlRide(this), this);

        // This method is called when the plugin is enabled
        getLogger().info("MyPlugin is enabled!");
    }

    @Override
    public void onDisable() {
        // This method is called when the plugin is disabled
        getLogger().info("MyPlugin is disabled!");
    }
}