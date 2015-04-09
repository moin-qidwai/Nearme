package com.nearme.moin.nearme;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ArrayAdapter;

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
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class LocationUpdateService extends Service {

    private String TAG = "LocationUpdateService";
    private LocationManager mLocationManager;

    // url to get all products list
    private final static String sendUrl = "http://nearme-env.elasticbeanstalk.com/update_location.php";

    private final JSONObject sendObj = new JSONObject();

    private UserDbAdapter userDbHelper;

    public LocationUpdateService() {
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {

            userDbHelper = new UserDbAdapter(getApplicationContext());
            userDbHelper.open();

            Log.e(TAG, "Location update inititated.");

            Cursor userCursor = userDbHelper.fetchAllUsers();
            userCursor.moveToFirst();

            try {
                sendObj.put("sourceId", userCursor.getInt(1));
                sendObj.put("lat", location.getLatitude());
                sendObj.put("lon", location.getLongitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    JSONObject retObj = SendHttpPost(sendUrl, sendObj);
                    // handle failure to update location
//                    try {
//                        if (retObj.getInt("status") == 1)
//                        {
//
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
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
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 7200000,
                    500, mLocationListener);
        }
        else
        {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 7200000,
                    500, mLocationListener);
        }

    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "The location update service was just destroyed.");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
