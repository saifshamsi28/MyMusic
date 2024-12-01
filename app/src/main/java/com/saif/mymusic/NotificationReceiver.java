package com.saif.mymusic;

import static com.saif.mymusic.ApplicationClass.ACTION_NEXT;
import static com.saif.mymusic.ApplicationClass.ACTION_PLAY;
import static com.saif.mymusic.ApplicationClass.ACTION_PREVIOUS;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String clickedActionButtonName=intent.getAction();
        Intent serviceIntent=new Intent(context,MusicService.class);
        if(clickedActionButtonName!=null){
            switch (clickedActionButtonName){
                case ACTION_PLAY:
                    serviceIntent.putExtra("ActionName","playPause");
                    context.startService(serviceIntent);
                    break;
                case ACTION_PREVIOUS:
                    serviceIntent.putExtra("ActionName","previous");
                    context.startService(serviceIntent);
                    break;
                case ACTION_NEXT:
                    serviceIntent.putExtra("ActionName","next");
                    context.startService(serviceIntent);
                    break;
            }
        }
    }
}
