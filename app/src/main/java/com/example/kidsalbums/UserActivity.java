package com.example.kidsalbums;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kidsalbums.BackgroundService.BackgroundWorker;
import com.example.kidsalbums.DropBox.UploadAsyncTask;
import com.example.kidsalbums.Modules.Child;
import com.example.kidsalbums.Modules.Kindergarten;
import com.example.kidsalbums.Modules.User;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class UserActivity extends AppCompatActivity {

    Button addPhotoBtn;
    Button logOutBtn;
    TextView childName_textView;
    TextView userName_textView;
    TextView userPhone_textView;
    TextView userEmail_textView;
    TextView kindergartenName_textView;
    TextView kindergartenAddress_textView;
    TextView kindergartenEmail_textView;
    ImageView childPhoto_imageView;
    static String childName;
    static String userName;
    static String userPhone;
    static String userEmail;
    static String kgName;
    static String kgAddress;
    static String kgEmail;
    static User currentUser;
    private static int RESULT_LOAD_IMAGE = 1;
    SharedPreferences sp;
    //SharedPreferences spLoadedChildPhoto;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        initViews();
        Intent intent = getIntent();
        setViews(intent);
        setObjects(intent);
        checkExternalStoragePermission();

        String kindergartenAddress = currentUser.getKindergarten().getAddress();
        double lat = currentUser.getKindergarten().getLatitude();
        double lon = currentUser.getKindergarten().getLongitude();
        String userDetailsString  = kindergartenAddress + "_" + String.valueOf(lat) + "_" + String.valueOf(lon) + "_" + currentUser.getChild().getTag() + "_" + currentUser.getChild().getName() + "_" + currentUser.getPhoneNumber() + "_" + currentUser.getEmail();
        sp.edit().putString("user_details",userDetailsString).apply();

        Data data = new Data.Builder()
                .putString(BackgroundWorker.EXTRA_USER_DETAILS, userDetailsString).build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build();

        /* this inits a periodicWorkRequest - a request for background worker to do every time interval (we set the interval) */
        final PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(BackgroundWorker.class, 4, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag("periodic_work")
                .build();

        /* This starts the periodicWorkRequest */

        //First implementation:
        //WorkManager.getInstance().enqueue(periodicWorkRequest);

        //Second implementation with 'ExistingPeriodicWorkPolicy.KEEP'
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueueUniquePeriodicWork("my_background_worker", ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);

        addPhotoBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //TODO: SELECT PHOTO FROM GALLERY
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        logOutBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO sharedPreferences
                SharedPreferences preferences =getSharedPreferences("CurrentSession", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    public void setObjects(Intent intent){
        Kindergarten kindergarten = new Kindergarten(
                intent.getStringExtra("kindergartenId"),
                intent.getStringExtra("kindergartenName"),
                intent.getStringExtra("kindergartenAddress"),
                intent.getStringExtra("kindergartenEmail"));

        double lat = Double.parseDouble(intent.getStringExtra("kindergartenLatitude"));
        double lon = Double.parseDouble(intent.getStringExtra("kindergartenLongitude"));

        kindergarten.setLatitude(lat);
        kindergarten.setLongitude(lon);

        Child child  = new Child(
                intent.getStringExtra("childId"),
                intent.getStringExtra("childName"),
                intent.getStringExtra("childBirthday"),
                intent.getStringExtra("childTag"));

        child.setKindergarten(kindergarten);

        currentUser = new User(
                intent.getStringExtra("id"),
                intent.getStringExtra("name"),
                intent.getStringExtra("phone"),
                intent.getStringExtra("email"));

        currentUser.setChild(child);

    }


    /* This method opens the image EXIF and writes to it (exactly into an exif tag called "UserComment")
     * It writes the data of the user (parent), name and tag number of the child, and that this photo is 'tagged'
     * When a photo is tagged we will use it in the Python Server to train the system to better recognize the child's face*/

    public void writeToChildPhotoExif(String childImagePath){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(childImagePath);
            String[] arr = childImagePath.split("/");
            String imageName = arr[arr.length-1];

            // Write author to image EXIF
            // example of what we write to UserComment tag in EXIF:
            //{"Image Name": "DSC03614.JPG", "Author": {"child of author": {"tag number": 4, "name": "TUVI BIMBA", "tagged": 1}, "author email": "aba.tuvi@gmail.com"}}'
            String tag = currentUser.getChild().getTag();
            String childName = currentUser.getChild().getName();
            String parentPhone = currentUser.getPhoneNumber();
            String parentEmail = currentUser.getEmail();

            String userCommentStr = "{\"image_name\": \"" + imageName + "\", \"author\": {\"child_of_author\": {\"tag_number\": " + tag + ", \"name\": \"" + childName + "\", \"tagged\": 1}, \"author_phone\": \"" + parentPhone + "\", \"author_email\": \"" + parentEmail + "\"}}";
            String bla = userCommentStr;
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, userCommentStr);
            exif.saveAttributes();
            String bla2 = "";
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String childImagePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bmp = null;
            try {
                bmp = getBitmapFromUri(selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // set picked photo in image view
            childPhoto_imageView.setImageBitmap(bmp);

            // save loaded photo to appear from now as default image in image view
            sp.edit().putString("loaded",childImagePath).apply();

            // write to image exif and upload to dropbox
            writeToChildPhotoExif(childImagePath);
            try{
                new UploadAsyncTask().execute(childImagePath);
            }catch (Exception e){
                e.printStackTrace();
            }
            // TODO implement keeping photo of child in ImageView using SharedPreferences

        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public void initViews(){
        addPhotoBtn = findViewById(R.id.addPhoto_button);
        logOutBtn = findViewById(R.id.logout_button);
        childName_textView = findViewById(R.id.childName_textView);
        userName_textView = findViewById(R.id.userName_textView);
        userPhone_textView = findViewById(R.id.userPhone_textView);
        userEmail_textView = findViewById(R.id.userEmail_textView);
        kindergartenName_textView = findViewById(R.id.kindergartenName_textView);
        kindergartenAddress_textView = findViewById(R.id.kindergartenAddress_textView);
        kindergartenEmail_textView = findViewById(R.id.kindergartenEmail_textView);
        sp = getSharedPreferences("CurrentSession",MODE_PRIVATE);
        //spLoadedChildPhoto = getSharedPreferences("loaded",MODE_PRIVATE);
        childPhoto_imageView = findViewById(R.id.childPhoto_imageView);
    }

    public void setViews(Intent intent){
        childName = intent.getStringExtra("childName");
        userName = intent.getStringExtra("name");
        userPhone = intent.getStringExtra("phone");
        userEmail = intent.getStringExtra("email");
        kgName = intent.getStringExtra("kindergartenName");
        kgAddress = intent.getStringExtra("kindergartenAddress");
        kgEmail = intent.getStringExtra("kindergartenEmail");

        childName_textView.setText(childName);
        userName_textView.setText(userName);
        userPhone_textView.setText(userPhone);
        userEmail_textView.setText(userEmail);
        kindergartenName_textView.setText(kgName + " Kindergarten");
        kindergartenAddress_textView.setText(kgAddress);
        kindergartenEmail_textView.setText(kgEmail);

        if (!sp.getString("loaded","").equals("")){
            String path = sp.getString("loaded","");
            File imgFile = new  File(path);
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                // set previously loaded photo in image view
                childPhoto_imageView.setImageBitmap(myBitmap);
            }
        }

//        if (!spLoadedChildPhoto.getString("loaded","").equals("")){
//            String path = spLoadedChildPhoto.getString("loaded","");
//            File imgFile = new  File(path);
//            if(imgFile.exists()){
//                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//                // set previously loaded photo in image view
//                childPhoto_imageView.setImageBitmap(myBitmap);
//            }
//        }
    }


    public void checkExternalStoragePermission(){

        //first check if permission exists
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted => ask permission from user
            ActivityCompat.requestPermissions(UserActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);

        }

        //first check if permission exists
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE )
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted => ask permission from user
            ActivityCompat.requestPermissions(UserActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 100: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case 101: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

}
