package de.privat.activityaddon.modules;

import de.privat.activityaddon.ActivityAddon;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityInspector extends Module {
    public record Pos(long time, Vec3d pos) {}

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("interval-ticks").description("Alle wie viele Ticks die Position gespeichert wird (20 = 1 Sek).")
        .defaultValue(20).min(1).sliderRange(1, 200).build());

    private final Setting<Integer> historyMinutes = sgGeneral.add(new IntSetting.Builder()
        .name("history-minutes").description("Wie lange Positionen behalten werden.")
        .defaultValue(10).min(1).sliderRange(1, 60).build());

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self").description("Eigene Bewegung nicht tracken.")
        .defaultValue(true).build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render-trails").description("Zeichnet die Wege als Linien.")
        .defaultValue(true).build());

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color").defaultValue(new SettingColor(255, 60, 60, 255)).build());

    /** Spielername -> Positionsverlauf */
    public static final Map<String, Deque<Pos>> HISTORY = new ConcurrentHashMap<>();
    private int timer;

    public ActivityInspector() {
        super(ActivityAddon.CATEGORY, "activity-inspector", "Debug: trackt, wo sich Spieler in letzter Zeit bewegt haben.");
    }

    @Override
    public void onDeactivate() {
        HISTORY.clear();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        HISTORY.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        if (++timer < interval.get()) return;
        timer = 0;

        long now = System.currentTimeMillis();
        long maxAge = historyMinutes.get() * 60_000L;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (ignoreSelf.get() && p == mc.player) continue;
            Deque<Pos> d = HISTORY.computeIfAbsent(p.getName().getString(), k -> new ArrayDeque<>());
            Vec3d v = new Vec3d(p.getX(), p.getY(), p.getZ());
            // nur speichern, wenn sich der Spieler bewegt hat
            if (d.isEmpty() || d.peekLast().pos().squaredDistanceTo(v) > 0.25) d.addLast(new Pos(now, v));
        }
        for (Deque<Pos> d : HISTORY.values()) {
            while (!d.isEmpty() && now - d.peekFirst().time() > maxAge) d.pollFirst();
        }
        HISTORY.values().removeIf(Deque::isEmpty);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        for (Deque<Pos> d : HISTORY.values()) {
            Vec3d prev = null;
            for (Pos p : d) {
                Vec3d cur = p.pos();
                // keine Linie bei Teleports / großen Sprüngen
                if (prev != null && prev.squaredDistanceTo(cur) < 64 * 64) {
                    event.renderer.line(prev.x, prev.y + 0.1, prev.z, cur.x, cur.y + 0.1, cur.z, color.get());
                }
                prev = cur;
            }
        }
    }
}
