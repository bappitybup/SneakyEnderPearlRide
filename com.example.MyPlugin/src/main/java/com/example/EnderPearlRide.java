package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.bukkit.event.block.Action;

/**
 * The EnderPearlRide class is a Bukkit plugin event listener that allows players to ride ender
 * pearls while leashing nearby entities to their vehicle. When the pearl lands or the player
 * toggles sneak, the leashed entities will be detached and leashed again to the player.
 */
public class EnderPearlRide implements Listener {

    // Map to store leashed entities for each player UUID.
    private HashMap<UUID, ArrayList<Entity>> _leashedEntitiesMap;
    // Map to store the dismount state for each player UUID.
    private HashMap<UUID, Boolean> _playerDismountStateMap;
    // Flag to track whether leashed entities are being attached.
    private boolean isAttachingLeashedEntities;
    // Atomic reference to hold the BukkitRunnable task for playing the sound while riding the pearl.
    private final AtomicReference<BukkitRunnable> soundLoopTask = new AtomicReference<>();
    
    // Referencing the main plugin
    private final MyPlugin _plugin;

    // Referencing the main plugin config.yml
    private final FileConfiguration _config;

    /**
     * EnderPearlRide constructor.
     *
     * @param leashedEntitiesMap A HashMap linking player UUIDs to lists of leashed entities.
     */
    public EnderPearlRide(MyPlugin plugin, FileConfiguration config) {
        _leashedEntitiesMap = new HashMap<>();
        _playerDismountStateMap = new HashMap<>();
        _plugin = plugin;
        _config = config;
    }

