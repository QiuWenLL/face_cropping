# face_cropping

Android 示例项目：使用 Google ML Kit 进行人脸检测与裁剪，并封装为独立模块 / AAR 供业务 App 调用。

## 功能概览

- 通过系统相机拍照获取人脸照片。
- 使用 ML Kit 人脸检测，自动定位人脸。
- 在原始人脸框基础上放大，裁剪出包含头部和部分肩膀的头像图（类似证件照）。
- 支持在预览界面：
  - 旋转预览图（每次 90°）。
  - 左右镜像预览图。
- 人脸裁剪逻辑提取到独立模块 `face_mlkit`，并可打包为 AAR 在 App 中复用。

## 模块结构

- `app/`
  - Demo App，负责：
    - 相机拍照
    - 权限申请
    - 显示裁剪后的头像
    - 旋转 / 镜像操作
  - 依赖 `face_mlkit` 提供的人脸裁剪能力。

- `face_mlkit/`
  - Android Library 模块。
  - 核心类：`com.qiu.face_mlkit.FaceCropper`
    - 提供接口：
      - `Task<Bitmap> cropHeadAndShoulders(Bitmap source)`
    - 输入：拍照得到的原始 `Bitmap`。
    - 输出：裁剪后的头像 `Bitmap`（包含头和肩膀）。
  - 可以单独构建为 AAR，在业务 App 中直接引用。

## 使用方式（在 App 中调用裁剪）

```java
import com.qiu.face_mlkit.FaceCropper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

// bitmap 为拍照得到的原始图片
Task<Bitmap> task = FaceCropper.cropHeadAndShoulders(bitmap);

task.addOnSuccessListener(new OnSuccessListener<Bitmap>() {
    @Override
    public void onSuccess(Bitmap faceBitmap) {
        // TODO: 使用裁剪后的人脸图，例如显示或上传
    }
}).addOnFailureListener(new OnFailureListener() {
    @Override
    public void onFailure(@NonNull Exception e) {
        // TODO: 处理未检测到人脸或其他异常
    }
});
```

## 在 App 中引用 AAR

如果不直接依赖模块，而是通过 AAR：

1. 将编译生成的 `face_mlkit-xxx.aar` 放到 `app/libs/` 目录下。
2. 在 `app/build.gradle` 中：

```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation(name: "face_mlkit-debug", ext: "aar")
    implementation 'com.google.mlkit:face-detection:16.1.5'
    implementation 'com.google.android.gms:play-services-tasks:17.2.1'
}
```

确保 AAR 内部使用的 ML Kit 依赖，在 App 侧也显式声明。

## 开发与构建

```bash
# 构建 Debug 包
./gradlew assembleDebug

# 仅构建库模块
./gradlew :face_mlkit:assembleDebug
```

---

如需接入实际的人脸门禁 / 人脸库，只需在拿到裁剪后的 `Bitmap` 后：
- 按平台要求进行尺寸缩放 / JPEG 压缩，
- 通过 HTTP/SDK 上传到服务端即可。