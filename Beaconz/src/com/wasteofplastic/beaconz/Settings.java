package com.wasteofplastic.beaconz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Settings {
    /**
     * World name. Currently only one world play is supported
     */
    public static String worldName;
    /**
     * The density of the random distribution of beacons in the world
     * Number should be between 0 and 1
     */
    public static Double distribution;
    /**
     * The x central position of the world. Will also be the default (not team) spawn point
     */
    public static int xCenter;
    /**
     * The z central position of the world. Will also be the default (not team) spawn point
     */
    public static int zCenter;
    /**
     * World size. Sets world border. If zero, size is unlimited.
     */
    public static int borderSize;
    /**
     * If true, player's will spawn randomly within the world border
     */
    public static boolean randomSpawn;
    /**
     * Adjusts random number seed
     */
    public static long seedAdjustment;
    /**
     * Time in seconds that players must wait between trying to hack a beacon
     */
    public static long hackCoolDown;
    /**
     * Effect if you hack during the cooldown period. Format is Effect:Amplifier
     * Options are: Blindness, Confusion, Harm, Hunger, Slow, Slow_digging, Weakness, Wither
     * Amplifier is a number, .e.g, 0, 1
     */
    public static List<String> overHackEffects;
    /**
     *   Rewards from hacking your own team beacon
     *   Format is "Id#/Material:[Durability/Qty]:Qty:%Chance"
     */
    public static TreeMap<Integer,ItemStack> teamGoodies = new TreeMap<Integer,ItemStack>();
    /**
     *   Rewards from hacking enemy team beacon
     *   Format is "Id#/Material:[Durability/Qty]:Qty:%Chance"
     */
    public static TreeMap<Integer,ItemStack> enemyGoodies = new TreeMap<Integer,ItemStack>();  
    /**
     * Effects from going into enemy triangle fields. Effects are cumulative
     * Integer is the level of triangle overlap. 
     */
    public static HashMap<Integer, List<PotionEffect>> enemyFieldEffects;
    /**
     * Effects from going into friendly triangle fields. Effects are cumulative
     * Integer is the level of triangle overlap. 
     */
    public static HashMap<Integer, List<PotionEffect>> friendlyFieldEffects;
    
    /**
     * What newbies get when they join the game
     * Format is "Id#/Material:[Durability/Qty]:Qty"
     */
    public static List<ItemStack> newbieKit = new ArrayList<ItemStack>();
}
