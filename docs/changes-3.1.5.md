# AutarPie 3.1.5

## Akkubalken in der Wettervorschau

Nach 3.1.4 konnte die Akku-Prognose bereits vollständig aus dem gespeicherten
Lernmodell vorliegen und trotzdem grau bleiben. Die Kennzeichnung wartete noch
auf das historische Lernjournal. Insbesondere nach einem Neustart oder bei einem
erneut gelieferten Journal-Zwischenstand passte sie nicht zur verwendbaren
Prognose.

Die Akkubalken verwenden jetzt die Bereitschaft des passenden gespeicherten
Modells. Sobald aktueller Shuntwert, Wetter und Verbrauchsprofil eine Prognose
erlauben, erscheint sie in der eingestellten Akkufarbe. Eigene Wetterfarben
bleiben erhalten. Der Hintergrundabgleich des Journals blockiert dies nicht und
startet für unveränderte Grundlagen kein neues Lernen.

Sind geänderte Lerngrundlagen bekannt, bleibt die bisherige Prognose bis zur
erfolgreichen Speicherung der neuen Faktoren grau. Modell-Speicherfehler bleiben
erkennbar. Andere Anlagen oder Datenbestände übernehmen keine ausstehenden
Prüfwerte der vorherigen Anlage. Fehlender aktueller Ladezustand, unbekannter
Verbrauch und unvollständige Ertragsdaten ergeben weiterhin keine erfundenen
Akkuprozente. Veraltetes Wetter behält seine eigene Kennzeichnung.

Die Hinweise nennen nun ausdrücklich das bereits verwendete gespeicherte Modell
und den getrennten Hintergrundabgleich des Lernjournals. Die Detailansicht zeigt
die vorhandenen Lernstunden auch dann, wenn das Journal noch nicht gelesen ist.

## Aktualisieren

Für diese Darstellungskorrektur 3.1.5 auf den Anzeigehandys beziehungsweise im
eigenständigen Betrieb über die vorhandene Installation installieren. Eine
Zentrale auf 3.1.4 bleibt kompatibel. Wenn der ganze Verbund aktualisiert wird,
zuerst G5/Zentrale, anschließend S20/S10 und weitere Anzeigen aktualisieren.
Messdaten, Kopplungen, Lernfaktoren, Profile, Farben und Layouts bleiben erhalten.
Ein Löschen oder vollständiger Neuaufbau der Lerndaten ist nicht erforderlich.

## Prüfung

Der Browser-Gegenversuch mit der unveränderten 3.1.4-Oberfläche reproduziert den
Fehler: Eine berechnete Akku-Prognose aus dem gespeicherten Modell bleibt bei
absichtlich zurückgehaltenem Journal mit `grayscale(1)` markiert. Die Regression
prüft diesen Fall, einen Journal-Zwischenstand, unveränderte ältere Grundlagen,
neue Wetterwerte ohne erneutes Lernen und die Übernahme geänderter Faktoren erst
nach erfolgreicher Speicherbestätigung. Wetteralter, fehlender aktueller Shunt
und Anlagenwechsel behalten ihre eigenen Prüfungen. Software- und
Emulatorprüfungen ersetzen keine Bluetooth-/WLAN- oder Laufzeitmessung am eigenen
G5, S20 oder S10.
