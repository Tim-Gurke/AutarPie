# AutarPie 3.0.8

![AutarPie Ressourcen-Kreislauf](app/src/main/assets/brand.svg)

**Das lokale Cockpit für autarkes Leben.** Solar und Stromverteilung, Wohnwagenakku, E-Scooter, Modellbau, Gas und Wasser in einer gemeinsamen App. AutarPie führt **Watt’n’Pit und WohnPit** weiter.

**[AutarPie 3.0.8 als APK herunterladen](https://github.com/Tim-Gurke/AutarPie/releases/download/v3.0.8/AutarPie-v3.0.8.apk)** · [Einrichtung und Bedienung](docs/AUTARPIE-3.md) · [Update und Prüfgrenzen](docs/changes-3.0.8.md) · [Vollständige Versionshistorie](CHANGELOG.md)

Android 8 oder neuer; das Cockpit benötigt einen aktuellen Android-System-WebView. Kein Konto, keine Werbung. Mess- und Lerndaten bleiben auf den eigenen Geräten. Der optionale Wetterabruf verwendet Open-Meteo. Der Anlagenverbund überträgt Daten verschlüsselt im lokalen WLAN.

## Ansichten

| Drei 608PD · dunkle Ansicht | Drei 608PD · helle Ansicht |
| --- | --- |
| ![AutarPie 3.0.8: drei getrennte Laderprofile mit eigenem Status und Journal, dunkle Ansicht](docs/images/autarpie-3.0.8-dark.svg) | ![AutarPie 3.0.8: drei getrennte Laderprofile mit eigenem Status und Journal, helle Ansicht](docs/images/autarpie-3.0.8-light.svg) |

Die Aufnahmen zeigen die aktuelle Oberfläche mit neutralen Beispieldaten, keinen Hardwaretest. Eigene Farben und Layouts bleiben erhalten. [Native Zentrale in 3.0.8](docs/images/autarpie-3.0.8-central.svg).

## Ein Cockpit, mehrere Anwendungen

- **Übersicht:** frei zusammenstellen, was gerade gebraucht wird – auch Werte aus E-Scooter, Modellbau, Gas und Wasser.
- **Solar & Speicher:** Anlage, Batteriebilanz, PV-Spannung, Lastausgang, Wetter und Prognose.
- **E-Scooter:** BMS-Werte, Speicherinhalt, Ladeweg, Ladeplanung und Sondernutzung.
- **Gas / Wasser:** Beschaffungen, Flaschenwechsel, korrigierter Vorrat und gelernter Bedarf.
- **Modellbau:** 608PD-Livewerte, begrenzte bestätigte Bedienfunktionen und Einordnung externer Batterieladung.
- **Verlauf, Geräte, Einstellungen:** Messungen, Phasenzeitleiste, Verbindungen und gemeinsame Konfiguration.

Alle Bereiche sind oben über **Menü** erreichbar. Untere Kurzwahlen lassen sich ein-/ausblenden und ordnen; Ziehen und Pfeile zeigen die neue Reihenfolge sofort im Dialog. Android-Zurück führt durch die geöffneten Einstellungs- und Menüebenen. Eigene benannte Dashboards können leer beginnen oder eine vorhandene Ansicht kopieren. Raster, Kachelgrößen und Hoch-/Queranordnungen bleiben gerätelokal. Kachelentfernen fragt nach; die Darstellungsgröße verlangt ausdrückliches Speichern.

Gemeinsame Bereichsfarben und weiche zwei-/dreifarbige Skalen sind zentral einstellbar. App- und Kachelhintergründe unterstützen eine oder zwei Farben mit Verlaufwinkel 0–360°. Einzelne Kacheln dürfen davon abweichen. Optional erhält jedes Dashboard ein eigenes Farbdesign. Die Farbauswahl startet beim aktuellen Farbton und bietet App-Farben sowie automatisch gemerkte eigene Mischfarben.

## Zentrale, Anzeige oder eigenständig

| Rolle | Aufgabe |
| --- | --- |
| Zentrale | Mess- und Wettergrundlagen erfassen, speichern und verteilen; native Bedienoberfläche und bestätigte Steuerung |
| Anzeigehandy | Cockpit, Diagramme, Prognosen, Verbrauchslernen und eigene Dashboards aus den Zentraldaten berechnen |
| Eigenständig | Erfassung, Berechnung, Planung und Cockpit auf einem Gerät ohne Zentrale |

Autostart stellt nach Neustart oder App-Update den gespeicherten Zustand wieder her. Eine pausierte Aufzeichnung bleibt pausiert. Das konkrete Android-Gerät kann erstes Entsperren und zusätzliche Hintergrund-/Bluetooth-Freigaben verlangen. Gemeinsame Zentralfunktionen sind auch von den Anzeigehandys bedienbar, einschließlich Archivabruf und Aufzeichnung.

## Solar, Lastausgang und 608PD

Standort, Aufstellung, Verschattung, Module, Verkabelung und Regler werden gemeinsam eingerichtet. Solarkreise behalten beim Umordnen ihre Kennung und historischen Daten. Mehrere Kreise an demselben Regler teilen sich in der Prognose die eingetragene Strom-/Leistungsgrenze. Jeder mobile Kreis erhält einen eigenen anklickbaren Namen in der Wetterkarte.

Der SmartSolar-Lastausgang ist E-Scooter, Modellbau oder einem anderen Verbraucher zuordenbar. Zustand, Modus, Spannung, Strom und Abschaltgrund werden soweit unterstützt vom Gerät gelesen; Leistung und Energie daraus auf der Anzeige ausgewertet. Regler je Kachel wählen; aktuelle und historische Werte bleiben getrennt. Werte werden nicht nochmals zum bereits am Shunt erfassten Verbrauch addiert.

Die **optionale Rollerautomatik ist zunächst aus**. Ladeziel, Wohnwagenreserve, Tagesziel, Überschussschwellen und Prognosereserve sind variabel. Ein aktives Anzeigehandy erstellt den zeitlich begrenzten Plan; die Zentrale kontrolliert direkte Abschaltbedingungen. Ein Schaltauftrag ist erst mit der tatsächlichen Geräteantwort bestätigt. Ohne gültigen Plan wird zum Ausschalten angefordert. BMS-Empfang allein beweist noch keinen eingesteckten Ladestecker.

Bis zu drei 608PD erhalten getrennte Profile, private Kennungen und eigene Verbindungen. Suche und Einrichtung laufen am erfassenden Handy; Anzeigen bedienen den Ablauf. Die neue direkte Ersteinrichtung verlangt eine Bestätigung am Lader und muss am eigenen 608PD praktisch geprüft werden. Vorhandene Verbindungen aus der eigenen Bluetooth-Aufzeichnung bleiben erhalten. Unterstützt sind die nachgewiesenen Livefelder und das Beenden von Laden/Powerbank-Betrieb. App-Start ist zunächst auf das beobachtete **3S-LiPo-Profil 4,21 V/Zelle / 0,50 A** mit ausdrücklicher Profilbestätigung begrenzt. Powerbank und andere Profile am Gerät starten. Nicht belegte Temperatur-/USB-/CC-CV-Felder werden nicht erfunden.

## Update 3.0.8

**Bekannte Grenze:** Der Archivabruf kann bei einer langsamen lokalen Bestandsprüfung weiterhin mit Bluetooth-Status 19 enden. Eine Korrektur mit eigenständiger Bluetooth-Verarbeitung wird als [3.0.9 vorbereitet](docs/changes-3.0.9.md); diese ist noch nicht gebaut oder veröffentlicht. 3.0.8 behebt den Empfangsüberlauf, bestätigt aber noch keinen erfolgreichen vollständigen Archivabruf auf jeder Anlage.

Der Archivempfang gibt Bluetooth-Pufferplatz nach der Verarbeitung frei und unterscheidet einen Transportfehler vom manuellen Abbruch. Gescheiterte Aufzeichnungsstarts bleiben mit Android-Freigaben und Fehlergrund in der Diagnose nachvollziehbar. Die allgemeine **Victron-Liveanzeige sitzt im App-Kopf**; jeder 608PD zeigt seinen eigenen Status.

**Alte Rollerwerte bleiben sichtbar, erscheinen grau und tragen eine Alt-Markierung mit Uhrzeit.** Das Anzeigehandy prüft den ursprünglichen BMS-Zeitpunkt selbst. Erst neue Messwerte heben die Markierung auf. **Modellbau → Journal öffnen** zeigt beobachtete Lade-/Powerbankvorgänge aller gespeicherten Lader, Messlücken und ausschließlich beobachtete Akkuseitenenergie; JSON-Export ist enthalten.

**Gas lernt auch auf Reisen:** Gespeicherte Aufenthaltszeiten werden mit dem damaligen örtlichen Wetter verbunden. Ortswechsel und Zwischenstopps sind kein pauschaler Ausschluss. Unter **Gas und Wasser → Einstellen** lässt sich der Wetterort am erfassenden Handy bei laufender Aufzeichnung per GPS nachführen. Dieses Handy muss beim Wohnwagen bleiben und Standortzugriff **Immer zulassen** haben. Fehlende Ortszeiten und fehlendes Wetter werden getrennt erklärt; die Flaschenwechsel bleiben erhalten.

**Zuerst die Zentrale beziehungsweise das direkt erfassende Handy, danach alle Anzeigen auf 3.0.8 aktualisieren.** Über die vorhandene Installation installieren; Messdaten, bestehende private Verbindungen und Layouts bleiben erhalten. Für Laderjournal, drei Profile und das neue Gaslernen den gemeinsamen Stand 3.0.8 verwenden. Echte Bluetooth-Stabilität, direkte Erstkopplung und nächtlichen G5-Neustart anschließend am eigenen Gerät prüfen. [Änderungen, Bedienung und Prüfgrenzen 3.0.8](docs/changes-3.0.8.md).

## Update 3.0.7

Der 608PD vereinbart vor der Anmeldung größere Bluetooth-Pakete, damit vollständige Messwerte auch auf älteren erfassenden Handys übertragen werden können. Eine erfolgreiche Anmeldung allein reichte dafür bisher nicht aus. Fehler beim Aushandeln der Paketgröße, bei der Dienstabfrage und ein tatsächlich fehlender Dienst werden getrennt angezeigt. Auch ein nicht ladender, erreichbarer 608PD kann „Bereit“ und 0 A melden.

**Zuerst die Zentrale beziehungsweise das direkt erfassende Handy, danach alle Anzeigen auf 3.0.7 aktualisieren.** Vorhandene private Verbindung, Messdaten und Layouts bleiben erhalten; ein neuer Mitschnitt ist bei bereits übernommener richtiger Verbindung nicht nötig. Die tatsächliche Verbindung nach dem Update am eigenen Lader bestätigen. [Änderungen und Prüfgrenzen 3.0.7](docs/changes-3.0.7.md).

Die Verbundschnittstelle bleibt unverändert: Anzeigen mit 3.0.6 bleiben zur Zentrale mit 3.0.7 kompatibel. Für den ersten Verbindungstest genügt deshalb zunächst das Update des tatsächlich per Bluetooth erfassenden Geräts; anschließend wird derselbe Versionsstand auf allen Geräten empfohlen.

## Update 3.0.6

Der Roller kann aus kurzen ausreichend vollständig erfassten Ladungen lernen, obwohl er den übrigen Tag außer Reichweite ist. Energie je Ladung und Ladehäufigkeit bleiben getrennt. Nur vollständig überwachte Ladeweg-Tage oder geeignete bekannte Tage belegen die Häufigkeit; eine Funklücke ist keine Nullnutzung. Eine ausdrücklich gekennzeichnete Wochen- oder eigene Tagesannahme kann überbrücken, bis genügend Grundlagen vorliegen. Bei Rückkehr ersetzt eine konkret geplante Ladung aus frischen BMS-Werten die pauschale Annahme für denselben Zeitraum.

**E-Scooter → Verbrauch lernen** zeigt die Grundlage und führt zur vorläufigen Wochenannahme. Alle ausgewählten Kurzwahlen einschließlich eigener Dashboards verwenden die gemeinsame App-Akzentfarbe; sie bleibt beim Umordnen, bei Farbänderungen und abweichendem Dashboard-Design erhalten. Der historische Haushaltsabgleich aus 3.0.5 bleibt enthalten.

**Die Funktionen aus 3.0.6 bleiben im aktuellen Update enthalten.** Über die vorhandene Installation installieren und anschließend den ersten Abgleich abwarten. Daten, Ladejournal, Kopplungen und Layouts bleiben erhalten. [Änderungen, Bedienung und Prüfgrenzen 3.0.6](docs/changes-3.0.6.md) · [Historisches Verbrauchslernen 3.0.5](docs/changes-3.0.5.md) · [Vollständige Historie](CHANGELOG.md).
