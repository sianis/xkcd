
package hu.sianis.xkcd;

import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Extra;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;

@EActivity(R.layout.activity_explain)
public class ExplainActivity extends Activity {

    @Extra
    String url;

    @ViewById
    WebView web;

    @AfterInject
    void afterInject() {
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @AfterViews
    void afterViews() {
        web.getSettings().setJavaScriptEnabled(true);
        web.setWebChromeClient(new WebChromeClient());
        web.getSettings().setUseWideViewPort(true);
        web.getSettings().setDefaultZoom(ZoomDensity.FAR);
        web.loadUrl(url);
    }

    @OptionsItem(android.R.id.home)
    void home() {
        finish();
    }
}
