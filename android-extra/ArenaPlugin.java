package __PACKAGE__;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * AI-Aréna natív része:
 *  - szöveg mentése a telefon Letöltések/AI-Arena mappájába (a böngészős blob-letöltés az Android WebView-ban nem működik),
 *  - webcím megnyitása az alapértelmezett böngészőben.
 */
@CapacitorPlugin(name = "ArenaNative")
public class ArenaPlugin extends Plugin {

    private static String msg(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    /** saveText({ name, text }) -> { path } */
    @PluginMethod
    public void saveText(PluginCall call) {
        try {
            String name = call.getString("name", "AI-Arena.txt");
            String text = call.getString("text", "");
            name = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            if (name.isEmpty()) name = "AI-Arena.txt";
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            String where;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: a MediaStore a Letöltések mappába írja a fájlt, engedély nélkül
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AI-Arena");
                ContentResolver resolver = getContext().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("A fájlt nem sikerült létrehozni a Letöltések mappában.");
                OutputStream os = resolver.openOutputStream(uri);
                if (os == null) throw new IOException("A fájl nem nyitható meg írásra.");
                try {
                    os.write(data);
                    os.flush();
                } finally {
                    os.close();
                }
                where = "Letöltések/AI-Arena/" + name;
            } else {
                // régebbi Android: az app saját külső mappájába (engedély nélkül)
                File dir = getContext().getExternalFilesDir(null);
                if (dir == null) throw new IOException("Nincs elérhető tárhely.");
                File out = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(out);
                try {
                    fos.write(data);
                    fos.flush();
                } finally {
                    fos.close();
                }
                where = out.getAbsolutePath();
            }

            JSObject ret = new JSObject();
            ret.put("path", where);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(msg(e));
        }
    }

    /** appendText({ name, text }) -> { path }  — a szöveget a meglévő fájl végéhez fűzi; ha még nincs ilyen fájl, létrehozza.
     *  Így az AI-Aréna havi gyűjtőfájlja nem íródik újra minden körben, és nem lesz belőle "fájl (1).txt" sem. */
    @PluginMethod
    public void appendText(PluginCall call) {
        try {
            String name = call.getString("name", "AI-Arena.txt");
            String text = call.getString("text", "");
            name = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            if (name.isEmpty()) name = "AI-Arena.txt";
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            String where;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = getContext().getContentResolver();
                Uri uri = null;

                // a Letöltések/AI-Arena mappában lévő, ilyen nevű fájl megkeresése
                Cursor c = null;
                try {
                    String[] cols = { MediaStore.MediaColumns._ID, MediaStore.MediaColumns.RELATIVE_PATH };
                    c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cols,
                            MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[] { name }, null);
                    while (c != null && c.moveToNext()) {
                        String rel = c.getString(1);
                        if (rel != null && rel.contains("AI-Arena")) {
                            uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0));
                            break;
                        }
                    }
                } catch (Exception e) {
                    uri = null;   // nem baj: ilyenkor új fájl készül
                } finally {
                    if (c != null) {
                        try { c.close(); } catch (Exception e2) {}
                    }
                }

                boolean done = false;
                if (uri != null) {
                    try {
                        OutputStream os = resolver.openOutputStream(uri, "wa");   // hozzáfűzés a fájl végéhez
                        if (os != null) {
                            try {
                                os.write(data);
                                os.flush();
                            } finally {
                                os.close();
                            }
                            done = true;
                        }
                    } catch (Exception e) {
                        done = false;   // a fájlt időközben törölték: újat hozunk létre
                    }
                }
                if (!done) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AI-Arena");
                    Uri nu = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (nu == null) throw new IOException("A fájlt nem sikerült létrehozni a Letöltések mappában.");
                    OutputStream os = resolver.openOutputStream(nu);
                    if (os == null) throw new IOException("A fájl nem nyitható meg írásra.");
                    try {
                        os.write(data);
                        os.flush();
                    } finally {
                        os.close();
                    }
                }
                where = "Letöltések/AI-Arena/" + name;
            } else {
                File dir = getContext().getExternalFilesDir(null);
                if (dir == null) throw new IOException("Nincs elérhető tárhely.");
                File out = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(out, true);   // hozzáfűzés
                try {
                    fos.write(data);
                    fos.flush();
                } finally {
                    fos.close();
                }
                where = out.getAbsolutePath();
            }

            JSObject ret = new JSObject();
            ret.put("path", where);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject(msg(e));
        }
    }

    /** Webcím megnyitása az Android alapértelmezett böngészőjében. */
    @PluginMethod
    public void openUrl(PluginCall call) {
        try {
            String url = call.getString("url", "").trim();
            String low = url.toLowerCase(Locale.ROOT);
            if (!(low.startsWith("http://") || low.startsWith("https://"))) {
                call.reject("Csak http/https cím nyitható meg: " + url);
                return;
            }
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (ActivityNotFoundException e) {
            call.reject("Nincs böngésző, amely megnyitná a címet");
        } catch (Exception e) {
            call.reject(msg(e));
        }
    }
}
