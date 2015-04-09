package com.nearme.moin.nearme;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

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
public class LoginFragment extends Fragment {

    private EditText userName;
    private static final String TAG = "LoginFragment";
    private String regid;
    private String PROJECT_NUMBER = "378063123871";
    GoogleCloudMessaging gcm;

    // url to get all products list
    private final static String sendUrl = "http://nearme-env.elasticbeanstalk.com/login.php";

    private final JSONObject sendObj = new JSONObject();

    // Local storage user info db instance
    private UserDbAdapter mDbHelper;

    // Progress Dialog
    private ProgressDialog pDialog;

    public LoginFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);

        userName = (EditText) view.findViewById(R.id.login_userName);
        mDbHelper = new UserDbAdapter(getActivity().getApplicationContext());

        getRegId();

        Button submit = (Button) view.findViewById(R.id.login_submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create the user in local storage and then check if user exists with this number in remote storage if so then fetch data from remote and proceed to main activity
                if (!isNetworkConnected()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("Please connect to the internet before trying to login.")
                            .setTitle("No Internet Access");

                    builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (!isNetworkConnected()) {
                                AlertDialog talert = builder.create();
                                talert.show();
                            } else {
                                final String username = userName.getText().toString();
                                try {
                                    sendObj.put("username", username);
                                    sendObj.put("gcmId", regid);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                Runnable r = new Runnable() {

                                    @Override
                                    public void run() {
                                        JSONObject retObj = SendHttpPost(sendUrl, sendObj);
                                        try {
                                            if (retObj.getInt("status") == 1)
                                            {
                                                Intent i = new Intent(getActivity(), MainActivity.class);
                                                i.putExtra("ID", retObj.getInt("ID"));
                                                // save data to local
                                                mDbHelper.open();
                                                mDbHelper.createUser(retObj.getInt("ID"), retObj.getString("name"), retObj.getString("number"), retObj.getString("countryCode"), retObj.getString("displayName"), retObj.getString("gcmId"));
                                                mDbHelper.close();
                                                getActivity().finish();
                                                startActivity(i);
                                            }
                                            else
                                            {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getActivity(), "The entered username does not exist. Please try again or register.", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    public JSONObject SendHttpPost(String URL, JSONObject jsonObjSend) {

                                        try {
                                            DefaultHttpClient httpclient = new DefaultHttpClient();
                                            HttpPost httpPostRequest = new HttpPost(URL);

                                            StringEntity se;
                                            se = new StringEntity(jsonObjSend.toString(), HTTP.UTF_8);
                                            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
                                                    "application/json"));

                                            httpPostRequest.setEntity(se);
                                            httpPostRequest.setHeader("Accept", "application/json");
                                            httpPostRequest.setHeader("Content-type", "application/json");
                                            httpPostRequest.setHeader("Accept-Encoding", "gzip");

                                            long t = System.currentTimeMillis();
                                            HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
                                            Log.i(TAG, "HTTPResponse received in [" + (System.currentTimeMillis() - t) + "ms]");

                                            HttpEntity entity = response.getEntity();

                                            if (entity != null) {
                                                InputStream instream = entity.getContent();
                                                Header contentEncoding = response.getFirstHeader("Content-Encoding");
                                                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                                                    instream = new GZIPInputStream(instream);
                                                }

                                                String resultString = convertStreamToString(instream);
                                                instream.close();
                                                resultString = resultString.substring(0, resultString.length() - 1);
                                                // try to find why the next line is giving an error and how to get the jsonObject in php
                                                JSONObject jsonObjRecv = new JSONObject(resultString);
                                                Log.i(TAG, "<JSONObject>\n" + jsonObjRecv.toString() + "\n</JSONObject>");

                                                return jsonObjRecv;
                                            }

                                        } catch (Exception e) {
                                            Log.e("Exception", "Exception");
                                            e.printStackTrace();
                                        }
                                        return null;
                                    }


                                    private String convertStreamToString(InputStream is) {
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                                        StringBuilder sb = new StringBuilder();

                                        String line = null;
                                        try {
                                            while ((line = reader.readLine()) != null) {
                                                sb.append(line + "\n");
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        } finally {
                                            try {
                                                is.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        return sb.toString();
                                    }
                                };

                                new Thread(r).start();
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else
                {
                    final String username = userName.getText().toString();
                    try {
                        sendObj.put("username", username);
                        sendObj.put("gcmId", regid);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Runnable r = new Runnable() {

                        @Override
                        public void run() {
                            JSONObject retObj = SendHttpPost(sendUrl, sendObj);
                            try {
                                if (retObj.getInt("status") == 1)
                                {
                                    Intent i = new Intent(getActivity(), MainActivity.class);
                                    i.putExtra("ID", retObj.getInt("ID"));
                                    // save data to local
                                    mDbHelper.open();
                                    mDbHelper.createUser(retObj.getInt("ID"), retObj.getString("name"), retObj.getString("number"), retObj.getString("countryCode"), retObj.getString("displayName"), retObj.getString("gcmId"));
                                    mDbHelper.close();
                                    getActivity().finish();
                                    startActivity(i);
                                }
                                else
                                {
                                    getActivity().runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(getActivity(), "The entered username does not exist. Please try again or register.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        public JSONObject SendHttpPost(String URL, JSONObject jsonObjSend) {

                            try {
                                DefaultHttpClient httpclient = new DefaultHttpClient();
                                HttpPost httpPostRequest = new HttpPost(URL);

                                StringEntity se;
                                se = new StringEntity(jsonObjSend.toString(), HTTP.UTF_8);
                                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
                                        "application/json"));

                                httpPostRequest.setEntity(se);
                                httpPostRequest.setHeader("Accept", "application/json");
                                httpPostRequest.setHeader("Content-type", "application/json");
                                httpPostRequest.setHeader("Accept-Encoding", "gzip");

                                long t = System.currentTimeMillis();
                                HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
                                Log.i(TAG, "HTTPResponse received in [" + (System.currentTimeMillis() - t) + "ms]");

                                HttpEntity entity = response.getEntity();

                                if (entity != null) {
                                    InputStream instream = entity.getContent();
                                    Header contentEncoding = response.getFirstHeader("Content-Encoding");
                                    if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                                        instream = new GZIPInputStream(instream);
                                    }

                                    String resultString = convertStreamToString(instream);
                                    instream.close();
                                    resultString = resultString.substring(0, resultString.length() - 1);
                                    // try to find why the next line is giving an error and how to get the jsonObject in php
                                    JSONObject jsonObjRecv = new JSONObject(resultString);
                                    Log.i(TAG, "<JSONObject>\n" + jsonObjRecv.toString() + "\n</JSONObject>");

                                    return jsonObjRecv;
                                }

                            } catch (Exception e) {
                                Log.e("Exception", "Exception");
                                e.printStackTrace();
                            }
                            return null;
                        }


                        private String convertStreamToString(InputStream is) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            StringBuilder sb = new StringBuilder();

                            String line = null;
                            try {
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line + "\n");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            return sb.toString();
                        }
                    };

                    new Thread(r).start();
                }
            }
        });

        return view;
    }

    public void getRegId(){
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getActivity().getApplicationContext());
                    }
                    regid = gcm.register(PROJECT_NUMBER);
                    msg = "Device registered, registration ID=" + regid;
                    Log.i("GCM",  msg);

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();

                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i("GCM", "message");
            }
        }.execute(null, null, null);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

}




