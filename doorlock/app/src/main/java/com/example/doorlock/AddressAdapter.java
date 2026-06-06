package com.example.doorlock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
    private List<AddressItem> items;
    private OnItemClickListener clickListener;
    public interface OnItemClickListener {
        void onItemClick(AddressItem item);
    }

    public AddressAdapter(List<AddressItem> items, OnItemClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }
    public AddressAdapter(List<AddressItem> items) {
        this.items = items;
    }

    public void updateData(List<AddressItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AddressItem item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ViewHolder(View view) {
            super(view);
            // Проверьте, что в item_address.xml ID именно такой:
            title = view.findViewById(R.id.text_address_name);
        }
    }
}