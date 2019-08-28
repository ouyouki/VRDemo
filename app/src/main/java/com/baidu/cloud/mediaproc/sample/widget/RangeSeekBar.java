package com.baidu.cloud.mediaproc.sample.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.util.BitmapUtil;
import com.baidu.cloud.mediaproc.sample.util.PixelUtil;

/**
 * Widget that lets users select a minimum and maximum value on a given numerical range.
 * The range value types can be one of Long, Double, Integer, Float, Short, Byte or BigDecimal.<br>
 * <br>
 * Improved {@link android.view.MotionEvent} handling for smoother use, anti-aliased painting for improved aesthetics.
 *
 * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
 * @author Peter Sinnott (psinnott@gmail.com)
 * @author Thomas Barrasso (tbarrasso@sevenplusandroid.org)
 * @author Alex Florescu (alex@florescu.org)
 * @author Michael Keppler (bananeweizen@gmx.de)
 */
public class RangeSeekBar extends AppCompatImageView {
    /**
     * An invalid pointer id.
     */
    public static final int INVALID_POINTER_ID = 255;

    // Localized constants from MotionEvent for compatibility
    // with API < 8 "Froyo".
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;

    public static final Integer DEFAULT_MINIMUM = 0;
    public static final Integer DEFAULT_MAXIMUM = 100;
    public static final int HEIGHT_IN_DP = 30;

    private static final int INITIAL_PADDING_IN_DP = 8;
    private static final int DEFAULT_TEXT_SIZE_IN_DP = 14;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap thumbImage;

    private float thumbHalfWidth;
    private float thumbHalfHeight;

    private float padding;
    protected int absoluteMinValue;
    protected int absoluteMaxValue;
    protected double normalizedMinValue = 0d;
    protected double normalizedMaxValue = 1d;
    protected double minDeltaForDefault = 0;
    private Thumb pressedThumb = null;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener listener;

    private float downMotionX;

    private int activePointerId = INVALID_POINTER_ID;

    private int scaledTouchSlop;

    private boolean isDragging;

    private int textSize;
    private RectF borderRect;

    private boolean lockRange;
    private int timeInterval = 3000;
    private boolean showTextBelowThumbs;
    private float internalPad;
    private int defaultColor;

    private Bitmap backgroundBitmap;

    private Bitmap foregroundBitmap;
    private Rect progressDrawableRect;
    private Matrix matrix = new Matrix();

    public RangeSeekBar(Context context) {
        super(context);
        init(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int thumbNormal = R.drawable.ic_left_chose_red_24dp;

        if (attrs == null) {
            setRangeToDefaultValues();
            internalPad = PixelUtil.dpToPx(context, INITIAL_PADDING_IN_DP);
            defaultColor = Color.GRAY;
            showTextBelowThumbs = true;
        } else {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar, 0, 0);
            try {
                timeInterval = a.getInteger(R.styleable.RangeSeekBar_absoluteInterval, 15000);
                setRangeValues(
                        a.getInteger(R.styleable.RangeSeekBar_absoluteMinValue, DEFAULT_MINIMUM),
                        a.getInteger(R.styleable.RangeSeekBar_absoluteMaxValue, DEFAULT_MAXIMUM)
                );
                showTextBelowThumbs = a.getBoolean(R.styleable.RangeSeekBar_valuesAboveThumbs, true);
                lockRange = a.getBoolean(R.styleable.RangeSeekBar_lockRange, false);
                internalPad = a.getDimensionPixelSize(R.styleable.RangeSeekBar_internalPadding, INITIAL_PADDING_IN_DP);
                defaultColor = a.getColor(R.styleable.RangeSeekBar_defaultColor, Color.GRAY);

                Drawable rangeBackground = a.getDrawable(R.styleable.RangeSeekBar_rangeBackground);
                if (rangeBackground != null) {
                    backgroundBitmap = BitmapUtil.drawableToBitmap(rangeBackground);
                }
                Drawable rangeForeground = a.getDrawable(R.styleable.RangeSeekBar_rangeForeground);
                if (rangeForeground != null) {
                    foregroundBitmap = BitmapUtil.drawableToBitmap(rangeForeground);
                }
                Drawable normalDrawable = a.getDrawable(R.styleable.RangeSeekBar_thumbNormal);
                if (normalDrawable != null) {
                    thumbImage = BitmapUtil.drawableToBitmap(normalDrawable);
                }
            } finally {
                a.recycle();
            }
        }

        if (thumbImage == null) {
            thumbImage = BitmapFactory.decodeResource(getResources(), thumbNormal);
        }
        thumbHalfWidth = 0.5f * thumbImage.getWidth();
        thumbHalfHeight = 0.5f * thumbImage.getHeight();

        textSize = PixelUtil.dpToPx(context, DEFAULT_TEXT_SIZE_IN_DP);

        borderRect = new RectF();
        if (backgroundBitmap != null) {
            progressDrawableRect = new Rect(0, 0, backgroundBitmap.getWidth(), backgroundBitmap.getHeight());
        }
        // make RangeSeekBar focusable. This solves focus handling issues in case EditText widgets are being used along with the RangeSeekBar within ScrollViews.
        setFocusable(true);
        setFocusableInTouchMode(true);
        scaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        if (lockRange) {
            normalizedMaxValue = normalizedMinValue + timeInterval * 1d / (absoluteMaxValue - absoluteMinValue);
        }
        borderPaint.setStyle(Style.STROKE);
        borderPaint.setColor(Color.rgb(0x41, 0xc5, 0xf6));
        borderPaint.setStrokeWidth(PixelUtil.dpToPx(context, 1));
    }

