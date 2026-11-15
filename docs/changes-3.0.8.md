# AutarPie 3.0.8 · Vorbereitung vor der Bau-Freigabe

Dieser Quellstand ist vorbereitet. Es wurde noch keine 3.0.8-APK gebaut oder veröffentlicht. Vor Beginn des APK-Baus einschließlich Android-CI wird Tim um Freigabe des konkreten Umfangs gebeten. Die bisherige Veröffentlichung bleibt 3.0.7.

## Victron-Archiv und Aufzeichnungsstart

Während des lokalen Archivabgleichs wurden Empfangsfreigaben bereits beim Eintreffen statt nach dem Verarbeiten der Pakete zurückgegeben. Ein weiter sendendes Gerät konnte dadurch den Empfangspuffer füllen. Dieser Transportfehler wurde zusätzlich irreführend als abgebrochener Import mit 30 Minuten Pause angezeigt.

Freigaben folgen nun dem tatsächlich freigewordenen Pufferplatz. Der Regler kann seine Übertragung während langsamer lokaler Verarbeitung anhalten und danach fortsetzen. Der Sitzungs-Wachhalter läuft unabhängig weiter. Ein dennoch überlaufender Puffer wird als konkreter Transportfehler gemeldet; nur ein ausdrücklicher Benutzerabbruch löst die entsprechende 30-Minuten-Pause aus. Ein manueller Schaltauftrag erhält eine eigene Unterbrechungsmeldung. Bereits gespeicherte Archivblöcke bleiben erhalten.

Nach einem gescheiterten Dienststart wird der tatsächliche Startfehler gespeichert und protokolliert. „Aufzeichnung pausiert“ wird nicht mehr pauschal für einen gar nicht begonnenen Start geloggt. Der native Diagnoseexport enthält zusätzlich Android-Version, Bluetooth-/Standortfreigaben, Standortdienst, Entsperrstatus, Batterieoptimierungs-Ausnahme, gewünschten und laufenden Aufzeichnungszustand sowie den letzten Startfehler mit Zeitpunkt. Öffnen der App entfernt diesen früheren Fehlerbeleg nicht.

Auf Android 10/11 bleibt für den Hintergrundstart die Freigabe **Berechtigungen → Standort → Immer zulassen** erforderlich. Die native Zentrale zeigt den fehlenden Zustand dauerhaft mit einem direkten Einstellungszugang. Diese Android-Freigabe kann die App nicht selbst erteilen. Ein echter Neustart des G5 muss anschließend vor Ort geprüft werden; ein erfolgreicher APK-Bau beweist keinen funktionierenden nächtlichen Neustart.

## Liveanzeige und alte Rollerwerte

Die allgemeine Anzeige sitzt beim App-Namen und bezeichnet ausdrücklich Victron. Sie bewertet jeden eingerichteten Regler und Shunt anhand seiner eigenen aktuellen Messwerte. Ein erreichbarer Regler bestätigt nicht zugleich einen zweiten. Ein Tippen erklärt den Bezug; die Modellbaulader behalten ihren eigenen Status direkt am Gerät.

Gespeicherte Rollerwerte bleiben verfügbar, erscheinen aber deutlich grau. Jede betroffene Dashboardkachel erhält eine sichtbare **Alt-Markierung mit Uhrzeit**, unabhängig vom bei kleinen Kacheln ausgeblendeten Erklärungstext. Der BMS-Bereich zeigt zusätzlich **„Letzter Stand – keine aktuellen BMS-Daten“** mit Messzeitpunkt. Historisch erfasste Ladungsenergie bleibt von aktuellen BMS-Feldern unterscheidbar.

Das Anzeigehandy bewertet das Alter des ursprünglichen Messzeitpunkts selbst. Ein zwischengespeichertes `fresh: true`, ein erneuter Empfang desselben WLAN-Snapshots oder bloßes Öffnen der Ansicht machen einen alten BMS-Wert nicht frisch. Angezeigte Werte altern auch während des Wartens auf den nächsten Abgleich sichtbar aus. Nach tatsächlichem Empfang aktueller, gegebenenfalls niedrigerer Akkudaten verschwinden Grau-Markierung und Warnhinweis. Alte Werte bleiben für aktuelle Formeln und Schaltentscheidungen gesperrt.

