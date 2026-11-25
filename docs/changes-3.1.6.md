# AutarPie 3.1.6

## Aufenthalte rückwirkend bestätigen

Die Gasansicht erkennt schon vorhandene Ortszeitlücken und fragt „Warst du
weiterhin in …?“. Ja bestätigt den vorgeschlagenen Zeitraum am vorgeschlagenen
Ort. Nein öffnet vorausgefüllte Zeiten, bekannte Orte und eine Ort-/Postleitzahlsuche.
Ein neuer Flaschenwechsel ist dafür nicht erforderlich. Bestätigungen bleiben
korrigierbar und entfernbar. Sie werden getrennt von ursprünglichen GPS-Zeiten
und Beschaffungen gespeichert; tatsächliche Ortsaufzeichnungen haben Vorrang.

Vorhandenes Wetter des bestätigten Ortes wird wiederverwendet. Fehlende
Wetterwerte werden für die benötigten Orte und Tage gezielt nachgeladen.
Unbekannte Ortszeit und fehlendes Wetter bleiben getrennt erkennbar. Eine
Bestätigung erzeugt keine Wetterwerte. Die Zentrale speichert die Bestätigung;
Anzeigehandys beziehungsweise der eigenständige Betrieb aktualisieren damit die
passenden Verbrauchsfaktoren. Erfolg wird erst nach Speicherbestätigung angezeigt.

## Den erwarteten Gasbedarf früher erkennen

Üblicher Durchschnitt, letzter vollständiger Flaschenverbrauch und erwarteter
Bedarf der nächsten 24 Stunden werden getrennt und mit zwei Nachkommastellen
angezeigt. Die bisherige Kachel heißt eindeutig „Gas · Üblicher Tagesbedarf“.
Die zusätzliche Kachel „Gas · Bedarf nächste 24 Stunden“ ist frei wählbar.

Bei gleich kalten oder kälteren ganzen Prognosetagen wird mindestens der letzte
vollständige Flaschenverbrauch angesetzt. Ein höherer Wettermodellwert bleibt
wirksam. Fehlt das historische Vergleichswetter oder eine ausreichend aktuelle
passende Vorhersage, gilt dieselbe Untergrenze ausdrücklich als vorsichtige
Annahme. Belegbar wärmere Tage dürfen einen niedrigeren modellierten Bedarf ergeben.

Die Untergrenze wird aus erfasster Flaschenmenge und Abschnittsdauer gebildet.
Beispielsweise ergeben 11 kg bei mindestens 1,10 kg/Tag höchstens etwa zehn Tage
Reichweite. Bedarf für 3/7/14 Tage und Reichweite verwenden dieselbe Regel.
Ein milder Nachmittag allein senkt die Untergrenze eines insgesamt kälteren
Prognosetags nicht. Neue Wettervorhersagen wenden gespeicherte Faktoren an.
Der heutige geschätzte Restvorrat wird durch zukünftiges Wetter nicht rückwirkend
neu gesetzt.

[Ausführliche Bedienung und Rechenregel mit neutralen Screenshots](https://github.com/Tim-Gurke/AutarPie/blob/main/docs/gas-stay-confirmation.md).

## Aktualisieren

Zuerst die **Zentrale**, danach **alle Anzeigehandys auf 3.1.6** aktualisieren.
Im eigenständigen Betrieb genügt das eine Handy. Über die bestehende Installation
installieren. Messdaten, Journal, Kopplungen, Farben, Layouts und gespeicherte
Faktoren bleiben erhalten. Keine Deinstallation und kein Löschen der Lerndaten nötig.

Alte Zentralen stellen weiterhin den bisherigen Gas-/Wasserstand bereit;
Bestätigungsaktionen bleiben mit Updatehinweis deaktiviert. Alte Anzeigen
verwenden neue Bestätigungen noch nicht. Die bestehende Offline-Warteschlange
für Beschaffungen und Flaschenwechsel bleibt erhalten. Die Ortsbestätigung
erfordert eine erreichbare Zentrale.

## Prüfung und Grenzen

Der vorbereitete Produktivcode bestand 393 JavaScript-Prüfungen, 46 Aussagen zu
nativer Speicherung und Wetterzuordnung sowie tatsächliche Browserprüfungen für
Ja/Nein, Suche, Korrektur, Entfernen, Abbrechen, Offline-/Fehlerfälle und alte
Zentralen. Ein Browser-Neustart verwendet gespeicherte Faktoren ohne erneuten
Lernaufruf. Rohdaten bleiben erhalten; kalte und warme Prognosen sowie andere
Flaschengrößen sind geprüft. Der vollständige Android-Freigabelauf ergänzt
APK-Bau/Lint, JVM- und native Integrationsprüfungen sowie Android-Oberflächen und
Sicherungswiederherstellung. Sein Ergebnis wird bei Veröffentlichung im README
verlinkt. Software- und Emulatorprüfungen sind keine GPS-/WLAN-/Bluetooth- oder
Laufzeitmessung am eigenen G5/S20/S10.