    public void setRangeValues(int minValue, int maxValue) {
        this.absoluteMinValue = minValue;
        this.absoluteMaxValue = maxValue;
    }

    public void setTimeInterval(int timeInterval) {
        timeInterval = timeInterval < 0 ? 0 : timeInterval;
        this.timeInterval = timeInterval > absoluteMaxValue ? absoluteMaxValue : timeInterval;
        setNormalizedMaxValue(normalizedMinValue + timeInterval * 1f / absoluteMaxValue);
    }

    @SuppressWarnings("unchecked")
    // only used to set default values when initialised from XML without any values specified
    private void setRangeToDefaultValues() {
        this.absoluteMinValue = DEFAULT_MINIMUM;
        this.absoluteMaxValue = DEFAULT_MAXIMUM;
    }

    @SuppressWarnings("unused")
    public void resetSelectedValues() {
        setSelectedMinValue(absoluteMinValue);
        setSelectedMaxValue(absoluteMaxValue);
    }

    @SuppressWarnings("unused")
    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    /**
     * Should the widget notify the listener callback while the user is still dragging a thumb? Default is false.
     */
    @SuppressWarnings("unused")
    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    /**
     * Returns the absolute minimum value of the range that has been set at construction time.
     *
     * @return The absolute minimum value of the range.
     */
    public long getAbsoluteMinValue() {
        return absoluteMinValue;
    }

    /**
     * Returns the absolute maximum value of the range that has been set at construction time.
     *
     * @return The absolute maximum value of the range.
     */
    public long getAbsoluteMaxValue() {
        return absoluteMaxValue;
    }

    /**
     * Returns the currently selected min value.
     *
     * @return The currently selected min value.
     */
    public int getSelectedMinValue() {
        return normalizedToValue(normalizedMinValue);
    }

    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Sets the currently selected minimum value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the minimum value to. Will be clamped to given absolute minimum/maximum range.
     */
    public void setSelectedMinValue(int value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero when normalizing.
        if (0 == (absoluteMaxValue - absoluteMinValue)) {
            setNormalizedMinValue(0d);
        } else {
            setNormalizedMinValue(valueToNormalized(value));
        }
    }

