package fun.kaituo.fallingcreeper.state;

import fun.kaituo.fallingcreeper.FallingCreeper;
import fun.kaituo.gameutils.game.GameState;
import fun.kaituo.gameutils.util.GameInventory;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PlayState implements GameState, Listener {
    public static final double POWERED_CHANCE = 0.1;
    public static final int CREEPER_MAX_FUSE_TICKS = 40;
    public static final int DEATH_Y = 60;
    public static final PlayState INST = new PlayState();
    private PlayState() {}

    private final Set<Integer> taskIds = new HashSet<>();
    private FallingCreeper game;
    private GameInventory inv;
    private final Set<UUID> playersAlive = new HashSet<>();
    private final Set<Creeper> creepers = new HashSet<>();
    private Location platform1;
    private Location platform2;
    private final List<Block> remainingBlocks = new ArrayList<>();
    private final Random random = new Random();
    private boolean isDuel = false;

    public Set<Player> getPlayersAlive() {
        Set<Player> players = new HashSet<>();
        for (UUID id : playersAlive) {
            Player p = game.getServer().getPlayer(id);
            assert p != null;
            players.add(p);
        }
        return players;
    }

    public void init() {
        game = FallingCreeper.inst();
        inv = game.getInv("play");
        platform1 = game.getLoc("platform1");
        platform2 = game.getLoc("platform2");
    }

    private void initRemainingBlocks() {
        int y = platform1.getBlockY();
        for (int x = platform1.getBlockX(); x <= platform2.getBlockX(); x += 1) {
            for (int z = platform1.getBlockZ(); z <= platform2.getBlockZ(); z += 1) {
                remainingBlocks.add(platform1.getWorld().getBlockAt(x, y, z));
            }
        }
    }

    private void clearCreepers() {
        for (Creeper c : creepers) {
            if (!c.isValid()) {
                continue;
            }
            c.remove();
        }
        creepers.clear();
    }

    @Override
    public void enter() {
        isDuel = false;
        for (Player p : game.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
            inv.apply(p);
            playersAlive.add(p.getUniqueId());
        }
        Bukkit.getPluginManager().registerEvents(this, game);
        initRemainingBlocks();
        taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(game, this::spawnCreeper, WaitState.INST.getSpawnIntervalTicks(), WaitState.INST.getSpawnIntervalTicks()));
    }

    private void spawnCreeper() {
        Block b = remainingBlocks.get(random.nextInt(remainingBlocks.size()));
        Location blockLoc = b.getLocation();
        Location spawnLoc = blockLoc.clone().add(0.5, 16, 0.5);
        Creeper c = spawnLoc.getWorld().spawn(spawnLoc, Creeper.class);
        if (random.nextDouble() < POWERED_CHANCE) {
            c.setPowered(true);
        }
        c.setVelocity(new Vector(0, -100, 0));
        c.setMaxFuseTicks(CREEPER_MAX_FUSE_TICKS);
        creepers.add(c);
    }

    @Override
    public void exit() {
        for (Player p : game.getPlayers()) {
            removePlayer(p);
        }
        for (int id : taskIds) {
            game.getServer().getScheduler().cancelTask(id);
        }
        HandlerList.unregisterAll(this);
        remainingBlocks.clear();
        clearCreepers();
        playersAlive.clear();
    }

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof Creeper c)) {
            return;
        }
        if (!creepers.contains(c)) {
            return;
        }
        remainingBlocks.removeAll(e.blockList());
        e.setYield(0);
    }

    @EventHandler
    public void preventDamage(EntityDamageByEntityEvent e) {
        if (game.playerIds.contains(e.getDamager().getUniqueId()) && game.playerIds.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventCreeperDamageDuringDuel(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Creeper c)) {
            return;
        }
        if (!creepers.contains(c)) {
            return;
        }
        if (getPlayersAlive().size() <= 2) {
            e.setCancelled(true);
        }
    }

    private void checkForEnd() {
        if (getPlayersAlive().size() == 1) {
            Player winner = getPlayersAlive().iterator().next();
            for (Player p : game.getPlayers()) {
                p.sendTitle("§a游戏结束", "§e" + winner.getName() + " §a获胜！", 10, 30, 20);
            }
            game.setState(WaitState.INST);
        } else if (getPlayersAlive().isEmpty()) {
            for (Player p : game.getPlayers()) {
                p.sendTitle("§a游戏结束", "§c无人存活！", 10, 30, 20);
            }
            game.setState(WaitState.INST);
        }
    }

    private void checkForDuel() {
        if (isDuel) {
            return;
        }
        if (getPlayersAlive().size() <= 2) {
            for (Player p : game.getPlayers()) {
                p.getInventory().clear();
                p.sendMessage("§a场上仅剩两名玩家，决斗开启，苦力怕不再能被击退！");
            }
            isDuel = true;
        }
    }

    private void checkForDeath() {
        for (Player p : game.getPlayers()) {
            if (p.getLocation().getY() <= DEATH_Y) {
                p.setGameMode(GameMode.SPECTATOR);
                playersAlive.remove(p.getUniqueId());
                for (Player target: game.getPlayers()) {
                    target.sendActionBar("§f§l" + p.getName() + " §c被苦力怕推进了虚空！");
                }
            }
        }
    }

    private void igniteCreepers() {
        for (Creeper c : creepers) {
            if (!c.isValid()) {
                continue;
            }
            if (!c.isOnGround()) {
                continue;
            }
            if (c.isIgnited()) {
                continue;
            }
            c.ignite();
        }
    }

    @Override
    public void tick() {
        checkForDeath();
        checkForEnd();
        igniteCreepers();
        checkForDuel();
    }

    @Override
    public void addPlayer(Player p) {
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(ReadyState.INST.getStartLoc());
    }

    @Override
    public void removePlayer(Player p) {
        playersAlive.remove(p.getUniqueId());
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.removePotionEffect(PotionEffectType.SATURATION);
    }

    @Override
    public void forceStop() {
        game.setState(WaitState.INST);
    }
}