## Drei 608PD einrichten und bedienen

Unter **Modellbau → Einrichten** werden bis zu drei physische Geräte mit eigenem Namen, Adresse, Energiepfad und privater Kennung gespeichert. Das vorhandene Einzelprofil wird beim Ergänzen übernommen; ein weiteres Gerät überschreibt es nicht. Jeder aktivierte Lader erhält seine eigene Verbindung und seine eigenen Messwerte. Der Wechsel des betrachteten Laders ersetzt keine Profile und schickt keinen Ladeauftrag.

1. Auf dem erfassenden Handy die Aufzeichnung starten; Anzeigen bedienen die Einrichtung über den Anlagenverbund.
2. **Weiteren 608PD hinzufügen → 608PD in Reichweite suchen** wählen. Die 15 Sekunden dauernde Suche läuft tatsächlich am erfassenden Handy. Den richtigen Lader anhand von Name und Adresse auswählen.
3. Namen und Energiepfad festlegen und **Neue Verbindung am Lader bestätigen** wählen. Eine neue lokale zufällige Kennung wird verschlüsselt gespeichert. Wartet das Gerät auf Bestätigung, dort eine Taste drücken. Ein wartender oder abgebrochener Vorgang gilt nicht als erfolgreiche Einrichtung.
4. Erst aktuelle gültige Statusantworten bestätigen die neue Einrichtung. Eine ausbleibende Bestätigung beendet den Versuch; ein bereits vorhandener gültiger Schlüssel wird nicht durch einen neuen ersetzt.
5. Für bereits eingerichtete Geräte die gespeicherte Verbindung verwenden. Ein eigener früherer Mitschnitt bleibt als Rückweg verfügbar; beim gezielten Import werden Adresszuordnung und wiederverwendete Bluetooth-Verbindungshandles berücksichtigt.

