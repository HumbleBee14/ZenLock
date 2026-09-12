package com.grepguru.zenlock.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.grepguru.zenlock.R;
import com.grepguru.zenlock.model.ScheduleModel;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private final List<ScheduleModel> schedules;
    private final ScheduleListener listener;
    private int expandedPosition = -1;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        holder.bind(schedules.get(position), position);
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {

        private final View scheduleCard;
        private final View scheduleInfo;
        private final TextView scheduleTime;
        private final TextView scheduleName;
        private final TextView scheduleTag;
        private final TextView scheduleMeta;
        private final SwitchCompat scheduleSwitch;
        private final View expandedContent;
        private final View editButton;
        private final View deleteButton;

        ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            scheduleCard = itemView.findViewById(R.id.scheduleCard);
            scheduleInfo = itemView.findViewById(R.id.scheduleInfo);
            scheduleTime = itemView.findViewById(R.id.scheduleTime);
            scheduleName = itemView.findViewById(R.id.scheduleName);
            scheduleTag = itemView.findViewById(R.id.scheduleTag);
            scheduleMeta = itemView.findViewById(R.id.scheduleMeta);
            scheduleSwitch = itemView.findViewById(R.id.scheduleSwitch);
            expandedContent = itemView.findViewById(R.id.expandedContent);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(ScheduleModel schedule, int position) {
            scheduleName.setText(schedule.getName());
            scheduleTag.setText(repeatTag(schedule));
            scheduleTime.setText(schedule.getFormattedStartTime());
            scheduleMeta.setText(buildMeta(schedule));
            scheduleInfo.setAlpha(schedule.isEnabled() ? 1f : 0.5f);

            scheduleSwitch.setOnCheckedChangeListener(null);
            scheduleSwitch.setChecked(schedule.isEnabled());
            scheduleSwitch.setOnCheckedChangeListener((button, checked) -> {
                if (checked == schedule.isEnabled()) return;
                button.setChecked(schedule.isEnabled());
                if (listener != null) listener.onToggleSchedule(schedule);
            });

            editButton.setOnClickListener(v -> {
                if (listener != null) listener.onEditSchedule(schedule);
            });
            deleteButton.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteSchedule(schedule);
            });

            boolean expanded = position == expandedPosition;
            expandedContent.setVisibility(expanded ? View.VISIBLE : View.GONE);

            scheduleCard.setOnClickListener(v -> {
                int previous = expandedPosition;
                expandedPosition = expanded ? -1 : position;
                if (previous >= 0) notifyItemChanged(previous);
                notifyItemChanged(position);
            });
        }

        private String buildMeta(ScheduleModel schedule) {
            return "– " + schedule.getFormattedEndTime() + " · " + schedule.getFormattedDuration();
        }

        private String repeatTag(ScheduleModel schedule) {
            switch (schedule.getRepeatType()) {
                case ONCE: return "Once";
                case DAILY: return "Daily";
                default: return weeklyTag(schedule.getRepeatDays());
            }
        }

        private String weeklyTag(Set<Integer> days) {
            if (days.size() == 5 && !days.contains(Calendar.SATURDAY) && !days.contains(Calendar.SUNDAY)) return "Weekdays";
            if (days.size() == 2 && days.contains(Calendar.SATURDAY) && days.contains(Calendar.SUNDAY)) return "Weekends";
            if (days.isEmpty() || days.size() > 3) return "Weekly";
            String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            StringBuilder tag = new StringBuilder();
            for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                if (!days.contains(day)) continue;
                if (tag.length() > 0) tag.append(' ');
                tag.append(names[day - 1]);
            }
            return tag.toString();
        }
    }
}
