package com.qiu.test_face_mlkit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 1001;
    private static final int REQ_PERMISSION = 2001;

    private String currentPhotoPath;
    private ImageView imgFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCapture = findViewById(R.id.btn_capture);
        imgFace = findViewById(R.id.img_face);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndOpenCamera();
            }
        });
    }

    private void checkPermissionAndOpenCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean needRequest = false;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
            }
            if (needRequest) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQ_PERMISSION);
                return;
            }
        }
        openCamera();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, REQ_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "FACE_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                fileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            if (currentPhotoPath != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
                if (bitmap != null) {
                    detectFaceAndCrop(bitmap);
                } else {
                    Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void detectFaceAndCrop(Bitmap bitmap) {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        FaceDetector detector = FaceDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces == null || faces.isEmpty()) {
                            Toast.makeText(MainActivity.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Face face = faces.get(0);
                        Rect box = face.getBoundingBox();

                        // 在原始人脸框基础上放大，包含头和部分肩膀
                        float expandRatioWidth = 0.4f;   // 左右各放大 40%
                        float expandRatioHeight = 0.6f;  // 上下各放大 60%

                        int centerX = box.centerX();
                        int centerY = box.centerY();
                        int faceWidth = box.width();
                        int faceHeight = box.height();

                        int newWidth = (int) (faceWidth * (1 + expandRatioWidth * 2));
                        int newHeight = (int) (faceHeight * (1 + expandRatioHeight * 2));

                        int left = centerX - newWidth / 2;
                        int top = centerY - newHeight / 2;
                        int right = centerX + newWidth / 2;
                        int bottom = centerY + newHeight / 2;

                        left = Math.max(0, left);
                        top = Math.max(0, top);
                        right = Math.min(bitmap.getWidth(), right);
                        bottom = Math.min(bitmap.getHeight(), bottom);

                        int width = right - left;
                        int height = bottom - top;

                        if (width <= 0 || height <= 0) {
                            Toast.makeText(MainActivity.this, "人脸框无效", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Bitmap faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);
                        imgFace.setImageBitmap(faceBitmap);

                        // TODO: 如需对接门禁，这里可以将 faceBitmap 保存或上传
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "人脸检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "需要相机与存储权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}