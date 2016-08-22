package net.acomputerdog.smallwarps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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

    Map<Player, Location> returnMap;
    Map<Player, Player> tpMap; //target player -> source player
    Map<Player, Player> tpSourceMap; //source player -> target player

    Map<String, Warp> warpMap;
    private File warpFile; //format name=owner-world,x,y,z
    private File deletedWarpFile; //format name=owner-world,x,y,z

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
            deletedWarpFile = new File(getDataFolder(), "deleted_warps.bak");
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
        deletedWarpFile = null;
    }

    public void loadWarps() throws IOException {
        if (warpFile.isFile()) {
            try (BufferedReader in = new BufferedReader(new FileReader(warpFile))){
                readWarps(in);
            }
        }
    }

    private void readWarps(BufferedReader in) throws IOException {
        while (in.ready()) {
            String line = in.readLine().trim();
            if (!line.startsWith("#")) { //skip comments
                Warp w = Warp.parse(this, line);
                if (w != null) {
                    warpMap.put(w.getName(), w);
                } else {
                    getLogger().warning("Malformed line: \"" + line + "\"");
                }
            }
        }
    }

    public void saveWarps() throws IOException {
        if (!getDataFolder().isDirectory() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create data directory!");
        }
        try (FileWriter writer = new FileWriter(warpFile)) {
            //Map.Entry<String, Warp> entry : warpMap.entrySet()
            for (Warp warp : warpMap.values()) {
                //writer.write(entry.getKey());
                //writer.write("=");
                //writer.write(entry.getValue().toString());
                writer.write(warp.toString());
                writer.write("\n");
            }
        }
    }

    public void recordDeletedWarp(Warp warp) throws IOException {
        //open in append mode
        try (Writer writer = new FileWriter(deletedWarpFile, true)) {
            writer.write(warp.toString());
            writer.write("\n");
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

    public boolean removeTP(Player source) {
        Player target = tpSourceMap.remove(source); //remove source
        if (target != null) {
            tpMap.remove(target); //remove target
            return true;// return true if it existed
        }
        return false;
    }

    public void teleportPlayer(Player p, Location l) {
        floor(l); //align to a block
        l.add(.5, 0, .5); //center player on the block
        while (l.getBlock().getType().isSolid() && l.getY() <= 255d) {
            l = l.add(0, 1, 0); //if TP location is inside block, find safe place above
        }
        p.setFallDistance(0.0f); //remove fall distance when players TP
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
