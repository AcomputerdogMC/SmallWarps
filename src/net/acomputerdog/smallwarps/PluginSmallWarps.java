package net.acomputerdog.smallwarps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public class PluginSmallWarps extends JavaPlugin implements Listener {
    public static final long CLEAR_TP_DELAY = 20 * 60; //1 minute in ticks

    Map<Player, Location> returnMap;
    Map<Player, Player> tpMap; //target player -> source player
    Map<Player, Player> tpSourceMap; //source player -> target player

    Map<String, Location> warpMap;
    private File warpFile; //format name=world,x,y,z

    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        try {
            returnMap = new HashMap<>();
            tpMap = new HashMap<>();
            tpSourceMap = new HashMap<>();
            warpMap = new HashMap<>();
            warpFile = new File(getDataFolder(), "warps.cfg");
            loadWarps();
            getServer().getPluginManager().registerEvents(this, this);
            commandHandler = new CommandHandler(this);
        } catch (Exception e) {
            getLogger().severe("Exception starting up!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        returnMap = null;
        tpMap = null;
        tpSourceMap = null;
        warpMap = null;
        warpFile = null;
        commandHandler = null;
    }

    public void loadWarps() throws IOException {
        if (warpFile.isFile()) {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(warpFile));
                while (in.ready()) {
                    String line = in.readLine().trim();
                    if (!line.startsWith("#")) { //skip comments
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String[] locParts = parts[1].split(",");
                                String name = parts[0];
                                Location l = parseLoc(locParts);
                                if (l != null) {
                                    warpMap.put(name, l);
                                } else {
                                    getLogger().warning("Malformed line: \"" + line + "\"");
                                }
                        } else {
                            getLogger().warning("Malformed line: \"" + line + "\"");
                        }
                    }
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    public void safeSaveWarps(CommandSender sender) {
        try {
            saveWarps();
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "An error occurred recording warp points, please report this to an server operator!");
            getLogger().warning("Exception saving warps!");
            e.printStackTrace();
        }
    }

    public void saveWarps() throws IOException {
        if (!getDataFolder().isDirectory()) {
            getDataFolder().mkdirs();
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(warpFile);
            for (Map.Entry<String, Location> entry : warpMap.entrySet()) {
                writer.write(entry.getKey());
                writer.write("=");
                writer.write(entry.getValue().getWorld().getName());
                writer.write(",");
                writer.write(String.valueOf(entry.getValue().getX()));
                writer.write(",");
                writer.write(String.valueOf(entry.getValue().getY()));
                writer.write(",");
                writer.write(String.valueOf(entry.getValue().getZ()));
                writer.write("\n");
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandHandler.processCommand(sender, command, label, args);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        returnMap.put(player, player.getLocation());
        player.sendMessage(ChatColor.YELLOW + "You have died!  Use /back to return to your death point.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogout(PlayerQuitEvent e) {
        returnMap.remove(e.getPlayer()); //clear return map
        tpMap.remove(e.getPlayer()); //clear from tp targets
        Player player = tpSourceMap.remove(e.getPlayer()); //clear from tp reverse map
        if (player != null) {
            tpMap.remove(player); //clear from tp requests
        }
    }

    private Location parseLoc(String[] parts) {
        if (parts.length == 4) {
            try {
                World world = getServer().getWorld(parts[0]);
                if (world != null) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    return new Location(world, x, y, z);
                } else {
                    return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public String formatLocation(Location l) {
        return l.getWorld().getName() + "@" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    public boolean removeTP(Player source) {
        Player target = tpSourceMap.remove(source); //remove source
        if (target != null) {
            tpMap.remove(target); //remove target
            return true;// return true if it existed
        }
        return false;
    }

    public void teleportPlayer(Player p, Location l) {
        floor(l);
        l.add(.5, 0, .5);
        while (l.getBlock().getType().isSolid() && l.getY() <= 255f) {
            l = l.add(0, 1, 0);
        }
        p.teleport(l, PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    private void floor(Location l) {
        l.setX(Math.floor(l.getX()));
        l.setY(Math.floor(l.getY()));
        l.setZ(Math.floor(l.getZ()));
    }
}
