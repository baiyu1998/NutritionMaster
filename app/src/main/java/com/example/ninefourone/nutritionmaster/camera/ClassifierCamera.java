package com.example.ninefourone.nutritionmaster.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ninefourone.nutritionmaster.R;
import com.example.ninefourone.nutritionmaster.bean.ClassifyResult;
import com.example.ninefourone.nutritionmaster.bean.ResultList;
import com.example.ninefourone.nutritionmaster.modules.classifyresult.DishResultActivity;
import com.example.ninefourone.nutritionmaster.utils.ConstantUtils;
import com.example.ninefourone.nutritionmaster.utils.MessageUtils;
import com.example.ninefourone.nutritionmaster.utils.WebUtils;
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ScorpioMiku on 2018/9/3.
 */

public class ClassifierCamera extends AppCompatActivity {

    public static int MATERAIL_CODE = 0;
    public static int DISH_CODE = 1;

    @BindView(R.id.camera_preview)
    FrameLayout mCameraLayout;
    @BindView(R.id.results_text_view)
    TextView resultsTextView;
    @BindView(R.id.more_take_photo_button_capture)
    ImageView moreTakePhotoButtonCapture;
    @BindView(R.id.more_takephoto_ok)
    ImageView moreTakephotoOk;
    @BindView(R.id.camera_cover_linearlayout)
    FrameLayout cameraCoverLinearlayout;

    private Camera mCamera;
    private CameraPreview mPreview;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private ArrayList<ClassifyResult> resultList = new ArrayList<>();

    private int code = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        code = intent.getIntExtra("CODE", -1);
        //取消toolbar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //注意：上面两个设置必须写在setContentView前面
        setContentView(R.layout.cameras_layout);
        ButterKnife.bind(this);

        if (!checkCameraHardware(this)) {
            MessageUtils.MakeToast("不支持相机");
        } else {
            openCamera();
        }

        setCameraDisplayOrientation(this, mCameraId, mCamera);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    /**
     * 检查当前设备是否有相机
     *
     * @param context
     * @return
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 打开相机
     */
    private void openCamera() {
        if (null == mCamera) {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            mCameraLayout.addView(mPreview);
            mCamera.startPreview();
        }
    }

    /**
     * 获取相机
     *
     * @return
     */
    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();

            Camera.Parameters mParameters = c.getParameters();
            List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();

            mParameters.setPictureSize(2048, 1536);
            mParameters.setPreviewSize(2048, 1536);
            c.setParameters(mParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return c;
    }

    /**
     * 对焦回调,对焦完成后进行拍照
     */
    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                mCamera.takePicture(null, null, mPictureCallback);
            }
        }
    };

    /**
     * 拍照回调
     */
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            try {
                String imgStr = Base64.encodeToString(data, Base64.DEFAULT);
                String imgParam = URLEncoder.encode(imgStr, "UTF-8");
                final String param = "image=" + imgParam + "&top_num=" + 1;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String result = null;
                        try {
                            if (code == 0) {
                                result = WebUtils.HttpPost(ConstantUtils.BD_DISH_URL,
                                        ConstantUtils.BD_ACCESS_TOKEN, param);
                            } else if (code == 1) {
                                result = WebUtils.HttpPost(ConstantUtils.BD_DISH_URL,
                                        ConstantUtils.BD_ACCESS_TOKEN, param);
                            } else {
                                Logger.e("拍照code为-1");
                            }
                            JSONObject jsonObject = new JSONObject(result);
                            ClassifyResult classifyResult = new ClassifyResult();
                            JSONArray resultObject = jsonObject.getJSONArray("result");
                            jsonObject = resultObject.getJSONObject(0);
                            classifyResult.setCalorie(jsonObject.getInt("calorie"));
                            classifyResult.setHas_calorie(jsonObject.getBoolean("has_calorie"));
                            classifyResult.setProbability(jsonObject.getDouble("probability"));
                            classifyResult.setName(jsonObject.getString("name"));
//                            Logger.d(classifyResult);
                            resultList.add(classifyResult);
                            refreshUI();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            MessageUtils.MakeToast("拍照！");
            mCamera.startPreview();
        }
    };


    /**
     * 两个按钮的事件
     *
     * @param view
     */
    @OnClick({R.id.more_take_photo_button_capture, R.id.more_takephoto_ok})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.more_take_photo_button_capture:
                mCamera.autoFocus(mAutoFocusCallback);
                cameraCoverLinearlayout.setVisibility(View.VISIBLE);
                break;
            case R.id.more_takephoto_ok:
                Intent intent = new Intent(ClassifierCamera.this, DishResultActivity.class);
                intent.putExtra("LIST", resultList);
//                intent.putExtra("LIST", ConstantUtils.testData);
                startActivity(intent);
                resultList.clear();
                refreshUI();
                finish();
                break;
        }
    }

    //将相机设置成竖屏
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {

        int degrees = 0;

        //可以获得摄像头信息
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        //获取屏幕旋转方向
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    private void refreshUI() {
        resultsTextView.post(new Runnable() {
            @Override
            public void run() {
                String text = "";
                for (int i = 0; i < resultList.size(); i++) {
                    text += resultList.get(i).getName() + " ";
                }
                resultsTextView.setText(text);
                cameraCoverLinearlayout.setVisibility(View.INVISIBLE);
            }
        });
    }
}