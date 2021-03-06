package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Beaconz extends JavaPlugin {
    BeaconPopulator beaconPop;
    Register register;
    World beaconzWorld;
    private static Beaconz plugin;
    static BlockPopulator beaconPopulator;
    private Scorecard scorecard;

    @Override
    public void onEnable() {
        plugin = this;
        // Save the default config from the jar
        saveDefaultConfig();
        loadConfig();

        // Register command(s)
        getCommand("beaconz").setExecutor(new CmdHandler(this));

        //getServer().getPluginManager().registerEvents(new WorldLoader(this), this);


        // Run commands that need to be run 1 tick after start
        getServer().getScheduler().runTask(this, new Runnable() {

            public void run() {

                // Load the scorecard - cannot be done until after the server starts
                scorecard = new Scorecard(plugin);
                // Add teams
                MaterialData teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) 11);
                scorecard.addTeam("Blue", teamBlock);
                teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) 14);
                scorecard.addTeam("Red", teamBlock);

                // Load the beacon register
                register = new Register(plugin);
                register.loadRegister();

                // Load the teams
                scorecard.loadTeamMembers();

                // Create the block populator
                if (beaconPopulator == null) {
                    getBp();
                }
                // Create the world
                World world = getBeaconzWorld();

                // Register the listeners - block break etc.
                BeaconListeners ev = new BeaconListeners(plugin);
                getServer().getPluginManager().registerEvents(ev, plugin);

                // Create the corner beacons if world boarder is active
                if (Settings.borderSize > 0) {
                    // Check corners
                    Set<Point2D> corners = new HashSet<Point2D>();
                    int xMin = Settings.xCenter - (Settings.borderSize /2) + 2;
                    int xMax = Settings.xCenter + (Settings.borderSize /2) - 3;
                    int zMin = Settings.zCenter - (Settings.borderSize /2) + 2;
                    int zMax = Settings.zCenter + (Settings.borderSize /2) - 3;
                    corners.add(new Point2D.Double(xMin,zMin));
                    corners.add(new Point2D.Double(xMin,zMax));
                    corners.add(new Point2D.Double(xMax,zMin));
                    corners.add(new Point2D.Double(xMax,zMax));
                    for (Point2D point : corners) {
                        if (!register.isNearBeacon(point, 5)) {
                            Block b = world.getHighestBlockAt((int)point.getX(), (int)point.getY());
                            while (b.getType().equals(Material.AIR) || b.getType().equals(Material.LEAVES) || b.getType().equals(Material.LEAVES_2)) {          
                                if (b.getY() == 0) {
                                    // Oops, nothing here
                                    break;
                                }
                                b = b.getRelative(BlockFace.DOWN);
                            }
                            if (b.getY() > 3) {
                                // Create a beacon
                                Bukkit.getLogger().info("DEBUG: made beacon at " + b.getLocation());
                                b.setType(Material.BEACON);
                                // Register the beacon
                                register.addBeacon(null, b.getLocation());
                                // Add the capstone
                                b.getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
                                // Create the pyramid
                                b = b.getRelative(BlockFace.DOWN);

                                // All diamond blocks for now
                                b.setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.SOUTH).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.SOUTH_EAST).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.SOUTH_WEST).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.EAST).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.WEST).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.NORTH).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.NORTH_EAST).setType(Material.DIAMOND_BLOCK);
                                b.getRelative(BlockFace.NORTH_WEST).setType(Material.DIAMOND_BLOCK);

              
                            }
                        }
                    }
                }
            }});
    }

    @Override
    public void onDisable()
    {
        if (register != null) {
            register.saveRegister();
        }
        if (scorecard != null) {
            scorecard.saveTeamMembers();
        }
        //getConfig().set("world.distribution", Settings.distribution);
        //saveConfig();
    }

    /**
     * @return the register
     */
    public Register getRegister() {
        return register;
    }

    /**
     * @return the bp
     */
    public BlockPopulator getBp() {
        if (beaconPopulator == null) {
            beaconPopulator = new BeaconPopulator(this);
        }
        return beaconPopulator;
    }

    /**
     * @return the scorecard
     */
    public Scorecard getScorecard() {
        return scorecard;
    }

    /**
     * Loads settings from config.yml
     */
    @SuppressWarnings("deprecation")
    public void loadConfig() {
        Settings.worldName = getConfig().getString("world.name", "beaconz");
        Settings.distribution = getConfig().getDouble("world.distribution", 0.05D);
        Settings.borderSize = getConfig().getInt("world.size",0);
        Settings.xCenter = getConfig().getInt("world.xcenter",0);
        Settings.zCenter = getConfig().getInt("world.zcenter",0);
        Settings.randomSpawn = getConfig().getBoolean("world.randomspawn", true);
        Settings.seedAdjustment = getConfig().getLong("world.seedadjustment", 0);
        Settings.hackCoolDown = getConfig().getInt("world.hackcooldown", 1) * 60000; // Minutes in millis
        ConfigurationSection enemyFieldSection = getConfig().getConfigurationSection("world.enemyfieldeffects");
        // Step through the numbers
        Settings.enemyFieldEffects = new HashMap<Integer, List<PotionEffect>>();
        for (Entry<String, Object> part : enemyFieldSection.getValues(false).entrySet()) {
            if (NumberUtils.isNumber(part.getKey())) {
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<PotionEffect>();
                List<String> effectsList = getConfig().getStringList("world.enemyfieldeffects." + part.getKey());
                for (String effectString : effectsList) {
                    String[] split = effectString.split(":");
                    if (split.length == 3) {
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            if (NumberUtils.isNumber(split[1]) && NumberUtils.isNumber(split[2])) {
                                getLogger().info("DEBUG: adding enemy effect " + type.toString());
                                effects.add(new PotionEffect(type, NumberUtils.toInt(split[1]), NumberUtils.toInt(split[2])));
                            }
                        }

                    }

                }
                Settings.enemyFieldEffects.put(NumberUtils.toInt(part.getKey()), effects);               
            }
        }
        Settings.friendlyFieldEffects = new HashMap<Integer, List<PotionEffect>>();
        ConfigurationSection friendlyFieldSection = getConfig().getConfigurationSection("world.friendlyfieldeffects");
        // Step through the numbers
        for (Entry<String, Object> part : friendlyFieldSection.getValues(false).entrySet()) {
            getLogger().info("DEBUG: Field: " + part.getKey());
            if (NumberUtils.isNumber(part.getKey())) {
                getLogger().info("DEBUG: Field is a number");
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<PotionEffect>();
                List<String> effectsList = getConfig().getStringList("world.friendlyfieldeffects." + part.getKey());
                getLogger().info("DEBUG: Effects list: " + effectsList);
                for (String effectString : effectsList) {
                    String[] split = effectString.split(":");
                    if (split.length == 3) {
                        getLogger().info("DEBUG: Potion found " + split[0]);
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            getLogger().info("DEBUG: Potion is known");
                            if (NumberUtils.isNumber(split[1]) && NumberUtils.isNumber(split[2])) {

                                getLogger().info("DEBUG: adding friendly effect " + type.toString());
                                effects.add(new PotionEffect(type, NumberUtils.toInt(split[1]), NumberUtils.toInt(split[2])));
                            }
                        }

                    }

                }
                Settings.friendlyFieldEffects.put(NumberUtils.toInt(part.getKey()), effects);               
            }
        }
        Settings.overHackEffects = getConfig().getStringList("world.overhackeffects");
        List<String> goodies = getConfig().getStringList("world.enemygoodies");
        Settings.enemyGoodies.clear();
        for (String goodie: goodies) {
            String[] split = goodie.split("=");
            int lastChance = 0;
            if (!Settings.enemyGoodies.isEmpty() ) {
                lastChance = Settings.enemyGoodies.lastKey();
            }
            if (split.length == 2) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[1])) {
                    chance = NumberUtils.toInt(split[1]) + lastChance;
                }
                ItemStack item = getItemFromString(split[0]);
                if (item != null) {
                    Settings.enemyGoodies.put(chance, item);
                }
            }
        }
        // Add the final element - any number above this value will return a null element
        Settings.enemyGoodies.put(Settings.enemyGoodies.lastKey() + 1, null);
        // Debug
        for (Entry<Integer, ItemStack> ent : Settings.enemyGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
        // Team goodies
        goodies = getConfig().getStringList("world.teamgoodies");
        Settings.teamGoodies.clear();
        for (String goodie: goodies) {
            String[] split = goodie.split("=");
            int lastChance = 0;
            if (!Settings.teamGoodies.isEmpty() ) {
                lastChance = Settings.teamGoodies.lastKey();
            }
            if (split.length == 2) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[1])) {
                    chance = NumberUtils.toInt(split[1]) + lastChance;
                }
                ItemStack item = getItemFromString(split[0]);
                if (item != null) {
                    Settings.teamGoodies.put(chance, item);
                }
            }
        }

        // Add the final element - any number above this value will return a null element
        Settings.teamGoodies.put(Settings.teamGoodies.lastKey() + 1, null);
        // Own team
        for (Entry<Integer, ItemStack> ent : Settings.teamGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
        
        // Add initial inventory
        List<String> newbieKit = getConfig().getStringList("world.newbiekit");
        Settings.newbieKit.clear();
        for (String item : newbieKit) {
            Settings.newbieKit.add(getItemFromString(item));
        }
    }

    /**
     * Format is Material:Qty or Material:Data:Qty or Integer:Qty or Integer:Data:Qty
     * @param item
     * @return
     */
    private ItemStack getItemFromString(String goodie) {
        String[] split = goodie.split(":");
        // No durability option - Material/#:Qty
        if (split.length == 2) {
            // Get the qty
            int qty = 0;
            if (NumberUtils.isNumber(split[1])) {
                qty = NumberUtils.toInt(split[1]);
            }
            // Try to get #
            if (NumberUtils.isNumber(split[0])) {
                int itemType = NumberUtils.toInt(split[0]);
                return new ItemStack(itemType, qty);
            } else {
                try {
                    Material itemType = Material.valueOf(split[0]);
                    return new ItemStack(itemType, qty); 
                } catch (Exception e) {
                    getLogger().severe("Could not parse " + split[0] + " skipping...");
                    return null;
                }
            }
        } else if (split.length == 3) {
            // Get the qty
            int qty = 0;
            if (NumberUtils.isNumber(split[2])) {
                qty = NumberUtils.toInt(split[2]);
            }
            // Get the durability
            int durability = 0;
            if (NumberUtils.isNumber(split[1])) {
                durability = NumberUtils.toInt(split[1]);
            }
            // Try to get #
            if (NumberUtils.isNumber(split[0])) {
                int itemType = NumberUtils.toInt(split[0]);
                ItemStack item = new ItemStack(itemType, qty);
                item.setDurability((short)durability);
                return item;
            } else {
                try {
                    Material itemType = Material.valueOf(split[0]);
                    ItemStack item = new ItemStack(itemType, qty);
                    item.setDurability((short)durability);
                    return item;
                } catch (Exception e) {
                    getLogger().severe("Could not parse " + split[0] + " skipping...");
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get the world that Beaconz runs in and if it doesn't exist, make it
     * @return
     */
    public World getBeaconzWorld() {
        // Check to see if the world exists, and if not, make it
        if (beaconzWorld == null) {
            // World doesn't exist, so make it
            getLogger().info("World is '" + Settings.worldName + "'");
            try {
                beaconzWorld = WorldCreator.name(Settings.worldName).type(WorldType.NORMAL).environment(World.Environment.NORMAL).createWorld();
            } catch (Exception e) {
                getLogger().info("Could not make world yet..");
                return null;
            }
            beaconzWorld.getPopulators().add(getBp());
        }
        beaconzWorld.setSpawnLocation(Settings.xCenter, beaconzWorld.getHighestBlockYAt(Settings.xCenter, Settings.zCenter), Settings.zCenter);
        beaconzWorld.getWorldBorder().setCenter(Settings.xCenter, Settings.zCenter);
        if (Settings.borderSize > 0) {
            beaconzWorld.getWorldBorder().setSize(Settings.borderSize);
        }
        return beaconzWorld;
    }
}
