# AutarPie 3.1.3

## Gespeicherten Verbrauchslernstand auch bei neuen Livewerten wiederverwenden

In 3.1.2 enthielt die Erkennung einer geänderten Anlagenkonfiguration auch den
letzten Empfangszeitpunkt, die Signalstärke und den Verbindungsstatus der Geräte.
Dadurch konnte eine normale neue Live-Messung unnötiges Nachlernen auslösen.

3.1.3 bewertet dafür die tatsächlich ausgewählten Solar-/Shunt-Geräte und die
fachlich relevanten Einstellungen. Neue Empfangszeitpunkte, RSSI, Gerätenamen,
Verbindungsfehler oder ein neu aufgelöster Name desselben GPS-Orts starten das
Verbrauchslernen nicht erneut. Geänderte Geräte, Standortkoordinaten, Aufstellung
und Anwesenheitsgrundlagen bleiben relevant. Ein weiterhin passender Lernstand
wird bis zu sechs Stunden wiederverwendet. Aktuelle Shuntwerte und Wetterdaten
aktualisieren die Akku-Prognose unabhängig davon.

Die Funktionen aus 3.1.2 bleiben enthalten: Vorrang der geöffneten Großansicht,
gespeicherte Kurven nach Neustarts, Wiederverwendung fertiger Berechnungen,
gezieltes Nachführen betroffener Tageswerte und die BMS-Anzeigegrenze von
1 bis 300 Sekunden mit Vorgabe 15 Sekunden unter Roller → Rollerprofil.

## Aktualisieren

Zuerst G5/Zentrale beziehungsweise das direkt erfassende Handy, danach S20/S10
und weitere Anzeigen auf 3.1.3 aktualisieren. Über die bestehende Installation
installieren. Messdaten, Kopplungen, Profile, Farben und Layouts bleiben erhalten.
Paketkennung und ursprüngliche Updatesignatur bleiben erhalten. Der Verbund
bleibt mit 3.1.2 kompatibel; die korrigierte Wiederverwendung benötigt 3.1.3 auf
der jeweiligen Anzeige. Einmaliges Aktualisieren des Lernstands nach dem Update
ist möglich, weil sich seine Gültigkeitserkennung geändert hat.

## Prüfung und Grenzen

Die Regression wird mit neuen Gerätezeitstempeln und RSSI über die produktive
verschlüsselte TCP-Verbindung geprüft: Die neuen Livewerte erreichen die Anzeige,
während der vorhandene Verbrauchslernstand samt Berechnungszeit erhalten bleibt.
Zusätzlich werden tatsächliche Geräte-, Standort- und Planungsänderungen geprüft.
Die Wartezeiten und Funkverbindungen am tatsächlichen G5 wurden nicht vor Ort
gemessen.

[Bedienung](AUTARPIE-3.md) · [Umfang 3.1.2](changes-3.1.2.md) · [Versionshistorie](../CHANGELOG.md)
