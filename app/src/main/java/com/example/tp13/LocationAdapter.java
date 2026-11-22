package com.example.tp13;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying campus locations in a horizontal list.
 */
public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    public interface OnLocationClickListener {
        void onLocationClick(CampusLocation location, int position);
    }

    private final List<CampusLocation> locations;
    private final OnLocationClickListener listener;
    private int selectedPosition = -1;

    public LocationAdapter(List<CampusLocation> locations, OnLocationClickListener listener) {
        this.locations = locations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_campus_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CampusLocation location = locations.get(position);
        boolean isSelected = (position == selectedPosition);

        holder.tvName.setText(location.getName());
        holder.tvCategory.setText(location.getCategory());
        holder.imgIcon.setImageResource(location.getIconResId());

        // Update card background according to selection state
        holder.cardContainer.setBackgroundResource(
                isSelected ? R.drawable.bg_card_selected : R.drawable.bg_card_unselected
        );

        holder.itemView.setOnClickListener(v -> {
            int prevPos = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            if (prevPos >= 0 && prevPos < locations.size()) {
                notifyItemChanged(prevPos);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onLocationClick(location, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations != null ? locations.size() : 0;
    }

    /**
     * Programmatically update selection when a marker is clicked on the map.
     */
    public void setSelectedLocation(CampusLocation location) {
        if (location == null || locations == null) return;
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).getId().equals(location.getId())) {
                int prevPos = selectedPosition;
                selectedPosition = i;
                if (prevPos >= 0 && prevPos < locations.size()) {
                    notifyItemChanged(prevPos);
                }
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View cardContainer;
        final ImageView imgIcon;
        final TextView tvName;
        final TextView tvCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            imgIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvName = itemView.findViewById(R.id.tvLocationName);
            tvCategory = itemView.findViewById(R.id.tvLocationCategory);
        }
    }
}