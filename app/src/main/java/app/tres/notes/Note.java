package app.tres.notes;

public final class Note {
    public final String id;
    public final String text;
    public final long created;
    public final long updated;
    public final boolean done;
    public final int priority;

    public final String due, repeat, nextId;
    public final long completedAt;
    public final int anchorDay;

    public Note(String id, String text, long created, long updated, boolean done, int priority) {
        this(id,text,created,updated,done,priority,"","none",done?updated:0,0,"");
    }
    public Note(String id, String text, long created, long updated, boolean done, int priority, String due, String repeat, long completedAt, int anchorDay, String nextId) {
        this.due=due;this.repeat=repeat;this.completedAt=completedAt;this.anchorDay=anchorDay;this.nextId=nextId;
        this.id = id;
        this.text = text;
        this.created = created;
        this.updated = updated;
        this.done = done;
        this.priority = priority;
    }
    public Note copy(String text,long updated,boolean done,int priority) {
        return new Note(id,text,created,updated,done,priority,due,repeat,completedAt,anchorDay,nextId);
    }
}

