import com.lanterns.GameLogic;
import java.util.*;

/** Dependency-free mirror of GameLogicTest. Run: see README "Run the logic checks without Gradle". */
public class LogicCheck {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) { if (ok) pass++; else fail++; System.out.println((ok ? "PASS " : "FAIL ") + name); }
    static List<UUID> ids(int n) { List<UUID> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(UUID.randomUUID()); return l; }

    public static void main(String[] a) {
        check("manhunterCount 6->2", GameLogic.manhunterCount(6, 2) == 2);
        check("manhunterCount 12->2", GameLogic.manhunterCount(12, 2) == 2);
        check("manhunterCount 3->1", GameLogic.manhunterCount(3, 2) == 1);
        check("manhunterCount 4,5->1", GameLogic.manhunterCount(4, 5) == 1);
        check("manhunterCount min 1", GameLogic.manhunterCount(10, 0) == 1);

        boolean rolesOk = true;
        for (int i = 0; i < 500; i++) { List<UUID> p = ids(10); Set<UUID> m = GameLogic.assignManhunters(p, 2, new Random(i)); rolesOk &= m.size() == 2 && p.containsAll(m); }
        check("auto-scale: 3,5 players -> 1", GameLogic.scaledManhunterCount(3) == 1 && GameLogic.scaledManhunterCount(5) == 1);
        check("auto-scale: 6 and 10 players -> 2", GameLogic.scaledManhunterCount(6) == 2 && GameLogic.scaledManhunterCount(10) == 2);
        check("auto-scale: 11 and 16 players -> 3", GameLogic.scaledManhunterCount(11) == 3 && GameLogic.scaledManhunterCount(16) == 3);
        boolean neverParity = true;
        for (int n = 3; n <= 20; n++) neverParity &= GameLogic.manhunterCount(n, GameLogic.scaledManhunterCount(n)) * 2 < n;
        check("auto-scale never starts at Manhunter parity (3-20 players)", neverParity);

        check("role assignment exact count (500 trials)", rolesOk);

        check("win: 0 manhunters -> innocents", GameLogic.checkWin(0, 5) == GameLogic.Winner.INNOCENTS);
        check("win: 2v2 -> manhunters", GameLogic.checkWin(2, 2) == GameLogic.Winner.MANHUNTERS);
        check("win: 2v1 -> manhunters", GameLogic.checkWin(2, 1) == GameLogic.Winner.MANHUNTERS);
        check("win: 2v3 -> none", GameLogic.checkWin(2, 3) == GameLogic.Winner.NONE);
        check("win: 1v2 -> none", GameLogic.checkWin(1, 2) == GameLogic.Winner.NONE);

        List<UUID> p = ids(5); Set<UUID> living = new LinkedHashSet<>(p);
        Map<UUID, UUID> v = new HashMap<>();
        v.put(p.get(0), p.get(4)); v.put(p.get(1), p.get(4)); v.put(p.get(2), p.get(3)); v.put(p.get(3), GameLogic.SKIP);
        check("vote: clear majority ejects", p.get(4).equals(GameLogic.tally(v, living).ejected()));

        List<UUID> q = ids(4); Set<UUID> lq = new LinkedHashSet<>(q);
        Map<UUID, UUID> t = new HashMap<>(); t.put(q.get(0), q.get(2)); t.put(q.get(1), q.get(3));
        check("vote: tie -> no ejection", GameLogic.tally(t, lq).ejected() == null);

        Map<UUID, UUID> s = new HashMap<>(); s.put(p.get(0), p.get(4)); s.put(p.get(1), GameLogic.SKIP);
        check("vote: 1 vs 1 skip -> none", GameLogic.tally(s, living).ejected() == null);
        s.put(p.get(2), GameLogic.SKIP);
        check("vote: skip majority -> none", GameLogic.tally(s, living).ejected() == null);
        s.put(p.get(3), p.get(4)); s.put(p.get(4), p.get(4));
        check("vote: 3 votes beat 2 skips", p.get(4).equals(GameLogic.tally(s, living).ejected()));

        check("vote: no votes -> none", GameLogic.tally(new HashMap<>(), living).ejected() == null);
        List<UUID> r = ids(4); Set<UUID> lr = new LinkedHashSet<>(r.subList(0, 3));
        Map<UUID, UUID> d = new HashMap<>(); d.put(r.get(3), r.get(0)); d.put(r.get(0), r.get(3));
        check("vote: dead voter/target ignored", GameLogic.tally(d, lr).ejected() == null);

        boolean ringOk = true;
        for (int i = 0; i < 1000; i++) {
            List<UUID> g = ids(6); Set<UUID> m = new HashSet<>(g.subList(0, 2)); UUID ex = g.get(2);
            Optional<UUID> c = GameLogic.pickRingCandidate(g, m, ex, new Random(i));
            ringOk &= c.isPresent() && !m.contains(c.get()) && !c.get().equals(ex);
        }
        check("ring transfer never Manhunter/excluded (1000 trials)", ringOk);
        List<UUID> g3 = ids(3);
        check("ring transfer empty when no innocents", GameLogic.pickRingCandidate(g3, new HashSet<>(g3.subList(0, 2)), g3.get(2), new Random(1)).isEmpty());

        boolean forcedOk = true;
        for (int i = 0; i < 500; i++) {
            List<UUID> f = ids(8);
            Set<UUID> m = GameLogic.assignManhunters(f, 2, Set.of(f.get(0)), Set.of(f.get(1), f.get(2)), new Random(i));
            forcedOk &= m.size() == 2 && m.contains(f.get(0)) && !m.contains(f.get(1)) && !m.contains(f.get(2));
        }
        check("forced Manhunter / forced non-Manhunter respected (500 trials)", forcedOk);
        List<UUID> h = ids(6); Set<UUID> hm = new HashSet<>(h.subList(0, 1));
        check("forced Lantern preferred when valid", h.get(3).equals(GameLogic.pickRingCandidate(h, hm, null, new Random(1), h.get(3)).get()));
        check("forced Lantern ignored if Manhunter", !h.get(0).equals(GameLogic.pickRingCandidate(h, hm, null, new Random(1), h.get(0)).get()));

        boolean setsOk = true, distinctOk = true;
        for (int seed = 0; seed < 300; seed++) {
            int stationsN = 6 + seed % 5, per = 3, ppl = 6 + seed % 7;
            List<int[]> sets = GameLogic.assignTaskSets(stationsN, per, ppl, new Random(seed));
            Set<String> sigs = new HashSet<>();
            for (int[] one : sets) {
                setsOk &= one.length == per && Arrays.stream(one).distinct().count() == per && Arrays.stream(one).allMatch(x -> x >= 0 && x < stationsN);
                sigs.add(Arrays.toString(one));
            }
            distinctOk &= sigs.size() == ppl; // C(6,3)=20 >= 12 players, so all mixes must differ
        }
        check("task sets: right size, distinct stations, in range (300 trials)", setsOk);
        check("task sets: every player gets a different mix (300 trials)", distinctOk);
        check("task sets: fewer stations than perPlayer is clamped", GameLogic.assignTaskSets(2, 5, 3, new Random(1)).get(0).length == 2);

        check("task scaling: 4,5 players -> 3", GameLogic.scaledTasksPerPlayer(4) == 3 && GameLogic.scaledTasksPerPlayer(5) == 3);
        check("task scaling: 6 players -> 4 (up to 9)", GameLogic.scaledTasksPerPlayer(6) == 4 && GameLogic.scaledTasksPerPlayer(9) == 4);
        check("task scaling: 10 players -> 5 (up to 13)", GameLogic.scaledTasksPerPlayer(10) == 5 && GameLogic.scaledTasksPerPlayer(13) == 5);
        check("task scaling: 14+ players -> 6", GameLogic.scaledTasksPerPlayer(14) == 6 && GameLogic.scaledTasksPerPlayer(30) == 6);

        check("last stand: 1 vs 2 and 2 vs 3 and 3 vs 4", GameLogic.lastStand(1, 2) && GameLogic.lastStand(2, 3) && GameLogic.lastStand(3, 4));
        check("last stand: not at 2 vs 4, 1 vs 3, or already lost (1 vs 1)", !GameLogic.lastStand(2, 4) && !GameLogic.lastStand(1, 3) && !GameLogic.lastStand(1, 1));
        check("last stand: never with no Manhunters", !GameLogic.lastStand(0, 1));

        double[] th = {0.25, 0.5, 0.75};
        check("guardian thresholds: 10% -> 0", GameLogic.crossedThresholds(th, 0, 0.1) == 0);
        check("guardian thresholds: 60% -> 2 charges earned", GameLogic.crossedThresholds(th, 0, 0.6) == 2);
        check("guardian thresholds: never re-awarded", GameLogic.crossedThresholds(th, 2, 0.6) == 2);
        check("guardian thresholds: 100% -> 3", GameLogic.crossedThresholds(th, 2, 1.0) == 3);
        check("progress math", GameLogic.progress(3, 12) == 0.25 && GameLogic.progress(0, 0) == 0.0);
        int lock = GameLogic.GUARDIAN_LOCK_MEETINGS;
        check("guardian: charged + unlocked is ready", GameLogic.guardianReady(1, 0));
        check("guardian: not ready without charge", !GameLogic.guardianReady(0, 0));
        check("guardian: locked right after use", !GameLogic.guardianReady(1, lock));
        lock = GameLogic.afterMeeting(lock);
        check("guardian: still locked after ONE meeting", !GameLogic.guardianReady(1, lock));
        lock = GameLogic.afterMeeting(lock);
        check("guardian: ready again after the SECOND meeting", GameLogic.guardianReady(1, lock));

        check("cooldown 0 when ready", GameLogic.remainingSeconds(100, 100) == 0 && GameLogic.remainingSeconds(90, 100) == 0);
        check("cooldown rounds up", GameLogic.remainingSeconds(101, 100) == 1 && GameLogic.remainingSeconds(160, 100) == 3);

        System.out.println("\n" + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
