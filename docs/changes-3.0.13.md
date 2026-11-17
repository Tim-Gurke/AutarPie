# AutarPie 3.0.13

Bau-/Veröffentlichungsfreigabe für den vollständigen Umfang am 17.11.2026 erteilt. Die Veröffentlichung setzt bestandene Android-/APK-Prüfungen voraus; die konkreten Nachweise stehen im Release und im README.

## Archiv und Anzeigen

Die [vorbereitete Archivkorrektur](archive-bounded-reads.md) bleibt enthalten: höchstens 2.048 fortlaufende Rohzeilen je Bestandsabfrage, unveränderte Lückenentscheidungen, frische GATT-Anmeldung nach einem vorübergehenden Anmeldefehler und zutreffende Auftragsdiagnose. Kein globales Bluetooth-Aus-/Einschalten und keine neue MTU. Eine Behebung jedes physikalischen Verbindungsfehlers wird daraus nicht abgeleitet.

Anzeigen bündeln bis zu acht versionierte Seiten mit zusammen höchstens 4.096 Rohzeilen je Antwort. Fertige Blöcke werden einzeln atomar gespeichert. Änderungen und Löschungen betreffen nur die jeweiligen Blöcke; unveränderte Blöcke bleiben vor Ort. Alte Gegenstellen verwenden weiter das bisherige Protokoll. Liveanzeige und Bedienung behalten ihre getrennten Ausführungspfade. Die Diagnose trennt Verbindung, Netzwerk, Speicherung und Berechnung. Alle Berechnungen bleiben auf der Anzeige beziehungsweise im eigenständigen Betrieb.

Ein erneut ausgeführter neutraler SQLite-/TCP-Vergleich liefert für 81.442 Rohzeilen dieselben Energiekurven/-summen mit 102 statt 690 Netzwerkaufrufen. Das ist keine konkrete Geschwindigkeitszusage für ein Handy. Bestehende Lernstände, Korrekturen, Löschungen, Datenbankwechsel und warme Replikate werden mitgeprüft.

## Neue Startdarstellung

Nur eine leere Neuinstallation erhält die gewünschte Vorlage mit 75 % Größe, 21 Übersichtskacheln, acht Bereichen, eigenen Verläufen/Wetterfarben und Kurzwahlreihenfolge. Die Vorlage enthält keine privaten Geräte-, Anlagen- oder Standortdaten. Bereits vorhandene Einstellungen und Layouts bleiben unverändert.

## Freiwillige vollständige Sicherung

Auf der Anzeige unter Einstellungen → Daten & Sicherung → „Datensicherung · automatisch / jetzt“. In der nativen Zentrale unter „Einstellungen und Daten“.

1. Eigenen Sicherungsordner wählen: lokal oder einen Online-Anbieter, den Android als beschreibbaren Ordner mit dauerhafter Freigabe anbietet. Ein nur im Dateimanager verknüpftes Konto genügt unter Umständen nicht. Anbieter ohne Ordnerfreigabe erlauben gegebenenfalls eine manuelle Sicherungsdatei über den bestehenden Exportdialog; alternativ lokalen Sicherungsordner selbst synchronisieren.
2. Kennwort mit mindestens zehn Zeichen eingeben und bestätigen. Für die Wiederherstellung auf einem anderen Handy getrennt aufbewahren. Die Automatik speichert es auf diesem Handy mit AndroidKeyStore-Schutz.
3. Ortszeit und Anzahl Stände festlegen; bei Bedarf „Täglich automatisch sichern“ einschalten und speichern. Standard: ausgeschaltet, 03:00 Uhr, sieben Stände. Zwei bis 30 Stände sind möglich.
4. „Jetzt sichern“ fordert einen zusätzlichen Lauf an. Status zeigt letzten geprüften Erfolg, nächsten Termin und Fehler. Android-Energiesparen kann die gewählte Startzeit verschieben. Fällige Termine werden nach Neustart nachgeholt; fehlgeschlagene automatische Aufträge später wiederholt.

