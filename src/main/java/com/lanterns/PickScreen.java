package com.lanterns;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Generic "pick a player from the lobby" menu (used by Shapeshift and the Sheriff's interrogation). */
public class PickScreen extends Screen {
    private static final int GREEN = 0xFF2ECC71, MINT = 0xFF7CFFB2, PANEL = 0xFF10161A, CELL = 0xFF263238, GREY = 0xFFB0B0B0;

    private record Row(String id, String name) {}

    private final String purpose, heading;
    private final List<Row> rows = new ArrayList<>();
    private int px, py, pw = 300, ph = 120, cols = 1, perCol = 1, bw = 260;

    public PickScreen(PickPlayerPayload p) {
        super(Text.literal(p.title()));
        this.purpose = p.purpose();
        this.heading = p.title();
        for (String r : p.data().split(";")) {
            if (r.isEmpty()) continue;
            String[] f = r.split("\\|");
            if (f.length >= 2) rows.add(new Row(f[0], f[1]));
        }
    }

    private void layout() {
        int n = Math.max(1, rows.size());
        cols = n > 8 ? 2 : 1;
        perCol = (n + cols - 1) / cols;
        bw = cols == 2 ? 136 : 260;
        ph = 40 + perCol * 24 + 26;
        px = (width - pw) / 2;
        py = Math.max(4, (height - ph) / 2);
    }

    private int bx(int i) { return cols == 2 ? 10 + (i / perCol) * (bw + 8) : 20; }
    private int by(int i) { return 32 + (i % perCol) * 24; }

    private void rect(DrawContext c, int x1, int y1, int x2, int y2, int col) { c.fill(px + x1, py + y1, px + x2, py + y2, col); }

    private void frame(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        rect(c, x1, y1, x2, y1 + 1, col); rect(c, x1, y2 - 1, x2, y2, col);
        rect(c, x1, y1, x1 + 1, y2, col); rect(c, x2 - 1, y1, x2, y2, col);
    }

    private void text(DrawContext c, String s, int x, int y, int col) { c.drawTextWithShadow(this.textRenderer, s, px + x, py + y, col); }

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
        text(c, heading.toUpperCase(), 26, 7, MINT);
        for (int i = 0; i < rows.size(); i++) {
            int x = bx(i), y = by(i);
            boolean hover = in(mx, my, x, y, x + bw, y + 20);
            rect(c, x, y, x + bw, y + 20, hover ? 0xFF37474F : CELL);
            frame(c, x, y, x + bw, y + 20, hover ? MINT : GREEN);
            text(c, rows.get(i).name(), x + 6, y + 6, 0xFFFFFFFF);
        }
        text(c, "Click a player  |  ESC to cancel", 20, ph - 14, GREY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;
        int mx = (int) mouseX - px, my = (int) mouseY - py;
        for (int i = 0; i < rows.size(); i++) {
            if (in(mx, my, bx(i), by(i), bx(i) + bw, by(i) + 20)) {
                ClientPlayNetworking.send(new PickResultPayload(purpose, rows.get(i).id()));
                close();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}
