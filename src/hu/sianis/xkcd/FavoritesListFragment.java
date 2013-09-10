
package hu.sianis.xkcd;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.InstanceState;
import com.googlecode.androidannotations.annotations.ItemClick;
import com.googlecode.androidannotations.annotations.OrmLiteDao;
import com.googlecode.androidannotations.annotations.ViewById;
import com.j256.ormlite.dao.Dao;

import android.app.Activity;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import hu.sianis.xkcd.tools.DatabaseHelper;

import java.sql.SQLException;
import java.util.ArrayList;

@EFragment(R.layout.fragment_favoriteslist)
public class FavoritesListFragment extends Fragment {

    OnFavoriteComicSelectedListener mCallback;

    public interface OnFavoriteComicSelectedListener {
        public void onFavoriteComicSelected(int num);
    }

    @OrmLiteDao(helper = DatabaseHelper.class, model = ComicItem.class)
    Dao<ComicItem, Integer> comicDao;

    @ViewById(android.R.id.list)
    ListView mList;

    @ViewById(android.R.id.empty)
    TextView mEmptyView;

    @InstanceState
    ArrayList<ComicItem> comicItems;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (OnFavoriteComicSelectedListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @AfterViews
    void afterViews() {
        mList.setEmptyView(mEmptyView);
        if (null == comicItems) {
            comicItems = new ArrayList<ComicItem>();
            try {
                comicItems.addAll(comicDao.queryForEq("starred", true));
            } catch (SQLException e) {

            }
        }
        if (!comicItems.isEmpty()) {
            // We need to use a different list item layout for devices older
            // than Honeycomb
            int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? android.R.layout.simple_list_item_activated_1
                    : android.R.layout.simple_list_item_1;
            // Create an array adapter for the list view, using the Ipsum
            // headlines array
            mList.setAdapter(new ArrayAdapter<ComicItem>(getActivity(), layout, android.R.id.text1,
                    comicItems));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getFragmentManager().findFragmentById(R.id.list_fragment) != null) {
            mList.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        }
    }

    @ItemClick(android.R.id.list)
    void itemClick(int position) {

        ComicItem item = (ComicItem)mList.getItemAtPosition(position);
        // Notify the parent activity of selected item
        mCallback.onFavoriteComicSelected(item.num);

        // Set the item as checked to be highlighted when in two-pane layout
        mList.setItemChecked(position, true);
    }
}
