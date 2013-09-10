
package hu.sianis.xkcd;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.FragmentArg;
import com.googlecode.androidannotations.annotations.OrmLiteDao;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.j256.ormlite.dao.Dao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

import hu.sianis.xkcd.SlideAwayAnimator.MOVE;
import hu.sianis.xkcd.tools.DatabaseHelper;
import hu.sianis.xkcd.tools.Tools;

import java.sql.SQLException;

@EFragment(R.layout.fragment_comic)
public class ComicFragment extends Fragment {

    @FragmentArg
    int number;

    @ViewById(android.R.id.text1)
    TextView title;

    @ViewById(android.R.id.text2)
    TextView alt;

    @ViewById
    View titleContainer;

    @ViewById
    View altContainer;

    @ViewById(android.R.id.button1)
    ImageButton starButton;

    private ComicItem item;

    @ViewById(android.R.id.icon)
    WebView image;

    @OrmLiteDao(helper = DatabaseHelper.class, model = ComicItem.class)
    Dao<ComicItem, Integer> comicDao;

    @ViewById(android.R.id.empty)
    View empty;

    @ViewById
    View progress;

    private RefreshBroadcastReceiver refreshReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }

    @AfterViews
    void afterViews() {

        image.getSettings().setBuiltInZoomControls(true);
        image.getSettings().setDisplayZoomControls(false);
        image.getSettings().setRenderPriority(RenderPriority.HIGH);
        image.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        image.getSettings().setDefaultZoom(ZoomDensity.FAR);
        image.getSettings().setLoadWithOverviewMode(true);

        if (0 != number) {
            if (null == item) {
                downloadContent();
            } else {
                loadImage(item);
                updateItemStar();
            }

            final GestureDetector mGestureDetector = new GestureDetector(image.getContext(),
                    new SimpleOnGestureListener() {
                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            titleSlider.toggle();
                            altSlider.toggle();
                            return super.onSingleTapConfirmed(e);
                        }

                        // @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            switch (image.getSettings().getDefaultZoom()) {
                                case FAR:
                                    image.getSettings().setDefaultZoom(ZoomDensity.MEDIUM);
                                    break;

                                case MEDIUM:
                                    image.getSettings().setDefaultZoom(ZoomDensity.CLOSE);
                                    break;

                                case CLOSE:
                                    image.getSettings().setDefaultZoom(ZoomDensity.FAR);
                                    break;
                            }

                            return super.onDoubleTap(e);
                        }
                    });

            image.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return mGestureDetector.onTouchEvent(event);
                }
            });
        }
    }

    @Click(android.R.id.text2)
    void altClicked() {
        if (null != alt.getTag()) {
            if ((Boolean)alt.getTag()) {
                alt.setTag(false);
                alt.setSingleLine(true);
            } else {
                alt.setTag(true);
                alt.setSingleLine(false);
            }
        } else {
            alt.setTag(true);
            alt.setSingleLine(false);
        }
    }

    @Background
    void downloadContent() {
        try {
            if (comicDao.idExists(number)) {
                item = comicDao.queryForId(number);
            }
        } catch (Exception e) {

        } finally {
            if (item == null) {
                try {
                    item = ComicItem.parse(Tools.getResponse(String.format(Consants.URL_COMIC,
                            number)));
                    comicDao.createIfNotExists(item);
                } catch (Exception e) {
                }
            }
            if (item != null) {
                setTexts(item);
                if (Tools.downloadComic(getActivity(), item.link)) {
                    loadLink(item.link);
                } else {
                    // TODO Load from web into webview
                }
                updateItemStar();
            } else {
                showEmptyText();
            }
        }
    }

    @UiThread
    void showEmptyText() {
        // Register receiver
        refreshReceiver = new RefreshBroadcastReceiver(this);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(refreshReceiver,
                new IntentFilter(Consants.REFRESH_ACTION));

        progress.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
    }

    @Background
    void loadImage(ComicItem item) {
        setTexts(item);
        loadLink(item.link);
    }

    private SlideAwayAnimator titleSlider;

    private SlideAwayAnimator altSlider;

    @UiThread
    void setTexts(ComicItem item) {
        progress.setVisibility(View.GONE);
        title.setText(item.title);
        alt.setText(item.alt);
        titleSlider = new SlideAwayAnimator(titleContainer, MOVE.UP);
        altSlider = new SlideAwayAnimator(altContainer, MOVE.DOWN);
        titleContainer.animate().alpha(1);
        altContainer.animate().alpha(1);
    }

    @UiThread
    void loadLink(String link) {
        link = Tools.getPathForUrl(image.getContext(), link);
        if (null == link) {

        } else {
            String HTML_FORMAT = "<html><head><style>html,body,#wrapper{height:100%%;width:100%%;margin:0;padding:0;border:0;backgorund-color:red;}#wrapper td{vertical-align:middle;text-align:center}</style></head><body><table id=\"wrapper\"><tr>\n<td><img src = \"file://%s\" /></td></tr></table></body></html>";

            final String html = String.format(HTML_FORMAT, link);

            image.loadDataWithBaseURL("", html, "text/html", "UTF-8", "");
        }
    }

    @Background
    @Click(android.R.id.button1)
    void toggleStar() {
        item.starred = !item.starred;
        updateItemStar();
        try {
            comicDao.update(item);
        } catch (SQLException e) {

        }
    }

    @Click(android.R.id.button2)
    void reload() {
        reload(true);
    }

    @UiThread
    void reload(boolean sendbroadcast) {
        // Unregister receiver
        if (refreshReceiver != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(refreshReceiver);
        }

        if (sendbroadcast) {
            Intent intent = new Intent(Consants.REFRESH_ACTION);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
        empty.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        downloadContent();
    }

    @UiThread
    void updateItemStar() {
        starButton.setImageResource(item.starred ? R.drawable.star_on_selector
                : R.drawable.star_off_selector);
    }

    public void setItem(ComicItem item) {
        this.item = item;
        loadImage(item);
        updateItemStar();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            try {
                if (item != null && comicDao.refresh(item) == 1) {
                    updateItemStar();
                }
            } catch (Exception e) {

            }
        }
    }

    private class RefreshBroadcastReceiver extends BroadcastReceiver {

        ComicFragment fragment;

        public RefreshBroadcastReceiver(ComicFragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (fragment != null) {
                fragment.reload(false);
            }
        }
    }
}
