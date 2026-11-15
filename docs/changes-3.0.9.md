# AutarPie 3.0.9 · Archivprüfung und Bluetooth unabhängig bedienen

**Gezieltes Update des Victron-Archivabrufs.** Bluetooth-Verarbeitung und Sitzungserhalt werden unabhängig von blockierenden lokalen Datenbankprüfungen bedient. Der konkrete Umfang wurde am 15.11.2026 zum APK-Bau und zur Veröffentlichung nach erfolgreicher Gesamtprüfung freigegeben.

## Problem und Änderung

Beim lokalen Bestandsabgleich konnte eine blockierende SQLite-Abfrage den gemeinsamen Arbeitsablauf anhalten. Eingehende Bluetooth-Pakete blieben dabei liegen, der Regler erhielt keine weiteren Empfangsfreigaben und die regelmäßige Sitzungsnachricht konnte ausbleiben. Die Empfangsbegrenzung aus 3.0.8 verhinderte einen Pufferüberlauf, beseitigte diese Warteabhängigkeit aber noch nicht. Die frühere Dokumentation behauptete irrtümlich bereits einen unabhängigen Sitzungserhalt und wurde berichtigt.

3.0.9 verwendet dafür einen eigenen seriellen Bluetooth-Arbeitsablauf. Android-GATT-Aufträge, ihre asynchronen Bestätigungen, DATA-/BULK-Verarbeitung, Freigaben und regelmäßige Sitzungsnachrichten werden unabhängig von lokalen Datenbankprüfungen bedient. Ein Steuerauftrag oder eine Archivabfrage wird weiterhin erst nach dem vorherigen GATT-Auftrag begonnen; zwei Threads schreiben nicht gleichzeitig auf dieselbe Verbindung. Zwischen abgeschlossenen Abfragen werden unaufgeforderte oder verspätete Meldungen verarbeitet und verworfen. Sie werden nicht als neue Archivmesswerte verbucht. Fragmente bleiben bis zur vollständigen Dekodierung erhalten.

Eine ausdrücklich abgebrochene Übertragung und ein vorrangiger manueller Schaltauftrag schließen die Bluetooth-Verbindung auch dann sofort, wenn die lokale SQL-Abfrage noch wartet. Wartende Transportaufrufe werden freigegeben. Ein vom Gerät ausgelöster Verbindungsabbruch bleibt ein Transportfehler mit Status. Ein nicht bestätigter Schreibauftrag beendet die nutzbare Sitzung; ein verspäteter Callback kann keine folgende Schreiboperation bestätigen. Die Datenbankprüfung und die abschließende Vollständigkeitsprüfung bleiben vollständig erhalten.

Die Diagnose ergänzt relative Zeiten je Schritt, Dauer einzelner Bestandsprüfungen, größte Android-Auftrags-/Wartezeit, verarbeitete Pakete während lokaler Abfragen und Anzahl/Abstände bestätigter Sitzungsnachrichten. Die Angaben erscheinen auch im Text des nativen Diagnoseexports. Unbekannte Werte werden nicht als Erfolg ausgegeben.

## Quellcodeprüfung vor dem APK-Bau

`tools/prove-archive-io.py` kompiliert alle produktiven Java-Quellen ohne APK. Für den Ablaufnachweis extrahiert es unverändert die produktiven Schreib-/Bestätigungs-, Empfangs-, Abfrage-, Fehler- und Diagnosemethoden aus `ArchiveService`. Der neue `ArchiveIo`, Codec, Bestandsabgleich und die Speicherung werden tatsächlich ausgeführt.

Ein zweiter echter SQLite-Anschluss hält eine exklusive Sperre. Eine Beobachtung bestätigt den Eintritt in die wirkliche Bestandsabfrage; die Sperre blockiert diese vor dem Durchlaufen der Zeilen. Die SQL-Abfrage wird weder durch eine Schlaf-Funktion ersetzt noch ruft der Test während der Sperre selbst die Wartung auf. Ein asynchroner Testpartner liefert über zwei simulierte GATT-Characteristics Daten, hält das gemeinsame 128-Paket-Fenster ein und fordert in einem kontrollierten Modell regelmäßige Sitzungsnachrichten. Android-Handler, Funkpartner und vierfach beschleunigte monotone Uhr sind Testadapter, keine gemessenen Geräteeigenschaften.

