package com.grepguru.zenlock.quotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.grepguru.zenlock.R;
import com.grepguru.zenlock.ui.Sheets;

public class QuotesSheet extends BottomSheetDialogFragment {

    private static final String TAG = "QuotesSheet";

    private LinearLayout rows;
    private EditText input;
    private MaterialButton addButton;

    public static void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.findFragmentByTag(TAG) != null) return;
        new QuotesSheet().show(manager, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_quotes, container, false);
        rows = view.findViewById(R.id.quoteRows);
        input = view.findViewById(R.id.quoteInput);
        addButton = view.findViewById(R.id.quoteAdd);
        addButton.setOnClickListener(v -> addQuote());
        render();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Sheets.expandAboveKeyboard(getDialog());
    }

    private void addQuote() {
        if (!QuoteStore.add(requireContext(), input.getText().toString())) return;
        input.setText("");
        render();
    }

    private void render() {
        rows.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String quote : QuoteStore.all(requireContext())) {
            View row = inflater.inflate(R.layout.item_quote, rows, false);
            ((TextView) row.findViewById(R.id.quoteText)).setText(quote);
            row.findViewById(R.id.quoteRemove).setOnClickListener(v -> {
                QuoteStore.remove(requireContext(), quote);
                render();
            });
            rows.addView(row);
        }
        boolean canAdd = QuoteStore.canAdd(requireContext());
        input.setEnabled(canAdd);
        addButton.setEnabled(canAdd);
        input.setHint(canAdd ? "New quote" : QuoteStore.MAX_QUOTES + " of " + QuoteStore.MAX_QUOTES);
    }
}
