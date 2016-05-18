package net.acomputerdog.smallwarps;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Warp implements Listener {

    private final Server server;
    private final double x;
    private final double y;
    private final double z;

    private String worldName;
    private World world;

    private Location location;

    public Warp(JavaPlugin plugin, Location l) {
        this(plugin, l.getX(), l.getY(), l.getZ(), l.getWorld());
        this.location = l;
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, String worldName) {
        this.server = plugin.getServer();
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        server.getPluginManager().registerEvents(this, plugin);
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, World world) {
        this.server = plugin.getServer();
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.worldName = world.getName();
        server.getPluginManager().registerEvents(this, plugin);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public World getWorld() {
        if (world == null) {
            world = server.getWorld(worldName);
            if (world != null) {
                location = new Location(world, x, y, z);
            }
        }
        return world;
    }

    public Location getLocation() {
        if (location == null) {
            location = new Location(getWorld(), x, y, z);
        }
        return location;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent e) {
        if (worldName.equals(e.getWorld().getName())) {
            world = e.getWorld();
            location = new Location(world, x, y, z);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        if (e.getWorld() == this.world) {
            world = null;
            location = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Warp)) return false;

        Warp warp = (Warp) o;

        if (Double.compare(warp.x, x) != 0) return false;
        if (Double.compare(warp.y, y) != 0) return false;
        if (Double.compare(warp.z, z) != 0) return false;
        return worldName.equals(warp.worldName);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(z);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + worldName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return worldName + "@" +x + "," + y + "," + z;
    }

    public static Warp parse(JavaPlugin plugin, String str) {
        if (str != null) {
            int idx = str.indexOf('@');
            if (idx > 0) {
                String worldName = str.substring(0, idx);
                if (str.length() > idx + 1) {
                    str = str.substring(idx + 1);
                    String[] parts = str.split(",");
                    if (parts.length == 3) {
                        try {
                            double x = Double.parseDouble(parts[0]);
                            double y = Double.parseDouble(parts[1]);
                            double z = Double.parseDouble(parts[2]);

                            return new Warp(plugin, x, y, z, worldName);
                        } catch(NumberFormatException ignored) {} //will return null
                    }
                }
            }
        }
        return null;
    }
}