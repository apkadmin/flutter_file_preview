package com.zzaning.flutter_file_preview;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.smtt.sdk.TbsReaderView;
import com.wuhenzhizao.titlebar.widget.CommonTitleBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileDisplayActivity extends AppCompatActivity {
    private String TAG = "log | flutter_file_preview | ";
    protected boolean isStatusBar = true;
//    private LeftTitleLayout leftBackLtl;
    private CommonTitleBar titleBar;
    private TBSWebView x5WebView;
    private RelativeLayout rlRoot;
    private String url;
    private TbsReaderView tbsReaderView;

    public static void show(Context context, String url, boolean isOpenFile, String title) {
        Intent intent = new Intent(context, FileDisplayActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("url", url);
        bundle.putSerializable("isOpenFile", isOpenFile);
        bundle.putSerializable("title", title);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    protected int getContentViewId() {
        return R.layout.act_common_webview;
    }

    protected void initView() {
        String title = getIntent().getStringExtra("title");
        boolean isOpenFile = getIntent().getBooleanExtra("isOpenFile", false);
        url = getIntent().getStringExtra("url");
        titleBar.getCenterTextView().setText(title);
//        leftBackLtl.setTitle(title).leftFinish(true);
        x5WebView.addJavascriptInterface(new JsObject(), "android");
        //??????????????????
        handler.sendEmptyMessageDelayed(1001, 500);
        //????????????
        if (!TextUtils.isEmpty(url) && isOpenFile) {
            tbsReaderView = new TbsReaderView(this, readerCallback);
            handler.removeMessages(1001);
            rlRoot.addView(tbsReaderView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            getFilePathAndShowFile();
        }
    }

    private void getFilePathAndShowFile() {
        if (url.startsWith("http")) {// ????????????????????????
            downLoadFromNet(url);
        } else {
            openFile(new File(url));
        }
    }

    private void downLoadFromNet(final String url) {
        //1.??????????????????????????????
        File cacheFile = getCacheFile(url);
        if (cacheFile.exists()) {
            if (cacheFile.length() <= 0) {
                Log.d(TAG, "?????????????????????");
                cacheFile.delete();
                return;
            }
        }

        LoadFileModel.loadPdfFile(url, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "????????????-->onResponse");
                boolean flag;
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    ResponseBody responseBody = response.body();
                    is = responseBody.byteStream();
                    long total = responseBody.contentLength();

                    File file1 = getCacheDir(url);
                    if (!file1.exists()) {
                        file1.mkdirs();
                        Log.d(TAG, "????????????????????? " + file1.toString());
                    }

                    //fileN : /storage/emulated/0/pdf/kauibao20170821040512.pdf
                    File fileN = getCacheFile(url); //new File(getCacheDir(url), getFileName(url))

                    Log.d(TAG, "????????????????????? " + fileN.toString());
                    if (!fileN.exists()) {
                        boolean mkdir = fileN.createNewFile();
                    }
                    fos = new FileOutputStream(fileN);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        Log.d(TAG, "??????????????????" + fileN.getName() + "??????: " + progress);
                    }
                    fos.flush();
                    Log.d(TAG, "??????????????????,?????????????????????");
                    //2.ACache????????????????????????
                    openFile(fileN);
                } catch (Exception e) {
                    Log.d(TAG, "?????????????????? = " + e.toString());
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d(TAG, "??????????????????");
                File file = getCacheFile(url);
                if (!file.exists()) {
                    Log.d(TAG, "????????????????????????");
                    file.delete();
                }
            }
        });
    }

    private void openFile(File file) {
        if (file != null && !TextUtils.isEmpty(file.toString())) {
            //??????????????????????????????TbsReaderTemp???????????????????????????????????????
            String bsReaderTemp = "/storage/emulated/0/TbsReaderTemp";
            File bsReaderTempFile = new File(bsReaderTemp);
            if (!bsReaderTempFile.exists()) {
                Log.d(TAG, "????????????/storage/emulated/0/TbsReaderTemp??????");
                boolean mkdir = bsReaderTempFile.mkdir();
                if (!mkdir) {
                    Log.e(TAG, "??????/storage/emulated/0/TbsReaderTemp?????????????????????");
                }
            }
            //????????????
            Bundle localBundle = new Bundle();
            Log.d(TAG, file.toString());
            localBundle.putString("filePath", file.toString());
            localBundle.putString("tempPath", Environment.getExternalStorageDirectory() + "/" + "TbsReaderTemp");
            boolean bool = this.tbsReaderView.preOpen(getFileType(file.toString()), false);
            if (bool) {
                this.tbsReaderView.openFile(localBundle);
            }
        } else {
            Log.e(TAG, "?????????????????????");
        }
    }

    private File getCacheDir(String url) {
        return new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/007/");
    }

    private File getCacheFile(String url) {
        File cacheFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/007/"
            + getFileName(url));
        Log.d(TAG, "???????????? = " + cacheFile.toString());
        return cacheFile;
    }

    private String getFileName(String url) {
        return Md5Tool.hashKey(url) + "." + getFileType(url);
    }

    private String getFileType(String paramString) {
        String str = "";

        if (TextUtils.isEmpty(paramString)) {
            Log.d(TAG, "paramString---->null");
            return str;
        }
        Log.d(TAG, "paramString:" + paramString);
        int i = paramString.lastIndexOf('.');
        if (i <= -1) {
            Log.d(TAG, "i <= -1");
            return str;
        }

        str = paramString.substring(i + 1);
        Log.d(TAG, "paramString.substring(i + 1)------>" + str);
        return str;
    }

    private String parseFormat(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private String parseName(String url) {
        String fileName = null;
        try {
            fileName = url.substring(url.lastIndexOf("/") + 1);
        } finally {
            if (TextUtils.isEmpty(fileName)) {
                fileName = String.valueOf(System.currentTimeMillis());
            }
        }
        return fileName;
    }

    TbsReaderView.ReaderCallback readerCallback = new TbsReaderView.ReaderCallback() {
        @Override
        public void onCallBackAction(Integer integer, Object o, Object o1) {

        }
    };
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == 1001) {
                x5WebView.loadUrl(url);
            }
            return false;
        }
    });

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.activity_exit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        x5WebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        x5WebView.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeMessages(1001);
        handler = null;
        x5WebView.reload();
        //??????
        x5WebView.clearCache(true);
        x5WebView.clearFormData();
        x5WebView.destroy();
        if (tbsReaderView != null) tbsReaderView.onStop();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //????????????????????????
        if (isStatusBar) {
            StatusNavBar.with(this).statusBarColor(R.color.transparent).statusBarDarkFont(true).navigationBarColor(R.color.black_degree_50).init();
        }
        setContentView(getContentViewId());
        // ??????action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
//        leftBackLtl = findViewById(R.id.left_back_ltl);
        titleBar = findViewById(R.id.titlebar);
        x5WebView = findViewById(R.id.x5WebView);
        rlRoot = findViewById(R.id.rl_root);
        titleBar.setListener(new CommonTitleBar.OnTitleBarListener() {
            @Override
            public void onClicked(View v, int action, String extra) {
                if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                    finish();
                }
            }
        });
        initView();
    }

    static class JsObject {
        @JavascriptInterface
        public void navigateTo(String jsonData) {//???????????????????????????????????????
        }
    }
}