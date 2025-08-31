package com.grepguru.focuslock.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.focuslock.R;
import com.grepguru.focuslock.model.ScheduleModel;

import java.util.List;

/**
 * Adapter for displaying schedules in RecyclerView
 * Features expandable items with brief info initially, full details when expanded
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {
    
    private List<ScheduleModel> schedules;
    private ScheduleListener listener;
    private int expandedPosition = -1; // Track which item is expanded
    
    public interface ScheduleListener {
        void onToggleSchedule(ScheduleModel schedule);
        void onEditSchedule(ScheduleModel schedule);
        void onDeleteSchedule(ScheduleModel schedule);
    }
    
    public ScheduleAdapter(List<ScheduleModel> schedules, ScheduleListener listener) {
        this.schedules = schedules;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        ScheduleModel schedule = schedules.get(position);
        holder.bind(schedule, position);
    }
    
    @Override
    public int getItemCount() {
        return schedules.size();
    }
    
    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        
        private LinearLayout scheduleCard;
        private TextView scheduleName;
        private TextView scheduleTime;
        private TextView scheduleStatus;
        private ImageView expandIcon;
        private LinearLayout expandedContent;
        
        // Expanded content views
        private TextView scheduleDetails;
        private TextView repeatInfo;
        private TextView notificationInfo;
        private TextView endTimeInfo;
        private Button toggleButton;
        private Button editButton;
        private Button deleteButton;
        
        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            
            scheduleCard = itemView.findViewById(R.id.scheduleCard);
            scheduleName = itemView.findViewById(R.id.scheduleName);
            scheduleTime = itemView.findViewById(R.id.scheduleTime);
            scheduleStatus = itemView.findViewById(R.id.scheduleStatus);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            expandedContent = itemView.findViewById(R.id.expandedContent);
            
            scheduleDetails = itemView.findViewById(R.id.scheduleDetails);
            repeatInfo = itemView.findViewById(R.id.repeatInfo);
            notificationInfo = itemView.findViewById(R.id.notificationInfo);
            endTimeInfo = itemView.findViewById(R.id.endTimeInfo);
            toggleButton = itemView.findViewById(R.id.toggleButton);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
        
        public void bind(ScheduleModel schedule, int position) {
            // Basic info (always visible)
            scheduleName.setText(schedule.getName());
            scheduleTime.setText(schedule.getFormattedStartTime());
            
            // Status indicator
            if (schedule.isEnabled()) {
                scheduleStatus.setText("Active");
                scheduleStatus.setTextColor(itemView.getContext().getColor(R.color.green));
            } else {
                scheduleStatus.setText("Inactive");
                scheduleStatus.setTextColor(itemView.getContext().getColor(R.color.gray));
            }
            
            // Expanded content
            scheduleDetails.setText(schedule.getFormattedDuration() + " focus session");
            repeatInfo.setText("Repeat: " + schedule.getRepeatDescription());
            notificationInfo.setText("Notify: " + schedule.getPreNotifyDescription());
            endTimeInfo.setText("Ends at: " + schedule.getFormattedEndTime());
            
            // Toggle button
            toggleButton.setText(schedule.isEnabled() ? "Disable" : "Enable");
            toggleButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleSchedule(schedule);
                }
            });
            
            // Edit and delete buttons
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditSchedule(schedule);
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteSchedule(schedule);
                }
            });
            
            // Expand/collapse functionality
            boolean isExpanded = position == expandedPosition;
            expandedContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            expandIcon.setRotation(isExpanded ? 180 : 0);
            
            scheduleCard.setOnClickListener(v -> {
                if (expandedPosition == position) {
                    // Collapse
                    expandedPosition = -1;
                    notifyItemChanged(position);
                } else {
                    // Expand
                    int previousExpanded = expandedPosition;
                    expandedPosition = position;
                    notifyItemChanged(previousExpanded);
                    notifyItemChanged(position);
                }
            });
        }
    }
} 