import app.tres.notes.Note;
import app.tres.notes.Notebook;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class NotebookTest {
    private static int passed;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected validation failure");
    }
    private static void test(String name, Runnable action) {
        action.run(); passed++; System.out.println("PASS " + name);
    }
    private static Notebook empty() { return new Notebook(Collections.emptyList()); }

    public static void main(String[] args) {
        test("Spanish and English preserved", () -> {
            Notebook b = empty();
            String es = b.add("  Llamar mañana a José.  ", 1);
            String en = b.add("Send the invoice. It's ready.", 2);
            check(b.find(es).text.equals("Llamar mañana a José."), "Spanish changed");
            check(b.find(en).text.equals("Send the invoice. It's ready."), "English changed");
        });
        test("Blank and oversized notes rejected", () -> {
            Notebook b = empty();
            for (String s : Arrays.asList(null, "", " \n\t", "a".repeat(20001))) rejects(() -> b.add(s, 1));
            check(b.all().isEmpty(), "Invalid notes saved");
            b.add("a".repeat(20000), 1);
        });
        test("Fourth priority rejected without mutation", () -> {
            Notebook b = empty();
            for (int i = 0; i < 3; i++) b.prioritize(b.add("Task " + i, i), i);
            String fourth = b.add("Fourth", 4);
            rejects(() -> b.prioritize(fourth, 5));
            check(b.priorities().size() == 3 && b.find(fourth).priority == 0, "Limit broken");
        });
        test("Repeated prioritization is idempotent", () -> {
            Notebook b = empty(); String id = b.add("Task", 1);
            b.prioritize(id, 2); b.prioritize(id, 3);
            check(b.priorities().size() == 1, "Duplicate priority");
        });
        test("Completion frees priority slot", () -> {
            Notebook b = empty(); List<String> ids = new ArrayList<>();
            for (int i = 0; i < 3; i++) { String id = b.add("Task " + i, 1); ids.add(id); b.prioritize(id, 2); }
            b.complete(ids.get(1), 3);
            String next = b.add("Next", 4); b.prioritize(next, 5);
            check(b.find(next).priority == 2 && b.priorities().size() == 3, "Slot not reused");
        });
        test("Duplicate widget delivery never restores a completed task", () -> {
            Notebook b = empty(); String id = b.add("Task", 1); b.prioritize(id, 2);
            b.complete(id, 3); b.complete(id, 4);
            check(b.find(id).done && b.find(id).updated == 3, "Duplicate delivery changed state");
            b.complete("stale-id", 5);
        });
        test("Restore does not displace current priorities", () -> {
            Notebook b = empty(); String done = b.add("Old task", 1); b.complete(done, 2);
            for (int i = 0; i < 3; i++) b.prioritize(b.add("Task " + i, 3), 3);
            b.restore(done, 4);
            check(!b.find(done).done && b.find(done).priority == 0 && b.priorities().size() == 3, "Restore broke priorities");
        });
        test("Completed notes cannot be pinned", () -> {
            Notebook b = empty(); String id = b.add("Task", 1); b.complete(id, 2);
            rejects(() -> b.prioritize(id, 3));
        });
        test("Editing preserves identity, creation and priority", () -> {
            Notebook b = empty(); String id = b.add("Draft", 1); b.prioritize(id, 2); b.edit(id, "Revised", 3);
            Note n = b.find(id);
            check(n.text.equals("Revised") && n.created == 1 && n.updated == 3 && n.priority == 1, "Edit lost state");
        });
        test("Invalid edit preserves previous content", () -> {
            Notebook b = empty(); String id = b.add("Keep me", 1); rejects(() -> b.edit(id, "  ", 2));
            check(b.find(id).text.equals("Keep me"), "Invalid edit mutated note");
        });
        test("Voice capture retries use durable identity", () -> {
            Notebook b = empty(); b.addCapture("capture-1", "Call tomorrow", 1); b.addCapture("capture-1", "Call tomorrow", 2);
            check(b.all().size() == 1 && b.find("capture-1").created == 1, "Capture duplicated");
            b.addCapture("capture-2", "Call tomorrow", 3);
            check(b.all().size() == 2, "Distinct intentional capture lost");
        });
        test("Unpin keeps note active", () -> {
            Notebook b = empty(); String id = b.add("Task", 1); b.prioritize(id, 2); b.unpin(id, 3);
            check(!b.find(id).done && b.find(id).priority == 0, "Unpin lost task");
        });
        test("Priorities survive day change and reload", () -> {
            Notebook b = empty(); String id = b.add("Tomorrow", 1); b.prioritize(id, 2);
            Notebook reloaded = new Notebook(b.all());
            check(reloaded.priorities().size() == 1 && reloaded.find(id).text.equals("Tomorrow"), "Reload lost priorities");
        });
        test("Snapshots cannot mutate notebook", () -> {
            Notebook b = empty(); List<Note> before = b.all(); b.add("New", 1);
            check(before.isEmpty(), "Snapshot changed later");
            try { b.all().clear(); throw new AssertionError("Mutable snapshot"); } catch (UnsupportedOperationException expected) {}
        });
        test("Malformed stored priorities rejected", () -> {
            rejects(() -> new Notebook(Arrays.asList(new Note("1", "A", 1, 1, false, 1), new Note("2", "B", 1, 1, false, 1))));
            rejects(() -> new Notebook(Collections.singletonList(new Note("1", "A", 1, 1, true, 1))));
            rejects(() -> new Notebook(Collections.singletonList(new Note("1", "A", 1, 1, false, 4))));
        });
        test("Duplicate stored ids rejected", () -> {
            rejects(() -> new Notebook(Arrays.asList(new Note("1", "A", 1, 1, false, 0), new Note("1", "B", 1, 1, false, 0))));
        });
        test("Large note collection keeps exact priorities", () -> {
            Notebook b = empty();
            for (int i = 0; i < 5000; i++) b.addCapture("note-" + i, "Pending " + i, i);
            b.prioritize("note-4999", 5001); b.prioritize("note-2500", 5002); b.prioritize("note-0", 5003);
            check(b.all().size() == 5000 && b.priorities().get(0).id.equals("note-4999"), "Collection corruption");
        });
        System.out.println("PASSED " + passed + " domain regression tests");
    }
}
