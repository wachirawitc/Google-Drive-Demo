package com.matoom.gdrive.demo;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivityMainActivity";

    private final Executor executorService = Executors.newSingleThreadExecutor();

    private final String ClientID = "890746005145-ou5kcjbu0q1g4s0qpbuhcenlre18jgg9.apps.googleusercontent.com";

    private Button loginButton;
    private Button button;

    private TextView usernameTextView;
    private TextView fileTextView;

    private Drive drive;

    private final int REQUEST_CODE_SIGN_IN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.loginButton);
        button = findViewById(R.id.button);

        usernameTextView = findViewById(R.id.usernameTextView);
        fileTextView = findViewById(R.id.fileTextView);

        loginButton.setOnClickListener(view -> requestSignIn());
        button.setOnClickListener(view -> getRootFolder()
                .addOnSuccessListener(MainActivity.this, files -> {

                    StringBuilder builder = new StringBuilder();
                    for (File file : files) {
                        builder.append(file.getId() + "\t" + file.getName() + "\n");
                    }
                    fileTextView.setText(builder.toString());

                }).addOnFailureListener(MainActivity.this, exception -> {
                    Log.e(TAG, exception.getMessage(), exception);
                }));
    }


    public void requestSignIn() {
        String key = ClientID;
        Scope[] scopes = {
                new Scope(DriveScopes.DRIVE),
                new Scope(DriveScopes.DRIVE_APPDATA),
                new Scope(DriveScopes.DRIVE_FILE),
                new Scope(DriveScopes.DRIVE_METADATA),
                new Scope(DriveScopes.DRIVE_METADATA_READONLY),
                new Scope(DriveScopes.DRIVE_PHOTOS_READONLY),
                new Scope(DriveScopes.DRIVE_READONLY),
                new Scope(DriveScopes.DRIVE_SCRIPTS)};

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE), scopes)
                .requestIdToken(key)
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        Intent googleAuth = client.getSignInIntent();
        this.startActivityForResult(googleAuth, REQUEST_CODE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {

        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                signedInAccount(resultData);
                break;
            default:
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);

    }

    public void signedInAccount(Intent resultData) {
        GoogleSignIn.getSignedInAccountFromIntent(resultData)
                .addOnSuccessListener(googleSignInAccount -> onSignedSuccess(googleSignInAccount.getAccount()))
                .addOnFailureListener(e -> onSignedFailure(e));
    }

    public void onSignedSuccess(Account account) {
        Log.d(TAG, "onSignedSuccess " + account.name);

        drive = signedDrive(account);
        usernameTextView.setText(account.name);

    }

    public void onSignedFailure(Exception exception) {
        Log.e(TAG, "onSignedFailure", exception);
    }

    public Drive signedDrive(Account account) {
        Collection<String> scopes = Collections.singleton(DriveScopes.DRIVE_FILE);
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = new GsonFactory();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, scopes);
        credential.setSelectedAccount(account);

        return new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("Cloud Drive")
                .build();
    }

    public Task<List<File>> getRootFolder() {
        return Tasks.call(executorService, () -> {
            FileList execute = drive.files().list()
                    .setSpaces("drive")
                    .setQ("mimeType='application/vnd.google-apps.folder' and 'root' in parents and trashed=false")
                    .setFields("files(id, name,size,createdTime,modifiedTime,starred,mimeType)")
                    .execute();

            return execute.getFiles();
        });
    }
}
