package de.wattpit.app;

import android.content.SharedPreferences;
import org.json.*;

/** Shared manual supply journal. No forecasts or learned consumption are calculated here. */
final class SuppliesStore {
    static final String KEY="supplies-v1", WEATHER="supplies-weather-v1";
    static synchronized JSONObject read(SharedPreferences prefs)throws JSONException {
        JSONObject value=new JSONObject(prefs.getString(KEY,"{}"));
        if(value.optInt("version",1)!=1)throw new IllegalArgumentException("Unbekanntes Gas-/Wasserformat.");
        if(!value.has("events"))value.put("events",new JSONArray());
        if(!value.has("confirmedStays"))value.put("confirmedStays",new JSONArray());
        if(!value.has("defaults"))value.put("defaults",new JSONObject().put("water",20).put("gas",11));
        if(!value.getJSONObject("defaults").has("waterContainer"))value.getJSONObject("defaults").put("waterContainer",20);
        return value.put("version",1).put("protocol",1).put("containerSizeSettings",true).put("stayConfirmationSupported",true);
    }
    static JSONObject site(JSONObject value)throws Exception {
        if(value==null||value.length()==0)return new JSONObject();
        JSONObject checked=WeatherClient.checkedPlace(value);
        checked.put("key",Math.round(checked.getDouble("latitude")*1000)+":"+Math.round(checked.getDouble("longitude")*1000));return checked;
    }
    static synchronized void command(Repository repo,String json)throws Exception {
        if(json==null||json.length()>10000)throw new IllegalArgumentException("Ungültiger Gas-/Wassereintrag.");
        JSONObject command=new JSONObject(json),value=read(repo.prefs);String op=command.getString("op");
        JSONArray events=value.getJSONArray("events");String operation=command.optString("operationId");JSONArray operations=value.optJSONArray("operations");if(operations==null)operations=new JSONArray();
        if(!operation.isEmpty()){if(!operation.matches("[A-Za-z0-9_-]{8,80}"))throw new IllegalArgumentException("Ungültige Eintragskennung.");for(int i=0;i<operations.length();i++)if(operation.equals(operations.optString(i)))return;}
        switch(op){
            case "confirmStay":case "deleteStay":{
                JSONArray stays=value.getJSONArray("confirmedStays");String id=command.getString("id");
                if(!id.matches("[A-Za-z0-9_-]{8,80}"))throw new IllegalArgumentException("Ungültige Aufenthaltskennung.");
                int found=-1;JSONObject prior=null;for(int i=0;i<stays.length();i++)if(id.equals(stays.getJSONObject(i).getString("id"))){found=i;prior=stays.getJSONObject(i);break;}
                if(command.has("baseStay")&&(prior==null||!sameStay(prior,command.getJSONObject("baseStay"))))throw new IllegalArgumentException("Der Aufenthalt wurde inzwischen geändert. Bitte neu laden.");
                if(prior!=null&&!command.has("baseStay"))throw new IllegalArgumentException("Diese Aufenthaltskennung wurde bereits verwendet.");
                if(op.equals("deleteStay")){JSONArray kept=new JSONArray();for(int i=0;i<stays.length();i++)if(i!=found)kept.put(stays.get(i));value.put("confirmedStays",kept);break;}
                long from=command.getLong("from"),to=command.getLong("to"),now=System.currentTimeMillis();
                if(from<946684800000L||to<=from||to>now+300000L||to-from>1096L*86400000)throw new IllegalArgumentException("Bitte einen gültigen vergangenen Aufenthaltszeitraum bestätigen.");
                JSONObject location=site(command.getJSONObject("site"));if(!location.has("key"))throw new IllegalArgumentException("Bitte den tatsächlichen Aufenthaltsort auswählen.");
                if(found<0&&stays.length()>=500)throw new IllegalArgumentException("Es sind bereits 500 Aufenthalte bestätigt. Bitte vorhandene Bestätigungen prüfen.");
                JSONObject stay=new JSONObject().put("id",id).put("from",from).put("to",to).put("site",location).put("source","user-confirmed").put("updated",now);
                if(found>=0)stays.put(found,stay);else stays.put(stay);break;
            }
            case "defaults":{
                JSONObject defaults=new JSONObject();for(String kind:new String[]{"water","gas"}){double amount=command.getDouble(kind);checkAmount(amount,kind,false);defaults.put(kind,amount);}
                double container=command.has("waterContainer")?command.getDouble("waterContainer"):value.getJSONObject("defaults").getDouble("waterContainer");checkAmount(container,"water",false);defaults.put("waterContainer",container);
                if(command.has("followGps"))value.put("followGps",command.getBoolean("followGps"));
                value.put("defaults",defaults);break;
            }
            case "delete":{
                String id=command.getString("id");if(command.has("baseEvent"))for(int i=0;i<events.length();i++)if(id.equals(events.getJSONObject(i).getString("id"))&&!same(events.getJSONObject(i),command.getJSONObject("baseEvent")))throw new IllegalArgumentException("Der Eintrag wurde inzwischen geändert. Bitte den Konflikt prüfen.");JSONArray kept=new JSONArray();for(int i=0;i<events.length();i++)if(!id.equals(events.getJSONObject(i).getString("id")))kept.put(events.get(i));value.put("events",kept);break;
            }
            case "add":case "edit":{
                JSONObject input=command.getJSONObject("event");String id=input.getString("id"),kind=input.getString("kind"),type=input.getString("type");
                if(!id.matches("[A-Za-z0-9_-]{8,80}")||!(kind.equals("water")||kind.equals("gas"))||!(type.equals("stock")||type.equals(kind.equals("gas")?"swap":"supply")))throw new IllegalArgumentException("Ungültiger Eintragstyp.");
                long at=input.getLong("at");double amount=input.getDouble("amount");checkAmount(amount,kind,type.equals("stock"));
                if(at<946684800000L||at>System.currentTimeMillis()+300000L)throw new IllegalArgumentException("Bitte einen vergangenen oder aktuellen Zeitpunkt wählen.");
                String note=input.optString("note","").trim();if(note.length()>240)throw new IllegalArgumentException("Die Notiz ist zu lang.");
                int found=-1;JSONObject old=null;for(int i=0;i<events.length();i++)if(id.equals(events.getJSONObject(i).getString("id"))){found=i;old=events.getJSONObject(i);break;}
                if(op.equals("edit")&&old!=null&&command.has("baseEvent")&&!same(old,command.getJSONObject("baseEvent")))throw new IllegalArgumentException("Der Eintrag wurde inzwischen geändert. Bitte den Konflikt prüfen.");
                if(op.equals("edit")&&found<0)throw new IllegalArgumentException("Der Eintrag wurde inzwischen entfernt. Bitte neu laden.");
                JSONObject location=input.has("site")?site(input.getJSONObject("site")):old!=null?old.optJSONObject("site"):SupplyLocations.current(repo);
                JSONObject event=new JSONObject().put("id",id).put("kind",kind).put("type",type).put("at",at).put("amount",amount).put("empty",input.optBoolean("empty",true)).put("note",note).put("order",old!=null?old.optLong("order"):value.optLong("revision")+1).put("site",location==null?new JSONObject():location);
                if(op.equals("add")&&found>=0){if(!same(old,event))throw new IllegalArgumentException("Diese Eintragskennung wurde bereits verwendet.");return;}
                if(found<0&&events.length()>=5000)throw new IllegalArgumentException("Das Journal enthält bereits 5000 Einträge. Bitte vor weiteren Einträgen sichern.");
                if(found>=0)events.put(found,event);else events.put(event);
                java.util.List<JSONObject> sorted=new java.util.ArrayList<>();for(int i=0;i<events.length();i++)sorted.add(events.getJSONObject(i));
                sorted.sort((a,b)->{int c=Long.compare(a.optLong("at"),b.optLong("at"));if(c!=0)return c;c=Long.compare(a.optLong("order"),b.optLong("order"));return c!=0?c:a.optString("id").compareTo(b.optString("id"));});
                JSONArray ordered=new JSONArray();for(JSONObject e:sorted)ordered.put(e);value.put("events",ordered);break;
            }
            default:throw new IllegalArgumentException("Unbekannte Gas-/Wasseraktion.");
        }
        if(!operation.isEmpty()){JSONArray kept=new JSONArray();for(int i=Math.max(0,operations.length()-499);i<operations.length();i++)kept.put(operations.getString(i));kept.put(operation);value.put("operations",kept);}
        value.put("revision",value.optLong("revision")+1).put("updated",System.currentTimeMillis());
        // Capabilities belong to the running host version, not persisted data.
        value.remove("stayConfirmationSupported");
        if(!repo.prefs.edit().putString(KEY,value.toString()).commit())throw new java.io.IOException("Der Eintrag konnte nicht gespeichert werden.");
        SupplyLocations.configured(repo);SuppliesWeather.refresh(repo,true);
    }
    private static boolean same(JSONObject a,JSONObject b){return a.optString("kind").equals(b.optString("kind"))&&a.optString("type").equals(b.optString("type"))&&a.optLong("at")==b.optLong("at")&&Double.compare(a.optDouble("amount"),b.optDouble("amount"))==0&&a.optBoolean("empty",true)==b.optBoolean("empty",true)&&a.optString("note").equals(b.optString("note"))&&a.optJSONObject("site").optString("key").equals(b.optJSONObject("site").optString("key"));}
    private static boolean sameStay(JSONObject a,JSONObject b){return a.optLong("from")==b.optLong("from")&&a.optLong("to")==b.optLong("to")&&a.optLong("updated")==b.optLong("updated")&&a.optJSONObject("site")!=null&&b.optJSONObject("site")!=null&&a.optJSONObject("site").optString("key").equals(b.optJSONObject("site").optString("key"));}
    private static void checkAmount(double amount,String kind,boolean zero){if(!Double.isFinite(amount)||amount<(zero?0:0.01)||amount>(kind.equals("water")?10000:100))throw new IllegalArgumentException("Bitte eine gültige Menge in "+(kind.equals("water")?"Litern":"Kilogramm")+" eingeben.");}
    static synchronized void recordSite(Repository repo)throws Exception {
        SupplyLocations.recover(repo);SupplyLocations.configured(repo);
    }
    static JSONObject snapshot(Repository repo)throws JSONException {
        JSONObject value=read(repo.prefs).put("sites",new JSONArray(repo.prefs.getString(SupplyLocations.SITES,"[]"))),weather=new JSONObject(repo.prefs.getString(WEATHER,"{}"));weather.remove("forecastArchive");
        JSONObject chosen;try{chosen=SupplyLocations.current(repo);}catch(Exception e){chosen=new JSONObject();}String chosenKey=chosen.optString("key");if(!chosenKey.equals(weather.optString("siteKey")))weather.put("forecast",new JSONArray()).put("siteKey",chosenKey);
        weather.put("currentPlace",chosen);
        weather.put("enabled",SupplyLocations.automatic(repo)||!repo.prefs.getString("weatherPlace","").isEmpty()).put("pending",SuppliesWeather.busy()).put("error",repo.prefs.getString("suppliesWeatherError","")).put("location",SupplyLocations.status(repo));
        return value.put("weather",weather);
    }
    private SuppliesStore(){}
}
