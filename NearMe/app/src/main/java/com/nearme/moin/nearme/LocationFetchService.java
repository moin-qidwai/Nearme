package com.nearme.moin.nearme;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class LocationFetchService extends Service {

    private String TAG = "LocationFetchService";

    // url to get all products list
    private final static String sendUrl = "http://nearme-env.elasticbeanstalk.com/fetch_location.php";

    public static final String RESULT = "FETCHEDRESULTS";

    private final JSONObject sendObj = new JSONObject();

    private LocalBroadcastManager broadcaster;

    private UserDbAdapter userDbHelper;
    private UserRelationDbAdapter userRelationDbHelper;

    List<Integer> targetIdList;

    public LocationFetchService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        broadcaster = LocalBroadcastManager.getInstance(this);

        userDbHelper = new UserDbAdapter(getApplicationContext());
        userDbHelper.open();

        Cursor userCursor = userDbHelper.fetchAllUsers();
        userCursor.moveToFirst();

        targetIdList = new ArrayList<>();
        userRelationDbHelper = new UserRelationDbAdapter(getApplicationContext());
        userRelationDbHelper.open();

        Cursor userRelationCursor = userRelationDbHelper.fetchAllRelations();
        while(userRelationCursor.moveToNext())
        {
            targetIdList.add(userRelationCursor.getInt(1));
        }

        StringBuilder builder = new StringBuilder();
        builder.append( targetIdList.remove(0));

        for( Integer s : targetIdList) {
            builder.append( ", ");
            builder.append( s);
        }

        try {
            sendObj.put("sourceId", userCursor.getInt(1));
            sendObj.put("targetIds", builder.toString());
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
                            int sourceId =  retObj.getInt("sourceId");
                            double sourceLat = retObj.getDouble("sourceLat");
                            double sourceLon = retObj.getDouble("sourceLon");
                            CharSequence[] targetIds = retObj.getString("targetIds").split(",");
                            CharSequence[] lats = retObj.getString("lats").split(",");
                            CharSequence[] lons = retObj.getString("lons").split(",");
                            sendResult("success", targetIds, lats, lons, sourceLat, sourceLon);
                            stopSelf();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }

            public void sendResult(String message, CharSequence[] targetIds, CharSequence[] lats, CharSequence[] lons, double sourceLat, double sourceLon) {
                Intent intent = new Intent(RESULT);
                if(message != null)
                {
                    intent.putExtra("status", message);
                    intent.putExtra("targetIds", targetIds);
                    intent.putExtra("lats", lats);
                    intent.putExtra("lons", lons);
                    intent.putExtra("sourceLat", sourceLat);
                    intent.putExtra("sourceLon", sourceLon);
                }
                broadcaster.sendBroadcast(intent);
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
