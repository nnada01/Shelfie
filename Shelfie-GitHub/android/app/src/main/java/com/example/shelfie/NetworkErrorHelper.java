package com.example.shelfie;

import android.content.Context;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

public final class NetworkErrorHelper {
    private NetworkErrorHelper() {
    }

    // Maps a Volley error to a user-facing string (no network, timeout, or generic fallback)
    public static String message(Context context, VolleyError error) {
        if (error instanceof NoConnectionError || error instanceof NetworkError) {
            return context.getString(R.string.error_network)
                    + "\n"
                    + context.getString(R.string.error_network_url, ApiConfig.BASE_URL);
        }
        if (error instanceof TimeoutError) {
            return context.getString(R.string.error_timeout);
        }
        return context.getString(R.string.error_generic);
    }
}
