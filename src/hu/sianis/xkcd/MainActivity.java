
package hu.sianis.xkcd;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Background;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OrmLiteDao;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.sharedpreferences.Pref;
import com.j256.ormlite.dao.Dao;

import org.json.JSONObject;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.ShareActionProvider;

import hu.sianis.xkcd.tools.DatabaseHelper;
import hu.sianis.xkcd.tools.Pref_;
import hu.sianis.xkcd.tools.Tools;

import java.util.Calendar;
import java.util.Random;

@EActivity(R.layout.activity_main)
public class MainActivity extends FragmentActivity implements OnPageChangeListener {

    @ViewById(android.R.id.primary)
    ViewPager mPager;

    @OrmLiteDao(helper = DatabaseHelper.class, model = ComicItem.class)
    Dao<ComicItem, Integer> comicDao;

    @Pref
    Pref_ pref;

    private Integer size;

    private Random random = new Random();

    private ShareActionProvider mShareActionProvider;

    private Integer savedPage = null;

    @Override
    protected void onCreate(Bundle savedInstance) {
        if (savedInstance != null) {
            savedPage = savedInstance.getInt("pageItem");
        }
        super.onCreate(savedInstance);
    }

    @AfterViews
    void afterViews() {
        mPager.setOnPageChangeListener(this);

        Calendar now = Calendar.getInstance();
        Calendar lastUpdate = Calendar.getInstance();
        lastUpdate.setTimeInMillis(pref.lastUpdate().get());
        lastUpdate.add(Calendar.MINUTE, 30);

        if (now.after(lastUpdate)) {
            // Try to run update
            Log.d("MainActivity", "Load new items from web");
            getLastComic();
        } else {
            loadFromDb();
        }
    }

    @Background
    void getLastComic() {
        JSONObject response = Tools.getResponse(Consants.URL_LAST);
        if (null != response) {
            pref.lastUpdate().put(Calendar.getInstance().getTimeInMillis());
            ComicItem item = ComicItem.parse(response);
            size = item.num;
            setAdapter(new ComicPagerAdapter(getSupportFragmentManager(), size.intValue()),
                    size.intValue() - 1);
            Log.d("MainActivity", "Items loaded from web");
        } else {
            loadFromDb();
        }
    }

    @UiThread
    void setAdapter(ComicPagerAdapter adapter) {
        mPager.setAdapter(adapter);
        if (savedPage != null) {
            mPager.setCurrentItem(savedPage);
            savedPage = null;
        }
    }

    @UiThread
    void setAdapter(ComicPagerAdapter adapter, int page) {
        mPager.setAdapter(adapter);
        if (savedPage != null) {
            mPager.setCurrentItem(savedPage);
            savedPage = null;
        } else {
            mPager.setCurrentItem(page);
        }
    }

    protected static class ComicPagerAdapter extends FragmentStatePagerAdapter {

        int size;

        public ComicPagerAdapter(FragmentManager fm, final int size) {
            super(fm);
            this.size = size;
        }

        @Override
        public Fragment getItem(int position) {
            return ComicFragment_.builder().number(position + 1).build();
        }

        @Override
        public int getCount() {
            return size;
        }
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageSelected(int position) {
        if (position != size - 1) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            getActionBar().setHomeButtonEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(false);
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(Consants.URL_BROWSER, position + 1));
        sendIntent.setType("text/plain");
        setShareIntent(sendIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(hu.sianis.xkcd.R.menu.main_menu, menu);

        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider)item.getActionProvider();

        return true;
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @OptionsItem
    void random() {
        if (null != size) {
            mPager.setCurrentItem(random.nextInt(size), false);
        }
    }

    @OptionsItem
    void favorites() {
        FavoritesActivity_.intent(this).start();
    }

    @Background
    void loadFromDb() {
        // Try to load from database
        try {
            if (comicDao.countOf() > 0L) {
                size = comicDao.queryForFirst(comicDao.queryBuilder().orderBy("num", false)
                        .prepare()).num;

                setAdapter(new ComicPagerAdapter(getSupportFragmentManager(), size),
                        size.intValue() - 1);
                Log.d("MainActivity", "Load items from db");
            } else {
                if (!isDataConnectionOn(this)) {
                    Builder builder = new Builder(this);
                    builder.setTitle(R.string.no_network_connection);
                    builder.setMessage(R.string.retry);
                    builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getLastComic();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    showDialog(builder);
                }
            }
        } catch (Exception e) {
            Log.e("", "", e);
        }
    }

    @OptionsItem
    void jumpto() {
        Builder builder = new Builder(this);
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(size);
        builder.setTitle(getString(R.string.pic_number, size));
        builder.setView(numberPicker);
        builder.setPositiveButton(android.R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPager.setCurrentItem(numberPicker.getValue() - 1);
            }
        });
        builder.setNeutralButton(android.R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

    @OptionsItem
    void openInBrowser() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(
                Consants.URL_BROWSER, mPager.getCurrentItem() + 1)));
        startActivity(browserIntent);
    }

    @OptionsItem
    void explain() {
        ExplainActivity_.intent(this)
                .url(String.format(Consants.URL_EXPLAIN, mPager.getCurrentItem() + 1)).start();
    }

    @OptionsItem(android.R.id.home)
    void home() {
        mPager.setCurrentItem(size - 1, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pageItem", mPager.getCurrentItem());
    }

    private static boolean isDataConnectionOn(final Context mContext) {
        final ConnectivityManager connectionManager = (ConnectivityManager)mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            if (connectionManager.getActiveNetworkInfo().isConnected()) {
                return true;
            } else {
                return false;
            }
        } catch (final NullPointerException e) {
            return false;
        }
    }

    @UiThread
    void showDialog(Builder builder) {
        builder.show();
    }
}
