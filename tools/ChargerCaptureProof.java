package de.wattpit.app;

import de.wattpit.app.core.ChargerCapture;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Sanitized HCI records only: a capture must belong to the profile being edited. */
public final class ChargerCaptureProof {
    static int checks;static final String A="00:00:00:00:00:11",B="00:00:00:00:00:12";
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;System.out.println("PASS "+message);}
    static void record(DataOutputStream out,int flags,byte[] bytes)throws Exception{out.writeInt(bytes.length);out.writeInt(bytes.length);out.writeInt(flags);out.writeInt(0);out.writeLong(0);out.write(bytes);}
    static byte[] connection(int handle,String mac){byte[] bytes={4,0x3e,19,1,0,(byte)handle,(byte)(handle>>8),0,1,0,0,0,0,0,0,0,0,0,0,0,0,0};String[] parts=mac.split(":");for(int i=0;i<6;i++)bytes[9+i]=(byte)Integer.parseInt(parts[5-i],16);return bytes;}
    static byte[] acl(int handle,byte[] att){int size=att.length+4;byte[] bytes=new byte[size+5];bytes[0]=2;bytes[1]=(byte)handle;bytes[2]=(byte)(0x20|(handle>>8));bytes[3]=(byte)size;bytes[4]=(byte)(size>>8);bytes[5]=(byte)att.length;bytes[6]=(byte)(att.length>>8);bytes[7]=4;System.arraycopy(att,0,bytes,9,att.length);return bytes;}
    static byte[] key(int first){byte[] key=new byte[16];for(int i=0;i<16;i++)key[i]=(byte)(first+i);return key;}
    static void bind(DataOutputStream out,int handle,byte[] key)throws Exception{record(out,1,acl(handle,new byte[]{9,7,1,0,0x1a,0x24,0,2,(byte)0xaf}));byte[] att=new byte[22];att[0]=0x52;att[1]=0x24;att[3]=0x18;System.arraycopy(key,0,att,4,16);record(out,0,acl(handle,att));}
    static byte[] capture(boolean addresses,boolean both)throws Exception{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.write("btsnoop\0".getBytes("US-ASCII"));out.writeInt(1);out.writeInt(1002);if(addresses)record(out,1,connection(7,A));bind(out,7,key(0));if(both){record(out,1,new byte[]{4,5,4,0,7,0,19});if(addresses)record(out,1,connection(7,B));bind(out,7,key(40));}return bytes.toByteArray();}
    static byte[] read(byte[] capture,String mac)throws Exception{return ChargerCapture.read(new ByteArrayInputStream(capture),mac);}
    public static void main(String[] args)throws Exception{
        byte[] both=capture(true,true);
        check(Arrays.equals(key(0),read(both,A)),"a reused ACL handle cannot import the second charger's identity into the first profile");
        check(Arrays.equals(key(40),read(both,B)),"the selected second profile receives only its own identity");
        check(Arrays.equals(key(0),read(capture(true,false),A.toLowerCase(Locale.ROOT))),"profile address matching accepts normalized casing");
        try{read(both,"00:00:00:00:00:13");throw new AssertionError("Unknown charger accepted");}catch(IOException expected){check(expected.getMessage().contains("zugeordnete"),"a capture of another charger leaves the selected profile untouched");}
        try{read(capture(false,false),A);throw new AssertionError("Unmapped handle accepted");}catch(IOException expected){check(true,"a capture without a connection-address mapping cannot overwrite a named profile");}
        check(Arrays.equals(key(0),ChargerCapture.read(new ByteArrayInputStream(capture(false,false)))),"the existing unfiltered reader remains compatible with old single-device captures");
        try{ChargerCapture.read(new ByteArrayInputStream(both));throw new AssertionError("Ambiguous legacy import accepted");}catch(IOException expected){check(expected.getMessage().contains("Mehrere"),"legacy import rejects multiple different identities");}
        ByteArrayOutputStream zipped=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(zipped)){zip.putNextEntry(new ZipEntry("FS/data/misc/bluetooth/logs/btsnoop_hci.log"));zip.write(both);zip.closeEntry();}
        check(Arrays.equals(key(40),read(zipped.toByteArray(),B)),"the same profile filtering applies inside an Android bugreport ZIP");
        System.out.println("RESULT "+checks+" charger capture checks");
    }
}
