# AutarPie 3.1.1: Anzeige fragt bei dauerhaft grauen Feldern erneut nach

Ausgangspunkt ist ein frischer morgendlicher Start der Anzeige-App: Felder
bleiben grau, ein erneuter Archivabruf hilft nicht zuverlässig. Erst ein weiteres
Schließen und Öffnen der Anzeige-App hilft, ohne Änderung an der Zentrale.

## Korrekturen

- Bleibt eine sichtbare Kachel 45 Sekunden durchgehend grau, fragt die Anzeige
  erneut nach. Weitere Wiederholungen für diese Kachel erfolgen frühestens
  nach 60 Sekunden. Wieder aktuelle, ausgeblendete oder geschlossene Anzeigen
  lösen keine solchen Wiederholungen aus.
- Betroffene Diagramme werden anhand ihrer tatsächlichen letzten Abfrage
  gezielt erneut angefordert. Vorhandene Kurven bleiben mit dem Hinweis auf
  den alten Stand sichtbar. Andere aktuelle Diagramme werden nicht ungültig
  gemacht. Bereits laufende gleiche Übertragungen werden nicht zusätzlich
  angefordert; mehrere Kacheln können dieselbe Abfrage teilen.
  Gleichzeitige Anforderungen einer neuen Zentralenansicht werden ebenfalls
  zu höchstens einer wartenden Anfrage zusammengefasst.
- Wetter und noch vorläufige Akku-Prognosebalken können eine Wiederholung
  auslösen. Die Anzeige fragt den Anlagenverbund erneut ab und fordert bei
  Bedarf den Lernstand erneut an. Im eigenständigen Betrieb wird bei grauem
  Wetter zusätzlich der Wetterabruf wiederholt.
- Scheitert eine Verbundabfrage, wird eine fehlgeschlagene oder stumme
  Android-Gerätesuche nach mindestens 30 Sekunden seit dem letzten Suchstart
  neu begonnen. Vorher fehlte diese Wiederholung. Das betrifft besonders
  einen Neustart der Zentrale mit anschließend geändertem TCP-Port.
- Verspätete Such- und Auflösungsantworten einer beendeten Suche dürfen eine
  neue Suche nicht beenden oder deren aktuelle Verbindungsadresse ersetzen.
- Ein vorheriger Wetterfehler darf weitere Wetterabrufe nicht bis zum Ende
  des sechsstündigen Cachefensters unterdrücken. Gültige Wetterdaten ohne
  Fehler werden weiterhin wiederverwendet.

Die Wiederholung setzt keine grauen Felder künstlich auf aktuell. Die bisherige
Aktualitätsprüfung und die getrennte Kennzeichnung von Wetter und Prognose
bleiben erhalten. Die Korrekturen starten keinen Archivabruf, ändern keine
Aufzeichnung, steuern keine Geräte und löschen weder Messwerte noch Kopplungen
oder Kachelanordnungen.

## Prüfung und Grenzen

Vorbereitet sind Zeit- und Warteschlangentests für den produktiven JavaScript-Code
sowie eine native Nachstellung mit echten verschlüsselten TCP-Verbindungen und
SQLite. Nur Androids NSD-Rückmeldungen werden im Verbindungstest gezielt
vorgegeben; echte Handys, Funk, Hotspot und Startdauer werden damit nicht gemessen.
Der Verbindungstest kann auch die veröffentlichte `LanSync`-Implementierung als
Negativvergleich ausführen. Der vorbereitete Umfang wurde am 23.11.2026 zum Bau und zur Veröffentlichung freigegeben.

Abgeschlossen am 23.11.2026:

- 127 JavaScript-Prüfungen für Aktualitätsanzeige, Abfragen, Wiederholung und
  Prognoseaktualisierung bestanden.
- 15 native Prüfungen mit echten verschlüsselten TCP-Anfragen und SQLite
  bestanden. Enthalten sind der fehlgeschlagene erste Suchstart, stumme Suche,
  verspätete Rückmeldungen, unveränderte Kopplung und Anzeigeanordnung sowie
  200 gleichzeitige Anforderungen bei gesperrter Warteschlange: Es folgt nur
  eine zusätzliche Zentralenabfrage.
- Alle produktiven Java-Quellen gegen die echte Android-API 35 kompiliert,
  ohne die NSD-Testadapter. Syntaxprüfung aller geänderten JavaScript-Dateien
  und Ausschluss privater Dateien bestanden.
- Negativvergleich mit der veröffentlichten `LanSync` aus 3.1.0 (`8eef541`):
  Nach dem fehlgeschlagenen Suchstart verbindet sie die unverändert geöffnete
  Anzeige nicht neu. Die Korrektur besteht denselben Verbindungsfall.

Die Tests belegen diese nachgestellten Fehler und Korrekturen. Sie rekonstruieren
nicht die komplette frühere Stunde Wartezeit und bestätigen noch keinen
fehlerfreien Start auf Tims echten Handys. Der Umfang für Version 3.1.1 ist am 23.11.2026 zum Bau und zur Veröffentlichung freigegeben.
