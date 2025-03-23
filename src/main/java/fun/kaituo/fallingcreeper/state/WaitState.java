package fun.kaituo.fallingcreeper.state;

import fun.kaituo.fallingcreeper.FallingCreeper;
import fun.kaituo.fallingcreeper.util.IntervalSign;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.Misc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaitState implements GameState, Listener {
    public static final WaitState INST = new WaitState();
    private WaitState() {}

    private FallingCreeper game;
    private Location hubLoc;
    private Location startButtonLoc;
    private IntervalSign intervalSign;

    public void init() {
        game = FallingCreeper.inst();
        hubLoc = game.getLoc("hub");
        startButtonLoc = game.getLoc("startButton");

        Location intervalSignLoc = game.getLoc("intervalSign");
        assert intervalSignLoc != null;
        intervalSign = new IntervalSign(game, intervalSignLoc);
    }

    public int getSpawnIntervalTicks() {
        return intervalSign.getSpawnIntervalTicks();
    }

    @EventHandler
    public void onClickStartButton(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Block block = e.getClickedBlock();
        assert block != null;
        if (!block.getLocation().equals(startButtonLoc)) {
            return;
        }
        if (game.getPlayers().size() < 2) {
            e.getPlayer().sendMessage("§c至少需要两名玩家才能开始游戏！");
        } else {
            game.setState(ReadyState.INST);
        }
    }

    @Override
    public void enter() {
        Bukkit.getPluginManager().registerEvents(this, game);
        Bukkit.getPluginManager().registerEvents(intervalSign, game);
        Misc.pasteSchematicAtOriginalPosition("FallingCreeperEmpty", hubLoc.getWorld(), false);
        for (Player p : game.getPlayers()) {
            addPlayer(p);
        }
    }

    @Override
    public void exit() {
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(intervalSign);
        for (Player p : game.getPlayers()) {
            removePlayer(p);
        }
    }

    @Override
    public void tick() {}

    @Override
    public void addPlayer(Player p) {
        p.teleport(hubLoc);
        p.getInventory().addItem(Misc.getMenu());
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
    }

    @Override
    public void removePlayer(Player p) {
        p.getInventory().clear();
        p.removePotionEffect(PotionEffectType.SATURATION);
        p.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    @Override
    public void forceStop() {}
}
