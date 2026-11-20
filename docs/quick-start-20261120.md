# Vorbereitung: erster Lernstand und begrenzte Datenübertragung

Stand: 20. November 2026. Vorbereitungsbranch `prepare/quick-start-20261120`, ausgehend von der veröffentlichten 3.0.13. Noch keine neue APK, kein neuer APK-bauender CI-Lauf und keine Veröffentlichung. Der APK-Bau benötigt die vereinbarte ausdrückliche Freigabe für diesen Umfang.

## Verhalten

- Den letzten erfolgreich ermittelten Verbrauchslernstand privat je Anlage über App-Prozesse erhalten. Beim Öffnen getrennt von der Warteschlange für Netzwerk-/Verlaufsabfragen lesen. Bestehende Installationen können dafür zuerst ihr bereits abgeleitetes SQLite-Lernjournal lesen; kein erneuter Rohdatenimport für diesen ersten Stand.
- Den früheren Verbrauchslernstand als „Bisheriger Lernstand … wird aktualisiert“ mit Zeitpunkt kennzeichnen. Die Akku-Prognose verwendet weiterhin aktuelle Shuntwerte und die verfügbaren Wetterdaten. „Lernstand wird geladen“ ersetzt die scheinbare Null-Messzeit, solange noch kein Lernstand eingetroffen ist.
- Anlage, Gerätequelle, Lernalgorithmus und Identität des zentralen Datenbestands prüfen. Veraltete Antworten dürfen neuere erfolgreiche Ergebnisse, einschließlich erfolgreicher leerer Ergebnisse, nicht überschreiben. Fehler behalten einen gültigen früheren Stand. Unpassende, beschädigte oder zu große Zwischenspeicher werden verworfen; die Originaldaten bleiben erhalten.
- Geänderte Modelltabellen in bis zu vier begrenzten Seiten je Verbindung übertragen. Zielgrenze 256 KiB; eine einzelne größere Tabellenzeile bleibt ungeteilt und unterliegt weiterhin dem Übertragungsmaximum. Alte Gegenstellen verwenden weiterhin das bisherige Seitenprotokoll. Versionsprüfung und vollständige Installation einer Tabelle bleiben erhalten. Das bestehende blockweise Rohdatenprotokoll überträgt weiter nur fehlende oder geänderte Blöcke.
- Die zeitaufwendige Vorprüfung fehlender Gerätearchivdaten erhält eine eigene Arbeitswarteschlange. Der passive Live-Empfang wartet dadurch nicht auf denselben Ausführungsthread. Fachliche Lücken-/Kanalentscheidungen und Archivabstände werden beibehalten. Die Wetteranforderung wartet nicht mehr auf das Gerätearchiv.
- Noch nicht aktualisierte Kacheln einschließlich Hintergrund, Zahlen, Skalen und Diagrammen in Graustufen anzeigen. Sichtbarer Status und Zeitpunkt ergänzen die Farbe. Jede Kachel kehrt nach ihrem erfolgreichen Abgleich zur eigenen gespeicherten Gestaltung zurück; andere wartende Kacheln bleiben grau. Bereits vollständig geladene Rückschau wird durch ihr historisches Datum allein nicht als veraltet eingestuft. Großansichten markieren wartende Diagramme ebenfalls. Die Wetterkachel kennzeichnet einen noch vorherigen/fehlenden Lernstand oder veraltetes Wetter.

Die Graustufen sind eine reine Darstellung. Sie verändern keine gespeicherten Farben, Layouts, Messwerte oder Berechnungsgrundlagen. Fehlende Live-Grundlagen bleiben für Prognosen und Steuerung unbekannt. Eine Anforderung historischer Daten ist kein Nachweis ihrer bereits vollständigen Übertragung.

## Prüfungen am Vorbereitungsstand

- Alle produktiven Java-Quellen mit API-35-Klassen und den vorhandenen JVM-Adaptern kompiliert. `tools/prove-lan.py` vollständig erfolgreich: 508 Prüfaussagen über reale SQLite-Datenbanken, produktive verschlüsselte TCP-Übertragung, getrennte Anzeigen, Änderungen/Löschungen, Lern-/Anwesenheitsgrundlagen, Sicherung sowie den neuen lokalen Vorschaupfad. Android-Kontext, Uhr und Lebenszyklus sind dabei Testadapter.
- Neuer `QuickStartProof`: persistierter Lernstand, bestehendes SQL-Journal, neue Anlage/Quelle/Bestandsidentität, Algorithmuswechsel, beschädigte/zu große Datei, Fehlererhalt und erfolgreiche Leerung. Lokaler Vorschauabruf bleibt auch bei absichtlich gehaltenem Monitor der Verlaufsauswertung verfügbar.
- Modell-Seitenvergleich mit 2.301 neutralen SQL-Zeilen: gleicher vollständiger Inhalt, neun einzelne Seitenabrufe gegenüber drei Bündelabrufen; Seiten-/Bytegrenzen und Versionsänderung geprüft.
- Bestehender produktiver TCP-Vergleich mit 81.442 neutralen Originalmessungen: gleiche Energiewerte bei 102 gebündelten gegenüber 690 alten Abrufen. Das ist eine Protokollmessung im Prüfaufbau, keine gemessene WLAN-/G5-Geschwindigkeit.
- 342 JavaScript-Prüfungen erfolgreich. Darunter verspätete Cache-Antworten, Pflanzen-/Shunt-/Bestandswechsel, erfolgreicher leerer Lernstand, erhaltene Sonnenuntergangsbalken und kachelweiser Wechsel zwischen wartendem und aktuellem Darstellungsstatus. Der produktive Statusmarker wird mit DOM-Adaptern ausgeführt.

Noch ausstehend: Android-Bau/Lint und tatsächliche Oberflächenaufnahmen nach Bau-Freigabe; Darstellung auf den Handys sowie reale G5-/WLAN-/Victron-Laufzeiten. Die gesamte beobachtete Wartezeit von zehn Minuten ist damit nicht nachträglich vermessen. Keine vollständige Hardwareabnahme aus diesen Softwareprüfungen ableiten.

Der automatische G5-Neustart mit anschließendem App-Start über Automate wurde inzwischen als funktionierend zurückgemeldet und gehört nicht mehr zu den offenen Fehlerpunkten.
