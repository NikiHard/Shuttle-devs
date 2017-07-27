package com.simplecity.amp_library.ui.views;

import com.simplecity.amp_library.tagger.TaggerDialog;
import com.simplecity.amp_library.ui.modelviews.SongView;
import com.simplecityapps.recycler_adapter.model.ViewModel;

import java.util.List;

public interface QueueView {

    void loadData(List<ViewModel> items, int position);

    void updateQueuePosition(int position);

    void showToast(String message, int duration);

    void startDrag(SongView.ViewHolder holder);

    void setCurrentQueueItem(int position);

    void showTaggerDialog(TaggerDialog taggerDialog);

    void removeFromQueue(int position);
}