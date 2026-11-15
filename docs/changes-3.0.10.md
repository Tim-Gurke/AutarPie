# AutarPie 3.0.10 · Gerätewechsel, stabile Wetterbalken und kurze Rollerladungen

**Update für Gerätewechsel, Wetteranzeige und das Lernen kurzer Rollerladungen.** Tim hat den konkret vorbereiteten Umfang am 15.11.2026 ausdrücklich zum APK-Bau und zur Veröffentlichung nach erfolgreicher Gesamtprüfung freigegeben.

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

## Wetterkachel: „Heute“ nach Sonnenuntergang

Beim erneuten Lesen des gespeicherten Abend-Akkustands wurde der bisherige Messpunkt bereits beim Start der Hintergrundanfrage verworfen. Dadurch verschwanden Akku-/Extras-Balken, bis die Antwort eintraf. Für denselben Anlagenverbund, Shunt und Sonnenuntergang bleibt der bekannte Messpunkt jetzt während der Anfrage sichtbar, mit Hinweis auf die laufende Aktualisierung. Eine vorübergehende Lesestörung erhält ihn mit Kennzeichnung. Eine erfolgreiche Antwort ohne gültigen Messpunkt entfernt ihn weiterhin; ein Anlagen-/Shuntwechsel übernimmt keine fremden Werte. Verspätete Antworten alter Anfragen werden ignoriert. Der gespeicherte Abendstand wird nicht durch einen aktuellen oder erfundenen Wert ersetzt.

## Roller: Teil- und Zwischenladungen lernen

Eine Zwischenladung benötigt weder einen vollen Akku noch das Erreichen des eingestellten Ladeziels. Ihre tatsächlich beobachtete Energie zählt unabhängig von der Dauer. Der bisherige Lernfilter verlangte allerdings mindestens 98 % Zeitabdeckung; das Häufigkeitslernen verlangte sogar exakt lückenlose Kalendertage. Damit konnten vorhandene Journal-Einträge aus dem Lernen fallen, obwohl nur kurze Funk-/Abfragegrenzen fehlten.

- Zeitlich zusammengehörige Fragmente desselben Ladewegs bis drei Minuten Abstand mit passendem Akkustand zusammen auswerten. Das gespeicherte Ladejournal bleibt unverändert; doppelte beziehungsweise überlappende Vorgänge werden nicht mehrfach gezählt.
- Für eine anteilige Lückenschätzung mindestens 90 % bekannte Zeit, höchstens drei Minuten am Stück und höchstens zehn Minuten insgesamt verlangen. Nur begrenzte Lücken mit der mittleren beobachteten Leistung dieses Vorgangs ergänzen. Beobachtete Eingangsenergie und geschätzter Lückenanteil stehen getrennt in den abgeleiteten Lerndaten. Journal-Messwerte, Anlagenverbrauch und Wirkungsgradmessung werden dadurch nicht umgeschrieben.
- Die automatische Erkennung wartet nach Ende der Ladung auf bestätigten Leerlauf. Beobachtete AUS-Zeiten desselben Ladewegs sind dabei bekannte Nichtladezeit. Sie dürfen eine kurze Ladung nicht wegen vermeintlich fehlender BMS-Energie aus dem Lernen werfen. Die bestehende Bestätigung eines positiven Ladestarts bleibt erhalten; bloßes Einschalten eines Lade-MOS zählt nicht als übertragene Energie.
- Ladehäufigkeit ab drei vergangenen ausreichend überwachten Ladeweg-Tagen ableiten: mindestens 98 % Zeitabdeckung, keine einzelne unbekannte Lücke über zwei Minuten und kein unzureichend erfasster Ladevorgang. Größere Ausfälle bleiben unbekannt. Die Tagesgrenzen berücksichtigen die jeweilige Zeitzone und Sommerzeit. Eine häufige kurze Zwischenladung zählt als Nutzung, ohne auf eine volle Akkuladung hochgerechnet zu werden.
- „Letzte aufgezeichnete Ladung“ und „Zuletzt fürs Lernen genutzt“ getrennt anzeigen. Bei einem ausgeschlossenen neuesten Vorgang Grund und Messabdeckung nennen. Zahl der für die Häufigkeit geeigneten Tage und geschätzte kurze Lücken sichtbar machen. „Bedarf unklar“ kann weiterhin eine noch unbekannte Häufigkeit bedeuten, obwohl die Energie einzelner Ladungen bereits gelernt wurde.

