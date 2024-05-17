package ru.blackflamest.jkitemfixer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class main extends JavaPlugin implements Listener {
    private ItemChecker checker;

    public static main instance;
    public static main getInstance() {return instance;};

    @Override
    public void onEnable() {
        this.checker = new ItemChecker();
        instance = this;
        getLogger().info("Плагин запущен.");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new EventListener(), this);

    }

    public boolean checkItem(ItemStack stack, Player p) {
        return this.checker.isHackedItem(stack, p);
    }


    @Override
    public void onDisable() {
        getLogger().info("Плагин выключен.");
    }

}
