/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelupdate;

import java.util.TreeSet;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author patrick
 */
public class UpdateRunner implements Runnable {

    TreeSet<String> messaged = new TreeSet<String>();
    
    @Override
    public void run() {
        Plugin[] plugins = VoxelUpdate.s.getPluginManager().getPlugins();
        
        for (Plugin plugin : plugins) {
            if (VoxelUpdate.um.isUpdateManagedPlugin(plugin.getDescription().getName())) {
                if (VoxelUpdate.um.needsUpdate(plugin.getDescription().getName())) {
                    for (Player p : VoxelUpdate.s.getOnlinePlayers()) {
                        if (VoxelUpdate.admns.contains(p.getName()) && !messaged.contains(plugin.getDescription().getName())) {
                            p.sendMessage("[" + ChatColor.AQUA + "Voxel" + ChatColor.LIGHT_PURPLE + "Update" + ChatColor.WHITE +"]: There is an update available for \"" + ChatColor.GREEN + plugin.getDescription().getName() + ChatColor.WHITE + "\"");
                            p.sendMessage("[" + ChatColor.AQUA + "Voxel" + ChatColor.LIGHT_PURPLE + "Update" + ChatColor.WHITE +"]: Do " + ChatColor.AQUA + "/voxelupdate " + plugin.getDescription().getName() + ChatColor.WHITE + " to update!");
                            messaged.add(plugin.getDescription().getName());
                        }
                    }
                }
            }
        }
    }  
}
