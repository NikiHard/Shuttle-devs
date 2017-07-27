package com.simplecity.amp_library.ui.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.IntentCompat;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.annimon.stream.IntPair;
import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.simplecity.amp_library.IabManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.model.CategoryItem;
import com.simplecity.amp_library.services.ArtworkDownloadService;
import com.simplecity.amp_library.ui.activities.MainActivity;
import com.simplecity.amp_library.ui.dialog.BlacklistDialog;
import com.simplecity.amp_library.ui.dialog.ChangelogDialog;
import com.simplecity.amp_library.ui.dialog.TabChooserDialog;
import com.simplecity.amp_library.ui.dialog.WhitelistDialog;
import com.simplecity.amp_library.ui.presenters.Presenter;
import com.simplecity.amp_library.utils.AnalyticsManager;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.ShuttleUtils;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class SettingsPresenter extends Presenter<SettingsView> {

    @Inject
    public SettingsPresenter() {
    }

    // Support Preferences

    void changelogClicked(Context context) {
        AnalyticsManager.logChangelogViewed();

        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showChangelog(ChangelogDialog.getChangelogDialog(context));
        }
    }

    void upgradeClicked(MainActivity activity) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showUpgradeDialog(new MaterialDialog.Builder(activity)
                    .title(activity.getResources().getString(R.string.get_pro_title))
                    .content(activity.getResources().getString(R.string.upgrade_dialog_message))
                    .positiveText(R.string.btn_upgrade)
                    .onPositive((dialog, which) -> {
                        if (ShuttleUtils.isAmazonBuild()) {
                            openPaidStoreLink();
                        } else {
                            purchaseUpgrade(activity);
                        }
                    })
                    .negativeText(R.string.get_pro_button_no)
                    .build());
        }
    }

    public void openPaidStoreLink() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            Intent intent = ShuttleUtils.getShuttleStoreIntent("com.simplecity.amp_pro");
            settingsView.openStoreLink(intent);
        }
    }

    public void purchaseUpgrade(MainActivity activity) {
        IabManager.getInstance().purchaseUpgrade(activity, success -> {
            SettingsView settingsView = getView();
            if (settingsView != null) {
                if (success) {
                    settingsView.showUpgradeSuccessDialog(
                            new MaterialDialog.Builder(activity)
                                    .title(activity.getResources().getString(R.string.upgraded_title))
                                    .content(activity.getResources().getString(R.string.upgraded_message))
                                    .positiveText(R.string.restart_button)
                                    .onPositive((materialDialog, dialogAction) -> {
                                        Intent intent = new Intent(activity, MainActivity.class);
                                        ComponentName componentNAme = intent.getComponent();
                                        Intent mainIntent = IntentCompat.makeRestartActivityTask(componentNAme);
                                        activity.startActivity(mainIntent);
                                    })
                                    .build());
                } else {
                    settingsView.showUpgradeFailedToast(R.string.iab_purchase_failed);
                }
            }
        });
    }

    public void restorePurchasesClicked() {
        IabManager.getInstance().restorePurchases(messageResId -> {
            SettingsView settingsView = getView();
            if (settingsView != null) {
                settingsView.showRestorePurchasesMessage(messageResId);
            }
        });
    }

    // Display

    public void chooseTabsClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showTabChooserDialog(TabChooserDialog.getDialog(context));
        }
    }

    public void chooseDefaultPageClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            List<CategoryItem> categoryItems = Stream.of(CategoryItem.getCategoryItems(sharedPreferences))
                    .filter(categoryItem -> categoryItem.isEnabled)
                    .toList();

            int defaultPageType = SettingsManager.getInstance().getDefaultPageType();
            int defaultPage = Math.min(Stream.of(categoryItems)
                    .indexed()
                    .filter(categoryItemIntPair -> categoryItemIntPair.getSecond().type == defaultPageType)
                    .map(IntPair::getFirst)
                    .findFirst()
                    .orElse(1), categoryItems.size());

            settingsView.showDefaultPageDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_default_page)
                            .items(Stream.of(categoryItems)
                                    .map(categoryItem -> context.getString(categoryItem.getTitleResId()))
                                    .toList())
                            .itemsCallbackSingleChoice(defaultPage, (dialog, itemView, which, text) -> {
                                SettingsManager.getInstance().setDefaultPageType(categoryItems.get(which).type);
                                return false;
                            })
                            .build());
        }
    }

    // Themes

    public void baseThemeClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showBaseThemeDialog(new MaterialDialog.Builder(context)
                    .title(R.string.pref_title_base_theme)
                    .items(R.array.baseThemeArray)
                    .itemsCallback((materialDialog, view, i, charSequence) -> changeBaseTheme(i))
                    .build());
        }
    }

    private void changeBaseTheme(int i) {
        int theme = R.style.AppTheme_Light;
        boolean isDark = false;
        switch (i) {
            case 0:
                //Light
                theme = R.style.AppTheme_Light;
                isDark = false;
                break;
            case 1:
                //Dark
                theme = R.style.AppTheme;
                isDark = true;
                break;
            case 2:
                //Black
                theme = R.style.AppTheme_Black;
                isDark = true;
                break;
        }

        Aesthetic.get()
                .activityTheme(theme)
                .isDark(isDark)
                .apply();
    }

    public void primaryColorClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showPrimaryColorDialog(
                    new ColorChooserDialog.Builder(context, R.string.pref_title_theme_pick_color)
                            .allowUserColorInput(true)
                            .dynamicButtonColor(true)
                            .preselect(Aesthetic.get().colorPrimary().blockingFirst())
                            .build());
        }
    }

    public void changePrimaryColor(int color) {
        Aesthetic.get()
                .colorPrimary(color)
                .colorStatusBarAuto()
                .apply();
    }

    public void accentColorClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showAccentColorDialog(
                    new ColorChooserDialog.Builder(context, R.string.pref_title_theme_pick_color)
                            .accentMode(true)
                            .allowUserColorInput(true)
                            .dynamicButtonColor(true)
                            .preselect(Aesthetic.get().colorAccent().blockingFirst())
                            .build());
        }
    }

    public void changeAccentColor(int color) {
        Aesthetic.get()
                .colorAccent(color)
                .apply();
    }

    public void tintNavBarClicked(boolean tintNavBar) {
        Aesthetic.get()
                .colorNavigationBarAuto(tintNavBar)
                .apply();
    }

    public void usePaletteClicked(boolean usePalette) {
        // If we're not using palette any more, set the primary color back to default
        if (!usePalette) {
            Aesthetic.get()
                    .colorPrimaryRes(R.color.blue_500)
                    .colorStatusBarAuto()
                    .apply();
        }
    }

    public void usePaletteNowPlayingOnlyClicked(boolean usePaletteNowPlayingOnly) {
        // If we're only using palette for 'now playing', set the primary color back to default
        if (usePaletteNowPlayingOnly) {
            Aesthetic.get()
                    .colorPrimaryRes(R.color.blue_500)
                    .colorStatusBarAuto()
                    .apply();
        }
    }

    // Artwork

    public void downloadArtworkClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showDownloadArtworkDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_download_artwork)
                            .content(R.string.pref_warning_download_artwork)
                            .positiveText(R.string.download)
                            .onPositive((dialog, which) -> downloadArtwork(context))
                            .negativeText(R.string.cancel)
                            .build());
        }
    }

    private void downloadArtwork(Context context) {
        Intent intent = new Intent(context, ArtworkDownloadService.class);
        ShuttleApplication.getInstance().startService(intent);
    }

    public void deleteArtworkClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showDeleteArtworkDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_delete_artwork)
                            .iconRes(R.drawable.ic_warning_24dp)
                            .content(R.string.delete_artwork_confirmation_dialog)
                            .positiveText(R.string.button_ok)
                            .onPositive((materialDialog, dialogAction) -> deleteArtwork())
                            .negativeText(R.string.cancel)
                            .build());
        }
    }

    private void deleteArtwork() {
        Observable.fromCallable(() -> {
            //Clear Glide' mem & disk cache
            Glide.get(ShuttleApplication.getInstance()).clearMemory();
            Glide.get(ShuttleApplication.getInstance()).clearDiskCache();
            return null;
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public void changeArtworkPreferenceClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showArtworkPreferenceChangeDialog(
                    new MaterialDialog.Builder(context)
                            .title(R.string.pref_title_delete_artwork)
                            .content(R.string.pref_summary_change_artwork_source)
                            .positiveText(R.string.pref_button_remove_artwork)
                            .onPositive((dialog1, which) -> deleteArtwork())
                            .negativeText(R.string.close)
                            .show());
        }
    }

    // Headset/Bluetooth

    // Scrobbling

    public void downloadScrobblerClicked() {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.launchDownloadScrobblerIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.adam.aslfms")));
        }
    }

    public void viewBlacklistClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showBlacklistDialog(BlacklistDialog.getDialog(context));
        }
    }

    public void viewWhitelistClicked(Context context) {
        SettingsView settingsView = getView();
        if (settingsView != null) {
            settingsView.showWhitelistDialog(WhitelistDialog.getDialog(context));
        }
    }
}