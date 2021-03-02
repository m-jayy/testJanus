package in.minewave.janusvideoroom;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class Permissons {
    //Request Permisson
    public static void Request_STORAGE(Activity act, int code)
    {

        ActivityCompat.requestPermissions(act, new
                String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},code);
    }
    public static void Request_CAMERA(Activity act,int code)
    {
        ActivityCompat.requestPermissions(act, new
                String[]{Manifest.permission.CAMERA},code);
    }
    public static void Request_AUDIO(Activity act,int code)
    {
        ActivityCompat.requestPermissions(act, new
                String[]{Manifest.permission.RECORD_AUDIO},code);
    }

    //Check Permisson
    public static boolean Check_STORAGE(Activity act)
    {
        int result = ContextCompat.checkSelfPermission(act,android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean Check_CAMERA(Activity act)
    {
        int result = ContextCompat.checkSelfPermission(act, Manifest.permission.CAMERA);
        return result == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean Check_AUDIO(Activity act)
    {
        int result = ContextCompat.checkSelfPermission(act, Manifest.permission.RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }

}
