package com.wear.isse.wear;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

/**
 * Created by isse on 11/11/2016.
 */

public class DigitalWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "DigitalWatchFaceService";

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        static final int BORDER_WIDTH_PX = 5;


        final Rect mCardBounds = new Rect();
        final Paint mPaint = new Paint();
        private Paint mDayPaint;
        private Paint mLinePaint = new Paint();
        private Paint mHighTempPaint;
        private Paint mLowTempPaint;
        private Paint mWeatherImagePaint = new Paint();

        private float mXOffset;
        private float mXDayOffset;
        private float mYOffset;
        private float mLineHeight;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .build());

            mYOffset = getResources().getDimension(R.dimen.date_y_offset);
            mLineHeight = getResources().getDimension(R.dimen.line_height);

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
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));

            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = DigitalWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound ? R.dimen.fit_x_offset_round : R.dimen.fit_x_offset);
            mXDayOffset = resources.getDimension(isRound ? R.dimen.day_offset_round : R.dimen.day_x_offset);
            mDayPaint.setTextSize(resources.getDimension(R.dimen.day_text_size));
            mHighTempPaint.setTextSize(resources.getDimension(R.dimen.temp_text_size));
            mLowTempPaint.setTextSize(resources.getDimension(R.dimen.temp_text_size));


        }

        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            super.onPeekCardPositionUpdate(bounds);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPeekCardPositionUpdate: " + bounds);
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
                //TODO: draw objects inside this if statement
                canvas.drawText(date, bounds.centerX() - (mDayPaint.measureText(date)) / 2, mYOffset + mLineHeight, mDayPaint);
            }

            //horizontal line
            int line_width = 50;
            int line_y_offset = 30;
            canvas.drawLine(bounds.centerX() - line_width, bounds.exactCenterY() + line_y_offset, bounds.centerX() + line_width, bounds.exactCenterY() + line_y_offset, mLinePaint);

            //high temp
            char degree = '\u00B0';
            String high_temp = "25" + degree;
            int temp_y_offset = 120;
            canvas.drawText(high_temp, bounds.centerX() - (mHighTempPaint.measureText(high_temp)) / 2, bounds.exactCenterY() + temp_y_offset, mHighTempPaint);


            //low temp
            String low_temp = "16" + degree;
            int temp_x_offset = 50;
            canvas.drawText(low_temp, bounds.centerX() + temp_x_offset, bounds.exactCenterY() + temp_y_offset, mLowTempPaint);

            //weather image
            Bitmap image_weather = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            int image_x_offset = 130;
            int image_y_offset = 55;
            canvas.drawBitmap(image_weather, bounds.centerX() - image_x_offset, bounds.exactCenterY() + image_y_offset, mWeatherImagePaint);

        }
    }
}
