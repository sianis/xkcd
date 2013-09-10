
package hu.sianis.xkcd;

import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.OptionsItem;
import com.googlecode.androidannotations.annotations.OrmLiteDao;
import com.j256.ormlite.dao.Dao;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import hu.sianis.xkcd.FavoritesListFragment.OnFavoriteComicSelectedListener;
import hu.sianis.xkcd.tools.DatabaseHelper;

import java.sql.SQLException;

@EActivity
public class FavoritesActivity extends FragmentActivity implements OnFavoriteComicSelectedListener {

    @OrmLiteDao(helper = DatabaseHelper.class, model = ComicItem.class)
    Dao<ComicItem, Integer> comicDao;

    private ShareActionProvider mShareActionProvider;

    private int selectedComic = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_favorites);

        // Check whether the activity is using the layout version with
        // the fragment_container FrameLayout. If so, we must add the first
        // fragment
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            FavoritesListFragment firstFragment = FavoritesListFragment_.builder().build();

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }
    }

    @Override
    public void onFavoriteComicSelected(int num) {
        selectedComic = num;
        invalidateOptionsMenu();

        // The user selected the headline of an article from the
        // HeadlinesFragment

        // Capture the article fragment from the activity layout
        ComicFragment_ comicFragment = (ComicFragment_)getSupportFragmentManager()
                .findFragmentById(R.id.comic_fragment);

        if (comicFragment != null) {
            try {
                comicFragment.setItem(comicDao.queryForId(num));
            } catch (SQLException e) {

            }
        } else {

            // Create fragment and give it an argument for the selected article
            ComicFragment newFragment = ComicFragment_.builder().number(num).build();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
    }

    @OptionsItem(android.R.id.home)
    void home() {
        finish();
    }

    @OptionsItem
    void openInBrowser() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(
                Consants.URL_BROWSER, selectedComic)));
        startActivity(browserIntent);
    }

    @OptionsItem
    void explain() {
        ExplainActivity_.intent(this).url(String.format(Consants.URL_EXPLAIN, selectedComic))
                .start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (selectedComic != 0) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(Consants.URL_BROWSER, selectedComic));
            sendIntent.setType("text/plain");
            setShareIntent(sendIntent);

            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(hu.sianis.xkcd.R.menu.favorites_menu, menu);

            MenuItem item = menu.findItem(R.id.menu_item_share);
            mShareActionProvider = (ShareActionProvider)item.getActionProvider();

            return true;
        } else {
            return super.onCreateOptionsMenu(menu);
        }

    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public void onBackPressed() {
        selectedComic = 0;
        invalidateOptionsMenu();
        super.onBackPressed();
    }
}
