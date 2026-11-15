package de.wattpit.app.core;

import java.util.*;

/** Outcomes for the complete queue; a failed second device cannot erase the first result. */
public final class ArchiveBatch {
    public static final class Result {
        public final String name,detail;public final boolean complete;public final int stored;
        Result(String name,boolean complete,int stored,String detail){this.name=name;this.complete=complete;this.stored=Math.max(0,stored);this.detail=detail;}
    }
    private final int expected;
    private final List<Result> results=new ArrayList<>();
    public ArchiveBatch(int expected){this.expected=Math.max(0,expected);}
    public void add(String name,boolean complete,int stored,String detail){results.add(new Result(name,complete,stored,detail));}
    public List<Result> results(){return Collections.unmodifiableList(results);}
    public int expected(){return expected;}
    public int completed(){int n=0;for(Result r:results)if(r.complete)n++;return n;}
    public long stored(){long n=0;for(Result r:results)n+=r.stored;return n;}
    public String summary(boolean cancelled,String reason){
        String prefix=cancelled?reason:completed()==expected?"Abgleich abgeschlossen":"Abgleich teilweise abgeschlossen";
        StringBuilder text=new StringBuilder(prefix).append(" · ").append(completed()).append(" von ").append(expected).append(" Geräten vollständig · ").append(stored()).append(" Werte insgesamt übernommen.");
        for(Result r:results){text.append(" ").append(r.name).append(": ");if(r.complete)text.append("vollständig · ").append(r.stored).append(" Werte.");else text.append(r.detail).append(" · ").append(r.stored).append(" Werte.");}
        if(results.size()<expected)text.append(" ").append(expected-results.size()).append(" Geräte noch nicht abgerufen.");
        return text.toString();
    }
}
