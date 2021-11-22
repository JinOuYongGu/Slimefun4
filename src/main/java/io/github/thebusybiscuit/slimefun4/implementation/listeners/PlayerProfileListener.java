package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.Optional;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * This {@link Listener} removes a {@link PlayerProfile} from memory if the corresponding {@link Player}
 * has left the {@link Server} or was kicked.
 * 
 * @author TheBusyBiscuit
 * @author SoSeDiK
 *
 */
public class PlayerProfileListener implements Listener {

    public PlayerProfileListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e) {
        Optional<PlayerProfile> profile = PlayerProfile.find(e.getPlayer());

        // if the player left the server, saves its profile instantly
        if (profile.isPresent()) {
            profile.get().save();

            String playerName = e.getPlayer().getName();
            Slimefun.logger().log(Level.INFO, "成功保存玩家 {0} 的数据", playerName);

            if (Slimefun.getRegistry().getPlayerProfiles().values().remove(profile.get())) {
                Slimefun.logger().log(Level.INFO, "已安全删除玩家 {0} 的缓存", playerName);
            }
        }
        
        // if we still have a profile of this Player in memory, delete it
        profile.ifPresent(PlayerProfile::markForDeletion);
    }

    @EventHandler(ignoreCancelled = true)
    public void onKick(PlayerKickEvent e) {
        Optional<PlayerProfile> profile = PlayerProfile.find(e.getPlayer());

        // if the player left the server, saves its profile instantly
        if (profile.isPresent()) {
            profile.get().save();

            String playerName = e.getPlayer().getName();
            Slimefun.logger().log(Level.INFO, "成功保存玩家 {0} 的数据", playerName);

            if (Slimefun.getRegistry().getPlayerProfiles().values().remove(profile.get())) {
                Slimefun.logger().log(Level.INFO, "已安全删除玩家 {0} 的缓存", playerName);
            }
        }

        // if we still have a profile of this Player in memory, delete it
        profile.ifPresent(PlayerProfile::markForDeletion);
    }

}
