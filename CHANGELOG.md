# Versionshistorie

Die Dokumentation bleibt vollständig erhalten, auch wenn ältere GitHub-Releases gemäß der Drei-Release-Regel entfernt werden.

## Projektpflege · 13.11.2026

Das bestehende GitHub-Repository heißt jetzt [`Tim-Gurke/AutarPie`](https://github.com/Tim-Gurke/AutarPie). Download- und Dokumentationslinks sind auf die neue Adresse umgestellt. Diese Änderung betrifft die Projektdokumentation; die veröffentlichte APK bleibt Version 3.0.0.

## Vorbereitet, noch nicht gebaut

### 3.0.10 · Gerätewechsel und gemeinsames Archivergebnis

Nach dem Schließen eines GATT-Clients vor der nächsten Verbindung eine kurze Freigabephase einhalten, auch über neue Archivdienst-Instanzen hinweg. Ausschließlich beim anfänglichen Verbindungsfehler 133 mit einem frischen Client höchstens dreimal verbinden; weitere Versuche nach begrenzter Wartezeit. Abbruch beendet die Wartephase. Alte Rückrufe bleiben vom neuen Client getrennt, bestehende Kopplungen und bereits gespeicherte Daten erhalten.

Am Ende das Ergebnis aller Geräte gemeinsam zeigen und in der Diagnose erhalten. Ein fehlgeschlagener zweiter Abruf mit 0 Werten verdeckt die Werte des erfolgreichen ersten Geräts nicht mehr. Geräte-Diagnosen erhalten absolute Beginn-/Endzeiten und die Zahl der Verbindungsversuche. 109 lokale Quellprüfschritte erfolgreich; noch kein APK-Bau oder APK erzeugender CI-Lauf gestartet. Der konkrete Umfang wartet auf Bau-Freigabe. Die native Ursache eines realen Status 133 ist durch die begrenzte Wiederholungsstrategie nicht bewiesen. [Änderungen und Prüfgrenzen](docs/changes-3.0.10.md).

## Veröffentlichte Versionen

### 3.0.9 · Bluetooth-Verarbeitung während blockierender Archivprüfung

Eigener serieller Bluetooth-Arbeitsablauf für GATT-Aufträge, Paketverarbeitung, Empfangsfreigaben und Sitzungserhalt. Die lokale Archivprüfung läuft getrennt und kann den Empfang nicht mehr durch eine blockierende SQL-Abfrage anhalten. Abbruch und Vorrang manueller Schaltaufträge schließen die Verbindung sofort; geschlossene oder fehlgeschlagene Schreibaufträge dürfen spätere Antworten nicht fälschlich bestätigen. Diagnose mit relativen Schrittzeiten, SQL-Laufzeiten, Android-Wartezeiten und tatsächlichen Sitzungsabständen. Empfangsfenster, lokale Bestandsprüfung, vollständige Abschlusskontrolle und bisher gespeicherte Messdaten bleiben erhalten.

Der konkrete Umfang wurde am 15.11.2026 zum Bau und zur Veröffentlichung nach erfolgreicher Gesamtprüfung freigegeben. Der neue Nachweis sperrt echtes SQLite und verwendet die unveränderten produktiven GATT-Schreib-, Callback-, Empfangs- und Abfragemethoden mit simuliertem Android-Transport. Der Vergleich reproduziert den Ausfall des bisherigen Ablaufs unter dieser kontrollierten Belastung; er bestimmt nicht den Firmware-Auslöser eines realen Status 19. [Umfang, Nachweis und Prüfgrenzen](docs/changes-3.0.9.md).

### 3.0.8 · Archivempfang, drei Lader, alte BMS-Werte und Gaslernen auf Reisen

- Empfangsfreigaben erst nach Verarbeitung der Bluetooth-Pakete zurückgeben, damit langsamer lokaler Archivabgleich den Empfangspuffer nicht füllt. Transportfehler, manuellen Abbruch und Vorrang eines Schaltauftrags unterscheiden.
- Gescheiterte Aufzeichnungsstarts mit ihrem Grund statt als freiwillige Pause dokumentieren; Android-Freigaben und letzten Startfehler exportieren und fehlende Hintergrundfreigabe in der Zentrale sichtbar machen.
- Allgemeine Victron-Liveanzeige beim App-Namen auf allen Dashboards; pro konfiguriertem Gerät prüfen. Eigene 608PD-Statusanzeigen beibehalten.
- Drei separate 608PD-Profile mit unverändert übernommener bestehender Verbindung, begrenzter Suche am erfassenden Handy, eigener verschlüsselter Kennung und bestätigter Bedienung je Adresse. Neue direkte Ersteinrichtung benötigt noch praktische Bestätigung am 608PD.
- Rohbeobachtungen, App-Aufträge und Funklücken der Lader dauerhaft speichern, zu Anzeigen replizieren und dort zu einem Lade-/Powerbankjournal mit ausschließlich beobachteter Akkuseitenenergie auswerten. JSON-Export und vollständige Sicherung erhalten.
- Veraltete Rollerwerte deutlich grau, mit sichtbarer Alt-Markierung und Messzeitpunkt zeigen. BMS-Zeitstempel auf der Anzeige prüfen, auch bei noch positivem zwischengespeichertem Frischestatus und während des Wartens auf Synchronisation. Neue aktuelle Werte heben die Markierung wieder auf.

- Gas-Wetterlernen mit nach Aufenthaltsdauer gewichtetem Ortswetter auch bei Ortswechseln und Zwischenstopps. Begrenzte GPS-Nachführung am erfassenden Handy, stündlicher Wetterrückblick einschließlich verlassener Orte und getrennte Anzeige fehlender Ortszeiten beziehungsweise Wetterdaten. Alle Flaschenwechsel bleiben erhalten; reine Temperaturmodelle benötigen keine vollständigen Windwerte.

[Änderungen, Bedienung und Prüfgrenzen](docs/changes-3.0.8.md).

### 3.0.7 · Vollständige 608PD-Messwerte nach der Anmeldung

Bluetooth-Paketgröße vor Diensterkennung und Anmeldung asynchron aushandeln, damit auch Leerlauf- und vollständige Sechszell-Antworten übertragen werden können. Zu kleine, abgelehnte oder ausbleibende Bestätigungen sowie Dienstabfragefehler gezielt melden; bei Wiederverbindung neu verhandeln und alte Rückrufe ignorieren. Vorhandene private Verbindung und Daten erhalten. Neutraler Testpartner reproduziert den alten Leerlauf-Timeout und prüft den produktiven Adapter mit 57 neuen Prüfungen. Reale Funkverbindung nach dem Update vor Ort bestätigen. [Update, Bedienung und Prüfgrenzen](docs/changes-3.0.7.md).

### 3.0.6 · Rollerlernen bei Abwesenheit und gemeinsame Kurzwahlfarbe

Energie je erfasster Ladung unabhängig vom ganztägigen BMS-Empfang lernen. Ladehäufigkeit aus vollständig überwachten Ladeweg-Tagen ableiten; unbekannte Häufigkeit mit ausdrücklich gekennzeichneter Wochen- oder eigener Tagesannahme überbrücken. Bestehende Journale wiederverwenden, doppelte/zusammengehörige Fragmente berücksichtigen, unvollständige und externe Ladung ausschließen, Relaisenergie getrennt halten und nach Rückkehr konkrete Ladeplanung einmalig einsetzen. Alle ausgewählten Kurzwahlen einschließlich eigener Dashboards verwenden die gemeinsame App-Akzentfarbe, auch nach Umordnung und bei abweichendem Dashboard-Design. [Installation, Bedienung und Prüfgrenzen](docs/changes-3.0.6.md).

### 3.0.5 · Vorhandenen Verlauf vollständig fürs Verbrauchslernen abgleichen

Auch bei bereits importierten Lernabschnitten den älteren Rohverlauf einmalig abrufen und neu bewerten. Gespeicherte stündliche Einstrahlung am damaligen Standort und mit damaliger Geometrie wiederverwenden. Ausschlussgründe je Anwesenheitsprofil erhalten, ältere nicht zuordenbare Gründe getrennt zeigen; lokalen Archivumfang sichtbar machen. Erneuten historischen Lernabgleich und Diagnoseexport der gekoppelten Anlage auf Anzeigehandys unter Daten & Sicherung anbieten. Eigene Handydaten, Layouts und bestehende Erkenntnisse erhalten. [Update, Bedienung und Prüfgrenzen](docs/changes-3.0.5.md).

### 3.0.4 · Lerndaten erhalten, frühere Wetterkorrektur und manueller Relaisbetrieb

Geänderte Messdaten löschen nicht mehr unwiederbringlich frühere Lernabschnitte: betroffene Blöcke gezielt neu bewerten, nicht neu berechenbare Erkenntnisse erhalten und sichtbar aufschieben, Datengrundlage im offenen Dialog aktualisieren. Beobachtete Roller-Nutzungstage auch bei zentraler Erfassung auf den Anzeigen auswerten. Letzten BMS-Datenstand bei fehlender Reichweite mit Zeitpunkt erhalten; Steuerung weiterhin nur mit frischen Daten. Relaisbetrieb ausdrücklich manuell wählbar, direkter Ladeweg bleibt Standard; historischen Wirkungsgrad als Referenz übernehmen, Spulenmessung und geschätzten Ladebedarf unterscheiden. Vorläufige Gas-Temperaturkorrektur nach erstem geeigneten Flaschenabschnitt, spätere persönliche Modellprüfung. Symbol-Kurzwahlen, neue Darstellungsvorgabe für neue AutarPie-Installationen, Solarertrag und gemessener Abend-Akkustand in der Wetterrückschau. Android-10/11-Hintergrundberechtigung und BLE-Neustart verbessern den gespeicherten Autostart. [Installation, Bedienung und Prüfgrenzen](docs/changes-3.0.4.md).

### 3.0.3 · Dashboardfarben eindeutig zuordnen

Der Verlauf verwendet sein eigenes Design oder die gemeinsamen Vorgaben, unabhängig von der zuvor geöffneten Seite. Der Farbeditor bearbeitet die sichtbare Seite; Speichern und „auf alle Kacheln anwenden“ verändern weder das zuletzt geöffnete andere Dashboard noch seine Kachelanordnung. Diagramme verwenden die wirksamen Seitenfarben. Fehlgeschlagenes Speichern lässt sich für dieselbe Seite wiederholen. Mit tatsächlichen Seitenwechseln, Speicherung, Neustart und Hoch-/Queransicht gegen Rückfälle geprüft. Zentralen-Hotfix 3.0.2 bleibt enthalten. [Update und Prüfgrenzen](docs/changes-3.0.3.md).


### 3.0.2 · Zentrale reagiert wieder

Hotfix für den in 3.0.1 eingeführten Deadlock zwischen Reglerabfrage und Repository-Snapshot. Gerätekonfiguration ohne gegenseitige Monitorsperre lesen, Lastarchivierung außerhalb der gemeinsamen Livewertsperre ausführen und die native Regleranzeige im Hintergrund abfragen. Nachgestellter 3.0.1-Fehler, paralleler Regressionstest und zusätzlicher Android-Test für einen belegten Regler. Bestehende Daten, Kopplungen, Layouts und Updatesignatur bleiben erhalten. [Ursache, Installation und Prüfung](docs/changes-3.0.2.md).

### 3.0.1 · Anzeige, Messkacheln und Wiederherstellung

Begrenzte Neuberechnungen und stabile Diagramme; Wetterrückschau, 16-Tage-Vorschau und optionaler Langfristtrend; selektives Zurücksetzen/Wiederherstellen; Farbpaletten und Dashboard-Design; sichtbare Kurzwahlreihenfolge, Menü-/Live-Position und Android-Zurück. Neue Roller-/Zell-/Reichweiten-/Wirkungsgrad- und Formel-Kacheln, mehrere getrennte Regler-Lastausgänge, priorisierte Schaltwünsche, ausführliches Ladejournal und dauerhafte Offline-Versorgungseinträge. Bestätigte BMS-Rückgabe und zeitweilige Verbindungsfehler abgesichert. Präziseres schlankes Logo, Originalpaket und Updatesignatur erhalten. [Umfang, Update und Prüfgrenzen](docs/changes-3.0.1.md).

### 3.0.0 · AutarPie

Großes Cockpit-Update mit Ressourcen-Kreislauf-Symbol, frei wählbaren Bereichen, eigenen Dashboards, gemeinsamen Farben/Verläufen und bestätigter Kachel-/Größenbearbeitung. Native Zentralenoberfläche, Berechnung auf Anzeigen/eigenständigen Geräten, zeitlich begrenzte Überschusspläne, Lastausgang und 608PD-Grundintegration, gemeinsame Solareinrichtung mit stabilen Kreiskennungen und gemeinsamen Reglergrenzen, PV-Spannung, Phasenzeitleiste und Autostart. Paket und Updatesignatur bleiben erhalten. [Umfang, Update und Prüfgrenzen](docs/changes-3.0.0.md).


| Version | Änderungen |
|---|---|
| [2.0.3](docs/changes-2.0.3.md) | Vollständige Statistikstände bei wechselnden Grundlagen erhalten und aktualisieren; sichtbare Kacheln vor dem Lernabgleich laden. Fehler mit echter TCP-/SQLite-Strecke und tatsächlicher Oberfläche nachgestellt. |
| [2.0.2](docs/changes-2.0.2.md) | Gas-/Wasserbedarf in Flaschen und Kanistern, einstellbare Behältergrößen, sichtbarer Wetter-Lernstand und passende Symbole. WohnPit klar als Zusammenführung von Watt’n’Pit und der bisherigen Zentrale gekennzeichnet; öffentliche Vorgeschichte und Anleitungen hier erhalten. |
| [2.0.1](docs/changes-2.0.1.md) | Ersten Statistikabruf auf Anzeigehandys bei laufender Aufzeichnung repariert; vollständige Ergebnisse sichtbar behalten und wartende Diagramme gleichmäßig laden. |
| [2.0.0](docs/changes-2.0.0.md) | WohnPit und Watt’n’Pit zusammengeführt; Zentrale, Anzeigehandy und eigenständiger Victron-Betrieb, private Vollsicherung, gemeinsame Messwertfarben und belegter Wetterrückblick. |
| [0.0.5](docs/changes-0.0.5.md) | Private vollständige Übernahmesicherung für den Wechsel zur gemeinsamen WohnPit-App. |
| [0.0.4](docs/changes-0.0.4.md) | Gemeinsames Gas-/Wasserjournal, Wetterrückblicke, Temperatur-/Winddaten und Statusanzeige; Bedienung und Auswertung in Watt’n’Pit ab 1.9.0. |
| [0.0.3](docs/changes-0.0.3.md) | Gespeicherte Lernabschnitte und Prognosevergleiche aus unterstützten Watt’n’Pit-Diagnosen übernehmen. |
| [0.0.2](docs/changes-0.0.2.md) | Historische Verbrauchsgrundlagen und Anlagen-/Wetterkontexte beim Umzug ergänzen. |
| 0.0.1 | Eigenständige native Zentrale aus Watt’n’Pit 1.8.2: Messwerte, Archive, verschlüsselter Anlagenverbund und vorhandene BMS-Übergabe. |

Die frühere [Watt’n’Pit-Versionshistorie](docs/wattnpit-archiv/CHANGELOG.md) bleibt vollständig erhalten und wird ab WohnPit 2.0.0 hier gemeinsam fortgeführt.