    /**
     * Handles the PlayerToggleSneakEvent when players toggle sneaking while riding EnderPearls.
     *
     * @param event The PlayerToggleSneakEvent being handled.
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();

        // Check if the player is riding an EnderPearl
        if (vehicle instanceof EnderPearl) {
            EnderPearl enderPearl = (EnderPearl) vehicle;
            if (event.isSneaking()) {
                // Update the player's dismount state to true if not already set
                Boolean currentPlayerState = _playerDismountStateMap.get(player.getUniqueId());
                if (currentPlayerState == null || !currentPlayerState) {
                    _playerDismountStateMap.put(player.getUniqueId(), true);
                }
            } else {
                // If the player stops sneaking mid-air for the second time, force them to leave the vehicle
                Boolean currentPlayerState = _playerDismountStateMap.get(player.getUniqueId());
                if (currentPlayerState != null && currentPlayerState && !vehicle.isOnGround()) {
                    // Detach leashed entities and re-leash them
                    detachLeashedEntities(player);
                    // Stop the flying sound for the player
                    player.stopSound(Sound.ITEM_ELYTRA_FLYING);

                    // Remove the EnderPearl vehicle and dismount the player
                    enderPearl.remove();
                    player.leaveVehicle();
                }
                // Set the player's dismount state to false
                _playerDismountStateMap.put(player.getUniqueId(), false);
            }
        } else {
            // If the player is not riding an EnderPearl and stops sneaking, set the dismount state to false
            if (!event.isSneaking()) {
                _playerDismountStateMap.put(player.getUniqueId(), false);
            }
        }
    }

/**
 * This function stops the flying sound when the teleport ends, and detaches the leashed entities.
 *
 * @param event The PlayerTeleportEvent being handled.
 */
@EventHandler
public void onPlayerTeleport(PlayerTeleportEvent event) {
    // Check if the teleport cause is due to an Enderpearl
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
        Player player = event.getPlayer();

        // Stop the flying sound for the player
        player.stopSound(Sound.ITEM_ELYTRA_FLYING);

        // Detach leashed entities and re-leash them
        detachLeashedEntities(player);
    }
}

    /**
     * Handles the EntityDismountEvent when players attempt to dismount from their EnderPearl vehicle.
     *
     * @param event The EntityDismountEvent being handled.
     */
    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        // Check if the dismounting entity is a player
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Check if the player is riding an EnderPearl
            if (event.getDismounted() instanceof EnderPearl) {
                // Check if the player is holding the sneak key
                if (player.isSneaking()) {
                    // Cancel the dismount event
                    event.setCancelled(true);
                } else {
                    // Set the player's dismount state to false when the player
                    // is not holding the sneak key and dismounts
                    _playerDismountStateMap.put(player.getUniqueId(), false);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Check if the entity is riding on the player
        if (entity.getVehicle() != null && entity.getVehicle().equals(player)) {
            // Handle the interaction
            boolean shouldCancel = throwableEnderpearlMain(player, player.getInventory().getItemInMainHand());

            if (shouldCancel) {
                // Cancel the event to prevent throwing another ender pearl
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles the PlayerInteractEvent when players launch an EnderPearl while sneaking or riding an EnderPearl.
     *
     * @param event The PlayerInteractEvent being handled.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Get the player
        Player player = event.getPlayer();

        // Check if the player is holding an ender pearl and it's a right-click action
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || 
             event.getAction() == Action.RIGHT_CLICK_BLOCK) && 
             (player.isSneaking() || player.getVehicle() instanceof EnderPearl)) {
                boolean shouldCancel = throwableEnderpearlMain(player, event.getItem());

                if (shouldCancel) {
                    // Cancel the event to prevent throwing another ender pearl
                    event.setCancelled(true);
                }
        }
    }

    private boolean throwableEnderpearlMain(Player player, ItemStack itemGet) {
            // Check if the player is holding an EnderPearl item in hand
            if (itemGet != null && itemGet.getType() == Material.ENDER_PEARL) {

                // Check if the player is already riding an EnderPearl
                if (player.getVehicle() instanceof EnderPearl) {
                    EnderPearl oldPearl = (EnderPearl) player.getVehicle();
                    player.leaveVehicle();
                    oldPearl.remove();
                    // Stop the flying sound for the player
                    player.stopSound(Sound.ITEM_ELYTRA_FLYING);
                }

                // Launch a new EnderPearl projectile
                EnderPearl pearl = player.launchProjectile(EnderPearl.class);
                Vector currentVelocity = pearl.getVelocity();
                Vector doubledVelocity = currentVelocity.multiply(1.2);
                pearl.setVelocity(doubledVelocity);

                // Add the player as a passenger on the EnderPearl
                pearl.addPassenger(player);

                // Attach leashed entities to the player and un-leash them
                attachLeashedEntities(player);

                final int customParticleCount = new ConfigValueGrabber<Integer>(_config).getCustomConfigValue(Integer.class, "particle-trail.enabled", "particle-trail.count", 0);
                final int customOffsetX = new ConfigValueGrabber<Integer>(_config).getCustomConfigValue(Integer.class, "particle-trail.enabled", "particle-trail.offsetX", 0);
                final int customOffsetY = new ConfigValueGrabber<Integer>(_config).getCustomConfigValue(Integer.class, "particle-trail.enabled", "particle-trail.offsetY", 0);
                final int customOffsetZ = new ConfigValueGrabber<Integer>(_config).getCustomConfigValue(Integer.class, "particle-trail.enabled", "particle-trail.offsetZ", 0);

                // Schedule tasks to spawn particle trail and play flying sound while riding EnderPearl
                BukkitRunnable particleTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isInsideVehicle() || !(player.getVehicle() instanceof EnderPearl)) {
                            if (soundLoopTask.get() != null) {
                                soundLoopTask.get().cancel();
                            }
                            cancel();
                        } else {
                            // Spawn particle trail behind EnderPearl
                            Vector offsetVec = pearl.getVelocity().normalize().multiply(-1).multiply(1.0);
                            Location particleLocation = pearl.getLocation().add(offsetVec);
                            if (customParticleCount != 0) {
                                pearl.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLocation, customParticleCount, customOffsetX, customOffsetY, customOffsetZ, 0);
                            }
                        }
                    }
                };

                final double customWindVolume = new ConfigValueGrabber<Double>(_config).getCustomConfigValue(Double.class, "wind-sound.enabled", "wind-sound.volume", 0.0);

                BukkitRunnable playSoundTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!particleTask.isCancelled() && player.isInsideVehicle() && player.getVehicle() instanceof EnderPearl) {
                            if ((float)customWindVolume != 0f) {
                                // Play the flying sound for the player
                                player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, (float)customWindVolume, 1.0f);
                            }
                        } else {
                            this.cancel();
                        }
                    }
                };

                particleTask.runTaskTimer(_plugin, 0L, 1L);
                playSoundTask.runTaskTimer(_plugin, 0L, 200L); // Repeat every 200 ticks (10 seconds)
                soundLoopTask.set(playSoundTask);

                // Remove one ender pearl from the player's inventory if not in creative mode
                if (player.getGameMode() != GameMode.CREATIVE) {
                    ItemStack item = itemGet;
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().removeItem(item);
                    }
                }

                return true;
            }

            return false;
    }

    /**
     * Handles the ProjectileHitEvent when an EnderPearl projectile lands while a player is riding it.
     *
     * @param event The ProjectileHitEvent being handled.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Only proceed if the projectile is an EnderPearl
        if (event.getEntity() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getEntity();

            // Only proceed if the pearl has a player passenger
            if (!pearl.getPassengers().isEmpty() && pearl.getPassengers().get(0) instanceof Player) {
                Player player = (Player) pearl.getPassengers().get(0);

                // Stop the flying sound for the player
                player.stopSound(Sound.ITEM_ELYTRA_FLYING);

                // Detach leashed entities and re-leash them
                detachLeashedEntities(player);
            }
        }
    }

    /**
     * Handles the EntityDamageEvent when a player receives fall damage while riding an EnderPearl.
     *
     * @param event The EntityDamageEvent being handled.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Check if the entity is a player and the cause is fall damage
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Get the player and check if they have a vehicle
            Player player = (Player) event.getEntity();
            if (player.getVehicle() != null && player.getVehicle() instanceof EnderPearl) {
                // Remove the vehicle, dismount the player, and cancel the damage
                player.leaveVehicle();
                event.setCancelled(true);

                // Despawn the pearl
                Entity pearl = player.getVehicle();
                pearl.remove();
            }
        }
    }

    /**
     * Handles the PlayerUnleashEntityEvent when a player unleashes an entity while attaching leashed entities.
     *
     * @param event The PlayerUnleashEntityEvent being handled.
     */
    @EventHandler
    public void onPlayerUnleashEntity(PlayerUnleashEntityEvent event) {
        if (isAttachingLeashedEntities) {
            // Change the leash holder to null to prevent leash from reattaching
            event.setCancelled(true);
            LivingEntity entity = (LivingEntity) event.getEntity(); // Cast the entity to LivingEntity
            entity.setLeashHolder(null);
        }
    }

    /**
     * Attaches nearby leashed entities to the player and un-leashes them.
     *
     * @param player The player to attach the nearby leashed entities.
     */
    private void attachLeashedEntities(Player player) {
        if (_leashedEntitiesMap != null) {
            if (_leashedEntitiesMap.get(player.getUniqueId()) == null || _leashedEntitiesMap.get(player.getUniqueId()).size() == 0) {
                isAttachingLeashedEntities = true;
                // Store the leashed entities temporarily
                ArrayList<Entity> leashedEntities = new ArrayList<>();
                Entity lastLeashedEntity = player; // Modify this line to initialize the first entity in the stack
                for (Entity nearbyEntity : player.getNearbyEntities(10, 10, 10)) {
                    // Exclude instances of Player
                    if (nearbyEntity instanceof Player) {
                        continue;
                    }

                    if (nearbyEntity instanceof LivingEntity && ((LivingEntity) nearbyEntity).isLeashed() && ((LivingEntity) nearbyEntity).getLeashHolder().equals(player)) {
                        leashedEntities.add(nearbyEntity);
                        ((LivingEntity) nearbyEntity).setLeashHolder(null); // Remove the leash holder
                        lastLeashedEntity.addPassenger(nearbyEntity); // Entities ride on the previous entity
                        lastLeashedEntity = nearbyEntity; // Modify this line to update the last entity in the stack
                    }
                }
                _leashedEntitiesMap.put(player.getUniqueId(), leashedEntities);
                isAttachingLeashedEntities = false;
            }
        }
    }

    /**
     * Detaches leashed entities from the player and re-leashes them.
     *
     * @param player The player to detach and re-leash the entities.
     */
    private void detachLeashedEntities(Player player) {
        // Reattach the leashed entities after the ender pearl lands or the player toggles sneak
        ArrayList<Entity> leashedEntities = _leashedEntitiesMap.remove(player.getUniqueId());
        if (leashedEntities != null) {
            for (Entity entity : leashedEntities) {
                entity.leaveVehicle(); // Entities stop riding the player

                // Apply invulnerability to the entities until they hit the ground
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setInvulnerable(true);
                    giveFallProtection((LivingEntity) entity);
                }

                // Re-leash the entities
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setLeashHolder(player);
                }
            }
        }
    }

    /**
     * Gives fall protection to an entity for a certain duration (until it hits the ground).
     *
     * @param entity The entity to give fall protection to.
     */
    private void giveFallProtection(LivingEntity entity) {
        
        // Create a new BukkitRunnable task that will repeatedly check if the entity is on the ground or invulnerable
        BukkitRunnable task = new BukkitRunnable() {

            // Define the run method, which will be executed on every tick
            @Override
            public void run() {

                // If the entity is not invulnerable or is already on the ground, proceed
                if (!entity.isInvulnerable() || entity.isOnGround()) {
                    
                    // Set the entity to be no longer invulnerable
                    entity.setInvulnerable(false);

                    // Cancel the task as it is no longer necessary
                    this.cancel();
                }
            }
        }; 

        // Start the task immediately (0L delay) and run it on every tick (1L repeat interval)
        task.runTaskTimer(_plugin, 0L, 1L);
    }
}