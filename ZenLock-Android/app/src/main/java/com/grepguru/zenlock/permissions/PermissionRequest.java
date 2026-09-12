package com.grepguru.zenlock.permissions;

import android.content.Context;

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
    private final List<Requirement> requirements;

    private PermissionRequest(String title, List<Requirement> requirements) {
        this.title = title;
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

    public static Builder titled(String title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final String title;
        private final List<Requirement> requirements = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
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
            return new PermissionRequest(title, requirements);
        }
    }
}
