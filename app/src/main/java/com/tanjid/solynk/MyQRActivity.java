package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class MyQRActivity extends AppCompatActivity {

    private ImageView imageViewQR;
    private TextView textViewMyUserId;
    private String myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qr);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Connection ID");
        }

        imageViewQR = findViewById(R.id.imageViewQR);
        textViewMyUserId = findViewById(R.id.textViewMyUserId);

        SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
        // Ensure "ID_NOT_FOUND" is not saved persistently.
        // It's just a fallback for this session if the ID wasn't generated properly.
        myUserId = prefs.getString("myUserId", "ID_NOT_FOUND");

        // Display the user ID
        textViewMyUserId.setText(myUserId);

        // Generate and display QR code
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(myUserId, BarcodeFormat.QR_CODE, 600, 600);
            imageViewQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}