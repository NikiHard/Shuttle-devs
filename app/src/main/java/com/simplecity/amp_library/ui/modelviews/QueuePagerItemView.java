package com.simplecity.amp_library.ui.modelviews;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.simplecity.amp_library.R;
import com.simplecity.amp_library.model.Song;
import com.simplecity.amp_library.ui.adapters.ViewType;
import com.simplecity.amp_library.utils.PlaceholderProvider;
import com.simplecityapps.recycler_adapter.model.BaseViewModel;
import com.simplecityapps.recycler_adapter.recyclerview.BaseViewHolder;

public class QueuePagerItemView extends BaseViewModel<QueuePagerItemView.ViewHolder> {

    public Song song;
    private RequestManager requestManager;

    public QueuePagerItemView(Song song, RequestManager requestManager) {
        this.song = song;
        this.requestManager = requestManager;
    }

    @Override
    public int getViewType() {
        return ViewType.QUEUE_PAGER_ITEM;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.list_item_queue_pager;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent) {
        return new ViewHolder(createView(parent));
    }

    @Override
    public void bindView(ViewHolder holder) {
        super.bindView(holder);

        requestManager
                .load(song)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(PlaceholderProvider.getInstance().getPlaceHolderDrawable(song.name, true))
                .into((ImageView) holder.itemView);

        holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);
                return false;
            }
        });
    }

    public static class ViewHolder extends BaseViewHolder<QueuePagerItemView> {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void recycle() {
            super.recycle();

            Glide.clear(itemView);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueuePagerItemView that = (QueuePagerItemView) o;

        return song != null ? song.equals(that.song) : that.song == null;
    }

    @Override
    public int hashCode() {
        return song != null ? song.hashCode() : 0;
    }
}