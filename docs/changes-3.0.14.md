# AutarPie 3.0.14

Bau und Veröffentlichung des vorbereiteten Umfangs am 20.11.2026 ausdrücklich freigegeben. Die Veröffentlichung setzt einen vollständig erfolgreichen Freigabelauf voraus; dessen konkrete Nachweise stehen im Release und im README.

## Bisheriger Lernstand sofort verfügbar

Beim Öffnen liest die Anzeige ihren letzten gültigen Verbrauchslernstand je Anlage lokal, während fehlende oder geänderte Grundlagen im Hintergrund abgeglichen werden. Bereits vorhandene abgeleitete Lerndaten werden auch nach dem Update verwendet. Die Anzeige beginnt deshalb nicht jedes Mal mit einer scheinbar neuen Lernphase bei null Stunden.

Der Zwischenstand ist als **Bisheriger Lernstand … wird aktualisiert** mit Zeitpunkt gekennzeichnet. Noch nicht eingetroffene Lerndaten heißen **Lernstand wird geladen**. Eine Akku-Prognose benötigt weiterhin einen aktuellen Shunt-Ladezustand, Spannung, Kapazität und passende Wetterdaten. Ein früherer kompletter Akku-Prognosewert oder alter Shuntwert wird dadurch nicht als live ausgegeben.

Anlage, ausgewählte Gerätequelle, Lernalgorithmus und Identität des zentralen Datenbestands werden geprüft. Nach einem Anlagenwechsel oder einer ersetzten Zentralendatenbank wird ein unpassender früherer Stand verworfen. Ein fehlgeschlagener neuer Abruf behält einen gültigen Zwischenstand; erfolgreiche neue Ergebnisse ersetzen ihn. Auch ein erfolgreich leerer neuer Lernstand wird nicht durch eine verspätete alte Cache-Antwort zurückgesetzt. Der neue Zwischenspeicher enthält abgeleitete Daten und ersetzt keine Originalmessungen oder vollständige Sicherung.

## Kleine Datenportionen gemeinsam übertragen

Geänderte Modelltabellen werden in bis zu vier begrenzten Seiten je Verbindung abgefragt. Zielgröße pro Bündel 256 KiB; eine einzelne größere Zeile bleibt ungeteilt und unterliegt weiterhin dem bestehenden Übertragungsmaximum. Unveränderte Tabellen werden wiederverwendet. Nicht inkrementelle Modelltabellen behalten ihre konsistente vollständige Übernahme bei einem Versionswechsel.

Der bestehende versionierte Rohdatenabgleich ergänzt weiter nur fehlende oder geänderte Blöcke. Alte Gegenstellen bleiben über ihr bisheriges Seitenprotokoll kompatibel. Zeitaufwendige Archiv-Lückenvorprüfungen laufen getrennt vom Ausführungsthread des passiven Live-Empfangs. Eine Wetteranforderung wartet nicht mehr auf einen Gerätearchivabruf. Berechnungen verbleiben auf den Anzeigehandys oder dem eigenständig erfassenden Handy.

Der neutrale Modelltest überträgt identische 2.301 SQL-Zeilen mit drei Bündel- statt neun Seitenabrufen. Der bestehende Rohdatenvergleich mit 81.442 Messungen benötigt 102 statt 690 Abrufe. Diese Prüfungen belegen das Übertragungsverhalten, keine feste Ladezeit im WLAN und keine allgemeine Behebung physischer Victron-Verbindungsfehler.

## Nicht aktuelle Kacheln vollständig grau

Noch nicht aktualisierte Kacheln erscheinen in Graustufen: **Hintergrund, Zahlen, Skalen und Diagramm**. Textstatus und Datenstand ergänzen die Farbe. Jede Kachel kehrt unabhängig zu ihrer gespeicherten Farbgestaltung zurück, sobald ihr Abgleich abgeschlossen ist. Bereits aktuelle Kacheln bleiben normal dargestellt, während andere noch laden.

Die Kennzeichnung gilt auch für gespeicherte Rollerwerte, ausstehende Diagrammergebnisse und die Wetter-/Solarvorschau mit bisherigem oder noch fehlendem Lernstand. In der Diagramm-Großansicht bleiben ausstehende Daten ebenfalls kenntlich. Ein vollständig geladener Verlauf von gestern wird durch sein historisches Datum allein nicht als veraltet eingestuft. Die Graustufen verändern keine gespeicherten Farben, Messwerte, Kopplungen oder Layouts. Fehlende Daten bleiben unbekannt.

## Installation

1. Optional vorab die vorhandene vollständige Sicherung verwenden: **Einstellungen → Daten & Sicherung → Datensicherung · automatisch / jetzt**, auf der nativen Zentrale unter **Einstellungen und Daten**.
2. Zuerst G5/Zentrale beziehungsweise das direkt erfassende Handy auf **3.0.14 / Code 89** aktualisieren, danach beide Anzeigen. Über die vorhandene App mit der ursprünglichen Updatesignatur installieren; nicht deinstallieren und keine App-Daten löschen.
3. Der erste verfügbare frühere Lernstand wird gekennzeichnet; sein neuer Abgleich läuft weiter. Eine erstmals neu eingerichtete Anzeige ohne passenden lokalen Lernstand benötigt weiterhin ihren ersten Datenabgleich.
4. Bei **Letzter Stand** Zeitpunkt und Status prüfen. Kacheln werden nach ihrem eigenen erfolgreichen Abgleich wieder farbig. Bei einem Fehler bleiben verfügbare ältere Daten kenntlich erhalten.

Die bisherigen Lernformeln, Datenbanken, Kopplungen, persönlichen Farben und Anordnungen bleiben erhalten. Ältere Anzeigen/Zentralen behalten das bisherige Seitenprotokoll; neue Darstellung und Modellbündel benötigen das jeweilige Update. Vollständige Sicherungen bleiben mit dem Format aus 3.0.13 kompatibel. Die bereits veröffentlichte optionale tägliche Sicherung bleibt enthalten. Der Neustart des G5 mit anschließendem App-Start über Automate ist als funktionierend zurückgemeldet.

## Prüfung und Grenzen

Die Vorprüfungen umfassen sämtliche produktiven Java-Klassen, reale SQLite-Datenbanken, produktive verschlüsselte TCP-Übertragung, getrennte Anzeigen, Änderungen/Löschungen und 343 JavaScript-Prüfungen. Der neue lokale Leseweg wird zusätzlich bei absichtlich blockierter Verlaufsauswertung geprüft. Der Android-Freigabelauf umfasst Lint/APK, Android 8 und 11, private Cache-Dateien auf Android, die bisherigen Sicherungs-/Wiederherstellungsprüfungen und echte Browserdarstellung einschließlich Bildpunktprüfung grauer Kachelhintergründe. Neutrale Testwerte und Geräte-/Versionsattrappen werden in Browseraufnahmen ausdrücklich als solche verwendet.

Der tatsächliche G5, WLAN-Laufzeiten und Victron-Funkverbindungen werden dadurch nicht vermessen. Die gesamte zuvor beobachtete Wartezeit von zehn Minuten ist nicht nachträglich zeitlich aufgeschlüsselt. Vorhandene Messdaten gehen aus einer vorübergehend leeren Ansicht nicht automatisch verloren. [Technischer Vorbereitungsnachweis](quick-start-20261120.md) · [Vollständige Versionshistorie](../CHANGELOG.md).
