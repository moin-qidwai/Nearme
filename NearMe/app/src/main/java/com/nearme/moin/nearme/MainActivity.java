package com.nearme.moin.nearme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;


public class MainActivity extends ListActivity {

    private int group1Id = 1;
    int addUserId = Menu.FIRST;

    Timer timer1;
    LocationManager lm;
    boolean gps_enabled=false;
    boolean network_enabled=false;
    Location sourceLocation;
    CharSequence[] targetIds;
    CharSequence[] lats;
    CharSequence[] lons;

    private ArrayList<Contact> listValues;
    private static final String TAG = "MainActivyt";

    // url to get all related users
    private final static String sendUrl = "http://nearme-env.elasticbeanstalk.com/find_related_users.php";

    // url to get all related users
    private final static String sendAddUserURL = "http://nearme-env.elasticbeanstalk.com/add_user.php";

    // Local storage user info db instance
    private UserRelationDbAdapter mDbHelper;
    private UserDbAdapter userDbHelper;

    private BroadcastReceiver receiver;

    private Activity mctx;

    private final JSONObject sendObj = new JSONObject();

    private AlertDialog.Builder messagesAlertDialog;
    ListView messagesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listValues = new ArrayList<Contact>();
        mctx = this;

        // this broadcast receiver listens to the fetch user relations service and updates the ui upon receiving the locations
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("status");
                targetIds = intent.getCharSequenceArrayExtra("targetIds");
                lats = intent.getCharSequenceArrayExtra("lats");
                lons = intent.getCharSequenceArrayExtra("lons");

                if (!isNetworkConnected()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getApplication());
                    builder.setMessage("Please connect to the internet.")
                            .setTitle("No Internet Access");

