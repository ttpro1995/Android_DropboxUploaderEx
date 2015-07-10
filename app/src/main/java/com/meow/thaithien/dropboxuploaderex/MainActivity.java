package com.meow.thaithien.dropboxuploaderex;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.meow.thaithien.createfilelib.CreateFile;
import com.meow.thaithien.dropboxuploader.DropboxUploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

//mxv552yhrlifveu
//cijevly717n7gwg

public class MainActivity extends ActionBarActivity {

    DropboxUploader dropboxUploader = null;
    String id = "mxv552yhrlifveu";
    String secret = "cijevly717n7gwg";

    Button Login_bt;
    Button Upload_bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Init();

    Login_bt = (Button) findViewById(R.id.Login);
    Upload_bt = (Button) findViewById(R.id.upload);



        Login_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });

        Upload_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadTempFile("Meow","meow meow and mewo");
            }
        });

    }

    private void Init(){
        dropboxUploader = new DropboxUploader(MainActivity.this,id,secret);
    }

    private void Login(){
        dropboxUploader.LoginDropbox();
    }

    private void uploadTempFile(String name,String body){
        CreateFile tmpfilecreator = new CreateFile(name,body,MainActivity.this);//create a file
        File tmp_file =tmpfilecreator.getFile();
        InputStream is = null;
        try {
            is = new FileInputStream(tmp_file);
        }catch (Exception e){e.printStackTrace();}

        Handler dp_handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int a = msg.arg1;
                if (a==1)
                {
                    Log.i("DP","UPLOAD Succeesfull");
                }
            }
        };


        //dropboxUploader.UploadFileDropbox(tmp_file.getName(),is,tmp_file.length(),dp_handler);
        dropboxUploader.UploadFileDropbox(tmp_file.getName(),"myMeowParent/myMeowChild",is,tmp_file.length(),dp_handler);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

        return super.onOptionsItemSelected(item);
    }
}
