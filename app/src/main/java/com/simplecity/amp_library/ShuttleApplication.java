package com.simplecity.amp_library;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.simplecity.amp_library.constants.Config;
import com.simplecity.amp_library.dagger.component.AppComponent;
import com.simplecity.amp_library.dagger.component.DaggerAppComponent;
import com.simplecity.amp_library.dagger.module.AppModule;
import com.simplecity.amp_library.model.Genre;
import com.simplecity.amp_library.model.Query;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.model.UserSelectedArtwork;
import com.simplecity.amp_library.services.EqualizerService;
import com.simplecity.amp_library.sql.SqlUtils;
import com.simplecity.amp_library.sql.databases.CustomArtworkTable;
import com.simplecity.amp_library.sql.providers.PlayCountTable;
import com.simplecity.amp_library.sql.sqlbrite.SqlBriteUtils;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.SettingsManager;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.jaudiotagger.tag.TagOptionSingleton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.fabric.sdk.android.Fabric;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class ShuttleApplication extends Application {

    private static ShuttleApplication instance;

    public static synchronized ShuttleApplication getInstance() {
        return instance;
    }

    private static final String TAG = "ShuttleApplication";

    private boolean isUpgraded;

    public static final double VOLUME_INCREMENT = 0.05;

    private RefWatcher refWatcher;

    public HashMap<String, UserSelectedArtwork> userSelectedArtwork = new HashMap<>();

    private static Logger jaudioTaggerLogger1 = Logger.getLogger("org.jaudiotagger.audio");
    private static Logger jaudioTaggerLogger2 = Logger.getLogger("org.jaudiotagger");

    private AppComponent appComponent;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if (BuildConfig.MULTIDEX_ENABLED) {
            MultiDex.install(base);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        instance = this;

        if (BuildConfig.DEBUG) {
            Log.w(TAG, "**Debug mode is ON**");

            // Traceur.enableLogging();

            // enableStrictMode();
        }

        appComponent = initDagger(this);

        refWatcher = LeakCanary.install(this);

        //Crashlytics
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).answers(new Answers()).build(), new Crashlytics());

        //Firebase Analytics
        FirebaseAnalytics.getInstance(this);
        
        VideoCastManager.initialize(this,
                new CastConfiguration.Builder(Config.CHROMECAST_APP_ID)
                        .enableLockScreen()
                        .enableNotification()
                        .build()
        );

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setIsUpgraded(prefs.getBoolean("pref_theme_gold", false));

        // we cannot call setDefaultValues for multiple fragment based XML preference
        // files with readAgain flag set to false, so always check KEY_HAS_SET_DEFAULT_VALUES
        if (!prefs.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            PreferenceManager.setDefaultValues(this, R.xml.settings_headers, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_artwork, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_blacklist, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_display, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_headset, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_scrobbling, true);
            PreferenceManager.setDefaultValues(this, R.xml.settings_themes, true);
        }

        // Turn off logging for jaudiotagger.
        jaudioTaggerLogger1.setLevel(Level.OFF);
        jaudioTaggerLogger2.setLevel(Level.OFF);

        TagOptionSingleton.getInstance().setPadNumbers(true);

        SettingsManager.getInstance().incrementLaunchCount();

        startService(new Intent(this, EqualizerService.class));

        Completable.fromAction(() -> {
            Query query = new Query.Builder()
                    .uri(CustomArtworkTable.URI)
                    .projection(new String[]{CustomArtworkTable.COLUMN_ID, CustomArtworkTable.COLUMN_KEY, CustomArtworkTable.COLUMN_TYPE, CustomArtworkTable.COLUMN_PATH})
                    .build();

            SqlUtils.createActionableQuery(ShuttleApplication.this, cursor ->
                            userSelectedArtwork.put(
                                    cursor.getString(cursor.getColumnIndexOrThrow(CustomArtworkTable.COLUMN_KEY)),
                                    new UserSelectedArtwork(
                                            cursor.getInt(cursor.getColumnIndexOrThrow(CustomArtworkTable.COLUMN_TYPE)),
                                            cursor.getString(cursor.getColumnIndexOrThrow(CustomArtworkTable.COLUMN_PATH)))
                            ),
                    query);
        })
                .subscribeOn(Schedulers.io())
                .subscribe();

        // Clean up the genres database - remove any genres which contain no songs. Also, populates song counts.
        // Since this is called on app launch, let's delay to allow more important tasks to complete.
        cleanGenres()
                .delaySubscription(5000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe();

        cleanMostPlayedPlaylist()
                .delay(7500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe();

        deleteOldResources()
                .delay(10000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public RefWatcher getRefWatcher() {
        return this.refWatcher;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        Glide.get(this).clearMemory();
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

    protected AppComponent initDagger(ShuttleApplication application) {
        return DaggerAppComponent.builder()
                .appModule(new AppModule(application))
                .build();
    }

    public static String getVersion() {
        try {
            return instance.getPackageManager().getPackageInfo(instance.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException | NullPointerException ignored) {

        }
        return "unknown";
    }

    public void setIsUpgraded(boolean isUpgraded) {
        this.isUpgraded = isUpgraded;
        AnalyticsManager.setIsUpgraded();
    }

    public boolean getIsUpgraded() {
        return isUpgraded || BuildConfig.DEBUG;
    }

    @NonNull
    private Completable deleteOldResources() {

        return Completable.fromAction(() -> {
            //Delete albumthumbs/artists directory
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(Environment.getExternalStorageDirectory() + "/albumthumbs/artists/");
                if (file.exists() && file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File child : files) {
                            child.delete();
                        }
                    }
                    file.delete();
                }
            }

            //Delete old http cache
            File oldHttpCache = getDiskCacheDir("http");
            if (oldHttpCache != null && oldHttpCache.exists()) {
                oldHttpCache.delete();
            }

            //Delete old thumbs cache
            File oldThumbsCache = getDiskCacheDir("thumbs");
            if (oldThumbsCache != null && oldThumbsCache.exists()) {
                oldThumbsCache.delete();
            }
        });
    }

    public static File getDiskCacheDir(String uniqueName) {
        try {
            // Check if media is mounted or storage is built-in, if so, try and use external cache dir
            // otherwise use internal cache dir
            String cachePath = null;
            File externalCacheDir = getInstance().getExternalCacheDir();
            if ((Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) && externalCacheDir != null) {
                cachePath = externalCacheDir.getPath();
            } else if (getInstance().getCacheDir() != null) {
                cachePath = getInstance().getCacheDir().getPath();
            }
            if (cachePath != null) {
                return new File(cachePath + File.separator + uniqueName);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "getDiskCacheDir() failed. " + e.toString());
        }
        return null;
    }

    /**
     * Check items in the Most Played playlist and ensure their ids exist in the MediaStore.
     * <p>
     * If they don't, remove them from the playlist.
     */
    @NonNull
    private Completable cleanMostPlayedPlaylist() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Completable.complete();
        }

        return Completable.fromAction(() -> {
            List<Integer> playCountIds = new ArrayList<>();

            Query query = new Query.Builder()
                    .uri(PlayCountTable.URI)
                    .projection(new String[]{PlayCountTable.COLUMN_ID})
                    .build();

            SqlUtils.createActionableQuery(this, cursor ->
                    playCountIds.add(cursor.getInt(cursor.getColumnIndex(PlayCountTable.COLUMN_ID))), query);

            List<Integer> songIds = new ArrayList<>();

            query = new Query.Builder()
                    .uri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    .projection(new String[]{MediaStore.Audio.Media._ID})
                    .build();

            SqlUtils.createActionableQuery(this, cursor ->
                    songIds.add(cursor.getInt(cursor.getColumnIndex(PlayCountTable.COLUMN_ID))), query);

            StringBuilder selection = new StringBuilder(PlayCountTable.COLUMN_ID + " IN (");

            selection.append(TextUtils.join(",", Stream.of(playCountIds)
                    .filter(playCountId -> !songIds.contains(playCountId))
                    .toList()));

            selection.append(")");

            try {
                getContentResolver().delete(PlayCountTable.URI, selection.toString(), null);
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    @NonNull
    private Observable<List<Song>> cleanGenres() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return Observable.empty();
        }

        // This observable emits a genre every 250ms. We then make a query against the genre database to populate the song count.
        // If the count is zero, then the genre can be deleted.
        // The reason for the delay is, on some slower devices, if the user has tons of genres, a ton of cursors get created.
        // If the maximum number of cursors is created (based on memory/processor speed or god knows what else), then the device
        // will start throwing CursorWindow exceptions, and the queries will slow down massively. This ends up making all queries slow.
        // This task isn't time critical, so we can afford to let it just casually do its job.
        return SqlBriteUtils.createSingleList(ShuttleApplication.getInstance(), Genre::new, Genre.getQuery())
                .doOnSuccess(genres -> Log.i(TAG, "Generated genres: " + genres.size()))
                .flatMapObservable(Observable::fromIterable)
                .concatMap(genre -> Observable.just(genre).delay(250, TimeUnit.MILLISECONDS))
                .flatMapSingle(genre -> genre.getSongsObservable()
                        .doOnSuccess(songs -> {
                            if (songs.isEmpty()) {
                                try {
                                    getContentResolver().delete(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, MediaStore.Audio.Genres._ID + " == " + genre.id, null);
                                } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                                    //Don't care if we couldn't delete this uri.
                                }
                            }
                        })
                );
    }

    private void enableStrictMode() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen()
                .build());
    }
}