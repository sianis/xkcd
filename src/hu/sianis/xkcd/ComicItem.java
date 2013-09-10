
package hu.sianis.xkcd;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONObject;

import java.io.Serializable;

@DatabaseTable(tableName = "comics")
public class ComicItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @DatabaseField(id = true)
    public int num;

    @DatabaseField
    public int year;

    @DatabaseField
    public int month;

    @DatabaseField
    public int day;

    @DatabaseField
    public String title;

    @DatabaseField
    public String link;

    @DatabaseField
    public String alt;

    @DatabaseField(defaultValue = "false")
    public boolean starred;

    public static ComicItem parse(JSONObject response) {
        ComicItem ret = new ComicItem();
        ret.year = response.optInt("year");
        ret.month = response.optInt("month");
        ret.day = response.optInt("day");
        ret.num = response.optInt("num");
        ret.title = response.optString("safe_title");
        ret.link = response.optString("img");
        ret.alt = response.optString("alt");
        return ret;
    }

    @Override
    public String toString() {
        return title;
    }

}
