package me.windy41.dragonegg;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class TrackerPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("DragonEggTracker enabled");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot drop the Dragon Egg!");
        }
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
