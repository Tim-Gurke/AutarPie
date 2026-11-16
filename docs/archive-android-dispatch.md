# Vorbereitete Archivkorrektur nach 3.0.11

Stand: 16.11.2026. Quellcode vorbereitet und lokal geprüft; noch keine neue APK gebaut oder veröffentlicht.

## Problem und Änderung

Der Archivdienst besitzt bereits einen eigenen seriellen Protokollablauf. In 3.0.11 laufen Android-GATT-Aufträge und ihre Rückmeldungen trotzdem über den Hauptthread. Längere Arbeit im Hauptthread kann dadurch die Diensterkennung sowie Schreibbestätigungen, Empfangsfreigaben und Sitzungserneuerungen verzögern. Ein unabhängiger Protokoll-Executor allein schützt davor nicht.

`ArchiveService` weist Android-GATT-Aufträgen und Rückmeldungen einen eigenen `HandlerThread` zu. `ArchiveIo` bleibt für die Reihenfolge der Protokollaufträge zuständig. Der neue Android-Thread verarbeitet keine Datenbankabfragen oder Lernmodelle. Beim Beenden werden Verbindung, wartende Aufträge und der zusätzliche Thread freigegeben.

`ArchiveSyncPlan` erweitert zusammenhängende Messintervalle ohne unnötiges Entfernen und erneutes Einfügen desselben Baumknotens. Bei durchgehenden Livewerten wird die identische Zeitabdeckung mit einem statt zwei überlappenden Einträgen gebildet. Die Abfrage durchsucht weiterhin den gespeicherten Bestand; echte Messlücken und verfügbare Auflösungen bleiben unverändert.

## Prüfung

- Der Vergleich verwendet produktive Methoden aus dem veröffentlichten Stand `9cf8d9db51a87141f44abcd63a852f0a833523d6` und dem vorbereiteten Quellcode. Bei blockiertem simuliertem Hauptthread bleiben im alten Ablauf Diensterkennung und Bestätigungen liegen. Im vorbereiteten Ablauf laufen sie für API 30 und 35 weiter; Abbruch schließt auch bei weiterhin blockiertem Hauptthread. Es bleibt höchstens ein GATT-Auftrag gleichzeitig offen.
- Insgesamt 114 Transportprüfungen in 15 gezielten Szenarien bestanden, darunter Verbindungswiederholung, Gerätewechsel, gemischter Stapelerfolg, Datenbankblockade, Abbruch, Verbindungsende und fehlgeschlagene Schreibbestätigung. Der gesamte produktive Java-Quellcode wird dabei gegen die Android-API kompiliert.
- 45 Gleichheitsprüfungen vergleichen Bestandsplanung und angeforderte Lücken mit 3.0.11. Enthalten sind 240.000 einzelne Entscheidungen zu vorhandenen Messpunkten sowie sortierte, verspätete, überlappende und unterbrochene Messreihen.
- Neun bestehende Anforderungen an SQLite-Abgleich und Fortsetzung nach Abbruch bestanden. Bereits gespeicherte oder als nicht verfügbar bestätigte Werte werden nicht erneut angefordert; vollständige Live-Aufzeichnung benötigt keine zusätzlichen Messwertabrufe.
- Lokale Messung nur des Java-Planers: 400.000 Livewerte, vier Auflösungen, Median aus drei Läufen nach Aufwärmen. Referenz etwa 108 ms, vorbereiteter Stand etwa 41 ms. Diese Zahlen gelten für den Prüfhost und enthalten weder SQLite noch Bluetooth oder die Übertragung an Anzeigehandys. Die Laufzeit ist kein Bestehensschwellwert.

Wiederholbare Befehle, ohne APK-Bau:

```sh
python3 tools/prove-archive-io.py --dependencies /pfad/zu/proof-deps --output /tmp/archive-io
python3 tools/prove-archive-availability.py --output /tmp/archive-availability
python3 tools/prove-archive-delta.py --output /tmp/archive-delta --sqlite-jar /pfad/zu/sqlite-jdbc.jar --slf4j-jar /pfad/zu/slf4j-api.jar
```

## Aussagegrenzen

Der Belastungstest reproduziert die Abhängigkeit vom Hauptthread und prüft die gezielte Korrektur. Android-Handler, GATT-Rückmeldungen und Gegenstelle sind dabei Adapter; es ist kein Test an einem physischen Regler, Shunt oder Android-Telefon. Ein Bluetooth-Status 19 allein belegt weder einen Defekt noch eine eindeutige Ursache. Nicht jeder beobachtete Verbindungsabbruch ist damit abschließend erklärt.

Die Geschwindigkeit von WLAN-Abgleich und Berechnungen auf Anzeigehandys ist separat zu messen. Aus den Java-Laufzeiten folgt keine Zusage zur gesamten Ladezeit auf Nutzergeräten. Messdaten, Datenbankschema und Schlüssel werden durch diese Änderung nicht migriert. Private Diagnose- und Messdaten gehören nicht zu den öffentlichen Prüfdaten.

Vor einem APK-Bau einschließlich APK-bauender CI ist Tims Freigabe für diesen vorbereiteten Umfang erforderlich.
