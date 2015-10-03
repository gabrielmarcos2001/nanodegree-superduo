package barqsoft.footballscores.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.List;

import barqsoft.footballscores.R;

/**
 * Created by gabrielmarcos on 10/3/15.
 */
public class WidgetViewFactory implements RemoteViewsService.RemoteViewsFactory {

    private List<Cursor> items;

    private Context ctx;
    private int appWidgetId;

    public WidgetViewFactory(Context ctx, Intent intent) {

        this.ctx=ctx;

        appWidgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews row=new RemoteViews(ctx.getPackageName(),
                R.layout.scores_list_item);

        //row.setTextViewText(android.R.id.text1, items[position]);

        Intent i = new Intent();
        Bundle extras = new Bundle();

        //extras.putString(WidgetProvider.EXTRA_WORD, items[position]);
        i.putExtras(extras);
        row.setOnClickFillInIntent(android.R.id.text1, i);

        return(row);
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
