/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thevoxelbox.voxelupdate;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author psanker
 */
public class UpdateManager {
    protected HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
    protected long lastDataFetch = 0;
    protected boolean couldNotFetch = false;

    public UpdateManager() {
        readData();
    }
    
    public boolean isUpdateManagedPlugin(String plugin) {
        if (map.containsKey(plugin))
            return true;
        
        return false;
    }

    public boolean needsUpdate(String plugin) {
        if (System.currentTimeMillis() - lastDataFetch > 60000)
                readData();

        if (VoxelUpdate.s.getPluginManager().isPluginEnabled(plugin)) {
            String plvers = VoxelUpdate.s.getPluginManager().getPlugin(plugin).getDescription().getVersion();
            
            if (get(plugin, "version") != null) {
                String version = get(plugin, "version");
                
                // @BEGIN version parse
                if (version.contains(".")) {
                    String[] nums = version.split("\\.");
                    String[] plnums = plvers.split("\\.");
                    
                    if ((nums.length == 3 && plnums.length == 3) || (nums.length == 2 && plnums.length == 2)) {
                        for (int i = 0; i < nums.length; i++) {
                            try {
                                Integer vrs = Integer.parseInt(nums[i]);
                                Integer plvrs = Integer.parseInt(plnums[i]);
                                
                                if (plvrs < vrs)
                                    return true;
                                else {
                                    if (i == nums.length - 1)
                                        return false;
                                    
                                    continue;
                                }
                                
                            } catch (NumberFormatException e) {
                                VoxelUpdate.log.warning("[VoxelUpdate] Could not compare versions due to a letter character in version for plugin \"" + plugin + "\"");
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public List<String> getListofPlugins() {
        List<String> l = new LinkedList<String>();
        l.clear();

        for (Map.Entry<String, HashMap<String, String>> e : map.entrySet()) {
            if (!l.contains(e.getKey())) {
                l.add(e.getKey());
            }
        }

        return l;
    }

    public String get(String id, String key) {
        if (map.containsKey(id)) {
            if (map.get(id).containsKey(key)) {
                return map.get(id).get(key);
            }
        }

        return null;
    }

    public boolean isInstalled(String plugin) {
        boolean installtemp = false;

        if (new File("plugins/" + plugin + ".jar").exists()) {
            installtemp = true;
        }

        return installtemp;
    }

    public boolean doDownload(String plugin) {
        boolean downloaded = false;

        try {
            File dl = new File("plugins/VoxelUpdate/Downloads/" + plugin + ".jar");

            if (!dl.getParentFile().isDirectory()) {
                dl.getParentFile().mkdirs();
            }

            if (!dl.exists()) {
                dl.createNewFile();
            }

            if (get(plugin, "url") == null) {
                return false;
            }

            BufferedInputStream bi = new BufferedInputStream(new URL(get(plugin, "url")).openStream());
            FileOutputStream fo = new FileOutputStream(dl);
            BufferedOutputStream bo = new BufferedOutputStream(fo, 1024);

            byte[] b = new byte[1024];
            int i = 0;

            while ((i = bi.read(b, 0, 1024)) >= 0) {
                bo.write(b, 0, i);
            }

            bo.close();
            bi.close();

            if (VoxelUpdate.autoUpdate) {
                File dupe = new File("plugins/" + dl.getName());
                
                FileChannel ic = new FileInputStream(dl).getChannel();
                FileChannel oc = new FileOutputStream(dupe).getChannel();
                ic.transferTo(0, ic.size(), oc);
                ic.close();
                oc.close();

                VoxelUpdate.s.getPluginManager().disablePlugin(VoxelUpdate.s.getPluginManager().getPlugin(plugin));

                try {
                    VoxelUpdate.s.getPluginManager().enablePlugin(VoxelUpdate.s.getPluginManager().loadPlugin(dl));
                } catch (Exception e) {
                    VoxelUpdate.log.severe("[VoxelUpdate] Could not reload plugin \"" + plugin + "\"");
                }
            }

            downloaded = true;
        } catch (MalformedURLException e) {
            VoxelUpdate.log.severe("[VoxelUpdate] Incorrectly formatted URL for download of \"" + plugin + "\"");
            return downloaded;
        } catch (FileNotFoundException e) {
            VoxelUpdate.log.severe("[VoxelUpdate] Could not save data to VoxelUpdate/Downloads");
            e.printStackTrace();
            return downloaded;
        } catch (IOException e) {
            VoxelUpdate.log.severe("[VoxelUpdate] Could not assign data to BIS");
            e.printStackTrace();
            return downloaded;
        }

        return downloaded;
    }

    private void readData() {
        if (couldNotFetch)
            return;
        
        String test = VoxelUpdate.url;
        test = test.toLowerCase();
        
        if (!test.endsWith(".xml")) {
            return;
        }

        try {
            File xml = new File("plugins/VoxelUpdate/temp.xml");

            if (!xml.exists()) {
                xml.createNewFile();
            }

            BufferedInputStream bi = new BufferedInputStream(new URL(VoxelUpdate.url).openStream());
            FileOutputStream fo = new FileOutputStream(xml);
            BufferedOutputStream bo = new BufferedOutputStream(fo, 1024);
            byte[] b = new byte[1024];
            int i = 0;

            while ((i = bi.read(b, 0, 1024)) >= 0) {
                bo.write(b, 0, i);
            }

            bo.close();
            bi.close();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xml);
            doc.getDocumentElement().normalize();

            Element base = doc.getDocumentElement();
            NodeList pluginList = doc.getElementsByTagName("plugin");
            xml.delete();

            for (i = 0; i < pluginList.getLength(); i++) {
                Node n = pluginList.item(i);
                String name = null;
                HashMap<String, String> _map = new HashMap<String, String>();

                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) n;
                    String version = "";
                    String url = "";
                    String authors = "";
                    String description = "";
                    
                    try {

                        if (getTagValue("name", e) != null) {
                            name = getTagValue("name", e);
                        } else {
                            return;
                        }
                        if (getTagValue("version", e) != null) {
                            version = getTagValue("version", e);
                        }
                        if (getTagValue("url", e) != null) {
                            url = getTagValue("url", e);
                        }
                        if (getTagValue("authors", e) != null) {
                            authors = getTagValue("authors", e);
                        }
                        if (getTagValue("description", e) != null) {
                            description = getTagValue("description", e);
                        }
                    
                    } catch (NullPointerException ex) {
                        continue;
                    }

                    if (!"".equals(version)) {
                        _map.put("version", version);
                    }
                    if (!"".equals(url)) {
                        _map.put("url", url);
                    }
                    if (!"".equals(authors)) {
                        _map.put("authors", authors);
                    }
                    if (!"".equals(description)) {
                        _map.put("description", description);
                    }

                    map.put(name, _map);
                }
            }
            
            lastDataFetch = System.currentTimeMillis();

        } catch (MalformedURLException e) {
            VoxelUpdate.log.severe("[VoxelUpdate] Incorrectly formatted URL to data file in preferences");
        } catch (IOException e) {
            VoxelUpdate.log.severe("[VoxelUpdate] Could not assign data to BIS");
            VoxelUpdate.log.warning("[VoxelUpdate] Probably because the link is broken or the server host is down");
            VoxelUpdate.log.warning("[VoxelUpdate] Also, check the properties file... It might be empty. If so, grab the default configuration (from TVB's wiki) after you stop your server, paste it into the .properties, and restart");
            VoxelUpdate.log.warning("[VoxelUpdate] ... Turning off data search until a reload...");
            couldNotFetch = true;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private static String getTagValue(String sTag, Element eElement) {
        try {
            NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

            Node nValue = (Node) nlList.item(0);

            return nValue.getNodeValue();
        } catch (NullPointerException ex) {
            return null;
        }
    }
}