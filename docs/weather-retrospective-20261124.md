# Vorbereitung: Sonnenanteil als Tagessymbol in der Wetterrückschau

Stand 24.11.2026, Ausgangspunkt AutarPie 3.1.3. Noch nicht als APK veröffentlicht.

Die dreitägige Rückschau in `update-ui.js:decorateWeather()` verwendete unmittelbar `day.code`. Die Vorschau in `solar-core.js:forecast()` verwendet bereits `sunshineCode(day)`. Dadurch konnten dieselben vorhandenen Sonnenstunden in Vorschau und Rückschau zu unterschiedlichen Symbolen führen.

Open-Meteo definiert den täglichen `weather_code` als stärkste Wettererscheinung innerhalb des gesamten Tages, nicht als überwiegendes Sonnenwetter. Eine solche Erscheinung kann neben einem hohen Sonnenscheinanteil auftreten. Dokumentation: https://open-meteo.com/en/docs#daily-parameter-definition. Das erklärt den ungeeigneten Symbolpfad; es beweist keine konkrete damalige lokale Wettererscheinung oder Fehlerfreiheit der Anbieterwerte.

Die Rückschau verwendet nun dieselbe vorhandene Sonnenanteil-Funktion wie die Vorschau. Ab 75 Prozent Sonnenschein innerhalb der Tageslänge erscheint Sonne, ab 20 Prozent heiter/wolkig, darunter bewölkt. Fehlen Sonnenstunden oder gültige Sonnenauf-/untergangszeiten, bleibt der ursprüngliche Anbieter-Wettercode der Rückweg. Die Detailansicht erklärt diese Bedeutung.

Originale Wetterdaten und Wettercodes bleiben erhalten. Weder Regler-Tagesertrag noch SmartShunt-Messungen, Wetterabruf, Gas-Wettermodell oder Prognoseberechnungen werden verändert. Energieertrag dient nicht als Ersatz-Wettermessung. Ein Regentag mit sonnigen Abschnitten wird hier nach seinem Sonnenanteil dargestellt; das Tagessymbol ist keine Aussage, dass überhaupt kein Niederschlag vorkam.

## Prüfung

Vier neue Node-Prüfungen führen die produktive Rückschau-Erzeugung und den produktiven SVG-Symbolrenderer mit DOM-Adaptern aus: hohe Sonnenstunden bei Regen-/Nebelcode, fehlende Grundlagen/Unbekannt, gemessene Null und drei korrekte Kalendertage sowie erklärender Detailtext bei erhaltenen Energie-/Akkudaten. Testwerte sind synthetisch.

Zusammen mit Solarberechnung und Wetterfarben bestehen 66 Prüfungen:

```
node --test tools/weather-retrospective.test.cjs tools/solar-core.test.cjs tools/weather-style.test.cjs
```

Dies ist kein visueller Browsertest und keine tatsächliche Wetter-/Handymessung. Gegenprüfung am unveränderten ausgelieferten UI schlägt für die neue Symbolerwartung und den neuen Detailhinweis fehl. APK-Bau und Veröffentlichung wurden in diesem Vorbereitungsschritt nicht gestartet.
