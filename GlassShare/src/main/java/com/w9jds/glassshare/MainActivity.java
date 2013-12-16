package com.w9jds.glassshare;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.glass.widget.CardScrollView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.w9jds.glassshare.Adapters.csaAdapter;
import com.w9jds.glassshare.Classes.ImageItem;
import com.w9jds.glassshare.Classes.StorageApplication;
import com.w9jds.glassshare.Classes.StorageService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

@SuppressLint("DefaultLocale")
public class MainActivity extends Activity
{
    private final String CAMERA_IMAGE_BUCKET_NAME = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
    private final String CAMERA_IMAGE_BUCKET_ID = getBucketId(CAMERA_IMAGE_BUCKET_NAME);

    private ConnectivityManager mcmCon;

    //create member variables for Azure
    private MobileServiceClient mClient;
    private StorageService mStorageService;

    //create member variables for google drive
    private Drive mdService;
    private GoogleAccountCredential mgacCredential;

    //custom adapter
    private csaAdapter mcvAdapter;
    //list for all the paths of the images on google glass
    private ArrayList<String> mlsPaths = new ArrayList<String>();
    //variable for the last selected index
    private int iPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mcmCon = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (mcmCon.getActiveNetworkInfo().isConnected())
        {
            StorageApplication myApp = (StorageApplication) getApplication();
            mStorageService = myApp.getStorageService();
        }

        //get all the images from the camera folder (paths)
        mlsPaths = getCameraImages(this);
        //sort the paths of pictures
        sortPaths();
        //create a new card scroll viewer for this context
        CardScrollView csvCardsView = new CardScrollView(this);
        //create a new adapter for the scroll viewer
        mcvAdapter = new csaAdapter(this, mlsPaths);
        //set this adapter as the adapter for the scroll viewer
        csvCardsView.setAdapter(mcvAdapter);
        //activate this scroll viewer
        csvCardsView.activate();
        //add a listener to the scroll viewer that is fired when an item is clicked
        csvCardsView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //save the card index that was selected
                iPosition = position;
                //open the menu
                openOptionsMenu();
            }
        });

        //set the view of this activity
        setContentView(csvCardsView);

    }

    private void sortPaths()
    {
        java.io.File[] fPics = new java.io.File[mlsPaths.size()];

        for (int i = 0; i < mlsPaths.size(); i++)
            fPics[i] = new java.io.File(mlsPaths.get(i));

        mlsPaths.clear();

        Arrays.sort(fPics, new Comparator<java.io.File>()
        {
            @Override
            public int compare(java.io.File o1, java.io.File o2)
            {
                return Long.valueOf(o1.lastModified()).compareTo(o2.lastModified());
            }
        });

        for (int i = fPics.length - 1; i >= 0; i--)
            mlsPaths.add(fPics[i].getAbsolutePath());
    }

    public String getBucketId(String path)
    {
        return String.valueOf(path.toLowerCase().hashCode());
    }

    public ArrayList<String> getCameraImages(Context context)
    {
        final String[] projection = {MediaStore.Images.Media.DATA};
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        ArrayList<String> result = new ArrayList<String>(cursor.getCount());

        if (cursor.moveToFirst())
        {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do
            {
                final String data = cursor.getString(dataColumn);
                result.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem iItem) {
        switch (iItem.getItemId()) {
            case R.id.delete_menu_item:


                return true;
            case R.id.upload_menu_item:

                if (mcmCon.getActiveNetworkInfo().isConnected())
                {

                }

                return true;

            case R.id.uploadphone_menu_item:

                if (mcmCon.getActiveNetworkInfo().isConnected())
                {
                    ImageItem item = new ImageItem();

                    String sContainer = "";

                    Account[] myAccounts = AccountManager.get(this).getAccounts();
                    //for each account
                    for (int i = 0; i < myAccounts.length; i++)
                    {
                        //if the account type is google
                        if (myAccounts[i].type.equals("com.google"))
                            //set this as the selected Account
                            sContainer = myAccounts[i].name;
                    }

//                    item.image = BitmapFactory.decodeFile(mlsPaths.get(iPosition));

//                    Intent blobDetailsIntent = new Intent(getApplicationContext(), ImageItem.class);
//                    blobDetailsIntent.putExtra("ContainerName", sContainer);
//                    blobDetailsIntent.putExtra("BlobName", "img1");
//                    blobDetailsIntent.putExtra("BlobPosition", iPosition);
//                    startActivity(blobDetailsIntent);
                }

                return true;

            default:
                return super.onOptionsItemSelected(iItem);
        }
    }

    /***
     * Handles uploading an image to a specified url
     */
//    class ImageUploaderTask extends AsyncTask<Void, Void, Boolean>
//    {
//        private String mUrl;
//
//        public ImageUploaderTask(String url)
//        {
//            mUrl = url;
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... params) {
//            try {
//                //Get the image data
//                Cursor cursor = getContentResolver().query(mImageUri, null,null, null, null);
//                cursor.moveToFirst();
//
//                FileInputStream fiStream = new FileInputStream(mlsPaths.get(iPosition));
//
//                int bytesRead = 0;
//                ByteArrayOutputStream bos = new ByteArrayOutputStream();
//
//                byte[] b = new byte[1024];
//                while ((bytesRead = fiStream.read(b)) != -1)
//                {
//                    bos.write(b, 0, bytesRead);
//                }
//                byte[] bytes = bos.toByteArray();
//
//                // Post our image data (byte array) to the server
//                HttpURLConnection urlConnection = (HttpURLConnection) new URL(mUrl.replace("\"", "")).openConnection();
//                urlConnection.setDoOutput(true);
//                urlConnection.setRequestMethod("PUT");
//                urlConnection.addRequestProperty("Content-Type", "image/jpeg");
//                urlConnection.setRequestProperty("Content-Length", ""+ bytes.length);
//
//                // Write image data to server
//                DataOutputStream doStream = new DataOutputStream(urlConnection.getOutputStream());
//                doStream.write(bytes);
//                doStream.flush();
//                doStream.close();
//
//                int response = urlConnection.getResponseCode();
//
//                //If we successfully uploaded, return true
//                if (response == 201 && urlConnection.getResponseMessage().equals("Created"))
//                    return true;
//
//            }
//
//            catch (Exception ex)
//            {
//                Log.e("GlassShareDebug", ex.getMessage());
//            }
//            return false;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean uploaded)
//        {
////            if (uploaded)
////            {
////                mAlertDialog.cancel();
////                mStorageService.getBlobsForContainer(mContainerName);
////            }
//        }
//    }



    private void saveFileToDrive(String sPath)
    {
        final String msPath = sPath;

        Thread t = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // File's binary content
                    java.io.File fImage = new java.io.File(msPath);
                    FileContent fcContent = new FileContent("image/jpeg", fImage);

                    // File's metadata.
                    File gdfBody = new File();
                    gdfBody.setTitle(fImage.getName());
                    gdfBody.setMimeType("image/jpeg");

                    File gdfFile = mdService.files().insert(gdfBody, fcContent).execute();
                    if (gdfFile != null)
                        Log.d("GlassShareUploadTask", "Uploaded");
                }
                catch (UserRecoverableAuthIOException e) {
                    Log.d("GlassShareUploadTask", e.toString());
//                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                }
                catch (IOException e) {
                    Log.d("GlassShareUploadTask", e.toString());
//                    e.printStackTrace();
                }
                catch (Exception e) {
                    Log.d("GlassShareUploadTask", e.toString());
                }
            }
        });
        t.start();

    }

    private Drive getDriveService(GoogleAccountCredential credential)
    {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
    }
}


