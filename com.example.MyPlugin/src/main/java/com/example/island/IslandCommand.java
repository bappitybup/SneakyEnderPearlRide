package com.example.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Random;

public class IslandCommand implements CommandExecutor {

    private static final int ISLAND_RADIUS = 10;
    private static final int ISLAND_DISTANCE = 40;
    private static final int ISLAND_HEIGHT = 30;
    private Random random;
    private SimplexOctaveGenerator generator;

    public IslandCommand() {
        random = new Random();
        generator = new SimplexOctaveGenerator(new Random(), 8);
        generator.setScale(0.01);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            World world = player.getWorld();
            Location location = player.getLocation();

            // Define the island parameters
            int islandSize = 10; // Adjust island size as needed

            // Generate the island
            generateSkyIslands(world, location);

            player.sendMessage("Floating island generated!");
            return true;
        } else {
            sender.sendMessage("This command can only be executed by a player.");
            return false;
        }
    }

    private void generateSkyIslands(World world, Location centerLocation) {
        int islandCount = 5; // Adjust the number of islands as desired

        for (int i = 0; i < islandCount; i++) {
            int islandX = centerLocation.getBlockX() + random.nextInt(ISLAND_DISTANCE) - ISLAND_DISTANCE / 2;
            int islandZ = centerLocation.getBlockZ() + random.nextInt(ISLAND_DISTANCE) - ISLAND_DISTANCE / 2;
            int islandY = findIslandY(world, islandX, islandZ);

            Location islandCenter = new Location(world, islandX, islandY, islandZ);

            generateIsland(world, islandCenter);
        }
    }

    private int findIslandY(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z) + 1;

        double noise = generator.noise(x, z, 0.5, 0.5);
        int maxHeight = (int) (ISLAND_HEIGHT * noise);

        if (y < maxHeight) {
            y = maxHeight;
        }

        return y;
    }

    private void generateIsland(World world, Location center) {
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int x = -ISLAND_RADIUS; x <= ISLAND_RADIUS; x++) {
            for (int z = -ISLAND_RADIUS; z <= ISLAND_RADIUS; z++) {
                for (int y = -ISLAND_RADIUS; y <= ISLAND_RADIUS; y++) {
                    double distance = Math.sqrt(x * x + z * z + y * y);
                    if (distance <= ISLAND_RADIUS) {
                        Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                        block.setType(Material.GRASS_BLOCK);
                    }
                }
            }
        }
    }


}