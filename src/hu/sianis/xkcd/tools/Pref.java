
package hu.sianis.xkcd.tools;

import com.googlecode.androidannotations.annotations.sharedpreferences.DefaultLong;
import com.googlecode.androidannotations.annotations.sharedpreferences.SharedPref;
import com.googlecode.androidannotations.annotations.sharedpreferences.SharedPref.Scope;

@SharedPref(value = Scope.APPLICATION_DEFAULT)
public interface Pref {

    @DefaultLong(0L)
    long lastUpdate();

}
