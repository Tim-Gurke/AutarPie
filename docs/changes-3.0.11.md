# AutarPie 3.0.11 · Wetterfarben und erhaltene Roller-Teilmengen

Tim hat diesen Umfang am 16.11.2026 mit „Bauen und veröffentlichen“ ausdrücklich freigegeben. Die passenden Quell- und Datenprüfungen werden vor dem APK-Lauf vorbereitet; veröffentlicht wird nach erfolgreicher Gesamtprüfung mit der vorhandenen Updatesignatur.

## Wetterkachel gestalten

Ein einzelner Tipp auf einen Tag öffnet weiterhin direkt die Solarvorschau beziehungsweise Wetterrückschau. Unten in dieser Ansicht führt **✎ Wetterkachel bearbeiten** zum vorhandenen Kacheleditor. Derselbe Editor bleibt über **Anordnen → Wetterkachel → Anpassen** erreichbar.

Unter **Wetter: Schrift, Balken & Symbole** lassen sich Hauptschrift/Zahlen, Nebenbeschriftungen, Symbole, Trennlinien, Bedienelemente, Auswahlmarkierungen, Solar-/Akku-/Extras-Balken und Balkenhintergrund separat färben. Ein Häkchen aktiviert die eigene Farbe; ohne Häkchen gilt die passende Vorgabe des Dashboards und Kachelhintergrunds. Hintergrund, Verlauf und Deckkraft bleiben im bisherigen Abschnitt **Farben & Hintergrund**.

Im Feld **Wettersymbol** stehen Sonne, Sonne und Wolke, Wolken, Nebel, Regen, Schnee, Gewitter und Unbekannt zur Auswahl. Die Bestandteile erhalten jeweils eigene Farben: beispielsweise Sonne und Wolke oder Wolke und Regentropfen. Die Sonne in einem kombinierten Symbol kann eine andere Farbe als das alleinstehende Sonnensymbol haben. Die Symbolvorschau verwendet die gerade bearbeiteten Farben. Hex-Eingabe, Farbauswahl und eigene gespeicherte Farben werden weiterverwendet.

Diese Farben gehören zur einzelnen Wetterkachel auf dem jeweiligen Dashboard. Übersicht, Solar-/Batterieansichten und eigene Dashboards übernehmen keine fremde Farbwahl, auch wenn ihre Kacheln dieselbe interne Kennung haben. Die aus einer Kachel geöffnete Vorschau/Rückschau übernimmt deren Farben und öffnet deren Editor. **Übernehmen** speichert; **Abbrechen** oder Zurück verwirft den Entwurf. Neustart und portable Ansichtssicherung erhalten die eigenen Farben. Alte Sicherungen und bewusst veränderte Kachelanordnungen bleiben verwendbar.

Die Rückschau stellt Solarertrag und Abend-Akkustand in getrennten Rasterzeilen dar. Bisher belegten beide Beschriftungen denselben Rasterplatz. Fehlende Werte bleiben als Strich erkennbar.

## Roller: Gemessene Teilmengen bleiben nutzbar

Eine größere Messlücke verwirft nicht mehr alle gültigen Teilabschnitte der Ladung. Die Anzeige wertet vorhandene Rohmessungen und Journale rückwirkend erneut aus. Kurze Zwischenladungen benötigen weiterhin weder einen vollen Akku noch das Erreichen des Ladeziels. Doppelte, überlappende und fremden Rollern zugeordnete Vorgänge liefern keine zusätzliche Lademenge.

Erfasste Energie, begrenzte Schätzungen kleiner Abfragelücken und unbekannte Restmengen werden getrennt geführt. Große Lücken werden nicht hochgerechnet. Der Wohnwagenanteil aus BMS-Leistung und Verlustprofil bleibt eine Schätzung, auch wenn seine zeitliche Grundlage beobachtet ist. Ursprüngliche Messwerte, Journale und Anlagenbilanz werden nicht umgeschrieben.

Die Lernanzeige nennt auswertbare Ladungen, erhaltene Teilmengen und positiv beobachtete Ladetage. Bei unbekanntem vollständigem Bedarf kann **≥ … Wh / beob. Ladetag** erscheinen. Dieser Wert ist der durchschnittliche erfasste Mindestanteil der beobachteten Ladetage, keine vollständige Tagesprognose. Fehlende Tage gelten nicht als Nullverbrauch. Zuverlässiger Tagesdurchschnitt/Ladehäufigkeit, eine ausdrücklich hinterlegte Wochenannahme und konkrete aktuelle Ladepläne bleiben getrennt. Unbekannte Energie wird nicht als vollständiger Extras-Akkubalken ausgegeben.

## Prüfungen und Grenzen

Gezielte Prüfungen verwenden produktive Java-/JavaScript-Methoden, echtes SQLite und neutrale Messdaten. Sie prüfen erhaltene Energie bei hoher Gesamtdeckung mit langer Einzellücke, zehn beobachtete Minuten in einem lückenhaften Vorgang, kurze automatische Zwischenladung, getrennte Häufigkeit und Mengen, Wiederholung ohne Verdopplung, native Sicherungsvalidierung und portable Wetterfarben.

Der vollständige Freigabelauf umfasst zusätzlich APK-Bau, Lint, bestehende Regressionstests, die tatsächlich gerenderte Wetterkachel einschließlich Mehrfarbsymbolen/Masken, Speichern/Abbrechen, Dashboardwechsel, Neustart, Export/Import und überlagerungsfreie Rückschau auf schmalem Bildschirm sowie bei 150 % Darstellung in Hoch-/Querformat. Android-8-/11-Prüfungen ersetzen keine Messung am persönlichen Handy oder echten Bluetooth-Gerät. Dieses Update enthält keine neue Änderung am Victron-Archivtransport.

## Aktualisieren

Zuerst die Zentrale beziehungsweise das direkt erfassende Handy, danach alle Anzeigen auf **3.0.11** aktualisieren. Paketkennung und ursprüngliche Updatesignatur bleiben erhalten; über die vorhandene App installieren. Messdaten, Profile, Kopplungen und Layouts behalten. Anzeigen mit 3.0.10 bleiben im Verbund kompatibel, benötigen für die neuen Farben und Lernanzeigen aber ihr eigenes Update. Keine Deinstallation oder erneute Geräteaufnahme erforderlich.

[Vorherige Änderungen 3.0.10](changes-3.0.10.md) · [Bedienung](AUTARPIE-3.md) · [Versionshistorie](../CHANGELOG.md)
