package de.wattpit.app;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;
/** Test APK's own UID grants its own fixture tree; production code still uses the system picker. */
public final class BackupGrantActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);grantUriPermission("de.wattpit.app",DocumentsContract.buildTreeDocumentUri("de.wattpit.app.test.backups","root"),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);finish();}
}
