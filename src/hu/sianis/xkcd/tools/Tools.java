
package hu.sianis.xkcd.tools;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Environment;
import android.util.Log;

import hu.sianis.xkcd.Consants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Tools {

    private static final String TAG = Tools.class.getName();

    public static JSONObject getResponse(String url) {
        Log.d(TAG, url);
        JSONObject ret = null;
        AndroidHttpClient client = AndroidHttpClient
                .newInstance(Consants.ANDROIDHTTPCLIENT_INSTANCE);
        try {
            HttpGet get = new HttpGet(url);
            AndroidHttpClient.modifyRequestToAcceptGzipResponse(get);
            InputStream is = AndroidHttpClient.getUngzippedContent(client.execute(get).getEntity());
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(new InputStreamReader(client.execute(get)
                    .getEntity().getContent()));

            String line;
            StringBuilder retBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                retBuilder.append(line);
            }

            br.close();
            isr.close();
            is.close();

            ret = new JSONObject(retBuilder.toString());

        } catch (Exception e) {
            ret = null;
        } finally {
            if (null != client) {
                client.close();
            }
        }
        return ret;
    }

    public static boolean downloadComic(Context context, String url) {
        Log.d(TAG, url);

        boolean ret = true;
        if (context != null) {
            File file = new File(context.getExternalFilesDir(null), extractFileName(url));
            if (null != file && file.exists() && file.canRead()) {
                Log.d(TAG, "Bitmap from file");
            } else {
                try {
                    HttpGet get = new HttpGet(url);
                    AndroidHttpClient.modifyRequestToAcceptGzipResponse(get);
                    AndroidHttpClient client = AndroidHttpClient
                            .newInstance(Consants.ANDROIDHTTPCLIENT_INSTANCE);
                    try {
                        HttpResponse response = client.execute(get);
                        if (response.getStatusLine().getStatusCode() == 301) {
                            get = new HttpGet(response.getFirstHeader("Location").getValue());
                            response = client.execute(get);
                            if (response.getStatusLine().getStatusCode() == 302) {
                                get = new HttpGet(response.getFirstHeader("Location").getValue());
                                response = client.execute(get);
                            }
                        }
                        InputStream is = AndroidHttpClient
                                .getUngzippedContent(response.getEntity());

                        if (isSdCardWritable()) {
                            Log.d(TAG, "Bitmap to file");
                            OutputStream out = new FileOutputStream(file);

                            int read = 0;
                            byte[] bytes = new byte[1024];

                            while ((read = is.read(bytes)) != -1) {
                                out.write(bytes, 0, read);
                            }

                            is.close();
                            out.flush();
                            out.close();
                        } else {
                            ret = false;
                            Log.d(TAG, "Bitmap from web");
                            is.close();
                        }
                    } finally {
                        if (null != client) {
                            client.close();
                        }
                    }
                } catch (Exception e) {
                    ret = false;
                    Log.e(TAG, "downloadComic", e);
                }
            }
        }
        return ret;
    }

    public static boolean isSdCardReadable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || Environment.MEDIA_MOUNTED_READ_ONLY
                        .equals(Environment.getExternalStorageState());
    }

    public static boolean isSdCardWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static String extractFileName(String path) {

        if (path == null) {
            return null;
        }
        String newpath = path.replace('\\', '/');
        int start = newpath.lastIndexOf("/");
        if (start == -1) {
            start = 0;
        } else {
            start = start + 1;
        }
        String pageName = newpath.substring(start, newpath.length());

        return pageName;
    }

    public static String getPathForUrl(Context context, String url) {
        return new File(context.getExternalFilesDir(null), extractFileName(url)).toString();
    }
}
