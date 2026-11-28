# Nächstes gemeinsames AutarPie-Update

Abgleich am 25.11.2026 mit dem veröffentlichten Stand **3.1.6**
(`fff10877c42f90e667105b87b31ae58504221b27`). Dies ist die Planung, keine
Behauptung einer bereits implementierten Korrektur oder neuen Veröffentlichung.

Ergänzungen am **27.11.2026**: mehrere Wettermodelle für Wetter und Solarertrag
mit nachvollziehbarem Mittelwert und Modellspanne sowie automatische 608PD-Erfassung
als Sonderverbrauch und als externe Ladequelle in den Wohnwagenakku. Beide Punkte
bleiben zur Sammlung.

Ergänzungen am **28.11.2026**: grafischer Langfristtrend mit Bewölkung,
Modellmix und echten Kalenderwochen von Montag bis Sonntag, zusätzlich als frei
platzierbare Wochenkachel; außerdem den ergebnislosen 608PD-Journalaufruf prüfen,
Ladeziel/Zeit/Menge darin nachvollziehbar machen und die vorhandene Tageswetter-
Kachel um Temperatur und weitere Wettergrößen ergänzen.

Weitere Praxisbestätigung am **28.11.2026**: aktives VPN verhindert bei Tim den
Zugriff auf AutarPie; lokale Netzführung und Diagnose als eigenen Punkt aufnehmen.

## Offene Entwicklungspunkte

| Punkt | Gewünschtes Ergebnis | Stand |
|---|---|---|
| PV-Eingangsspannung | Echte Spannung des ausgewählten Reglers abrufen, speichern und bis zur Kachel übertragen. Quelle und Datenstand korrekt zeigen; keinen Batterie- oder errechneten Ersatzwert als PV-Spannung ausgeben. | Noch offen. Der Quellstand besitzt bereits einen aktiven Registerabruf und Speicherung. Auslösebedingungen, Aktualität und Kachelzuordnung müssen als gesamte Datenkette geprüft werden. Die Beschriftung „Berechnet“ ist ebenfalls zu prüfen. |
| Sonneneinfluss beim Gaslernen | Historische Sonneneinstrahlung und vorhandenen PV-Ertrag als ergänzende Grundlage betrachten, einen belegten Einfluss auf den Heizbedarf lernen und in einer Gas-Lernnotiz erklären. Fertige Faktoren speichern und für neue Vorhersagen anwenden. | Noch nicht implementiert. Wetterdaten, Modell und Lernnotiz müssen gemeinsam erweitert werden. Gemessene PV-Wh sind keine feste direkte Heizwärme-/Gasgutschrift. Ein Sonnenfaktor darf erst mit ausreichender Daten- und Prüfgrundlage gelten. |
| Wetterrückblick: 14 Tage mit gewichtetem Prognosevergleich | Je vergangenem Tag den gewichteten Mittelwert der ursprünglich für genau diesen Zieltag ausgegebenen Prognosen aus den 14 Tagen davor mit dem tatsächlichen Solarertrag vergleichen. Vorlauf 14–8 Tage schwach, 7–4 stärker, letzte drei Tage am stärksten gewichten; tatsächlichen Akkustand erhalten. | Neu vorgemerkt, noch nicht implementiert. Originalprognosen mit Datum speichern, verfügbare Anzahl und Ertragsabweichung zeigen. Die 14 vergangenen Tage der Ansicht und die 14 Vorlauftage je Zieltag sind getrennte Zeiträume. |
| Mehrere Wettermodelle für Wetter und Ertrag | Vorhersagen verschiedener Wetterzentren für denselben Ort und Zeitraum getrennt abrufen; Wettergrößen und daraus berechnete Solarerträge zu einem nachvollziehbaren Mittelwert mit Modellspanne zusammenführen. | Neu am 27.11.2026 gesammelt, noch nicht implementiert. Vorschlag: DWD ICON, ECMWF IFS und NOAA GFS über die bestehende Open-Meteo-Schnittstelle. Modellhorizonte, Datenstand, Ausfälle und vorhandene Lernfaktoren berücksichtigen. |
| 608PD automatisch als Sondernutzung und externe Ladequelle | Vorhandene Bluetooth-/Journalwerte je Lader für beide Energierichtungen automatisch übernehmen: Wohnwagen → Modellbauakku als Sonderverbrauch; externe Energie → 608PD → Wohnwagenakku als externe Ladung. Schnellwahl „Modellbauakku laden“ / „Wohnwagenakku laden“ direkt beim Lader vorsehen, da derselbe Lader beide Aufgaben übernimmt. Nutzung, Energie, passende Lernfaktoren und Tagesbilanz ohne erneute manuelle Erfassung verbinden. | Am 27.11.2026 gesammelt und um Rückladung präzisiert, noch nicht implementiert. Geräteprofile, Journal und Energiepfad „Powerbank → 608PD → Wohnwagenakku“ existieren. Automatische Bilanzverknüpfung fehlt; Quellenanteil, Nettoakkuwert und Verbrauch nicht doppelt zählen. |
| Langfristtrend: Kalenderwochen, Diagramme, Bewölkung und Modellmix | Zusätzliche frei platzierbare Wettertrend-Kachel neben der Tageswetter-Kachel, mit wischbaren Wochen und Detailaufruf. Vorschau nach ISO-Kalenderwochen Montag–Sonntag mit KW-Nummer und Datum; ausgewählte Werte grafisch mit beschrifteten, vergleichbaren Skalen darstellen; Inhalte ebenso wie bei der Tageskachel einzeln wählen und getrennt speichern. Bewölkung mit passenden Symbolen und Messgröße ergänzen; mehrere geeignete Modelle als Mittel und erkennbare Spanne berücksichtigen. | Neu am 28.11.2026 gesammelt, noch nicht implementiert. Betrifft die gezeigte EC46-Langfristansicht. Teilwochen, fehlende Daten, tatsächliche Modellhorizonte und erhöhte Unsicherheit kenntlich halten. |
| 608PD-Journal: Aufruf und sichtbarer Inhalt | Das gespeicherte Lade-/Powerbankjournal zuverlässig und gut auffindbar öffnen; je Vorgang zeigen, welcher Lader welches Ziel/Akku wann und mit welcher Energiemenge geladen hat. Bei Ladezustand, fehlenden Einträgen, Offlinebetrieb oder Fehlern immer verständliche Rückmeldung; gespeicherte Historie auch ohne aktuellen Bluetooth-Empfang anzeigen. | Neue Praxisrückmeldung am 28.11.2026: Der Aufruf am Vortag zeigte laut Tim nichts. Einstieg und Leseweg sind im Code vorhanden; konkrete Ursache am Gerät noch nicht belegt. Als Fehlerbehebung prüfen, nicht als bereits repariert darstellen. |
| Tageswetter-Kachel: Temperatur und vollständigeres Wetter | In der vorhandenen Tageswetter-/Solarvorschau Temperaturminimum/-maximum direkt je Tag anzeigen; Wetterzustand/Bewölkung, Niederschlag und Wind ergänzen. Sonnenstunden, Solarertrag und Akkubalken erhalten; weitere Details beim Antippen, übersichtliche Darstellung mit einzeln wählbarem Temperaturmittel, Minimum, Maximum und weiteren Wetterbestandteilen je Kachel. | Neu am 28.11.2026 gesammelt, noch nicht implementiert. Temperatur, Temperaturminimum und -maximum werden bereits abgerufen und im Tagescache gespeichert, fehlen aber in der kompakten Tageskachel. Weitere Wettergrößen benötigen passende Datenfelder und deren durchgängige Übernahme. |
| VPN und lokaler Zugriff auf die Zentrale | Lokale Zentrale möglichst über das passende physische WLAN-/Hotspot-Netz erreichen, ohne Internetverkehr pauschal am VPN vorbeizuführen. VPN-Adressen nicht mit echten LAN-Adressen verwechseln; bei Sperren verständliche Diagnose und Hinweise anbieten. | Neue Praxisbestätigung am 28.11.2026: Tim bestätigt die Abhängigkeit des Zugriffs vom VPN. Technischer Ansatz in der App möglich, aber von Android-/VPN-Richtlinien und verfügbaren Netzen abhängig. Noch keine Umsetzung oder allgemeine Kompatibilitätszusage. |

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
- **Wann und wie lange begrenzt wurde:** beobachteten Beginn, Ende und Dauer
  je Phase beziehungsweise Tag festhalten. Messlücken als unbekannte Zeit
  erhalten. Erkennbare Begrenzungszeiten und unbekannten zusätzlichen möglichen
  Ertrag getrennt darstellen; aus der Dauer allein keine verlorenen Wh ableiten.
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

## Mehrere Wettermodelle für Wetter und Solarertrag · 27.11.2026

Tim möchte weitere Wetterdienste beziehungsweise Vorhersagegrundlagen einbeziehen
und daraus einen Mittelwert für Wetter und Ertragsprognose erhalten. Dies ist
ein zusätzlicher gesammelter Entwicklungspunkt; kein neuer APK-Bau ist beauftragt.

Am 28.11.2026 ergänzt Tim den Wunsch ausdrücklich auch für die Langfristtrend-
Ansicht. Die unten beschriebene Darstellung nach Kalenderwochen übernimmt
geeignete zusätzliche Modelle je tatsächlich verfügbarem Horizont und Messgröße.
Kurzfristmodelle nicht künstlich auf 46 Tage verlängern.

