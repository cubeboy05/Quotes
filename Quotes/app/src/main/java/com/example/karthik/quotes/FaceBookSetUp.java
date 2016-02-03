package com.example.karthik.quotes;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FaceBookSetUp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        printHashKey();
    }

    public void printHashKey(){
        try {
            PackageInfo info = null;
            try {
                info = getPackageManager().getPackageInfo(
                        "com.example.karthik.quotes",
                        PackageManager.GET_SIGNATURES);
            } catch (PackageManager.NameNotFoundException e1) {
                e1.printStackTrace();
            }
            for (Signature signature : info.signatures) {
                MessageDigest md = null;
                try {
                    md = MessageDigest.getInstance("SHA");
                } catch (NoSuchAlgorithmException e1) {
                    e1.printStackTrace();
                }
                md.update(signature.toByteArray());
                Log.d("KeyHashzx:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
