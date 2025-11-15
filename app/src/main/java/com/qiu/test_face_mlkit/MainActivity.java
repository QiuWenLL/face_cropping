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

import com.qiu.face_mlkit.FaceCropper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

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
    private Bitmap currentFaceBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCapture = findViewById(R.id.btn_capture);
        Button btnRotate = findViewById(R.id.btn_rotate);
        Button btnMirror = findViewById(R.id.btn_mirror);
        imgFace = findViewById(R.id.img_face);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndOpenCamera();
            }
        });

        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateCurrentFace();
            }
        });

        btnMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mirrorCurrentFace();
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
        Task<Bitmap> task = FaceCropper.cropHeadAndShoulders(bitmap);
        task.addOnSuccessListener(new OnSuccessListener<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap faceBitmap) {
                        currentFaceBitmap = faceBitmap;
                        imgFace.setImageBitmap(faceBitmap);
                    }
                });

        task.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "人脸检测失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rotateCurrentFace() {
        if (currentFaceBitmap == null) {
            Toast.makeText(this, "请先拍照并检测人脸", Toast.LENGTH_SHORT).show();
            return;
        }

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(90);

        Bitmap rotated = Bitmap.createBitmap(currentFaceBitmap,
                0,
                0,
                currentFaceBitmap.getWidth(),
                currentFaceBitmap.getHeight(),
                matrix,
                true);

        currentFaceBitmap = rotated;
        imgFace.setImageBitmap(rotated);
    }

    private void mirrorCurrentFace() {
        if (currentFaceBitmap == null) {
            Toast.makeText(this, "请先拍照并检测人脸", Toast.LENGTH_SHORT).show();
            return;
        }

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.preScale(-1, 1); // 水平镜像

        Bitmap mirrored = Bitmap.createBitmap(currentFaceBitmap,
                0,
                0,
                currentFaceBitmap.getWidth(),
                currentFaceBitmap.getHeight(),
                matrix,
                true);

        currentFaceBitmap = mirrored;
        imgFace.setImageBitmap(mirrored);
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