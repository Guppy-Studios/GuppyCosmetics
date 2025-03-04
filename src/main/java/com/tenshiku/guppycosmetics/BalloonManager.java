package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Chicken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BalloonManager {
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, ArmorStand> activeBalloons;
    private final Map<UUID, Chicken> leadAnchors;
    private final Map<UUID, Location> lastPlayerLocations;
    private final Map<UUID, Double> idleTime;
    private final Map<UUID, Double> bobPhase;
    private final Map<UUID, Double> swayPhase;

    private static final double BALLOON_HEIGHT = 2.5;
    private static final double LEAD_HEIGHT = 1.8;
    private static final double IDLE_THRESHOLD = 0.1;
    private static final double BOB_SPEED = 2.0;
    private static final double SWAY_SPEED = 1.5;
    private static final double BOB_AMPLITUDE = 0.15;
    private static final double SWAY_AMPLITUDE = 0.1;
    private static final double FOLLOW_DISTANCE = 1.0; // Distance behind player

    public BalloonManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.activeBalloons = new HashMap<>();
        this.leadAnchors = new HashMap<>();
        this.lastPlayerLocations = new HashMap<>();
        this.idleTime = new HashMap<>();
        this.bobPhase = new HashMap<>();
        this.swayPhase = new HashMap<>();

        // Start the update task
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllBalloons, 0L, 1L);

        // Start the validation task
        validateAllBalloons();
    }

    // Run this every few seconds to check if balloons are still valid
    public void validateAllBalloons() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            new HashMap<>(activeBalloons).forEach((uuid, balloon) -> {
                Player player = Bukkit.getPlayer(uuid);

                // If the player is online but the balloon is invalid or missing
                if (player != null && player.isOnline() &&
                        (!balloon.isValid() || balloon.isDead())) {
                    // Remove the invalid balloon
                    removeBalloon(uuid);

                    // Check if they still have the balloon item in their custom inventory
                    ItemStack balloonItem = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBalloon(player);
                    if (balloonItem != null && ItemManager.isBalloon(balloonItem, configManager)) {
                        // Recreate the balloon
                        createBalloon(player, balloonItem);
                    }
                }
            });
        }, 100L, 100L); // Run every 5 seconds (100 ticks)
    }

    private void cleanupOldLeads(World world, Location location) {
        world.getNearbyEntities(location, 10, 10, 10).stream()
                .filter(entity -> {
                    if (entity instanceof org.bukkit.entity.Item) {
                        return ((org.bukkit.entity.Item) entity).getItemStack().getType() == org.bukkit.Material.LEAD;
                    }
                    // Also remove any hanging leads
                    return entity instanceof org.bukkit.entity.LeashHitch;
                })
                .forEach(Entity::remove);
    }

    private void removeBalloonEntity(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            cleanupOldLeads(player.getWorld(), player.getLocation());
        }

        // Remove lead anchor
        Chicken leadAnchor = leadAnchors.remove(uuid);
        if (leadAnchor != null && leadAnchor.isValid()) {
            leadAnchor.setLeashHolder(null);
            leadAnchor.remove();
        }

        // Remove balloon
        ArmorStand balloon = activeBalloons.remove(uuid);
        if (balloon != null && balloon.isValid()) {
            balloon.remove();
        }
    }

    private void cleanupExistingBalloonEntities(Player player) {
        player.getWorld().getNearbyEntities(player.getLocation(), 10, 10, 10).forEach(entity -> {
            if (entity instanceof ArmorStand) {
                String customName = entity.getCustomName();
                if (customName != null && customName.startsWith("Balloon:" + player.getUniqueId())) {
                    entity.remove();
                }
            }
        });
    }

    private void createLeadAnchor(Player player, ArmorStand balloon) {
        if (!balloon.isValid()) return;

        cleanupOldLeads(player.getWorld(), player.getLocation());

        // First, remove any existing lead anchor
        Chicken existingAnchor = leadAnchors.get(player.getUniqueId());
        if (existingAnchor != null && existingAnchor.isValid()) {
            existingAnchor.setLeashHolder(null);
            existingAnchor.remove();
        }

        // Calculate offset position for lead anchor starting at player location
        Location anchorLoc = balloon.getLocation().clone().add(0, 0.5, 0);

        // Create new lead anchor
        Chicken leadAnchor = player.getWorld().spawn(anchorLoc, Chicken.class, chicken -> {
            chicken.setInvulnerable(true);
            chicken.setInvisible(true);
            chicken.setSilent(true);
            chicken.setBaby();
            chicken.setAgeLock(true);
            chicken.setAware(false);
            chicken.setCollidable(false);
            chicken.setCustomName("BalloonAnchor:" + player.getUniqueId());
            chicken.setCustomNameVisible(false);
            chicken.setLeashHolder(player);
        });

        if (!leadAnchor.isValid()) {
            balloon.remove();
            return;
        }

        // Store reference
        leadAnchors.put(player.getUniqueId(), leadAnchor);
    }

    public void createBalloon(Player player, ItemStack balloonItem) {
        String itemId = ItemManager.getItemId(balloonItem);
        if (itemId == null) return;

        // Remove existing balloon
        removeBalloon(player.getUniqueId());

        // Calculate spawn location with offset above the player
        Location spawnLoc = player.getLocation().add(0, BALLOON_HEIGHT, 0);

        // Create balloon armorstand
        ArmorStand balloon = player.getWorld().spawn(spawnLoc, ArmorStand.class, stand -> {
            stand.setBasePlate(false);
            stand.setVisible(false);
            stand.setInvulnerable(true);
            stand.setCanPickupItems(false);
            stand.setGravity(false);
            stand.setSmall(false);
            stand.setMarker(true);
            stand.setCollidable(false);
            stand.setCustomName("Balloon:" + player.getUniqueId());
            stand.setCustomNameVisible(false);
            stand.getEquipment().setHelmet(balloonItem);
            stand.setMetadata("itemId", new FixedMetadataValue(plugin, itemId));
        });

        // Create lead anchor with delay to ensure proper sequencing
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            createLeadAnchor(player, balloon);

            activeBalloons.put(player.getUniqueId(), balloon);
            lastPlayerLocations.put(player.getUniqueId(), player.getLocation());
            idleTime.put(player.getUniqueId(), 0.0);
            bobPhase.put(player.getUniqueId(), 0.0);
            swayPhase.put(player.getUniqueId(), 0.0);
        }, 2L);
    }

    public void removeBalloon(UUID uuid) {
        // Clean up tracking data
        lastPlayerLocations.remove(uuid);
        idleTime.remove(uuid);
        bobPhase.remove(uuid);
        swayPhase.remove(uuid);

        // Only remove balloon-related entities
        removeBalloonEntity(uuid);
    }

    private void updateAllBalloons() {
        new HashMap<>(activeBalloons).forEach((uuid, balloon) -> {
            Player player = Bukkit.getPlayer(uuid);
            Chicken leadAnchor = leadAnchors.get(uuid);
            Location lastLoc = lastPlayerLocations.get(uuid);

            // Validate entities
            if (!balloon.isValid() || player == null || !player.isOnline() ||
                    leadAnchor == null || !leadAnchor.isValid()) {
                removeBalloon(uuid);
                return;
            }

            // Validate item in cosmetic inventory instead of leggings slot
            ItemStack cosmeticBalloon = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBalloon(player);
            if (cosmeticBalloon == null || !ItemManager.isBalloon(cosmeticBalloon, configManager)) {
                removeBalloon(uuid);
                return;
            }

            Location currentLoc = player.getLocation();

            // Handle teleports or large movements
            if (lastLoc != null) {
                // Check if player has teleported or moved significantly
                boolean hasTeleported = currentLoc.getWorld() != lastLoc.getWorld();
                boolean hasMovedFar = currentLoc.distanceSquared(lastLoc) > 100; // > 10 blocks

                if (hasTeleported || hasMovedFar) {
                    // Always recreate the balloon on teleports or significant movements
                    removeBalloon(uuid);

                    // Use a slightly longer delay for stability
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            ItemStack newBalloon = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBalloon(player);
                            if (newBalloon != null && ItemManager.isBalloon(newBalloon, configManager)) {
                                createBalloon(player, newBalloon);
                            }
                        }
                    }, 3L);
                    return;
                }
            }

            // Check for invalid leadholder relationship
            if (leadAnchor.getLeashHolder() == null || !leadAnchor.getLeashHolder().equals(player)) {
                // Fix the leash holder if possible
                try {
                    leadAnchor.setLeashHolder(player);
                } catch (Exception e) {
                    // If we can't fix it, recreate the balloon completely
                    removeBalloon(uuid);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            ItemStack newBalloon = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBalloon(player);
                            if (newBalloon != null && ItemManager.isBalloon(newBalloon, configManager)) {
                                createBalloon(player, newBalloon);
                            }
                        }
                    }, 2L);
                    return;
                }
            }

            // Calculate player movement
            double movement = lastLoc != null ? currentLoc.distance(lastLoc) : 0;
            double currentIdleTime = idleTime.getOrDefault(uuid, 0.0);

            // Update idle time based on movement
            if (movement < IDLE_THRESHOLD) {
                currentIdleTime += 0.05; // Increment idle time (50ms for 20 TPS)
            } else {
                currentIdleTime = 0;
            }
            idleTime.put(uuid, currentIdleTime);

            // Update bobbing and swaying phases
            double currentBobPhase = bobPhase.getOrDefault(uuid, 0.0);
            double currentSwayPhase = swayPhase.getOrDefault(uuid, 0.0);
            currentBobPhase = (currentBobPhase + BOB_SPEED * 0.05) % (2 * Math.PI);
            currentSwayPhase = (currentSwayPhase + SWAY_SPEED * 0.05) % (2 * Math.PI);
            bobPhase.put(uuid, currentBobPhase);
            swayPhase.put(uuid, currentSwayPhase);

            // Update balloon physics with improved movement and idle animations
            try {
                // Calculate idle animations
                double idleFactor = Math.min(currentIdleTime, 2.0) / 2.0; // Smooth transition into idle animations
                double bobOffset = idleFactor * BOB_AMPLITUDE * Math.sin(currentBobPhase);
                double swayOffset = idleFactor * SWAY_AMPLITUDE * Math.sin(currentSwayPhase);

                // Calculate base position behind player based on their yaw
                double angle = Math.toRadians(currentLoc.getYaw());
                Location targetLoc = currentLoc.clone();

                // Position balloon behind player using -sin(yaw) for X and -cos(yaw) for Z
                targetLoc.add(
                        -Math.sin(angle) * FOLLOW_DISTANCE + (swayOffset * Math.cos(angle)),
                        bobOffset,
                        -Math.cos(angle) * FOLLOW_DISTANCE + (swayOffset * Math.sin(angle))
                );

                // Apply base height and positioning
                targetLoc.add(0, BALLOON_HEIGHT, 0);

                // Calculate balloon physics
                Vector toPlayer = currentLoc.toVector().subtract(balloon.getLocation().toVector());
                double distance = toPlayer.length();

                if (distance > 0.1) {
                    toPlayer.normalize().multiply(Math.min(distance * 0.3, 0.5));
                    targetLoc.add(toPlayer);
                }

                // Calculate tilt based on movement and sway
                double tiltZ = toPlayer.getZ() * 30.0 * -1.0;
                double tiltX = toPlayer.getX() * 30.0 * -1.0;

                // Add subtle tilt from swaying when idle
                tiltX += idleFactor * 15.0 * Math.sin(currentSwayPhase);

                EulerAngle tilt = new EulerAngle(
                        Math.toRadians(tiltZ),
                        Math.toRadians(currentLoc.getYaw()),
                        Math.toRadians(tiltX)
                );

                // Always match player's rotation exactly
                float playerYaw = currentLoc.getYaw();

                // Update balloon position and rotation
                balloon.teleport(targetLoc);
                balloon.setRotation(playerYaw, 0);

                // Set head pose to match player direction
                EulerAngle headPose = new EulerAngle(
                        Math.toRadians(tiltZ),
                        0, // Keep Y rotation at 0 to maintain forward orientation
                        Math.toRadians(tiltX)
                );
                balloon.setHeadPose(headPose);

                // Update chicken position to sit on top of balloon
                leadAnchor.teleport(targetLoc.clone().add(0, 0.5, 0));

                // Check if balloon is too far and force teleport it closer
                if (balloon.getLocation().distance(currentLoc) > 5.0) {
                    balloon.teleport(currentLoc.clone().add(0, BALLOON_HEIGHT, 0));
                    leadAnchor.teleport(balloon.getLocation().clone().add(0, 0.5, 0));
                }

                // Update tracking data
                lastPlayerLocations.put(uuid, currentLoc.clone());

            } catch (Exception e) {
                plugin.getLogger().warning("Error updating balloon for player " + player.getName() + ": " + e.getMessage());
                cleanupOldLeads(player.getWorld(), player.getLocation());
                removeBalloon(uuid);

                // Try to recreate after error
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        ItemStack newBalloon = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBalloon(player);
                        if (newBalloon != null && ItemManager.isBalloon(newBalloon, configManager)) {
                            createBalloon(player, newBalloon);
                        }
                    }
                }, 5L);
            }
        });
    }

    public void checkAndRestoreBalloon(Player player) {
        ItemStack balloon = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBalloon(player);
        if (balloon != null && ItemManager.isBalloon(balloon, configManager)) {
            createBalloon(player, balloon);
        }
    }

    public void shutdown() {
        new HashMap<>(activeBalloons).forEach((uuid, balloon) -> {
            removeBalloon(uuid);
        });
        lastPlayerLocations.clear();
        idleTime.clear();
        bobPhase.clear();
        swayPhase.clear();
    }

    public boolean hasBalloon(UUID uuid) {
        return activeBalloons.containsKey(uuid);
    }
}