package com.grepguru.zenlock.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.model.SelectableAppModel;
import java.util.List;
import java.util.Set;

public class WhitelistAdapter extends RecyclerView.Adapter<WhitelistAdapter.ViewHolder> {
    private List<SelectableAppModel> appList;
    private Set<String> selectedApps;
    private int maxAdditionalApps; // Configurable max additional selectable apps
    private OnSelectionChangeListener selectionChangeListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged();
    }

    public WhitelistAdapter(List<SelectableAppModel> appList, Set<String> selectedApps, int maxAdditionalApps) {
        this.appList = appList;
        this.selectedApps = selectedApps;
        this.maxAdditionalApps = maxAdditionalApps;
    }
    
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_whitelist_app, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectableAppModel app = appList.get(position);
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.appName.setText(app.getAppName());
        
        // Show package name for system apps
        if (app.getPackageName().startsWith("com.android.") || 
            app.getPackageName().startsWith("com.google.android.") ||
            app.getPackageName().startsWith("com.samsung.")) {
            holder.appPackage.setText(app.getPackageName());
            holder.appPackage.setVisibility(View.VISIBLE);
        } else {
            holder.appPackage.setVisibility(View.GONE);
        }
        
        holder.appCheckBox.setOnCheckedChangeListener(null);
        
        boolean isCurrentlySelected = selectedApps.contains(app.getPackageName());
        holder.appCheckBox.setChecked(isCurrentlySelected);
        app.setSelected(isCurrentlySelected);
        holder.appCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && selectedApps.size() >= maxAdditionalApps) {
                buttonView.setChecked(false); // Prevent selecting more than allowed
                android.widget.Toast.makeText(buttonView.getContext(), 
                    "Maximum " + maxAdditionalApps + " additional apps allowed", 
                    android.widget.Toast.LENGTH_SHORT).show();
            } else {
                app.setSelected(isChecked);
                if (isChecked) {
                    selectedApps.add(app.getPackageName());
                } else {
                    selectedApps.remove(app.getPackageName());
                }
                
                // Notify listener of selection change
                if (selectionChangeListener != null) {
                    selectionChangeListener.onSelectionChanged();
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        SwitchCompat appCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appPackage = itemView.findViewById(R.id.appPackage);
            appCheckBox = itemView.findViewById(R.id.appCheckBox);
        }
    }
}
