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
import java.util.HashMap;
import java.util.Map;

public class PluginSmallWarps extends JavaPlugin implements Listener {
    public static final long CLEAR_TP_DELAY = 20 * 60; //1 minute in ticks

    private static final String WARPS_FILE_VERSION = "1";

    Map<Player, Location> returnMap;
    Map<Player, Player> tpMap; //target player -> source player
    Map<Player, Player> tpSourceMap; //source player -> target player

    Map<String, Warp> warpMap;
    private File warpFile; //format name=world,x,y,z

    private CommandHandler commandHandler;

    //don't reset in onEnable or onDisable
    private boolean reloading = false;

    @Override
    public void onEnable() {
        try {
            returnMap = new HashMap<>();
            tpMap = new HashMap<>();
            tpSourceMap = new HashMap<>();
            warpMap = new HashMap<>();
            warpFile = new File(getDataFolder(), "warps.cfg");
            loadWarps();
            if (!reloading) {
                getServer().getPluginManager().registerEvents(this, this);
            }
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
            try (BufferedReader in = new BufferedReader(new FileReader(warpFile))){
                String ver = getVersion(in);
                if (!WARPS_FILE_VERSION.equals(ver)) {
                    convertWarps(ver, in);
                } else {
                    readWarps(in);
                }
            }
        }
    }

    private String getVersion(BufferedReader in) throws IOException {
        if (in.ready()) {
            in.mark(100);
            String line = in.readLine();
            //starts with #version= and has more characters
            int idx = line.indexOf('=');
            String ver = null;
            if (line.startsWith("#version=") && line.length() > idx + 1) {
                ver = line.substring(idx + 1);
            }
            in.reset();
            return ver;
        }
        return null;
    }

    private void convertWarps(String version, BufferedReader in) throws IOException {
        getLogger().info("Converting warps from version " + version + " to version " + WARPS_FILE_VERSION);
        if (version == null) {
            while (in.ready()) {
                String line = in.readLine().trim();
                if (!line.startsWith("#")) { //skip comments
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        String name = parts[0];
                        String[] locParts = parts[1].split(",");
                        String world = locParts[0];
                        locParts[0] = getServer().getWorlds().get(0).getName(); //hack to fix multiworld bug
                        Location l = parseLoc(locParts);
                        if (l != null) {
                            Warp w = new Warp(this, l.getX(), l.getY(), l.getZ(), world);
                            warpMap.put(name, w);
                        } else {
                            getLogger().warning("Malformed line: \"" + line + "\"");
                        }
                    } else {
                        getLogger().warning("Malformed line: \"" + line + "\"");
                    }
                }
            }
            in.close();
            saveWarps();
        } else if ("1".equals(version)) {
            getLogger().warning("Attempted to convert a supported warps file format, this is a bug!");
            readWarps(in);
        } else {
            getLogger().severe("Unsupported warps file version!  Did you downgrade?");
        }
    }

    private void readWarps(BufferedReader in) throws IOException {
        while (in.ready()) {
            String line = in.readLine().trim();
            if (!line.startsWith("#")) { //skip comments
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    //String[] locParts = parts[1].split(",");
                    String name = parts[0];
                    Warp w = Warp.parse(this, parts[1]);
                    //Location l = parseLoc(locParts);
                    if (w != null) {
                        warpMap.put(name, w);
                    } else {
                        getLogger().warning("Malformed line: \"" + line + "\"");
                    }
                } else {
                    getLogger().warning("Malformed line: \"" + line + "\"");
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
            writer.write("#version=");
            writer.write(WARPS_FILE_VERSION);
            writer.write("\n");
            for (Map.Entry<String, Warp> entry : warpMap.entrySet()) {
                writer.write(entry.getKey());
                writer.write("=");
                writer.write(entry.getValue().toString());
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

    /*
    public String formatLocation(Warp w) {
        return l.getWorld().getName() + "@" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
    */

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

    public void reload() {
        reloading = true;
        onDisable();
        onEnable();
        reloading = false;
    }
}
