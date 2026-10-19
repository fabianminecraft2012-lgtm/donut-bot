# Donut Gamble Bot (Fabric 1.21.4, Client-Mod)

Build: `gradle wrapper && ./gradlew build` -> `build/libs/donutgamble-1.0.0.jar`
(Java 21 noetig. Versionen in gradle.properties bei Bedarf anpassen.)

Config: `.minecraft/config/donutgamble.json` (wird beim ersten Start erzeugt)
Befehle: `/gamble on|off|reload`

WICHTIG: `payRegex` muss zum echten Chat-Format passen. Auf dem Server eine kleine
Zahlung erhalten, die Nachricht kopieren und die Regex entsprechend anpassen.

Zufaellige Zahlungen: `randomPayEnabled`, `randomPayAmount` (Default 1) und
`randomPayIntervalSeconds` (Default 60) in der Config.

Keybind: `G` schaltet den Bot an/aus (aenderbar unter Optionen > Steuerung > Donut Gamble Bot).
