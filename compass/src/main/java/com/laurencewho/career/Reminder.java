package com.laurencewho.career;
import android.app.*;import android.content.*;import android.os.Build;import org.json.*;import java.util.*;
/** Reminders for saved calls: one inexact alarm per saved call at 9:00, N days before its deadline. Rescheduled on boot and whenever saved/settings change. */
public class Reminder extends BroadcastReceiver{
 static final String CH="deadlines";
 @Override public void onReceive(Context c,Intent i){String a=i.getAction();
  if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)){schedule(c);return;}
  try{String t=i.getStringExtra("title");String dl=i.getStringExtra("deadline");NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm==null)return;
   if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CH,"deadlines",NotificationManager.IMPORTANCE_DEFAULT));
   PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,Main.class),PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CH):new Notification.Builder(c);
   b.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("deadline "+dl).setContentText(t).setContentIntent(open).setAutoCancel(true);
   nm.notify(Math.abs((t+dl).hashCode()),b.build());}catch(Exception e){}}
 static void schedule(Context c){try{SharedPreferences sp=c.getSharedPreferences("c",0);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;
   JSONObject saved=new JSONObject(sp.getString("saved","{}"));
   // cancel old
   for(String id:sp.getStringSet("alarms",new HashSet<>()))am.cancel(pi(c,id,null,null));
   Set<String> now=new HashSet<>();
   if(sp.getBoolean("remind",true)){int days=sp.getInt("remindDays",7);
    for(Iterator<String> it=saved.keys();it.hasNext();){String id=it.next();JSONObject o=saved.optJSONObject(id);if(o==null)continue;
     try{Calendar k=Calendar.getInstance();k.setTime(new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(o.optString("deadline")));k.add(Calendar.DAY_OF_MONTH,-days);k.set(Calendar.HOUR_OF_DAY,9);
      if(k.getTimeInMillis()<System.currentTimeMillis())continue;am.set(AlarmManager.RTC_WAKEUP,k.getTimeInMillis(),pi(c,id,o.optString("title"),o.optString("deadline")));now.add(id);}catch(Exception e){}}}
   sp.edit().putStringSet("alarms",now).apply();}catch(Exception e){}}
 static PendingIntent pi(Context c,String id,String t,String dl){Intent i=new Intent(c,Reminder.class).setAction("remind."+id).putExtra("title",t).putExtra("deadline",dl);return PendingIntent.getBroadcast(c,id.hashCode(),i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
}
