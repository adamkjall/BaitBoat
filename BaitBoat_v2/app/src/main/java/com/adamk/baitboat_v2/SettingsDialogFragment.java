package com.adamk.baitboat_v2;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

public class SettingsDialogFragment extends DialogFragment {

    private EditText mIpAddress;
    private EditText mPort;

    public SettingsDialogFragment(){

    }

    public static SettingsDialogFragment newInstance(String title) {
        SettingsDialogFragment frag = new SettingsDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        mIpAddress = (EditText) view.findViewById(R.id.et_ipAddress);
        mPort = (EditText) view.findViewById(R.id.et_ipAddress);
        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title");
        getDialog().setTitle(title);
        // Request focus to field
        mIpAddress.requestFocus();
    }
}
