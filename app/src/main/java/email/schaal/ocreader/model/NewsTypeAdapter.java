package email.schaal.ocreader.model;

import android.util.Log;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;

import java.io.IOException;

/**
 * Base class for TypeAdapters
 */
public abstract class NewsTypeAdapter<T> extends JsonAdapter<T> {
    private static final String TAG = NewsTypeAdapter.class.getName();

    protected String nullSafeString(JsonReader in) throws IOException {
        if(in.peek() == JsonReader.Token.NULL) {
            Log.w(TAG, "unexpected NULL in json");
            in.nextNull();
            return null;
        } else
            return in.nextString();
    }
}
