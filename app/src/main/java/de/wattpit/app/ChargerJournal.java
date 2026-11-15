package de.wattpit.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.util.*;

/** The collector stores observations. Only a display/standalone query derives sessions and Wh. */
final class ChargerJournal {
    static final String SOURCE_KEY="charger-journal-source-v1";
    static synchronized String source(Repository repo)throws Exception{
        String source=repo.prefs.getString(SOURCE_KEY,"");if(!source.isEmpty())return source;
        source=UUID.randomUUID().toString();if(!repo.prefs.edit().putString(SOURCE_KEY,source).commit())throw new java.io.IOException("Journalzuordnung konnte nicht gespeichert werden.");return source;
    }
    static void schema(SQLiteDatabase db){RollerLedger.chargerSchema(db);}
    static void record(Repository repo,String mac,JSONObject sample)throws Exception{
        RollerLedger ledger=RollerLedger.get(repo.applicationContext());
        synchronized(ledger){record(ledger.getWritableDatabase(),source(repo),mac,sample);}
    }
    static void record(SQLiteDatabase db,String source,String mac,JSONObject input)throws Exception{
        long at=input.optLong("at"),bucket=at/60000*60000;if(at<=0||mac.isEmpty())return;
        String type=input.optString("type","sample");if(!Arrays.asList("sample","command","gap").contains(type))return;
        JSONObject point=new JSONObject().put("at",at).put("type",type).put("name",input.optString("name","608PD")).put("role",input.optString("role","model"));
        if(type.equals("sample")){
            if(!Arrays.asList(0,1,3).contains(input.optInt("mode",-1)))return;
            for(String field:new String[]{"mode","voltage","current","capacity","elapsed"}){double value=input.optDouble(field,Double.NaN);if(!Double.isFinite(value)||value<0)return;point.put(field,input.get(field));}
            JSONArray cells=new JSONArray(),in=input.optJSONArray("cells");if(in!=null)for(int i=0;i<Math.min(6,in.length());i++){double v=in.optDouble(i,Double.NaN);if(Double.isFinite(v)&&v>=0&&v<=5.5)cells.put(v);}point.put("cells",cells);
        }else if(type.equals("command")){String action=input.optString("action");if(!action.equals("start")&&!action.equals("stop"))return;point.put("action",action);}
        JSONArray rows=new JSONArray();try(Cursor c=db.rawQuery("SELECT payload FROM charger_raw WHERE source=? AND start=? AND mac=?",new String[]{source,""+bucket,mac})){if(c.moveToFirst())rows=new JSONArray(c.getString(0));}
        JSONObject last=null;
        for(int i=rows.length()-1;i>=0;i--){JSONObject p=rows.getJSONObject(i);if(p.optLong("at")==at&&p.optString("type").equals(type))return;if(last==null&&p.optString("type").equals("sample"))last=p;}
        // Five-second raw samples, with immediate state changes and one-minute idle observations.
        if(type.equals("sample")&&last!=null&&last.optInt("mode")==point.optInt("mode")&&at>=last.optLong("at")&&at-last.optLong("at")<(point.optInt("mode")==0?60000:5000)&&point.optLong("elapsed")>=last.optLong("elapsed")&&point.optLong("capacity")>=last.optLong("capacity"))return;
        rows.put(point);ContentValues values=new ContentValues();values.put("source",source);values.put("start",bucket);values.put("end",at+1);values.put("mac",mac);values.put("payload",rows.toString());db.insertWithOnConflict("charger_raw",null,values,SQLiteDatabase.CONFLICT_REPLACE);
    }
    static JSONObject query(RollerLedger ledger,String source,long from,long to,String selected,long now)throws Exception{
        if(from<0||to<=from||to-from>366L*86400000)throw new IllegalArgumentException("Ungültiger Journalzeitraum.");
        Map<String,State> states=new HashMap<>();List<JSONObject> sessions=new ArrayList<>(),commands=new ArrayList<>();long samples=0;
        String where="source=? AND end>? AND start<?";List<String> args=new ArrayList<>(Arrays.asList(source,""+from,""+to));if(selected!=null&&!selected.isEmpty()){where+=" AND mac=?";args.add(selected);}
        synchronized(ledger){try(Cursor cursor=ledger.getReadableDatabase().rawQuery("SELECT mac,payload FROM charger_raw WHERE "+where+" ORDER BY mac,start",args.toArray(new String[0]))){
            while(cursor.moveToNext()){
                String mac=cursor.getString(0);State state=states.computeIfAbsent(mac,key->new State(mac,sessions,commands));JSONArray rows=new JSONArray(cursor.getString(1));
                List<JSONObject> ordered=new ArrayList<>();for(int i=0;i<rows.length();i++)ordered.add(rows.getJSONObject(i));ordered.sort(Comparator.comparingLong(p->p.optLong("at")));
                for(JSONObject point:ordered){long at=point.optLong("at");if(at<from||at>=to)continue;state.accept(point);if(point.optString("type").equals("sample"))samples++;}
            }
        }}
        for(State state:states.values())state.finish();
        sessions.sort((a,b)->Long.compare(b.optLong("lastAt"),a.optLong("lastAt")));commands.sort((a,b)->Long.compare(b.optLong("at"),a.optLong("at")));
        JSONArray output=new JSONArray(),requests=new JSONArray();for(int i=0;i<Math.min(1000,sessions.size());i++){
            JSONObject session=sessions.get(i);boolean ended=session.optLong("endAt")>0;
            session.put("state",ended?(session.optBoolean("startObserved")&&session.optBoolean("endObserved")&&!session.optBoolean("hasGap")?"complete":"incomplete"):now>=session.optLong("lastAt")&&now-session.optLong("lastAt")<=15000?"active":"unknown");output.put(session);
        }
        for(int i=0;i<Math.min(500,commands.size());i++)requests.put(commands.get(i));
        return new JSONObject().put("version",1).put("from",from).put("to",to).put("at",now).put("energySide","608PD-Akkuseite").put("sessions",output).put("commands",requests).put("sampleCount",samples).put("truncated",sessions.size()>1000||commands.size()>500);
    }
    private static final class State {
        final String mac;final List<JSONObject> output,commands;final List<JSONObject> pending=new ArrayList<>();
        JSONObject previous,current;boolean gap;
        State(String mac,List<JSONObject> output,List<JSONObject> commands){this.mac=mac;this.output=output;this.commands=commands;}
        void accept(JSONObject point)throws JSONException{
            String type=point.optString("type");long at=point.optLong("at");
            if(type.equals("gap")){gap=true;if(current!=null)current.put("hasGap",true);return;}
            if(type.equals("command")){JSONObject request=new JSONObject().put("device",mac).put("at",at).put("action",point.optString("action")).put("confirmed",false);commands.add(request);pending.add(request);return;}
            if(!type.equals("sample")||previous!=null&&at<=previous.optLong("at"))return;
            int mode=point.optInt("mode");long priorAt=previous==null?0:previous.optLong("at"),delta=at-priorAt;
            for(Iterator<JSONObject> iterator=pending.iterator();iterator.hasNext();){JSONObject request=iterator.next();long age=at-request.optLong("at");if(age<0)continue;if(age<=8000&&(request.optString("action").equals("stop")?mode==0:mode==1))request.put("confirmed",true).put("confirmedAt",at);if(age>8000||request.optBoolean("confirmed"))iterator.remove();}
            boolean reset=previous!=null&&previous.optInt("mode")==mode&&(point.optLong("elapsed")+2<previous.optLong("elapsed")||point.optLong("capacity")<previous.optLong("capacity"));
            boolean roleChanged=current!=null&&!current.optString("role").equals(point.optString("role"));
            if(current!=null&&(mode!=current.optInt("mode")||reset||roleChanged)){
                boolean confirmed=!gap&&delta>0&&delta<=15000&&!reset&&!roleChanged;
                current.put("endAt",confirmed?at:current.optLong("lastAt")).put("endObserved",confirmed);
                if(reset)current.put("reason","Gerätezähler zurückgesetzt");if(roleChanged)current.put("reason","Energiepfad geändert");output.add(current);current=null;
            }
            if(mode==1||mode==3){
                if(current==null)current=new JSONObject().put("id",mac+"@"+at).put("device",mac).put("name",point.optString("name","608PD")).put("role",point.optString("role","model")).put("mode",mode).put("startAt",at).put("lastAt",at).put("startObserved",previous!=null&&previous.optInt("mode")==0&&delta>0&&delta<=65000&&!gap&&point.optLong("elapsed")<=15).put("endObserved",false).put("hasGap",false).put("batteryWh",0).put("observedMillis",0).put("gapMillis",0);
                else if(previous!=null&&previous.optInt("mode")==mode){
                    if(!gap&&delta>0&&delta<=15000){
                        double first=previous.optDouble("voltage",Double.NaN)*previous.optDouble("current",Double.NaN),last=point.optDouble("voltage",Double.NaN)*point.optDouble("current",Double.NaN);
                        if(Double.isFinite(first)&&Double.isFinite(last)&&first>=0&&last>=0)current.put("batteryWh",current.optDouble("batteryWh")+(first+last)*.5*delta/3600000.0).put("observedMillis",current.optLong("observedMillis")+delta);
                    }else current.put("hasGap",true).put("gapMillis",current.optLong("gapMillis")+Math.max(0,delta));
                }
                current.put("lastAt",at).put("counterMah",point.optLong("capacity")).put("deviceElapsed",point.optLong("elapsed")).put("voltage",point.optDouble("voltage")).put("cells",point.optJSONArray("cells"));
            }
            previous=point;gap=false;
        }
        void finish(){if(current!=null)output.add(current);}
    }
    private ChargerJournal(){}
}
