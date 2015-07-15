/*
 * Copyright (c) 2015 Layer. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.layer.atlas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.layer.atlas.Atlas.Tools;

/**
 * @author Oleg Orlov
 * @since  15 Jun 2015
 */
public class AtlasImageView2 extends View {
    private static final String TAG = AtlasImageView2.class.getSimpleName();
    private static final boolean debug = false;
    
    public static final int ORIENTATION_NORMAL = 0;
    public static final int ORIENTATION_90_CW = 1;
    public static final int ORIENTATION_180 = 2;
    public static final int ORIENTATION_90_CCW = 3;
    
    private int defaultLayerType;
    
    private Drawable drawable;
    
    private int contentWidth;
    private int contentHeight;
    private float angle;
    
    private final Position pos = new Position();
    
    // TODO: 
    // - support contentDimensions: 0x0
    // - support contentDimensions + MeasureSpec.EXACT sizes 
    // - support boundaries + drawable instead of contentDimensions + drawable 
    
    //----------------------------------------------------------------------------
    public AtlasImageView2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupPaints();
    }

    public AtlasImageView2(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    public AtlasImageView2(Context context) {
        super(context);
        setupPaints();
    }
    
    protected void onMeasure(int widthSpec, int heightSpec) {
        int mWidthBefore  = getMeasuredWidth();
        int mHeightBefore = getMeasuredHeight();
        super.onMeasure(widthSpec, heightSpec);
        int mWidthAfter = getMeasuredWidth();
        int mHeightAfter = getMeasuredHeight();

        if (debug) Log.w(TAG, "onMeasure() before: " + mWidthBefore + "x" + mHeightBefore
                + ", spec: " + Tools.toStringSpec(widthSpec, heightSpec)
                + ", after: " + mWidthAfter + "x" + mHeightAfter
                + ", content: " + contentWidth + "x" + contentHeight + " h/w: " + (1.0f * contentHeight / contentWidth)
                );

        int widthMode = MeasureSpec.getMode(widthSpec);
        int heightMode = MeasureSpec.getMode(heightSpec);
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            if (debug) Log.w(TAG, "onMeasure() exact dimensions, skipping " + Tools.toStringSpec(widthSpec, heightSpec)); 
        } else if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            if (debug) Log.w(TAG, "onMeasure() first pass, skipping " + Tools.toStringSpec(widthSpec, heightSpec));
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            if (widthMode == MeasureSpec.EXACTLY) {
                setMeasuredDimension(w, (int)(1.0 * w * contentHeight / contentWidth));
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                if (contentWidth >= w) {
                    setMeasuredDimension(w, (int)(1.0 * w * contentHeight / contentWidth));
                } else {
                    setMeasuredDimension(contentWidth, contentHeight);
                }
            }
        } else {
            if (debug) Log.w(TAG, "onMeasure() unchanged. " + Tools.toStringSpec(widthSpec, heightSpec));
        }
                
        if (debug) Log.w(TAG, "onMeasure() final: " + getMeasuredWidth() + "x" + getMeasuredHeight());
    }
    
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (debug) Log.d(TAG, "onLayout() changed: " + changed+ " left: " + left+ " top: " + top+ " right: " + right+ " bottom: " + bottom);
    }
    
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (debug) Log.w(TAG, "onSizeChanged() w: " + w + " h: " + h+ " oldw: " + oldw+ " oldh: " + oldh);
    }

    private void setupPaints() {
        this.defaultLayerType = getLayerType();
        debugTextPaint.setTextSize(Tools.getPxFromDp(10, getContext()));
    }
    
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        
        if (getWidth() != getMeasuredWidth() || getHeight() != getMeasuredHeight()) {
            if (debug) Log.w(TAG, "onDraw() actual: " + getWidth() + "x" + getHeight()
                    + ", measured: " + getMeasuredWidth() + "x" + getMeasuredHeight());
        }
        
        if (drawable == null) return;
        
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bmpDrw = (BitmapDrawable) drawable;
            if (bmpDrw.getBitmap() != null) {
                if (debug) Log.w(TAG, "onDraw() bitmap: " + bmpDrw.getBitmap().getWidth() + "x" + bmpDrw.getBitmap().getHeight());
            } else {
                if (debug) Log.w(TAG, "onDraw() bitmap: null");
            }
        }
        
        if (debug) Log.w(TAG, 
                    "onDraw() bounds: " + drawable.getBounds() + ", content: " + contentWidth + "x" + contentHeight + ", angle: " + angle 
                    +         ", min: " + drawable.getMinimumWidth() + "x" + drawable.getMinimumHeight()
                    +  ", instrinsic: " + drawable.getIntrinsicWidth() + "x" + drawable.getIntrinsicHeight());
        
        int viewWidth  = getWidth();
        int viewHeight = getHeight();
        int imgWidth  = contentWidth;
        int imgHeight = contentHeight;

        if (contentWidth == 0 && contentHeight == 0) {
            imgWidth = drawable.getIntrinsicWidth();
            imgHeight = drawable.getIntrinsicHeight();
        }
        
        // fit in width
        if (imgWidth > viewWidth) {
            int newHeight = (int) (1.0 * imgHeight * viewWidth / imgWidth);
            int newWidth  = viewWidth;
            if (debug) Log.w(TAG, "onDraw() fit in width:  " + imgWidth + "x" + imgHeight + " -> " + newWidth + "x" + newHeight);
            imgWidth = newWidth;
            imgHeight = newHeight;
        }
        if (imgHeight > viewHeight) {
            int newWidth = (int) (1.0 * imgWidth * viewHeight / imgHeight);
            int newHeight = viewHeight;
            if (debug) Log.w(TAG, "onDraw() fit in height: " + imgWidth + "x" + imgHeight + " -> " + newWidth + "x" + newHeight);
            imgWidth = newWidth;
            imgHeight = newHeight;
        }
        
        float zoomedWidth =  (int) (imgWidth * pos.zoom);
        float zoomedHeight = (int) (imgHeight * pos.zoom);
        int left = (int) ((viewWidth  - zoomedWidth) / 2);
        int top  = (int) ((viewHeight - zoomedHeight) / 2);
        int right = (int) (left + zoomedWidth);
        int bottom = (int) (top + zoomedHeight);
        if (debug) Log.w(TAG, "onDraw() left: " + left + ", top: " + top + ", right: " + right + ", bottom: " + bottom);
        drawable.setBounds(left, top, right, bottom);

        if (!useBitmapBuffer && buffer != null) {
            buffer = null;
            bufferCanvas = null;
        }
        if (useBitmapBuffer && (buffer == null || buffer.getWidth() != viewWidth || buffer.getHeight() != viewHeight)) {
            buffer = Bitmap.createBitmap(viewWidth, viewHeight, Config.ARGB_8888);
            bufferCanvas = new Canvas(buffer);
        }
        
        Canvas workCanvas = useBitmapBuffer ? bufferCanvas : canvas;
        if (useBitmapBuffer) {          
            workCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);            // clean before using
        }
        
        if (debug) Log.w(TAG, "onDraw() useBitmapBuffer: " + useBitmapBuffer + ", buffer: " + (buffer == null ? "null" : buffer.getWidth() + "x" + buffer.getHeight()) );
        int saved = workCanvas.save();
        workCanvas.translate(pos.x, pos.y);
        workCanvas.rotate(angle, 0.5f * viewWidth , 0.5f * viewHeight);
        drawable.draw(workCanvas);
        Tools.drawX(drawable.getBounds(), debugGreenPaint, workCanvas);
        workCanvas.restoreToCount(saved);
        if (useBitmapBuffer) {
            canvas.drawBitmap(buffer, 0, 0, bitmapPaint);
        }
        
        boolean debug = true;
        if (debug) {
            Tools.drawPlus(0, 0, getWidth(), getHeight(), debugGrayPaint, canvas);
        }
        
        if (debug && showMarker) {
            canvas.drawLine(0, lastTouch1y, getWidth(), lastTouch1y, debugRedPaint);
            canvas.drawLine(lastTouch1x, 0, lastTouch1x, getHeight(), debugRedPaint);
            canvas.drawLine(0, lastTouch2y, getWidth(), lastTouch2y, debugBluePaint);
            canvas.drawLine(lastTouch2x, 0, lastTouch2x, getHeight(), debugBluePaint);
        }
        if (debug) {
            float lineHeight = debugTextPaint.getFontMetrics().descent - debugTextPaint.getFontMetrics().ascent;

            float x = Tools.getPxFromDp(20, getContext());
            float y = getHeight() - lineHeight * 5;
            
            canvas.drawText(String.format("1: %.1fx%.1f", lastTouch1x, lastTouch1y), x, y, debugTextPaint); y += lineHeight;
            canvas.drawText(String.format("2: %.1fx%.1f", lastTouch2x, lastTouch2y), x, y, debugTextPaint); y += lineHeight;
            canvas.drawText(pos.toString(), x, y, debugTextPaint); y += lineHeight;
        }
        if (debug) {
            Tools.drawPlusCircle(0.5f * (lastTouch1x + lastTouch2x), 0.5f * (lastTouch1y + lastTouch2y), 10, debugRedPaint, canvas);
            Tools.drawPlusCircle(0.5f * (zoomTouch1x + zoomTouch2x), 0.5f * (zoomTouch1y + zoomTouch2y), 10, debugBluePaint, canvas);
        }
        
    }
    
    private final static Paint debugRedPaint   = new Paint();
    private final static Paint debugGreenPaint  = new Paint();
    private final static Paint debugBluePaint  = new Paint();
    private final static Paint debugGrayPaint  = new Paint();
    private final static TextPaint debugTextPaint = new TextPaint();

    static {
        debugRedPaint.setStyle(Paint.Style.STROKE);
        debugRedPaint.setPathEffect(new DashPathEffect(new float[]{10, 20f}, 0));
        debugRedPaint.setColor(Color.rgb(200, 0, 0));
        debugGreenPaint.setStyle(Paint.Style.STROKE);
        debugGreenPaint.setColor(Color.rgb(0, 200, 0));
        debugBluePaint.setStyle(Paint.Style.STROKE);
        debugBluePaint.setColor(Color.rgb(0, 0, 200));
        debugGrayPaint.setStyle(Paint.Style.STROKE);
        debugGrayPaint.setColor(Color.rgb(200, 200, 200));
        debugTextPaint.setColor(Color.RED);
    }
    
    private float lastTouch1x, lastTouch2x;
    private float lastTouch1y, lastTouch2y;
    
    private boolean showMarker;
    
    private float dragTouch1x, dragTouch1y;
    private Position dragStart;
    
    private float zoomTouch1x, zoomTouch1y, zoomTouch2x, zoomTouch2y;
    private Position zoomStart;
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean debug = true;
        
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE : break;
            default : if (debug) Log.w(TAG, "onTouch() event: " + Tools.toString(event));
        }
        
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP              : { showMarker = false; break;  }
            case MotionEvent.ACTION_DOWN            : {
                showMarker = true; 
                dragTouch1x = event.getX(0);
                dragTouch1y = event.getY(0);
                dragStart = pos.copy();
                break;
            }
            
            case MotionEvent.ACTION_POINTER_UP: { 
                final int releasedPointer = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                dragTouch1x = event.getX(releasedPointer == 0 ? 1 : 0);
                dragTouch1y = event.getY(releasedPointer == 0 ? 1 : 0);
                dragStart = pos.copy();
                break;
            }
            
            case MotionEvent.ACTION_POINTER_DOWN    : {
                if (event.getPointerCount() == 2) {
                    zoomTouch1x = event.getX(0);
                    zoomTouch1y = event.getY(0);
                    zoomTouch2x = event.getX(1);
                    zoomTouch2y = event.getY(1);
                    zoomStart = new Position(pos);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE            : {
                if (event.getPointerCount() < 2) {
                    // drag
                    pos.x = dragStart.x + event.getX() - dragTouch1x;
                    pos.y = dragStart.y + event.getY() - dragTouch1y;
                } else { 
                    // calculate zoom
                    
                    double distanceXbefore = zoomTouch2x - zoomTouch1x;
                    double distanceYbefore = zoomTouch2y - zoomTouch1y;
                    double distanceBefore = Math.sqrt(distanceXbefore * distanceXbefore + distanceYbefore * distanceYbefore);

                    double distanceXNow = event.getX(1) - event.getX(0);
                    double distanceYNow = event.getY(1) - event.getY(0);
                    double distanceAfter  = Math.sqrt(distanceXNow    * distanceXNow    + distanceYNow    * distanceYNow);
                    
                    double newZoom = 1.0 * zoomStart.zoom * distanceAfter / distanceBefore;
                    pos.zoom = (float) newZoom;
                    
                    double centerXbefore = 0.5 * (zoomTouch1x + zoomTouch2x);
                    double centerYbefore = 0.5 * (zoomTouch1y + zoomTouch2y);
                    double centerXafter  = 0.5 * (event.getX(0) + event.getX(1));
                    double centerYafter  = 0.5 * (event.getY(0) + event.getY(1));
                    
                    double centerXdrBefore = getContentX(centerXbefore, zoomStart);
                    double centerYdrBefore = getContentY(centerYbefore, zoomStart);
                    
                    pos.x = (float) (centerXafter - centerXdrBefore * newZoom - 0.5f * getWidth());
                    pos.y = (float) (centerYafter - centerYdrBefore * newZoom - 0.5f * getHeight());
                    
                }
                break;
            }
        }
        trackLastTouch(event);
        
        invalidate();
        return true;
    }
    
    private double getContentX(double viewX, Position pos) {
        return ( viewX - pos.x - 0.5 * getWidth()) / pos.zoom;
    }
    private double getContentY(double viewY, Position pos) {
        return ( viewY - pos.y - 0.5 * getHeight() ) / pos.zoom;
    }
    
    private double getViewX(double contentX, Position pos) {
        return contentX * pos.zoom + 0.5 * getWidth() + pos.x;
    }
    private double getViewY(double contentY, Position pos) {
        return contentY * pos.zoom + 0.5 * getHeight() + pos.y;
    }

    private void trackLastTouch(MotionEvent event) {
        lastTouch1x = event.getX(0);
        lastTouch1y = event.getY(0);
        if (event.getPointerCount() > 1) {
            lastTouch2x = event.getX(1);
            lastTouch2y = event.getY(1);
        }
        if (debug) Log.w(TAG, "trackLastTouch() 0: " + lastTouch1x + "x" + lastTouch1y + ", 1: " + lastTouch2x + "x" + lastTouch2y);
    }
    
    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (who == this.drawable) return true;
        return super.verifyDrawable(who);
    }

    public void setBitmap(Bitmap bmp) {
        setDrawable(new BitmapDrawable(bmp));
    }
    
    public void setDrawable(Drawable drawable) {
        if (this.drawable != null) {
            this.drawable.setCallback(null);
        }
        this.drawable = drawable;
        if (drawable != null) {
            this.drawable.setCallback(this);
        }
        if (drawable instanceof GIFDrawable) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        } else {
            setLayerType(defaultLayerType, null);
        }
        invalidate();
    }
    
    public void setContentDimensions(int contentWidth, int contentHeight) {
        boolean requestLayout = false;
        if (this.contentWidth != contentWidth || this.contentHeight != contentHeight) {
            requestLayout = true;
        }
        if (debug) Log.w(TAG, "setContentDimensions() new: " + contentWidth + "x" + contentHeight + ", old: " + this.contentWidth + "x" + this.contentHeight);
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        
        if (requestLayout) {
            requestLayout();
        }
        invalidate();
    }
    
    boolean useBitmapBuffer;
    private Bitmap buffer;
    private Canvas bufferCanvas;
    private static final Paint bitmapPaint = new Paint();

    /** 
     * Big bitmaps may not fit into GL_MAX_TEXTURE_SIZE boundaries (2048x2048 for Nexus S).
     * To draw such images, buffer bitmap needs to be created
     * TODO: understand it automatically
     */
    public void setUseBitmapBuffer(boolean useBitmapBuffer) {
        this.useBitmapBuffer = useBitmapBuffer;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
        invalidate();
    }

    public float getZoom() {
        return pos.zoom;
    }

    /** zoom value for drawable. Default value is 1.0f when 1 contentPixel is equal to 1 viewPixel */
    public void setZoom(float zoomValue) {
        pos.zoom = zoomValue;
        invalidate();
    }
    
    public float getXOffset() {
        return pos.x;
    }
    
    /** horizontal difference between center of view and center of drawable. Default is 0. */
    public void setXOffset(float xOffset) {
        pos.x = xOffset;
        invalidate();
    }
    
    public float getYOffset() {
        return pos.y;
    }
    /** vertical difference between center of view and center of drawable. Default is 0. */
    public void setYOffset(float yOffset) {
        pos.y = yOffset;
        invalidate();
    }
    
    private static class Position {
        
        private float zoom = 1.0f;
        private float x    = 0.0f;
        private float y    = 0.0f;
        
        public Position() {}
        
        public Position(Position src) {
            this.zoom = src.zoom;
            this.x    = src.x;
            this.y    = src.y;
        }

        public Position(float zoom, float x, float y) {
            this.zoom = zoom;
            this.x = x;
            this.y = y;
        }
        
        public Position copy() {
            return new Position(zoom, x, y);
        }
        
        @Override
        public String toString() {
            return String.format("Zoom: %.1f at: %.1fx%.1f", zoom, x, y);
        }
        
    }
    
}