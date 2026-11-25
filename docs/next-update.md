# Nächstes gemeinsames AutarPie-Update

Abgleich am 25.11.2026 mit dem veröffentlichten Stand **3.1.6**
(`fff10877c42f90e667105b87b31ae58504221b27`). Dies ist die Planung, keine
Behauptung einer bereits implementierten Korrektur oder neuen Veröffentlichung.

## Offene Entwicklungspunkte

| Punkt | Gewünschtes Ergebnis | Stand |
|---|---|---|
| PV-Eingangsspannung | Echte Spannung des ausgewählten Reglers abrufen, speichern und bis zur Kachel übertragen. Quelle und Datenstand korrekt zeigen; keinen Batterie- oder errechneten Ersatzwert als PV-Spannung ausgeben. | Noch offen. Der Quellstand besitzt bereits einen aktiven Registerabruf und Speicherung. Auslösebedingungen, Aktualität und Kachelzuordnung müssen als gesamte Datenkette geprüft werden. Die Beschriftung „Berechnet“ ist ebenfalls zu prüfen. |
| Sonneneinfluss beim Gaslernen | Historische Sonneneinstrahlung und vorhandenen PV-Ertrag als ergänzende Grundlage betrachten, einen belegten Einfluss auf den Heizbedarf lernen und in einer Gas-Lernnotiz erklären. Fertige Faktoren speichern und für neue Vorhersagen anwenden. | Noch nicht implementiert. Wetterdaten, Modell und Lernnotiz müssen gemeinsam erweitert werden. Gemessene PV-Wh sind keine feste direkte Heizwärme-/Gasgutschrift. Ein Sonnenfaktor darf erst mit ausreichender Daten- und Prüfgrundlage gelten. |
| Wetterrückblick: 14 Tage mit gewichtetem Prognosevergleich | Je vergangenem Tag den gewichteten Mittelwert der ursprünglich für genau diesen Zieltag ausgegebenen Prognosen aus den 14 Tagen davor mit dem tatsächlichen Solarertrag vergleichen. Vorlauf 14–8 Tage schwach, 7–4 stärker, letzte drei Tage am stärksten gewichten; tatsächlichen Akkustand erhalten. | Neu vorgemerkt, noch nicht implementiert. Originalprognosen mit Datum speichern, verfügbare Anzahl und Ertragsabweichung zeigen. Die 14 vergangenen Tage der Ansicht und die 14 Vorlauftage je Zieltag sind getrennte Zeiträume. |

Vor dem Bau einmal abstimmen: **gesammelte Punkte im gemeinsamen Update** oder
**einen ausdrücklich gewählten Akutfix vorziehen**. Einen
zurückgestellten Punkt mit Begründung hier erhalten. Tim möchte diese Frage
**beim Vorbereiten des nächsten Updates**; bis dahin wird weiter gesammelt.
Noch keine neue
Versionsnummer oder neue Bau-/Veröffentlichungsfreigabe für diese Auswahl ableiten.

## Präzisierung: Prognosequalität im 14-Tage-Rückblick

- Die Ansicht zeigt **14 vergangene Kalendertage** mit dem bereits verfügbaren
  tatsächlichen Solarertrag und Akkustand.
- Für jeden Zieltag D die damals tatsächlich ausgegebenen Ertragsprognosen für
  **denselben Tag D** aus D−14 bis D−1 heranziehen. Der gewünschte Mittelwert
  bezieht sich auf diese früheren Prognosestände.
- **Gewichteter Mittelwert:** D−14 bis D−8 schwach, D−7 bis D−4 stärker,
  D−3 bis D−1 am stärksten. Positive Gewichte mit dieser verbindlichen
  Reihenfolge bei der Umsetzung festlegen; konkrete Zahlen sind noch nicht
  vorgegeben. Mit der Summe der verwendeten Gewichte normalisieren.
- Einen nachvollziehbaren Prognosestand je Vorlauftag verwenden. Häufiges
  Aktualisieren an einem Tag darf dessen Gewicht nicht zusätzlich vergrößern.
  Datum/Zeit der Ausgabe und Zieltag erhalten; nur vor dem Zieltag ausgegebene
  Prognosen in diesen Vorabvergleich aufnehmen.
