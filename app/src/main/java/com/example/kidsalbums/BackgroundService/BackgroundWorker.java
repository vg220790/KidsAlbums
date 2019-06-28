package com.example.kidsalbums.BackgroundService;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;


import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.example.kidsalbums.DropBox.DropboxClient;
import com.example.kidsalbums.GeoDegreeConverter.GeoDegree;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;


import androidx.work.Data;
import androidx.work.Worker;

import static android.content.Context.MODE_PRIVATE;

public class BackgroundWorker extends Worker {

    public static final String EXTRA_OUTPUT_MESSAGE = "output_message";
    public static final String EXTRA_USER_DETAILS = "user_details";
    public static final String EXTRA_ADDRESS = "address";
    public static String USER_DETAILS = "user_details";
    public static double LATITUDE = 0.0;
    public static double LONGITUDE = 0.0;
    DropboxClient dropboxClient;
    SharedPreferences sp;

    @NonNull
    @Override
    public Result doWork() {

        sp = getApplicationContext().getSharedPreferences("CurrentSession", MODE_PRIVATE);
        Data d = getInputData();
        String ud = getInputData().getString(EXTRA_USER_DETAILS, "");
        ud = sp.getString("user_details","");
        String address = getInputData().getString(EXTRA_ADDRESS, "");

        if( address!=null ){

            // Create Dropbox client
            dropboxClient = DropboxClient.getInstance();

            /*
            ud are the user details string that is passed from UserActivity when we create a new
            periodic task. In order for the background process to do it's job
            the user details can not be empty ("")
            */
            if (ud!=""){
                String[] details = ud.split("_");
                LATITUDE  = Double.valueOf(details[1]);
                LONGITUDE = Double.valueOf(details[2]);
                USER_DETAILS = details[3]+"_"+details[4]+"_"+details[5]+"_"+details[6];
                collectRelevantPhotosAndUploadToDropbox();
                updateLatestBackgroundScanDate();
                Data output = new Data.Builder()
                        .putString(EXTRA_OUTPUT_MESSAGE, "I have come from MyWorker!")
                        .build();

                setOutputData(output);

                return Result.SUCCESS;
            }
        }
        return Result.RETRY;
    }


    //collects all photos taken near the kindergarten and uploads them to Dropbox
    public  void collectRelevantPhotosAndUploadToDropbox(){

        ArrayList<String> cameraImagesPaths = getCameraImagesPath();
        Collections.reverse(cameraImagesPaths); //so that newest images will be checked first
        collectRelevantPhotosToUploadFolder(cameraImagesPaths);
    }

    public void collectRelevantPhotosToUploadFolder(ArrayList<String> cameraImagesPaths){
        ArrayList<String> uploadImagesPaths = new ArrayList<String>();

        for (String path: cameraImagesPaths) {
            //checks if image was taken near the kindergarten
            if(isRelevant(path)){
                uploadImageToDropbox(path);
            }
        }
    }