**Geprüfte technische Grundlage:** Der bisherige `WeatherClient` verwendet
Open-Meteo ohne explizite Modellauswahl. Open-Meteos automatische Best-Match-Auswahl
verbindet bereits regionale und globale Grundlagen je Verfügbarkeit. Sie ersetzt
aber keinen ausdrücklich nachvollziehbaren Vergleich getrennter Modellprognosen.
Über dieselbe Schnittstelle lassen sich auch einzelne Modelle verschiedener
Wetterzentren abrufen. Vorschlag für den Einstieg: DWD ICON, ECMWF IFS und
NOAA GFS. Ein Modell mehrfach über verschiedene Dienste abzurufen erzeugt keine
zusätzliche unabhängige Stimme. Auch verschiedene Modellfamilien sind nicht
vollständig statistisch unabhängig.

**Vorgeschlagene Umsetzung:**

- Gleichen Ort, gleiche Anlage, Größen, Einheiten, Zeitzone und Gültigkeitsintervalle
  vergleichen. Temperatur, Wind und Einstrahlung numerisch verarbeiten; Wettercodes
  nicht arithmetisch mitteln. Ein Wettersymbol aus den fachlich passenden Größen
  beziehungsweise einer nachvollziehbaren Kategorienentscheidung ableiten.
- Für jedes geeignete Modell den Solarertrag mit derselben Anlagenkonfiguration
  und den bereits gespeicherten Kalibrierfaktoren berechnen. Regler- und Anlagenlimits
  pro Modell berücksichtigen, danach vergleichbare Ertragswerte mitteln. Das
  Mitteln von Wetter und anschließende Begrenzen liefert bei nichtlinearen
  Begrenzungen nicht zwingend denselben Ertragsmittelwert.
- Zunächst gleiche Gewichte je Modellfamilie für vorhandene geeignete Werte
  vorschlagen. Einen nach Ort, Vorlauf und Zielgröße erlernten Gewichtsvorteil erst
  übernehmen, wenn er an zurückgehaltenen tatsächlichen Vergleichsdaten belastbar
  besser abschneidet. Keine pauschale Überlegenheitsgarantie des Mittelwerts.
- Mittelwert, Anzahl nutzbarer Modelle und deren Spanne nachvollziehbar anzeigen.
  Eine Min-/Max-Modellspanne ist kein statistisch kalibriertes Vertrauensintervall.
  Große Unterschiede als unsichere Vorhersage erkennbar machen.
- Nur für den jeweiligen Vorhersagezeitpunkt tatsächlich vorhandene Modellwerte
  verwenden und Gewichte darauf normalisieren. Fehlende oder abgelaufene Werte
  nicht als null zählen. Bei nur einer brauchbaren Quelle deren eingeschränkte
  Grundlage zeigen. Ohne aktuelle Grundlagen letzten gespeicherten Stand mit
  Alter/Fehlerstatus erhalten; keine Live-Aktualität vortäuschen.
- Vorhersagelängen sind verschieden: ICON global ungefähr 7,5 Tage, regionale
  ICON-Modelle kürzer; ECMWF IFS bis 15, GFS bis 16 Tage laut geprüfter
  Dokumentation. Deshalb nicht für sämtliche 16 Tage drei Modelle behaupten.
  Teilweise abgedeckte Tage nicht unbemerkt als vollständigen Tagesertrag mitteln.
  Konkrete Varianten, verfügbare Felder und API-Bedingungen vor Umsetzung prüfen.
- Ursprüngliche Einzelprognosen sowie den damals verwendeten gemeinsamen
  Prognosewert dauerhaft mit Ausgabe-/Abrufzeit, Modellherkunft, Gültigkeitszeitraum,
  Ort und Gewichtung speichern. Für den geplanten 14-Tage-Rückblick gilt der
  damalige ausgegebene Gesamtwert; Einzelmodelle bleiben nachvollziehbar.
  Modellgewichtung und Gewichtung früherer Vorhersagetage sind getrennte Schritte.
- Modellgüte nur an passenden tatsächlichen Grundlagen bewerten. Durch vollen
  Akku oder sonstige Begrenzung verfälschte Ertragsabschnitte nicht als Beleg
  schlechterer Wetterprognosen heranziehen. Keine nachträglich neu berechneten
  historischen Prognosen zum eigenen Gütenachweis verwenden.
- Zentrale erfasst, speichert und synchronisiert die begrenzten Wettergrundlagen;
  Anzeigehandy/eigenständiger Betrieb berechnet Mischung, Ertrag und gegebenenfalls
  Gewichte. Cache zuerst anzeigen, Hintergrundabrufe begrenzen; kein Neulernen
  aller Rohdaten bei jedem Modellabruf. Bestehende Gas-Untergrenze, Ortszuordnung,
  Rohdaten und Nutzereinstellungen erhalten.

**Prüfbedarf bei Umsetzung:** Normalisierung/Nullwerte, fehlende Modelle oder
Variablen, veraltete Läufe, unterschiedliche Horizonte und Tagesabdeckung,
Zeitumstellung, doppelte Modellfamilien, Ertragsbegrenzung vor Mittelwert,
kategorische Wettercodes, persistierte ursprüngliche Prognosen und Wiederanlauf
ohne vollständigen neuen Lernlauf. Es wurden hierfür noch keine App-Funktionstests
oder Hardwaretests ausgeführt.

