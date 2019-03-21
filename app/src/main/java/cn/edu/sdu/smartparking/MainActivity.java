package cn.edu.sdu.smartparking;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.SupportMapFragment;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.amap.api.navi.model.NaviLatLng;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener,
        AMapLocationListener, AMap.OnMarkerClickListener {
    /** mess up ... i know **/
    private AMap mMap;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private Marker mLocationMarker;
    private Marker mSelectedMarker;
    private Circle mLocationCircle;
    private AMapLocation mCurrentLocation;
    private RadioGroup radioGroup;
    private Button goButton;

    private ArrayList<LatLng> mPosList = new ArrayList<LatLng>();
    private ArrayList<Marker> mMarks = new ArrayList<Marker>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        radioGroup = findViewById(R.id.radGroup);
        radioGroup.setOnCheckedChangeListener(this);
        goButton = findViewById(R.id.btnNav);
        goButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startAMapNavi(mSelectedMarker);
            }
        });

        setUpMapIfNeeded();
        initParkingPosition();
        initLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyLocation();
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
            mMap.setOnMarkerClickListener(this);
        }
    }

    private void destroyLocation() {
        if (mLocationClient != null) {
            mLocationClient.unRegisterLocationListener(this);
            mLocationClient.onDestroy();
        }
    }

    private void initParkingPosition() {
        mPosList.add(new LatLng(37.527782, 122.061519));
        mPosList.add(new LatLng(37.527608, 122.061503));
        mPosList.add(new LatLng(37.527233, 122.061525));
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setOnceLocation(true);
        mLocationClient = new AMapLocationClient(this.getApplicationContext());
        mLocationClient.setLocationListener(this);
        mLocationClient.startLocation();

        for(LatLng position : mPosList) {
            mMarks.add(addGrowMarker(position));
        }

    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

        if (aMapLocation == null || aMapLocation.getErrorCode() != AMapLocation.LOCATION_SUCCESS) {
            Toast.makeText(this,aMapLocation.getErrorInfo()+"  "+aMapLocation.getErrorCode(),Toast.LENGTH_LONG).show();
            return;
        }
        mCurrentLocation = aMapLocation;
        LatLng curLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
        if (mLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(curLatLng);
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.navi_map_gps_locked));
            mLocationMarker = mMap.addMarker(markerOptions);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()), 18f));

        }
        if (mLocationCircle == null) {
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(curLatLng);
            circleOptions.radius(aMapLocation.getAccuracy());
            circleOptions.strokeWidth(2);
            circleOptions.strokeColor(getResources().getColor(R.color.stroke));
            circleOptions.fillColor(getResources().getColor(R.color.fill));
            mLocationCircle = mMap.addCircle(circleOptions);
        }
    }

    private Marker addGrowMarker(LatLng markerPosition) {
        MarkerOptions options = new MarkerOptions();
        options.position(markerPosition);
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),R.drawable.icon_openmap_mark)));
        Marker marker = mMap.addMarker(options);
        Animation markerAnimation = new ScaleAnimation(0, 1, 0, 1); //初始化生长效果动画
        markerAnimation.setDuration(800);  //设置动画时间 单位毫秒
        marker.setAnimation(markerAnimation);
        marker.setAlpha(0.6f);
        return marker;
    }

    /**
     * 点击一键导航按钮跳转到导航页面
     *
     * @param marker
     */
    private void startAMapNavi(Marker marker) {
        if (mCurrentLocation == null) {
            return;
        }
        Intent intent = new Intent(this, RouteNaviActivity.class);
        intent.putExtra("gps", true);
        intent.putExtra("start", new NaviLatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        intent.putExtra("end", new NaviLatLng(marker.getPosition().latitude, marker.getPosition().longitude));
        startActivity(intent);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.startAnimation();
        return true;
    }

    public void zoomToSpan() {
        try {
            LatLngBounds bounds = getLatLngBounds();
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 5));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private LatLngBounds getLatLngBounds() {
        LatLngBounds.Builder b = LatLngBounds.builder();
        for (LatLng position : mPosList) {
            b.include(position);
        }
        return b.build();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Marker marker = mMarks.get(0);
        switch (checkedId) {
            case R.id.radBtn1:
                marker = mMarks.get(0);
                break;
            case R.id.radBtn2:
                marker = mMarks.get(1);
                break;
            case R.id.radBtn3:
                marker = mMarks.get(2);
                break;
            default:
                break;
        }
        marker.startAnimation();
        marker.setAlpha(1);
        mSelectedMarker = marker;
        zoomToSpan();
    }


}
