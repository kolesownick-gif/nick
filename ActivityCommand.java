package de.privat.activityaddon.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.privat.activityaddon.modules.ActivityInspector;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;

public class ActivityCommand extends Command {
    public ActivityCommand() {
        super("activity", "Zeigt die letzten Positionen eines Spielers (Activity Inspector).");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            if (ActivityInspector.HISTORY.isEmpty()) { info("Keine Daten. Ist das Modul aktiv?"); return SINGLE_SUCCESS; }
            info("Getrackte Spieler: " + String.join(", ", ActivityInspector.HISTORY.keySet()));
            return SINGLE_SUCCESS;
        });
        builder.then(argument("player", StringArgumentType.word())
            .executes(ctx -> { list(StringArgumentType.getString(ctx, "player"), 10); return SINGLE_SUCCESS; })
            .then(argument("minutes", IntegerArgumentType.integer(1))
                .executes(ctx -> {
                    list(StringArgumentType.getString(ctx, "player"), IntegerArgumentType.getInteger(ctx, "minutes"));
                    return SINGLE_SUCCESS;
                })));
    }

    private void list(String name, int minutes) {
        Deque<ActivityInspector.Pos> d = null;
        for (var e : ActivityInspector.HISTORY.entrySet()) if (e.getKey().equalsIgnoreCase(name)) d = e.getValue();
        if (d == null || d.isEmpty()) { error("Keine Daten für " + name); return; }
        long since = System.currentTimeMillis() - minutes * 60_000L;
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        info(name + " – letzte " + minutes + " Min:");
        ActivityInspector.Pos last = null;
        for (ActivityInspector.Pos p : d) {
            if (p.time() < since) continue;
            if (last != null && last.pos().squaredDistanceTo(p.pos()) < 9) continue;
            info(String.format("%s  %.0f %.0f %.0f", f.format(new Date(p.time())), p.pos().x, p.pos().y, p.pos().z));
            last = p;
        }
    }
}
