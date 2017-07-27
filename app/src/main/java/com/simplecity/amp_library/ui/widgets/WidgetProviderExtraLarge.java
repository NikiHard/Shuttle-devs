package com.simplecity.amp_library.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.utils.ColorUtils;
import com.simplecity.amp_library.utils.DrawableUtils;

public class WidgetProviderExtraLarge extends BaseWidgetProvider {

    private static final String TAG = "MusicAppWidgetProvider";

    public static final String ARG_EXTRA_LARGE_LAYOUT_ID = "widget_extra_large_layout_id_";

    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate_extra_large";

    @Override
    public String getUpdateCommandString() {
        return CMDAPPWIDGETUPDATE;
    }

    @Override
    public String getLayoutIdString() {
        return ARG_EXTRA_LARGE_LAYOUT_ID;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.widget_layout_extra_large;
    }

    @Override
    public int getRootViewId() {
        return R.id.widget_layout_extra_large;
    }

    private static WidgetProviderExtraLarge sInstance;

    public static synchronized WidgetProviderExtraLarge getInstance() {
        if (sInstance == null) {
            sInstance = new WidgetProviderExtraLarge();
        }
        return sInstance;
    }

    @Override
    protected void initialiseWidget(Context context, int appWidgetId) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), mLayoutId);

        views.setViewVisibility(R.id.text1, View.GONE);
        views.setTextViewText(R.id.text2, res.getText(R.string.widget_initial_text));

        int textColor = mPrefs.getInt(ARG_WIDGET_TEXT_COLOR + appWidgetId, context.getResources().getColor(R.color.white));
        views.setImageViewResource(R.id.next_button, R.drawable.ic_skip_next_24dp);
        views.setImageViewResource(R.id.prev_button, R.drawable.ic_skip_previous_24dp);
        views.setTextColor(R.id.text2, textColor);
        views.setTextColor(R.id.text1, textColor);

        int backgroundColor = mPrefs.getInt(ARG_WIDGET_BACKGROUND_COLOR + appWidgetId, ColorUtils.adjustAlpha(context.getResources().getColor(R.color.white), 35 / 255f));
        views.setInt(R.id.widget_layout_extra_large, "setBackgroundColor", backgroundColor);
        int colorFilter = mPrefs.getInt(ARG_WIDGET_COLOR_FILTER + appWidgetId, -1);
        if (colorFilter != -1) {
            views.setInt(R.id.album_art, "setColorFilter", colorFilter);
        }
        boolean showAlbumArt = mPrefs.getBoolean(ARG_WIDGET_SHOW_ARTWORK + appWidgetId, true);
        if (!showAlbumArt) {
            views.setViewVisibility(R.id.album_art, View.GONE);
        }

        setupButtons(context, views, appWidgetId, getRootViewId());
        pushUpdate(context, appWidgetId, views);
    }

    @Override
    public void update(MusicService service, int[] appWidgetIds, boolean updateArtwork) {

        if (appWidgetIds == null) {
            return;
        }

        for (int appWidgetId : appWidgetIds) {

            boolean showAlbumArt = mPrefs.getBoolean(ARG_WIDGET_SHOW_ARTWORK + appWidgetId, true);

            mLayoutId = mPrefs.getInt(ARG_EXTRA_LARGE_LAYOUT_ID + appWidgetId, R.layout.widget_layout_extra_large);

            final Resources res = service.getResources();
            final RemoteViews views = new RemoteViews(service.getPackageName(), mLayoutId);

            CharSequence titleName = service.getSongName();
            CharSequence albumName = service.getAlbumName();
            CharSequence artistName = service.getAlbumArtistName();
            CharSequence errorState = null;

            // Format title string with track number, or show SD card message
            String status = Environment.getExternalStorageState();
            if (status.equals(Environment.MEDIA_SHARED) || status.equals(Environment.MEDIA_UNMOUNTED)) {
                if (android.os.Environment.isExternalStorageRemovable()) {
                    errorState = res.getText(R.string.sdcard_busy_title);
                } else {
                    errorState = res.getText(R.string.sdcard_busy_title_nosdcard);
                }
            } else if (status.equals(Environment.MEDIA_REMOVED)) {
                if (android.os.Environment.isExternalStorageRemovable()) {
                    errorState = res.getText(R.string.sdcard_missing_title);
                } else {
                    errorState = res.getText(R.string.sdcard_missing_title_nosdcard);
                }
            } else if (titleName == null) {
                errorState = res.getText(R.string.emptyplaylist);
            }

            if (errorState != null) {
                // Show error state to user
                views.setViewVisibility(R.id.text1, View.GONE);
                views.setTextViewText(R.id.text2, errorState);

            } else {
                // No error, so show normal titles
                views.setViewVisibility(R.id.text1, View.VISIBLE);
                views.setTextViewText(R.id.text1, titleName);
                views.setTextViewText(R.id.text2, artistName + " | " + albumName);
            }

            boolean invertIcons = mPrefs.getBoolean(ARG_WIDGET_INVERT_ICONS + appWidgetId, false);

            // Set correct drawable for pause state
            final boolean isPlaying = service.isPlaying();
            if (isPlaying) {
                if (invertIcons) {
                    views.setImageViewBitmap(R.id.play_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_pause_24dp));
                } else {
                    views.setImageViewResource(R.id.play_button, R.drawable.ic_pause_24dp);
                }
            } else {
                if (invertIcons) {
                    views.setImageViewBitmap(R.id.play_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_play_24dp));
                } else {
                    views.setImageViewResource(R.id.play_button, R.drawable.ic_play_24dp);
                }
            }

            setupShuffleView(service, views, invertIcons);

            setupRepeatView(service, views, invertIcons);

            int textColor = mPrefs.getInt(ARG_WIDGET_TEXT_COLOR + appWidgetId, service.getResources().getColor(R.color.white));
            if (invertIcons) {
                views.setImageViewBitmap(R.id.next_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_skip_next_24dp));
                views.setImageViewBitmap(R.id.prev_button, DrawableUtils.getBlackBitmap(service, R.drawable.ic_skip_previous_24dp));
            } else {
                views.setImageViewResource(R.id.next_button, R.drawable.ic_skip_next_24dp);
                views.setImageViewResource(R.id.prev_button, R.drawable.ic_skip_previous_24dp);
            }

            views.setTextColor(R.id.text2, textColor);
            views.setTextColor(R.id.text1, textColor);

            int backgroundColor = mPrefs.getInt(ARG_WIDGET_BACKGROUND_COLOR + appWidgetId, ColorUtils.adjustAlpha(service.getResources().getColor(R.color.white), 35 / 255f));
            views.setInt(R.id.widget_layout_extra_large, "setBackgroundColor", backgroundColor);

            setupButtons(service, views, appWidgetId, getRootViewId());

            if (!showAlbumArt) {
                views.setViewVisibility(R.id.album_art, View.GONE);
            }

            pushUpdate(service, appWidgetId, views);

            if (updateArtwork && errorState == null && showAlbumArt) {

                views.setImageViewResource(R.id.album_art, R.drawable.ic_placeholder_light_large);

                doOnMainThread(() -> loadArtwork(service, appWidgetIds, views, 1024));
            }
        }
    }

}