    /**
     * Returns the currently selected max value.
     *
     * @return The currently selected max value.
     */
    public int getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValue);
    }

    /**
     * Sets the currently selected maximum value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the maximum value to. Will be clamped to given absolute minimum/maximum range.
     */
    public void setSelectedMaxValue(int value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero when normalizing.
        if (0 == (absoluteMaxValue - absoluteMinValue)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }

    /**
     * Registers given listener callback to notify about changed selected values.
     *
     * @param listener The listener to notify about changed selected values.
     */
    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Handles thumb selection and movement. Notifies listener callback on certain events.
     */
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        int pointerIndex;

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                activePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(activePointerId);
                downMotionX = event.getX(pointerIndex);

                pressedThumb = evalPressedThumb(downMotionX);
                if (pressedThumb == Thumb.MIN) {
                    temp = normalizedMinValue;
                } else {
                    temp = normalizedMaxValue;
                }

                // Only handle thumb presses.
                if (pressedThumb == null) {
                    return super.onTouchEvent(event);
                }

                setPressed(true);
                invalidate();
                onStartTrackingTouch();
//                trackTouchEvent(event);
                attemptClaimDrag();

                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {

                    if (isDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(activePointerId);
                        final float x = event.getX(pointerIndex);

                        if (Math.abs(x - downMotionX) > scaledTouchSlop) {
                            setPressed(true);
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }

                    if (notifyWhileDragging && listener != null) {
                        listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                pressedThumb = null;
                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue());
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                downMotionX = event.getX(index);
                activePointerId = event.getPointerId(index);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == activePointerId) {
            // This was our active pointer going up. Choose
            // a new active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            downMotionX = ev.getX(newPointerIndex);
            activePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private double temp;

    private void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(activePointerId);
        final float x = event.getX(pointerIndex);
        int width = getWidth();
        double delta;
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            delta = 0d;
        } else {
            double result = (x - downMotionX) / width;
            delta = Math.min(1d, Math.max(-1d, result));
        }
        double value = temp + delta;
        if (Thumb.MIN.equals(pressedThumb)) {
            setNormalizedMinValue(value);
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(value);
        }
    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        isDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is canceled.
     */
    void onStopTrackingTouch() {
        isDragging = false;
    }

    /**
     * Ensures correct size of the widget.
     */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }

        int height = 0;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = Math.max(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        if (backgroundBitmap != null) {
            height = Math.max(height, backgroundBitmap.getHeight() * 2);
        }
        if (foregroundBitmap != null) {
            height = Math.max(height, foregroundBitmap.getHeight() * 2);
        }
        setMeasuredDimension(width, height);
    }

    /**
     * Draws the widget on the given canvas.
     */
    @Override
    protected synchronized void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        paint.setTextSize(textSize);
        paint.setStyle(Style.FILL);
        paint.setColor(defaultColor);
        paint.setAntiAlias(true);

        padding = internalPad + thumbHalfWidth;

        borderRect.left = getWidth() / 2 - getWidth() * 8f / 25 - borderPaint.getStrokeWidth() / 2;
        borderRect.right = getWidth() / 2 + getWidth() * 8f / 25 - borderPaint.getStrokeWidth() / 2;
        if (foregroundBitmap != null) {
            progressDrawableRect.left = (int) (foregroundBitmap.getWidth() * normalizedMinValue);
            progressDrawableRect.right = (int) (foregroundBitmap.getWidth() * normalizedMaxValue);
            double targetRatio = 0.7 * getHeight() / (borderRect.right - borderRect.left);
            double clipWidth = (foregroundBitmap.getWidth() * (normalizedMaxValue - normalizedMinValue));
            double clipRatio = foregroundBitmap.getHeight() / clipWidth;
            if (clipRatio >= targetRatio) {
                double clipHeight = targetRatio * clipWidth;
                progressDrawableRect.top = (int) (foregroundBitmap.getHeight() / 2 - clipHeight / 2);
                progressDrawableRect.bottom = (int) (foregroundBitmap.getHeight() / 2 + clipHeight / 2);
                borderRect.top = (float) (0.15 * getHeight());
                borderRect.bottom = (float) (0.85 * getHeight());
            } else {
                double targetHeight = clipRatio * (getWidth() * 16f / 25);
                borderRect.top = (float) (getHeight() / 2 - targetHeight / 2);
                borderRect.bottom = (float) (getHeight() / 2 + targetHeight / 2);
                progressDrawableRect.top = 0;
                progressDrawableRect.bottom = foregroundBitmap.getHeight();
            }
            float scale = (float) ((borderRect.right - borderRect.left) / clipWidth);
            matrix.preScale(scale, scale);
            matrix.postTranslate(borderRect.left - scale * progressDrawableRect.left, -scale * backgroundBitmap.getHeight() / 2 + getHeight() / 2);
            canvas.drawBitmap(backgroundBitmap, matrix, null);
            matrix.reset();
            canvas.drawBitmap(foregroundBitmap, progressDrawableRect, borderRect, null);
        }

        borderRect.top = (float) (0.15 * getHeight() + borderPaint.getStrokeWidth() / 2);
        borderRect.bottom = (float) (0.85 * getHeight() - borderPaint.getStrokeWidth() / 2);
        canvas.drawRoundRect(borderRect, 10, 10, borderPaint);
    }

    /**
     * Overridden to save instance state when device orientation changes. This method is called automatically if you assign an id to the RangeSeekBar widget using the {@link #setId(int)} method. Other members of this class than the normalized min and max values don't need to be saved.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        return bundle;
    }

    /**
     * Overridden to restore instance state when device orientation changes. This method is called automatically if you assign an id to the RangeSeekBar widget using the {@link #setId(int)} method.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
    }

    /**
     * Decides which (if any) thumb is touched by the given x-coordinate.
     *
     * @param touchX The x-coordinate of a touch event in screen space.
     * @return The pressed thumb or null if none has been touched.
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = Thumb.MIN;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue);
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue);
        if (minThumbPressed && maxThumbPressed) {
            // if both thumbs are pressed (they lie on top of each other), choose the one with more room to drag. this avoids "stalling" the thumbs in a corner, not being able to drag them apart anymore.
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    /**
     * Decides if given x-coordinate in screen space needs to be interpreted as "within" the normalized thumb x-coordinate.
     *
     * @param touchX               The x-coordinate in screen space to check.
     * @param normalizedThumbValue The normalized x-coordinate of the thumb to check.
     * @return true if x-coordinate is in thumb range, false otherwise.
     */
    private boolean isInThumbRange(float touchX, double normalizedThumbValue) {
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth;
    }

    /**
     * Sets normalized min value to value so that 0 <= value <= normalized max value <= 1. The View will get invalidated when calling this method.
     *
     * @param value The new normalized min value to set.
     */
    private void setNormalizedMinValue(double value) {
        double normalizedInterval = timeInterval * 1d / (absoluteMaxValue - absoluteMinValue);
        if (lockRange) {
            normalizedMinValue = Math.max(0d, Math.min(1d - normalizedInterval, value));
            normalizedMaxValue = normalizedMinValue + normalizedInterval;
        } else {
            normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        }
        invalidate();
    }

    /**
     * Sets normalized max value to value so that 0 <= normalized min value <= value <= 1. The View will get invalidated when calling this method.
     *
     * @param value The new normalized max value to set.
     */
    private void setNormalizedMaxValue(double value) {
        double normalizedInterval = timeInterval * 1d / (absoluteMaxValue - absoluteMinValue);
        if (lockRange) {
            normalizedMaxValue = Math.max(normalizedInterval, Math.min(1d, value));
            normalizedMinValue = normalizedMaxValue - normalizedInterval;
        } else {
            normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        }
        invalidate();
    }

    /**
     * Converts a normalized value to a Number object in the value space between absolute minimum and maximum.
     */
    @SuppressWarnings("unchecked")
    protected int normalizedToValue(double normalized) {
        double v = absoluteMinValue + normalized * (absoluteMaxValue - absoluteMinValue);
        return (int) (Math.round(v * 100) / 100d);
    }

    /**
     * Converts the given Number value to a normalized double.
     *
     * @param value The Number value to normalize.
     * @return The normalized double.
     */
    protected double valueToNormalized(int value) {
        if (0 == absoluteMaxValue - absoluteMinValue) {
            // prevent division by zero, simply return 0.
            return 0d;
        }
        return (value - absoluteMinValue) * 1d / (absoluteMaxValue - absoluteMinValue);
    }

    /**
     * Converts a normalized value into screen space.
     *
     * @param normalizedCoord The normalized value to convert.
     * @return The converted value in screen space.
     */
    private float normalizedToScreen(double normalizedCoord) {
        return (float) (padding + normalizedCoord * (getWidth() - 2 * padding));
    }

    /**
     * Converts screen space x-coordinates into normalized values.
     *
     * @param screenCoord The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoord) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            double result = (screenCoord - padding) / (width - 2 * padding);
            return Math.min(1d, Math.max(0d, result));
        }
    }

    /**
     * Thumb constants (min and max).
     */
    private enum Thumb {
        MIN, MAX
    }

    /**
     * Callback listener interface to notify about changed range values.
     *
     * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
     */
    public interface OnRangeSeekBarChangeListener {

        void onRangeSeekBarValuesChanged(RangeSeekBar bar, int minValue, int maxValue);
    }

}
