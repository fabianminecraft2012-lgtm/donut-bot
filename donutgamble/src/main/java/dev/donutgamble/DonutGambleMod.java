package dev.donutgamble;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DonutGambleMod implements ClientModInitializer {
    private static final SecureRandom RNG = new SecureRandom();
    private final Queue<Runnable> queue = new ArrayDeque<>();
    private Config cfg;
    private Pattern payPattern;
    private int cooldown = 0;
    private int advertTimer = 0;
    private int randomPayTimer = 0;
    private KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        cfg = Config.load();
        payPattern = Pattern.compile(cfg.payRegex);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && cfg.enabled) handle(message.getString());
        });

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donutgamble.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.donutgamble"));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(ClientCommandManager.literal("gamble")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("Gamble-Bot ist " + (cfg.enabled ? "AN" : "AUS")
                        + " | /gamble on, /gamble off, /gamble reload"));
                    return 1;
                })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    cfg.enabled = true; cfg.save();
                    ctx.getSource().sendFeedback(Text.literal("Gamble-Bot AN"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    cfg.enabled = false; cfg.save();
                    ctx.getSource().sendFeedback(Text.literal("Gamble-Bot AUS"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    cfg = Config.load();
                    payPattern = Pattern.compile(cfg.payRegex);
                    ctx.getSource().sendFeedback(Text.literal("Config neu geladen"));
                    return 1;
                }))));
    }

    private void handle(String raw) {
        Matcher m = payPattern.matcher(raw);
        if (!m.find()) return;

        String player = m.group(1);
        long bet = parseAmount(m.group(2), m.groupCount() >= 3 ? m.group(3) : "");
        if (bet <= 0) return;

        if (bet < cfg.minBet || bet > cfg.maxBet) {
            // Einsatz ausserhalb der Grenzen: zurueckzahlen
            queue.add(() -> command("pay " + player + " " + bet));
            queue.add(() -> chat(player + ": Einsatz muss zwischen " + cfg.minBet + " und " + cfg.maxBet + " liegen (zurueckgezahlt)."));
            return;
        }

        boolean win = RNG.nextDouble() < cfg.winChance;
        long payout = Math.round(bet * cfg.multiplier);
        String chance = String.format(Locale.ROOT, "%.0f", cfg.winChance * 100);

        if (win) {
            queue.add(() -> command("pay " + player + " " + payout));
            queue.add(() -> chat(cfg.winMessage
                .replace("{player}", player).replace("{payout}", String.valueOf(payout))
                .replace("{chance}", chance)));
        } else {
            queue.add(() -> chat(cfg.loseMessage
                .replace("{player}", player).replace("{bet}", String.valueOf(bet))
                .replace("{chance}", chance)));
        }
    }

    private static long parseAmount(String num, String suffix) {
        try {
            double v = Double.parseDouble(num.replace(",", ""));
            switch (suffix == null ? "" : suffix.toLowerCase(Locale.ROOT)) {
                case "k": v *= 1_000L; break;
                case "m": v *= 1_000_000L; break;
                case "b": v *= 1_000_000_000L; break;
                default: break;
            }
            return (long) v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void tick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            if (client.player == null) continue;
            cfg.enabled = !cfg.enabled;
            cfg.save();
            client.player.sendMessage(Text.literal("Gamble-Bot " + (cfg.enabled ? "AN" : "AUS")), true);
        }
        if (client.player == null || client.getNetworkHandler() == null || !cfg.enabled) return;

        if (cooldown > 0) { cooldown--; }
        else if (!queue.isEmpty()) {
            queue.poll().run();
            cooldown = cfg.delayTicks;
        }

        // Zufaelligen Spieler anpayen
        if (cfg.randomPayEnabled && ++randomPayTimer >= 20 * Math.max(1, cfg.randomPayIntervalSeconds)) {
            randomPayTimer = 0;
            String target = pickRandomPlayer(client);
            if (target != null) {
                queue.add(() -> command("pay " + target + " " + cfg.randomPayAmount));
            }
        }

        // Alle 5 Minuten Werbung, solange nichts ansteht
        if (queue.isEmpty() && ++advertTimer >= 20 * 300) {
            advertTimer = 0;
            chat(cfg.advertMessage
                .replace("{chance}", String.format(Locale.ROOT, "%.0f", cfg.winChance * 100))
                .replace("{mult}", String.valueOf(cfg.multiplier)));
        }
    }

    private String pickRandomPlayer(MinecraftClient client) {
        String self = client.player.getGameProfile().getName();
        java.util.List<String> names = new java.util.ArrayList<>();
        for (net.minecraft.client.network.PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().getName();
            if (!n.equalsIgnoreCase(self)) names.add(n);
        }
        if (names.isEmpty()) return null;
        return names.get(RNG.nextInt(names.size()));
    }

    private void command(String cmd) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.getNetworkHandler() != null) c.getNetworkHandler().sendChatCommand(cmd);
    }

    private void chat(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.getNetworkHandler() != null) c.getNetworkHandler().sendChatMessage(msg);
    }
}
