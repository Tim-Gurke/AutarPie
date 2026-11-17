package de.wattpit.app;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import java.io.*;

/** Disposable emulator-only documents provider. Never included in the production APK. */
public final class BackupDocumentsProvider extends DocumentsProvider {
    static final String AUTH="de.wattpit.app.test.backups";private volatile String fault="";
    @Override public boolean onCreate(){return true;}
    private File root(){File f=new File(getContext().getCacheDir(),"backup-provider-proof");f.mkdirs();return f;}
    private File file(String id)throws FileNotFoundException{if(id.equals("root"))return root();if(!id.matches("[A-Za-z0-9._-]{1,250}")||id.contains(".."))throw new FileNotFoundException();return new File(root(),id);}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(method.equals("grantProof")){Uri tree=DocumentsContract.buildTreeDocumentUri(AUTH,"root");getContext().grantUriPermission(arg,tree,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);Bundle b=new Bundle();b.putString("uri",tree.toString());return b;}
        if(method.equals("faultProof")){fault=arg==null?"":arg;return new Bundle();}return super.call(method,arg,extras);
    }
    @Override public Cursor queryRoots(String[] projection){String[] cols=projection==null?new String[]{DocumentsContract.Root.COLUMN_ROOT_ID,DocumentsContract.Root.COLUMN_DOCUMENT_ID,DocumentsContract.Root.COLUMN_TITLE,DocumentsContract.Root.COLUMN_FLAGS}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++){String k=cols[i];row[i]=k.equals(DocumentsContract.Root.COLUMN_FLAGS)?DocumentsContract.Root.FLAG_SUPPORTS_CREATE:k.equals(DocumentsContract.Root.COLUMN_TITLE)?"AutarPie test storage":"root";}c.addRow(row);return c;}
    private void add(MatrixCursor c,String id)throws FileNotFoundException{File f=file(id);Object[] row=new Object[c.getColumnCount()];for(int i=0;i<row.length;i++){String k=c.getColumnName(i);row[i]=k.equals(DocumentsContract.Document.COLUMN_DOCUMENT_ID)?id:k.equals(DocumentsContract.Document.COLUMN_DISPLAY_NAME)?(id.equals("root")?"Proof storage":id):k.equals(DocumentsContract.Document.COLUMN_MIME_TYPE)?(f.isDirectory()?DocumentsContract.Document.MIME_TYPE_DIR:"application/octet-stream"):k.equals(DocumentsContract.Document.COLUMN_FLAGS)?(f.isDirectory()?DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE:DocumentsContract.Document.FLAG_SUPPORTS_DELETE|DocumentsContract.Document.FLAG_SUPPORTS_RENAME|DocumentsContract.Document.FLAG_SUPPORTS_WRITE):k.equals(DocumentsContract.Document.COLUMN_SIZE)?f.length():k.equals(DocumentsContract.Document.COLUMN_LAST_MODIFIED)?f.lastModified():null;}c.addRow(row);}
    private MatrixCursor cursor(String[] p){return new MatrixCursor(p==null?new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_FLAGS,DocumentsContract.Document.COLUMN_SIZE}:p);}
    @Override public Cursor queryDocument(String id,String[] p)throws FileNotFoundException{MatrixCursor c=cursor(p);add(c,id);return c;}
    @Override public Cursor queryChildDocuments(String parent,String[] p,String sort)throws FileNotFoundException{if(!parent.equals("root"))throw new FileNotFoundException();MatrixCursor c=cursor(p);File[] files=root().listFiles();if(files!=null)for(File f:files)if(f.isFile())add(c,f.getName());return c;}
    @Override public boolean isChildDocument(String parent,String child){return parent.equals("root")&&!child.equals("root");}
    @Override public String createDocument(String parent,String mime,String name)throws FileNotFoundException{if(!parent.equals("root")||fault.equals("offline"))throw new FileNotFoundException("Proof unavailable");File f=file(name);try{if(!f.createNewFile())throw new IOException("Exists");return name;}catch(IOException e){throw new FileNotFoundException("Proof create failed");}}
    @Override public String renameDocument(String id,String name)throws FileNotFoundException{if(fault.equals("no-rename"))throw new UnsupportedOperationException("Proof provider has no rename");File f=file(id),to=file(name);if(to.exists()||!f.renameTo(to))throw new FileNotFoundException();return name;}
    @Override public void deleteDocument(String id)throws FileNotFoundException{if(id.equals("root")||!file(id).delete())throw new FileNotFoundException();}
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)throws FileNotFoundException{if(fault.equals("offline")||fault.equals("read-failure")&&!mode.contains("w"))throw new FileNotFoundException("Proof read failure");return ParcelFileDescriptor.open(file(id),ParcelFileDescriptor.parseMode(mode));}
}
