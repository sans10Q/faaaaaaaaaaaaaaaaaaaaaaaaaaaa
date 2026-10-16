package com.lanterns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;

/** JSON config at config/lanterns.json. Single-use limits (Verdict, self-shield) are NOT configurable. */
public class Config {
    public int configVersion = 0; // internal: used to migrate old config files
    public boolean autoScaleManhunters = true; // 1 (<6 players), 2 (6-10), 3 (11+); false = use "manhunters" below
    public int manhunters = 1;                 // only used when autoScaleManhunters is false
    public int minPlayers = 6;
    public int killCooldownSec = 60;
    public int initialKillCooldownSec = 30;
    public int postMeetingKillCooldownSec = 10;
    public double killRange = 3.5;
    public int blackoutDurationSec = 10;
    public int blackoutCooldownSec = 90;
    public int residualCooldownSec = 45;
    public int discussionSec = 45;
    public int votingSec = 30;
    public int emergencyMeetingsPerPlayer = 1;
    public int meetingGraceSec = 20;
    public int emergencyCooldownSec = 30;
    public double reportRange = 8.0;
    public int briefingSec = 8;                   // everyone gathers at the meeting point before play starts
    public boolean briefingGather = true;
    public ButtonPoint meetingButton = new ButtonPoint();
    public boolean autoScaleTasks = true;         // tasks per innocent: <6 players 3, 6-9 = 4, 10-13 = 5, 14+ = 6
    public int tasksPerPlayer = 3;                // only used when autoScaleTasks is false
    public boolean sheriffEnabled = true;
    public boolean sheriffGunEnabled = true;      // last stand: one-bullet revolver when Manhunters are 1 death from winning
    public int sheriffMinPlayers = 7;
    public double killAlertRadius = 16.0;         // nearby players hear a struggle (direction only). 0 = off
    public boolean tasksCompleteWins = true;     // innocents win when every (remaining) task is done
    public double[] guardianThresholds = {0.25, 0.50, 0.75}; // team task progress that charges Guardian Light
    public java.util.List<TaskStation> tasks = new java.util.ArrayList<>();
    public int shapeshiftDurationSec = 20;
    public int shapeshiftCooldownSec = 60;
    public boolean shapeshiftEvidence = true;      // transformation shimmer + sound visible to everyone nearby
    public boolean manhunterSeesDisguise = true;   // true: Manhunters see disguises too (allies are still marked by the red outline); false: allies see through them
    public boolean manhunterOutline = true; // Manhunters see allies with a red outline (only they can see it)
    public double meetingFreeRadius = 3.0; // players may shuffle this far from the meeting point before being pulled back
    public boolean revealRoleOnDeath = false;
    public boolean revealVotesPublicly = false;
    public boolean returnPlayersAfterMeeting = true;
    public MeetingPoint meetingPoint = new MeetingPoint();

    public static class ButtonPoint {
        public boolean set = false;
        public String dimension = "minecraft:overworld";
        public int x, y, z;
    }

    public static class TaskStation {
        public String dimension = "minecraft:overworld";
        public int x, y, z;
        public String type = "scan"; // scan | puzzle | calibrate

        public boolean matches(String dim, int bx, int by, int bz) {
            return dimension.equals(dim) && x == bx && y == by && z == bz;
        }

        public RegistryKey<World> key() {
            return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimension));
        }
    }

    public static class MeetingPoint {
        public boolean set = false;
        public String dimension = "minecraft:overworld";
        public double x, y, z;
        public float yaw, pitch;

        public RegistryKey<World> key() {
            return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimension));
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public static Config get() { return instance; }

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("lanterns.json"); }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Config c = GSON.fromJson(Files.readString(p), Config.class);
                if (c != null) instance = c;
                if (instance.meetingPoint == null) instance.meetingPoint = new MeetingPoint();
            }
        } catch (Exception e) {
            LanternsMod.LOGGER.error("Failed to read config, using defaults", e);
        }
        if (instance.configVersion < 2) { // v1.5 defaults: 1 Manhunter, 45s discussion
            instance.manhunters = 1;
            instance.discussionSec = 45;
            instance.configVersion = 2;
        }
        if (instance.configVersion < 3) { // v1.17: disguises are visible to everyone, including the shifter
            instance.manhunterSeesDisguise = true;
            instance.configVersion = 3;
        }
        if (instance.tasks == null) instance.tasks = new java.util.ArrayList<>();
        if (instance.guardianThresholds == null) instance.guardianThresholds = new double[]{0.25, 0.50, 0.75};
        save();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            LanternsMod.LOGGER.error("Failed to save config", e);
        }
    }
}
