package com.example.tripsync_phone_app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.databinding.ItemItineraryBinding;

import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.VH> {

    public interface OnEdit { void onEdit(Itinerary it); }
    public interface OnDelete { void onDelete(Itinerary it); }

    private final List<Itinerary> data;
    private final OnEdit onEdit;
    private final OnDelete onDelete;

    public ItineraryAdapter(List<Itinerary> data, OnEdit onEdit, OnDelete onDelete) {
        this.data = data;
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    static class VH extends RecyclerView.ViewHolder {
        ItemItineraryBinding b;
        VH(ItemItineraryBinding b) { super(b.getRoot()); this.b = b; }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemItineraryBinding b = ItemItineraryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Itinerary it = data.get(position);
        h.b.txtTripName.setText(it.tripName);
        h.b.btnEdit.setOnClickListener(v -> onEdit.onEdit(it));
        h.b.btnDelete.setOnClickListener(v -> onDelete.onDelete(it));
    }

    @Override public int getItemCount() { return data.size(); }
}