Geprüfte Schnittstellenunterlagen:
[Open-Meteo / Modellauswahl](https://open-meteo.com/en/docs),
[DWD ICON](https://open-meteo.com/en/docs/dwd-api),
[ECMWF](https://open-meteo.com/en/docs/ecmwf-api),
[NOAA GFS](https://open-meteo.com/en/docs/gfs-api).

## 608PD automatisch als Sondernutzung lernen · 27.11.2026

Tim weist auf den vorhandenen direkten Bluetooth-Zugriff auf seine Modellbaulader
hin. Er erwartet wie beim Roller automatische Erkennung und Lernen statt
erneuter manueller Eingabe derselben Ladeenergie. Der bisherige manuelle
Sondernutzungseintrag ist dafür nur ein vorhandener Umweg, keine vollständige
Lösung dieses Wunsches. Die automatische Verbindung ist noch nicht implementiert.

### Geprüfter vorhandener Stand

- `EnergyProtocol.ChargerSample` decodiert Betriebsart, Akkuspannung, Akkustrom,
  Laufzeit, Kapazitätszähler und Zellen. Daraus entsteht keine unabhängig gemessene
  USB-/Wohnwagen-Eingangsenergie.
- `ChargerJournal` speichert Grundlagen getrennt nach Quelle und physischem Lader.
  Aktive Messungen werden ungefähr alle fünf Sekunden und Zustandswechsel sofort
  gespeichert. Die Auswertung auf Anzeige/eigenständigem Handy erkennt Vorgänge
  und integriert geeignete Spannung-/Stromabschnitte als `batteryWh`.
  Fehlender Beginn, fehlendes Ende, Zählerrücksetzung und Messlücken bleiben erkennbar.
- Das allgemeine Sondernutzungslernen in `special-use-core.js` nutzt für
  Nicht-Roller derzeit die manuell erfassten `config.events`. Es übernimmt keine
  608PD-Journalvorgänge automatisch. `SpecialUses.windows()` und der entsprechende
  Verbrauchslernpfad in `SolarPlanning` kennen dafür ebenfalls noch keine
  automatisch aus den Laderjournalen abgeleiteten Nutzungsfenster.

### Gewünschte Verbindung der vorhandenen Funktionen

- Je eingerichteten physischen 608PD eine stabile Zuordnung zur Sondernutzung
  bereitstellen, aus vorhandenen Geräteprofilen und Journalgrundlagen ableiten.
  Umbenennen und wechselnde Auswahl erzeugen keine neuen Verbraucher. Geräte-
  und Anlagenherkunft erhalten; mehrere Lader getrennt lernen. Bewusst gewählte
  Einstellungen und vorhandene manuelle Nutzungseinträge bei Migration erhalten.
- Beginn, laufende Nutzung, Ende und beobachtete Energie automatisch aus
  passenden Betriebszuständen übernehmen. Derselbe Vorgang darf nach Neustart,
  erneutem Abgleich oder nachgelieferten Rohdaten nicht doppelt zählen.
  Nachgelieferte Werte korrigieren gezielt den bestehenden Vorgang.
- Bedarf je ausreichend beobachteter Ladung und tatsächliche übliche Häufigkeit
  getrennt lernen, vorhandene Rollermechanismen nach fachlicher Eignung
  wiederverwenden. Nicht ganztägigen Bluetooth-Empfang als Voraussetzung für
  jede einzelne verwertbare Ladung verlangen.
- Fehlende Zeitabschnitte sind keine Nullnutzung. Für eine verlässliche Häufigkeit
  braucht es ausreichend belegte Beobachtungs-/Versorgungszeiten, einschließlich
  belegter Nichtnutzung. Ein ausgeschalteter oder nicht erreichbarer Lader allein
  belegt keinen verbrauchsfreien Tag. Bis zur ausreichenden Grundlage
  Energie je Ladung und noch unbekannte Häufigkeit getrennt anzeigen; eine
  freiwillige Annahme bleibt ausdrücklich eine Annahme.
- `608PD-Akkuseite` und `Wohnwagenversorgung` unterscheiden. Ladeenergie des
  angeschlossenen Modellbauakkus automatisch messen beziehungsweise aus passenden
  Messintervallen integrieren. Für den Wohnwagenbedarf Wandlungsverluste und
  gegebenenfalls Leerlaufanteil berücksichtigen: nur mit geeigneter unabhängiger
  Eingangsgrundlage als gemessen bezeichnen, sonst eine nachvollziehbare
  konfigurierbare Schätzung verwenden. Kein pauschaler fester Wirkungsgrad als
  Messwert; die gesamte Shuntleistung allein isoliert keinen Laderwirkungsgrad.
- Den wirklichen Energiepfad beachten. Nur vom Wohnwagen versorgte Ladevorgänge
  sind dort Sonderverbrauch. Externe Versorgung und `house-source` beziehungsweise
  Powerbank → 608PD → Wohnwagenakku als Quelle einordnen. Powerbank-/Entlademodus
  und unbekannte Richtung nicht automatisch zum Wohnwagenverbrauch addieren.
  Vorhandene Profilzuordnungen nutzen; unklarer Energiepfad bleibt offen.
- Geeignete bekannte Ladefenster beim normalen Verbrauchslernen berücksichtigen,
  damit dieselben Ladungen nicht zugleich als Grundbedarf und zusätzlich unter
  „Mit Extras“ eingeplant werden. Ausschluss/Zuordnung auf betroffene Zeitfenster
  begrenzen, benachbarte normale Messgrundlagen erhalten. Der tatsächlich am Shunt
  gemessene Gesamtverbrauch wird dadurch weder überschrieben noch nochmals erhöht.
- Aus gespeicherten Faktoren die übliche zusätzliche Nutzung für „Mit Extras“
  ableiten. Konkrete geplante Nutzung oder eine nachvollziehbare laufende Ladung
  für den betreffenden Zeitraum anstelle desselben pauschalen Ansatzes verwenden.
  Nicht aus Akkuspannung allein ein exaktes Ladeziel oder eine Restladeenergie
  erfinden; fehlendes Ziel beziehungsweise unzureichende Beobachtung kennzeichnen.
- Fertige Faktoren mit Herkunft, Qualität und Eingangsrevision dauerhaft speichern
  und inkrementell ergänzen. Neue Rohpakete müssen nicht sämtliche alten Ladungen
  erneut auswerten. Zentrale bleibt Sammler und Synchronisationsgerät; Auswertung,
  Lernen und Prognose auf Anzeigehandys/eigenständig.
- Manuelle Sondernutzung bleibt als Ergänzung/Korrektur möglich. Eine bestehende
  manuelle Eintragung und derselbe automatisch erkannte Vorgang dürfen keinen
  doppelten Bedarf erzeugen. Automatisch angelegte Zuordnung und Lernbasis
  verständlich im Lader-/Extrasbereich anzeigen.

### Externe Energie über den 608PD in den Wohnwagenakku

**Präzisierung von Tim am 27.11.2026:** Wenn er anderweitig Energie beschafft
und über einen Modellbaulader in die Wohnwagenbatterie lädt, soll dieser
Vorgang automatisch aufgezeichnet und als Energiezufuhr verbucht werden.
Das ist ausdrücklich Teil derselben automatischen 608PD-Anbindung.

Die Richtung richtet sich nach dem tatsächlich gewählten Energiepfad:

| Energiepfad | Buchung |
|---|---|
| Wohnwagenversorgung → 608PD → Modellbauakku | Sonderverbrauch am Wohnwagen; Akkuseitenenergie und gegebenenfalls geschätzte Eingangsverluste getrennt. |
| Externer Energiespeicher / externe Versorgung → 608PD → Wohnwagenakku | Externe Batterieladung mit eigener Quellenzuordnung und beobachteter gelieferter Energie. |

- Die Auswahl **Modellbau → Einrichten → Energiepfad → „Powerbank → 608PD →
  Wohnwagenakku“** ist im vorhandenen Geräteprofil bereits vorgesehen
  (`role=house-source`). Das vorhandene Journal speichert die Rolle zu jedem
  Messpunkt; ein beobachteter Rollenwechsel beendet beziehungsweise trennt den
  zugehörigen Vorgang. Diese Grundlagen weiterverwenden.
- Pro beobachtetem Rückladevorgang Lader, Quelle/Energiepfad, Beginn, Ende,
  Messabdeckung und gelieferte Wh automatisch speichern beziehungsweise auf der
  Anzeige aus den gespeicherten Rohdaten ableiten. Tageswerte als beispielsweise
  **„Externe Ladung · 608PD“** ausweisen; diese Energie nicht als Dach-/Mobilsolarertrag
  oder als Sonderverbrauch buchen.
- Wenn der Wohnwagenakku an der Akkuseite des 608PD geladen wird, ist dessen
  Akkuseitenleistung die gemeldete elektrische Abgabe in diesen Ladepfad.
  Dafür aus geeigneten Spannung-/Stromintervallen die beobachteten gelieferten Wh
  verwenden. Nicht erneut den Wirkungsgradaufschlag des umgekehrten Modellbau-
  Ladefalls auf diese abgegebene Energiemenge anwenden. Verbrauch des externen
  Speichers und etwaige Leitungs-/Speicherverluste sind damit nicht separat gemessen.
- Die Beschaffung oder Aufladung einer externen Powerbank allein erzeugt noch
  keine verbuchte Wohnwagenladung. Maßgeblich ist der tatsächlich beobachtete
  Energiefluss in den Wohnwagen-Ladepfad. Eine erst geplante Rückladung bleibt
  eine kenntliche Planungsannahme und verändert keine historischen Messwerte.
- Beide Geräte können im Betriebszustand „Laden“ sein: Die Betriebsart oder die
  Akkuspannung allein unterscheiden Modellbauakku und Wohnwagenakku nicht sicher.
  Den tatsächlichen Energiepfad bei geänderter Verwendung richtig zuordnen.
  Frühere Vorgänge behalten ihre damalige Zuordnung; nicht rückwirkend mit der
  heutigen Profilrolle umdeuten. Unklare Richtung bleibt unbekannt.
- Der SmartShunt meldet den Nettofluss des Wohnwagenakkus. Sofern der zusätzliche
  Ladepfad dort korrekt erfasst ist, steckt sein Einfluss bereits im Messwert und
  Ladezustand. Den Quellenanteil separat nachweisen, aber denselben Zufluss nicht
  nochmals auf SoC, Akkuenergie oder die gemessene Akkubilanz aufaddieren.
- Laufende Verbraucher können einen Teil der extern zugeführten Energie sofort
  nutzen. Gelieferte Wh des Laders und Netto-Wh am Wohnwagenakku müssen deshalb
  nicht identisch sein. Bei vollständigen, auf dieselbe Systemgrenze normierten
  Grundlagen gilt: Verbrauch = Solarzufuhr + externe Zufuhr − Netto-Akkuladung.
  Fehlt eine Grundlage, daraus keinen vollständigen Verbrauch erfinden.
- Neutrales Beispiel: Der 608PD liefert 200 Wh, andere Verbraucher benötigen
  gleichzeitig 50 Wh. Dann sind 200 Wh externe Zufuhr und etwa 150 Wh Netto-
  Akkuladung möglich. Nicht zusätzlich 200 Wh auf die bereits gemessenen
  150 Wh aufschlagen und daraus 350 Wh Akkuzuwachs behaupten.
- Die zusätzliche Quelle bei Verbrauchslernen, Energieherkunft und korrigierten
  Tagesauswertungen berücksichtigen. Eine Akkuzunahme durch externe Ladung darf
  weder als zusätzlicher Solarertrag noch als geringerer normaler Verbrauch
  gelernt werden. Bei fehlender Zuordnung betroffene Lernintervalle offen lassen.
- Für Prognosen den aktuellen tatsächlichen SmartShunt-Ladezustand als Grundlage
  verwenden. Künftige extern beschaffte Energie nicht allein aus früheren
  Rückladungen als sicher verfügbar einplanen. Eine ausdrücklich geplante Menge
  bleibt eine Annahme mit eigener Herkunft.
- Nachgelieferte Daten und unterbrochene Verbindungen nur gezielt ergänzen.
  Beobachtete Teilenergie erhalten, fehlende Wh nicht aus dem späteren SoC
  rückwirkend einem bestimmten Lader zuschreiben. Rohdaten und vorhandene
  Profile bleiben erhalten; Neustart und erneute Replikation erzeugen keine
  zweiten Buchungen.

### Schnellwahl für einen Lader mit wechselnder Verwendung

**Präzisierung von Tim am 27.11.2026:** Er verwendet zunächst denselben
physischen 608PD sowohl für Modellbauakkus als auch für das Laden des
Wohnwagenakkus. Dafür eine direkt erreichbare Schnellwahl vorsehen und prüfen,
ob sich die Verwendung künftig zuverlässig automatisch erkennen lässt.

- Auf der Laderkarte eine klar beschriftete, mit einem Tipp erreichbare Auswahl
  **„Modellbauakku laden“** / **„Wohnwagenakku laden“** vorsehen. Zugeordneten
  Lader und aktuell gespeicherte Richtung sichtbar lassen; Bedeutung nicht
  ausschließlich durch Farbe vermitteln. Kein wiederholter Weg durch die
  vollständige Geräteeinrichtung.
- Dieselbe physische Gerätekennung, private Verbindung und das bestehende Journal
  weiterverwenden. Weder erneut koppeln noch zwei konkurrierende physische
  Geräteprofile für dieselbe Adresse anlegen. Die bisherige Option „Andere
  Anwendung“ und bewusste bestehende Einstellungen bei der Erweiterung erhalten.
- Auswahl gerätebezogen dauerhaft speichern. Beim Umschließen passend umstellen;
  solange dieselbe Aufgabe bleibt, ist keine erneute Auswahl pro Ladung nötig.
  Vom Anzeigehandy gilt die neue Auswahl erst nach Speicherbestätigung der
  Zentrale als gespeichert. Fehler oder unerreichbare Zentrale sichtbar
  behandeln, keine erfolgreiche Umstellung vortäuschen.
- Die Schnellwahl ändert die fachliche Zuordnung für folgende beobachtete
  Energieabschnitte. Sie betätigt keinen physischen Umschalter, startet oder
  beendet keine Ladung und setzt kein Ladeprogramm am 608PD. Dafür bleiben
  die tatsächliche Verkabelung und die getrennten Bedienfunktionen maßgeblich.
- Umschaltzeitpunkt und Herkunft speichern; eine laufende Beobachtung am
  Rollenwechsel nachvollziehbar teilen. Bereits aufgezeichnete Vorgänge behalten
  ihren damaligen Energiepfad. Unsichere Übergangszeiten oder fehlende
  Messabschnitte nicht willkürlich einer der Richtungen zuschreiben.
- Automatische Erkennung als gesonderten Prüfpunkt behandeln: feststellen, ob
  zusätzliche tatsächlich verfügbare, verifizierte Geräteinformationen die
  beiden Verwendungen eindeutig unterscheiden. Der derzeit ausgelesene Zustand
  „Laden“, Spannung und Zellenzahl reichen allgemein nicht aus, um Modellbauakku,
  Wohnwagenakku und Herkunft der Versorgung eindeutig zu bestimmen. Auch der
  Shunt-Nettofluss kann durch gleichzeitigen Solarertrag und Verbraucher
  überlagert sein.
- Ohne belegbar zuverlässige Unterscheidung keine automatische Buchungsrichtung
  versprechen oder die manuelle Wahl unbemerkt überschreiben. Die explizit gewählte
  Zuordnung ist maßgeblich und sichtbar; eindeutige Widersprüche als unklar melden.
  Falls später eine Erkennung umgesetzt wird, deren Grundlage und Ausfälle
  nachvollziehbar machen und die manuelle Schnellwahl weiterhin erhalten.
- Unabhängig von dieser Auswahl sollen danach Vorgangserkennung, beobachtete
  Energie und deren passende Verbuchung automatisch laufen. Es bleibt keine
  zusätzliche Pflicht zur manuellen Eingabe derselben Wh pro Vorgang.

**Zusätzlicher Prüfbedarf:** Ein Lader mit wechselnder Aufgabe; ein Tipp ohne
erneute Kopplung; persistierte Auswahl nach App-Neustart; bestätigte Übernahme
Zentrale/Anzeige sowie Fehlerfall; Rollenwechsel während eines beobachteten
Vorgangs mit unveränderter Vergangenheit; keine Lade-/Stopp-/Profilbefehle durch
die Schnellwahl; unklare und widersprüchliche Erkennungsgrundlagen; keine
Doppelbuchung zwischen Verbrauch und externer Ladung. Schnellwahl und mögliche
Erkennung sind noch nicht implementiert oder an Hardware getestet.

### Prüfbedarf bei Umsetzung

Mehrere parallele Lader und stabile Herkunft; unveränderte Gerätekennungen bei
Umbenennen; Laden/Powerbank/externe Wohnwagenladung; gemessene Akkuseite versus
geschätzter Eingang; unklare Quelle; begonnene, beendete und lückenhafte Vorgänge;
Zählerrücksetzung; nachgelieferte Messdaten; keine Doppelungen beim Wiederanlauf
oder manuellen Korrekturen; getrennte Häufigkeit und Ladeenergie; gezielte
Grundverbrauchsbereinigung; unveränderte Rohdaten, Profile und Journale;
wiederverwendeter Lernstand und unveränderte Zentrale/Anzeige-Aufgabenteilung.
Zusätzlich: explizite externe Quellenbuchung und Tagesaggregation; Quellenwechsel
am selben Lader mit erhaltener Historienrolle; gleichzeitiger Solarertrag, Verbrauch
und externe Ladung; Brutto-Zufuhr versus Shunt-Nettofluss; keine SoC-Doppelzählung;
keine erfundene Erhöhung des Solarertrags; korrektes Beispiel 200 Wh Zufuhr und
50 Wh Verbrauch; Teilenergie bei Messlücken; geplante versus beobachtete Zuführung.

Der Stand ist ein konkretisierter Sammelpunkt. Für diese Verbindung wurden
noch kein Produktivcode geändert, keine Funktions-/Hardwaretests ausgeführt und
keine neue APK gebaut.

## Langfristtrend: Kalenderwochen, Diagramme, Bewölkung und Modellmix · 28.11.2026

Tim möchte die gezeigte Ansicht **„Langfristtrend · erhöhte Unsicherheit“**
übersichtlicher gestalten: zu sämtlichen angegebenen Werten Diagramme,
gut lesbare Skalen und zusätzlich Bewölkungssymbole. Während der Abstimmung
ergänzt er einen Mix verschiedener Wettermodelle und eine Orientierung an
Kalenderwochen Montag–Sonntag statt rollierender Sieben-Tage-Zeiträume ab heute.
Alle drei Wünsche gehören gemeinsam zu diesem neuen Sammelpunkt.

### Geprüfter Ausgangspunkt

Die gezeigte 3.1.6-Ansicht listet Wochenmittel für Temperatur, Niederschlag
und Einstrahlung als Text. `WeatherClient.trend()` ruft aktuell für den gewählten
Ort bis zu 46 Tage aus `ecmwf_ec46_ensemble_mean` ab, mit den drei Tagesgrößen
`temperature_2m_mean`, `precipitation_sum` und `shortwave_radiation_sum`.
Die Darstellung liegt in `app/src/main/assets/update-ui.js`.

EC46-Ensemblemittel ist bereits das Mittel verschiedener Ensemblemitglieder
dieser Modellfamilie, noch kein Vergleich mehrerer unterschiedlicher
Wettermodellfamilien. Die geprüfte Open-Meteo-Dokumentation bietet auch
Bewölkungsgrößen an; konkrete Parameter, Modell-/Variablenverfügbarkeit und
Antwortform vor Umsetzung prüfen. Der bisherige Trendabruf fordert diese
Bewölkungsdaten noch nicht an.

### Kalenderwochen und Orientierung

- Einheitliche **ISO-Kalenderwochen Montag 00:00 bis zum folgenden Montag
  00:00 exklusiv** am Vorhersageort. Sichtbarer Datumsbereich Montag–Sonntag;
  beispielsweise **„KW 49 · 30.11.–06.12.“**. Einheit und Ortzeitbezug auf allen
  Übersichten und Detailansichten beibehalten.
- KW-Nummer und **ISO-Wochenjahr** korrekt berechnen. Eine Woche am Jahreswechsel
  nicht am 1. Januar künstlich teilen. Kalenderdatum/Jahr und Wochenjahr können
  voneinander abweichen; KW 53 unterstützen.
- Für die laufende Kalenderwoche den verbleibenden verfügbaren Prognoseteil
  zeigen und mit Datum/Anzahl abgedeckter Tage als **Teilwoche** kenntlich machen.
  Vergangene Wochentage nicht als Null oder als nachträgliche Vorhersagen ergänzen.
  Der sichtbare Rahmen bleibt die Kalenderwoche Montag–Sonntag.
- Eine am Modellhorizont angeschnittene letzte Kalenderwoche ebenfalls als
  Teilwoche kennzeichnen. Der Kalenderwochenbezug verlängert den tatsächlichen
  Vorhersagehorizont nicht; je Starttag können mehr Wochenabschnitte als bei
  rollierenden Sieben-Tage-Blöcken entstehen.
- Grafiken, Wochenkarten, Bewölkungsangaben und Modellvergleiche verwenden
  dieselben Wochenintervalle. Mittelwerte auf tatsächlich vorhandene Daten
  beziehen, Abdeckung nennen; Teilwochen nicht als vollständige Wochen ausgeben.

### Grafische Darstellung aller Werte

- **Temperatur:** gut lesbares Linien-/Punktdiagramm über die Kalenderwochen,
  Skala in **°C**, sichtbare Null-/Frostlinie und korrekt dargestellte negative Werte.
- **Niederschlag:** Balkendiagramm mit Skala in **mm/Tag** für den gezeigten
  durchschnittlichen Tagesniederschlag. Eine etwaige Wochensumme eindeutig anders
  beschriften; Mittelwert und Summe bei Teilwochen nicht verwechseln.
- **Sonneneinstrahlung:** Balken-/Verlaufsdarstellung in **MJ/m²/Tag**, passend zum
  bisherigen Wochenmittel der Tagesstrahlungsmenge. Eine optionale Umrechnung
  separat und korrekt als Einstrahlung pro Fläche beschriften; nicht ungeprüft
  als tatsächlichen Anlagen-/Akkuertrag in Wh ausgeben.
- **Bewölkung:** verständliche Sonne-/Wolken-Symbole sowie, soweit verfügbar, eine
  Bewölkungsskala **0–100 %** mit Zahlenwert und nachvollziehbarem Wochenbezug.
  Aus tatsächlichen gelieferten beziehungsweise fachlich aggregierten
  Bewölkungsgrundlagen ableiten. Weder Niederschlagsmenge noch niedrige winterliche
  Einstrahlung allein belegen einen konkreten Bewölkungsgrad. Bei fehlender
  Grundlage offen kennzeichnen und keine fiktive sichere Sonne/Wolke einsetzen.
- Für jede Messgröße eine **über alle sichtbaren Wochen gemeinsame Skala**
  verwenden. Nicht jede Wochenkarte auf ihren eigenen Höchstwert normieren.
  Verschiedene Einheiten auf getrennten Skalen; Temperatur nicht als beliebigen
  Prozentbalken darstellen. Niederschlag und Strahlung sinnvoll bei null beginnen.
- Zahlenwerte erhalten, Wochen beschriften und Legenden/Einheiten sichtbar machen.
  Die Übersicht soll Unterschiede auf einen Blick zeigen; Antippen kann
  Quellen, genaue Werte und Abdeckung ergänzen. Lücken nicht unbemerkt als
  durchgehende Mess-/Prognoselinie oder als null zeichnen.
- Vorhandene Farben und bewusste Anpassungen beachten, klare Symbole und Text
  ergänzen. Gute Kontraste, schmale Handyansicht, Hoch-/Querformat und vergrößerte
  Schrift prüfen; keine überlagerten Achsen oder Beschriftungen.
  Bereits gespeicherte Daten sofort darstellen. Die Visualisierung darf keine
  erneuten Lernläufe oder zusätzliche große Abrufe bei jedem Öffnen verursachen.

### Modellmix auch in dieser Ansicht

- Den bestehenden Wunsch nach mehreren Wettermodellen ausdrücklich auf den
  Langfristtrend erweitern. Nur geeignete Modelle, die für den jeweiligen Ort,
  Zeitraum und die jeweilige Wettergröße tatsächlich Daten liefern, kombinieren.
  Die bisherigen DWD-/ECMWF-/NOAA-Kurzfristvorschläge decken nicht pauschal
  alle 46 Tage ab; geeignete länger reichende Ensemble-/Langfristmodelle prüfen.
- Auf denselben Zeitraum, Ort und physikalische Größe normieren. Absolute
  Temperatur oder Niederschlag nicht mit klimatologischen Anomalien mitteln.
  Unterschiedliche Zeitauflösung und teilweise Wochenabdeckung berücksichtigen.
- Zunächst nachvollziehbare Gewichte je Modellfamilie vorschlagen. Viele Mitglieder
  eines Ensembles dürfen derselben Familie nicht automatisch mehr Gewicht als
  einer anderen geben. Abhängigkeiten und Herkunft sichtbar halten; Varianten
  desselben Modells nicht als unabhängige Wetterdienste zählen.
- **Gemeinsamen Prognosewert und erkennbare Modellspanne** zeigen, etwa als
  Band um einen Temperaturverlauf oder als Spanne an einem Balken.
  Modellanzahl und tatsächlich verwendete Quellen je Kalenderwoche beziehungsweise
  Messgröße nachvollziehbar machen. Bei unterschiedlicher Abdeckung nur
  vergleichbare Zeitintervalle vergleichen.
- Wenn später im Horizont nur ein Modell vorliegt, ausdrücklich **„1 Modell“**
  anzeigen. Fehlende Modelle nicht als null werten und aus fehlender Vergleichs-
  spanne keine vermeintlich sichere Prognose ableiten. Gewichte auf verfügbare
  geeignete Werte normieren; keine Kurzfristwerte künstlich fortschreiben.
- Streuung verschiedener Modelle und Ensembleunsicherheit unterscheiden.
  Eine Min-/Max-Spanne ist kein kalibriertes Vertrauensintervall. Vorhandene
  belastbare Ensembleinformationen können ergänzt werden; keine erfundenen
  Wahrscheinlichkeiten oder präzisen Tageszustände aus einem Wochenmittel.
- Ursprüngliche Einzel-/Gesamtprognosen und verwendete Gewichte/Herkunft erhalten,
  wie im bereits gesammelten Modellmix vorgesehen. Der Hinweis **„Langfristtrend ·
  erhöhte Unsicherheit“**, Quelle, Datenstand und wöchentlicher Orientierungscharakter
  bleiben erkennbar. Keine sichere Tages- oder Akkuvorhersage daraus ableiten.
- Dies ist die länger reichende **Vorschau**; sie ersetzt weder den geplanten
  14-Tage-Rückblick mit Prognosegüte noch dessen Gewichtung früherer Ausgabetage.

### Zusätzliche Wettertrend-Kachel mit Wochenansicht

Tim möchte die Wochenvorschau zusätzlich als Kachel wie die vorhandene
Tageswetter-/Solarvorschau, damit sie ohne Umweg über die große Trendansicht
auf dem Dashboard sichtbar ist.

- Eine eigene frei wählbare **„Wettertrend · Wochen“**-Kachel im gemeinsamen
  Kacheleditor vorsehen. In Standardbereichen und eigenen Dashboards platzierbar,
  verschiebbar und in der Größe einstellbar, mit den vorhandenen Layouts je Gerät
  und Ausrichtung. Bestehende Layouts nicht ungefragt verändern.
- Tageswetter-Kachel und Wochenkachel **gleichzeitig** nutzbar halten. Die neue
  Kachel ersetzt weder die Tagesvorschau noch den Rückblick. Getrennte
  Darstellungscontainer/Elementkennungen verwenden, damit nicht dieselben
  DOM-Inhalte zwischen zwei Kacheln verschoben und dabei unsichtbar werden.
- Analog zur Tagesvorschau horizontal wischbare **Wochenkarten** mit KW-Nummer,
  Datumsbereich Montag–Sonntag, passenden Sonne-/Wolken-Symbolen und kompakten
  grafischen Angaben für Temperatur, Niederschlag und Einstrahlung. Bewölkung
  samt Skala anzeigen, sofern geeignete Grundlagen vorhanden sind.
- Dieselben Wochenintervalle, Teilwochenkennzeichen, Werte, Modellgewichte und
  Datenstände wie in der großen Langfristansicht verwenden. Die dort vereinbarten
  Skalen-, Einheiten- und Unsicherheitsregeln gelten unverändert. Keine neue
  scheinbar sichere langfristige Akkuvorhersage allein aus der optischen
  Ähnlichkeit mit der Tageskachel ableiten.
- Tippen auf eine Woche öffnet deren Details mit Diagrammen, Zahlen, Quellen,
  Modellspanne und Abdeckung. Wischen, Antippen und Kachelbearbeitung dürfen sich
  nicht gegenseitig auslösen. Schmale/breite Kacheln und vergrößerte Schrift
  ohne abgeschnittene Beschriftungen oder überlagerte Skalen behandeln.
- Ort, Quellen/Modellanzahl soweit sinnvoll, Datenstand und erhöhte Unsicherheit
  auch kompakt nachvollziehbar lassen. Fehlende, teilweise oder alte Werte
  entsprechend kennzeichnen; eine leere Kachel ist keine ausreichende Rückmeldung.
- Die unter „Einzeln wählbare Inhalte in Tages- und Wochenvorschau“ präzisierte
  Auswahl gilt auch für die Wochenkachel. Tages- und Wochenkonfiguration dürfen
  unterschiedlich sein; Datenverfügbarkeit und korrekte Wochenaggregation beachten.
- Große Ansicht und Kachel teilen den vorhandenen Wetter-/Trendcache und die
  berechnete Wochenaggregation. Platzieren/Öffnen darf nicht alle Grundlagen
  erneut lernen oder pro Kachel einen eigenen gleichartigen Netzabruf starten.
  Erforderliche Trendaktivierung und fehlenden Wetterort nachvollziehbar behandeln;
  vorhandene Daten sofort anzeigen, notwendige Ergänzungen im Hintergrund.
- In die spätere Prüfung zusätzlich aufnehmen: beide Wetterkacheln nebeneinander,
  eigenes Dashboard, Wechsel der Bereiche/Ausrichtung, gespeicherte Größe/Position,
  Entfernen und erneutes Hinzufügen, Tages-/Wochendetails und Cache-/Offlinefälle.

### Prüfbedarf bei Umsetzung

Montag-/Sonntag-Grenzen, laufende und letzte Teilwoche, lokale Zeit/Zeitzonenwechsel
und Sommerzeit, ISO-Wochenjahr/KW 53 und Jahreswechsel; unveränderter echter
Vorhersagehorizont; korrekte Wochenmittel und Einheiten bei fehlenden Daten;
negative/konstante/null Werte; gemeinsame Skalen; nicht erfundene Bewölkung;
Modellhorizonte und Variablenabdeckung; gleiche Familie mit vielen Mitgliedern;
nur ein Modell, große Modellunterschiede und fehlende Quellen; Erhalt vorhandener
Werte und Unsicherheit; dunkles/helles Design, große Schrift und schmale Ansichten.

Dieser Stand dokumentiert die Wünsche und den geprüften Datenzugriff. Es wurden
für die Erweiterung noch keine App-Diagramme implementiert, keine neue APK
gebaut und keine entsprechenden Funktions-/Hardwaretests durchgeführt.

Geprüfte Hersteller-/Schnittstellenunterlagen:
[Open-Meteo Seasonal / EC46](https://open-meteo.com/en/docs/seasonal-forecast-api),
[Open-Meteo Ensemble-Modelle und Horizonte](https://open-meteo.com/en/docs/ensemble-api).

## 608PD-Journal: Aufruf und sichtbarer Inhalt · 28.11.2026

**Neue Praxisrückmeldung:** Tim berichtet am 28.11.2026, dass beim Betätigen
des Journalzugangs am Vortag nichts angezeigt wurde. Ob kein Dialog erschien,
ein Dialog leer blieb oder ein Datenabruf nicht fertig wurde, ist aus dieser
Rückmeldung noch nicht eindeutig. Keine Datenlosigkeit oder bestimmte
Fehlerursache am Gerät erfinden.

### Geprüfter vorhandener Weg

- `autarpie-energy.js` legt unter **Modellbau** eine Karte
  **„Lade- & Powerbankjournal“** mit **„Journal öffnen“** an. Der Handler ruft
  `openJournal()` auf und erstellt Filter für Lader sowie 7/30/90 Tage.
- `renderJournal()` fragt `chargerJournal` über `app.read` mit dem Bereich
  `608pd-journal` ab; `autarpie-history-ready` soll die Ansicht aktualisieren.
  `app.js` stellt im geprüften Quellstand `read:historyRead` bereit, und die
  Lesebrücke kennt den Methodennamen `chargerJournal`.
- Bereits vorgesehene Anzeigezustände sind Datenabgleich, Fehler und keine
  aufgezeichneten Vorgänge. Es gibt aber einen stillen Rücksprung, wenn die
  Journalansicht beziehungsweise `app.read` fehlt. Die Fokus-/Anfrageverwaltung
  kann Anfragen außerhalb ihres zulässigen Bereichs zunächst nur aus dem Cache
  beantworten. Diese Pfade sind konkrete Prüfpunkte, keine nachgewiesene
  Erklärung für Tims Gerät.
- Das bestehende Journal speichert unabhängig vom aktuellen Liveempfang
  Rohdaten. Ein nicht erreichbarer oder gerade nicht ladender 608PD ist kein
  Grund, bereits gespeicherte Vorgänge unsichtbar zu machen.

### Gewünschtes Ergebnis und Diagnose

- Den Journalzugang im Modellbaubereich beziehungsweise beim betrachteten Lader
  gut sichtbar und mit einem Tipp erreichbar halten. Gewählten Lader erkennen
  lassen; **„Alle Lader“** und Zeitraumwechsel zuverlässig anbieten. Frühere
  Vorgänge entfernter Profile bleiben über das vorhandene Journal auffindbar.
- Der Aufruf muss **sofort sichtbare Rückmeldung** geben. Unterscheiden:
  Abruf läuft; vorhandene gespeicherte Vorgänge; für diesen Filter keine
  Vorgänge; Zentrale nicht erreichbar mit letztem gespeicherten Stand;
  tatsächlich noch keine lokalen Journalgrundlagen; konkrete Fehler.
- Ein legitimes leeres Ergebnis ausdrücklich erklären, statt nur eine leere
  Fläche oder einen scheinbar wirkungslosen Knopf stehen zu lassen.
  Eine fehlende Brücke/Funktion beziehungsweise inkompatible Zentralenversion
  benötigt einen verständlichen Hinweis, keinen stillen Rücksprung.
- Den gesamten Weg prüfen: tatsächlicher Klick/Handler, Dialogaufbau,
  JavaScript-Ausnahmen, Sichtbarkeit, Lesebrückenfokus und Priorität, Anfrage-
  warteschlange/Timeout, native Journalabfrage, Anlagen-/Laderzuordnung,
  gezielter LAN-Abgleich und Rückmeldung an die sichtbare Ansicht.
  Ein geöffnetes Journal darf nicht dauerhaft hinter alten Detailabfragen hängen.
- Vorhandene gespeicherte Daten zuerst anzeigen und ihren Stand kennzeichnen.
  Bei Bedarf gezielt den gewählten Zeitraum abgleichen. Ein unbeendeter Abruf
  muss eine konkrete Meldung und erneute Abrufmöglichkeit erhalten; Historie,
  Filterwahl und vorhandene Einträge dabei bewahren.
- Mehrere Lader, alte/wechselnde Profile und unterschiedliche Zeiträume
  berücksichtigen. Der aktuell gewählte neue Lader kann berechtigt keine
  Vorgänge haben; das darf nicht die gesamte Historie wie verloren erscheinen
  lassen. Beginn und Grenzen der tatsächlichen Aufzeichnung klar benennen.
- Die bereits gesammelte Unterscheidung **Sonderverbrauch / externe Ladung**
  auch im Journal zeigen. Messseite, beobachtete Wh, Beginn/Ende und Lücken
  nachvollziehbar lassen; keine fehlenden Vorgänge erfinden und keine
  doppelten Buchungen beim erneuten Einlesen erzeugen.

### Je Vorgang: Was wurde wann und wie viel geladen?

Tim präzisiert am 28.11.2026, dass er die tatsächlich geladenen Ziele und Mengen
nachvollziehen möchte. Ein funktionierender Journalzugang allein genügt dafür
nicht; der Inhalt muss pro Vorgang unmittelbar verständlich sein.

- Pro Eintrag **Ladername**, **Ladeziel beziehungsweise Verwendung**, Datum,
  Beginn/Ende oder letzter beobachteter Zeitpunkt, beobachtete Dauer und
  nachgeladene Energie in **Wh** zeigen. Vorhandenen Gerätezähler in **mAh**
  ergänzend verständlich beschriften; dieser Wert ist keine automatisch bekannte
  Nennkapazität des angeschlossenen Akkus.
- Ziel/Verwendung eindeutig als **Modellbauakku laden**, **Wohnwagenakku laden**
  oder **Powerbank / Entladen** samt passendem Energiepfad einordnen. Quelle,
  Ziel und gespeicherte damalige Zuordnung erhalten. Quelle/Sonderverbrauch,
  Messseite und gegebenenfalls geschätzte Eingangsverluste weiter unterscheiden.
- Eine **eigene Akkubezeichnung** als optionale Zuordnung zum Vorgang ermöglichen,
  falls ein bestimmter Modellbauakku wiedererkannt werden soll. Der aktuelle
  Decoder liefert Laderzustand und Akku-Messwerte, aber keine verifizierte
  eindeutige Kennung des angeschlossenen Akkupacks. Ladername, gleiche Spannung
  oder gleiche Zellenzahl dürfen nicht als sichere Pack-Identität ausgegeben werden.
- Für die reine automatische Mengen-/Zeiterfassung keine zusätzliche manuelle
  Eingabe derselben Ladung verlangen. Eigene Bezeichnung/Zuordnung ist eine
  Ergänzung, keine Voraussetzung für das Erhalten beobachteter Werte. Unbekanntes
  Ladeziel offen benennen statt einen konkreten Akku zu erfinden.
- Zugeordnete Bezeichnungen und Korrekturen dauerhaft mit nachvollziehbarem Bezug
  zum Vorgang speichern, in der Darstellung und Sicherung erhalten und nicht mit
  einem neuen aktuellen Profilnamen rückwirkend alle alten Ziele umbenennen.
  Originalmesswerte und belegte Energie unverändert erhalten.
- Fehlender Beginn, unbekanntes Ende und Messlücken sichtbar lassen. Beobachtete
  Teilenergie als solche zeigen; laufende oder nur teilweise aufgezeichnete
  Ladungen nicht als vollständig abgeschlossen ausgeben.
- Nach Lader und Zeitraum filtern; passende Summen für die ausgewählte Ansicht
  können ergänzt werden. Gegensätzliche Energierichtungen getrennt ausweisen
  statt Verbrauch und externe Ladung als unbeschriftete Menge zusammenzurechnen.

Diese Präzisierung ergänzt die bereits gesammelte Reparatur des Journalaufrufs
und die automatische 608PD-Verbuchung; sie ist noch nicht implementiert.
Zusätzlich prüfen: klare Ziel-/Laderunterscheidung, optionale Akkubezeichnung,
unbekannte Pack-Identität, historische Zuordnung und Korrekturen, Wh/mAh-Einheiten,
zeitlich unvollständige Einträge und getrennte Summen beider Richtungen.

### Prüfbedarf und Stand

Den echten Einstieg Ende-zu-Ende mit bestätigendem Datenpartner prüfen,
zusätzlich leere Historie, ausgewählter Lader ohne Vorgänge, „Alle Lader“,
vorhandene Historie ohne Live-Bluetooth, offline mit/ohne Cache, alte Zentrale,
verzögerte Antwort, Fehler/Timeout, Fokuswechsel, Schließen/Wiederöffnen und
Wechsel zwischen Modellbaubereich und eigenem Dashboard. Native Rohdatenerhalt
und Viewer-/Standalone-Verhalten getrennt prüfen.

Bisher sind Quellpfad und mögliche Prüfpunkte untersucht. Es gibt noch keine
reproduzierte konkrete Ursache am Gerät und keine veröffentlichte Reparatur
für diese neue Rückmeldung; keine entsprechenden Laufzeit-/Hardwaretests behaupten.

## Tageswetter-Kachel: Temperatur und vollständigeres Wetter · 28.11.2026

Tim möchte in der bestehenden Tageswetter-/Solarvorschau, die er als
14-Tage-Wetterkachel bezeichnet, neben Sonneneinstrahlung auch Temperaturen und
weitere Wetterinformationen auf einen Blick sehen. Die neue Wochenkachel ersetzt
diese Erweiterung der vorhandenen Tageskachel nicht.

### Geprüfter vorhandener Stand

`WeatherClient` fordert für die reguläre Vorschau bereits
`temperature_2m_mean`, `temperature_2m_min` und `temperature_2m_max` an.
`timeline()` speichert sie in den Tagesgrundlagen als `temperature`,
`temperatureMin` und `temperatureMax`, einschließlich gültiger negativer
Temperaturen und ausdrücklich fehlender Werte.

Die kompakte Tageskarte in `solar-ui.js` zeigt derzeit Datum, Wettersymbol,
Sonnenstunden und Solar-/Akku-/Extrasbalken, aber keine Temperaturzeile.
Die reguläre Abfrage unterstützt im aktuellen Stand bis zu 16 Tage; die
Bezeichnung im Gespräch ist kein Auftrag, einen vorhandenen längeren
Vorhersagehorizont ungefragt zu verkürzen.

### Gewünschte Darstellung und Datenweg

- **Temperaturminimum und -maximum in °C direkt je Tageskarte** sichtbar machen,
  beispielsweise „−2 / 5 °C“ als neutrales Darstellungsbeispiel. Vorhandener
  Tagesmittelwert kann in Details ergänzt werden. Damit die Temperaturen erkennbar
  sind, soll dafür kein zusätzlicher Umweg über Einstellungen oder Tagesdetails
  erforderlich sein.
- Wetterzustand mit passenden Sonne-/Wolken-/Regen-/Schneesymbolen erhalten
  beziehungsweise ergänzen. Bewölkungsgrad, sofern verfügbar, mit eigener
  Prozentangabe/Skala nachvollziehbar machen. Aus Sonnenstunden oder Einstrahlung
  allein keine genaue Bewölkung erfinden.
- **Niederschlag** mit Menge in mm/Tag und gegebenenfalls separat benannter
  Wahrscheinlichkeit in % ergänzen; Menge und Wahrscheinlichkeit nicht
  austauschbar verwenden.
- **Wind** mit Geschwindigkeit und, sofern verfügbar, Böen samt Einheit
  ergänzen. Hauptangaben übersichtlich halten, weitere Einzelwerte in der
  Tagesdetailansicht zugänglich machen.
- Sonnenstunden, erwarteten Solarertrag, tatsächlichen beziehungsweise
  prognostizierten Akkustand und „Mit Extras“ erhalten. Neue Wetterzeilen dürfen
  eigene bestehende Kachelgrößen, Layouts oder Farben nicht ungefragt ersetzen.
- Für schmale Kacheln und große Schrift eine lesbare kompakte Darstellung
  vorsehen; über den vorhandenen Editor passende sichtbare Bestandteile
  einstellbar machen. Tageskarten bleiben horizontal wischbar und antippbar.
  Werte und Symbole dürfen sich nicht überlagern.
- Vorhandene Temperaturgrundlagen wiederverwenden. Zusätzlich erforderliche
  Niederschlags-/Bewölkungs-/Windfelder anhand des tatsächlichen API-Angebots
  abrufen, validieren, speichern und bis auf Anzeigehandys übertragen. Ein
  angeforderter Wert allein ist noch kein gespeicherter oder angezeigter Wert.
  Wetterort, Zeitgrenzen und Datenstand der übrigen Vorschau beibehalten.
- Fehlende Größen als unbekannt behandeln, gültige null und negative Temperaturen
  korrekt zeigen. Wetterwerte verschiedener Zeitstände/Orte nicht unbemerkt
  vermischen. Alte gespeicherte Werte eindeutig kennzeichnen.
- Gemeinsame Tagesgrundlagen mit Modellmix, Gas-Wetterbedarf, Wochenaggregation
  und Details konsistent nutzen. Die zusätzliche Darstellung löst keine
  vollständige Neuberechnung aller Lernmodelle bei jedem Öffnen aus.
  Tatsächliche Historie und Prognose weiterhin unterscheiden.

### Einzeln wählbare Inhalte in Tages- und Wochenvorschau

Tim präzisiert am 28.11.2026: In beiden Vorschauen soll er selbst auswählen
können, ob er Temperatur, Temperaturminimum/-maximum, Niederschlag und weitere
Wetterangaben sehen möchte. Die Auswahl ist ausdrücklich Teil der geplanten
Erweiterung, keine Behauptung über den derzeit veröffentlichten Funktionsumfang.

- Über den gemeinsamen Kacheleditor beziehungsweise **Wetterkachel bearbeiten**
  **jede einzelne Wetter-/Energieangabe mit eigenem An-/Aus-Schalter** anbieten.
  Tims erneute Präzisierung vom 28.11.2026: kein gemeinsamer Schalter für
  Wettersymbol und Bewölkungsgrad, ebenso kein gemeinsamer Schalter für Wind und
  Böen. Dasselbe Prinzip gilt für alle anderen Angaben.

| Einzeln wählbarer Bestandteil | Eigener Schalter |
|---|---|
| Wetter-/Bewölkungssymbol | Ja |
| Bewölkungsgrad in % | Ja, unabhängig vom Symbol |
| Temperaturmittel | Ja |
| Temperaturminimum | Ja |
| Temperaturmaximum | Ja |
| Niederschlagsmenge | Ja |
| Niederschlagswahrscheinlichkeit, soweit unterstützt | Ja, unabhängig von der Menge |
| Windgeschwindigkeit | Ja |
| Böengeschwindigkeit, soweit unterstützt | Ja, unabhängig vom Windwert |
| Sonnenstunden | Ja |
| Sonneneinstrahlung, soweit für die Ansicht verfügbar | Ja |
| Solarertrag, soweit für die Ansicht verfügbar | Ja |
| Akkustand/-balken, soweit für die Ansicht verfügbar | Ja |
| „Mit Extras“-Akkustand/-balken, soweit für die Ansicht verfügbar | Ja, unabhängig vom normalen Akkustand |

- Rein optische Kategorien in der Bearbeitung sind möglich, dürfen aber keine
  Auswahlkopplung erzwingen. Ein einzelner Schalter verändert nur seinen
  Bestandteil. Symbol ohne Bewölkungsgrad und Bewölkungsgrad ohne Symbol müssen
  ebenso möglich sein wie Wind ohne Böen und Böen ohne Wind.
- Werte, Beschriftung/Einheit und ihre zugehörige grafische Darstellung passend
  zum einzelnen gewählten Bestandteil behandeln. Keine versteckte Kopplung an
  ein anderes Datenfeld oder automatisches Wiedereinschalten ausgeblendeter
  Angaben beim Umschalten, Neuladen oder Wechsel der Ansicht.
- So sind nur ein Temperaturwert, nur Min/Max oder alle drei Angaben möglich.
  Die zugehörigen grafischen Elemente/Skalen sollen der Auswahl folgen.
  Einzelne Werte bleiben in Details zugänglich, soweit passende Daten vorliegen.
- Auswahl **je Kachelinstanz beziehungsweise Vorschau** dauerhaft gerätelokal
  speichern. Tages- und Wochenansicht sowie unterschiedliche eigene Dashboards
  können bewusst verschiedene Inhalte zeigen. Eine Auswahländerung darf nicht
  still andere Wetterkacheln oder die Darstellung auf einem anderen Handy ändern.
- Bestehende eigene Layouts und bewusste Einstellungen bei Migration erhalten.
  Komponentenwahl in die vorhandene Sicherung/gezielte Übernahme der Darstellung
  aufnehmen; Größen- und Farbwahl bleiben erhalten.
- Nur tatsächlich unterstützte Angaben anbieten beziehungsweise bei fehlenden
  Grundlagen mit verständlichem Hinweis offen lassen. Eine angekreuzte Option
  erzeugt keine Wetterdaten. Nicht verfügbare Bewölkung, Wahrscheinlichkeit oder
  Wochenextreme nicht mit null oder erfundenen Werten ersetzen.
- Für Wochen die Bedeutung präzise beschriften: Temperaturmittel aus den
  abgedeckten Tagesgrundlagen; Minimum/Maximum nur aus geeigneten tatsächlich
  verfügbaren Tages-/Intervallwerten, nicht aus einem einzigen Wochenmittel
  erfinden. Temperatur-Extrema sind kein statistisches Unsicherheitsband.
  Regeln für Teilwochen, Niederschlagsmittel/-summe und Modellabdeckung gelten weiter.
- Eine Niederschlagswahrscheinlichkeit für einen Tag nicht ungeprüft als
  Wahrscheinlichkeit für die gesamte Kalenderwoche ausgeben. Unterschiedliche
  Wind-/Böen- und Niederschlagsgrößen samt Einheit verständlich benennen.
- Auswahl betrifft die Darstellung. Erforderliche Grundlagen für Prognosen und
  Lernen nicht beim Ausblenden löschen oder ihre Rechenbedeutung verändern.
  Keine zusätzlichen kompletten Netz-/Lernläufe beim einfachen Umschalten.
- Auch bei reduzierter Anzeige bleiben Datenstand, fehlende/alte Grundlagen und
  erhöhte Unsicherheit der Langfristvorschau nachvollziehbar. Ausgewählte Inhalte
  müssen sich an Kachelbreite und Schriftgröße ohne Überlagerung anpassen.

Zusätzlich prüfen: alle vier An-/Aus-Kombinationen für Symbol/Bewölkungsgrad,
Wind/Böen und Niederschlagsmenge/-wahrscheinlichkeit; sämtliche Kombinationen
von Mittel/Min/Max; unabhängige Energieangaben; kein Schalter verändert ein anderes
Feld; unabhängige Tages-/Wochen- und Dashboardauswahl; Speicherung/Wiederanlauf
und Sicherung ohne ungewolltes Wiedereinschalten;
fehlende Felder oder reine Wochenmittel; unveränderte Daten-/Lernbasis;
kleine Kachel, große Schrift, Farb-/Layoutbestand und zugängliche Bearbeitung.

### Prüfbedarf und Stand

Negative/null/fehlende Temperaturen; Min/Max und Mittelwert; erhaltene Energie-
und Akkubalken; Regenmenge versus Wahrscheinlichkeit; Wind und Böen; fehlende
Bewölkung; Datenweg Zentrale → Anzeige und eigenständig; unveränderter Wetterort
und vorhandener Horizont; gespeicherte Daten/Offlinefall; Wetterkachel in eigenen
Dashboards, schmale/breite Ansicht, große Schrift, Komponentenwahl und bestehende
Farben/Layouts. Noch keine Umsetzung, neue APK oder entsprechende Geräteprüfung.

## VPN und lokaler Zugriff auf die Zentrale · 28.11.2026

Tim bestätigt am 28.11.2026 nach dem Gegencheck, dass der Zugriff tatsächlich
am aktiven VPN hängt, und fragt nach einer Lösung innerhalb von AutarPie.
Das ist eine bestätigte Nutzerbeobachtung der Abhängigkeit, noch kein Nachweis
einer bestimmten VPN-App, Android-Richtlinie oder internen Routingursache.

### Geprüfter technischer Stand

- `LanSync.addresses()` sammelt derzeit passende private IPv4-Adressen aller
  aktiven, nicht als Loopback markierten Schnittstellen. Eine gezielte Trennung
  anhand von Android-Netzwerkfähigkeiten zwischen physischem LAN und VPN ist in
  diesem Pfad nicht vorhanden.
- `LanSync.request()` verwendet unverbundene gewöhnliche `Socket`-Instanzen und
  verbindet diese mit gespeicherten beziehungsweise gefundenen lokalen Gegenstellen.
  Im gelesenen Pfad erfolgt keine explizite Bindung an ein physisches `Network`.
- Android bietet `Network.bindSocket()` beziehungsweise eine netzgebundene
  `SocketFactory`. Damit können einzelne Verbindungen einen gezielten Netzweg
  verwenden, ohne alle Verbindungen des Prozesses umzubinden.
- Das gilt unter den Regeln des aktiven VPNs: Der VPN-Dienst muss ein Umgehen
  zulassen; bei „Verbindungen ohne VPN blockieren“ sind entsprechende Verbindungen
  weiterhin gesperrt. AutarPie kann weder die Konfiguration einer fremden
  VPN-App selbst ändern noch deren Richtlinien durch eine eigene Einstellung
  garantiert außer Kraft setzen.

### Gewünschte Verbesserung und Grenzen

- Eine geeignete automatische lokale Netzführung prüfen, gegebenenfalls mit
  verständlicher Option **„Lokale Zentrale über WLAN/Hotspot bevorzugen“**.
  Im aktuellen veröffentlichten Stand keinen bereits vorhandenen solchen
  Schalter behaupten. Einen erfolgreichen Zugriff mit jedem VPN nicht versprechen.
- Für den verschlüsselten lokalen Anlagenverbund das tatsächlich geeignete
  erreichbare Netz beziehungsweise die passende lokale Route zur gekoppelten
  Gegenstelle bestimmen. VPN- und Mobilfunkadressen nicht allein wegen eines
  privaten IPv4-Präfixes als Wohnwagen-LAN behandeln. Bestehende echte LAN-
  Verbindungen und direkte Nutzung ohne VPN erhalten.
- Wenn ein passendes zugängliches physisches Android-`Network` vorliegt und die
  Richtlinien es zulassen, ausschließlich die betreffenden LAN-Sockets **vor dem
  Verbinden** daran binden. Keine globale Prozessbindung, die zusätzlich Wetter-
  oder andere Internetabfragen am VPN vorbeiführt. Vorhandene Kopplungsprüfung,
  Verschlüsselung, Quellenzuordnung und Befehlsbestätigung erhalten.
- **Hotspot-Anbieter und WLAN-Teilnehmer getrennt prüfen:** Bei Tim kann das S20
  selbst den Hotspot bereitstellen, während das G5 die Zentrale ist. Das
  Hotspot-Netz ist nicht pauschal mit einem gewöhnlichen WLAN-Client-`Network`
  gleichzusetzen. Fehlendes `TRANSPORT_WIFI` im zugänglichen Netzwerkangebot
  darf deshalb nicht ungeprüft „kein lokales Netz“ bedeuten. Tatsächlich nutzbare
  Wege und Einschränkungen auf den beteiligten Android-Versionen prüfen.
- Netzwerk-/VPN-Wechsel und geänderte Hotspot-Adressen ohne Löschen der Kopplung
  behandeln. Betroffene Verbindungen gezielt erneuern, gespeicherte Messwerte
  und Historie mit ihrem Stand weiter anzeigen. Fehler- oder Übergangszeiten
  sind keine aktuellen Livewerte.
- Aktives VPN und tatsächlich beobachtete Verbindungsfehler nachvollziehbar
  anzeigen. Eine bloß erkannte VPN-Verbindung beweist noch keine Blockade.
  Bei fehlendem geeigneten Netz, verweigerter Bindung oder fortbestehender
  Sperre konkrete Hinweise geben; keine dauerhaft leere Ansicht oder
  fälschliche Erfolgsanzeige.
- Wenn Android beziehungsweise das VPN den lokalen Zugriff sperrt, auf eine
  erlaubte Konfiguration in der VPN-App verweisen: lokale Netzwerkfreigabe
  oder eine passende App-Ausnahme, soweit angeboten. Androids Einstellung
  „Verbindungen ohne VPN blockieren“ kann auch Ausnahmen weiterhin verhindern.
  Die genaue Anleitung hängt von der tatsächlich verwendeten VPN-App ab.
- Keine automatische Abschaltung/Ersetzung des fremden VPNs, keine pauschale
  Ausnahme des gesamten Internetverkehrs und kein eigener VPN-Dienst als
  vermeintlicher universeller Ausweg. Bluetooth-Erfassung bleibt ein anderer
  Datenweg und wird durch eine lokale Routingkorrektur nicht neu eingerichtet.
- Die mögliche Verbindung zum ebenfalls gemeldeten leeren Journalaufruf prüfen.
  Aus dem VPN-Gegencheck nicht ohne erneuten Ende-zu-Ende-Test ableiten, dass
  damit der separate Journalfehler bereits behoben oder bewiesen sei.

### Prüfbedarf und Stand

VPN aus/an; geeignetes physisches Netz mit erlaubtem Bypass; VPN ohne Bypass;
Android-Sperre für Verkehr außerhalb des VPNs; VPN mit LAN-Freigabe oder
App-Ausnahme; kein physisches Netz; konkurrierende private VPN-/LAN-Adressen;
WLAN-Client und Hotspot-Anbieter als Anzeige/Zentrale; Netzwechsel während
Abruf oder Steuerauftrag; Timeout und gespeicherter Offline-Stand; unveränderte
Internetroute, Kopplung und Rohdaten. Konkreten VPN-Anbieter und reale Geräte
für eine spätere praktische Kompatibilitätsprüfung berücksichtigen.

Es wurden Quellpfad und Android-Dokumentation geprüft. Noch kein Produktivcode
für diese Verbesserung geändert, keine entsprechende APK gebaut und keine
VPN-/Hardwaretests am G5/S20/S10 durchgeführt.

Primärquellen:
[Android: VPN, Bypass und blockierte Verbindungen](https://developer.android.com/develop/connectivity/vpn),
[Android: Network.bindSocket / SocketFactory](https://developer.android.com/reference/android/net/Network).

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
