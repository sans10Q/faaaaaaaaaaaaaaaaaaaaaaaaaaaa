package com.lanterns;

import java.util.*;

/** Pure game rules. No Minecraft imports so they can be unit-tested anywhere. */
public final class GameLogic {
    private GameLogic() {}

    public static final UUID SKIP = new UUID(0L, 0L);

    public enum Winner { NONE, INNOCENTS, MANHUNTERS }

    public record TallyResult(UUID ejected, Map<UUID, Integer> counts, int skips) {}

    /** Auto-scaling: under 6 players = 1, 6 to 10 players = 2, 11 or more = 3. */
    public static int scaledManhunterCount(int players) {
        if (players > 10) return 3;
        if (players >= 6) return 2;
        return 1;
    }

    /** Manhunter count clamped so they can never start with parity: at least 1, at most (n-1)/2. */
    public static int manhunterCount(int players, int configured) {
        int max = Math.max(1, (players - 1) / 2);
        return Math.max(1, Math.min(configured, max));
    }

    public static Set<UUID> assignManhunters(Collection<UUID> players, int count, Random rng) {
        return assignManhunters(players, count, Set.of(), Set.of(), rng);
    }

    /**
     * Admin-forced roles: players in forcedYes are Manhunters (up to count), players in forcedNo are
     * never Manhunters unless there is literally nobody else. The rest is random.
     */
    public static Set<UUID> assignManhunters(Collection<UUID> players, int count, Set<UUID> forcedYes,
                                             Set<UUID> forcedNo, Random rng) {
        Set<UUID> chosen = new LinkedHashSet<>();
        for (UUID id : players)
            if (forcedYes.contains(id) && !forcedNo.contains(id) && chosen.size() < count) chosen.add(id);
        List<UUID> pool = new ArrayList<>();
        List<UUID> last = new ArrayList<>();
        for (UUID id : players) {
            if (chosen.contains(id)) continue;
            (forcedNo.contains(id) ? last : pool).add(id);
        }
        Collections.shuffle(pool, rng);
        Collections.shuffle(last, rng);
        pool.addAll(last);
        for (UUID id : pool) { if (chosen.size() >= count) break; chosen.add(id); }
        return chosen;
    }

    /** Living Manhunters vs ALL living non-Manhunters (the Lantern counts as innocent). */
    public static Winner checkWin(int livingManhunters, int livingNonManhunters) {
        if (livingManhunters <= 0) return Winner.INNOCENTS;
        if (livingManhunters >= livingNonManhunters) return Winner.MANHUNTERS;
        return Winner.NONE;
    }

