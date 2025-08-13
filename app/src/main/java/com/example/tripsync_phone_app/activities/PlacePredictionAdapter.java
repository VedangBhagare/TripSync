package com.example.tripsync_phone_app.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tripsync_phone_app.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import java.util.ArrayList;
import java.util.List;

class PlacePredictionAdapter extends RecyclerView.Adapter<PlacePredictionAdapter.VH> {

    interface OnItemClick {
        void onClick(AutocompletePrediction p);
    }

    private final List<AutocompletePrediction> data = new ArrayList<>();
    private final OnItemClick onItemClick;

    PlacePredictionAdapter(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    void submit(List<AutocompletePrediction> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place_prediction, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AutocompletePrediction p = data.get(pos);
        h.primary.setText(p.getPrimaryText(null));
        h.secondary.setText(p.getSecondaryText(null));
        h.itemView.setOnClickListener(v -> onItemClick.onClick(p));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView primary, secondary;
        VH(@NonNull View itemView) {
            super(itemView);
            primary = itemView.findViewById(R.id.primary);
            secondary = itemView.findViewById(R.id.secondary);
        }
    }
}
