# Archivverbindungen: Rückvergleich und vorbereitete Initialisierung

Stand 16.11.2026. Vorbereitung nach 3.0.12, ohne neuen APK-Bau und ohne neue Veröffentlichung. Dieser Stand ist keine Zusage, dass sämtliche realen Verbindungsabbrüche behoben sind.

## Historischer Vergleich

| Quellstand | Initiales Lesen der Transportsteuerung | Verbindungspflege |
| --- | --- | --- |
| Früher Zentralstand vom 03.11., `bdedf35` | fehlt | Wartungsaufrufe innerhalb lokaler Archivprüfungen |
| AutarPie 3.0.4, vorhandener Tag | fehlt | bisheriger gemeinsamer Protokollablauf, Android-Rückmeldungen am Hauptthread |
| 3.0.9, `73c9074` | fehlt | eigener serieller Protokollablauf; Android-Aufträge und Rückmeldungen noch am Hauptthread |
| 3.0.12, geprüfte APK-Quelle `5fac3e7` | fehlt | Protokollablauf und Android-Rückmeldungen getrennt vom Hauptthread |
| Hier vorbereiteter Stand | wird bei angebotener Lesefunktion vor Anmeldung ausgeführt | bisherige Serialisierung, Credits, Kontextanmeldung und Abbruchbedingungen bleiben erhalten |

Die Bytes der Sitzungserneuerung und die drei angemeldeten Kontexte sind im frühen Zentralstand bereits dieselben. Ein pauschales Zurücknehmen des letzten Thread-Umbaus beseitigt die fehlende Initialisierung daher nicht. Der Quellvergleich identifiziert damit noch keinen einzelnen Update-Commit als Ursache aller realen Abbrüche.

Ein weiterer historischer Unterschied sind zusätzliche kurze Solar-Live-/Lastausgangsabfragen im neueren Monitorbetrieb (`5d88e91`). Das verändert die Anzahl der Verbindungsaufbauten. Ob diese zusätzliche Belastung einen konkreten Ausfall verursacht, ist ohne Transportaufzeichnung nicht belegt.

## Nachgewiesene Lücke und Vorbereitung

Der Autor der verwendeten Trendaufzeichnung beschreibt für `306b0002` einen initialen Lesezugriff vor Benachrichtigungen und Anmeldung. Auf seinen realen SmartShunt-/SmartSolar-Geräten konnte der Zustand nach Einschalten darüber entscheiden, ob die nachfolgende Anmeldung Werte lieferte. Quelle: [Protokollbeschreibung, Abschnitt 1.1](https://github.com/edilAraujo/victron-trends/blob/main/PROTOCOL.md#11-keeping-a-session-alive), [ausführbarer Leser](https://github.com/edilAraujo/victron-trends/blob/main/victron_trends/ble.py). Das sind Angaben zu den dort untersuchten Geräten, kein physischer Test dieser Vorbereitung an einem anderen Gerät.

`ArchiveService` liest die vorhandene Transportsteuerung nach Diensterkennung/Paketgrößenverhandlung und vor Anmeldung. Die Antwort wird verbindungslokal kopiert; unbekannte Bytes werden nicht als dokumentierte Grenzwerte ausgegeben. Bei fehlender Lesefunktion bleibt die bisherige Anmeldung erhalten. Ein angebotener, aber fehlgeschlagener oder leerer Lesezugriff startet keine nachfolgenden Anmeldebefehle. Fehler, Abbruch und eine neue Verbindung verwenden keine alte Antwort.

Die Diagnose unterscheidet außerdem einen von Android angenommenen Auftrag von einer tatsächlich eingegangenen Android-Antwort. Bisher beschriftete der `finally`-Block auch Wartezeiten bis zum Verbindungsabbruch als Bestätigung. Das war kein Beleg für eine empfangene Schreibantwort. Sitzungserneuerungszähler bezeichnen Android-Schreibantworten, nicht unabhängig geprüfte Antworten auf Anwendungsebene des Geräts.

## Gezielte Prüfung ohne APK

```sh
python3 tools/prove-archive-session-start.py --dependencies /pfad/zu/proof-deps --output /tmp/session-start
python3 tools/prove-archive-session-start.py --dependencies /pfad/zu/proof-deps --output /tmp/session-start-old --baseline-ref bdedf35 --baseline-only
python3 tools/prove-archive-session-start.py --dependencies /pfad/zu/proof-deps --output /tmp/session-start-304 --baseline-ref v3.0.4 --baseline-only
```

Die Prüfung führt den vollständigen extrahierten produktiven Verbindungs- und Anmeldeablauf aus. Android-GATT-Aufträge und Rückmeldungen sind asynchrone Adapter. Das Modell bildet den vom Aufzeichnungsautor beschriebenen Unterschied zwischen bereits initialisierter und frisch gestarteter Gegenstelle nach; es wird nicht als echte Hardware ausgegeben. Es vergleicht alte und vorbereitete Quellen, erhält die beobachteten Anmeldebytes, prüft API-30-/35-Rückmeldungen, fehlende Lesefunktion, leere/fehlgeschlagene Antwort und das Beenden einer laufenden Initialisierung. Die bestehende IO-Prüfung kompiliert zusätzlich sämtliche Produktions-Javaquellen und prüft blockierten Hauptthread, Abbruch und fehlerhafte Schreibantwort.

## Grenzen und noch benötigte Unterscheidung

Ein Timeout bei der Diensterkennung tritt vor dem neuen Initialisierungszugriff auf. Ein später fehlender Schreibcallback während einer laufenden Archivsitzung ist ebenfalls nicht allein durch diesen Zugriff erklärt. Aus einem Status 19, einer langsamen lokalen Prüfung und erfolgreichen Adaptertests folgt keine eindeutige Ursache im Bluetooth-System oder Gerät.

Für diese Unterscheidung ist eine zeitlich passende Bluetooth-System-/HCI-Aufzeichnung des tatsächlich erfassenden Telefons nötig: Wird die Anfrage an den Controller übergeben, trifft eine Geräteantwort ein, und wann meldet Android das Ergebnis an die App? Der normale Zentralen-Diagnoseexport enthält diese Paketebene nicht. [Android: Bluetooth prüfen und debuggen](https://source.android.com/docs/core/connect/bluetooth/verifying_debugging).

Vor einem weiteren APK-Bau gilt weiterhin die vereinbarte konkrete Freigabe. Neue Grundeinstellungen, Anzeigeleistung und automatische Sicherungen bleiben gesondert offen; diese Vorbereitung erledigt diese Funktionen nicht.
