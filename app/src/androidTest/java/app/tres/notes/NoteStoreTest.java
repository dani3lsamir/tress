package app.tres.notes;

import android.test.AndroidTestCase;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Device tests exercise Android AtomicFile and real JSON persistence. */
public final class NoteStoreTest extends AndroidTestCase {
    private File file;
    private NoteStore store;

    @Override protected void setUp() throws Exception {
        super.setUp();
        file = new File(getContext().getCacheDir(), "test-" + UUID.randomUUID() + ".json");
        store = new NoteStore(file);
    }

    @Override protected void tearDown() throws Exception {
        file.delete();
        new File(file + ".new").delete();
        new File(file + ".bak").delete();
        super.tearDown();
    }

    public void testUnicodeRoundTripAndRestart() throws Exception {
        store.change(b -> { b.addCapture("one", "Mañana llamar a José 👋", 1); b.prioritize("one", 2); });
        Notebook loaded = new NoteStore(file).read();
        assertEquals("Mañana llamar a José 👋", loaded.find("one").text);
        assertEquals(1, loaded.find("one").priority);
    }

    public void testFailedMutationDoesNotOverwrite() throws Exception {
        store.change(b -> b.addCapture("one", "Keep", 1));
        byte[] before = Files.readAllBytes(file.toPath());
        try { store.change(b -> { b.add("Unsaved", 2); throw new IllegalArgumentException("failure"); }); fail(); }
        catch (IllegalArgumentException expected) { }
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(file.toPath())));
    }

    public void testCorruptDataNeverReplacedByEmptyNotebook() throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) { output.write("{broken".getBytes(StandardCharsets.UTF_8)); }
        try { store.change(b -> b.add("Overwrite", 2)); fail(); }
        catch (java.io.IOException expected) { }
        assertEquals("{broken", new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    public void testConcurrentWritersDoNotLoseNotes() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(8);
        AtomicReference<Throwable> error = new AtomicReference<>();
        for (int i = 0; i < 8; i++) {
            final String id = "thread-" + i;
            new Thread(() -> {
                try { start.await(); new NoteStore(file).change(b -> b.addCapture(id, "Task " + id, 1)); }
                catch (Throwable failure) { error.compareAndSet(null, failure); }
                finally { finish.countDown(); }
            }).start();
        }
        start.countDown();
        assertTrue(finish.await(10, java.util.concurrent.TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(8, store.read().all().size());
    }

    public void testInterruptedNewFilePreservesCommittedNotes() throws Exception {
        store.change(b -> b.addCapture("one", "Keep", 1));
        try (FileOutputStream output = new FileOutputStream(file + ".new")) { output.write("partial".getBytes(StandardCharsets.UTF_8)); }
        assertEquals("Keep", new NoteStore(file).read().find("one").text);
    }

    public void testCaptureRetryAfterRestartIsIdempotent() throws Exception {
        store.change(b -> b.addCapture("voice", "Dictado", 1));
        new NoteStore(file).change(b -> b.addCapture("voice", "Dictado", 2));
        assertEquals(1, store.read().all().size());
    }
}
