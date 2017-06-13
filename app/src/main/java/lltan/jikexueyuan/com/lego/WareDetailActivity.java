package lltan.jikexueyuan.com.lego;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dmax.dialog.SpotsDialog;
import lltan.jikexueyuan.com.lego.bean.Favorites;
import lltan.jikexueyuan.com.lego.bean.User;
import lltan.jikexueyuan.com.lego.bean.Wares;
import lltan.jikexueyuan.com.lego.http.OkHttpHelper;
import lltan.jikexueyuan.com.lego.http.SpotsCallBack;
import lltan.jikexueyuan.com.lego.utils.CartProvider;
import lltan.jikexueyuan.com.lego.utils.ToastUtils;
import lltan.jikexueyuan.com.lego.widget.EasyGoToolBar;
import okhttp3.Response;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

/**
 * Created by legoer on 2016/5/26.
 */
public class WareDetailActivity extends AppCompatActivity implements View.OnClickListener {
    @ViewInject(R.id.webView)
    private WebView mWebView;
    @ViewInject(R.id.toolbar)
    private EasyGoToolBar mToolBar;

    private Wares mWare;

    private WebAppInterface mAppInterfce;

    private CartProvider cartProvider;

    private SpotsDialog mDialog;


    private OkHttpHelper okHttpHelper = OkHttpHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ware_detail);
        ViewUtils.inject(this);


        Serializable serializable = getIntent().getSerializableExtra(Contants.WARE);
        if(serializable ==null)
            this.finish();


        mDialog = new SpotsDialog(this,"loading....");
        mDialog.show();


        mWare = (Wares) serializable;
        cartProvider = new CartProvider(this);

        initToolBar();
        initWebView();

    }


    private void initWebView(){

        WebSettings settings = mWebView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(false);
        settings.setAppCacheEnabled(true);


        mWebView.loadUrl(Contants.API.WARES_DETAIL);

        mAppInterfce = new WebAppInterface(this);
        mWebView.addJavascriptInterface(mAppInterfce,"appInterface");
        mWebView.setWebViewClient(new WC());


    }



    private void initToolBar(){

        if(mToolBar == null){
            System.out.println("mToolbar为空");
            return;
        }
        mToolBar.setNavigationOnClickListener(this);
        mToolBar.setRightButtonText("分享");

        mToolBar.setRightButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showShare();
            }
        });

    }


    private void showShare() {
        ShareSDK.initSDK(this);


        OnekeyShare oks = new OnekeyShare();
        //关闭sso授权
        oks.disableSSOWhenAuthorize();

// 分享时Notification的图标和文字  2.5.9以后的版本不调用此方法
        //oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));
        // title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
        oks.setTitle(getString(R.string.share));

        // titleUrl是标题的网络链接，仅在人人网和QQ空间使用
        oks.setTitleUrl("http://www.cniao5.com");

        // text是分享文本，所有平台都需要这个字段
        oks.setText(mWare.getName());

        // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
//        oks.setImagePath("/sdcard/test.jpg");//确保SDcard下面存在此张图片
        oks.setImageUrl(mWare.getImgUrl());

        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl("http://www.cniao5.com");
        // comment是我对这条分享的评论，仅在人人网和QQ空间使用
        oks.setComment(mWare.getName());

        // site是分享此内容的网站名称，仅在QQ空间使用
        oks.setSite(getString(R.string.app_name));

        // siteUrl是分享此内容的网站地址，仅在QQ空间使用
        oks.setSiteUrl("http://www.cniao5.com");

// 启动分享GUI
        oks.show(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ShareSDK.stopSDK(this);
    }

    @Override
    public void onClick(View v) {
        this.finish();
    }



    class  WC extends WebViewClient {


        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);


            if(mDialog !=null && mDialog.isShowing())
                mDialog.dismiss();

            mAppInterfce.showDetail();


        }
    }


    private void addToFavorite(){

        User user = EasyGoApplication.getInstance().getUser();

        if(user==null){
            startActivity(new Intent(this,LoginActivity.class));
        }


        Long userId = EasyGoApplication.getInstance().getUser().getId();

        Map<String, Object> params = new HashMap<>();
        params.put("user_id",userId);
        params.put("ware_id",mWare.getId());


        okHttpHelper.postData(Contants.API.FAVORITE_CREATE, params, new SpotsCallBack<List<Favorites>>(this) {
            @Override
            public void onSuccess(Response response, List<Favorites> favorites) {
                ToastUtils.show(WareDetailActivity.this, "已添加到收藏夹");
            }

            @Override
            public void onError(Response response, int code, Exception e) {

                LogUtils.d("code:" + code);
            }
        });

    }



    class WebAppInterface{


        private Context mContext;
        public WebAppInterface(Context context){
            mContext = context;
        }

        @JavascriptInterface
        public  void showDetail(){


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    mWebView.loadUrl("javascript:showDetail("+mWare.getId()+")");

                }
            });
        }


        @JavascriptInterface
        public void buy(long id){

            cartProvider.put(mWare);
            ToastUtils.show(mContext, "已添加到购物车");

        }

        @JavascriptInterface
        public void addFavorites(long id){

            addToFavorite();
        }

    }
}
