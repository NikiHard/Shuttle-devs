package com.simplecity.amp_library.ui.modelviews;

import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.format.PrefixHighlighter;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.ui.views.NonScrollImageButton;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecity.amp_library.utils.SettingsManager;
import com.simplecity.amp_library.utils.SortManager;
import com.simplecity.amp_library.utils.StringUtils;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SongView extends BaseSelectableViewModel<SongView.ViewHolder, Song> implements
        SectionedView,
        SelectableViewModel<Song> {

    public interface ClickListener {

        void onSongClick(int position, SongView songView);

        boolean onSongLongClick(int position, SongView songView);

        void onSongOverflowClick(int position, View v, Song song);

        void onStartDrag(ViewHolder holder);
    }

    private static final String TAG = "SongView";

    public Song song;

    private RequestManager requestManager;

    private PrefixHighlighter prefixHighlighter;

    private char[] prefix;

    private boolean editable;

    private boolean showAlbumArt;

    private boolean showPlayCount;

    private boolean showTrackNumber;

    private boolean showArtistName = true;

    private boolean showAlbumName = true;

    private boolean isCurrentTrack;

    @Nullable
    private ClickListener listener;

    public SongView(Song song, RequestManager requestManager) {
        this.song = song;
        this.requestManager = requestManager;
    }

    public void setClickListener(@Nullable ClickListener listener) {
        this.listener = listener;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void showAlbumArt(boolean showAlbumArt) {
        this.showAlbumArt = showAlbumArt;
    }

    public void showPlayCount(boolean showPlayCount) {
        this.showPlayCount = showPlayCount;
    }

    public void showArtistName(boolean showArtistName) {
        this.showArtistName = showArtistName;
    }

    public void showAlbumName(boolean showAlbumName) {
        this.showAlbumName = showAlbumName;
    }

    public void setPrefix(PrefixHighlighter prefixHighlighter, char[] prefix) {
        this.prefixHighlighter = prefixHighlighter;
        this.prefix = prefix;
    }

    public void setShowTrackNumber(boolean showTrackNumber) {
        this.showTrackNumber = showTrackNumber;
    }

    public void setCurrentTrack(boolean isCurrentTrack) {
        this.isCurrentTrack = isCurrentTrack;
    }

    public boolean isCurrentTrack() {
        return isCurrentTrack;
    }

    private void onItemClick(int position) {
        if (listener != null) {
            listener.onSongClick(position, this);
        }
    }

    private void onOverflowClick(int position, View v) {
        if (listener != null) {
            listener.onSongOverflowClick(position, v, song);
        }
    }

    private boolean onItemLongClick(int position) {
        if (listener != null) {
            return listener.onSongLongClick(position, this);
        }
        return false;
    }

    private void onStartDrag(ViewHolder holder) {
        if (listener != null) {
            listener.onStartDrag(holder);
        }
    }

    @Override
    public int getViewType() {
        return editable ? ViewType.SONG_EDITABLE : ViewType.SONG;
    }

    @Override
    public int getLayoutResId() {
        return editable ? R.layout.list_item_edit : R.layout.list_item_two_lines;
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        holder.lineOne.setText(song.name);

        if (holder.playCount != null) {
            if (showPlayCount && song.playCount > 1) {
                holder.playCount.setVisibility(View.VISIBLE);
                holder.playCount.setText(String.valueOf(song.playCount));
            } else {
                holder.playCount.setVisibility(View.GONE);
            }
        }

        if (showArtistName && showAlbumName) {
            holder.lineTwo.setText(String.format("%s - %s", song.artistName, song.albumName));
            holder.lineTwo.setVisibility(View.VISIBLE);
        } else if (showAlbumName) {
            holder.lineTwo.setText(song.albumName);
            holder.lineTwo.setVisibility(View.VISIBLE);
        } else {
            holder.lineTwo.setVisibility(View.GONE);
        }

        holder.lineThree.setText(song.getDurationLabel());

        if (holder.dragHandle != null) {
            holder.dragHandle.setActivated(isCurrentTrack);
        }

        if (holder.artwork != null) {
            if (showAlbumArt && SettingsManager.getInstance().showArtworkInQueue()) {
                holder.artwork.setVisibility(View.VISIBLE);
                requestManager.load(song)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(PlaceholderProvider.getInstance().getPlaceHolderDrawable(song.albumName, false))
                        .into(holder.artwork);
            } else {
                holder.artwork.setVisibility(View.GONE);
            }
        }

        holder.overflowButton.setContentDescription(holder.itemView.getResources().getString(R.string.btn_options, song.name));

        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }

        if (holder.trackNumber != null) {
            if (showTrackNumber) {
                holder.trackNumber.setVisibility(View.VISIBLE);
                holder.trackNumber.setText(String.valueOf(song.track));
            } else {
                holder.trackNumber.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void bindView(ViewHolder holder, int position, List payloads) {
        super.bindView(holder, position, payloads);

        //A partial bind. Due to the areContentsEqual implementation, the only reason this is called
        //is because the prefix changed. Update accordingly.
        if (prefixHighlighter != null) {
            prefixHighlighter.setText(holder.lineOne, prefix);
            prefixHighlighter.setText(holder.lineTwo, prefix);
        }

        if (holder.dragHandle != null) {
            holder.dragHandle.setActivated(isCurrentTrack);
        }
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public String getSectionName() {
        int sortOrder = SortManager.getInstance().getSongsSortOrder();

        if (sortOrder != SortManager.SongSort.DATE
                && sortOrder != SortManager.SongSort.DURATION
                && sortOrder != SortManager.SongSort.TRACK_NUMBER) {

            String string = null;
            boolean requiresSubstring = true;
            switch (sortOrder) {
                case SortManager.SongSort.DEFAULT:
                    string = StringUtils.keyFor(song.name);
                    break;
                case SortManager.SongSort.NAME:
                    string = song.name;
                    break;
                case SortManager.SongSort.YEAR:
                    string = String.valueOf(song.year);
                    if (string.length() != 4) {
                        string = "-";
                    } else {
                        string = string.substring(2, 4);
                    }
                    requiresSubstring = false;
                    break;
                case SortManager.SongSort.ALBUM_NAME:
                    string = StringUtils.keyFor(song.albumName);
                    break;
                case SortManager.SongSort.ARTIST_NAME:
                    string = StringUtils.keyFor(song.artistName);
                    break;
            }

            if (requiresSubstring) {
                if (!TextUtils.isEmpty(string)) {
                    string = string.substring(0, 1).toUpperCase();
                } else {
                    string = " ";
                }
            }
            return string;
        }
        return "";
    }

    @Override
    public boolean areContentsEqual(Object other) {
        if (other instanceof SongView) {
            return this.song.equals(((SongView) other).song)
                    && Arrays.equals(prefix, ((SongView) other).prefix);
        }
        return false;
    }

    @Override
    public Song getItem() {
        return song;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SongView songView = (SongView) o;

        if (editable != songView.editable) return false;
        if (showAlbumArt != songView.showAlbumArt) return false;
        if (showPlayCount != songView.showPlayCount) return false;
        if (showTrackNumber != songView.showTrackNumber) return false;
        if (showArtistName != songView.showArtistName) return false;
        if (showAlbumName != songView.showAlbumName) return false;
        if (isCurrentTrack != songView.isCurrentTrack) return false;
        return song != null ? song.equals(songView.song) : songView.song == null;
    }

    @Override
    public int hashCode() {
        int result = song != null ? song.hashCode() : 0;
        result = 31 * result + (editable ? 1 : 0);
        result = 31 * result + (showAlbumArt ? 1 : 0);
        result = 31 * result + (showPlayCount ? 1 : 0);
        result = 31 * result + (showTrackNumber ? 1 : 0);
        result = 31 * result + (showArtistName ? 1 : 0);
        result = 31 * result + (showAlbumName ? 1 : 0);
        result = 31 * result + (isCurrentTrack ? 1 : 0);
        return result;
    }

    public static class ViewHolder extends BaseViewHolder<SongView> {

        @BindView(R.id.line_one)
        TextView lineOne;

        @BindView(R.id.line_two)
        TextView lineTwo;

        @BindView(R.id.line_three)
        TextView lineThree;

        @Nullable @BindView(R.id.trackNumber)
        TextView trackNumber;

        @Nullable @BindView(R.id.play_count)
        TextView playCount;

        @BindView(R.id.btn_overflow)
        public NonScrollImageButton overflowButton;

        @Nullable @BindView(R.id.drag_handle)
        ImageView dragHandle;

        @Nullable @BindView(R.id.image)
        ImageView artwork;

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            if (playCount != null) {
                //Todo: Set background color of playCount
            }

            itemView.setOnClickListener(v -> viewModel.onItemClick(getAdapterPosition()));
            itemView.setOnLongClickListener(v -> viewModel.onItemLongClick(getAdapterPosition()));

            overflowButton.setOnClickListener(v -> viewModel.onOverflowClick(getAdapterPosition(), v));

            if (dragHandle != null) {
                dragHandle.setOnTouchListener((v, event) -> {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        viewModel.onStartDrag(this);
                    }
                    return true;
                });
            }
        }

        @Override
        public String toString() {
            return "SongView.ViewHolder";
        }

        @Override
        public void recycle() {
            super.recycle();

            if (artwork != null) {
                Glide.clear(artwork);
            }
        }
    }
}