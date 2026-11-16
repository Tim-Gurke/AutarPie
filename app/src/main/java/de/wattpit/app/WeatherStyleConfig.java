package de.wattpit.app;

import org.json.*;

/** Portable, tile-local weather colors. No CSS or selector text is accepted. */
final class WeatherStyleConfig {
    private static JSONObject colors(JSONObject input,String... keys)throws Exception {
        JSONObject out=new JSONObject();if(input==null)return out;
        for(String key:keys){String value=input.optString(key);if(value.matches("#[0-9a-fA-F]{6}"))out.put(key,value.toLowerCase(java.util.Locale.ROOT));}return out;
    }
    static JSONObject checked(JSONObject input)throws Exception {
        JSONObject out=new JSONObject().put("version",1).put("colors",new JSONObject()).put("icons",new JSONObject());
        if(input.optInt("version",1)!=1)return out;
        out.put("colors",colors(input.optJSONObject("colors"),"text","muted","symbol","line","control","selected","solar","battery","extras","track"));
        String[][] types={{"sun","sun"},{"partly","sun","cloud"},{"cloud","cloud"},{"fog","cloud","fog"},{"rain","cloud","rain"},{"snow","cloud","snow"},{"storm","cloud","lightning"},{"unknown","unknown"}};
        JSONObject given=input.optJSONObject("icons"),icons=out.getJSONObject("icons");if(given!=null)for(String[] type:types){JSONObject parts=colors(given.optJSONObject(type[0]),java.util.Arrays.copyOfRange(type,1,type.length));if(parts.length()>0)icons.put(type[0],parts);}return out;
    }
}
