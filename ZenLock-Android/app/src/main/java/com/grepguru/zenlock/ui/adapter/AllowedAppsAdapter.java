package com.grepguru.zenlock.ui.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.model.AppModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllowedAppsAdapter extends RecyclerView.Adapter<AllowedAppsAdapter.ViewHolder> {

    private final List<AppModel> allowedApps;
    private final Context context;
    private final Map<String, Drawable> flatIcons = new HashMap<>();
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_allowed_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppModel app = allowedApps.get(position);
        holder.icon.setImageDrawable(flatIcon(app));
        holder.name.setText(app.getAppName());
        holder.itemView.setContentDescription(app.getAppName());
        holder.itemView.setOnClickListener(v -> launch(app));
    }

    private Drawable flatIcon(AppModel app) {
        Drawable cached = flatIcons.get(app.getPackageName());
        if (cached != null) return cached;
        Drawable source = app.getIcon();
        if (source == null) return null;
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, context.getResources().getDisplayMetrics());
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        source.setBounds(0, 0, size, size);
        source.draw(new Canvas(bitmap));
        Drawable flat = new BitmapDrawable(context.getResources(), bitmap);
        flatIcons.put(app.getPackageName(), flat);
        return flat;
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
        return allowedApps.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.appIcon);
            name = view.findViewById(R.id.appName);
        }
    }
}
