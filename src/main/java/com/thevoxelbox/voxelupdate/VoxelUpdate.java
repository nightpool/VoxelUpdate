package com.thevoxelbox.voxelupdate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author psanker & giltwist
 */
public class VoxelUpdate extends JavaPlugin {
    public static Server s;
    public static String url = "http://dl.dropbox.com/u/53561471/VoxelUpdate.xml";
    public static boolean autoUpdate = false;
    public static boolean searchForUpdates = true;
    public static boolean announceBetaBuilds = true;
    public File preferences = new File("plugins/VoxelUpdate/VoxelUpdate.properties");
    public static final Logger log = Logger.getLogger("Minecraft");
    public static List<String> admns = new LinkedList<String>();
    public static UpdateManager updateManager = new UpdateManager();

    @Override
    public void onDisable() {
        s.getScheduler().cancelTasks(this);

        File dldir = new File("plugins/VoxelUpdate/Downloads/");

        if (dldir.isDirectory() && dldir.listFiles().length != 0) {
            File[] list = dldir.listFiles();

            for (File dl : list) {
                if (!dl.exists()) {
                    continue;
                }

                File dupe = new File("plugins/" + dl.getName());

                try {
                    FileChannel ic = new FileInputStream(dl).getChannel();
                    FileChannel oc = new FileOutputStream(dupe).getChannel();
                    ic.transferTo(0, ic.size(), oc);
                    ic.close();
                    oc.close();
                } catch (FileNotFoundException e) {
                    log.log(Level.SEVERE, "[VoxelUpdate] Could not find data in VoxelUpdate/Downloads", e);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "[VoxelUpdate] Error copying files from VoxelUpdate/Downloads", e);
                }
            }

            dldir.delete();
        }
    }

    @Override
    public void onEnable() {
        s = this.getServer();
        readPreferences();
        readAdmins();
        log.info("[VoxelUpdate] VoxelUpdate version " + getDescription().getVersion() + " loaded");
        s.broadcastMessage(ChatColor.WHITE + "[" + ChatColor.AQUA + "Voxel" + ChatColor.LIGHT_PURPLE + "Update" + ChatColor.WHITE + "] - Be sure to check out " + ChatColor.RED + "www.VoxelWiki.com" + ChatColor.WHITE + " for documentation for all VoxelPlugins.");

        if (searchForUpdates) {
            s.getScheduler().scheduleSyncRepeatingTask(this, new UpdateRunner(), 1200L, 10L);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String[] trimmedArgs = args;
        List<String> voxelplugins = updateManager.getListofPlugins();

        String comm = command.getName().toLowerCase();
        if (admns.contains(sender.getName()) || sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) {
            if (comm.equalsIgnoreCase("voxelplugins")) {
                sender.sendMessage(ChatColor.WHITE + "[" + ChatColor.AQUA + "Voxel" + ChatColor.LIGHT_PURPLE + "Update" + ChatColor.WHITE + "] - Plugin List");
                for (String plugin : voxelplugins) {
                    boolean isEnabled = s.getPluginManager().isPluginEnabled(plugin);
                    boolean isInstalled = updateManager.isInstalled(plugin);

                    if (isInstalled) {
                        if (isEnabled) {

                            if (updateManager.needsUpdate(plugin)) {
                                sender.sendMessage("* " + plugin + ": " + ((updateManager.isBeta(plugin)) ? ("\u00a76Beta available \u00a7c[WARNING: Potentially buggy]") : ("\u00a76Update available")));
                            } else {
                                sender.sendMessage("* " + plugin + ": " + ChatColor.GREEN + "Installed");
                            }

                        } else {
                            sender.sendMessage("* " + plugin + ": " + ChatColor.GRAY + "Disabled");
                        }
                    } else {
                        sender.sendMessage("* " + plugin + ": " + ChatColor.RED + "Available");
                    }
                }
                return true;
            } else if (comm.equalsIgnoreCase("voxelinstall")) {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.GOLD + "Use: /voxelinstall <plugin>");
                } else {
                    if (!updateManager.getListofPlugins().contains(trimmedArgs[0])) {
                        sender.sendMessage(ChatColor.RED + "Could not find plugin \"" + trimmedArgs[0] + "\"");
                        return true;
                    }

                    if (updateManager.doDownload(trimmedArgs[0])) {
                        sender.sendMessage(ChatColor.WHITE + "[" + ChatColor.AQUA + "Voxel" + ChatColor.LIGHT_PURPLE + "Update" + ChatColor.WHITE + "] Successfully downloaded \"" + ChatColor.GREEN + trimmedArgs[0] + ChatColor.WHITE + "\"");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Download failed. See server logs for details.");
                    }
                }
                return true;
            } else if (comm.equalsIgnoreCase("voxelupdate")) {
                if (args.length == 0) {
                    sender.sendMessage(ChatColor.GOLD + "Use: /voxelupdate <plugin>");
                } else {
                    if (!updateManager.getListofPlugins().contains(trimmedArgs[0])) {
                        sender.sendMessage(ChatColor.RED + "Could not find plugin \"" + trimmedArgs[0] + "\"");
                        return true;
                    }

                    if (updateManager.needsUpdate(trimmedArgs[0])) {
                        if (updateManager.doDownload(trimmedArgs[0])) {
                            sender.sendMessage(ChatColor.WHITE + "[" + ChatColor.AQUA + "Voxel" + ChatColor.LIGHT_PURPLE + "Update" + ChatColor.WHITE + "] Successfully downloaded \"" + ChatColor.GREEN + trimmedArgs[0] + ChatColor.WHITE + "\"");
                        } else {
                            sender.sendMessage(ChatColor.RED + "Download failed. See server logs for details.");
                        }
                    }
                }
                return true;
            } else if (comm.equalsIgnoreCase("voxelinfo")) {
                for (String tempplugin : voxelplugins) {
                    sender.sendMessage(tempplugin + ": \u00a7a" + updateManager.get(tempplugin, "description"));
                }
                return true;
            }
        }
        return false;
    }

    public void readAdmins() { //Shamelessly stolen from VoxelGuest to maintain inter-plugin compatibility
        try {
            File f = new File("plugins/admns.txt");
            if (!f.exists()) {
                f.createNewFile();
                log.info("[VoxelUpdate] admns.txt was missing and thus created.");
            }
            Scanner snr = new Scanner(f);
            while (snr.hasNext()) {
                String st = snr.nextLine();
                admns.add(st);
            }
            snr.close();
        } catch (Exception e) {
            log.warning("[VoxelUpdate] Error while loading admns.txt");
        }
    }

    public void readPreferences() {
        Properties props = new Properties();
        FileInputStream fi = null;

        try {
            if (!preferences.getParentFile().isDirectory()) {
                preferences.getParentFile().mkdirs();
            }

            if (!preferences.exists()) {
                writePreferences();
                return;
            }

            fi = new FileInputStream(preferences);
            props.load(fi);
            HashMap<String, Object> map = new HashMap<String, Object>();

            for (Map.Entry<Object, Object> e : props.entrySet()) {
                map.put(e.getKey().toString(), e.getValue());
            }

            if (map != null) {
                if (map.containsKey("url")) {
                    url = map.get("url").toString();
                }
                if (map.containsKey("auto-update")) {
                    autoUpdate = Boolean.parseBoolean(map.get("auto-update").toString());
                }
                if (map.containsKey("search-for-updates")) {
                    searchForUpdates = Boolean.parseBoolean(map.get("search-for-updates").toString());
                }
                if (map.containsKey("announce-beta-builds")) {
                    announceBetaBuilds = Boolean.parseBoolean(map.get("announce-beta-builds").toString());
                }
            }

        } catch (IOException e) {
            log.log(Level.WARNING, "[VoxelUpdate] Could not read preferences", e);
        } finally {
            try {
                if (fi != null) {
                    fi.close();
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "[VoxelUpdate] Fatal error while attempting to close an FIS", e);
            }
        }
    }

    public void writePreferences() {
        Properties props = new Properties();
        FileOutputStream fo = null;

        try {
            if (!preferences.getParentFile().isDirectory()) {
                preferences.getParentFile().mkdirs();
            }

            if (!preferences.exists()) {
                preferences.createNewFile();
            }

            fo = new FileOutputStream(preferences);

            props.setProperty("url", url);
            props.setProperty("auto-update", ((Boolean) autoUpdate).toString());
            props.setProperty("search-for-updates", ((Boolean) searchForUpdates).toString());
            props.setProperty("announce-beta-builds", ((Boolean) announceBetaBuilds).toString());
            props.store(fo, null);
        } catch (IOException e) {
            log.log(Level.WARNING, "[VoxelUpdate] Could not write preferences", e);
        } finally {
            try {
                if (fo != null) {
                    fo.close();
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "[VoxelUpdate] Fatal error while attempting to close an FOS", e);
            }
        }
    }
}