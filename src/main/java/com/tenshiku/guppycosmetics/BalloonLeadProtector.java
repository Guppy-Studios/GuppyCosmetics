package com.tenshiku.guppycosmetics;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Player;

/**
 * This class specifically handles events related to balloon lead detachment prevention
 */
public class BalloonLeadProtector implements Listener {

    private final GuppyCosmetics plugin;

    public BalloonLeadProtector(GuppyCosmetics plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent players from interacting directly with balloon entities
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Check if the entity is a balloon armorstand
        if (entity instanceof ArmorStand && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("Balloon:")) {
            event.setCancelled(true);
            return;
        }

        // Check if the entity is a balloon anchor chicken
        if (entity instanceof Chicken && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("BalloonAnchor:")) {
            event.setCancelled(true);
            return;
        }

        // Also prevent interaction with lead hitches (if any)
        if (entity instanceof LeashHitch) {
            // Get nearby entities to see if this might be connected to a balloon
            for (Entity nearby : entity.getNearbyEntities(5, 5, 5)) {
                if ((nearby instanceof Chicken && nearby.getCustomName() != null &&
                        nearby.getCustomName().startsWith("BalloonAnchor:")) ||
                        (nearby instanceof ArmorStand && nearby.getCustomName() != null &&
                                nearby.getCustomName().startsWith("Balloon:"))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * This is the critical event handler that directly prevents leads from being detached
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerUnleashEntity(PlayerUnleashEntityEvent event) {
        Entity entity = event.getEntity();

        // Check if this is our balloon anchor chicken
        if (entity instanceof Chicken && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("BalloonAnchor:")) {
            event.setCancelled(true);
            // Return false to prevent lead item from dropping
            event.setDropLeash(false);
            return;
        }
    }

    /**
     * Prevent lead hitches from breaking if they belong to a balloon
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof LeashHitch) {
            LeashHitch hitch = (LeashHitch) event.getEntity();

            // Check if any nearby entities are balloon related
            for (Entity nearby : hitch.getNearbyEntities(5, 5, 5)) {
                if ((nearby instanceof Chicken && nearby.getCustomName() != null &&
                        nearby.getCustomName().startsWith("BalloonAnchor:")) ||
                        (nearby instanceof ArmorStand && nearby.getCustomName() != null &&
                                nearby.getCustomName().startsWith("Balloon:"))) {

                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    /**
     * Specifically handle entity-caused breaking of leads
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getEntity() instanceof LeashHitch) {
            LeashHitch hitch = (LeashHitch) event.getEntity();

            // Check if any nearby entities are balloon related
            for (Entity nearby : hitch.getNearbyEntities(5, 5, 5)) {
                if ((nearby instanceof Chicken && nearby.getCustomName() != null &&
                        nearby.getCustomName().startsWith("BalloonAnchor:")) ||
                        (nearby instanceof ArmorStand && nearby.getCustomName() != null &&
                                nearby.getCustomName().startsWith("Balloon:"))) {

                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}