    public void writeAuthorToImageExif(ExifInterface exif, String imageName){

        //Example of exif tag we are writing to the image:
        // {"Image Name": "DSC03614.JPG", "Author": {"child of author": {"tag number": 4, "name": "TUVI BIMBA"}, "author email": "aba.tuvi@gmail.com"}}'
        String[] userDetails = USER_DETAILS.split("_");
        String tag = userDetails[0];
        String childName = userDetails[1];
        String parentPhone = userDetails[2];
        String parentEmail = userDetails[3];

        String userCommentStr = "{\"Image Name\": \"" + imageName + "\", \"Author\": {\"child of author\": {\"tag number\": " + tag + ", \"name\": \"" + childName + "\", \"tagged\": 0}, \"author phone\": \"" + parentPhone + "\", \"author email\": \"" + parentEmail + "\"}}'";
        String bla = userCommentStr;
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, userCommentStr);
        try {
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadImageToDropbox(String path){

        String[] arr = path.split("/");
        String imageName = arr[arr.length-1];

        // Upload image to Dropbox (our app is "PythonServer" and upload folder called "Photos_Before_Processing")
        try (InputStream in = new FileInputStream(path)) {
            FileMetadata metadata = dropboxClient.uploadFileToDropbox(in, imageName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UploadErrorException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        }

    }

    public boolean isRelevant(String imgPath){
        ExifInterface exif = null;
        String[] s = imgPath.split("/");
        String imageName = s[s.length - 1];

        try {
            //location of kindergarten
            Location kindergarten_location = new Location("Kindergarten");

            ///////////////////
            // set kindergarten location to Bar Giora 24, TLV
            LATITUDE = 32.076010;
            LONGITUDE = 34.776280;
            //////////////////

            kindergarten_location.setLatitude(LATITUDE);
            kindergarten_location.setLongitude(LONGITUDE);

            //location where photo was taken
            exif = new ExifInterface(imgPath);

            /* we want to save the latest date that we scanned the camera photos
             * so that next time background service will scan camera photos it will
             * upload only new shot images */

            /* if "last_date" is defined scan only relevant photos  */
            sp.edit().putString("last_date","").apply();
            if(!sp.getString("last_date","").equals("")){

                /* compare latest date and image date */
                String exifDateStr =  exif.getAttribute(ExifInterface.TAG_DATETIME);

                if (exifDateStr != null){
                    String inputString = (exifDateStr.split(" ")[0]).replace(":","-");
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date photoWasTakenDate = (Date) dateFormat.parse(inputString);
                    Date latestBackgroundScanDate = (Date) dateFormat.parse(sp.getString("last_date",""));

                    if(photoWasTakenDate.before(latestBackgroundScanDate)){
                        return false;
                    }
                }
            }

            /* Compare kindergarten and image locations distances */
            double latB = 0.0;
            double longB = 0.0;
            try{
                GeoDegree geoDegree = new GeoDegree(exif);
                latB = geoDegree.getLatitudeE6();
                longB = geoDegree.getLongitudeE6();

                if (latB > 50 || longB > 50){
                    latB = geoDegree.getLatitude();
                    longB = geoDegree.getLongitude();
                }

            } catch (Exception e){
                //couldn't get geodegree
                e.printStackTrace();
            }

            Location photo_taken_location = new Location("WherePhotoWasTaken");
            photo_taken_location.setLatitude(latB);
            photo_taken_location.setLongitude(longB);

            float distance = kindergarten_location.distanceTo(photo_taken_location);
            double max_distance = 50.0;
            if (distance < max_distance){
                writeAuthorToImageExif(exif, imageName);
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void updateLatestBackgroundScanDate(){
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c);
        sp.edit().putString("last_date", formattedDate).apply();
    }

    //get all files from camera
    public ArrayList<String> getCameraImagesPath(){
        final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStorageDirectory().toString()+ "/DCIM/Camera";
        final String CAMERA_IMAGE_BUCKET_ID = String.valueOf(CAMERA_IMAGE_BUCKET_NAME.toLowerCase().hashCode());
        final String[] projection = { MediaStore.Images.Media.DATA };
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
        final Cursor cursor = getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        ArrayList<String> listOfKindergartenImages = new ArrayList<String>(cursor.getCount());
        if (cursor.moveToFirst()) {
            final int dataColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do {
                final String data = cursor.getString(dataColumn);
                listOfKindergartenImages.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return listOfKindergartenImages;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //               UNNEEDED FUNCTIONS THAT WE MIGHT NEED LATER
    //////////////////////////////////////////////////////////////////////////////////////////////

    //gets all files from gallery
    public ArrayList<String> getImagesPath() {
        try {

            ArrayList<String> listOfKindergartenImages = new ArrayList<String>();

            final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
            final String orderBy = MediaStore.Images.Media._ID;
            //Stores all the images from the gallery in Cursor
            Cursor cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                    null, orderBy);
            //Total number of images
            int count = cursor.getCount();

            //Create an array to store path to all the images
            String[] arrPath = new String[count];

            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                //Store the path of the image
                arrPath[i]= cursor.getString(dataColumnIndex);

                //check if image was taken near the kindergarten
                if(isRelevant(arrPath[i])){
                    listOfKindergartenImages.add(arrPath[i]);
                }
            }
            cursor.close();
            String[] a = arrPath;
            return listOfKindergartenImages;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    public void deleteUploadedImageFromCamera(String imagePath){

        try{
            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.ImageColumns.DATA + "=?" , new String[]{ imagePath });
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