                    builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (!isNetworkConnected()) {
                                AlertDialog talert = builder.create();
                                talert.show();
                            } else {
                                getLocation(getApplicationContext());
                            }
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else
                {
                    getLocation(getApplicationContext());
                }

            }
        };

        mDbHelper = new UserRelationDbAdapter(this);
        mDbHelper.open();

        Cursor c = mDbHelper.fetchAllRelations();
        // if there are already relations saved in the local storage then load them without connecting to the remote server
        // else connect to the server and fetch the relations as well as create some based on users contacts
        if(c.getCount() > 0)
        {
            while(c.moveToNext())
            {
                Contact tempContact = new Contact();
                tempContact.setId(c.getInt(1));
                tempContact.setDisplayName(c.getString(3));
                listValues.add(tempContact);
            }
            Collections.sort(listValues);

            Intent locationUpdate = new Intent(mctx, LocationUpdateService.class);
            startService(locationUpdate);

            Intent fetchLocations = new Intent(mctx, LocationFetchService.class);
            startService(fetchLocations);
        }
        else
        {
            final int sourceId = getIntent().getIntExtra("ID", 0);

            ContentResolver mContentResolver = getContentResolver();
            Cursor cursor = mContentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null, null, null, null);

            final ArrayList<String> phoneNumbers = new ArrayList<>();

            while(cursor.moveToNext())
            {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor pCur = mContentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                        new String[]{id}, null);
                while (pCur.moveToNext()) {
                    String phone = pCur.getString(
                            pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumbers.add(phone);
                }
                pCur.close();

            }

            cursor.close();

            userDbHelper = new UserDbAdapter(this);
            userDbHelper.open();

            final Cursor userCursor = userDbHelper.fetchUser(sourceId);
            userCursor.moveToFirst();

            if (!isNetworkConnected()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getApplication());
                builder.setMessage("Please connect to the internet.")
                        .setTitle("No Internet Access");

                builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!isNetworkConnected()) {
                            AlertDialog talert = builder.create();
                            talert.show();
                        } else {
                            findRelatedUsers(phoneNumbers, sourceId, userCursor);
                        }

                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                });
            }
            else
            {
                findRelatedUsers(phoneNumbers, sourceId, userCursor);
            }
        }
    }

    private void findRelatedUsers(ArrayList<String> phoneNumbers, int sourceId, Cursor userCursor)
    {
            try {
                sendObj.put("numbers", phoneNumbers);
                sendObj.put("sourceId", sourceId);
                sendObj.put("countryCode", userCursor.getString(4));
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
                            String[] names = retObj.getString("names").split(",");
                            String[] idString = retObj.getString("ids").split(",");
                            String[] usernames = retObj.getString("usernames").split(",");
                            ArrayList<Integer> ids = new ArrayList<>();

                            if (!retObj.getString("ids").isEmpty())
                            {
                                for (int i=0;i<idString.length; i++)
                                {
                                    ids.add(i, Integer.parseInt(idString[i]));
                                }

                                for (int i=0;i<idString.length;i++)
                                {
                                    Contact tempContact = new Contact();
                                    tempContact.setId(ids.get(i).intValue());
                                    tempContact.setDisplayName(names[i]);
                                    listValues.add(tempContact);
                                    mDbHelper.createRelation(ids.get(i).intValue(), usernames[i], names[i]);
                                }

                                Collections.sort(listValues);
                            }
                            else
                            {
                                Log.e(TAG, "The returned id string in main activity is empty");
                            }

                            Intent locationUpdate = new Intent(mctx, LocationUpdateService.class);
                            startService(locationUpdate);

                            Intent fetchLocations = new Intent(mctx, LocationFetchService.class);
                            startService(fetchLocations);

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(group1Id, addUserId, addUserId, "Add Contact");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == 1)
        {
            // get prompts.xml view
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.prompts, null);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    this);

            // set prompts.xml to alertdialog builder
            alertDialogBuilder.setView(promptsView);

            final EditText userInput = (EditText) promptsView
                    .findViewById(R.id.editTextDialogUserInput);

            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Add",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    // get user input and set it to result
                                    // edit text

                                    if (!isNetworkConnected()) {
                                        final AlertDialog.Builder builder = new AlertDialog.Builder(getApplication());
                                        builder.setMessage("Please connect to the internet.")
                                                .setTitle("No Internet Access");

                                        builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                if (!isNetworkConnected()) {
                                                    AlertDialog talert = builder.create();
                                                    talert.show();
                                                } else {
                                                    addUser(userInput);
                                                }
                                            }
                                        });

                                        AlertDialog talert = builder.create();
                                        talert.show();
                                    }
                                    else
                                    {
                                        addUser(userInput);
                                    }


                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    dialog.cancel();
                                }
                            });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void addUser(EditText userInput) {
        final JSONObject sendAddObject = new JSONObject();

        userDbHelper = new UserDbAdapter(getApplicationContext());
        userDbHelper.open();

        final Cursor userCursor = userDbHelper.fetchAllUsers();
        userCursor.moveToFirst();

        try {
            sendAddObject.put("targetUsername", userInput.getText());
            sendAddObject.put("sourceId", userCursor.getInt(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Runnable r = new Runnable() {

            @Override
            public void run() {
                final JSONObject retObj = SendHttpPost(sendAddUserURL, sendAddObject);
                try {
                    if (retObj.getInt("status") == 1)
                    {
                        mctx.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(mctx, "User Added Succesfully", Toast.LENGTH_LONG).show();
                            }
                        });
                        UserRelationDbAdapter tempRelationDbAdapter = new UserRelationDbAdapter(mctx);
                        tempRelationDbAdapter.open();

                        Cursor tempC = tempRelationDbAdapter.fetchRelation(retObj.getInt("targetId"));
                        if (tempC.getCount() == 0)
                        {
                            tempRelationDbAdapter.createRelation(retObj.getInt("targetId"), retObj.getString("username"), retObj.getString("displayName"));
                        }
                    }
                    else if(retObj.getInt("status") == 0)
                    {
                        mctx.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    Toast.makeText(mctx, retObj.getString("message"), Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
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

    // when an item of the list is clicked
    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);
        Contact selectedItem = (Contact) getListView().getItemAtPosition(position);
        Log.e(TAG, selectedItem.getDisplayName() + " id is "+String.valueOf(selectedItem.getId()));

        ArrayList<String> messages = new ArrayList<>();
        messages.add("Lets Sutta");
        messages.add("Ebeneezers?");
        messages.add("SU?");
        messagesAlertDialog = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.popuplist, null);
        messagesAlertDialog.setView(convertView);
        messagesAlertDialog.setTitle("Messages");
        messagesListView = (ListView) convertView.findViewById(R.id.messageslist);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,messages);
        messagesListView.setAdapter(adapter);
        final long userid = selectedItem.getId();
        final AlertDialog ad = messagesAlertDialog.show();
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String message;
                switch (position)
                {
                    case 0:
                        message = "sutta";
                        break;
                    case 1:
                        message = "ebz";
                        break;
                    case 2:
                        message = "su";
                        break;
                    default:
                        message = "sutta";
                        break;
                }

                if (!isNetworkConnected()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getApplication());
                    builder.setMessage("Please connect to the internet.")
                            .setTitle("No Internet Access");

                    builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (!isNetworkConnected()) {
                                AlertDialog talert = builder.create();
                                talert.show();
                            } else {
                                sendMessage(userid, message);
                                ad.dismiss();
                            }
                        }
                    });

                    AlertDialog talert = builder.create();
                    talert.show();
                }
                else
                {
                    sendMessage(userid, message);
                    ad.dismiss();
                }
            }
        });

    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(LocationFetchService.RESULT));
    }

    private void sendMessage(long userId, String message)
    {

        UserDbAdapter ondemandUserDbHelper = new UserDbAdapter(getApplicationContext());
        ondemandUserDbHelper.open();

        Cursor userCursor = ondemandUserDbHelper.fetchAllUsers();
        userCursor.moveToFirst();

        try {
            sendObj.put("sourceId", userCursor.getInt(1));
            sendObj.put("targetId", userId);
            sendObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Runnable r = new Runnable() {

            @Override
            public void run() {
                JSONObject retObj = SendHttpPost("http://nearme-env.elasticbeanstalk.com/send_message.php", sendObj);

                // handle failure to send message
//                try {
//                    if (retObj.getInt("status") == 1)
//                    {
//
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
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

    private void uponLocationFetch(CharSequence[] targetIds,CharSequence[] lats, CharSequence[] lons, Location location )
    {
        if (location == null || lats[0].equals("") )
        {
            Log.e(TAG, "Location was not fetched");
            return;
        }
        final ArrayList<Contact> tempListValues = new ArrayList<>();
        for (int i =0; i<targetIds.length;i++)
        {
            Location targetLocation = new Location("TargetLocation");
            targetLocation.setLatitude(Double.parseDouble(lats[i].toString()));
            targetLocation.setLongitude(Double.parseDouble(lons[i].toString()));
            if(location.distanceTo(targetLocation) < 250.0)
            {
                for (int j=0;j<listValues.size();j++)
                {
                    Contact tempContact = listValues.get(j);
                    if (tempContact.getId() == Integer.parseInt(targetIds[i].toString()))
                    {
                        tempContact.setProximity(1);
                        tempListValues.add(tempContact);
                    }
                }
            }
            else if(location.distanceTo(targetLocation) < 500.0)
            {
                for (int j=0;j<listValues.size();j++)
                {
                    Contact tempContact = listValues.get(j);
                    if (tempContact.getId() == Integer.parseInt(targetIds[i].toString()))
                    {
                        tempContact.setProximity(2);
                        tempListValues.add(tempContact);
                    }
                }
            }
            else if(location.distanceTo(targetLocation) < 1000.0)
            {
                for (int j=0;j<listValues.size();j++)
                {
                    Contact tempContact = listValues.get(j);
                    if (tempContact.getId() == Integer.parseInt(targetIds[i].toString()))
                    {
                        tempContact.setProximity(3);
                        tempListValues.add(tempContact);
                    }
                }
            }
            else if(location.distanceTo(targetLocation) > 1000.0)
            {
                for (int j=0;j<listValues.size();j++)
                {
                    Contact tempContact = listValues.get(j);
                    if (tempContact.getId() == Integer.parseInt(targetIds[i].toString()))
                    {
                        tempContact.setProximity(4);
                        tempListValues.add(tempContact);
                    }
                }
            }
        }

        UserDbAdapter ondemandUserDbHelper = new UserDbAdapter(getApplicationContext());
        ondemandUserDbHelper.open();

        Cursor userCursor = ondemandUserDbHelper.fetchAllUsers();
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
                JSONObject retObj = SendHttpPost("http://nearme-env.elasticbeanstalk.com/update_location.php", sendObj);
                mctx.runOnUiThread(new Runnable() {
                    public void run() {
                        Collections.sort(tempListValues);
                        ContactAdapter tempMyAdapter = new ContactAdapter(mctx,
                                R.layout.user_row, tempListValues);

                        setListAdapter(tempMyAdapter);
                    }
                });

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

    public boolean getLocation(Context context)
    {

        if(lm==null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try{gps_enabled=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
        try{network_enabled=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

        //don't start listeners if no provider is enabled
        if(!gps_enabled && !network_enabled)
            return false;

        if(gps_enabled)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        if(network_enabled)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        timer1=new Timer();
        timer1.schedule(new GetLastLocation(), 20000);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
            timer1.cancel();
            uponLocationFetch(targetIds, lats, lons, location);
            lm.removeUpdates(this);
            lm.removeUpdates(locationListenerNetwork);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            timer1.cancel();
            uponLocationFetch(targetIds, lats, lons,  location);
            lm.removeUpdates(this);
            lm.removeUpdates(locationListenerGps);
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    class GetLastLocation extends TimerTask {
        @Override
        public void run() {
            lm.removeUpdates(locationListenerGps);
            lm.removeUpdates(locationListenerNetwork);

            Location net_loc=null, gps_loc=null;
            if(gps_enabled)
                gps_loc=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(network_enabled)
                net_loc=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            //if there are both values use the latest one
            if(gps_loc!=null && net_loc!=null){
                if(gps_loc.getTime()>net_loc.getTime())
                    uponLocationFetch(targetIds, lats, lons,  gps_loc);
                else
                    uponLocationFetch(targetIds, lats, lons,  net_loc);
                return;
            }

            if(gps_loc!=null){
                uponLocationFetch(targetIds, lats, lons,  gps_loc);
                return;
            }
            if(net_loc!=null){
                uponLocationFetch(targetIds, lats, lons,  net_loc);
                return;
            }
            uponLocationFetch(targetIds, lats, lons,  null);
            return;
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

}
