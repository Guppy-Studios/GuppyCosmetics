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

    private void removeBalloonEntity(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            cleanupOldLeads(player.getWorld(), player.getLocation());
        }

        // Remove lead anchor
        Chicken leadAnchor = leadAnchors.remove(playerId);
        if (leadAnchor != null && leadAnchor.isValid()) {
            leadAnchor.setLeashHolder(null);
            leadAnchor.remove();
        }

        // Remove balloon
        ArmorStand balloon = activeBalloons.remove(playerId);
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

        // Get current balloon from leggings slot
        ItemStack currentBalloon = player.getInventory().getLeggings();

        // Store the old balloon and put it back in inventory - only handle balloon items
        if (currentBalloon != null && ItemManager.isBalloon(currentBalloon, configManager)) {
            // Remove only the balloon entity first
            removeBalloonEntity(player.getUniqueId());

            // Add the old balloon back to inventory and clear leggings slot
            player.getInventory().addItem(currentBalloon.clone());
            player.getInventory().setLeggings(null);
        }

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

        // Set the new balloon in leggings slot and remove it from inventory
        player.getInventory().setLeggings(balloonItem.clone());
        player.getInventory().removeItem(balloonItem);
    }

    public void removeBalloon(UUID playerId) {
        // Clean up tracking data
        lastPlayerLocations.remove(playerId);
        idleTime.remove(playerId);
        bobPhase.remove(playerId);
        swayPhase.remove(playerId);

        // Only remove balloon-related entities
        removeBalloonEntity(playerId);
    }

    private void updateAllBalloons() {
        new HashMap<>(activeBalloons).forEach((playerId, balloon) -> {
            Player player = Bukkit.getPlayer(playerId);
            Chicken leadAnchor = leadAnchors.get(playerId);
            Location lastLoc = lastPlayerLocations.get(playerId);

            // Validate entities
            if (!balloon.isValid() || player == null || !player.isOnline() ||
                    leadAnchor == null || !leadAnchor.isValid()) {
                removeBalloon(playerId);
                return;
            }

            // Validate item in leggings slot
            ItemStack leggings = player.getInventory().getLeggings();
            if (leggings == null || !ItemManager.isBalloon(leggings, configManager)) {
                removeBalloon(playerId);
                return;
            }

            Location currentLoc = player.getLocation();

            // Handle teleports or large movements
            if (lastLoc != null && (currentLoc.getWorld() != lastLoc.getWorld() ||
                    currentLoc.distanceSquared(lastLoc) > 100)) {
                removeBalloon(playerId);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        ItemStack newLeggings = player.getInventory().getLeggings();
                        if (newLeggings != null && ItemManager.isBalloon(newLeggings, configManager)) {
                            createBalloon(player, newLeggings);
                        }
                    }
                }, 2L);
                return;
            }

            // Calculate player movement
            double movement = lastLoc != null ? currentLoc.distance(lastLoc) : 0;
            double currentIdleTime = idleTime.getOrDefault(playerId, 0.0);

            // Update idle time based on movement
            if (movement < IDLE_THRESHOLD) {
                currentIdleTime += 0.05; // Increment idle time (50ms for 20 TPS)
            } else {
                currentIdleTime = 0;
            }
            idleTime.put(playerId, currentIdleTime);

            // Update bobbing and swaying phases
            double currentBobPhase = bobPhase.getOrDefault(playerId, 0.0);
            double currentSwayPhase = swayPhase.getOrDefault(playerId, 0.0);
            currentBobPhase = (currentBobPhase + BOB_SPEED * 0.05) % (2 * Math.PI);
            currentSwayPhase = (currentSwayPhase + SWAY_SPEED * 0.05) % (2 * Math.PI);
            bobPhase.put(playerId, currentBobPhase);
            swayPhase.put(playerId, currentSwayPhase);

            // Update balloon physics with improved movement and idle animations
            try {
                // Calculate idle animations
                double idleFactor = Math.min(currentIdleTime, 2.0) / 2.0; // Smooth transition into idle animations
                double bobOffset = idleFactor * BOB_AMPLITUDE * Math.sin(currentBobPhase);
                double swayOffset = idleFactor * SWAY_AMPLITUDE * Math.sin(currentSwayPhase);

                Location targetLoc = currentLoc.clone();
                // Add -0.5 to Z position when idle, smoothly transitioning based on idleFactor
                targetLoc.add(
                        swayOffset * Math.cos(currentLoc.getYaw()),
                        bobOffset,
                        (swayOffset * Math.sin(currentLoc.getYaw())) + (-0.5 * idleFactor)
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

                // Update balloon position and rotation
                balloon.teleport(targetLoc);
                balloon.setHeadPose(tilt);

                // Update chicken position to sit on top of balloon
                leadAnchor.teleport(targetLoc.clone().add(0, 0.5, 0));

                // If balloon is too far, teleport closer
                if (balloon.getLocation().distance(currentLoc) > 5.0) {
                    balloon.teleport(currentLoc.clone().add(0, BALLOON_HEIGHT, 0));
                }

                // Update tracking data
                lastPlayerLocations.put(playerId, currentLoc.clone());

            } catch (Exception e) {
                cleanupOldLeads(player.getWorld(), player.getLocation());
                removeBalloon(playerId);
            }
        });
    }

    public void checkAndRestoreBalloon(Player player) {
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings != null && ItemManager.isBalloon(leggings, configManager)) {
            createBalloon(player, leggings);
        }
    }

    public void shutdown() {
        new HashMap<>(activeBalloons).forEach((playerId, balloon) -> {
            removeBalloon(playerId);
        });
        lastPlayerLocations.clear();
        idleTime.clear();
        bobPhase.clear();
        swayPhase.clear();
    }

    public boolean hasBalloon(UUID playerId) {
        return activeBalloons.containsKey(playerId);
    }
}