package com.example.kidsalbums.DropBox;
import android.os.AsyncTask;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class UploadAsyncTask extends AsyncTask<String, Integer, String> {

    DropboxClient dropboxClient;

    @Override
    protected String doInBackground(String...params){
        String imagePath = params[0];
        dropboxClient = DropboxClient.getInstance();
        publishProgress(0);
        uploadImageToDropboxWithAsyncTask(imagePath);
        publishProgress(1);
        return "uploaded tagged photo";
    }

    @Override
    protected void onProgressUpdate(Integer...values){

    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
    }

    public void uploadImageToDropboxWithAsyncTask(String path){

        String[] arr = path.split("/");
        String[] imageNameArr = (arr[arr.length-1]).split("\\.");
        String imageName = imageNameArr[0] + "_TAGGED." + imageNameArr[1];

        try (InputStream in = new FileInputStream(path)) {
            // get Dropbox client
            dropboxClient = DropboxClient.getInstance();
            try{
                FileMetadata metadata = dropboxClient.uploadFileToDropbox(in, imageName);
            }catch (DbxException e){
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
