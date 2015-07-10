package com.meow.thaithien.dropboxuploader;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.InputStream;

/**
 * Created by Thien on 7/10/2015.
 */
public class DropboxUploader {

    private Context context;
    private String DROPBOX_LOG_TAG = DropboxUploader.class.getSimpleName();

    //Dropbox api
    private String Dropbox_AppId = null;
    private String Dropbox_AppSecret = null;
    DropboxAPI<AndroidAuthSession> Dropbox_mApi = null;
    String Dropbox_token = null;

    // for a file
   private     String File_Name = null;
    private   Long File_length = null;
    private  InputStream inputStream = null;
    private  Handler dropbox_mHandler = null;


    //flag
    boolean error_Dropbox =false;
    boolean busy = false;


    //frefs
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;


    public DropboxUploader(Context context, String dropbox_AppId, String dropbox_AppSecret) {
        this.context = context;
        Dropbox_AppId = dropbox_AppId;
        Dropbox_AppSecret = dropbox_AppSecret;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
    }

    /*
 * LoginDropbox will open device web browser, ask user to login, ask for permission, and store dropbox_token
 * After login successfully with LoginDropbox ( use  public boolean Dropbox_isLogin() to check)
 *   you can upload thing to dropbox
 * */
    public void LoginDropbox()
    {
        Intent intent = new Intent(context,LoginDropboxActivity.class);
        intent.putExtra(context.getResources().getString(R.string.extra_dropbox_app_id_request),Dropbox_AppId);
        intent.putExtra(context.getResources().getString(R.string.extra_dropbox_app_secret_request),Dropbox_AppSecret);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public boolean Dropbox_isLogin(){
        //TODO: test Dropbox_isLogin
        Log.d(DROPBOX_LOG_TAG, "start_isLogin");
        String tmp_DP_TOKEN = prefs.getString(context.getResources().getString(R.string.prefs_dropbox_token) ,null);
        Log.d(DROPBOX_LOG_TAG,"isLogin token key ="+tmp_DP_TOKEN);
        //if no dp_token then dropbox is not login
        if (tmp_DP_TOKEN==null) {
            return false;
        }
        //check if dropbox token is valid
        AndroidAuthSession session = buildSession();
        Dropbox_mApi = new DropboxAPI<AndroidAuthSession>(session);
        Dropbox_mApi.getSession().setOAuth2AccessToken(tmp_DP_TOKEN);
        if (Dropbox_mApi.getSession().isLinked()) {
            Log.d(DROPBOX_LOG_TAG,"end_isLogin, return true");
            return true;
        }
        Log.d(DROPBOX_LOG_TAG, "end_isLogin return false");
        return false;
    }


    public void UploadFileDropbox(String Name, InputStream is, Long Length, Handler handler){
        if (busy){
            //some other upload is running
            dropbox_failed_handle();
            return;
        }

        busy_on();//set busy flag on

        this.File_Name = Name;
        this.File_length = Length;
        this.inputStream = is;
        this.dropbox_mHandler = handler;
        new LoginDropboxAndUpload().execute();
    }

    //path = myfolder/mysubfolder
    public void UploadFileDropbox(String Name,String path, InputStream is, Long Length, Handler handler){
        if (busy){
            //some other upload is running
            dropbox_failed_handle();
            return;
        }

        busy_on();

        this.File_Name = path+"/"+ Name;
        this.File_length = Length;
        this.inputStream = is;
        this.dropbox_mHandler = handler;
        new LoginDropboxAndUpload().execute();
    }

    //----------------DROP BOX HELPER Function--------------------------

    //build AndroidAuthSession
    private AndroidAuthSession buildSession() {
        // APP_KEY and APP_SECRET goes here
        AppKeyPair appKeyPair = new AppKeyPair(Dropbox_AppId,Dropbox_AppSecret );

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair, Dropbox_token);

        return session;
    }



    //set token to Dropbox_mApi
    private class LoginDropboxAndUpload extends AsyncTask<Void, Void, Void> {



        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            error_Dropbox=false;

        }

        @Override
        protected Void doInBackground(Void... params) {
            //get token
            Dropbox_token = prefs.getString(context.getResources().getString(R.string.prefs_dropbox_token), null);
            // bind APP_KEY and APP_SECRET with session and Access token
            AndroidAuthSession session = buildSession();
            Dropbox_mApi = new DropboxAPI<AndroidAuthSession>(session);

            if (Dropbox_token != null)
                Dropbox_mApi.getSession().setOAuth2AccessToken(Dropbox_token);

            if (Dropbox_token == null) {
                //dropbox not link because token is null
                // Login at Login Activity
                error_Dropbox = true;
                Log.e(DROPBOX_LOG_TAG, "Dropbox is not linked");
            }
            if (Dropbox_mApi.getSession().isLinked())
                Log.i(DROPBOX_LOG_TAG, "Dropbox is Link");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (Dropbox_mApi.getSession().isLinked())
                new UploadPicture_Dropbox().execute();

            if (error_Dropbox) {
                busy_off();//turn of busy flag
                String tmp = "Dropbox Upload Error. ";
                if (Dropbox_token == null) {
                    tmp = tmp + "Dropbox is not linked";
                }
                dropbox_failed_handle();
                Log.e(DROPBOX_LOG_TAG,tmp);
            }

        }
    }

    //Upload select picture (which is shown on image view to dropbox
    private class UploadPicture_Dropbox extends AsyncTask<Void, Long, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(DROPBOX_LOG_TAG,"Start uploading");
        }

        @Override
        protected Void doInBackground(Void... voids) {



            /////////////////////////
            try {

                CopyInputStream copyInputStream = new CopyInputStream(inputStream);//init copyInputStream
                inputStream = copyInputStream.getCopy();//restore source inputstream

                Log.d(DROPBOX_LOG_TAG, "File Name = " + File_Name);
                Log.d(DROPBOX_LOG_TAG, "File length = " + File_length);



                InputStream tmpis = copyInputStream.getCopy();//create tmp inputstream
                Dropbox_mApi.putFile(File_Name // path in drop box
                        , tmpis //input stream
                        , File_length //file.length()
                        ,null
                        ,null);


            }
            catch (DropboxException e){ e.printStackTrace();
                error_Dropbox = true;

            }
            return null;
        }



        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            busy_off();//turn of busy flag
            if (!error_Dropbox ) {
                Log.i(DROPBOX_LOG_TAG, "Dropbox Upload Complete");
                dropbox_ok_handle();
            }
            else {
                dropbox_failed_handle();

            }


        }
    }

    private void dropbox_failed_handle(){
        Message message = new Message();
        message.arg1 = -1;//error
        dropbox_mHandler.sendMessage(message);
        dropbox_mHandler=null;
    }

    private void dropbox_ok_handle(){
        Message message = new Message();
        message.arg1 = 1;//upload success
        dropbox_mHandler.sendMessage(message);
        dropbox_mHandler=null;
    }

    private void busy_on(){
        busy=true;
    }

    private void busy_off(){
        busy=false;
    }

}
