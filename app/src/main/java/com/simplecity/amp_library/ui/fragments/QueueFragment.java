package com.simplecity.amp_library.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.aesthetic.Aesthetic;
import com.afollestad.aesthetic.Util;
import com.bumptech.glide.RequestManager;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.dagger.module.FragmentModule;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecity.amp_library.ui.presenters.PlayerPresenter;
import com.simplecity.amp_library.ui.presenters.QueuePresenter;
import com.simplecity.amp_library.ui.recyclerview.ItemTouchHelperCallback;
import com.simplecity.amp_library.ui.views.ContextualToolbar;
import com.simplecity.amp_library.ui.views.PlayerViewAdapter;
import com.simplecity.amp_library.ui.views.QueueView;
import com.simplecity.amp_library.utils.ContextualToolbarHelper;
import com.simplecity.amp_library.utils.MenuUtils;
import com.simplecity.amp_library.utils.MusicUtils;
import com.simplecity.amp_library.utils.PermissionUtils;
import com.simplecity.amp_library.utils.PlaylistUtils;
import com.simplecityapps.recycler_adapter.adapter.CompletionListUpdateCallbackAdapter;
import com.simplecityapps.recycler_adapter.adapter.ViewModelAdapter;
import com.simplecityapps.recycler_adapter.model.ViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.RecyclerListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class QueueFragment extends BaseFragment implements
        QueueView {

    private static final String TAG = "QueueFragment";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.line1)
    TextView lineOne;

    @BindView(R.id.line2)
    TextView lineTwo;

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView recyclerView;

    @BindView(R.id.contextualToolbar)
    ContextualToolbar contextualToolbar;

    private ItemTouchHelper itemTouchHelper;

    private ViewModelAdapter adapter;

    @Inject
    RequestManager requestManager;

    QueuePresenter queuePresenter;

    @Inject PlayerPresenter playerPresenter;

    private CompositeDisposable disposables = new CompositeDisposable();

    private ContextualToolbarHelper<Song> contextualToolbarHelper;

    private Disposable loadDataDisposable;
    private Unbinder unbinder;

    public static QueueFragment newInstance() {
        Bundle args = new Bundle();
        QueueFragment fragment = new QueueFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public QueueFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShuttleApplication.getInstance().getAppComponent()
                .plus(new FragmentModule(this))
                .inject(this);

        setHasOptionsMenu(true);

        adapter = new ViewModelAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_queue, container, false);

        unbinder = ButterKnife.bind(this, rootView);

        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());
        toolbar.inflateMenu(R.menu.menu_queue);

        SubMenu sub = toolbar.getMenu().addSubMenu(0, MusicUtils.Defs.ADD_TO_PLAYLIST, 1, R.string.save_as_playlist);
        PlaylistUtils.makePlaylistMenu(getContext(), sub);

        toolbar.setOnMenuItemClickListener(toolbarListener);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setRecyclerListener(new RecyclerListener());
        recyclerView.setAdapter(adapter);

        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback(
                (fromPosition, toPosition) ->
                        adapter.moveItem(fromPosition, toPosition), MusicUtils::moveQueueItem,
                () -> {
                    // Nothing to do
                }));

        itemTouchHelper.attachToRecyclerView(recyclerView);

        disposables.add(Aesthetic.get()
                .colorPrimary()
                .subscribe(color -> {
                    boolean isLight = Util.isColorLight(color);
                    lineOne.setTextColor(isLight ? Color.BLACK : Color.WHITE);
                    lineTwo.setTextColor(isLight ? Color.BLACK : Color.WHITE);
                }));

        setupContextualToolbar();

        queuePresenter = new QueuePresenter(requestManager, contextualToolbarHelper);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.notifyItemRangeChanged(0, adapter.getItemCount());

        playerPresenter.bindView(playerViewAdapter);
        queuePresenter.bindView(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        playerPresenter.unbindView(playerViewAdapter);
        queuePresenter.unbindView(this);

        if (loadDataDisposable != null) {
            loadDataDisposable.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        disposables.clear();

        unbinder.unbind();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupContextualToolbar() {
        contextualToolbar.getMenu().clear();
        contextualToolbar.inflateMenu(R.menu.context_menu_queue);
        SubMenu sub = contextualToolbar.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.makePlaylistMenu(getActivity(), sub);
        contextualToolbar.setOnMenuItemClickListener(MenuUtils.getSongMenuClickListener(getContext(), MusicUtils::getQueue));
        contextualToolbarHelper = new ContextualToolbarHelper<>(contextualToolbar, new ContextualToolbarHelper.Callback() {
            @Override
            public void notifyItemChanged(int position) {
                adapter.notifyItemChanged(position, 0);
            }

            @Override
            public void notifyDatasetChanged() {
                adapter.notifyItemRangeChanged(0, adapter.items.size(), 0);
            }
        });
    }

    @Override
    protected String screenName() {
        return TAG;
    }

    @Override
    public void loadData(List<ViewModel> items, int position) {
        PermissionUtils.RequestStoragePermissions(() -> {
            if (getActivity() != null && isAdded()) {

                if (loadDataDisposable != null) {
                    loadDataDisposable.dispose();
                }

                loadDataDisposable = adapter.setItems(items, new CompletionListUpdateCallbackAdapter() {
                    @Override
                    public void onComplete() {
                        setCurrentQueueItem(position);
                        recyclerView.scrollToPosition(position);
                    }
                });
            }
        });
    }

    @Override
    public void updateQueuePosition(int position) {
        recyclerView.scrollToPosition(position);
    }

    @Override
    public void showToast(String message, int duration) {
        Toast.makeText(getContext(), message, duration).show();
    }

    @Override
    public void startDrag(SongView.ViewHolder holder) {
        itemTouchHelper.startDrag(holder);
    }

    @Override
    public void setCurrentQueueItem(int position) {

        if (adapter.items.isEmpty()) {
            return;
        }

        int prevPosition = -1;
        int len = adapter.items.size();
        for (int i = 0; i < len; i++) {
            ViewModel viewModel = adapter.items.get(i);
            if (viewModel instanceof SongView) {
                if (((SongView) viewModel).isCurrentTrack()) {
                    prevPosition = i;
                }
                ((SongView) viewModel).setCurrentTrack(i == position);
            }
        }

        ((SongView) adapter.items.get(position)).setCurrentTrack(true);

        adapter.notifyItemChanged(prevPosition, 1);
        adapter.notifyItemChanged(position, 1);
    }

    @Override
    public void showTaggerDialog(TaggerDialog taggerDialog) {
        taggerDialog.show(getFragmentManager());
    }

    @Override
    public void removeFromQueue(int position) {
        adapter.removeItem(position);
    }

    private PlayerViewAdapter playerViewAdapter = new PlayerViewAdapter() {
        @Override
        public void trackInfoChanged(@Nullable Song song) {
            if (song != null) {
                lineOne.setText(song.name);
                if (song.albumArtistName != null && song.albumName != null) {
                    lineTwo.setText(String.format("%s | %s", song.albumArtistName, song.albumName));
                }
            }
        }
    };

    Toolbar.OnMenuItemClickListener toolbarListener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_clear:
                    queuePresenter.clearQueue();
                    return true;
                case MusicUtils.Defs.NEW_PLAYLIST:
                    queuePresenter.saveQueue(getContext());
                    return true;
                case MusicUtils.Defs.PLAYLIST_SELECTED:
                    queuePresenter.saveQueue(getContext(), item);
                    return true;
            }
            return false;
        }
    };
}