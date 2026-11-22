# Versionshistorie

Die Dokumentation bleibt vollständig erhalten, auch wenn ältere GitHub-Releases gemäß der Drei-Release-Regel entfernt werden.

## Projektpflege · 13.11.2026

Das bestehende GitHub-Repository heißt jetzt [`Tim-Gurke/AutarPie`](https://github.com/Tim-Gurke/AutarPie). Download- und Dokumentationslinks sind auf die neue Adresse umgestellt. Diese Änderung betrifft die Projektdokumentation; die veröffentlichte APK bleibt Version 3.0.0.

## Aktuelles Update

### 3.0.16 · Passiver Direkt-Liveempfang, kleinere Diagrammabgleiche und Diagnoseexport

Anzeigehandys können die Sofortanzeige ihrer Victron-Geräte optional direkt mithören, ohne aktive Verbindung oder zusätzliche Messhistorie. GPS, durchgehende Aufzeichnung, Archive, Roller-BMS und bestätigte Steuerung bleiben bei der Zentrale. Bei fehlendem Direktempfang gelten wieder die verfügbaren Zentralwerte und deren ursprüngliche Zeitstempel.

Kurze Diagrammzeiträume erhalten begrenzte zeitabhängige Modellgrundlagen; langfristiges Lernen bleibt getrennt erhalten. Geprüfte Modellkennungen erlauben die Wiederverwendung kurz aufeinanderfolgender Abgleiche. Aktuelles Wetter wartet nicht auf einen vollständigen Lernstand; noch nicht aktuelle Batterieprognosen bleiben einzeln gekennzeichnet. Die Diagnose wird unabhängig von der Berechnungswarteschlange exportiert und bei Analysefehlern als lesbare Teildiagnose abgeschlossen. [Bedienung, Sicherung vor ROM-Wechsel und Prüfgrenzen](docs/changes-3.0.16.md).

### 3.0.15 · Stabile Kachelfarben beim normalen Nachladen

Am 20.11.2026 ausdrücklich zum Bau und zur Veröffentlichung freigegeben. Ein gerade erfolgreich geladener Diagrammstand derselben Ansicht bleibt während normaler Hintergrundaktualisierung farbig. Neue Live-Revisionen und die nächste Zeitgrenze machen ihn nicht sofort wieder grau. Die normale Aktualisierung läuft weiter, mit Hinweis „Aktualisierung im Hintergrund“.

Erste fehlende Daten, ausdrücklich ungültige Grundlagen, Abruffehler und tatsächlich verspätete Folgeabrufe bleiben grau erkennbar. Die begrenzte Aktualitätsfrist zählt ab Abfragestart; langsame Antworten erhalten keine neue volle Frist beim Eintreffen. Anlagen-/Quellenwechsel und unabhängige Solarzeitgrenzen bleiben getrennt. Messwerte, Lernformeln, Steuerfreigaben, Kopplungen, Farben und Layouts bleiben erhalten. [Installation und Prüfgrenzen](docs/changes-3.0.15.md).

### 3.0.14 · Sofortiger Lernstand, begrenzte Modellpakete und graue Zwischenstände

Am 20.11.2026 ausdrücklich zum Bau und zur Veröffentlichung freigegeben. Beim Öffnen den letzten gültigen Verbrauchslernstand je Anlage lokal lesen, während neue Historien- und Modellgrundlagen abgeglichen werden. Bisherigen Stand mit Zeitpunkt kennzeichnen; anfänglich wartende Lerndaten nicht als null ausgewertete Stunden darstellen. Akku-Prognosen benötigen weiterhin frische Shuntwerte.

Geänderte Modelltabellen werden in begrenzten Seiten gebündelt übertragen. Bestehende Rohdatenblöcke bleiben wiederverwendbar, alte Gegenstellen kompatibel. Die Archiv-Lückenvorprüfung läuft getrennt vom passiven Live-Empfang; Wetteranforderungen warten nicht mehr auf das Gerätearchiv.

Noch nicht aktuelle Kacheln einschließlich Hintergrund, Zahlen, Skalen und Diagramm erscheinen in Graustufen mit Textstatus. Jede Kachel kehrt unabhängig zu ihren gespeicherten Farben zurück. Geladene historische Daten werden durch ihr Datum allein nicht als veraltet eingestuft. [Installation, Bedienung und Prüfgrenzen](docs/changes-3.0.14.md).

### 3.0.13 · Archiv, Anzeigeabgleich, Startdarstellung und Datensicherung

Am 17.11.2026 für den vollständigen Umfang zum Bau und zur Veröffentlichung freigegeben. Die Archiv-Bestandsprüfung verwendet begrenzte SQL-Seiten und eine frische Wiederanmeldung nach einem vorübergehenden Anmeldefehler. Anzeigen übertragen mehrere unabhängig geprüfte Rohdaten-Seiten je Netzwerkaufruf und protokollieren Verbindung, Übertragung, Speicherung und Berechnung getrennt. Bereits vorhandene Blöcke bleiben erhalten; das ältere Protokoll bleibt kompatibel.

Leere Neuinstallationen starten mit der gewünschten Darstellung: acht Bereiche, 21 Übersichtskacheln, 75 % Größe, eigene Wetterfarben und Kurzwahlreihenfolge. Bestehende Installationen behalten ihre Darstellung und Kopplungen.

Optional täglich vollständige verschlüsselte Sicherungen mit frei gewählter Ortszeit, lokalem oder unterstütztem Online-Ordner, geschütztem Kennwort, zwei bis 30 Ständen, „Jetzt sichern“, Status und geprüfter Wiederherstellung. Anfangs ausgeschaltet. Neue Sicherungen übertragen App-Geräteschlüssel innerhalb der Verschlüsselung und verschlüsseln sie beim Import im Ziel-AndroidKeyStore neu. Aufwendige Auswertungen bleiben auf den Anzeigen. [Bedienung und Prüfgrenzen](docs/changes-3.0.13.md).

Die Freigabe ist dokumentiert; eine Veröffentlichung erfolgt nach bestandener APK-/Android-Gesamtprüfung. Die konkreten Nachweise stehen im Release und im README.


### 3.0.12 · Archiv-Bluetooth vom Hauptthread trennen

Android-Bluetooth-Aufträge und ihre Rückmeldungen im Archivdienst verwenden einen eigenen HandlerThread. Ein beschäftigter Hauptthread blockiert damit nicht mehr Diensterkennung, Schreibbestätigungen und den bestehenden seriellen Sitzungserhalt. Abbruch und Freigabe des GATT-Clients bleiben erhalten. Die lokale Bestandsplanung verlängert zusammenhängende Messintervalle ohne unnötiges Entfernen und erneutes Einfügen; vorhandene Daten und echte Lücken werden unverändert erkannt.

Direkter Vergleich mit 3.0.11 bei blockiertem Hauptthread, 114 gezielte Transportprüfungen, 45 Vergleichsprüfungen mit 240.000 Punktentscheidungen und neun SQLite-/Fortsetzungsanforderungen lokal bestanden. Tim hat diesen konkreten Reparaturumfang am 16.11.2026 zum APK-Bau und zur Veröffentlichung freigegeben. Ein Softwaretest beweist weder die Ursache jedes Status-19-Abbruchs noch reale Funkstabilität oder die gesamte Ladezeit der Anzeigehandys. [Aktualisieren und Prüfgrenzen](docs/changes-3.0.12.md).

## Veröffentlichte Versionen

### 3.0.11 · Wetterfarben und erhaltene Roller-Teilmengen

Wetterkacheln je Dashboard unabhängig gestalten: Haupt-/Nebenbeschriftungen, Zahlen, Bedienung, Linien, Balken und einzelne Bestandteile jedes Wettertyps. Gemeinsamer Kacheleditor mit Symbolvorschau, Speichern/Abbrechen und portabler Sicherung. Ein Tag öffnet weiterhin sofort seine Details; dort zusätzlich „Wetterkachel bearbeiten“. Rückschau mit getrennten Solar-/Akkuzeilen statt übereinanderliegenden Werten.

Gültige Roller-Teilmengen bleiben auch bei größeren Messlücken erhalten und werden aus vorhandenen Rohdaten neu gelernt. Fehlende Energie bleibt unbekannt; ein beobachteter Mindestanteil je Ladetag wird klar von einem vollständigen Tagesbedarf und sicherer Ladehäufigkeit unterschieden. Kurzladungen, Journal und Anlagenbilanz bleiben erhalten. Am 16.11.2026 ausdrücklich zum Bau und zur Veröffentlichung freigegeben. [Bedienung und Prüfgrenzen](docs/changes-3.0.11.md).

### 3.0.10 · Gerätewechsel, stabile Wetterbalken und kurze Rollerladungen

Nach dem Schließen eines GATT-Clients vor der nächsten Verbindung eine kurze Freigabephase einhalten, auch über neue Archivdienst-Instanzen hinweg. Ausschließlich beim anfänglichen Verbindungsfehler 133 mit einem frischen Client höchstens dreimal verbinden; weitere Versuche nach begrenzter Wartezeit. Abbruch beendet die Wartephase. Alte Rückrufe bleiben vom neuen Client getrennt, bestehende Kopplungen und bereits gespeicherte Daten erhalten.

Am Ende das Ergebnis aller Geräte gemeinsam zeigen und in der Diagnose erhalten. Ein fehlgeschlagener zweiter Abruf mit 0 Werten verdeckt die Werte des erfolgreichen ersten Geräts nicht mehr. Geräte-Diagnosen erhalten absolute Beginn-/Endzeiten und die Zahl der Verbindungsversuche.

Den bekannten Abend-Akkustand in „Heute“ während einer Hintergrundaktualisierung erhalten und den laufenden beziehungsweise gestörten Abruf kennzeichnen. Erfolgreich leere Antworten und ein Anlagen-/Shuntwechsel übernehmen keinen alten Messpunkt. Kurze Roller-Zwischenladungen unabhängig vom Ladeziel lernen; begrenzte Abfragelücken anteilig als Schätzung ergänzen und beobachtete AUS-Zeiten bei der automatischen Enderkennung berücksichtigen. Ladehäufigkeit aus ausreichend überwachten Tagen statt aus unmöglich exakt lückenlosen Abfragezeiten ableiten. Letzte aufgezeichnete und zuletzt zum Lernen verwendete Ladung sowie Ausschlussgründe unterscheiden. Journal-Messwerte bleiben erhalten.

109 Archiv-Prüfschritte, 323 JavaScript-Prüfungen und 35 native Roller-Prüfschritte plus Abgleich mit der produktiven Anzeige erfolgreich. Der konkrete Umfang wurde am 15.11.2026 ausdrücklich zum APK-Bau und zur Veröffentlichung nach erfolgreicher Gesamtprüfung freigegeben. Die native Ursache eines realen Status 133 ist durch die begrenzte Wiederholungsstrategie nicht bewiesen. [Änderungen und Prüfgrenzen](docs/changes-3.0.10.md).

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
