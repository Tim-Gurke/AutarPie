# AutarPie 3.1.4

## Gespeicherte Lernfaktoren für Solar, Gas und Wasser

Die Solarvorschau verwendet fertige dauerhaft gespeicherte Verbrauchsprofile und
Solarkorrekturfaktoren. Ein normaler neuer Wetterbericht oder Livewert startet
keine erneute Ableitung dieser Faktoren aus historischen Lernbeobachtungen.
Aktueller Akku, Wetter, Anwesenheitsplanung und Sondernutzungen aktualisieren
weiter die Prognose. Die bisherigen saisonalen, nach Anwesenheit getrennten
Verfahren und ihre Unsicherheiten bleiben erhalten.

Je Anlage und passender Konfiguration werden morgens/mittags/abends getrennte
Profile für 32 Kalendertage gespeichert. Die übliche 16-Tage-Vorschau liest daraus
ihre Werte. Ein neuer Lernstand wird auf dem Anzeigehandy oder im eigenständigen
Betrieb in kleinen zeitlich getrennten Schritten vorbereitet und erst nach
Speicherbestätigung übernommen. Die Zentrale sammelt weiter die Grundlagen.

Für Gas bleiben Grundbedarf, Heizbedarf und nur bei ausreichender Beleglage
Wind-Heizbedarf als Koeffizienten erhalten. Neue Vorhersagen werden damit in
zeitabhängigen Bedarf und Reichweite umgesetzt. Wasserstatistiken werden ebenso
wiederverwendet. Neue/korrigierte Verbrauchseinträge, historische Wettergrundlagen
und der Tagwechsel können eine getrennte Aktualisierung des Modells auslösen.
Eine bloße Abrufuhrzeit gleicher Wetterdaten löst kein erneutes Gaslernen aus.
Ein noch vorläufiges Temperaturmodell bleibt ausdrücklich vorläufig.

## Tageswerte vor dem Abgleich und ein dauerhaftes Tagesjournal

Bereits gespeicherte heutige Energieansichten können ohne Netz-/Rohdatenprüfung
sofort als bisheriger Stand erscheinen; die aktuelle Abfrage läuft weiter und
ersetzt sie anschließend. Zeitpunkt, Grau-/Lädt-Kennzeichnung und Messlücken
bleiben sichtbar. Der Zusatzspeicher ist an Anlage, Geräte-/Anlagenkonfiguration,
zentralen Datenbestand, Kalendertag und Abfrage gebunden. Eine andere Anlage oder
ein ersetzter Datenbestand darf daraus keine Werte übernehmen.

Abgeschlossene Tagesauswertungen bleiben in einem dauerhaften SQLite-Tagesjournal,
unabhängig von der begrenzten Kurven-Zwischenspeicherung. Gleiche abgeschlossene
Tage werden wiederverwendet; geänderte Eingaben berechnen betroffene Tage und ihre
notwendigen Modellabhängigkeiten neu. Der laufende Tag kann mit neuen Messungen
weiter ausgewertet werden. Bereits vorhandene Messdaten und Lernjournale bleiben
erhalten. Verfügbare Tagesmitteltemperaturen werden als **Wettermodell** im
Tagesarchiv angezeigt; fehlende Temperaturen bleiben unbekannt.

## Wetterrückschau

Die dreitägige Rückschau verwendet wie die Vorschau das Verhältnis zwischen
Sonnenstunden und Tageslänge für ihr Tagessymbol. Zuvor verwendete sie unmittelbar
den Anbieter-Tagescode der stärksten Wettererscheinung, sodass ein Nebel- oder
Regensymbol neben vielen Sonnenstunden stehen konnte. Der Detaildialog erklärt
die neue Symbolbedeutung. Ohne passende Sonnenzeiten bleibt das Anbieter-Symbol.
Wettercodes und Messwerte werden nicht überschrieben; das Sonnenanteil-Symbol
behauptet nicht, dass es am betreffenden Tag keinerlei Niederschlag gab.

## Aktualisieren und erster Start

Zuerst G5/Zentrale beziehungsweise direkt erfassendes Handy, anschließend S20/S10
und weitere Anzeigen aktualisieren. Über die bestehende Installation installieren.
Paket `de.wattpit.app`, ursprüngliche Updatesignatur, Messdaten, Kopplungen, Profile,
Farben und eigene Layouts bleiben erhalten.

Der Datenverbund bleibt mit 3.1.3 kompatibel. Die neue Wiederverwendung benötigt
3.1.4 auf der jeweiligen Anzeige; zusätzliche Wettertemperaturen benötigen einen
entsprechend aktualisierten Wetterabruf der Zentrale. Beim ersten Start werden
die neuen Faktoren einmal aus bereits vorhandenen abgeleiteten Lernbeobachtungen
erzeugt. Fehlen diese noch vollständig, müssen echte Grundlagen zunächst ankommen.
Vorhandene gespeicherte Ansichten können währenddessen sichtbar bleiben. Eine
vollständig fehlende neue Tagesaufzeichnung wird nicht als gemessene Null erfunden.

## Prüfungen und Grenzen

Die neuen Regressionsprüfungen vergleichen gespeicherte Solarprofile mit der
bisherigen Berechnung und prüfen ausdrücklich neue Wetter-/Akkugrundlagen ohne
Zugriff auf die historischen Lernbeobachtungen. Gasprognosen ändern ihren Bedarf
mit neuem Wetter bei unveränderten gespeicherten Koeffizienten. Speicherprüfungen
verwenden neue Kontexte und prüfen Anlagen-/Bestandswechsel und Fehlererhalt.
Ein zusätzlicher Browserablauf prüft die wirkliche Oberfläche nach Neuladen und
neue Wetterdaten ohne erneute Faktor-/Gaslernberechnung.

Der vollständige Freigabelauf muss vor Veröffentlichung erfolgreich sein.
Software-, Emulator- und Browserprüfungen sind keine gemessene Laufzeit oder
Bluetooth-/WLAN-Abnahme am tatsächlichen G5, S20 oder S10.
