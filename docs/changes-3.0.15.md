# AutarPie 3.0.15

## Stabile Farben während normaler Aktualisierung

Ein frisch geladenes Diagramm wird beim nächsten normalen Datenabruf nicht mehr
sofort wieder schwarz-weiß. Ein vollständiger Stand derselben Ansicht bleibt
während der Hintergrundaktualisierung bis zu 45 Sekunden farbig. Der Hinweis
„Aktualisierung im Hintergrund“ beschreibt den laufenden Abruf. Neue Livewerte
und die nächste 30-Sekunden-Zeitgrenze lösen keinen sofortigen Farbwechsel aus.

Der Abruf läuft weiter. Die Frist beginnt beim Abfragestart, sodass eine langsam
eintreffende Antwort keinen alten Stand erneut als frisch ausgeben kann.
Tatsächlich verspätete oder fehlgeschlagene Folgeabrufe, fehlende Erstdaten und
vom Modell ausdrücklich als veraltet erkannte Grundlagen bleiben grau mit
Textstatus und Datenstand. Ein Quellenwechsel und eine ausdrücklich gestartete
Neuauswertung verlangen weiterhin die dafür passenden neuen Daten.

Die Korrektur betrifft die Anzeige. Messwerte, Lernformeln, Steuerfreigaben,
gespeicherte Farben, Kopplungen und Kachelanordnungen bleiben erhalten.

## Installation und Kompatibilität

1. Vorhandene Installation auf **3.0.15 / Code 90** aktualisieren. Die APK
   verwendet die ursprüngliche Updatesignatur. Nicht deinstallieren und keine
   App-Daten löschen.
2. Zuerst G5/Zentrale beziehungsweise das direkt erfassende Handy, danach
   S20 und S10/Anzeigen aktualisieren. Die sichtbare Korrektur benötigt das
   Update auf jedem Anzeigehandy und im eigenständigen Betrieb.
3. Beim normalen Nachladen bleibt ein jüngst vollständig geladener Stand
   farbig. Bei tatsächlich fehlenden oder verspäteten Daten den eingeblendeten
   Zeitpunkt beziehungsweise Fehler prüfen.

Zentralen und Anzeigen mit 3.0.14 bleiben über das bestehende Protokoll
kompatibel. Die native Zentrale erhält in dieser Version keine neue
Berechnungsaufgabe. Vollständige Sicherungen bleiben zum Format ab 3.0.13
kompatibel; die freiwillige tägliche Sicherung bleibt enthalten.

## Prüfung und Grenzen

347 JavaScript-Prüfungen und die gezielten Browserprüfungen haben den
vorbereiteten Funktionsstand bestätigt. Der Ablauf wird über 180 fortlaufende
Revisionen sowie mehrere aufeinanderfolgende Aktualisierungszyklen geprüft,
einschließlich Antworten während neuer Revisionen, verspäteter Daten und
Fehlern. Ganze Kacheln und deren Hintergründe werden zusätzlich per
Bildpunktprüfung auf Graustufen kontrolliert.

Der vollständige Android-Freigabelauf prüft zusätzlich APK-Bau, Lint,
Java-/SQLite-/Übertragungsprüfungen, Oberflächen und Wiederherstellung auf
Android 8 und 11. Die konkreten Ergebnisse des gebauten Quellstands stehen im
Release und README. Die tatsächliche G5-/WLAN-/Victron-Verbindung wurde hier
nicht vor Ort gemessen.

[Technischer Nachweis](stable-tile-freshness-20261120.md) ·
[Vollständige Versionshistorie](../CHANGELOG.md)
