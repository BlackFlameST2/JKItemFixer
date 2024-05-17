package ru.blackflamest.jkitemfixer;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getType() != EntityType.PLAYER)
            return;
        Player p = (Player)event.getWhoClicked();
        if (event.getCurrentItem() == null)
            return;
        if (main.getInstance().checkItem(event.getCurrentItem(), p)) {
            event.setCancelled(true);
            p.updateInventory();
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (event.getItemDrop() == null)
            return;
        if (main.getInstance().checkItem(event.getItemDrop().getItemStack(), p)) {
            event.setCancelled(true);
            p.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onSlotChange(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        ItemStack stack = p.getInventory().getItem(event.getNewSlot());
        if (main.getInstance().checkItem(stack, p)) {
            event.setCancelled(true);
            p.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onItem(InventoryCreativeEvent event) {
        Player p = (Player) event.getWhoClicked();
        ItemStack stack = event.getCursor();
        if (main.getInstance().checkItem(stack, p)) {
            event.setCancelled(true);
            p.updateInventory();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (ItemStack stack : event.getPlayer().getInventory().getContents())
            main.getInstance().checkItem(stack, event.getPlayer());
    }
}
