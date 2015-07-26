package tm.veriloft.mapsplayground;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by alashov on 26/07/15.
 */

public class MapUtils {

    /**
     * Decode polyline with 1E6 precision
     *
     * @param encoded polyline
     * @return array of latlngs
     */
    public static ArrayList<LatLng> decodePoly( String encoded ) {
        return decodePoly(encoded, 1E6);
    }

    /**
     * Decode poly with given precision
     *
     * @param encoded polyline
     * @return array of latlngs
     */
    public static ArrayList<LatLng> decodePoly( String encoded, double precision ) {
        ArrayList<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~ (result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~ (result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / precision)), (((double) lng / precision)));
            poly.add(p);
        }

        return poly;
    }

}
