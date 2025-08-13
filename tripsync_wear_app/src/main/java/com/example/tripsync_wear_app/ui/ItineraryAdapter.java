package com.example.tripsync_wear_app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.tripsync_wear_app.databinding.ItemItineraryBinding;
import com.example.tripsync_wear_app.model.ItineraryModel;

import java.util.ArrayList;
import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.VH> {

    private final List<ItineraryModel> data = new ArrayList<>();

    public void submit(List<ItineraryModel> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemItineraryBinding b = ItemItineraryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        ItineraryModel it = data.get(pos);
        h.b.tvTripName.setText(it.tripName);
        String preview = it.destinations.isEmpty()
                ? "No destinations yet"
                : it.destinations.get(0).address + " · " + it.destinations.get(0).time;
        h.b.tvPreview.setText(preview);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemItineraryBinding b;
        VH(ItemItineraryBinding b) { super(b.getRoot()); this.b = b; }
    }
}
