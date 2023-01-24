package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jin_ou
 */
public class TpsTask implements Runnable {
    private long lastTime = -1;
    private final List<Double> historyTps = new LinkedList<>();

    @Override
    public void run() {
        long curTime = System.nanoTime();

        if (lastTime == -1) {
            lastTime = curTime;
            return;
        }

        long timeSpent = curTime - lastTime;

        int maxHistory = 10;
        if (this.historyTps.size() > maxHistory) {
            this.historyTps.remove(0);
        }

        // 总 tick / seconds = tps（ticks per second，每秒能处理几个tick）
        // 20 tick, 1纳秒 = 1e9秒
        double tps = 20.0 / (timeSpent / 1E9);

        historyTps.add(tps);

        this.lastTime = curTime;
    }

    /**
     * @return 最近10秒的平均tps
     */
    public double getAverageTps() {
        double total = 0D;

        for (double tps : this.historyTps) {
            total += tps;
        }

        return total / this.historyTps.size();
    }

    /**
     * This method starts the {@link TpsTask} on an asynchronous schedule.
     *
     * @param plugin
     *            The instance of our {@link Slimefun}
     */
    public void start(@Nonnull Slimefun plugin) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this, 0L, 20L);
    }
}
