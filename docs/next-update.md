# Nächstes gemeinsames AutarPie-Update

Abgleich am 25.11.2026 mit dem veröffentlichten Stand **3.1.6**
(`fff10877c42f90e667105b87b31ae58504221b27`). Dies ist die Planung, keine
Behauptung einer bereits implementierten Korrektur oder neuen Veröffentlichung.

Ergänzungen am **27.11.2026**: mehrere Wettermodelle für Wetter und Solarertrag
mit nachvollziehbarem Mittelwert und Modellspanne sowie automatische 608PD-Erfassung
als Sonderverbrauch und als externe Ladequelle in den Wohnwagenakku. Beide Punkte
bleiben zur Sammlung.

Ergänzungen am **28.11.2026**: grafischer Langfristtrend mit Bewölkung,
Modellmix und echten Kalenderwochen von Montag bis Sonntag.

## Offene Entwicklungspunkte

| Punkt | Gewünschtes Ergebnis | Stand |
|---|---|---|
| PV-Eingangsspannung | Echte Spannung des ausgewählten Reglers abrufen, speichern und bis zur Kachel übertragen. Quelle und Datenstand korrekt zeigen; keinen Batterie- oder errechneten Ersatzwert als PV-Spannung ausgeben. | Noch offen. Der Quellstand besitzt bereits einen aktiven Registerabruf und Speicherung. Auslösebedingungen, Aktualität und Kachelzuordnung müssen als gesamte Datenkette geprüft werden. Die Beschriftung „Berechnet“ ist ebenfalls zu prüfen. |
| Sonneneinfluss beim Gaslernen | Historische Sonneneinstrahlung und vorhandenen PV-Ertrag als ergänzende Grundlage betrachten, einen belegten Einfluss auf den Heizbedarf lernen und in einer Gas-Lernnotiz erklären. Fertige Faktoren speichern und für neue Vorhersagen anwenden. | Noch nicht implementiert. Wetterdaten, Modell und Lernnotiz müssen gemeinsam erweitert werden. Gemessene PV-Wh sind keine feste direkte Heizwärme-/Gasgutschrift. Ein Sonnenfaktor darf erst mit ausreichender Daten- und Prüfgrundlage gelten. |
| Wetterrückblick: 14 Tage mit gewichtetem Prognosevergleich | Je vergangenem Tag den gewichteten Mittelwert der ursprünglich für genau diesen Zieltag ausgegebenen Prognosen aus den 14 Tagen davor mit dem tatsächlichen Solarertrag vergleichen. Vorlauf 14–8 Tage schwach, 7–4 stärker, letzte drei Tage am stärksten gewichten; tatsächlichen Akkustand erhalten. | Neu vorgemerkt, noch nicht implementiert. Originalprognosen mit Datum speichern, verfügbare Anzahl und Ertragsabweichung zeigen. Die 14 vergangenen Tage der Ansicht und die 14 Vorlauftage je Zieltag sind getrennte Zeiträume. |
| Mehrere Wettermodelle für Wetter und Ertrag | Vorhersagen verschiedener Wetterzentren für denselben Ort und Zeitraum getrennt abrufen; Wettergrößen und daraus berechnete Solarerträge zu einem nachvollziehbaren Mittelwert mit Modellspanne zusammenführen. | Neu am 27.11.2026 gesammelt, noch nicht implementiert. Vorschlag: DWD ICON, ECMWF IFS und NOAA GFS über die bestehende Open-Meteo-Schnittstelle. Modellhorizonte, Datenstand, Ausfälle und vorhandene Lernfaktoren berücksichtigen. |
| 608PD automatisch als Sondernutzung und externe Ladequelle | Vorhandene Bluetooth-/Journalwerte je Lader für beide Energierichtungen automatisch übernehmen: Wohnwagen → Modellbauakku als Sonderverbrauch; externe Energie → 608PD → Wohnwagenakku als externe Ladung. Schnellwahl „Modellbauakku laden“ / „Wohnwagenakku laden“ direkt beim Lader vorsehen, da derselbe Lader beide Aufgaben übernimmt. Nutzung, Energie, passende Lernfaktoren und Tagesbilanz ohne erneute manuelle Erfassung verbinden. | Am 27.11.2026 gesammelt und um Rückladung präzisiert, noch nicht implementiert. Geräteprofile, Journal und Energiepfad „Powerbank → 608PD → Wohnwagenakku“ existieren. Automatische Bilanzverknüpfung fehlt; Quellenanteil, Nettoakkuwert und Verbrauch nicht doppelt zählen. |
| Langfristtrend: Kalenderwochen, Diagramme, Bewölkung und Modellmix | Vorschau nach ISO-Kalenderwochen Montag–Sonntag mit KW-Nummer und Datum; alle Werte grafisch mit beschrifteten, vergleichbaren Skalen darstellen. Bewölkung mit passenden Symbolen und Messgröße ergänzen; mehrere geeignete Modelle als Mittel und erkennbare Spanne berücksichtigen. | Neu am 28.11.2026 gesammelt, noch nicht implementiert. Betrifft die gezeigte EC46-Langfristansicht. Teilwochen, fehlende Daten, tatsächliche Modellhorizonte und erhöhte Unsicherheit kenntlich halten. |

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
