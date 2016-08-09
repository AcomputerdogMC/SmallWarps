package net.acomputerdog.smallwarps;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.logging.Logger;

public class CommandHandler {
    private final PluginSmallWarps plugin;

    public CommandHandler(PluginSmallWarps plugin) {
        this.plugin = plugin;
    }

    public boolean processCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (command.getName().toLowerCase()) {
                case "home":
                    onCmdHome(player);
                    break;
                case "spawn":
                    onCmdSpawn(player);
                    break;
                case "back":
                    onCmdBack(player);
                    break;
                case "tpa":
                    onCmdTpa(player, args);
                    break;
                case "tpaccept":
                    onCmdTpaccept(player);
                    break;
                case "tpdeny":
                    onCmdTpdeny(player);
                    break;
                case "tpcancel":
                    onCmdCancel(player);
                    break;
                case "warp":
                    onCmdWarp(player, args);
                    break;
                case "mkwarp":
                    onCmdMkWarp(player, args);
                    break;
                case "rmwarp":
                    onCmdRmWarp(player, args);
                    break;
                case "lswarp":
                    onCmdLsWarp(player);
                    break;
                case "swreload":
                    onCmdReload(player);
                    break;
                case "tp":
                    onCmdTp(player, args);
                    break;
                default:
                    sendError(sender, "Unknown command!");
                    getLogger().warning("Received unknown command: " + command.getName() + ".  This is likely a bug, please report it!");
            }
        }
        return true;
    }

    private Logger getLogger() {
        return plugin.getLogger();
    }

    private void sendText(CommandSender p, String message) {
        p.sendMessage(ChatColor.AQUA + message);
    }
    
    private void sendList(CommandSender p, String message) {
        p.sendMessage(ChatColor.BLUE + message);
    }

    private void sendError(CommandSender p, String message) {
        p.sendMessage(ChatColor.RED + message);
    }

    private boolean checkPerms(CommandSender p, String perm) {
        if (!p.hasPermission(perm)) {
            sendError(p, "You do not have perimssion.");
            return false;
        }
        return true;
    }

    private boolean checkArgs(CommandSender sender, boolean check, String usage) {
        if (!check) {
            sendError(sender, "Incorrect usage!  Use \"" + usage + "\".");
        }
        return check;
    }

    private void onCmdHome(Player p) {
        if (checkPerms(p, "smallwarps.home")) {
            plugin.returnMap.put(p, p.getLocation());
            Location bed = p.getBedSpawnLocation();
            if (bed == null) {
                sendError(p, "Home bed is missing or obstructed!");
            } else {
                plugin.teleportPlayer(p, bed);
                sendText(p, "Teleported to home bed.");
            }
        }
    }

    private void onCmdSpawn(Player p) {
        if (checkPerms(p, "smallwarps.spawn")) {
            plugin.returnMap.put(p, p.getLocation());
            plugin.teleportPlayer(p, p.getWorld().getSpawnLocation());
            sendText(p, "Teleported to world spawn.");
        } else {
            sendError(p, "You do not have permission.");
        }
    }

    private void onCmdBack(Player p) {
        Location rtn = plugin.returnMap.get(p);
        if (rtn != null) {
            plugin.returnMap.put(p, p.getLocation());
            plugin.teleportPlayer(p, rtn);
            sendText(p, "Returned to previous warp point.");
        } else {
            sendError(p, "You do not have a previous warp point!");
        }
    }

    private void onCmdTpa(Player p, String[] args) {
        if (checkPerms(p, "smallwarps.tpa")) {
            if (checkArgs(p, args.length >= 1, "/tpa <player>")) {
                Player other = plugin.getServer().getPlayer(args[0]);
                if (other != null) {
                    plugin.tpMap.put(other, p);
                    plugin.tpSourceMap.put(p, other);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (plugin.removeTP(p)) {
                                sendText(p, "Your teleport request has expired.");
                            }
                        }
                    }.runTaskLater(plugin, PluginSmallWarps.CLEAR_TP_DELAY);
                    sendText(p, "Teleport request sent.");
                    sendText(other, "Player " + p.getName() + " has requested to teleport to you.  Accept the request with /tpaccept, or deny it with /tpdeny.");
                } else {
                    sendError(p, "That player could not be found!");
                }
            }
        }
    }

    private void onCmdTpaccept(Player p) {
        Player source = plugin.tpMap.get(p);
        if (source != null) {
            sendText(p, "Accepted teleport request from " + source.getName() + ".");
            sendText(source, "Teleporting to " + p.getName() + ".");
            plugin.returnMap.put(source, source.getLocation());
            plugin.teleportPlayer(source, p.getEyeLocation());
            plugin.tpMap.remove(p);
            plugin.tpSourceMap.remove(source);
        } else {
            sendError(p, "No one has requested to teleport to you!");
        }
    }

    private void onCmdTpdeny(Player p) {
        Player source = plugin.tpMap.get(p);
        if (source != null) {
            sendText(p, "Denied teleport request from " + source.getName() + ".");
            plugin.tpMap.remove(p);
            plugin.tpSourceMap.remove(source);
        } else {
            sendError(p, "No one has requested to teleport to you!");
        }
    }

    private void onCmdCancel(Player p) {
        Player target = plugin.tpSourceMap.get(p);
        if (target != null) {
            plugin.removeTP(p);
            sendText(target, p.getName() + " has canceled their request to teleport to you.");
            sendText(p, "Teleport request canceled.");
        } else {
            sendError(p, "You have not requested to TP to anyone!");
        }
    }

    private void onCmdWarp(Player p, String[] args) {
        if (checkPerms(p, "smallwarps.warp.use")) {
            if (checkArgs(p, args.length == 1, "/warp <name>")) {
                Warp warp = plugin.warpMap.get(args[0]);
                if (warp != null) {
                    plugin.returnMap.put(p, p.getLocation());
                    plugin.teleportPlayer(p, warp.getLocation());
                    sendText(p, "Teleported to " + args[0]);
                } else {
                    sendError(p, "That warp does not exist!");
                }
            }
        }
    }

    private void onCmdMkWarp(Player p, String[] args) {
        if (checkPerms(p, "smallwarps.warp.edit")) {
            if (checkArgs(p, args.length >= 1, "/mkwarp <name> [<world> <x> <y> <z>]")) {
                try {
                    String name = args[0];
                    Location loc;
                    if (args.length == 5) {
                        World world = plugin.getServer().getWorld(args[1]);
                        if (world != null) {
                            loc = new Location(world, Double.parseDouble(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
                            //plugin.warpMap.put(name, Warp.create(plugin, loc));
                        } else {
                            sendError(p, "Error creating warp: that world could not be found!");
                            return;
                        }
                    } else {
                        loc = p.getLocation();
                    }
                    plugin.warpMap.put(name, new Warp(plugin, loc));
                    plugin.safeSaveWarps(p);
                    sendText(p, "Warp created successfully.");
                } catch (NumberFormatException e) {
                    sendError(p, "Error creating warp: one or more coordinates is invalid!");
                }
            }
        }
    }

    private void onCmdRmWarp(Player p, String[] args) {
        if (checkPerms(p, "smallwarps.warp.edit")) {
            if (checkArgs(p, args.length >= 1, "/rmwarp <name>")) {
                Warp warp = plugin.warpMap.remove(args[0]);
                if (warp != null) {
                    sendText(p, "Warp removed successfully.");
                    plugin.safeSaveWarps(p);
                } else {
                    sendError(p, "Error removing warp: that warp could not be found!");
                }
            }
        }
    }

    private void onCmdLsWarp(Player p) {
        if (checkPerms(p, "smallwarps.warp.list")) {
            sendText(p, "Defined warp points: ");
            for (Map.Entry<String, Warp> entry : plugin.warpMap.entrySet()) {
                p.sendMessage(ChatColor.AQUA + entry.getKey() + ": " + ChatColor.BLUE + entry.getValue().toString());
            }
        }
    }

    private void onCmdReload(Player p) {
        if (checkPerms(p, "smallwarps.reload")) {
            plugin.reload();
            sendText(p, "Smallwarps has been reloaded.");
        }
    }

    private void onCmdTp(Player p, String[] args) {
        StringBuilder cmd = new StringBuilder((1 + args.length) * 2);
        cmd.append("minecraft:tp");
        for (String str : args) {
            cmd.append(' ');
            cmd.append(str);
        }
        if (p.hasPermission("smallwarps.tp")) { //if player has access to bypass normal /tp permissions
            PermissionAttachment attachment = p.addAttachment(plugin, 1);
            attachment.setPermission("bukkit.command.teleport", true);
            attachment.setPermission("minecraft.command.tp", true);
        }
        Location l = p.getLocation();
        if (p.performCommand(cmd.toString())) { //if player teleported succesfully
            plugin.returnMap.put(p, l);
        }
    }
}
