package com.lanterns;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/** Among Us style ballot. Shows who has voted (never for whom), a countdown, and a skip button. */
public class VoteScreen extends Screen {
    private static final int GREEN = 0xFF2ECC71, MINT = 0xFF7CFFB2, PANEL = 0xFF10161A, CELL = 0xFF263238,
            OKCELL = 0xFF1B5E37, GREY = 0xFFB0B0B0, RED = 0xFFE53935;

    private record Row(String id, String name, boolean voted) {}

    private List<Row> rows = new ArrayList<>();
    private String myVote = "";
    private long endAtMs;
    private final long totalMs;
    private int px, py, pw = 300, ph = 200, perCol = 1, cols = 1, bw = 260;

    public VoteScreen(VoteStatePayload p) {
        super(Text.literal("Vote"));
        this.totalMs = Math.max(1000, p.secondsLeft() * 1000L);
        update(p);
    }

    public void update(VoteStatePayload p) {
        List<Row> list = new ArrayList<>();
        for (String r : p.data().split(";")) {
            if (r.isEmpty()) continue;
            String[] f = r.split("\\|");
            if (f.length >= 3) list.add(new Row(f[0], f[1], f[2].equals("1")));
        }
        rows = list;
        myVote = p.myVote();
        endAtMs = Util.getMeasuringTimeMs() + p.secondsLeft() * 1000L;
    }

    private void layout() {
        int n = Math.max(1, rows.size());
        cols = n > 8 ? 2 : 1;
        perCol = (n + cols - 1) / cols;
        bw = cols == 2 ? 136 : 260;
        ph = 62 + perCol * 24 + 34;
        px = (width - pw) / 2;
        py = Math.max(4, (height - ph) / 2);
    }

    private int bx(int i) { return cols == 2 ? 10 + (i / perCol) * (bw + 8) : 20; }
    private int by(int i) { return 52 + (i % perCol) * 24; }

    private void rect(DrawContext c, int x1, int y1, int x2, int y2, int col) { c.fill(px + x1, py + y1, px + x2, py + y2, col); }

    private void frame(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        rect(c, x1, y1, x2, y1 + 1, col); rect(c, x1, y2 - 1, x2, y2, col);
        rect(c, x1, y1, x1 + 1, y2, col); rect(c, x2 - 1, y1, x2, y2, col);
    }

    private void text(DrawContext c, String s, int x, int y, int col) { c.drawTextWithShadow(this.textRenderer, s, px + x, py + y, col); }

    private void ctext(DrawContext c, String s, int cx, int y, int col) { c.drawCenteredTextWithShadow(this.textRenderer, s, px + cx, py + y, col); }

    private static boolean in(int mx, int my, int x1, int y1, int x2, int y2) { return mx >= x1 && mx <= x2 && my >= y1 && my <= y2; }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        layout();
        int mx = mouseX - px, my = mouseY - py;
        c.fill(0, 0, width, height, 0xB0000000);
        rect(c, -3, -3, pw + 3, ph + 3, 0xFF0B3D1A);
        rect(c, 0, 0, pw, ph, PANEL);
        frame(c, 0, 0, pw, ph, GREEN);
        rect(c, 0, 0, pw, 22, 0xFF0F2A18);
        rect(c, 6, 5, 18, 17, GREEN); rect(c, 8, 7, 16, 15, PANEL); rect(c, 10, 9, 14, 13, MINT);
        text(c, "EMERGENCY MEETING - VOTE", 26, 7, MINT);

        long left = Math.max(0, endAtMs - Util.getMeasuringTimeMs());
        rect(c, 20, 30, pw - 20, 38, CELL);
        rect(c, 21, 31, 21 + (int) ((pw - 42) * Math.min(1.0, left / (double) totalMs)), 37, left < 8000 ? RED : GREEN);
        ctext(c, (left / 1000 + 1) + "s", pw / 2, 41, GREY);

        boolean locked = !myVote.isEmpty();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int x = bx(i), y = by(i);
            boolean mine = r.id().equals(myVote);
            boolean hover = !locked && in(mx, my, x, y, x + bw, y + 20);
            rect(c, x, y, x + bw, y + 20, mine ? OKCELL : (hover ? 0xFF37474F : CELL));
            frame(c, x, y, x + bw, y + 20, mine ? MINT : GREEN);
            text(c, r.name(), x + 6, y + 6, 0xFFFFFFFF);
            if (mine) text(c, "YOUR VOTE", x + bw - 62, y + 6, MINT);
            else if (r.voted()) text(c, "VOTED", x + bw - 38, y + 6, 0xFF6FAF8A);
        }
        int sy = ph - 30;
        boolean skipMine = "skip".equals(myVote);
        boolean skipHover = !locked && in(mx, my, 20, sy, pw - 20, sy + 20);
        rect(c, 20, sy, pw - 20, sy + 20, skipMine ? OKCELL : (skipHover ? 0xFF37474F : CELL));
        frame(c, 20, sy, pw - 20, sy + 20, skipMine ? MINT : 0xFF607D8B);
        ctext(c, skipMine ? "SKIPPED" : "SKIP VOTE", pw / 2, sy + 6, 0xFFFFFFFF);
        if (locked) ctext(c, "Vote locked in. Waiting for the others...", pw / 2, ph - 8, GREY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !myVote.isEmpty()) return true;
        int mx = (int) mouseX - px, my = (int) mouseY - py;
        for (int i = 0; i < rows.size(); i++) {
            int x = bx(i), y = by(i);
            if (in(mx, my, x, y, x + bw, y + 20)) { cast(rows.get(i).id()); return true; }
        }
        if (in(mx, my, 20, ph - 30, pw - 20, ph - 10)) cast("skip");
        return true;
    }

    private void cast(String target) {
        myVote = target; // optimistic; the server's next update confirms it
        ClientPlayNetworking.send(new VoteCastPayload(target));
    }

    @Override
    public boolean shouldPause() { return false; }
}
