# Nächstes gemeinsames AutarPie-Update

Abgleich am 25.11.2026 mit dem veröffentlichten Stand **3.1.6**
(`fff10877c42f90e667105b87b31ae58504221b27`). Dies ist die Planung, keine
Behauptung einer bereits implementierten Korrektur oder neuen Veröffentlichung.

## Offene Entwicklungspunkte

| Punkt | Gewünschtes Ergebnis | Stand |
|---|---|---|
| PV-Eingangsspannung | Echte Spannung des ausgewählten Reglers abrufen, speichern und bis zur Kachel übertragen. Quelle und Datenstand korrekt zeigen; keinen Batterie- oder errechneten Ersatzwert als PV-Spannung ausgeben. | Noch offen. Der Quellstand besitzt bereits einen aktiven Registerabruf und Speicherung. Auslösebedingungen, Aktualität und Kachelzuordnung müssen als gesamte Datenkette geprüft werden. Die Beschriftung „Berechnet“ ist ebenfalls zu prüfen. |
| Sonneneinfluss beim Gaslernen | Historische Sonneneinstrahlung und vorhandenen PV-Ertrag als ergänzende Grundlage betrachten, einen belegten Einfluss auf den Heizbedarf lernen und in einer Gas-Lernnotiz erklären. Fertige Faktoren speichern und für neue Vorhersagen anwenden. | Noch nicht implementiert. Wetterdaten, Modell und Lernnotiz müssen gemeinsam erweitert werden. Gemessene PV-Wh sind keine feste direkte Heizwärme-/Gasgutschrift. Ein Sonnenfaktor darf erst mit ausreichender Daten- und Prüfgrundlage gelten. |

Vor dem Bau einmal abstimmen: **beide Punkte im gemeinsamen Update** oder
**zunächst die PV-Spannung als ausdrücklich gewählter Akutfix**. Einen
zurückgestellten Punkt mit Begründung hier erhalten. Tim möchte diese Frage
**beim Vorbereiten des nächsten Updates**; bis dahin wird weiter gesammelt.
Noch keine neue
Versionsnummer oder neue Bau-/Veröffentlichungsfreigabe für diese Auswahl ableiten.

## Bereits abgeschlossen und weiter zu erhalten

| Thema | Veröffentlichter Stand |
|---|---|
| Rückwirkender Ortsbestätiger, Durchschnitt/letzte Flasche/24-Stunden-Bedarf und vorsichtige Gasuntergrenze bei anhaltender Kälte | 3.1.6 |
| Grüne Wetter-Akkubalken mit bereitem gespeichertem Modell | 3.1.5; Funktion durch Rückmeldung bestätigt |
| Gespeicherte Verbrauchsfaktoren, Tagesauswertungen und korrigiertes Wetterrückschau-Symbol | 3.1.4 |
| Priorisierte Datenabfragen, graue ältere Kacheln und einstellbare BMS-Anzeigegrenze | 3.1.2/3.1.3; spätere Ergänzungen erhalten |
| Gewünschte Grundeinstellung nur für Neuinstallation und freiwillige vollständige automatische Sicherungen | 3.0.13; eigene bestehende Einstellungen erhalten |
| Wetterfarben/-bestandteile, Rückschau-Anordnung und Roller-Teilladungslernen | 3.0.11 und spätere Ergänzungen |
| Kurzwahlfarben, drei 608PD-Profile und dauerhaftes Lade-/Powerbankjournal | 3.0.6/3.0.8 und spätere Ergänzungen |

Die Sammlung enthält frühere Zwischenstände mit „offen“. Spätere Abschlüsse sind
für den aktuellen Status maßgeblich. Der G5-Neustart wurde in der späteren
Rückmeldung als funktionierend beschrieben und wird deshalb nicht erneut als
unimplementierter Fehler eingeplant.

## Praktische Prüfpunkte

Reale Victron-/WLAN-Laufzeiten, direkte Einrichtung beziehungsweise gleichzeitiger
Betrieb der drei 608PD und das Verhalten eines persönlich gewählten Online-
Speicheranbieters sind keine durch Softwareprüfungen garantierten Eigenschaften.
Ein konkreter neuer Befund kann daraus einen weiteren Reparaturpunkt machen;
vorhandene Implementierung und noch fehlende Geräteprüfung bleiben unterscheidbar.

## Vorgehen vor jedem Update

1. Gesamte Änderungssammlung und spätere Abschlüsse abgleichen.
2. Alle noch offenen Entwicklungspunkte kurz vorlegen und gemeinsam den Umfang wählen.
3. Gewählten Umfang zusammen umsetzen, passende Prüfungen ausführen und Zwischenstände sichern.
4. Konkreten fertigen Umfang bauen/veröffentlichen, nachdem die dafür nötige Freigabe vorliegt.
5. Jeden erledigten Punkt mit Version und Nachweis abhaken; Verschobenes sichtbar erhalten.
