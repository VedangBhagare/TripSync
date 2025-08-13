package com.example.tripsync_phone_app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.SupportMapFragment;

/** Lets the map handle gestures smoothly inside scrollable parents. */
public class ScrollAwareSupportMapFragment extends SupportMapFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            v.setOnTouchListener((view, event) -> {
                ViewParent parent = view.getParent();
                if (parent != null) {
                    int a = event.getActionMasked();
                    if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                        parent.requestDisallowInterceptTouchEvent(false);
                    }
                }
                return false;
            });
        }
        return v;
    }
}
