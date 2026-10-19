package dev.donutgamble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donutgamble.json");

    public boolean enabled = true;
    /** Gewinnchance des Zahlers, 0.30 = 30 % */
    public double winChance = 0.30;
    /** Auszahlung = Einsatz * multiplier (2.0 = Einsatz wird verdoppelt). */
    public double multiplier = 2.0;
    public long minBet = 1000;
    public long maxBet = 1_000_000;
    /** Ticks zwischen zwei gesendeten Nachrichten/Commands (20 Ticks = 1 s) */
    public int delayTicks = 40;
    /**
     * Regex fuer die Pay-Nachricht. Gruppe 1 = Spielername, Gruppe 2 = Betrag, Gruppe 3 = Suffix (K/M/B).
     * An das echte Chat-Format des Servers anpassen!
     */
    /** Zufaelligen Online-Spielern automatisch Geld per /pay schicken (Werbung/Anlocken) */
    public boolean randomPayEnabled = true;
    public long randomPayAmount = 1;
    /** Sekunden zwischen zwei zufaelligen Zahlungen */
    public int randomPayIntervalSeconds = 60;
    public String payRegex = "(\\w{3,16}) paid you \\$([\\d,.]+)([KkMmBb]?)";
    public String winMessage = "{player} hat {payout} gewonnen! (Chance {chance}%)";
    public String loseMessage = "{player} hat {bet} verloren. (Chance {chance}%)";
    public String advertMessage = "Pay mir fuer Gamble: {chance}% Chance auf x{mult}!";

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                return GSON.fromJson(Files.readString(FILE), Config.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Config c = new Config();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
