package com.wear.isse.wear;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by isse on 11/11/2016.
 */

public class SunshineWatchFaceService extends CanvasWatchFaceService {

    private static final String LOG_TAG = SunshineWatchFaceService.class.getSimpleName();

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        static final int BORDER_WIDTH_PX = 5;


        final Rect mCardBounds = new Rect();
        final Paint mPaint = new Paint();
        private Paint mDayPaint;
        private Paint mLinePaint = new Paint();
        private Paint mHighTempPaint;
        private Paint mLowTempPaint;
        private Paint mWeatherImagePaint = new Paint();

        private float mYOffset;
        private float mLineHeight;

        private int mExtra_image_paddingLeft;
        private int mExtra_temp_paddingTop;


        private static final String WEATHER_DATA_PATH = "/weather-data";
        private static final String WEATHER_PATH = "/weather";

        private static final String KEY_HIGH = "high_temp";
        private static final String KEY_LOW = "low_temp";
        private static final String KEY_WEATHER_ID = "weather_id";


        private String mHigh_temp;
        private String mLow_temp;
        private String mWeatherIcon;

        /*
        * To call the Data Layer API, create an instance of GoogleApiClient,
        * the main entry point for any of the Google Play services APIs.
        * */
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API) // Request access only to the Wearable API
                .build();


        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "onCreate");
            }
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .build());


            final int colorBlueInteractive = getResources().getColor(R.color.date_color);
            final int colorGrayAmbient = Color.GRAY;
            mDayPaint = createTextPaint(isInAmbientMode() ? colorGrayAmbient : colorBlueInteractive);
            mLinePaint.setColor(isInAmbientMode() ? colorGrayAmbient : colorBlueInteractive);
            mHighTempPaint = createTextPaint(Color.WHITE);
            mLowTempPaint = createTextPaint(isInAmbientMode() ? colorGrayAmbient : colorBlueInteractive);

        }

        private Paint createTextPaint(int color) {
            return createTextPaint(color, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Log.d(LOG_TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));

            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();

            mYOffset = resources.getDimension(isRound ? R.dimen.date_y_offset_round : R.dimen.date_y_offset);
            mLineHeight = getResources().getDimension(R.dimen.line_height);

            mExtra_image_paddingLeft = (isRound ? 0 : 20);
            mExtra_temp_paddingTop = (isRound ? 20 : 30);

            mDayPaint.setTextSize(resources.getDimension(R.dimen.day_text_size));
            mHighTempPaint.setTextSize(resources.getDimension(R.dimen.temp_text_size));
            mLowTempPaint.setTextSize(resources.getDimension(R.dimen.temp_text_size));


        }

        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            super.onPeekCardPositionUpdate(bounds);
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "onPeekCardPositionUpdate: " + bounds);
            }
            super.onPeekCardPositionUpdate(bounds);
            if (!bounds.equals(mCardBounds)) {
                mCardBounds.set(bounds);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Clear screen.
            int colorActive = getResources().getColor(R.color.interactive_background);
            int colorAmbient = Color.BLACK;
            canvas.drawColor(isInAmbientMode() ? colorAmbient : colorActive);

            // Draw border around card in interactive mode.
            if (!isInAmbientMode()) {
                mPaint.setColor(Color.MAGENTA);
                canvas.drawRect(mCardBounds.left - BORDER_WIDTH_PX,
                        mCardBounds.top - BORDER_WIDTH_PX,
                        mCardBounds.right + BORDER_WIDTH_PX,
                        mCardBounds.bottom + BORDER_WIDTH_PX, mPaint);
            }

            // Fill area under card.
            mPaint.setColor(isInAmbientMode() ? Color.RED : Color.GREEN);
            canvas.drawRect(mCardBounds, mPaint);


            // Only render day if there is no peek card, so they do not bleed into each other
            // in ambient mode.
            String date = getString(R.string.day);
            if (getPeekCardPosition().isEmpty()) {
                canvas.drawText(date, bounds.centerX() - (mDayPaint.measureText(date)) / 2, mYOffset + mLineHeight, mDayPaint);


                //horizontal line
                int line_width = 50;
                int line_y_offset = 20;
                canvas.drawLine(bounds.centerX() - line_width, bounds.exactCenterY() + line_y_offset, bounds.centerX() + line_width, bounds.exactCenterY() + line_y_offset, mLinePaint);


                //high & low temp
                char degree = '\u00B0';
                String high_temp = "25" + degree;
                String low_temp = "16" + degree;
                //String temp = high_temp + " " + low_temp;
                String temp = mHigh_temp + " " + mLow_temp;
                float temp_y_offset = bounds.height() / 5 + mExtra_temp_paddingTop;


                //weather image
                Bitmap image_weather = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                int image_x_offset = bounds.width() / 3 + mExtra_image_paddingLeft;
                float image_y_offset = temp_y_offset - image_weather.getHeight() / 2 - 15;

                if (!isInAmbientMode()) {
                    canvas.drawBitmap(image_weather, bounds.centerX() - image_x_offset, bounds.exactCenterY() + image_y_offset, mWeatherImagePaint);
                    canvas.drawText(temp, bounds.centerX() - (mHighTempPaint.measureText(high_temp)) / 2, bounds.exactCenterY() + temp_y_offset, mHighTempPaint);
                } else {
                    //set temp to center
                    canvas.drawText(temp, bounds.centerX() - (mHighTempPaint.measureText(temp)) / 2, bounds.exactCenterY() + temp_y_offset, mHighTempPaint);
                }
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(LOG_TAG, "onConnected: " + bundle);
            // Now you can use the Data Layer API
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            requestWeatherInfo();
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(LOG_TAG, "onConnectionFailed: " + connectionResult);
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    Log.d(LOG_TAG, path);
                    if (path.equals(WEATHER_DATA_PATH)) {
                        if (dataMap.containsKey(KEY_HIGH)) {
                            mHigh_temp = dataMap.getString(KEY_HIGH);
                            Log.d(LOG_TAG, "High Temperature ==========> " + mHigh_temp);
                        } else {
                            Log.e(LOG_TAG, "High temperature not found");
                        }

                        if (dataMap.containsKey(KEY_LOW)) {
                            mLow_temp = dataMap.getString(KEY_LOW);
                            Log.d(LOG_TAG, "Low Temperature ==========>" + mLow_temp);
                        } else {
                            Log.d(LOG_TAG, "Low temperature not found");
                        }

//                        if (dataMap.containsKey(KEY_WEATHER_ID)) {
//                            int weatherId = dataMap.getInt(KEY_WEATHER_ID);
//                            Drawable b = getResources().getDrawable(Utility.getIconResourceForWeatherCondition(weatherId));
//                            Bitmap icon = ((BitmapDrawable) b).getBitmap();
//                            float scaledWidth = (mTextTempHighPaint.getTextSize() / icon.getHeight()) * icon.getWidth();
//                            mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) mTextTempHighPaint.getTextSize(), true);
//
//                        } else {
//                            Log.d(TAG, "What? no weatherId?");
//                        }

                        invalidate();
                    }
                }
            }
        }


        public void requestWeatherInfo() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();


            Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                    if (!dataItemResult.getStatus().isSuccess()) {
                        Log.d(LOG_TAG, "Failed asking phone for weather data");
                    } else {
                        Log.d(LOG_TAG, "Successfully asked for weather data");
                    }
                }
            });
        }
    }
}
