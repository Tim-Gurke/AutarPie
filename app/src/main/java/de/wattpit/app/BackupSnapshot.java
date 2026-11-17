package de.wattpit.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import java.io.*;
import java.util.*;

/** Native online snapshot when supported; bounded legacy copier for older SQLite. */
final class BackupSnapshot {
    private static String q(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    static void copy(File source,File target)throws Exception{
        if(target.exists())throw new IOException("Zwischenstand bereits vorhanden.");
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(source.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
            String version;try(Cursor c=db.rawQuery("SELECT sqlite_version()",null)){if(!c.moveToFirst())throw new IOException("SQLite-Version fehlt.");version=c.getString(0);}
            String[] parts=version.split("\\.");int major=Integer.parseInt(parts[0]),minor=Integer.parseInt(parts[1]);
            if(major>3||major==3&&minor>=27){check();db.execSQL("VACUUM INTO ?",new Object[]{target.getAbsolutePath()});try(SQLiteDatabase verify=SQLiteDatabase.openDatabase(target.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);Cursor c=verify.rawQuery("PRAGMA quick_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0)))throw new IOException("Datenbankprüfung fehlgeschlagen.");}return;}
        }
        copyPreservingRowids(source,target);
    }
    static void copyPreservingRowids(File source,File target)throws Exception{
        if(target.exists())throw new IOException("Zwischenstand bereits vorhanden.");
        try(SQLiteDatabase in=SQLiteDatabase.openDatabase(source.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.NO_LOCALIZED_COLLATORS|SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
            SQLiteDatabase out=SQLiteDatabase.openDatabase(target.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.CREATE_IF_NECESSARY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
            in.beginTransactionNonExclusive();try{
                List<String[]> tables=new ArrayList<>(),schema=new ArrayList<>();
                try(Cursor c=in.rawQuery("SELECT type,name,sql FROM sqlite_master WHERE sql IS NOT NULL ORDER BY CASE type WHEN 'table' THEN 0 WHEN 'index' THEN 1 WHEN 'view' THEN 2 ELSE 3 END,name",null)){
                    while(c.moveToNext()){String type=c.getString(0),name=c.getString(1),sql=c.getString(2);if(name.startsWith("sqlite_"))continue;if(type.equals("table")){if(sql.toUpperCase(Locale.ROOT).contains("VIRTUAL TABLE"))throw new IOException("Nicht unterstützte Datenbanktabelle.");tables.add(new String[]{name,sql});}else schema.add(new String[]{sql});}
                }
                out.beginTransaction();try{
                    for(String[] t:tables){check();out.execSQL(t[1]);copyRows(in,out,t[0],!t[1].toUpperCase(Locale.ROOT).matches("(?s).*WITHOUT\\s+ROWID\\s*;?\\s*$"));}
                    try(Cursor c=in.rawQuery("SELECT 1 FROM sqlite_master WHERE name='sqlite_sequence'",null)){if(c.moveToFirst()){out.delete("sqlite_sequence",null,null);copyRows(in,out,"sqlite_sequence",true);}}
                    for(String[] s:schema){check();out.execSQL(s[0]);}
                    for(String pragma:new String[]{"user_version","application_id"})try(Cursor c=in.rawQuery("PRAGMA "+pragma,null)){if(c.moveToFirst())out.execSQL("PRAGMA "+pragma+"="+c.getLong(0));}
                    out.setTransactionSuccessful();
                }finally{out.endTransaction();}in.setTransactionSuccessful();
            }finally{in.endTransaction();}
            try(Cursor c=out.rawQuery("PRAGMA quick_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0)))throw new IOException("Datenbankprüfung fehlgeschlagen.");}
        }
    }
    private static void copyRows(SQLiteDatabase in,SQLiteDatabase out,String table,boolean rowid)throws Exception{
        boolean first=true;long after=0;while(true){check();int n=0;String sql="SELECT "+(rowid?"_rowid_,":"")+"* FROM "+q(table)+(rowid&&!first?" WHERE _rowid_>?":"")+(rowid?" ORDER BY _rowid_ LIMIT 512":"");
            try(Cursor c=in.rawQuery(sql,rowid&&!first?new String[]{""+after}:null)){
                while(c.moveToNext()){ContentValues values=new ContentValues();for(int i=0;i<c.getColumnCount();i++){String name=q(rowid&&i==0?"_rowid_":c.getColumnName(i));switch(c.getType(i)){case Cursor.FIELD_TYPE_NULL:values.putNull(name);break;case Cursor.FIELD_TYPE_INTEGER:values.put(name,c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:values.put(name,c.getDouble(i));break;case Cursor.FIELD_TYPE_BLOB:values.put(name,c.getBlob(i));break;default:values.put(name,c.getString(i));}}
                    out.insertOrThrow(q(table),null,values);if(rowid)after=c.getLong(0);n++;
                }
            }if(!rowid||n<512)break;first=false;
        }
    }
    private static void check()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Sicherung unterbrochen.");}
    private BackupSnapshot(){}
}
