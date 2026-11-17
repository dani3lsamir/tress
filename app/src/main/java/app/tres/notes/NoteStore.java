package app.tres.notes;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Every read-modify-write runs in the same process and under the same lock. */
public final class NoteStore {
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Object LOCK = new Object();
    private final AtomicFile file;
    public interface Change { void apply(Notebook notebook); }

    public NoteStore(Context context) { this(new File(context.getFilesDir(), "notes.json")); }
    public NoteStore(File path) { file = new AtomicFile(path); }

    public Notebook read() throws IOException {
        synchronized (LOCK) {
            Notebook book = load();
            if (book.cleanup(System.currentTimeMillis())) {
                // Expiry is housekeeping: a full disk must not hide readable notes. Retry on the next change.
                try { persist(book); } catch (IOException ignored) { }
            }
            return book;
        }
    }

    public Notebook change(Change change) throws IOException {
        synchronized (LOCK) {
            Notebook book = load();
            book.cleanup(System.currentTimeMillis());
            change.apply(book);
            persist(book);
            return book;
        }
    }

    private Notebook load() throws IOException {
        byte[] bytes;
        try { bytes = file.readFully(); }
        catch (FileNotFoundException error) {
            if (file.getBaseFile().exists()) throw error;
            return new Notebook(Collections.emptyList());
        }
        try {
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (root.getInt("version") != 1) throw new IOException("Versión de notas incompatible.");
            JSONArray rows = root.getJSONArray("notes");
            List<Note> notes = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject n = rows.getJSONObject(i);
                notes.add(new Note(n.getString("id"), n.getString("text"), n.getLong("created"),
                        n.getLong("updated"), n.getBoolean("done"), n.getInt("priority"),n.optString("due",""),n.optString("repeat","none"),n.optLong("completedAt",n.getBoolean("done")?n.getLong("updated"):0),n.optInt("anchorDay",0),n.optString("nextId","")));
            }
            return new Notebook(notes);
        } catch (Exception error) {
            throw new IOException("No se pudieron leer las notas. Se conserva el archivo original.", error);
        }
    }

    private void persist(Notebook book) throws IOException {
        final byte[] bytes;
        try {
            JSONArray rows = new JSONArray();
            for (Note n : book.all()) rows.put(new JSONObject().put("id", n.id).put("text", n.text)
                    .put("created", n.created).put("updated", n.updated).put("done", n.done)
                    .put("priority", n.priority).put("due",n.due).put("repeat",n.repeat).put("completedAt",n.completedAt).put("anchorDay",n.anchorDay).put("nextId",n.nextId));
            bytes = new JSONObject().put("version", 1).put("notes", rows).toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception error) { throw new IOException("No se pudieron preparar las notas.", error); }
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(bytes);
            file.finishWrite(output);
        } catch (IOException error) {
            file.failWrite(output);
            throw error;
        }
    }
}
