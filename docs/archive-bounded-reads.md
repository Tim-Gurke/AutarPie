# Vorbereitete Archivkorrektur: begrenzte Bestandsabfragen und erneute Anmeldung

Vorbereitung vom 16.11.2026, noch keine veröffentlichte APK. Der bisherige
[Initialisierungsbefund](archive-session-initialization.md) bleibt Teil der
Vorbereitung. Diese Änderung ergänzt ihn, ohne das Datenbankformat zu ändern.

## Problem und Verhalten

Die Bestandsprüfung in `HistoryDb.archiveAvailability` liest bisher je Kanal
alle passenden Rohzeilen in einem einzigen `SQLiteCursor`. Ein großer Bestand
wird bei der Planung, bei geänderten Gerätestufen und bei der abschließenden
Bestandsprüfung erneut durchlaufen. Androids begrenztes `CursorWindow` muss bei
großen Ergebnissen wiederholt nachgeladen werden. Das ist Arbeit an den lokalen
Daten, unabhängig von der Menge noch fehlender Bluetooth-Messwerte.

Die Vorbereitung liest höchstens 2.048 Rohzeilen pro Abfrage und setzt hinter
dem letzten Paar aus Zeitstempel und `rowid` fort. Auch die untere Zeitgrenze der
SQL-Abfrage wird weitergesetzt, damit SQLite den vorhandenen Index
`history_device(device,ts)` benutzen kann. Unterschiedliche Auflösungen und
Datenquellen am selben Zeitpunkt bleiben getrennt erhalten. Es gibt kein
wachsendes `OFFSET`, keine Löschung oder Zusammenfassung von Messwerten und
keine neue Datenmigration. Die Prüfung gespeicherter Bestätigungen und die
abschließende erneute Prüfung des tatsächlichen Bestands bleiben bestehen.
Abbrechen wird vor jeder Seite und weiterhin während des Lesens geprüft.

Eine zweite Änderung behandelt eine unvollständige Bluetooth-Anmeldung. Nach
einer Zeitüberschreitung, einem Verbindungsabbruch oder GATT-Status 133 innerhalb
der Anmeldung wird der alte Android-Client geschlossen. Nach einer Freigabepause
wird die Anmeldung höchstens einmal mit einem frischen Client begonnen. Die
vorhandenen begrenzten Status-133-Versuche beim eigentlichen Verbindungsaufbau
bleiben erhalten. Ein dauerhafter Fehler beendet den Versuch weiterhin als
unvollständig; Abbrechen startet keine neue Anmeldung. Nicht unterstützte
Dienste, fehlende Berechtigungen und ungültige Protokollantworten werden nicht
als Erfolg behandelt.

Diese Wiederholung umfasst nur die Anmeldung. Ein anschließender manueller
Schaltauftrag liegt weiterhin außerhalb der Wiederholung. Bluetooth wird nicht
global aus- und eingeschaltet. Ein späterer Abbruch während des eigentlichen
Archivtransfers wird dadurch nicht automatisch als behoben ausgegeben.

## Nachweise ohne APK-Bau

- `tools/prove-archive-paging.py` führt den veröffentlichten `HistoryDb` aus
  Quellstand `5fac3e7bf06a463590b866e6e40c76cb2ebba94e` und die Vorbereitung mit
  echtem SQLite und einem großen neutralen Bestand aus. Sechs Felder, echte
  Lücken, NULL-Felder, gemischte Datenquellen, unterschiedliche Auflösungen mit
  identischen Zeitstempeln und bestätigte nicht verfügbare Werte werden
  verglichen. Ein zweiter Prozess prüft die Wiederverwendung desselben Bestands.
  Pro Durchlauf stimmen 288.000 einzelne Abdeckungsentscheidungen und alle
  Archivpläne überein. Daten und Datenbankversion bleiben unverändert.
- Derselbe Nachweis kontrolliert begrenzte Ergebnismengen, Fortsetzung ohne
  OFFSET, die tatsächliche SQLite-Indexwahl ohne temporäre Sortierung, die
  Feldfreigabe und Abbrechen vor der nächsten Seite.
- `tools/prove-archive-session-start.py` führt den vollständigen produktiven
  Anmeldeablauf mit asynchronen Android-/GATT-Adaptern aus. Eine tatsächlich
  ausbleibende Dienstrückmeldung lässt die veröffentlichte Fassung scheitern;
  die Vorbereitung meldet sich nach dem begrenzten Neuaufbau an. Dauerhafter
  Abbruch, verspätete Rückmeldung eines geschlossenen Clients und Abbrechen
  während der Diensterkennung sind ebenfalls abgedeckt.
- Die vorhandenen Speicherprüfungen laufen weiterhin auf dem tatsächlichen
  `HistoryDb` und `Repository` mit SQLite. Sie prüfen unter anderem atomare
  Bestätigungen, unveränderte erfasste Messwerte, Wiederaufnahme, echte Lücken
  und Archivzeitkorrektur. Die vorhandenen Bluetooth-/Abbruchprüfungen verwenden
  weiterhin den tatsächlichen produktiven Auftragspfad und GATT-Adapter.

Die Javaquellen werden für diese Prüfungen kompiliert; es entsteht keine APK.
JDBC streamt seine Ergebnisse und bildet Androids CursorWindow-Kosten nicht
nach. Seine Ausführungszeit wäre deshalb kein belastbarer G5-Benchmark. Die
Prüfung belegt kleinere SQL-Ergebnismengen bei gleichbleibenden Entscheidungen,
nicht eine bestimmte Laufzeit oder dauerhaft störungsfreien Funkbetrieb auf
einem physischen Handy. Ein erfolgreicher Mitschnitt nach einem Bluetooth-Neustart
beweist außerdem nicht nachträglich die Ursache eines früheren Abbruchs.

## Quellen und Ausführung

Android 11 implementiert das erste Füllen und spätere Nachladen der Fenster in
[`SQLiteCursor.fillWindow`](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android11-release/core/java/android/database/sqlite/SQLiteCursor.java).
Die Zeit-/Zeilenbegrenzung der Vorbereitung benötigt keine neuen SQLite-
Sprachfunktionen und keine Änderung bestehender gespeicherter Profile.

```sh
python3 tools/prove-archive-paging.py --dependencies "$deps" --output "$results/paging"
python3 tools/prove-archive-session-start.py --dependencies "$deps" --output "$results/session"
```

Die vorbereitete Prüfkonfiguration nimmt den neuen Nachweis in den nächsten
ausdrücklich freigegebenen Gesamtlauf auf. Der Vorbereitungsbranch startet
keinen APK-Bau. Die Freigabe für einen neuen Bau und eine Veröffentlichung steht
noch aus.
