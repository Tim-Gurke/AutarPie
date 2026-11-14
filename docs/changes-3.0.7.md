# AutarPie 3.0.7 · 608PD-Messwerte nach der Anmeldung

Nach erfolgreicher Anmeldung konnte der 608PD weiterhin „Keine aktuellen Laderdaten“ und anschließend „608PD antwortet nicht mehr“ zeigen. Die Verbindung vereinbarte bisher keine größere Bluetooth-Paketgröße. Die kleinste vollständige Statusantwort benötigt bereits 23 Nutzdatenbytes; eine Standard-ATT-MTU von 23 erlaubt in einer Benachrichtigung nur 20 Nutzdatenbytes. Eine erfolgreiche Anmeldung oder Geräteabfrage beweist deshalb noch keinen funktionierenden Empfang der längeren Messwerte.

## Korrektur

- Vor Diensterkennung, Benachrichtigungen und privater Anmeldung eine ATT-MTU von 247 anfordern und die asynchrone Bestätigung abwarten. Mindestens 50 Byte bestätigen lassen, damit auch ein vollständiger Status mit sechs Zellen hineinpasst.
- Eine abgelehnte, zu kleine oder ausbleibende Paketgrößenbestätigung verständlich melden. Bei jeder Wiederverbindung erneut verhandeln; verspätete Rückmeldungen einer alten Verbindung verwerfen.
- Einen Fehler beim Auslesen der Bluetooth-Dienste mit Android-Status getrennt von einem tatsächlich fehlenden AF00-Dienst anzeigen. Die bisherige Sammelmeldung unterschied diese Ursachen nicht.
- Der vorhandene private Import und seine verschlüsselte Speicherung bleiben erhalten. Es ist kein neuer Mitschnitt nötig, wenn die richtige Verbindung bereits übernommen wurde. Das Update startet keine Ladung selbstständig.

## Prüfen am Gerät

1. Zuerst das erfassende Gerät beziehungsweise die **G5-Zentrale auf 3.0.7** aktualisieren, danach die Anzeigen. Im eigenständigen Betrieb das direkt mit dem Lader verbundene Handy zuerst aktualisieren. Paket und ursprüngliche Updatesignatur bleiben gleich; über die vorhandene Installation installieren.
2. Den 608PD einschalten und in Bluetooth-Reichweite des erfassenden Geräts lassen. Eine belegende Verbindung der Hersteller-App trennen.
3. Unter **Modellbau → Einrichten** vorhandene Adresse und **„Private Verbindung: vorhanden“** kontrollieren, Bluetooth-Verbindung aktivieren und speichern. Bei bereits aktivierter Einrichtung muss nichts neu importiert werden.
4. Auch ohne Ladung sollte bei frischen Antworten **„Bereit“** und ein aktueller Ladestrom von **0 A** erscheinen. Ohne angeschlossenen Akku ist keine gültige Akkuspannung zugesichert. Fehlende Bluetooth-Antworten bleiben als fehlende Daten gekennzeichnet. Zum Verbindungstest muss keine Ladung gestartet werden.

Eine Quelle aus Powerbank → 608PD → Wohnwagenakku bleibt externe Batterieladung; keine zusätzliche Solar- oder Shuntenergie daraus addieren. Die bestehende bestätigungspflichtige 3S-LiPo-Startfunktion und ihre Grenzen ändern sich nicht.

## Softwareprüfung und Grenzen

Der unveränderte Adapter aus 3.0.6 reproduziert an einem neutralen asynchronen BLE-Testpartner mit Standard-MTU 23 den Leerlauf-Timeout: Anmeldung und Geräteinfo funktionieren, die längere Statusantwort kann nicht übertragen werden. Der korrigierte produktive Adapter besteht 57 Prüfungen für ältere und neuere Android-Rückrufformen, Leerlauf mit 0 A, drei/sechs Zellen, Mindest-MTU, verweigerte/fehlgeschlagene/ausbleibende Verhandlung, Dienstfehler, Wiederverbindung und verspätete Rückrufe. Drei Browser-Szenarien prüfen Bereit/0 A in heller/dunkler Ansicht und unbekannte Werte mit gesperrten Aktionen nach Datenverlust.

Diese Prüfungen ersetzen weder den realen Bluetooth-Stack des G5 noch einen Hardwaretest. Ob dies alle Ursachen der persönlichen Verbindungsausfälle behebt, muss nach dem Update vor Ort bestätigt werden. Private Fehlerberichte, Adressen und Verbindungskennungen gehören nicht in öffentliche Prüfartefakte.

Mehrere gespeicherte 608PD und eine direkte Ersteinrichtung ohne Mitschnitt sind in diesem gezielten Verbindungsfix noch nicht enthalten. [Bedienung](AUTARPIE-3.md) · [Vollständige Historie](../CHANGELOG.md).
