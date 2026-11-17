package app.tres.notes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import java.util.List;

public final class PriorityWidget extends AppWidgetProvider {
    private static final String COMPLETE = "app.tres.notes.COMPLETE";
    private static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        final PendingResult pending = goAsync();
        NoteStore.IO.execute(() -> {
            try { updateAll(context); } finally { pending.finish(); }
        });
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (!COMPLETE.equals(intent.getAction())) { super.onReceive(context, intent); return; }
        final PendingResult pending = goAsync();
        final String id = intent.getStringExtra("note_id");
        NoteStore.IO.execute(() -> {
            try {
                new NoteStore(context).change(book -> book.complete(id, System.currentTimeMillis()));
                updateAll(context);
            } catch (Exception error) { showError(context); }
            finally { pending.finish(); }
        });
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PriorityWidget.class));
        if (ids.length == 0) return;
        try {
            List<Note> notes = new NoteStore(context).read().priorities();
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.priority_widget);
            views.setOnClickPendingIntent(R.id.widget_title, open(context, false, null));
            views.setOnClickPendingIntent(R.id.widget_voice, open(context, true, null));
            int[] labels = {R.id.note_one, R.id.note_two, R.id.note_three};
            int[] checks = {R.id.check_one, R.id.check_two, R.id.check_three};
            for (int i = 0; i < 3; i++) {
                Note note = i < notes.size() ? notes.get(i) : null;
                views.setTextViewText(labels[i], note == null ? "Elegir prioridad" : note.text);
                views.setTextViewText(checks[i], note == null ? "+" : "○");
                views.setContentDescription(checks[i], note == null ? "Elegir prioridad" : "Completar " + note.text);
                views.setOnClickPendingIntent(labels[i], open(context, false, note == null ? null : note.id));
                PendingIntent action = open(context, false, null);
                if (note != null) {
                    Intent done = new Intent(context, PriorityWidget.class).setAction(COMPLETE)
                            .setData(Uri.parse("tres://complete/" + note.id)).putExtra("note_id", note.id);
                    action = PendingIntent.getBroadcast(context, 0, done, FLAGS);
                }
                views.setOnClickPendingIntent(checks[i], action);
            }
            manager.updateAppWidget(ids, views);
        } catch (Exception error) { showError(context); }
    }

    private static PendingIntent open(Context context, boolean voice, String id) {
        Intent intent = new Intent(context, MainActivity.class)
                .setData(Uri.parse("tres://" + (voice ? "voice" : "open") + "/" + (id == null ? "" : id)))
                .putExtra("voice", voice).putExtra("note_id", id)
                // Deliver to a running MainActivity via onNewIntent instead of only surfacing its task.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 0, intent, FLAGS);
    }

    private static void showError(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.priority_widget);
        views.setTextViewText(R.id.widget_title, "Abre Tres para revisar las notas");
        views.setOnClickPendingIntent(R.id.widget_title, open(context, false, null));
        for (int id : new int[]{R.id.check_one, R.id.check_two, R.id.check_three}) views.setViewVisibility(id, View.INVISIBLE);
        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, PriorityWidget.class), views);
    }
}
