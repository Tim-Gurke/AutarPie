# AutarPie 3.0.10 · Gerätewechsel und vollständiges Archivergebnis

**Vorbereitet, noch nicht gebaut oder veröffentlicht.** Aktuell veröffentlicht bleibt 3.0.9. Der APK-Bau einschließlich eines APK erzeugenden CI-Laufs benötigt die Freigabe für diesen konkreten Umfang.

## Eingegrenzter Fehler

Ein einzelner Geräteabruf kann vollständig erfolgreich sein und der Verbindungsaufbau zum nächsten Gerät trotzdem scheitern. In 3.0.9 folgt der nächste Aufbau unmittelbar auf das Schließen des bisherigen Clients. Ein anfänglicher Status 133 beendet diesen Geräteabruf nach einem einzigen Versuch. Das gemeinsame Abschlussergebnis übernimmt anschließend nur das letzte Gerät. Dadurch kann „0 Archivwerte übernommen“ einen zuvor erfolgreichen Abruf mit übernommenen Werten verdecken.

Der verfügbare Geräteexport zeigt den Fehler im Verbindungsaufbau vor Diensterkennung und vor empfangenen Archivpaketen. Er verrät nicht, welche interne Android-/Controllerentscheidung den Status 133 ausgelöst hat. Eine fehlende Freigabephase ist deshalb eine zu entschärfende Ablaufabhängigkeit, kein bewiesener nativer Fehlerauslöser. Die unabhängige Bluetooth-Verarbeitung aus 3.0.9 und die vollständige Bestandsprüfung bleiben erhalten.

## Begrenzte Verbindungserholung

- Nach dem Schließen des bisherigen GATT-Clients bis mindestens eine Sekunde seit dessen Freigabe verstreichen lassen. Dies gilt auch, wenn der nächste Aufbau in einer neuen Dienstinstanz erfolgt. Keine Wartephase bei einem erstmaligen Aufbau ohne kurz zuvor geschlossenen Client.
- Nur beim anfänglichen Verbindungsfehler **133** einen frischen Client verwenden und vor dem zweiten beziehungsweise dritten Versuch mindestens eine beziehungsweise zwei Sekunden warten. Insgesamt höchstens drei Versuche. Ein anderer Fehler oder ein Fehler während eines bereits laufenden Archivtransfers wird dadurch nicht pauschal wiederholt.
- Fehlgeschlagene Clients schließen und verspätete Rückrufe anhand des zugehörigen Clients ignorieren. Die Wartephase bleibt abbrechbar. Es werden keine Archiv-/Schaltbefehle wiederholt, keine Kopplungen entfernt und keine Android-Bluetooth-Einstellungen automatisch zurückgesetzt.
- Verbindungsversuche samt Wartephasen protokollieren. Absolute Beginn-/Endzeiten je Geräteabruf stehen auch im nativen Diagnose-Text; die ISO-Zeitstempel verwenden UTC.

Die [Android-Referenz zu BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt) beschreibt das Schließen eines nicht mehr benötigten Clients und das Beenden beziehungsweise Abbrechen einer Verbindung. Sie garantiert keine erfolgreiche Wiederverbindung nach einem fest vorgegebenen Abstand. Die hier gewählten Wartezeiten und die Obergrenze sind eine begrenzte App-Strategie, keine zugesicherte Geräteheilung.

## Gemeinsamer Abschluss

Das Abschlussfeld zählt übernommene Werte aller tatsächlich bearbeiteten Geräte und nennt erfolgreiche sowie gescheiterte Geräte getrennt. Bei einem Fehler des zweiten Geräts bleiben die Werte des ersten im Gesamtergebnis enthalten. Ein vollständiger, ein teilweiser und ein ausdrücklich abgebrochener Durchlauf erhalten verschiedene Texte. Noch nicht bearbeitete Geräte werden nicht als erfolgreich eingestuft. Die einzelnen Ergebnisse werden zusätzlich als strukturierte Liste im bestehenden Archivstatus und gemeinsam im Diagnoseexport erhalten.

Wiederholte Durchläufe ersetzen das vorherige Ergebnis; alte Summen werden nicht erneut aufaddiert. Die vorhandene Geräte-Diagnose bleibt erhalten und erhält passende absolute Zeitangaben. Messwerte und bereits gespeicherte Archivblöcke werden dabei nicht verändert.

## Vorprüfung und Grenze

Alle produktiven Java-Quellen lassen sich kompilieren. Die lokale Prüfung besteht 109 Prüfschritte in 14 Phasen: 30 neue Prüfungen für Verbindungswiederholung, begrenztes Scheitern, Abbruch, Wechsel über Dienstinstanzen und das tatsächliche Repository-Gesamtergebnis sowie die bisherigen 79 SQL-/Transportprüfungen.

Die Verbindungstests führen unverändert extrahierte produktive Aufbau-, Warte-, Callback-, Operations- und Freigabemethoden mit asynchronen Android-/GATT-Adaptern aus. Geprüft sind 133 gefolgt von Erfolg, drei dauerhafte Fehlschläge, ein anderer Fehlerstatus, frische Clients, ignorierte alte Rückrufe, Abbruch während der Wartezeit und der Dienstwechsel. Der Ergebnisnachweis verwendet die tatsächlichen Repository-/JSON-/Diagnosemethoden. Die bisherigen Tests blockieren weiterhin echtes SQLite, übertragen einen auf DATA/BULK geteilten Archivblock und speichern ohne doppelte Zeilen. Die monotone Uhr läuft im Test vierfach beschleunigt; weder Testpartner noch Fehlerfolge sind ein physischer Funknachweis.

Gezielte Wiederholung ohne APK:

```sh
python3 tools/prove-archive-io.py --dependencies /tmp/autarpie-proof-deps --output /tmp/autarpie-connect-proof --phases connect-recovery connect-limits connect-stop connect-switch batch
```

Vor einer Veröffentlichung folgen nach Bau-Freigabe der vollständige APK-/Lint-/Browser-/Android-Prüflauf und die Prüfung der ursprünglichen Updatesignatur. Der tatsächliche Wechsel zwischen Regler und Shunt muss anschließend am Gerät bestätigt werden. Keine Aussage, dass der reale Status 133 damit bereits endgültig behoben ist.

## Vorgesehener Updateweg

Die Korrektur gehört auf die Zentrale beziehungsweise das direkt per Bluetooth erfassende Handy. Die bestehende Installation, Paketkennung, Updatesignatur, Kopplungen, Messwerte und Layouts erhalten. Anzeigen mit 3.0.9 bleiben mit dem vorhandenen Verbundprotokoll kompatibel und können den neuen Abschlusstext übernehmen; zusätzliche JSON-Felder sind ergänzend. Die bisherige Abruffrequenz bleibt erhalten. Nach dem späteren Update einmal einen vollständigen Geräteabruf ausführen und den Abschluss beider Geräte anhand des gemeinsamen Ergebnisses prüfen.

[Veröffentlichter Stand 3.0.9](changes-3.0.9.md) · [Versionshistorie](../CHANGELOG.md)
