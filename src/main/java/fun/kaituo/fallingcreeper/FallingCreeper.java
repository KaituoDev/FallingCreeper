package fun.kaituo.fallingcreeper;

import fun.kaituo.gameutils.Game;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.event.PlayerChangeGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


public class FallingCreeper extends JavaPlugin implements Listener {
    private GameUtils gameUtils;

    static List<Player> players;
    static int spawnFrequency;

    public static FallingCreeperGame getGameInstance() {
        return FallingCreeperGame.getInstance();
    }


    @EventHandler
    public void onButtonClicked(PlayerInteractEvent pie) {
        if (!pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!pie.getClickedBlock().getType().equals(Material.OAK_BUTTON)) {
            return;
        }
        if (pie.getClickedBlock().getLocation().equals(new Location(gameUtils.getWorld(), 1000, 13, 2004))) {
            FallingCreeperGame.getInstance().startGame();
        }
    }

    @EventHandler
    public void setCreeperSpawnRate(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        Location location = pie.getClickedBlock().getLocation();
        long x = location.getBlockX();
        long y = location.getBlockY();
        long z = location.getBlockZ();
        if (x == 1000 && y == 14 && z == 2004) {
            if (pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                if (pie.getPlayer().isSneaking()) {
                    if (spawnFrequency == 5) {
                        return;
                    } else {
                        spawnFrequency -= 5;
                    }
                } else {
                    if (spawnFrequency == 100) {
                        return;
                    } else {
                        spawnFrequency += 5;
                    }
                }
            }
            Sign sign = (Sign) pie.getClickedBlock().getState();
            sign.setLine(3, spawnFrequency + " 刻生成1只");
            sign.update();
        }
    }


    public void onEnable() {
        gameUtils = (GameUtils) Bukkit.getPluginManager().getPlugin("GameUtils");
        players = new ArrayList<>();
        spawnFrequency = 40;
        Bukkit.getPluginManager().registerEvents(this, this);
        gameUtils.registerGame(getGameInstance());
        Sign sign = (Sign) gameUtils.getWorld().getBlockAt(1000, 14, 2004).getState();
        sign.setLine(3, +spawnFrequency + " 刻生成1只");
        sign.update();
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin) this);
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (gameUtils.getPlayerGame(p) == getGameInstance()) {
                Bukkit.dispatchCommand(p, "join Lobby");
            }
        }
        gameUtils.unregisterGame(getGameInstance());
    }
}
