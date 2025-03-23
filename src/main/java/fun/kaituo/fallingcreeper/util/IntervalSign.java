package fun.kaituo.fallingcreeper.util;

import fun.kaituo.gameutils.util.AbstractSignListener;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class IntervalSign extends AbstractSignListener {
    @Getter
    private int spawnIntervalTicks = 10;

    public IntervalSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1, "§f§l苦力怕生成间隔");
        lines.set(2, "§b§l" + spawnIntervalTicks + "§f§l 游戏刻");
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        if (spawnIntervalTicks < 40) {
            spawnIntervalTicks += 1;
            lines.set(2, "§b§l" + spawnIntervalTicks + "§f§l 游戏刻");
            update();
        }
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        if (spawnIntervalTicks > 1) {
            spawnIntervalTicks -= 1;
            lines.set(2, "§b§l" + spawnIntervalTicks + "§f§l 游戏刻");
            update();
        }
    }
}
