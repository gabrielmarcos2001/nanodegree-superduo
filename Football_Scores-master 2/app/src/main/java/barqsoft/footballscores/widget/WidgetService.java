package barqsoft.footballscores.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by gabrielmarcos on 10/3/15.
 */
public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return(new WidgetViewFactory(this.getApplicationContext(),
                intent));
    }
}
