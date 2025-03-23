package fun.kaituo.fallingcreeper;

import fun.kaituo.fallingcreeper.state.PlayState;
import fun.kaituo.fallingcreeper.state.ReadyState;
import fun.kaituo.fallingcreeper.state.WaitState;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FallingCreeper extends Game {
    private static FallingCreeper instance;
    public static FallingCreeper inst() {
        return instance;
    }

    public final Set<UUID> playerIds = new HashSet<>();

    public Set<Player> getPlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID id : playerIds) {
            Player p = Bukkit.getPlayer(id);
            assert p != null;
            players.add(p);
        }
        return players;
    }

    private void initStates() {
        WaitState.INST.init();
        ReadyState.INST.init();
        PlayState.INST.init();
    }

    @Override
    public void addPlayer(Player p) {
        playerIds.add(p.getUniqueId());
        super.addPlayer(p);
    }

    @Override
    public void removePlayer(Player p) {
        playerIds.remove(p.getUniqueId());
        super.removePlayer(p);
    }

    @Override
    public void forceStop() {
        super.forceStop();
    }


    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        updateExtraInfo("§a天降苦力怕", getLoc("hub"));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            initStates();
            setState(WaitState.INST);
        }, 1);
    }

    @Override
    public void onDisable() {
        state.exit();
        for (Player p : getPlayers()) {
            removePlayer(p);
            GameUtils.inst().join(p, GameUtils.inst().getLobby());
        }
        super.onDisable();
    }
}
