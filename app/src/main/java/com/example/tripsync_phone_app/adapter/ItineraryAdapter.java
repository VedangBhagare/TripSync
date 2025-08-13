package com.example.tripsync_phone_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.database.Itinerary;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ItineraryAdapter extends RecyclerView.Adapter<ItineraryAdapter.Holder> {

    public interface OnItineraryClick {
        void onView(Itinerary it);
        void onEdit(Itinerary it);
        void onDelete(Itinerary it);
    }

    private final List<Itinerary> data;
    private final OnItineraryClick callbacks;

    public ItineraryAdapter(List<Itinerary> data, OnItineraryClick cb) {
        this.data = data;
        this.callbacks = cb;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_itinerary, parent, false);
        return new Holder(v);
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
        Itinerary it = data.get(pos);
        h.name.setText(it.tripName);
        h.btnView.setOnClickListener(v -> callbacks.onView(it));
        h.btnEdit.setOnClickListener(v -> callbacks.onEdit(it));
        h.btnDelete.setOnClickListener(v -> callbacks.onDelete(it));
    }

    @Override public int getItemCount() { return data.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView name;
        MaterialButton btnView, btnEdit, btnDelete;
        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtTripName);
            btnView = itemView.findViewById(R.id.btnView);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
