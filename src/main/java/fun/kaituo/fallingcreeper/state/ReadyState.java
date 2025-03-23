package fun.kaituo.fallingcreeper.state;

import fun.kaituo.fallingcreeper.FallingCreeper;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.Misc;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class ReadyState implements GameState, Listener {
    public static final int COUNTDOWN_SECONDS = 10;
    public static final ReadyState INST = new ReadyState();
    private ReadyState() {}

    private FallingCreeper game;
    @Getter
    private Location startLoc;
    private final Set<Integer> taskIds = new HashSet<>();

    public void init() {
        game = FallingCreeper.inst();
        startLoc = game.getLoc("start");
    }

    @Override
    public void enter() {
        Misc.pasteSchematicAtOriginalPosition("FallingCreeper", startLoc.getWorld());
        for (Player p : game.getPlayers()) {
            addPlayer(p);
            taskIds.addAll(Misc.displayCountdown(p, COUNTDOWN_SECONDS, game));
        }
        taskIds.add(Bukkit.getScheduler().runTaskLater(game, () -> {
            game.setState(PlayState.INST);
        }, COUNTDOWN_SECONDS * 20).getTaskId());
        Bukkit.getPluginManager().registerEvents(this, game);
    }

    @Override
    public void exit() {
        for (Player p : game.getPlayers()) {
            removePlayer(p);
        }
        for (int id : taskIds) {
            Bukkit.getScheduler().cancelTask(id);
        }
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void preventDamage(EntityDamageByEntityEvent e) {
        if (game.playerIds.contains(e.getDamager().getUniqueId()) && game.playerIds.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public void addPlayer(Player p) {
        p.teleport(startLoc);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
    }

    @Override
    public void removePlayer(Player p) {
        p.removePotionEffect(PotionEffectType.SATURATION);
        p.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    @Override
    public void forceStop() {
        game.setState(WaitState.INST);
    }
}
