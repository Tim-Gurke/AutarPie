# Vorbereitet: stabile Aktualitätsanzeige nach 3.0.14

Eine bereits farbige Diagrammkachel wurde bei der nächsten normalen
Messwertrevision sofort wieder grau. `poll()` meldet solche Revisionen als
gedrosselte Aktualisierung an `WattReadBridge`; dessen Ausgabe setzte dennoch
jedes frühere Ergebnis auf `stale`, auch kurz nach erfolgreichem Empfang.
Eine verschobene 30-Sekunden-Zeitgrenze löste denselben Farbwechsel aus.

Die Korrektur unterscheidet normales Nachladen von einem ungültigen Ergebnis.
Ein vollständig geladener Stand derselben Quelle, Auswertung und Ansicht bleibt
während einer normalen Hintergrundaktualisierung bis zu 45 Sekunden farbig.
Der Hinweis „Aktualisierung im Hintergrund“ beschreibt den laufenden Abruf.
Die normale 30-Sekunden-Aktualisierung läuft weiter; der Hintergrundstand wird
nicht im Dashboard zwischengespeichert und dadurch dauerhaft festgehalten.

Die Frist zählt ab Start der tatsächlichen Abfrage, nicht erst ab Empfang einer
möglicherweise langsamen Antwort. Ein zu lange ausbleibender Folgeabruf,
Abruffehler, vom nativen Modell ausdrücklich als veraltet gemeldete Grundlagen,
Quellenwechsel und ausdrücklich ausgelöste Neuauswertung bleiben als solche
erkennbar. Unabhängig geänderte Solargrenzen erhalten keine Aktualitätsfreigabe
aus einem anderen Zeitraum. Erste noch fehlende Daten bleiben grau. Livewerte
behalten ihre eigenen Gerätefristen; die Korrektur ändert keine Messwerte,
Lernformeln, Steuerfreigaben, Layouts oder gespeicherten Farben.

## Nachweise

- Neue Ablaufprüfung reproduziert den sofortigen Farbverlust mit unverändertem
  3.0.14 und prüft anschließend 180 fortlaufende Messwertrevisionen über mehrere
  Abrufintervalle, einschließlich Antworten während neuer Revisionen.
- Weitere Prüfungen decken alle vier Energieströme, Null/unbekannt, langsame
  Antworten, Fehler und ausdrücklich ungültig gewordene Ergebnisse ab.
- Alle 347 JavaScript-Prüfungen erfolgreich, darunter 103 Prüfungen der
  Read-Bridge und Dashboardlogik.
- Die Browserprüfung `tools/energy-refresh-smoke.cjs` wurde um drei aufeinander
  folgende Aktualisierungszyklen ergänzt. Sie kontrolliert die tatsächliche
  Graustufenklasse vor und nach Antworten sowie den Übergang zu wirklich
  verspäteten Daten. Erfolgreich im Browser auf dem gesicherten Quellstand.
- Auch `tools/quick-start-smoke.cjs` erfolgreich: unabhängige aktuelle/graue
  Kacheln, unveränderte gespeicherte Hintergründe, Bildpunktprüfung der
  vollständigen Graustufen sowie Erhalt des gekennzeichneten Lernstands.
- [Erfolgreicher UI-Prüflauf](https://github.com/Tim-Gurke/AutarPie/actions/runs/44695042497),
  Quellstand `9702743ef783e3568d15645a8a9d4d29477cd9d3`. Dieser Lauf baut
  ausdrücklich keine APK. Die tatsächliche G5-/WLAN-Verbindung ist damit
  nicht vor Ort gemessen.

Tim hat am 20.11.2026 den konkret vorbereiteten Umfang zum Bau und zur
Veröffentlichung als 3.0.15 ausdrücklich freigegeben. Die endgültigen APK- und
Veröffentlichungsnachweise werden im Release und README ergänzt.
