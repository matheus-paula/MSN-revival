package com.app.messenger.messenger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CommonMethods {
    static void requestAppPermissions(Context c, Activity a) {
        if (hasReadPermissions(c) && hasWritePermissions(c) && hasInternetPermissions(c)) {
            return;
        }
        ActivityCompat.requestPermissions(a,
            new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
            }, 1);
    }

    private static boolean hasReadPermissions(Context c) {
        return (ContextCompat.checkSelfPermission(c,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private static boolean hasInternetPermissions(Context c) {
        return (ContextCompat.checkSelfPermission(c,
                Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED);
    }

    private static boolean hasWritePermissions(Context c) {
        return (ContextCompat.checkSelfPermission(c,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    static String getStaticMap(String lat, String lon) {
        String mapUrl = "https://open.mapquestapi.com/staticmap/v5/map?key=Kn5ElD87GXB4ggI7G5MaFMvZcRnOvAPy&locations=%s,%s&zoom=15&size=250,150&scale=2@2x";
        return String.format(mapUrl,lat,lon);
    }

    public static void createChat(String contactId, String userName, String bio,
                                  String photo, String status, String userEmail, Context context) {
        Intent intent = new Intent(context, ChatScreenActivity.class);
        intent.putExtra("contactId", contactId);
        intent.putExtra("photo", photo);
        intent.putExtra("bio", bio);
        intent.putExtra("status", status);
        intent.putExtra("userName", userName);
        intent.putExtra("userEmail", userEmail);
        context.startActivity(intent);
    }

    static String getUtcTimestamp() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        @SuppressLint("SimpleDateFormat")
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        return df.format(new Date());
    }

    static Date fromISO8601UTC(String dateStr) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.getDefault());
        df.setTimeZone(tz);
        try {
            return df.parse(dateStr);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static int[] getStatusFrame(String status){
        switch (status) {
            case "online":
                return new int[]{R.drawable.profile_status_online,0};
            case "absent":
                return new int[]{R.drawable.profile_status_absent,1};
            case "busy":
                return new int[]{R.drawable.profile_status_busy,2};
            case "offline":
                return new int[]{R.drawable.profile_status_offline,3};
            default:
                return new int[]{R.drawable.profile_status_offline,3};
        }
    }

    static int getNotificationIcon() {
        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return useWhiteIcon ? R.drawable.ic_flat_msn_messenger : R.drawable.ic_flat_msn_messenger_color;
    }

    static Bitmap statusStampPhoto(Bitmap imgA, Bitmap imgB){
        Bitmap resultBitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(Bitmap.createScaledBitmap(imgA, 100, 100, false), 25, 25, paint);
        canvas.drawBitmap(Bitmap.createScaledBitmap(imgB, 150, 150, false), 0, 0, paint);
        return resultBitmap;
    }

}
