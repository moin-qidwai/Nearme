package com.nearme.moin.nearme;

/**
 * Created by moin on 3/14/15.
 */

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.nearme.moin.nearme.GcmBroadcastReceiver;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class GcmMessageHandler extends IntentService {

    String mes;
    private Handler handler;
    public GcmMessageHandler() {
        super("GcmMessageHandler");
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        handler = new Handler();
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        mes = extras.getString("message");
        Log.e("GCM", "The type was "+extras.getString("type")+" The message was "+extras.getString("message"));
        if(extras.getString("type").equals("message"))
        {
            String fromUser = extras.getString("fromuser");
            String msg = "Sutta";
            if(extras.getString("message").equals("sutta"))
            {
                msg = fromUser+": Lets have Sutta";

            }
            else if(extras.getString("message").equals("ebz"))
            {
                msg = fromUser+": Lets go to ebz";
            }
            else if(extras.getString("message").equals("su"))
            {
                msg = fromUser+": Lets go to su";
            }
            // prepare intent which is triggered if the
            // notification is selected

            Intent notIntent = new Intent(this, MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, notIntent, 0);

            // build notification
            // the addAction re-use the same intent to keep the example short
            NotificationCompat.Builder mBuilder  = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcherwhite)
                    .setContentTitle("You have a new message.")
                    .setContentText(msg)
                    .setAutoCancel(true)
                    .setContentIntent(pIntent)
                    .addAction(R.drawable.ic_launcherwhite, "OK", pIntent)
                    .addAction(R.drawable.ic_launcherwhite, "NO BITCH!", pIntent);


            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(0,mBuilder.build());

            Vibrator v = (Vibrator) getApplication().getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            v.vibrate(500);
        }
        else
        {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcherwhite)
                            .setContentTitle("Some friends are nearby.")
                            .setContentText(extras.getString("message"))
                            .setAutoCancel(true);
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, MainActivity.class);

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(MainActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            mNotificationManager.notify(0,mBuilder.build());
        }



        GcmBroadcastReceiver.completeWakefulIntent(intent);

    }

    public void showToast(){
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(),mes , Toast.LENGTH_LONG).show();
            }
        });

    }
}