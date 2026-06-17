package it.markus.playerstats.listener;

import it.markus.playerstats.PlayerStatsPlugin;
import it.markus.playerstats.custom.CustomKeys;
import it.markus.playerstats.custom.CustomStatService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Zaehlt Sammel-CUSTOM-Stats: geerntete Beeren (Sweet/Glow) und mit der Flasche
 * geerntete Honig-Portionen.
 */
public final class GatheringListener implements Listener {

    private final CustomStatService custom;

    public GatheringListener(PlayerStatsPlugin plugin) {
        this.custom = plugin.custom();
    }

    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent event) {
        long berries = 0;
        for (ItemStack item : event.getItemsHarvested()) {
            if (item.getType() == Material.SWEET_BERRIES || item.getType() == Material.GLOW_BERRIES) {
                berries += item.getAmount();
            }
        }
        if (berries > 0) {
            custom.add(event.getPlayer().getUniqueId(), CustomKeys.BERRIES_COLLECTED, berries);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getMaterial() != Material.GLASS_BOTTLE) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Beehive hive)) {
            return;
        }
        // Nur volle Bienenstoecke geben Honig.
        if (hive.getHoneyLevel() >= hive.getMaximumHoneyLevel()) {
            custom.add(event.getPlayer().getUniqueId(), CustomKeys.HONEY_COLLECTED, 1);
        }
    }
}