Die direkte Erstkopplung folgt dem dokumentierten ISDT-Bindformat und ist gegen einen asynchronen Testpartner geprüft. Die [Protokollquelle](https://github.com/mtheli/isdt_air_ble/blob/master/PROTOCOL.md) dokumentiert andere ISDT-Geräte; der [Projektstand](https://github.com/mtheli/isdt_air_ble#tested-models) führt den 608PD als noch nicht praktisch getestet. Eine erfolgreiche direkte Kopplung an Tims 608PD wird deshalb erst nach einem Versuch am Gerät zugesagt. Die vorhandene bereits funktionierende Verbindung bleibt verwendbar.

Bedienaufträge sind an eine konkrete Laderadresse und eine weiterhin gültige Verbindung mit frischen Messwerten gebunden. Die begrenzte, bestätigungspflichtige **3S-LiPo-Funktion 4,21 V/Zelle / 0,50 A** gilt unverändert nur für passende Modellbauprofile. Andere Ladeprofile und Powerbank-Betrieb am Gerät starten. Ein Profil für externe Wohnwagenladung erhält diesen Modellbau-Ladestart nicht.

## Dauerhaftes Lade- und Powerbankjournal

Die Zentrale speichert rohe Laderbeobachtungen in einer ergänzten SQLite-Tabelle, getrennt nach physischem Gerät. Aktive Betriebszustände werden etwa alle fünf Sekunden und bei Zustandswechseln unmittelbar festgehalten; auch App-Aufträge und Verbindungsunterbrechungen werden gespeichert. Ruhende Geräte erhalten sparsame Beobachtungen. Die bestehende Roller-Datenbank wird ohne Verlust ihrer bisherigen Tabellen erweitert. Eine eigene dauerhafte Journalzuordnung bleibt auch bei geänderten Victron-Geräteeinträgen erhalten.

Unter **Modellbau → Journal öffnen** lassen sich alle oder einzelne Lader und 7, 30 oder 90 Tage betrachten. Anzeigen replizieren die Rohdaten und berechnen daraus selbst die beobachteten Vorgänge; die Zentrale führt keine neue Energieauswertung durch. Im eigenständigen Betrieb erfolgt dies lokal.

- Laden und Powerbank/Entladen erhalten eigene Vorgänge mit Messzeitpunkten, beobachteter Dauer, Gerätezählern, Pack- und Zellwerten.
- Wh werden ausschließlich über geeignete beobachtete Abschnitte derselben Betriebsart integriert und als **608PD-Akkuseite** bezeichnet. Lücken zählen nicht als 0 W oder als belegte Laufzeit. Die Werte werden nicht erneut zur Shunt-Gesamtbilanz addiert.
- Fehlender Anfang, fehlendes Ende, Verbindungsunterbrechung und Zählerrücksetzung bleiben sichtbar. Ein nach einem Ausfall nicht mehr beobachteter Lader meldet ein unbekanntes Ende.
- App-Aufträge gelten als angefordert. Erst ein passender späterer Betriebszustand innerhalb des Bestätigungsfensters bestätigt den Auftrag im Journal.
- Wiederholtes Einlesen und erneuter Abgleich legen vorhandene Rohzeilen nicht doppelt an. Die Auswertung lässt sich nach einem App-Neustart erneut aus gespeicherten Grundlagen aufbauen.
- **Journal als JSON exportieren** speichert das geladene Ergebnis am selbst gewählten Ort. Die vollständige verschlüsselte App-Sicherung enthält die zugrunde liegende Datenbank und Laderprofile. Ein entfernter Profileintrag löscht seine bisherigen Vorgänge nicht; unter „Alle Lader“ bleiben sie auffindbar.

Vor Beginn dieser Aufzeichnung nicht gespeicherte Ladevorgänge lassen sich nicht exakt nachträglich rekonstruieren. Die Laderwerte messen keinen unabhängigen Wohnwageneingang und liefern daher allein keinen gemessenen Gesamtwirkungsgrad.

## Vorprüfung und nächste Freigabe

Lokale Quellcodeprüfungen führen die produktiven Java- und JavaScript-Pfade mit neutralen Daten aus. Dazu gehören die Reproduktion des alten Archivüberlaufs, Rückstau und Fortsetzung, drei getrennte Laderverbindungen, bestätigte und abgelaufene Ersteinrichtung, auf die Adresse begrenzte Befehle, tatsächliche SQLite-Speicherung und Datenbank-Erweiterung, neuer Prozess, wiederholte Viewer-Replikation sowie gefilterter HCI-Import. Die gemeinsamen JavaScript-Tests prüfen zusätzlich die BMS-Altersgrenze und die Rückkehr mit niedrigerem Akkustand.

Browser-Szenarien für die neue Darstellung, Hell-/Dunkelansicht, schmale Bildschirme und Journalbedienung sowie Android-Emulatorprüfungen sind vorbereitet. Der vollständige Build-/Lint-/Emulatorlauf startet erst nach der ausdrücklich erteilten Bau-Freigabe. Es werden daraus weder reale Funkstabilität noch eine bereits durchgeführte Direktkopplung oder ein erfolgreicher nächtlicher G5-Neustart abgeleitet.

Nach erfolgreichem freigegebenem Bau zuerst die **Zentrale beziehungsweise das direkt erfassende Handy**, danach alle Anzeigen aktualisieren. Für neue Einrichtung und Journal ist der gemeinsame Stand 3.0.8 vorgesehen. Die bisherige Verbundschnittstelle bleibt erhalten; ältere Anzeigen nutzen weiterhin das ausgewählte Einzelprofil. Ein aktueller Viewer kann die bisherigen Analysen einer älteren Zentrale weiter lesen, weist beim neuen Journal aber auf das erforderliche Zentralenupdate hin.
