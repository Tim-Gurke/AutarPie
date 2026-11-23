# AutarPie 3.1.2

Die geöffnete Großansicht erhält Vorrang vor den Kacheln des aktiven Dashboards.
Historisches Verbrauchslernen folgt danach. Beim Wechsel werden noch wartende
Anfragen der vorherigen Ansicht verworfen; laufende Hintergrundabgleiche geben
an der nächsten Paketgrenze frei. Ein bereits laufender Netzwerkabruf kann noch
bis zu seiner Antwort oder Zeitüberschreitung dauern.

Berechnete Kurven bleiben auf dem Anzeigehandy gespeichert und erscheinen nach
Neustart zunächst mit ihrem Datenstand in Grau. Fehlende oder geänderte
Grundlagen werden gezielt ergänzt. Bei identischen Grundlagen wird das fertige
Ergebnis wiederverwendet. Die Tagesübersicht speichert ihre Ergebnisse je
Kalendertag; bei einer Änderung werden nur davon betroffene Tage neu berechnet.

Der Verbrauchslernstand wird bei unveränderter Anlagenkonfiguration bis zu sechs
Stunden wiederverwendet. Ältere Korrekturen und Konfigurationsänderungen können
früheres Nachlernen auslösen. Akku- und Wetterprognose reagieren weiterhin auf
aktuelle Grundlagen. Eine Diagrammabfrage lädt keine vollständigen Beobachtungs-
oder Vorhersagetabellen für das Verbrauchslernen.

Im Rollerprofil ist die Anzeigegrenze für „nicht aktuell“ zwischen 1 und 300
Sekunden einstellbar, Vorgabe 15 Sekunden. Kurze Empfangslücken lassen die
zuletzt empfangenen Zahlen normal stehen. Danach bleiben sie mit Zeitangabe
grau sichtbar. Diese Einstellung betrifft die Anzeige; Aufzeichnung,
Ladeplanung und Schaltentscheidungen verwenden ihre bisherigen Frischeprüfungen.

## Aktualisieren

Zuerst G5/Zentrale beziehungsweise das direkt erfassende Handy, danach S20/S10
und weitere Anzeigen auf 3.1.2 aktualisieren. Über die bestehende Installation
installieren. Messdaten, Kopplungen, Farben und eigene Layouts bleiben erhalten.
Die ursprüngliche App-Kennung und Updatesignatur bleiben erhalten.
Der Verbund bleibt mit 3.1.1 kompatibel; neue Speicherung und Priorisierung
benötigen 3.1.2 auf dem jeweiligen Anzeigehandy.

## Bedienung

Eine Kachel öffnen, um ihre Großansicht bevorzugt zu laden. Beim Schließen
werden wieder die Kacheln des aktiven Dashboards versorgt. Die BMS-Anzeigegrenze
unter Roller → Rollerprofil einstellen. Gespeicherte Werte ersetzen keine neuen
Messungen; Datenstand und laufende Aktualisierung bleiben sichtbar.

## Prüfgrenzen

Die Softwareprüfungen prüfen gespeicherte Ergebnisse, gezielte Korrekturen,
Prioritätswechsel, Grau-Kennzeichnung und die getrennte BMS-Anzeigegrenze.
Die tatsächlichen Wartezeiten und Funkverbindungen am G5 wurden hier nicht
vor Ort gemessen.

[Bedienung](AUTARPIE-3.md) · [Vollständige Versionshistorie](../CHANGELOG.md)