Die Neuberechnung verwendet vorhandene Journale und Rohbeobachtungen auf den Anzeigehandys beziehungsweise im eigenständigen Betrieb. Fehlende größere Messabschnitte lassen sich dadurch nicht nachträglich als gemessen herstellen. Das Zentralsystem bleibt Rohdatensammler.

## Vorprüfung und Grenze

Alle produktiven Java-Quellen lassen sich kompilieren. Die lokale Prüfung besteht 109 Prüfschritte in 14 Phasen: 30 neue Prüfungen für Verbindungswiederholung, begrenztes Scheitern, Abbruch, Wechsel über Dienstinstanzen und das tatsächliche Repository-Gesamtergebnis sowie die bisherigen 79 SQL-/Transportprüfungen.

Die Verbindungstests führen unverändert extrahierte produktive Aufbau-, Warte-, Callback-, Operations- und Freigabemethoden mit asynchronen Android-/GATT-Adaptern aus. Geprüft sind 133 gefolgt von Erfolg, drei dauerhafte Fehlschläge, ein anderer Fehlerstatus, frische Clients, ignorierte alte Rückrufe, Abbruch während der Wartezeit und der Dienstwechsel. Der Ergebnisnachweis verwendet die tatsächlichen Repository-/JSON-/Diagnosemethoden. Die bisherigen Tests blockieren weiterhin echtes SQLite, übertragen einen auf DATA/BULK geteilten Archivblock und speichern ohne doppelte Zeilen. Die monotone Uhr läuft im Test vierfach beschleunigt; weder Testpartner noch Fehlerfolge sind ein physischer Funknachweis.

Zusätzlich bestehen 323 JavaScript-Prüfungen einschließlich sieben neuer Prüfungen der produktiven Wetter-Anfrage-/Callback-/Balkenmethoden und fünf neuer Rollerprüfungen. Der Wettervergleich reproduziert vier fehlschlagende Fälle vor der Korrektur. 35 native Roller-Prüfschritte mit echtem SQLite sowie eine zusätzliche Prüfung der tatsächlichen JavaScript-Anzeige aus dem nativen Ergebnis sind erfolgreich. Die neue Stichprobenprüfung scheitert am bisherigen Lernfilter und besteht mit der Korrektur. Sie prüft auch den produktiven automatischen Ladestart/-abschluss mit neutralen BMS-Paketen: eine Minute Zwischenladen, weit unter dem Ladeziel, bleibt als eigener kleiner Energiebedarf erhalten. Keine physische Bluetooth-Verbindung wird daraus abgeleitet. Ein zusätzlicher Browserfall prüft eine absichtlich verzögerte Wetterantwort im vollständigen Gesamtprüflauf. Die sieben lokalen JavaScript-Fälle führen die produktiven Methoden aus; ein DOM-Adapter ersetzt dabei das Browserlayout.

Gezielte Wiederholung ohne APK:

```sh
python3 tools/prove-archive-io.py --dependencies /tmp/autarpie-proof-deps --output /tmp/autarpie-connect-proof --phases connect-recovery connect-limits connect-stop connect-switch batch
```

Vor einer Veröffentlichung müssen der vollständige APK-/Lint-/Browser-/Android-Prüflauf und die Prüfung der ursprünglichen Updatesignatur erfolgreich abgeschlossen sein. Der tatsächliche Wechsel zwischen Regler und Shunt muss anschließend am Gerät bestätigt werden. Keine Aussage, dass der reale Status 133 damit bereits endgültig behoben ist.

## Vorgesehener Updateweg

Zuerst die Zentrale beziehungsweise das direkt per Bluetooth erfassende Handy aktualisieren, danach die Anzeigehandys für die korrigierten Wetterbalken und das Rollerlernen. Die bestehende Installation, Paketkennung, Updatesignatur, Kopplungen, Messwerte und Layouts erhalten. Anzeigen mit 3.0.9 bleiben mit dem vorhandenen Verbundprotokoll kompatibel und können den neuen Abschlusstext übernehmen; die neuen Lern- und Darstellungskorrekturen benötigen dort jedoch ebenfalls 3.0.10. Zusätzliche JSON-Felder sind ergänzend. Die bisherige Abruffrequenz bleibt erhalten. Nach dem späteren Update einmal einen vollständigen Geräteabruf ausführen und den Abschluss beider Geräte anhand des gemeinsamen Ergebnisses prüfen.

[Veröffentlichter Stand 3.0.9](changes-3.0.9.md) · [Versionshistorie](../CHANGELOG.md)
