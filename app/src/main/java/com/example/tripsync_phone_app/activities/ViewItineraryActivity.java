package com.example.tripsync_phone_app.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.databinding.ActivityViewItineraryBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ViewItineraryActivity extends AppCompatActivity {

    private ActivityViewItineraryBinding binding;
    private int itineraryId;
    private AppDatabase db;

    private GoogleMap map;
    private boolean mapLoaded = false;
    private final List<Destination> dests = new ArrayList<>();

    private final ActivityResultLauncher<String> storagePerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) requestSnapshotThenCreatePdf();
                else Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        itineraryId = getIntent().getIntExtra("itinerary_id", -1);
        if (itineraryId <= 0) { finish(); return; }

        binding.topAppBar.setNavigationOnClickListener(v -> onBackPressed());
        binding.btnDownload.setOnClickListener(v -> tryDownload());

        db = AppDatabase.getInstance(this);

        SupportMapFragment frag = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapContainer, frag)
                .commitNow();
        frag.getMapAsync(gm -> {
            map = gm;
            map.getUiSettings().setZoomControlsEnabled(true);
            populate();
            map.setOnMapLoadedCallback(() -> mapLoaded = true);
        });
    }

    private void populate() {
        Itinerary it = db.itineraryDao().getItineraryById(itineraryId);
        if (it == null) { finish(); return; }
        binding.txtTripTitle.setText(it.tripName);

        dests.clear();
        dests.addAll(db.destinationDao().getDestinationsForItinerary(itineraryId));

        // EARLIEST → LATEST
        Collections.sort(dests, Comparator.comparingLong(d -> epoch(d.date, d.time)));

        LayoutInflater inf = LayoutInflater.from(this);
        binding.destListContainer.removeAllViews();

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        boolean hasAny = false;

        for (int i = 0; i < dests.size(); i++) {
            Destination d = dests.get(i);

            // Card UI: "Destination N: <address>"
            View card = inf.inflate(R.layout.card_view_destination, binding.destListContainer, false);
            String title = "Destination " + (i + 1) + ": " + emptyDash(d.address);
            ((TextView) card.findViewById(R.id.txtAddress)).setText(title);
            ((TextView) card.findViewById(R.id.txtDateTime))
                    .setText(emptyDash(joinNonEmpty(d.date, d.time, " · ")));
            ((TextView) card.findViewById(R.id.txtNote)).setText(emptyDash(d.note));
            binding.destListContainer.addView(card);

            // Map markers follow the same chronological order
            LatLng ll = geocode(d.address);
            if (ll != null && map != null) {
                map.addMarker(new MarkerOptions()
                        .position(ll)
                        .title("Destination " + (i + 1)));
                bounds.include(ll);
                hasAny = true;
            }
        }

        if (hasAny && map != null) {
            try { map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80)); }
            catch (Exception ignored) {}
        }
    }

    private void tryDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestSnapshotThenCreatePdf();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                storagePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else requestSnapshotThenCreatePdf();
        }
    }

    private void requestSnapshotThenCreatePdf() {
        if (map == null) { createPdf(null); return; }
        if (!mapLoaded) {
            map.setOnMapLoadedCallback(() -> map.snapshot(this::createPdf));
            return;
        }
        map.snapshot(this::createPdf);
    }

    /** Build and save PDF (mapBmp may be null). */
    private void createPdf(Bitmap mapBmp) {
        PdfDocument doc = new PdfDocument();
        final int pageW = 595; // A4 @72dpi
        final int pageH = 842;

        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas c = page.getCanvas();

        int x = 40, y = 50, line = 18;

        android.graphics.Paint title = new android.graphics.Paint();
        title.setTextSize(18f);
        title.setFakeBoldText(true);

        android.graphics.Paint h2 = new android.graphics.Paint();
        h2.setTextSize(14f);
        h2.setFakeBoldText(true);

        android.graphics.Paint body = new android.graphics.Paint();
        body.setTextSize(12f);

        c.drawText(binding.txtTripTitle.getText().toString(), x, y, title);
        y += 22;

        if (mapBmp != null) {
            float w = pageW - 2 * x;
            float ratio = mapBmp.getHeight() / (float) mapBmp.getWidth();
            float h = w * ratio;
            Bitmap scaled = Bitmap.createScaledBitmap(mapBmp, (int) w, (int) h, true);
            c.drawBitmap(scaled, x, y, null);
            y += (int) h + 16;
        }

        // EARLIEST → LATEST already ensured in populate()
        for (int i = 0; i < dests.size(); i++) {
            Destination d = dests.get(i);

            y += line;
            c.drawText("Destination " + (i + 1) + ": " + emptyDash(d.address), x, y, h2);
            y += line;
            c.drawText("When: " + emptyDash(joinNonEmpty(d.date, d.time, " · ")), x + 10, y, body);
            if (!TextUtils.isEmpty(d.note)) {
                y += line;
                c.drawText("Note: " + d.note, x + 10, y, body);
            }
            y += 8;

            if (y > pageH - 60) {
                doc.finishPage(page);
                info = new PdfDocument.PageInfo.Builder(pageW, pageH, doc.getPages().size() + 1).create();
                page = doc.startPage(info);
                c = page.getCanvas();
                y = 40;
            }
        }
        doc.finishPage(page);

        String fileName = "Itinerary_" + System.currentTimeMillis() + ".pdf";
        try {
            OutputStream os;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                cv.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                cv.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                os = getContentResolver().openOutputStream(uri);
                doc.writeTo(os);
                os.close();
                cv.clear();
                cv.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(uri, cv, null, null);
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, fileName);
                os = new FileOutputStream(f);
                doc.writeTo(os);
                os.close();
            }
            doc.close();
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            doc.close();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- helpers ----------
    private static String emptyDash(String s) { return TextUtils.isEmpty(s) ? "—" : s; }
    private static String joinNonEmpty(String a, String b, String sep) {
        if (TextUtils.isEmpty(a) && TextUtils.isEmpty(b)) return "";
        if (TextUtils.isEmpty(a)) return b;
        if (TextUtils.isEmpty(b)) return a;
        return a + sep + b;
    }

    /** Parse multiple date formats (supports "13 Aug 2025" and "Aug 13, 2025"). */
    private long epoch(String date, String time) {
        if (TextUtils.isEmpty(date)) return Long.MAX_VALUE;

        String[] patterns = new String[]{
                "d MMM yyyy",    // 13 Aug 2025
                "dd MMM yyyy",   // 03 Sep 2025
                "MMM d, yyyy",   // Aug 13, 2025
                "MMM dd, yyyy"   // Aug 03, 2025
        };

        Calendar cal = Calendar.getInstance();
        boolean parsed = false;
        for (String p : patterns) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(p, Locale.getDefault());
                df.setLenient(false);
                cal.setTime(df.parse(date));
                parsed = true;
                break;
            } catch (ParseException ignored) {}
        }
        if (!parsed) return Long.MAX_VALUE;

        int hour = 0, min = 0;
        if (!TextUtils.isEmpty(time)) {
            try {
                String[] t = time.split(":");
                hour = Integer.parseInt(t[0]);
                min  = Integer.parseInt(t[1]);
            } catch (Exception ignored) {}
        }
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private LatLng geocode(String address) {
        try {
            if (TextUtils.isEmpty(address)) return null;
            android.location.Geocoder g = new android.location.Geocoder(this, Locale.getDefault());
            List<android.location.Address> list = g.getFromLocationName(address, 1);
            if (!list.isEmpty()) {
                android.location.Address a = list.get(0);
                return new LatLng(a.getLatitude(), a.getLongitude());
            }
        } catch (Exception ignored) {}
        return null;
    }
}
