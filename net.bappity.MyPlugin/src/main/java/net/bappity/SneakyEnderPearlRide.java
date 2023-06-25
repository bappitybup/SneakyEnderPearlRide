package net.bappity;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class SneakyEnderPearlRide extends JavaPlugin {

    private EnderPearlRide _enderPearlRideClass;

    @Override
    public void onEnable() {
        // Save the default config.yml if it doesn't exist in the plugin's data folder
        this.saveDefaultConfig();

        // Load the merged config (user's config with missing values filled from default
        // config)
        FileConfiguration config = this.getConfig();

        _enderPearlRideClass = new EnderPearlRide(this, config);

        getCommand("sneaky").setExecutor(new SneakyCommand(this));
        getCommand("sneaky").setTabCompleter(new MyTabCompleter());

        // Register the event listener in the plugin manager
        getServer().getPluginManager().registerEvents(_enderPearlRideClass, this);

        // This method is called when the plugin is enabled
        getLogger().info("SneakyEnderPearlRide is enabled!");
    }

    @Override
    public void onDisable() {
        // This method is called when the plugin is disabled
        getLogger().info("SneakyEnderPearlRide is disabled!");
    }

    public void reloadAllEventClasses(FileConfiguration config) {
        reloadEnderPearlRide();
    }

    public void reloadEnderPearlRide() {
        if (_enderPearlRideClass != null) {
            _enderPearlRideClass.reload();
        }
    }

    public EnderPearlRide getEnderPearlRideClass() {
        return _enderPearlRideClass;
    }
}