package com.grepguru.zenlock.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.model.AppModel;
import com.grepguru.zenlock.ui.layout.AllowedAppsGrid;
import java.util.List;

/** Each recycled item is a centered row of up to four equal-width app buttons. */
public class AllowedAppsAdapter extends RecyclerView.Adapter<AllowedAppsAdapter.ViewHolder> {
    private final List<AppModel> allowedApps;
    private final Context context;
    private OnAppLaunchListener onAppLaunchListener;

    public interface OnAppLaunchListener {
        void onAppLaunching();
    }

    public AllowedAppsAdapter(Context context, List<AppModel> allowedApps) {
        this.context = context;
        this.allowedApps = allowedApps;
    }

    public void setOnAppLaunchListener(OnAppLaunchListener listener) {
        this.onAppLaunchListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.TOP);
        row.setWeightSum(AllowedAppsGrid.COLUMNS);
        row.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int row) {
        int count = AllowedAppsGrid.itemsInRow(allowedApps.size(), row);
        float sideWeight = AllowedAppsGrid.sideWeight(allowedApps.size(), row);
        holder.leading.setLayoutParams(new LinearLayout.LayoutParams(0, 0, sideWeight));
        holder.trailing.setLayoutParams(new LinearLayout.LayoutParams(0, 0, sideWeight));
        for (int column = 0; column < AllowedAppsGrid.COLUMNS; column++) {
            View cell = holder.cells[column];
            cell.setVisibility(column < count ? View.VISIBLE : View.GONE);
            cell.setOnClickListener(null);
            if (column >= count) continue;
            AppModel app = allowedApps.get(row * AllowedAppsGrid.COLUMNS + column);
            ((ImageView) cell.findViewById(R.id.appIcon)).setImageDrawable(app.getIcon());
            ((TextView) cell.findViewById(R.id.appName)).setText(app.getAppName());
            cell.setContentDescription(app.getAppName());
            cell.setOnClickListener(v -> launch(app));
        }
    }

    private void launch(AppModel app) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
        try {
            if (intent == null) throw new ActivityNotFoundException();
            context.startActivity(intent);
            if (onAppLaunchListener != null) onAppLaunchListener.onAppLaunching();
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(context, context.getString(R.string.cannot_open_app, app.getAppName()), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return AllowedAppsGrid.rowCount(allowedApps.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View leading;
        final View trailing;
        final View[] cells = new View[AllowedAppsGrid.COLUMNS];

        ViewHolder(LinearLayout row) {
            super(row);
            leading = new View(row.getContext());
            trailing = new View(row.getContext());
            row.addView(leading, new LinearLayout.LayoutParams(0, 0));
            for (int i = 0; i < cells.length; i++) {
                cells[i] = LayoutInflater.from(row.getContext()).inflate(R.layout.item_allowed_app, row, false);
                row.addView(cells[i], new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
            row.addView(trailing, new LinearLayout.LayoutParams(0, 0));
        }
    }
}
