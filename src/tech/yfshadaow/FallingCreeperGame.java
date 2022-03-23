package tech.yfshadaow;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static tech.yfshadaow.GameUtils.world;

public class FallingCreeperGame extends Game implements Listener {

    private static final FallingCreeperGame instance = new FallingCreeperGame((FallingCreeper) Bukkit.getPluginManager().getPlugin("FallingCreeper"));

    List<Player> playersAlive;
    ItemStack rod;
    Random random;
    List<Creeper> creepers;
    List<Block> blocks;
    int spawnFrequency;

    private FallingCreeperGame(FallingCreeper plugin) {
        rod = new ItemStackBuilder(Material.BLAZE_ROD).setDisplayName("§e击退棒").setLore("§b把苦力怕推向你的对手！").addEnchantment(Enchantment.KNOCKBACK, 3, true).build();
        this.plugin = plugin;
        random = new Random();
        creepers = new ArrayList<>();
        blocks = new ArrayList<>();
        players = plugin.players;
        playersAlive = new ArrayList<>();
        initGame(plugin, "FallingCreeper", "§e天降苦力怕", 10, new Location(world, 1000, 13, 2004), BlockFace.NORTH,
                new Location(world, 996, 13, 2000), BlockFace.EAST, new Location(world, 1000, 12, 2000), new BoundingBox(700, -64, 1300, 1700, 320, 2300));
    }


    public static FallingCreeperGame getInstance() {
        return instance;
    }


