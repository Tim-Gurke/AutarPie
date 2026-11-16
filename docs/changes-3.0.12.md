# AutarPie 3.0.12 · Archiv-Bluetooth unabhängig vom Hauptthread

Tim hat diesen konkreten Reparaturumfang am 16.11.2026 zum Bauen und Veröffentlichen freigegeben. Die Veröffentlichung setzt eine erfolgreiche Gesamtprüfung und die vorhandene Updatesignatur voraus.

## Gezielte Korrektur

Der Archivdienst gibt Android-Bluetooth-Aufträge und deren Rückmeldungen an einen eigenen HandlerThread. Diensterkennung und Schreibbestätigungen warten damit nicht mehr auf den Hauptthread, wenn dieser mit Oberfläche oder anderen Aufgaben beschäftigt ist. Der vorhandene serielle Protokollablauf erhält weiterhin die Sitzung und verarbeitet Empfangsfreigaben; es werden keine zusätzlichen gleichzeitigen GATT-Aufträge eingeführt. Abbrechen und Vorrang bestätigter manueller Schaltaufträge schließen die Verbindung weiterhin unmittelbar.

Die lokale Bestandsplanung verlängert durchgehende Messintervalle mit weniger Objekt- und Baumoperationen. Gespeicherte Daten, echte Lücken und als nicht verfügbar bestätigte Werte werden unverändert bewertet. Es werden keine Messwerte gelöscht oder Vollständigkeitsprüfungen übersprungen. Datenbankformat und Verbundprotokoll ändern sich nicht.

## Prüfumfang und Aussagegrenzen

Vor dem APK-Lauf wurden sämtliche Produktions-Javaquellen kompiliert. 114 gezielte Transportprüfungen in 15 Durchläufen bestanden, darunter der direkte Vergleich mit 3.0.11 bei blockiertem Hauptthread und die vorbereitete Korrektur für die Android-30-/35-Adapter. Diensterkennung, Benachrichtigungen, Empfangsfreigaben, Sitzungserhalt und Abbrechen werden während derselben Blockade geprüft. Zusätzlich bestanden 45 Vergleichsprüfungen mit 240.000 gleichen Punktentscheidungen und neun bestehende SQLite-/Fortsetzungsanforderungen.

Der vollständige Freigabelauf prüft zusätzlich APK-Bau, Lint, bestehende Unit- und Integrationsprüfungen, Browseransichten und die native Oberfläche auf Android 8/11. Die transportbezogenen Belastungstests verwenden Android-/GATT-Adapter und eine modellierte Gegenstelle. Sie belegen die beseitigte Hauptthread-Abhängigkeit, aber nicht die Ursache jedes realen Bluetooth-Abbruchs. Reale Funkstabilität, G5-Laufzeit und der gesamte erste Datenabgleich auf Anzeigehandys wurden hier nicht gemessen. [Technischer Nachweis und Grenzen](archive-android-dispatch.md).

Neue Grundeinstellungen, weitere Beschleunigung der Anzeigeübertragung und optionale automatische Sicherungen sind nicht Bestandteil dieses Reparaturupdates und bleiben vorgemerkt.

## Aktualisieren und verwenden

1. Zuerst die Zentrale beziehungsweise das tatsächlich per Bluetooth erfassende Handy auf **3.0.12** aktualisieren. Über die vorhandene App installieren; nicht deinstallieren. Paketkennung und ursprüngliche Updatesignatur bleiben erhalten.
2. Die gespeicherte Aufzeichnung fortsetzen lassen. Messdaten, private Verbindungen, Kopplungen und eigene Layouts bleiben erhalten. Ein erneuter Bluetooth-Mitschnitt ist für dieses Update nicht erforderlich.
3. Unter **Geräte → Fehlende Archivdaten abrufen** einmal den vollständigen Abschluss prüfen. Die laufenden Livewerte und das Ergebnis des Archivabrufs sind unterschiedliche Statusangaben. Bei Abbruch bleiben bereits gespeicherte Daten erhalten.
4. Anschließend die Anzeigehandys aktualisieren, wenn ein gemeinsamer Versionsstand gewünscht ist. Anzeigen mit **3.0.11** bleiben zur Zentrale mit **3.0.12** kompatibel; die Reparatur des Archivtransports wirkt am erfassenden Handy.

[Vorherige Änderungen 3.0.11](changes-3.0.11.md) · [Bedienung](AUTARPIE-3.md) · [Versionshistorie](../CHANGELOG.md)
