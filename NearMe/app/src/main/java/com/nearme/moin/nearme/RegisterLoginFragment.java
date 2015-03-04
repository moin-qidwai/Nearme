package com.nearme.moin.nearme;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * Created by moin on 2/25/15.
 */
public class RegisterLoginFragment extends Fragment {

    private OnRegisterLoginButtonListener listener;
    private Activity mActivity;

    private static final String TAG = "RegisterLoginFragment";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
        try {
            listener = (OnRegisterLoginButtonListener) activity;
        }
        catch (ClassCastException e)
        {
            e.printStackTrace();
        }
    }

    public RegisterLoginFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_registerlogin, container, false);

        Button loginButton = (Button) view.findViewById(R.id.login_button);
        Button registerButton = (Button) view.findViewById(R.id.register_button);

        loginButton.setOnClickListener(new buttonsListener());
        registerButton.setOnClickListener(new buttonsListener());

        return view;
    }

    private class buttonsListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            int type = 1;
            if(v.getId() == R.id.login_button)
            {
                type = 1;
            }
            else if(v.getId() == R.id.register_button)
            {
                type = 2;
            }
            listener.onButtonClicked(type);
        }
    }


}