    @EventHandler
    public void preventBlockDrop(EntityExplodeEvent eee) {
        if (gameBoundingBox.contains(eee.getLocation().toVector())) {
            if (eee.getLocation().getY() > 40) {
                for (Block b : eee.blockList()) {
                    blocks.remove(b);
                }
                eee.setYield(0f);
            } else {
                eee.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void preventDamage(EntityDamageByEntityEvent edbee) {
        if (edbee.getEntity() instanceof Creeper && edbee.getDamager() instanceof Player) {
            if (creepers.contains(edbee.getEntity())) {
            }
        } else if (edbee.getEntity() instanceof Player && edbee.getDamager() instanceof Player) {
            if (players.contains(edbee.getEntity()) && players.contains(edbee.getDamager())) {
                edbee.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent ede) {
        if (creepers.contains(ede.getEntity())) {
            ede.setDamage(0.01);
            /*
            Vector v = ede.getEntity().getVelocity().clone();
            v.setY(0);
            ede.getEntity().setVelocity(v);
            Location l = ede.getEntity().getLocation().clone();
            l.setY(100);
            ede.getEntity().teleport(l);

             */
        }
    }

    @EventHandler
    public void preventDropping(PlayerDropItemEvent pdie) {
        if (players.contains(pdie.getPlayer())) {
            pdie.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangeGame(PlayerChangeGameEvent pcge) {
        players.remove(pcge.getPlayer());
        playersAlive.remove(pcge.getPlayer());
    }

    @Override
    protected void initGameRunnable() {
        gameRunnable = () -> {
            spawnFrequency = FallingCreeper.spawnFrequency;
            Collection<Player> startingPlayers = getStartingPlayers();
            players.addAll(startingPlayers);
            playersAlive.addAll(startingPlayers);
            if (players.size() < 2) {
                for (Player p : players) {
                    p.sendMessage("§c至少需要2人才能开始游戏！");
                }
                players.clear();
                playersAlive.clear();
            } else {
                for (int x = 985; x <= 1016; x ++) {
                    for (int z = 1985; z <= 2016; z ++) {
                        blocks.add(world.getBlockAt(x, 99, z));
                    }
                }
                pasteSchematic("fallingcreeper", 1000, 100, 2000, true);
                Bukkit.getPluginManager().registerEvents(this, plugin);
                removeStartButton();
                placeSpectateButton();
                startCountdown();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player p : players) {
                        p.teleport(new Location(world, 1000.5, 100.0, 2000.5));
                        p.getInventory().clear();
                    }
                });
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player p : players) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 999999, 49, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999, 0, false, false));
                        p.getInventory().setItem(0, rod.clone());
                    }

                }, countDownSeconds * 20L);

                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    Block b = blocks.get(random.nextInt(blocks.size()));
                    Location l = b.getLocation();
                    l.setY(128);
                    l.setYaw((float) ((random.nextDouble() - 0.5) * 2 * 180));
                    Creeper c = world.spawn(l, Creeper.class);
                    Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                        if (!c.isDead()) {
                            c.setGravity(false);
                            /*
                            taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                                if (!c.isDead()) {
                                    Vector v = c.getVelocity().clone();
                                    if (v.getY() != 0) {
                                        v.setY(0);
                                        c.setVelocity(v);
                                    }
                                    Location loc = c.getLocation().clone();
                                    if (loc.getY() != 100) {
                                        loc.setY(100);
                                        c.teleport(loc);
                                    }
                                }
                            }, 1, 1));

                             */
                        }
                    }, 20);
                    c.setMaxFuseTicks(75);
                    c.ignite();
                    c.setAware(false);
                    if (random.nextInt(10) == 0) {
                        c.setPowered(true);
                    }
                    c.setVelocity(new Vector(0, -10, 0));
                    creepers.add(c);
                }, countDownSeconds * 20L, spawnFrequency));
                /*
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    for (Creeper c : creepers) {
                        if (c != null) {
                            if (!c.isDead()) {
                                Location l = c.getLocation().clone();
                                float yaw = l.getYaw();
                                yaw += 10;
                                if (yaw > 180) {
                                    yaw = - (180 - (yaw - 180));
                                }
                                l.setYaw(yaw);
                            }
                        }
                    }
                }, countDownSeconds * 20L, 2));

                 */

                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    Player playerOut = null;
                    for (Player p : playersAlive) {
                        if (p.getLocation().getY() <= 50) {
                            playerOut = p;
                            break;
                        }
                    }
                    if (playerOut != null) {
                        for (Player p : players) {
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§f§l" + playerOut.getName() + " §c被苦力怕推进了虚空！"));
                        }
                        playerOut.setGameMode(GameMode.SPECTATOR);
                        playersAlive.remove(playerOut);
                    }

                }, countDownSeconds * 20L, 1));
                taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (playersAlive.size() == 2) {
                        for (Player p : playersAlive) {
                            p.getInventory().clear();
                        }
                    } else if (playersAlive.size() <= 1) {
                        for (Creeper c : creepers) {
                            if (c != null) {
                                if (!c.isDead()) {
                                    c.remove();
                                }
                            }
                        }
                        creepers.clear();
                        blocks.clear();
                        Player winner = playersAlive.get(0);
                        spawnFireworks(winner);
                        List<Player> playersCopy = new ArrayList<>(players);
                        for (Player p : playersCopy) {
                            p.sendTitle("§e" + playersAlive.get(0).getName() + " §b获胜了！", null, 5, 50, 5);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                p.teleport(new Location(world, 1000.5, 12, 2000.5));
                                Bukkit.getPluginManager().callEvent(new PlayerEndGameEvent(p, this));
                                pasteSchematic("fallingcreeperempty", 1000, 100, 2000, false);
                            }, 100);
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            placeStartButton();
                            removeSpectateButton();
                            HandlerList.unregisterAll(this);
                        }, 100);
                        players.clear();
                        playersAlive.clear();
                        List<Integer> taskIdsCopy = new ArrayList<>(taskIds);
                        taskIds.clear();
                        for (int i : taskIdsCopy) {
                            Bukkit.getScheduler().cancelTask(i);
                        }
                    }

                }, countDownSeconds * 20L, 1));
            }
        };
    }

    @Override
    protected void savePlayerQuitData(Player p) throws IOException {
        players.remove(p);
        playersAlive.remove(p);
    }


    @Override
    protected void rejoin(Player player) {

    }

    private void pasteSchematic(String name, double x, double y, double z, boolean ignoreAir) {
        File file = new File("plugins/WorldEdit/schematics/" + name + ".schem");
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        Clipboard clipboard = null;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(operation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
