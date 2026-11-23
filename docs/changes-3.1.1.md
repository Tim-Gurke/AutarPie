# AutarPie 3.1.1

## Anzeige fragt bei länger grauen Feldern erneut nach

Bleibt eine sichtbare Kachel **45 Sekunden durchgehend grau**, fragt die Anzeige
gezielt erneut nach. Weitere Versuche für dieselbe Kachel erfolgen frühestens
nach **60 Sekunden**. Bereits laufende gleiche Übertragungen werden nicht
verdoppelt; gleichzeitige Anforderungen einer Zentralenansicht werden gebündelt.
Andere aktuelle Diagramme werden durch die Wiederholung nicht ungültig gemacht.
Bei geschlossener Anzeige pausiert diese zusätzliche Überwachung.

Vorhandene Kurven bleiben mit ihrem alten Stand sichtbar. **Farbig bedeutet
weiterhin aktuell:** Der Neuversuch allein macht keine Kachel farbig. Wetterdaten
fehlen, sind älter als sechs Stunden oder haben einen Abruffehler: Wetter bleibt
grau. Aktuelles Wetter und noch vorläufige Akku-Prognosen bleiben getrennt
gekennzeichnet. Ein grauer Prognosebalken kann den Lernstand erneut anfordern.

## Verbindungsaufbau und Wetterabruf erholen sich selbst

Eine beim Start fehlgeschlagene oder stumme Suche nach der Zentrale wird nach
fehlgeschlagenen Verbundabfragen mit mindestens 30 Sekunden Abstand zum letzten
Suchstart wiederholt. Verspätete Antworten einer alten Suche dürfen die neue
Suche oder ihre aktuelle Adresse nicht mehr ersetzen.

Ein vorheriger Wetterfehler verhindert einen neuen Wetterabruf nicht mehr allein
deshalb, weil gespeicherte Wetterdaten noch innerhalb des Cachefensters liegen.
Gültige Daten ohne Fehler werden weiterhin wiederverwendet.

Diese Korrekturen starten keinen zusätzlichen Gerätearchivabruf und ändern keine
Aufzeichnung oder Gerätebefehle. Messwerte, Kopplungen, Farben und Layouts bleiben
erhalten. Die Zentrale bleibt für durchgehende Sammlung, GPS/Anlagenstandort,
Archive, Roller-BMS und bestätigte Steuerung zuständig. Der freiwillige passive
Victron-Liveempfang aus 3.1.0 bleibt enthalten.

## Aktualisieren

**Zuerst G5/Zentrale beziehungsweise das erfassende Handy, danach S20/S10 und
weitere Anzeigen auf 3.1.1 aktualisieren.** Über die vorhandene Installation
installieren. App-Kennung und ursprüngliche Updatesignatur bleiben erhalten.
Der Anlagenverbund und der passive Direkt-Liveempfang bleiben mit 3.1.0
kompatibel; die automatische Wiederholung grauer Felder benötigt das Update
auf dem jeweiligen Anzeigehandy, die Wetterkorrektur auch auf der Zentrale.

Nach dem Update die Anzeige beim nächsten morgendlichen Start geöffnet lassen.
Aktuelle Livewerte können bereits farbig sein, während andere Kacheln noch laden.
Bleiben Felder grau, zeigen die vorhandenen Textstatus weiterhin den alten Stand
oder den laufenden Abruf. Ein manuelles erneutes Archivieren ist für die neue
Wiederholung nicht erforderlich.

## Prüfung und Grenzen

Der nachgestellte Verbindungsfehler wurde gegen die veröffentlichte
`LanSync`-Implementierung aus 3.1.0 und die Korrektur verglichen. Der Test verwendet
echte verschlüsselte TCP-Anfragen und SQLite; Androids Suchrückmeldungen werden
kontrolliert vorgegeben. Zusätzliche Zeit- und Warteschlangentests prüfen
gezielte Wiederholung, erhaltene Grau-Kennzeichnung und begrenzte Anfragen.

Die komplette frühere Stunde Wartezeit lässt sich aus der bisherigen Diagnose
nicht rekonstruieren. Dass dieser Suchfehler die konkrete Ursache an Tims Handys
war und der morgendliche Start damit vollständig behoben ist, ist noch nicht
vor Ort bestätigt. Software-, Browser- und Emulatorprüfungen ersetzen keine
Messung der echten G5-/WLAN-/Victron-Verbindung.

[Bedienung](AUTARPIE-3.md) · [Vollständige Versionshistorie](../CHANGELOG.md)
