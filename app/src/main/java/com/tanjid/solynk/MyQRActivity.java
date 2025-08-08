package com.tanjid.solynk;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.UUID;

public class MyQRActivity extends AppCompatActivity {

    private ImageView imageViewQR;
    private TextView textViewMyUserId;
    private String myUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qr);

        imageViewQR = findViewById(R.id.imageViewQR);
        textViewMyUserId = findViewById(R.id.textViewMyUserId);

        SharedPreferences prefs = getSharedPreferences("SoloConnectPrefs", MODE_PRIVATE);
        myUserId = prefs.getString("myUserId", null);

        if (myUserId == null) {
            myUserId = UUID.randomUUID().toString();
            prefs.edit().putString("myUserId", myUserId).apply();
        }

        textViewMyUserId.setText("Your ID: " + myUserId);

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(myUserId, BarcodeFormat.QR_CODE, 400, 400);
            imageViewQR.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code.", Toast.LENGTH_SHORT).show();
        }
    }
}