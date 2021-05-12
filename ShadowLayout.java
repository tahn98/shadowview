package vn.fpt.camera.ui.enterprise.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import vn.fpt.camera.R;

public class ShadowLayout extends RelativeLayout{
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap mBitmap = null;
    private Canvas mCanvas = new Canvas();
    private Rect mBounds = new Rect();

    private Boolean mInvalidateShadow = true;
    private Boolean mIsShowShadow = false;

    //shadow variables
    private int mShadowColor;
    private int mShadowAlpha;
    private float mShadowRadius;
    private float mShadowDistance;
    private float mShadowAngle;

    private static final float DEFAULT_SHADOW_RADIUS = 30.0F;
    private static final float DEFAULT_SHADOW_DISTANCE = 15.0F;
    private static final float DEFAULT_SHADOW_ANGLE = 45.0F;
    private static final int DEFAULT_SHADOW_COLOR = -12303292;
    private static final int MAX_ALPHA = 255;
    private static final float MAX_ANGLE = 360.0F;
    private static final float MIN_RADIUS = 0.1F;
    private static final float MIN_ANGLE = 0.0F;

    private float mShadowDx = 0f;
    private float mShadowDy = 0f;

    private int mDyDefault = dip2px(getContext(), 2f);

    public ShadowLayout(Context context) {
        super(context);
        init(context, null);
    }

    public ShadowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet){
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, mPaint);
        TypedArray attrs = context.obtainStyledAttributes(attributeSet, R.styleable.SddsShadowLayout);
        try {
            mIsShowShadow = attrs.getBoolean(R.styleable.SddsShadowLayout_sl_shadowed, true);
            mShadowRadius = attrs.getDimension(R.styleable.SddsShadowLayout_sl_shadowRadius, DEFAULT_SHADOW_RADIUS);
            mShadowDistance = attrs.getDimension(R.styleable.SddsShadowLayout_sl_shadowDistance, DEFAULT_SHADOW_DISTANCE);
            mShadowAngle = attrs.getFloat(R.styleable.SddsShadowLayout_sl_shadowAngle, DEFAULT_SHADOW_ANGLE);
            mShadowColor = attrs.getColor(R.styleable.SddsShadowLayout_sl_shadowColor, DEFAULT_SHADOW_COLOR);

            setIsShowShadow(mIsShowShadow);

            setShadowRadius(mShadowRadius);
            setShadowDistance(mShadowDistance);
            setShadowAngle(mShadowAngle);
            setShadowColor(mShadowColor);

        }finally {
            attrs.recycle();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setIsShowShadow(Boolean isShowShadow){
        this.mIsShowShadow = isShowShadow;
        postInvalidate();
    }

    public void setShadowRadius(float radius){
        this.mShadowRadius = Math.max(MIN_RADIUS, radius);
        if(isInEditMode()){
            mPaint.setMaskFilter(new BlurMaskFilter(mShadowRadius, BlurMaskFilter.Blur.NORMAL));
            resetShadow();
        }
    }

    public void setShadowDistance(Float distance){
        this.mShadowDistance = distance;
        resetShadow();
    }

    public void setShadowAngle(Float angle){
        this.mShadowAngle = Math.max(MIN_ANGLE, Math.min(angle, MAX_ANGLE));
        resetShadow();
    }

    public void setShadowColor(int color){
        this.mShadowColor = color;
        mShadowAlpha = Color.alpha(color);
        resetShadow();
    }

    private void resetShadow(){
        mShadowDy = (float) ((mShadowDistance * Math.sin(mShadowAngle / 180.0f * Math.PI))) + mDyDefault;

        // Set padding for shadow bitmap
        int padding = (int) (mShadowDistance + mShadowRadius);
        setPadding(padding, padding, padding, padding);
        requestLayout();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mIsShowShadow) {
            // If need to redraw shadow
            if (mInvalidateShadow) {
                // If bounds is zero
                if (mBounds.width() != 0 && mBounds.height() != 0) {
                    // Reset bitmap to bounds
                    mBitmap = Bitmap.createBitmap(mBounds.width(), mBounds.height(), Bitmap.Config.ARGB_8888);
                    // Canvas reset
                    mCanvas.setBitmap(mBitmap);

                    // We just redraw
                    mInvalidateShadow = false;
                    // Main feature of this lib. We create the local copy of all content, so now
                    // we can draw bitmap as a bottom layer of natural canvas.
                    // We draw shadow like blur effect on bitmap, cause of setShadowLayer() method of
                    // paint does`t draw shadow, it draw another copy of bitmap
                    super.dispatchDraw(mCanvas);

                    // Get the alpha bounds of bitmap
                    Bitmap extractedAlpha = mBitmap.extractAlpha();
                    // Clear past content content to draw shadow
                    mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

                    // Draw extracted alpha bounds of our local canvas
                    mPaint.setColor(adjustShadowAlpha(false));
                    mCanvas.drawBitmap(extractedAlpha, mShadowDx, mShadowDy, mPaint);

                    // Recycle and clear extracted alpha
                    extractedAlpha.recycle();
                } else {
                    // Create placeholder bitmap when size is zero and wait until new size coming up
                    mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
                }
            }

            // Reset alpha to draw child with full alpha
            mPaint.setColor(adjustShadowAlpha(true));
            // Draw shadow bitmap
            if(!mBitmap.isRecycled()){
                canvas.drawBitmap(mBitmap, 0.0f, 0.0f, mPaint);
            }
        }
        super.dispatchDraw(canvas);
    }

    private int adjustShadowAlpha(Boolean adjust){
        return Color.argb(
                adjust ? MAX_ALPHA : mShadowAlpha,
                Color.red(mShadowColor),
                Color.green(mShadowColor),
                Color.blue(mShadowColor)
        );
    }

    @Override
    public void requestLayout() {
        mInvalidateShadow = true;
        super.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mBounds.set(0, 0, this.getMeasuredWidth(), this.getMeasuredHeight());
    }

    int dip2px(Context context, Float dipValue) {
        if (context == null) return 0;
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}