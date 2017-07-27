package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.afollestad.aesthetic.ViewBackgroundAction;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.views.PlayPauseView;
import com.simplecity.amp_library.ui.views.PlayerViewAdapter;
import com.simplecity.amp_library.utils.PlaceholderProvider;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;
import com.simplecity.multisheetview.ui.view.MultiSheetView;

import static com.afollestad.aesthetic.Rx.distinctToMainThread;
import static com.afollestad.aesthetic.Rx.onErrorLogAndRethrow;

public class MiniPlayerFragment extends BaseFragment {

    private static final String TAG = "MiniPlayerFragment";

    View rootView;

    @BindView(R.id.mini_play)
    PlayPauseView playPauseView;

    @BindView(R.id.progressbar)
    ProgressBar progressBar;

    @BindView(R.id.track_name)
    TextView trackName;

    @BindView(R.id.artist_name)
    TextView artistName;

    @BindView(R.id.mini_album_artwork)
    ImageView miniArtwork;

    @Inject
    PlayerPresenter presenter;

    private CompositeDisposable disposable = new CompositeDisposable();

    private Unbinder unbinder;

    public MiniPlayerFragment() {

    }

    public static MiniPlayerFragment newInstance() {
        MiniPlayerFragment fragment = new MiniPlayerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_mini_player, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        rootView.setOnClickListener(v -> {
            MultiSheetView multiSheetView = MultiSheetView.getParentMultiSheetView(rootView);
            if (multiSheetView != null) {
                multiSheetView.expandSheet(MultiSheetView.Sheet.FIRST);
            }
        });
        rootView.setOnTouchListener(new OnSwipeTouchListener(getActivity()));

        playPauseView.setOnClickListener(v -> {
            playPauseView.toggle();
            playPauseView.postDelayed(() -> presenter.togglePlayback(), 200);
        });

        progressBar.setMax(1000);

        disposable.add(Aesthetic.get()
                .colorPrimary()
                .compose(distinctToMainThread())
                .subscribe(color -> {
                    boolean isDark = !Util.isColorLight(color);
                    trackName.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                    artistName.setTextColor(isDark ? Color.WHITE : Color.BLACK);
                    ViewBackgroundAction.create(rootView).accept(color);
                }, onErrorLogAndRethrow()));

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.bindView(playerViewAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (presenter != null) {
            presenter.updateTrackInfo();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        presenter.unbindView(playerViewAdapter);
        disposable.clear();
        unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        rootView.setOnTouchListener(null);

        super.onDestroy();
    }

    private class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        void onSwipeLeft() {
            presenter.skip();
        }

        void onSwipeRight() {
            presenter.prev(false);
        }

        public boolean onTouch(View v, MotionEvent event) {

            boolean consumed = gestureDetector.onTouchEvent(event);

            if (!consumed) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
            }

            return consumed;
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            GestureListener() {
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {

        @Override
        public void setSeekProgress(int progress) {
            progressBar.setProgress(progress);
        }


        @Override
        public void playbackChanged(boolean isPlaying) {
            if (isPlaying) {
                if (playPauseView.isPlay()) {
                    playPauseView.toggle();
                }
            } else {
                if (!playPauseView.isPlay()) {
                    playPauseView.toggle();
                }
            }
        }

        @Override
        public void trackInfoChanged(@Nullable Song song) {

            if (song == null) return;

            trackName.setText(song.name);
            artistName.setText(String.format("%s | %s", song.artistName, song.albumName));

            Glide.with(getContext())
                    .load(song)
                    .priority(Priority.HIGH)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(PlaceholderProvider.getInstance().getPlaceHolderDrawable(song.name, false))
                    .into(miniArtwork);

            rootView.setContentDescription(getString(R.string.btn_now_playing, song.name, song.artistName));

        }
    };
}