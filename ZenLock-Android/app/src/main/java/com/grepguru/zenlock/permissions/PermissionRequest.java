package com.grepguru.zenlock.permissions;

import com.grepguru.zenlock.R;
import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PermissionRequest {

    public static final class Requirement {
        public final AppPermission permission;
        public final boolean required;

        Requirement(AppPermission permission, boolean required) {
            this.permission = permission;
            this.required = required;
        }
    }

    public final String title;
    public final String actionLabel;
    public final int titleRes;
    public final int actionLabelRes;
    private final List<Requirement> requirements;

    private PermissionRequest(String title, String actionLabel, int titleRes, int actionLabelRes, List<Requirement> requirements) {
        this.title = title;
        this.actionLabel = actionLabel;
        this.titleRes = titleRes;
        this.actionLabelRes = actionLabelRes;
        this.requirements = Collections.unmodifiableList(requirements);
    }

    public List<Requirement> applicable(Context context) {
        List<Requirement> result = new ArrayList<>();
        for (Requirement requirement : requirements) {
            if (requirement.permission.appliesTo(context)) result.add(requirement);
        }
        return result;
    }

    public boolean requiredGranted(Context context) {
        for (Requirement requirement : applicable(context)) {
            if (requirement.required && !requirement.permission.isGranted(context)) return false;
        }
        return true;
    }

    Bundle toBundle() {
        Bundle state = new Bundle();
        state.putString("title", title);
        state.putInt("title_res", titleRes);
        state.putInt("action_res", actionLabelRes);
        state.putString("action", actionLabel);
        ArrayList<String> required = new ArrayList<>();
        ArrayList<String> recommended = new ArrayList<>();
        for (Requirement item : requirements) {
            (item.required ? required : recommended).add(item.permission.name());
        }
        state.putStringArrayList("required", required);
        state.putStringArrayList("recommended", recommended);
        return state;
    }

    static PermissionRequest fromBundle(Bundle state) {
        Builder builder = state.getInt("title_res") != 0
                ? titled(state.getInt("title_res")) : titled(state.getString("title", ""));
        if (state.getInt("action_res") != 0) builder.action(state.getInt("action_res"));
        else builder.action(state.getString("action", ""));
        for (String name : state.getStringArrayList("required")) builder.require(AppPermission.valueOf(name));
        for (String name : state.getStringArrayList("recommended")) builder.recommend(AppPermission.valueOf(name));
        return builder.build();
    }

    public static Builder titled(String title) {
        return new Builder(title);
    }

    public static Builder titled(int titleRes) {
        Builder builder = new Builder(null);
        builder.titleRes = titleRes;
        return builder;
    }

    public static final class Builder {
        private final String title;
        private String actionLabel;
        private int titleRes;
        private int actionLabelRes = R.string.background_continue;
        private final List<Requirement> requirements = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder action(String label) {
            actionLabel = label;
            actionLabelRes = 0;
            return this;
        }

        public Builder action(int labelRes) {
            actionLabel = null;
            actionLabelRes = labelRes;
            return this;
        }

        public Builder require(AppPermission permission) {
            requirements.add(new Requirement(permission, true));
            return this;
        }

        public Builder recommend(AppPermission permission) {
            requirements.add(new Requirement(permission, false));
            return this;
        }

        public PermissionRequest build() {
            return new PermissionRequest(title, actionLabel, titleRes, actionLabelRes, requirements);
        }
    }
}
