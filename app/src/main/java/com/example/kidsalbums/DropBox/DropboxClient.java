package com.example.kidsalbums.DropBox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;

import java.io.IOException;
import java.io.InputStream;

public class DropboxClient {

    public static final String ACCESS_TOKEN = "9WHpN5kNjbAAAAAAAAADiKgpRaceYuYQoRKAE7NA5fGuume9_wdhRCVkXhjJKlcr";
    public static final String PROJECT_NAME_DROPBOX = "dropbox/PythonServer";
    public static final String DROPBOX_DEST_FOLDER_NAME = "/Photos_Before_Processing/";
    private static DropboxClient instance = null;

    DbxRequestConfig config;
    DbxClientV2 client;
    FullAccount account;


    private DropboxClient(){
        config = DbxRequestConfig.newBuilder(PROJECT_NAME_DROPBOX).build();
        client = new DbxClientV2(config, ACCESS_TOKEN);

        // Get current account info
        try {
            account = client.users().getCurrentAccount();
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    public static DropboxClient getInstance(){
        if (instance == null){
            instance = new DropboxClient();
        }
        return instance;
    }

    /* Upload image to Dropbox (our app is "PythonServer" and upload folder called "Photos_Before_Processing") */
    public FileMetadata uploadFileToDropbox(InputStream in, String fileName) throws IOException, DbxException {
        FileMetadata metadata = client.files().uploadBuilder(DROPBOX_DEST_FOLDER_NAME + fileName).uploadAndFinish(in);
        return metadata;
    }



}
