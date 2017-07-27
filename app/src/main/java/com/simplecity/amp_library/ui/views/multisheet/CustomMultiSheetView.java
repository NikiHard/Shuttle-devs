package com.simplecity.amp_library.ui.views.multisheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.util.AttributeSet;

import com.simplecity.amp_library.ShuttleApplication;
import com.simplecity.amp_library.ui.drawer.DrawerLockManager;
import com.simplecity.multisheetview.ui.view.MultiSheetView;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

/**
 * A custom MultiSheetView with an RXRelay for responding to expand/collapse events.
 */
public class CustomMultiSheetView extends MultiSheetView {

    @Inject MultiSheetEventRelay multiSheetEventRelay;

    private CompositeDisposable disposables;

    private DrawerLockManager.DrawerLock sheet1Lock = () -> "Sheet 1";
    private DrawerLockManager.DrawerLock sheet2Lock = () -> "Sheet 2";

    public CustomMultiSheetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        disposables = new CompositeDisposable();

        setSheetStateChangeListener((sheet, state) -> {
            if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                switch (sheet) {
                    case Sheet.FIRST:
                        DrawerLockManager.getInstance().removeDrawerLock(sheet1Lock);
                        break;
                    case Sheet.SECOND:
                        DrawerLockManager.getInstance().removeDrawerLock(sheet2Lock);
                        break;
                }
            } else if (state == BottomSheetBehavior.STATE_EXPANDED) {
                switch (sheet) {
                    case Sheet.FIRST:
                        DrawerLockManager.getInstance().addDrawerLock(sheet1Lock);
                        break;
                    case Sheet.SECOND:
                        DrawerLockManager.getInstance().addDrawerLock(sheet2Lock);
                        break;
                }
            }
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        ShuttleApplication.getInstance().getAppComponent().inject(this);

        disposables.add(multiSheetEventRelay.getEvents().subscribe(event -> {
            switch (event.action) {
                case MultiSheetEventRelay.MultiSheetEvent.Action.GOTO:
                    goToSheet(event.sheet);
                    break;
                case MultiSheetEventRelay.MultiSheetEvent.Action.HIDE:
                    hide(false);
                    break;
                case MultiSheetEventRelay.MultiSheetEvent.Action.SHOW_IF_HIDDEN:
                    unhide();
                    break;
            }
        }));
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        disposables.clear();
    }
}