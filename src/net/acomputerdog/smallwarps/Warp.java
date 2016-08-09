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

    //should be the owner name, not UUID
    private final String owner;

    private final String worldName;
    private World world;

    private Location location;

    public Warp(JavaPlugin plugin, Location l, String owner) {
        this(plugin, l.getX(), l.getY(), l.getZ(), l.getWorld(), owner);
        this.location = l;
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, String worldName, String owner) {
        this.server = plugin.getServer();
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.owner = owner;
        server.getPluginManager().registerEvents(this, plugin);
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, World world, String owner) {
        /*
        this.server = plugin.getServer();
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
        this.worldName = world.getName();
        server.getPluginManager().registerEvents(this, plugin);
        */
        this(plugin, x, y, z, world.getName(), owner);
        this.world = world;
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

    public String getOwner() {
        return owner;
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
        return owner + "-" + worldName + "@" + x + "," + y + "," + z;
    }

    public String locationToString() {
        return worldName + "@[" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ", " + String.format("%.2f", z) + "]";
    }

    public static Warp parse(JavaPlugin plugin, String str) {
        if (str != null) {
            String owner = "?";
            //get owner (if it exists)
            int dashIdx = str.indexOf('-');
            if (dashIdx > 0) {
                owner = str.substring(0, dashIdx);
            }
            if (str.length() > dashIdx + 1) {
                str = str.substring(dashIdx + 1);
                //get world
                int atIdx = str.indexOf('@');
                if (atIdx > 0) {
                    String worldName = str.substring(0, atIdx);
                    if (str.length() > atIdx + 1) {
                        str = str.substring(atIdx + 1);
                        //get location
                        String[] parts = str.split(",");
                        if (parts.length >= 3) {
                            try {
                                double x = Double.parseDouble(parts[0]);
                                double y = Double.parseDouble(parts[1]);
                                double z = Double.parseDouble(parts[2]);

                                return new Warp(plugin, x, y, z, worldName, owner);
                            } catch (NumberFormatException ignored) {
                            } //will return null
                        }
                    }
                }
            }
        }
        return null;
    }
}
