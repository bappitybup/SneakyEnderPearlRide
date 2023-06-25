package net.bappity;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadConfigCommand implements CommandExecutor {
    private final MyPlugin _plugin;

    public ReloadConfigCommand(MyPlugin plugin) {
        _plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the player has permission to reload the config
            if (player.hasPermission("myplugin.reloadsneaky")) {
                reloadAndNotify(player);
            } else {
                player.sendMessage("§cYou do not have permission to use this command.");
            }
        } else {
            // If the command sender is not a player (e.g., from the console)
            reloadAndNotify(null);
        }
        return true;
    }

    private void reloadAndNotify(Player player) {
        _plugin.reloadConfig();
        _plugin.reloadEnderPearlRide();
        _plugin.reloadAllEventClasses(_plugin.getConfig());
        String reloadMessage = "§aConfiguration reloaded successfully!";

        if (player != null) {
            player.sendMessage(reloadMessage);
        } else {
            _plugin.getLogger().info(reloadMessage);
        }
    }
}