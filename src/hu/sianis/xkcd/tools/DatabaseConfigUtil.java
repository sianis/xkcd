
package hu.sianis.xkcd.tools;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import hu.sianis.xkcd.ComicItem;

import java.io.IOException;
import java.sql.SQLException;

public class DatabaseConfigUtil extends OrmLiteConfigUtil {

    private static final Class<?>[] classes = new Class[] {
        ComicItem.class
    };

    public static void main(String[] args) throws SQLException, IOException {
        writeConfigFile("ormlite_config.txt", classes);
    }
}
