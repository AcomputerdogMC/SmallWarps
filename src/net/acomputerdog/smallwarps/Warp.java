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
    private final String name;
    private final long time;

    private final String worldName;
    private World world;

    private Location location;

    public Warp(JavaPlugin plugin, Location l, String owner, String name) {
        this(plugin, l.getX(), l.getY(), l.getZ(), l.getWorld(), owner, name, now());
        this.location = l;
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, String worldName, String owner, String name, long time) {
        this.server = plugin.getServer();
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
        this.owner = owner;
        this.name = name;
        this.time = time;
        server.getPluginManager().registerEvents(this, plugin);
    }

    public Warp(JavaPlugin plugin, double x, double y, double z, World world, String owner, String name, long time) {
        this(plugin, x, y, z, world.getName(), owner, name, time);
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

    public String getName() {
        return name;
    }

    public long getTime() {
        return time;
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
        return name + "," + owner + "," + worldName + "," + x + "," + y + "," + z;
    }

    public String locationToString() {
        return worldName + "@[" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ", " + String.format("%.2f", z) + "]";
    }

    public static long now() {
        return System.currentTimeMillis() / 1000L;
    }

    public static Warp parse(JavaPlugin plugin, String str) {
        if (str != null) {
            if (str.indexOf('=') >= 0) {
                return parseLegacy(plugin, str);
            } else {
                return parseCurrent(plugin, str);
            }
        }
        return null;
    }

    /**
     * Parses a current format warp file.
     * name,owner,world,x,y,z
     */
    private static Warp parseCurrent(JavaPlugin plugin, String str) {
        if (str != null) {
            String[] parts = str.split(",");
            if (parts.length >= 6) {
                String name = parts[0];
                String owner = parts[1];
                String world = parts[2];
                try {
                    double x = Double.parseDouble(parts[3]);
                    double y = Double.parseDouble(parts[4]);
                    double z = Double.parseDouble(parts[5]);
                    long time = now();
                    if (parts.length >= 7) {
                        time = Long.parseLong(parts[6]);
                    }

                    return new Warp(plugin, x, y, z, world, owner, name, time);
                } catch (NumberFormatException ignored) {
                } //will return null
            }
        }
        return null;
    }

    /**
     * Parses a legacy warp in the format name=owner-world@x,y,z
     */
    private static Warp parseLegacy(JavaPlugin plugin, String str) {
        //get name (if it exists)
        int equalsIdx = str.indexOf('=');
        if (equalsIdx > -1 && equalsIdx < str.length() - 1) {
            String name = str.substring(0, equalsIdx);
            str = str.substring(equalsIdx + 1, str.length());

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

                                return new Warp(plugin, x, y, z, worldName, owner, name, now());
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
