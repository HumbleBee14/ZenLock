package com.grepguru.focuslock.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.grepguru.focuslock.R;
import com.grepguru.focuslock.model.SelectableAppModel;
import java.util.List;
import java.util.Set;

public class WhitelistAdapter extends RecyclerView.Adapter<WhitelistAdapter.ViewHolder> {
    private List<SelectableAppModel> appList;
    private Set<String> selectedApps;
    private int maxAdditionalApps; // Configurable max additional selectable apps

    public WhitelistAdapter(List<SelectableAppModel> appList, Set<String> selectedApps, int maxAdditionalApps) {
        this.appList = appList;
        this.selectedApps = selectedApps;
        this.maxAdditionalApps = maxAdditionalApps;
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
        CheckBox appCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appCheckBox = itemView.findViewById(R.id.appCheckBox);
        }
    }
}
