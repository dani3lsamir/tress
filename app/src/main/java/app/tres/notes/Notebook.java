package app.tres.notes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.stream.Collectors;

/** Pure domain logic, also exercised by the standalone JVM regression suite. */
public final class Notebook {
    public static final int MAX_TEXT = 20000;
    public static final long DAY_MS = 86_400_000L;
    public static final long RETENTION_MS = 10 * DAY_MS;
    public static final List<String> REPEATS = Collections.unmodifiableList(Arrays.asList("none", "daily", "weekly", "monthly"));
    private final List<Note> notes;

    public Notebook(List<Note> source) {
        notes = new ArrayList<>(source);
        Set<String> ids = new HashSet<>();
        Set<Integer> slots = new HashSet<>();
        for (Note note : notes) {
            if (note.id == null || note.id.trim().isEmpty() || !ids.add(note.id))
                throw new IllegalArgumentException("Identificador de nota inválido.");
            validText(note.text);
            validateSchedule(note.due,note.repeat);
            if (note.anchorDay < 0 || note.anchorDay > 31 || note.nextId == null)
                throw new IllegalArgumentException("Repetición inválida.");
            if (note.priority < 0 || note.priority > 3 || (note.done && note.priority != 0)
                    || (note.priority > 0 && !slots.add(note.priority)))
                throw new IllegalArgumentException("Prioridades inválidas.");
        }
    }

    public List<Note> all() { return Collections.unmodifiableList(new ArrayList<>(notes)); }

    public List<Note> priorities() {
        return notes.stream().filter(n -> n.priority > 0)
                .sorted(Comparator.comparingInt(n -> n.priority)).collect(Collectors.toList());
    }

    public Note find(String id) {
        return notes.stream().filter(n -> n.id.equals(id)).findFirst().orElse(null);
    }

    public String add(String text, long now) {
        String id = UUID.randomUUID().toString();
        addCapture(id, text, now);
        return id;
    }

    public void addCapture(String id, String text, long now) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Identificador de captura inválido.");
        if (find(id) != null) return;
        notes.add(new Note(id, validText(text), now, now, false, 0));
    }

    public void edit(String id, String text, long now) {
        Note old = required(id);
        replace(old.copy(validText(text),now,old.done,old.priority));
    }

    public void prioritize(String id, long now) {
        Note old = required(id);
        if (old.done) throw new IllegalArgumentException("Restaura la nota antes de priorizarla.");
        if (old.priority > 0) return;
        Set<Integer> used = new HashSet<>();
        for (Note note : notes) used.add(note.priority);
        int slot = 1;
        while (slot <= 3 && used.contains(slot)) slot++;
        if (slot > 3) throw new IllegalArgumentException("Ya tienes tres prioridades. Libera una primero.");
        replace(old.copy(old.text,now,false,slot));
    }

    public void unpin(String id, long now) {
        Note old = required(id);
        replace(old.copy(old.text,now,old.done,0));
    }

    /** Set a target state, making repeated widget delivery harmless. */
    public void complete(String id, long now) {
        Note old = find(id);
        if (old == null || old.done) return;
        String nextId=old.nextId;
        if (!old.repeat.equals("none") && nextId.isEmpty()) {
            nextId = UUID.randomUUID().toString();
            LocalDate current = LocalDate.parse(old.due);
            int anchor = old.anchorDay > 0 ? old.anchorDay : current.getDayOfMonth();
            notes.add(new Note(nextId, old.text, now, now, false, 0, nextDue(current, old.repeat, anchor).toString(), old.repeat, 0, anchor, ""));
        }
        replace(new Note(id,old.text,old.created,now,true,0,old.due,old.repeat,now,old.anchorDay,nextId));
    }

    public void restore(String id, long now) {
        Note old = required(id);
        replace(new Note(id,old.text,old.created,now,false,0,old.due,old.repeat,0,old.anchorDay,old.nextId));
    }

    /** Next occurrence; monthly schedules keep the anchor day, clamped to shorter months. */
    static LocalDate nextDue(LocalDate current, String repeat, int anchor) {
        switch (repeat) {
            case "daily": return current.plusDays(1);
            case "weekly": return current.plusWeeks(1);
            case "monthly": {
                LocalDate month = current.withDayOfMonth(1).plusMonths(1);
                return month.withDayOfMonth(Math.min(anchor, month.lengthOfMonth()));
            }
            default: throw new IllegalArgumentException("Repetición inválida.");
        }
    }

    public static void validateSchedule(String due, String repeat) {
        if (due == null || repeat == null || !REPEATS.contains(repeat)) throw new IllegalArgumentException("Repetición inválida.");
        if (!due.isEmpty()) {
            try {
                if (!due.matches("\\d{4}-\\d{2}-\\d{2}") || !LocalDate.parse(due).toString().equals(due)) throw new IllegalArgumentException();
            } catch (RuntimeException error) { throw new IllegalArgumentException("Fecha inválida."); }
        }
        if (!repeat.equals("none") && due.isEmpty()) throw new IllegalArgumentException("Elige una fecha para repetir.");
    }

    public void schedule(String id, String due, String repeat, long now) {
        validateSchedule(due, repeat);
        Note n = required(id);
        // Saving an unchanged schedule must keep the monthly anchor (e.g. 31 while due on Feb 28).
        if (n.due.equals(due) && n.repeat.equals(repeat)) return;
        int anchor = due.isEmpty() ? 0 : LocalDate.parse(due).getDayOfMonth();
        replace(new Note(id, n.text, n.created, now, n.done, n.priority, due, repeat, n.completedAt, anchor, n.nextId));
    }

    public boolean cleanup(long now) { return notes.removeIf(n -> n.done && now - n.completedAt >= RETENTION_MS); }

    private Note required(String id) {
        Note note = find(id);
        if (note == null) throw new IllegalArgumentException("La nota ya no está disponible.");
        return note;
    }

    private void replace(Note replacement) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id.equals(replacement.id)) { notes.set(i, replacement); return; }
        }
    }

    public static String validText(String text) {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("Añade algo a la nota.");
        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("La nota supera los 20.000 caracteres.");
        return text.trim();
    }
}
