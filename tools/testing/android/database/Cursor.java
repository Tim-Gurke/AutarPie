package android.database;

/** Minimal JVM storage-test surface; avoids initializing Android's ContentResolver. */
public interface Cursor extends AutoCloseable {
    int FIELD_TYPE_NULL=0,FIELD_TYPE_INTEGER=1,FIELD_TYPE_FLOAT=2,FIELD_TYPE_STRING=3,FIELD_TYPE_BLOB=4;
    int getColumnCount();String getColumnName(int column);int getType(int column);byte[] getBlob(int column);
    boolean moveToFirst();
    boolean moveToNext();
    boolean isNull(int column);
    long getLong(int column);
    int getInt(int column);
    double getDouble(int column);
    String getString(int column);
    int getColumnIndexOrThrow(String name);
    void close();
}
