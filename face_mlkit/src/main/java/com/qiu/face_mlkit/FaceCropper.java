package com.qiu.face_mlkit;

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.android.gms.tasks.Task;

import java.util.List;

/**
 * 封装使用 ML Kit 对单张图片做人脸检测并裁剪（包含头和肩膀）的工具类。
 */
public class FaceCropper {

    /**
     * 异步裁剪第一张检测到的人脸，返回放大后的头像 Bitmap（包含头和部分肩膀）。
     *
     * @param source 原始图片
     * @return Task，成功时结果为裁剪后的人脸 Bitmap；失败或未检测到人脸会触发失败回调。
     */
    public static Task<Bitmap> cropHeadAndShoulders(Bitmap source) {
        if (source == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalArgumentException("source bitmap is null"));
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        InputImage image = InputImage.fromBitmap(source, 0);

        return detector.process(image)
                .continueWith(task -> {
                    List<Face> faces = task.getResult();
                    if (faces == null || faces.isEmpty()) {
                        throw new IllegalStateException("未检测到人脸");
                    }

                    Face face = faces.get(0);
                    Rect box = face.getBoundingBox();

                    // 基于 MainActivity 中的放大策略：左右各放大 40%，上下各放大 60%
                    float expandRatioWidth = 0.4f;
                    float expandRatioHeight = 0.6f;

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
                    right = Math.min(source.getWidth(), right);
                    bottom = Math.min(source.getHeight(), bottom);

                    int width = right - left;
                    int height = bottom - top;

                    if (width <= 0 || height <= 0) {
                        throw new IllegalStateException("人脸框无效");
                    }

                    return Bitmap.createBitmap(source, left, top, width, height);
                });
    }
}