Die Sicherung umfasst alle App-Datenbanken und Einstellungen: Messwerte, Archivbestätigungen, Gas-/Wasserjournal, Wetter, Lernstände, Roller-/608PD-Journale, Anzeige-Replikate und Darstellung. Jede Datenbank wird separat konsistent kopiert; ein gemeinsamer atomarer Zeitpunkt über sämtliche Datenbanken wird nicht behauptet. Zeilenkennungen, NULL-Werte, Binärdaten, Schema, Indizes, Trigger und AUTOINCREMENT-Stände bleiben erhalten. Beim Kopieren reserviert die jeweilige Datenbank den Schreibzugriff; Aufzeichnung und LAN-Dienste werden nicht abgeschaltet. Ein laufender Archivabruf verschiebt den Sicherungsversuch. Verschlüsselung und Zielübertragung halten keine Transaktion auf den Quelldatenbanken offen.

Eine Datei gilt erst nach vollständigem Zurücklesen und SHA-256-Vergleich beim gewählten Speicheranbieter als gespeichert. Erst danach werden ältere eigene Stände entfernt; der neueste Erfolg bleibt geschützt. Fehler behalten ältere Sicherungen. Der Anbieter steuert seinen nachfolgenden Cloud-Upload; die App bestätigt dessen Abschluss nicht ohne Anbieternachweis. Für die gemeinsame Historie die Zentrale sichern, für eigene lokale Daten bei Bedarf die Anzeigen zusätzlich.

## Wiederherstellen

„Sicherung wiederherstellen“ öffnen, Aufzeichnung/Archivabruf pausieren, Datei wählen und Kennwort eingeben. Zunächst werden Authentifizierung, Inhalt und Datenbanken geprüft. Erst „Übernehmen und schließen“ merkt die Übernahme vor; beim anschließenden vollständigen Neustart werden die Daten ersetzt. Der bisherige Stand bleibt zusätzlich intern zurückgehalten.

Geräteschlüssel verlassen die Quellinstallation ausschließlich innerhalb der kennwortverschlüsselten Sicherung und werden im Ziel-AndroidKeyStore neu verschlüsselt. Android-Systemberechtigungen und System-Bluetooth-Kopplungen können weiterhin Bestätigungen erfordern. Automatikkennwort und Zielordnerfreigabe werden nicht auf das andere Handy übertragen: automatische Sicherung nach Wiederherstellung neu einrichten. Alte Sicherungen im Format 1 bleiben lesbar, können aber nicht übertragbare Geräteschlüssel enthalten; darauf weist der Import hin. Neue vollständige Sicherungen im Format 2 benötigen Version 3.0.13 oder neuer.

## Prüfungen und Grenzen

Alle produktiven Javaquellen gegen Android API 35 kompiliert. Erneute SQLite-/TCP-Prüfungen einschließlich Neuinstallations-/Bestandswahrung, paralleler Schreibvorgänge, kompletter Transaktionen, Zeilenkennungen, NULL/Binärdaten, Triggern, AUTOINCREMENT, Verschlüsselungs-Rundreise, falschem Kennwort, beschädigter Abschlussauthentifizierung und Zeitumstellungen bestanden.

Die Android-Prüfung umfasst: echte SAF-Freigabe, Schreiben/Zurücklesen, Versionserhalt, Offline-/Lese-/Umbenennungsfehler sowie vollständige Wiederherstellung über zwei wirklich getrennte Nutzer/App-UIDs mit echten AndroidKeyStore-Schlüsseln. Die tatsächlichen Ergebnisse werden in den Freigabenachweisen verlinkt. Keine physische Victron-/608PD-Abnahme, kein unbeaufsichtigter nächtlicher G5-Neustart und keine allgemeine Zusage für jeden Cloud-Anbieter aus Softwaretests ableiten.

Zuerst die Zentrale, anschließend die Anzeigen aktualisieren. Die bestehende Updatekennung/Signatur sowie Messwerte und Kopplungen bleiben erhalten. Abgeschlossene Quellschritte werden vor langen Prüfungen dauerhaft auf GitHub gesichert; lokale Arbeitsverzeichnisse allein gelten nicht als Sicherung.
