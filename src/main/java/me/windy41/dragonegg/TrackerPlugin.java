package me.windy41.dragonegg;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import github.scarsz.discordsrv.DiscordSRV;
public class TrackerPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("DragonEggTracker enabled");
        getServer().getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            @Override
            public void run() {
                trackDragonEgg();
            }
        }.runTaskTimer(this, 0, 20 * 60 * 60); // every hour
        new BukkitRunnable() {
            @Override
            public void run() {
                halfWayWarning();
            }
        }.runTaskTimer(this, 20 * 60 * 30, 20 * 60 * 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack item = player.getInventory().getItemInOffHand();

                    boolean hasEgg = item != null && item.getType() == Material.DRAGON_EGG;

                    if (hasEgg) {
                        // Strength I
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, true, false, false));

                        // Extra 3 hearts
                        if (player.getAttribute(Attribute.MAX_HEALTH) != null &&
                            player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() == 20.0) {
                            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(26.0);
                        }

                        // Mark player as immune to fall damage (see EntityDamageEvent below)
                        player.setMetadata("eggHolder", new FixedMetadataValue(TrackerPlugin.this, true));
                    } else {
                        // Remove effects if not holding egg
                        player.removePotionEffect(PotionEffectType.STRENGTH);

                        // Reset max health
                        if (player.getAttribute(Attribute.MAX_HEALTH) != null &&
                            player.getAttribute(Attribute.MAX_HEALTH).getBaseValue() > 20.0) {
                            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
                        }

                        player.removeMetadata("eggHolder", TrackerPlugin.this);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // Runs every second

    }
    
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot drop the Dragon Egg!");
        }
    }
    
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (player.hasMetadata("eggHolder")) {
                event.setCancelled(true);
            }
        }
    }
    
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("egghunt")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("ping")) {
                trackDragonEgg();
                sender.sendMessage("§aEgg location tracked and sent to Discord.");
                return true;
            }
        }
        return false;
    }
    
    private void halfWayWarning() {

    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() == Material.DRAGON_EGG) {
            // Block placing into containers or moving around
            if (event.getClickedInventory() != null && !(event.getClickedInventory().getHolder() instanceof Player)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    player.sendMessage("§cYou cannot store the Dragon Egg in containers!");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack dragged = event.getOldCursor();
        if (dragged != null && dragged.getType() == Material.DRAGON_EGG) {
            // Cancel if any slot is outside the player's inventory
            for (int slot : event.getRawSlots()) {
                if (slot >= event.getView().getTopInventory().getSize()) continue;
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    player.sendMessage("§cYou cannot drag the Dragon Egg into containers!");
                }
                break;
            }
        }
    }
    
    private void trackDragonEgg() {
        boolean foundInInventory = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isHoldingEgg = false;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.DRAGON_EGG) {
                    isHoldingEgg = true;
                    foundInInventory = true;

                    // Apply glowing effect
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 61, 1, false, false));

                    String world = player.getWorld().getName();
                    int x = player.getLocation().getBlockX();
                    int z = player.getLocation().getBlockZ();
                    String msg = "**" + player.getName() + "** is holding the 🥚Dragon Egg at X:" + x + " Z:" + z + " in world `" + world + "`.";
                    sendToDiscord(msg);
                    return;
                }
            }

            // Remove glow if not holding
            if (!isHoldingEgg) {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }
        }

        if (!foundInInventory) {
            // Search placed eggs in all loaded chunks
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < world.getMaxHeight(); y++) {
                            for (int z = 0; z < 16; z++) {
                                Block block = chunk.getBlock(x, y, z);
                                if (block.getType() == Material.DRAGON_EGG) {
                                    String msg = "🥚 A Dragon Egg is placed at X:" + block.getX() + " Y:" + block.getY() + " Z:" + block.getZ() + " in world `" + world.getName() + "`.";
                                    sendToDiscord(msg);
                                    return;
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private void sendToDiscord(String msg) {
    	github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById("1371403308056580136");
    	channel.sendMessage(msg).queue();
    	getLogger().info(msg);
    }

	@EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                event.getDrops().removeIf(drop -> drop.getType() == Material.DRAGON_EGG);

                Block block = player.getLocation().getBlock();
                if (block.getType() == Material.AIR || block.isPassable()) {
                    block.setType(Material.DRAGON_EGG);
                } else {
                    block.getLocation().add(0, 1, 0).getBlock().setType(Material.DRAGON_EGG);
                }

                break;
            }
        }
    }
}
