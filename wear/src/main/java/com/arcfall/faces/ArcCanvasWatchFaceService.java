package com.arcfall.faces;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;

public class ArcCanvasWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "ArcAngle";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        boolean mLowBitAmbient;

        static final int MSG_UPDATE_TIME = 0;

        static final int INTERACTIVE_UPDATE_RATE_MS = 10;

        private Calendar mCalendar;
        private Paint mSecondPaint, mTimePaint, mInsetPaint, mCirclePaint, mArcPaint;
        private final Rect textBounds = new Rect();

        public static final float RADS_TO_DEGREES = (float) (360.0f / Math.PI / 2.0f);
        public static final float TWO_PI = (float) Math.PI * 2.0f;

        private final Path mPath = new Path();
        private boolean mRegisteredTimeZoneReceiver;

        // handler to update the time once a second in interactive mode
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        // receiver to update the time zone
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(surfaceHolder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(ArcCanvasWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 200, 200, 200);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            mArcPaint = new Paint();
            mArcPaint.setARGB(255, 200, 200, 200);
            mArcPaint.setAntiAlias(true);
            mArcPaint.setStyle(Paint.Style.STROKE);

            mTimePaint = new Paint();
            mTimePaint.setColor(Color.WHITE);
            mTimePaint.setStyle(Paint.Style.FILL);
            mTimePaint.setSubpixelText(true);
            final Typeface tf = Typeface.createFromAsset(ArcCanvasWatchFaceService.this.getAssets(), "fonts/Roboto-Thin.ttf");
            mTimePaint.setTypeface(tf);
            mTimePaint.setAntiAlias(true);
            mTimePaint.setTextSize(60);
            mTimePaint.setDither(true);

            mInsetPaint = new Paint();
            mInsetPaint.setColor(Color.BLACK);
            mInsetPaint.setAntiAlias(true);
            mInsetPaint.setStrokeCap(Paint.Cap.ROUND);

            mCirclePaint = new Paint();
            mCirclePaint.setStyle(Paint.Style.STROKE);
            mCirclePaint.setAntiAlias(true);
            mCirclePaint.setARGB(255, 60, 60, 60);

            mCalendar = Calendar.getInstance();
        }
        
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {

            super.onAmbientModeChanged(inAmbientMode);

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mSecondPaint.setAntiAlias(antiAlias);
            }
            invalidate();
            updateTimer();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode, so we may need to start or stop the timer
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ArcCanvasWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ArcCanvasWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }

            invalidate();
        }

        @Override
        public void onDraw(final Canvas canvas, final Rect bounds) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }

            canvas.drawColor(Color.BLACK);

            mCalendar.setTimeInMillis(System.currentTimeMillis());

            final int width = bounds.width();
            final int height = bounds.height();
            final int minBound = Math.min(width, height);

            final float center = minBound / 2.0f;

            final float longHandLength = center - (minBound / 26.667f);//12.0f;
            final float hrLength = center - (minBound / 4.7f); //68.0f;

            mSecondPaint.setStrokeWidth(minBound / 128.0f);
            mArcPaint.setStrokeWidth(minBound / 128.0f);
            mCirclePaint.setStrokeWidth(minBound / 160.0f);

            //calculating rotations for the different arcs
            final float seconds = mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000.0f;
            final float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60.0f;
            final float minRot = minutes / 60.0f * TWO_PI;
            final float hours = mCalendar.get(Calendar.HOUR) + minutes / 60.0f;
            final float hrRot = hours / 12.0f * TWO_PI;

            final float hrX = (float) Math.sin(hrRot) * hrLength + center;
            final float hrY = (float) -Math.cos(hrRot) * hrLength + center;

            final float minSin = (float) Math.sin(minRot);
            final float minNegCos = (float) -Math.cos(minRot);
            final float minX = minSin * longHandLength + center;
            final float minY = minNegCos * longHandLength + center;

            final float hrDegrees = RADS_TO_DEGREES * hrRot;
            final float minDegrees = RADS_TO_DEGREES * minRot;

            float delta = minDegrees - hrDegrees;
            if (delta > 180.0f) {
                delta -= 360.0f;
            } else if (delta < -180.0f) {
                delta += 360.0f;
            }

            mPath.arcTo(center - hrLength, center - hrLength, center + hrLength, center + hrLength, hrDegrees - 90.0f, delta, true);
            mPath.lineTo(minX, minY);

            //only drawing the second arc if we're awake.
            if (!isInAmbientMode()) {
                canvas.drawCircle(center, center, hrLength, mCirclePaint);
                canvas.drawCircle(center, center, longHandLength, mCirclePaint);

                final float secRot = seconds / 60f * TWO_PI;
                final float secondX = (float) Math.sin(secRot) * longHandLength + center;
                final float secondY = (float) -Math.cos(secRot) * longHandLength + center;

                final float secDegrees = RADS_TO_DEGREES * secRot;
                delta = secDegrees - minDegrees;
                if (delta > 180.0f) {
                    delta -= 360.0f;
                } else if (delta < -180.0f) {
                    delta += 360.0f;
                }

                mPath.arcTo(center - longHandLength, center - longHandLength, center + longHandLength, center + longHandLength, minDegrees - 90.0f, delta, false);
                canvas.drawCircle(secondX, secondY, minBound / 40.0f, mSecondPaint);
            }

            canvas.drawPath(mPath, mArcPaint);
            mPath.reset();

            //dots.
            canvas.drawCircle(minX, minY, minBound / 40.0f, mSecondPaint);
            canvas.drawCircle(minX, minY, minBound / 64.0f, mInsetPaint);
            canvas.drawCircle(hrX, hrY, minBound / 22.857f, mSecondPaint);
            canvas.drawCircle(hrX, hrY, minBound / 29.09091f, mInsetPaint);

            int hourValue = (int) hours;
            if (hourValue == 0) {
                hourValue = 12;
            }

            final String centerHourText = String.format("%02d", (int) hourValue);
            mTimePaint.getTextBounds(centerHourText, 0, centerHourText.length(), textBounds);
            canvas.drawText(centerHourText, center - textBounds.exactCenterX(), center + textBounds.bottom - (minBound / 64.0f), mTimePaint);

            final String centerMinuteText = String.format("%02d", (int) minutes);
            mTimePaint.getTextBounds(centerMinuteText, 0, centerMinuteText.length(), textBounds);
            canvas.drawText(centerMinuteText, center - textBounds.exactCenterX(), center - textBounds.top + (minBound / 64.0f), mTimePaint);
        }
    }
}