Der Vergleich verwendet den veröffentlichten 3.0.8-Quellstand `c15f75c6b4be0adea1e543b029484a7353d14473`. Während derselben echten SQL-Sperre bleiben dort alle 128 Pakete unverarbeitet und die modellierte Gegenstelle trennt. Mit der Korrektur laufen Paketverarbeitung, Empfangsfreigaben und Sitzungsnachrichten weiter. Nach der Freigabe wird ein über DATA/BULK aufgeteilter Archivblock über die produktiven Schreib-/Callback-/Abfragemethoden dekodiert und in echtes SQLite übernommen. Wiederholung erzeugt keine doppelten Messzeilen.

79 erfolgreiche Prüfschritte decken den bisherigen Ablauf, den korrigierten Transport über die Android-30-/35-Schreibpfade, getrennte SQL-/Bluetooth-Arbeit, Abbruch, Schaltvorrang, Trennung mit Status 19, verschachtelte/gleichzeitige Aufträge, Schließen wartender Aufrufe, abgelehnte Schreibaufträge, GATT-Status 133 sowie eine tatsächlich ausbleibende Schreibbestätigung einschließlich verspätetem Callback ab. Berichte enthalten die Quellprüfsummen und gemessenen Testzähler. Ergänzend bestehen 227 vorhandene Archiv-Speicher-/Zeitzuordnungsprüfungen und 14 Rückstauprüfungen: insgesamt 320 erfolgreiche Prüfschritte mit dem vorbereiteten Quellstand. Alle produktiven Java-Dateien lassen sich dabei kompilieren. Bei Sitzungsnachrichten bezeichnet die Bestätigung den Android-GATT-Schreibcallback, nicht eine zusätzlich gemessene Firmwareantwort.

Beispiel zum erneuten Prüfen ohne APK:

```sh
python3 tools/fetch-proof-deps.py /tmp/autarpie-proof-deps
git fetch --no-tags --depth=1 origin c15f75c6b4be0adea1e543b029484a7353d14473
python3 tools/prove-archive-io.py --dependencies /tmp/autarpie-proof-deps --output /tmp/autarpie-archive-io-proof
```

Diese Vorprüfung ist kein Android-Build, Lint-, Emulator-, Funk- oder nächtlicher Neustarttest. Vor Veröffentlichung müssen zusätzlich der vollständige APK-Bau, Lint sowie die Browser- und Android-Oberflächenprüfungen erfolgreich sein. Der Release-Nachweis nennt den dazugehörigen geprüften Quellstand und die erfolgreichen Prüfungen. Der tatsächliche vollständige Archivabruf an Regler und Shunt muss nach Installation praktisch bestätigt werden. Das kontrollierte Fehlerbild erklärt die Abhängigkeit von langsamen SQL-Abfragen, beweist aber weder Datenbankwachstum als konkreten Auslöser noch den genauen Firmwaregrund des realen Status 19.

## Installation und Update-Reihenfolge

Über die bestehende Installation mit unveränderter Updatesignatur installieren. Die Änderung gehört auf die **Zentrale beziehungsweise das direkt per Bluetooth erfassende Handy**. Anzeigen mit 3.0.8 bleiben kompatibel; weder Datenbankformat noch Verbundprotokoll werden verändert. Zuerst das erfassende Handy aktualisieren, anschließend können die Anzeigen folgen. Gespeicherte Messdaten, Archive, private Verbindungen und eigene Layouts bleiben erhalten. Der bisherige automatische Abrufabstand wird nicht verkürzt.

Danach einmal **Fehlende Archivdaten abrufen** ausführen. Entscheidend sind vollständig abgeschlossene Geräteabrufe und tatsächlich gespeicherte Zeiträume beziehungsweise übernommene Archivwerte, sofern Lücken bestehen. Null neue Werte können bei bereits vollständigem lokalem Bestand richtig sein; eine Trennung mit 0 Werten gilt nicht als erfolgreicher Abgleich. Bei erneutem Fehler enthält die native Diagnose nun die dazugehörigen Schritt-/SQL-/Transportzeiten. Kein Werksreset und keine erneute Kopplung für diese Korrektur vorgesehen.

[Vorgängerversion 3.0.8](https://github.com/Tim-Gurke/AutarPie/blob/main/docs/changes-3.0.8.md) · [Versionshistorie](../CHANGELOG.md)
