package tm.veriloft.mapsplayground;

import android.location.Location;

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

    /**
     * Sometimes OSRM api returns reversed points.
     * So find true end point by finding nearest point to requestedEndPoint between first and last items of returned route points array
     *
     * @param requestEndPoint    requested end point to api
     * @param returnedPointFirst first item from returned array
     * @param returnedPointLast  last item from returned array
     * @return true end point, from given returnedPointFirst or returnedPointLast
     */
    public static LatLng findTrueEndPoint( LatLng requestEndPoint, LatLng returnedPointFirst, LatLng returnedPointLast ) {
        float[] resultsFirst = new float[1];
        Location.distanceBetween(requestEndPoint.latitude, requestEndPoint.longitude, returnedPointFirst.latitude, returnedPointFirst.longitude, resultsFirst);

        float[] resultsSecond = new float[1];
        Location.distanceBetween(requestEndPoint.latitude, requestEndPoint.longitude, returnedPointLast.latitude, returnedPointLast.longitude, resultsSecond);

        return (resultsFirst[0] > resultsSecond[0]) ? returnedPointLast : returnedPointFirst;
    }
}