- Je Zieltag **gewichtete damalige Prognose, tatsächlichen Ertrag und Abweichung**
  in Wh sowie, soweit sinnvoll, Prozent zeigen. Prognosestände und ihr Vorlauf
  sollen im Detail nachvollziehbar bleiben.
- Originale Prognosestände dauerhaft speichern und vorhandene Historie nutzen.
  Der Vergleich bleibt ein Vergleich mit dem damals verfügbaren Wissensstand.
  Die tatsächlichen Erträge anderer Tage, neu berechnete historische Prognosen
  und der tatsächliche Ertrag des Zieltags bilden keine Ersatzprognosen.
- Fehlende Prognosestände offen lassen und beispielsweise **9 von 14** verfügbare
  Vorlauftage nennen. Mit vorhandenen Gewichten normalisieren; einen Vergleich
  ohne Grundlage als unbekannt zeigen. Bei Prognose null eine Prozentabweichung
  nicht durch Division erzeugen.
- Für den vollständigen Rückblick samt maximalem Vorlauf sind ursprüngliche
  Prognosestände über mindestens **28 Kalendertage** erforderlich. Aufbewahrung,
  Ortszeit und Vergleich derselben Anlage/Ertragsgröße bei der Umsetzung beachten.

### Voller Akku und abgeregelter Ertrag

Der tatsächlich geerntete Ertrag kann durch einen vollen Akku und geringe
Abnahme begrenzt sein. Wetter-/Anlagenpotenzial und tatsächlich gelieferte
Energie unterscheiden. Ein solcher Tagesunterschied allein belegt keinen
Prognosefehler.

- Tatsächliche Wh und tatsächlichen Akkustand unverändert anzeigen. Bei belegter
  Begrenzung beispielsweise **„Ertrag begrenzt – Akku voll“**, bei unklarer
  Grundlage **„Begrenzung unklar“** ergänzen.
- Aktuelle Ladezustands-, Ladephasen-, Spannungs-/Strom- und Reglergrundlagen
  gemeinsam beurteilen. 100 % SoC oder Float allein messen keine abgeregelten Wh;
  laufende Verbraucher können auch bei vollem Akku Solarleistung abnehmen.
- Bei ausreichender zeitlicher Auflösung ursprüngliche Prognose und Messung
  ausschließlich über dieselben nachgewiesen unbegrenzten Stunden vergleichen.
  Abgeregelte oder unklare Abschnitte nicht als Minderertrag zum Herunterlernen
  des Sonnen-/Ertragsmodells verwenden.
- Ist nur der Tageswert verfügbar, den Tag als **eingeschränkt vergleichbar**
  markieren. Keine scheinbar belastbare Fehlerquote für den möglichen
  Solarertrag aus dem begrenzten Tagesertrag ableiten.
- Ungenutzten möglichen Ertrag höchstens mit eigener belegter Grundlage als
  Schätzung ausweisen. Die gesamte Differenz zwischen Prognose und Messung ist
  kein gemessener Verlust durch Abregeln. Eine aus derselben Prognose ergänzte
  fiktive Ertragsmenge darf diese Prognose nicht selbst bestätigen.
- Bereits in der Anlagenprognose berücksichtigte Reglerlimits von der
  zusätzlichen Begrenzung durch den Ladezustand unterscheiden.
- Auch beim geplanten Gaslernen ist niedriger gemessener PV-Ertrag unter
  Abregelung kein Beleg für wenig Sonnenwärme. Ortsbezogene Einstrahlung bleibt
  die fachliche Hauptgrundlage für den Sonnen-Heizeinfluss.

Im Stand 3.1.6 schützt die bestehende Solarkalibrierung ihre Lerngrundlage bereits
konservativ: native `unrestricted()`-Prüfung und als unbegrenzt belegte
`yieldParts`, danach passende ursprüngliche Zeitintervall-Prognosen. Das ist
keine Messung ungenutzter Energie und noch keine fertige Kennzeichnung im
geplanten 14-Tage-Rückblick.

Herstellergrundlage: [Victron-Handbuch](https://www.victronenergy.com/media/pg/Manual_SmartSolar_MPPT_75-10_up_to_100-20/en/troubleshooting.html),
Abschnitt 8.5.1 „Battery is full“.

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
