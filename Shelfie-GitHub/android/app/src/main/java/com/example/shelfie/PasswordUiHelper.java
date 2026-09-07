package com.example.shelfie;

import android.content.Context;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public final class PasswordUiHelper {

    private PasswordUiHelper() {
    }

    public static void updateRequirementLabels(Context ctx, PasswordRules.Result r,
            TextView lengthRow,
            TextView upperRow,
            TextView lowerRow,
            TextView specialRow) {
        bindRow(ctx, lengthRow, r.minLength, R.string.password_rule_length);
        bindRow(ctx, upperRow, r.hasUpperCase, R.string.password_rule_upper);
        bindRow(ctx, lowerRow, r.hasLowerCase, R.string.password_rule_lower);
        bindRow(ctx, specialRow, r.hasSpecialChar, R.string.password_rule_special);
    }

    private static void bindRow(Context ctx, TextView tv, boolean met, int labelRes) {
        String marker = ctx.getString(met ? R.string.password_req_met_marker : R.string.password_req_unmet_marker);
        tv.setText(ctx.getString(R.string.password_rule_row, marker, ctx.getString(labelRes)));
        tv.setTextColor(ContextCompat.getColor(ctx, met ? R.color.primary : R.color.on_surface_variant));
    }
}