    /**
     * Ties and skip-ties produce no elimination. Votes from non-living voters and
     * votes for non-living targets are ignored. Abstaining (no vote) is not a skip.
     */
    public static TallyResult tally(Map<UUID, UUID> votes, Set<UUID> living) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (UUID id : living) counts.put(id, 0);
        int skips = 0;
        for (Map.Entry<UUID, UUID> e : votes.entrySet()) {
            if (!living.contains(e.getKey())) continue;
            UUID t = e.getValue();
            if (SKIP.equals(t)) skips++;
            else if (counts.containsKey(t)) counts.merge(t, 1, Integer::sum);
        }
        int max = 0;
        for (int c : counts.values()) max = Math.max(max, c);
        if (max == 0 || skips >= max) return new TallyResult(null, counts, skips);
        UUID top = null;
        for (Map.Entry<UUID, Integer> e : counts.entrySet()) {
            if (e.getValue() == max) {
                if (top != null) return new TallyResult(null, counts, skips); // tie
                top = e.getKey();
            }
        }
        return new TallyResult(top, counts, skips);
    }

    /** Ring candidate: random living non-Manhunter, never the excluded player. */
    public static Optional<UUID> pickRingCandidate(Collection<UUID> living, Set<UUID> manhunters,
                                                   UUID exclude, Random rng, UUID preferred) {
        if (preferred != null && living.contains(preferred) && !manhunters.contains(preferred) && !preferred.equals(exclude))
            return Optional.of(preferred);
        return pickRingCandidate(living, manhunters, exclude, rng);
    }

    public static Optional<UUID> pickRingCandidate(Collection<UUID> living, Set<UUID> manhunters,
                                                   UUID exclude, Random rng) {
        List<UUID> pool = new ArrayList<>();
        for (UUID id : living) {
            if (!manhunters.contains(id) && !id.equals(exclude)) pool.add(id);
        }
        if (pool.isEmpty()) return Optional.empty();
        return Optional.of(pool.get(rng.nextInt(pool.size())));
    }

    /**
     * Gives every player a set of {@code perPlayer} distinct stations. Sets are pairwise different whenever enough
     * combinations exist, and less-used stations are preferred so every station gets visited.
     */
    public static List<int[]> assignTaskSets(int stations, int perPlayer, int players, Random rng) {
        int n = Math.max(0, Math.min(perPlayer, stations));
        List<int[]> out = new ArrayList<>();
        if (n == 0) { for (int p = 0; p < players; p++) out.add(new int[0]); return out; }
        long combos = 1;
        for (int i = 1; i <= n; i++) combos = combos * (stations - n + i) / i;
        int[] use = new int[stations];
        Set<String> seen = new HashSet<>();
        for (int p = 0; p < players; p++) {
            int[] pick = null;
            for (int attempt = 0; attempt < 80 && pick == null; attempt++) {
                final double[] key = new double[stations];
                Integer[] idx = new Integer[stations];
                for (int i = 0; i < stations; i++) { idx[i] = i; key[i] = use[i] + rng.nextDouble() * 1.5; }
                Arrays.sort(idx, Comparator.comparingDouble(i -> key[i]));
                int[] cand = new int[n];
                for (int k = 0; k < n; k++) cand[k] = idx[k];
                Arrays.sort(cand);
                if (seen.add(Arrays.toString(cand)) || seen.size() >= combos) pick = cand;
            }
            if (pick == null) { pick = new int[n]; for (int k = 0; k < n; k++) pick[k] = k; }
            for (int s : pick) use[s]++;
            out.add(pick);
        }
        return out;
    }

    /** Tasks per innocent by lobby size: <6 = 3, 6-9 = 4, 10-13 = 5, 14+ = 6. */
    public static int scaledTasksPerPlayer(int players) {
        if (players >= 14) return 6;
        if (players >= 10) return 5;
        if (players >= 6) return 4;
        return 3;
    }

    /**
     * "Last stand": the Manhunters need exactly ONE more innocent death to win. Living non-Manhunters
     * (the Sheriff included) outnumber living Manhunters by exactly one.
     */
    public static boolean lastStand(int livingManhunters, int livingNonManhunters) {
        return livingManhunters > 0 && livingNonManhunters == livingManhunters + 1;
    }

    /** How many guardian thresholds are crossed at this progress fraction (never goes down). */
    public static int crossedThresholds(double[] thresholds, int alreadyAwarded, double fraction) {
        int n = alreadyAwarded;
        while (thresholds != null && n < thresholds.length && fraction >= thresholds[n]) n++;
        return n;
    }

    public static double progress(int done, int total) { return total <= 0 ? 0.0 : (double) done / total; }

    /** Guardian Light may only trigger when charged AND not locked from a previous use. */
    public static boolean guardianReady(int charge, int lockMeetings) { return charge > 0 && lockMeetings <= 0; }

    /** After using Guardian Light it is locked until this many meetings have ENDED. */
    public static final int GUARDIAN_LOCK_MEETINGS = 2;

    public static int afterMeeting(int lockMeetings) { return Math.max(0, lockMeetings - 1); }

    public static long remainingSeconds(long readyAtTick, long nowTick) {
        long left = readyAtTick - nowTick;
        return left <= 0 ? 0 : (left + 19) / 20;
    }
}
