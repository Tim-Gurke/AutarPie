# AutarPie 3.0.16

## Direkter Liveempfang auf Anzeigehandys

Unter **Einstellungen → Anlagenverbund → Verbund öffnen → Victron-Livewerte direkt empfangen** kann jedes gekoppelte Anzeigehandy den direkten Empfang freiwillig einschalten. Die Funktion ist zunächst aus und arbeitet nur bei geöffneter Anzeige. Android fragt gegebenenfalls nach Bluetooth-/Umgebungsgerätefreigabe; ältere Android-Versionen benötigen für BLE die Standortfreigabe und den eingeschalteten Standortdienst. Dadurch wird der Wohnwagenstandort nicht auf das Anzeigehandy umgestellt.

Die Anzeige hört ausschließlich die verschlüsselte **Instant-Readout-Sofortanzeige** der eingerichteten SmartSolar- und SmartShunt-Geräte. Sie öffnet keine aktive Victron-Verbindung, holt kein Gerätearchiv und sendet keine Steuerbefehle direkt. Das G5 bleibt für Aufzeichnung, Archive, Anlagenstandort/GPS, Roller-BMS und bestätigte Steuerung zuständig. Die zusätzlichen Livewerte werden nicht als zweite Messhistorie gespeichert.

Die gekoppelte Zentrale überträgt die benötigten Sofortanzeige-Schlüssel über den bereits verschlüsselten Anlagenverbund. Das Anzeigehandy speichert sie mit AndroidKeyStore-Schutz, getrennt je Anlage. Die Schlüssel gehören nicht in die normale Einstellungssicherung oder Diagnose. Die vollständige verschlüsselte Datensicherung kann sie für einen Geräteumzug übertragen.

Frische direkt empfangene Messwerte erhalten Vorrang; nach 15 Sekunden ohne passenden Empfang verwendet die Anzeige wieder die verfügbaren Zentralwerte. Deren eigener Zeitstempel entscheidet weiter über Aktualität. Ohne frischen Wert wird kein Liveempfang behauptet. Der Live-Status nennt den direkten Empfang. Andere Geräte und fremde Anlagen bleiben ausgeschlossen. Beim Schließen der Anzeige endet die Suche.

## Kleinere Abgleiche für sichtbare Diagramme

Kurze Diagrammzeiträume lösen nicht mehr automatisch einen mindestens 90-tägigen Abgleich zeitabhängiger Modellgrundlagen aus. Sie erhalten den gewählten Zeitraum mit dem notwendigen Vorlauf. Die längerfristige Roller-/Verbrauchsauswertung bleibt erhalten und läuft weiter auf den Anzeigehandys.

Kurz aufeinanderfolgende Diagramme können bereits geladene Modellgrundlagen wiederverwenden. Vorher wird deren Kennung an der Zentrale geprüft; geänderte Einstellungen, Wettergrundlagen oder Datenbestände werden weiterhin erkannt. Messdatenblöcke bleiben revisionsgeprüft. Ein echter Quellenwechsel verwirft die vorherige Anlagenzuordnung.

Aktuelle Wetterdaten können sichtbar werden, während die Verbrauchsauswertung noch lädt. Noch nicht aktuelle Akku-/Extras-Prognosen behalten eine eigene graue Kennzeichnung und Erklärung. Veraltete oder fehlende Wetterdaten bleiben als solche erkennbar. Die Hintergrundaktualisierung wird fortgesetzt.

## Diagnose auch bei gestörter Auswertung

Der Export liest den bereits gespeicherten lokalen Stand unabhängig von der laufenden Berechnungswarteschlange. Er schreibt den ZIP-Anfang vor dem Lesen der Auswertung. Scheitert ein Teil, bleiben ein lesbarer Fehlerbericht, verfügbare Laufzeiten und nach Möglichkeit die Messhistorie erhalten. **Teildiagnose** wird ausdrücklich gemeldet; `export-result.json` unterscheidet vollständigen und teilweisen Export. Ein nicht beschreibbarer Speicheranbieter bleibt ein Fehler und wird nicht als erfolgreiche Sicherung ausgegeben.

Eine Mess- und Lerndiagnose ist keine vollständige Datensicherung.

## Aktualisieren und vor einem ROM-Wechsel sichern

**Zuerst die G5-Zentrale, danach S20/S10 und weitere Anzeigen auf 3.0.16 aktualisieren.** Über die vorhandene Installation installieren; dieselbe App-Kennung und ursprüngliche Updatesignatur bleiben erhalten. Die Daten, Kopplungen, Rollerprofile, Farben und Layouts werden nicht zurückgesetzt. Der normale Verbund bleibt mit 3.0.15 kompatibel; für die erstmalige Bereitstellung der Empfangsschlüssel muss auch die Zentrale 3.0.16 verwenden.

Für einen späteren Wechsel auf LineageOS vor dem Löschen des G5 unter **Datensicherung · automatisch / jetzt → Jetzt sichern** eine neue vollständige verschlüsselte Sicherung erstellen. Den geprüften Erfolg abwarten und die Datei außerhalb des G5 aufbewahren. Das Kennwort getrennt sichern. Dieser Sicherungsweg ab 3.0.13 verwendet übertragbare Schlüssel im Format 2; ältere Übernahmesicherungen können noch an die alte Android-Installation gebundene Schlüssel enthalten. Automate-Abläufe und andere persönliche Dateien separat exportieren. Android-Berechtigungen und System-Bluetooth-Kopplungen müssen nach einer Neuinstallation gegebenenfalls neu bestätigt werden.

## Prüfgrenzen

Die automatisierten Prüfungen verwenden künstliche verschlüsselte BLE-Pakete, echte Produktionsdekodierung, lokale Datenbanken, verschlüsselten TCP-Austausch und Browser-/Android-Oberflächen. Sie prüfen unter anderem Quellenzuordnung, Schlüsselwechsel, Zeitgrenzen, Rückfall auf Zentralwerte, Erhalt von GPS-/Rollerzuständigkeit und lesbare Teildiagnosen. Reale Funkreichweite, gleichzeitiger Empfang am eigenen SmartSolar/SmartShunt, das Roller-BMS und die tatsächlichen Ladezeiten am G5/S20/S10 müssen am eigenen Aufbau geprüft werden. Ein Geschwindigkeitsgewinn durch einen ROM-Wechsel wird daraus nicht zugesagt.

[Bedienung](AUTARPIE-3.md) · [Vollständige Versionshistorie](../CHANGELOG.md)
