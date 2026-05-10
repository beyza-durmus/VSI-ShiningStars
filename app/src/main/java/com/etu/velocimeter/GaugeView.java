package com.etu.velocimeter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

// custom view that draws the speedometer gauge
// took me a while to figure out how to draw with canvas
public class GaugeView extends View {

    // maximum speed the gauge shows
    static final float MAX_SPEED = 3f;

    // speed where red zone starts (above this = alarm)
    static final float RED_ZONE_SPEED = 2f;

    // how fast the needle moves (lerp value)
    // lower = smoother but slower
    float lerpValue = 0.18f;

    // conversion factor for feet per meter
    float FT_PER_M = 3.28084f;

    // paint for background circle
    Paint backgroundPaint;

    // paint for the border/bevel
    Paint bevelPaint;

    // paint for green zone
    Paint greenPaint;

    // paint for yellow zone
    Paint yellowPaint;

    // paint for red zone
    Paint redPaint;

    // paint for major tick marks
    Paint tickPaint;

    // paint for minor tick marks (smaller ones)
    Paint minorTickPaint;

    // paint for the number labels on the gauge
    Paint labelPaint;

    // paint for "VERTICAL SPEED" title text
    Paint titlePaint;

    // paint for the speed number at the bottom
    Paint valuePaint;

    // paint for the up/down labels
    Paint upDownPaint;

    // paint for the needle
    Paint needlePaint;

    // paint for the center hub circle
    Paint hubPaint;

    // rectangle for drawing arcs
    RectF arcRect;

    // center x and y of the view
    float cx;
    float cy;
    float radius;

    // the speed we want to show
    float targetSpeed = 0f;

    // the speed currently displayed (for smooth animation)
    float displaySpeed = 0f;

    // true if showing feet/s, false for m/s
    boolean useFeet = false;

    // constructor 1 (used when creating in code)
    public GaugeView(Context context) {
        super(context);
        setupPaints();
    }

    // constructor 2 (used when inflating from xml)
    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaints();
    }

    // constructor 3 (not sure when this is used but android needs it)
    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupPaints();
    }

    // helper to convert dp to pixels (for different screen sizes)
    private float dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return dp * density;
    }

    // set up all the paint objects
    // a Paint object controls how things are drawn (color, stroke width etc)
    private void setupPaints() {
        // background gray circle
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(Color.parseColor("#5C5C5C"));
        backgroundPaint.setStyle(Paint.Style.FILL);

        // dark border around the gauge
        bevelPaint = new Paint();
        bevelPaint.setAntiAlias(true);
        bevelPaint.setColor(Color.parseColor("#1A1A1A"));
        bevelPaint.setStyle(Paint.Style.STROKE);

        // green arc (slow speed zone, 0-1 m/s)
        greenPaint = new Paint();
        greenPaint.setAntiAlias(true);
        greenPaint.setStyle(Paint.Style.STROKE);
        greenPaint.setStrokeWidth(dpToPx(15));
        greenPaint.setStrokeCap(Paint.Cap.BUTT);
        greenPaint.setColor(Color.parseColor("#2E7D32"));

        // yellow arc (medium speed zone, 1-2 m/s)
        yellowPaint = new Paint();
        yellowPaint.setAntiAlias(true);
        yellowPaint.setStyle(Paint.Style.STROKE);
        yellowPaint.setStrokeWidth(dpToPx(15));
        yellowPaint.setStrokeCap(Paint.Cap.BUTT);
        yellowPaint.setColor(Color.parseColor("#F57F17"));

        // red arc (danger zone, 2-3 m/s)
        redPaint = new Paint();
        redPaint.setAntiAlias(true);
        redPaint.setStyle(Paint.Style.STROKE);
        redPaint.setStrokeWidth(dpToPx(15));
        redPaint.setStrokeCap(Paint.Cap.BUTT);
        redPaint.setColor(Color.parseColor("#B71C1C"));

        // white tick marks (the big lines)
        tickPaint = new Paint();
        tickPaint.setAntiAlias(true);
        tickPaint.setColor(Color.WHITE);
        tickPaint.setStrokeWidth(dpToPx(2.5f));
        tickPaint.setStrokeCap(Paint.Cap.BUTT);

        // lighter gray minor tick marks (the smaller lines in between)
        minorTickPaint = new Paint();
        minorTickPaint.setAntiAlias(true);
        minorTickPaint.setColor(Color.parseColor("#CCCCCC"));
        minorTickPaint.setStrokeWidth(dpToPx(1.5f));
        minorTickPaint.setStrokeCap(Paint.Cap.BUTT);

        // labels for the numbers on the gauge
        labelPaint = new Paint();
        labelPaint.setAntiAlias(true);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setFakeBoldText(true);

        // small title text
        titlePaint = new Paint();
        titlePaint.setAntiAlias(true);
        titlePaint.setColor(Color.parseColor("#CCCCCC"));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        // needle is red
        needlePaint = new Paint();
        needlePaint.setAntiAlias(true);
        needlePaint.setColor(Color.parseColor("#EF5350"));
        needlePaint.setStrokeWidth(dpToPx(4.5f));
        needlePaint.setStrokeCap(Paint.Cap.ROUND);

        // center hub is dark
        hubPaint = new Paint();
        hubPaint.setAntiAlias(true);
        hubPaint.setColor(Color.parseColor("#111111"));
        hubPaint.setStyle(Paint.Style.FILL);

        // big speed number at bottom
        valuePaint = new Paint();
        valuePaint.setAntiAlias(true);
        valuePaint.setColor(Color.WHITE);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);

        // up/down labels
        upDownPaint = new Paint();
        upDownPaint.setAntiAlias(true);
        upDownPaint.setColor(Color.WHITE);
        upDownPaint.setTextAlign(Paint.Align.CENTER);
    }

    // this runs when the view size is known
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // calculate center point
        cx = w / 2f;
        cy = h / 2f;

        // radius with some padding
        radius = Math.min(cx, cy) - dpToPx(10);

        // the arc is slightly smaller than the full circle
        float arcRadius = radius - dpToPx(10);
        arcRect = new RectF(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius);

        // set the border width
        bevelPaint.setStrokeWidth(dpToPx(6));

        // scale text sizes based on how big the gauge is
        float scale = radius / dpToPx(130);
        labelPaint.setTextSize(dpToPx(12) * scale);
        titlePaint.setTextSize(dpToPx(9) * scale);
        valuePaint.setTextSize(dpToPx(22) * scale);
        upDownPaint.setTextSize(dpToPx(12) * scale);
    }

    // this is called every frame to draw everything
    @Override
    protected void onDraw(Canvas canvas) {
        // smooth animation using linear interpolation
        // slowly move displaySpeed toward targetSpeed
        displaySpeed = displaySpeed * (1 - lerpValue) + targetSpeed * lerpValue;

        // snap to target if close enough (avoid infinite tiny updates)
        if (Math.abs(displaySpeed - targetSpeed) < 0.003f) {
            displaySpeed = targetSpeed;
        }

        // step 1: draw the background circle
        canvas.drawCircle(cx, cy, radius, backgroundPaint);

        // step 2: draw the border
        canvas.drawCircle(cx, cy, radius, bevelPaint);

        // step 3: draw the colored speed zones
        drawColorZones(canvas);

        // step 4: draw tick marks and numbers
        drawTicks(canvas);

        // step 5: draw text in center
        drawCenterText(canvas);

        // step 6: draw the needle
        drawNeedle(canvas);

        // step 7: draw the center hub circle on top of needle
        canvas.drawCircle(cx, cy, dpToPx(9), hubPaint);

        // keep animating if not at target yet
        if (displaySpeed != targetSpeed) {
            postInvalidateOnAnimation();
        }
    }

    // draw the green, yellow, red arcs
    private void drawColorZones(Canvas canvas) {
        // UP direction arcs (top half) - goes from left to right starting at 180 degrees
        // green = 180 to 240 degrees
        canvas.drawArc(arcRect, 180, 60, false, greenPaint);
        // yellow = 240 to 300 degrees
        canvas.drawArc(arcRect, 240, 60, false, yellowPaint);
        // red = 300 to 360 degrees
        canvas.drawArc(arcRect, 300, 60, false, redPaint);

        // DOWN direction arcs (bottom half) - same but going other way
        // green = 180 to 120 degrees (negative direction)
        canvas.drawArc(arcRect, 180, -60, false, greenPaint);
        // yellow = 120 to 60 degrees
        canvas.drawArc(arcRect, 120, -60, false, yellowPaint);
        // red = 60 to 0 degrees
        canvas.drawArc(arcRect, 60, -60, false, redPaint);
    }

    // draw all the tick marks and number labels
    private void drawTicks(Canvas canvas) {
        float outerRadius = radius - dpToPx(3);
        float majorInnerRadius = radius - dpToPx(28);
        float minorInnerRadius = radius - dpToPx(17);
        float labelRadius = radius - dpToPx(44);

        // draw tick at 0 (straight left = stopped)
        drawOneTick(canvas, 180f, outerRadius, majorInnerRadius, tickPaint);

        // draw ticks and labels for 1 m/s on both sides
        int i = 1;
        float upAngle1 = 180f + i * 60f;   // 240 degrees
        float downAngle1 = 180f - i * 60f; // 120 degrees
        drawOneTick(canvas, upAngle1, outerRadius, majorInnerRadius, tickPaint);
        drawOneTick(canvas, downAngle1, outerRadius, majorInnerRadius, tickPaint);
        String label1 = getTickLabel(i);
        float ascent = labelPaint.getTextSize() * 0.35f;
        canvas.drawText(label1, cx + labelRadius * cosine(upAngle1), cy + labelRadius * sine(upAngle1) + ascent, labelPaint);
        canvas.drawText(label1, cx + labelRadius * cosine(downAngle1), cy + labelRadius * sine(downAngle1) + ascent, labelPaint);

        // draw ticks and labels for 2 m/s on both sides
        i = 2;
        float upAngle2 = 180f + i * 60f;   // 300 degrees
        float downAngle2 = 180f - i * 60f; // 60 degrees
        drawOneTick(canvas, upAngle2, outerRadius, majorInnerRadius, tickPaint);
        drawOneTick(canvas, downAngle2, outerRadius, majorInnerRadius, tickPaint);
        String label2 = getTickLabel(i);
        canvas.drawText(label2, cx + labelRadius * cosine(upAngle2), cy + labelRadius * sine(upAngle2) + ascent, labelPaint);
        canvas.drawText(label2, cx + labelRadius * cosine(downAngle2), cy + labelRadius * sine(downAngle2) + ascent, labelPaint);

        // draw ticks and labels for 3 m/s on both sides
        i = 3;
        float upAngle3 = 180f + i * 60f;   // 360 degrees
        float downAngle3 = 180f - i * 60f; // 0 degrees
        drawOneTick(canvas, upAngle3, outerRadius, majorInnerRadius, tickPaint);
        drawOneTick(canvas, downAngle3, outerRadius, majorInnerRadius, tickPaint);
        String label3 = getTickLabel(i);
        canvas.drawText(label3, cx + labelRadius * cosine(upAngle3), cy + labelRadius * sine(upAngle3) + ascent, labelPaint);
        canvas.drawText(label3, cx + labelRadius * cosine(downAngle3), cy + labelRadius * sine(downAngle3) + ascent, labelPaint);

        // draw minor ticks at 0.5, 1.5, 2.5 m/s (both up and down directions)
        drawOneTick(canvas, 180f + 0.5f * 60f, outerRadius, minorInnerRadius, minorTickPaint);
        drawOneTick(canvas, 180f - 0.5f * 60f, outerRadius, minorInnerRadius, minorTickPaint);

        drawOneTick(canvas, 180f + 1.5f * 60f, outerRadius, minorInnerRadius, minorTickPaint);
        drawOneTick(canvas, 180f - 1.5f * 60f, outerRadius, minorInnerRadius, minorTickPaint);

        drawOneTick(canvas, 180f + 2.5f * 60f, outerRadius, minorInnerRadius, minorTickPaint);
        drawOneTick(canvas, 180f - 2.5f * 60f, outerRadius, minorInnerRadius, minorTickPaint);
    }

    // helper method to draw a single tick mark line
    private void drawOneTick(Canvas canvas, float degrees, float outerR, float innerR, Paint paint) {
        float cosVal = cosine(degrees);
        float sinVal = sine(degrees);
        float x1 = cx + innerR * cosVal;
        float y1 = cy + innerR * sinVal;
        float x2 = cx + outerR * cosVal;
        float y2 = cy + outerR * sinVal;
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    // draw the text in the middle of the gauge
    private void drawCenterText(Canvas canvas) {
        // title at top
        canvas.drawText("VERTICAL SPEED", cx, cy - radius * 0.30f, titlePaint);

        // up and down labels
        float labelX = cx - radius * 0.30f;
        canvas.drawText("▲  UP", labelX, cy - dpToPx(14), upDownPaint);
        canvas.drawText("DOWN  ▼", labelX, cy + dpToPx(18), upDownPaint);

        // show the current speed as a number
        float absSpeed = Math.abs(displaySpeed);
        float displayValue;
        String unitText;

        if (useFeet == true) {
            displayValue = absSpeed * FT_PER_M;
            unitText = "ft/s";
        } else {
            displayValue = absSpeed;
            unitText = "m/s";
        }

        // format to 2 decimal places
        String speedText = String.format("%.2f %s", displayValue, unitText);
        canvas.drawText(speedText, cx, cy + radius * 0.32f, valuePaint);
    }

    // draw the red needle that points to current speed
    private void drawNeedle(Canvas canvas) {
        float angle = speedToAngle(displaySpeed);
        float cosVal = cosine(angle);
        float sinVal = sine(angle);

        float needleLength = radius - dpToPx(26);
        float tailLength = needleLength * 0.20f;

        // start point (the tail behind center)
        float x1 = cx - tailLength * cosVal;
        float y1 = cy - tailLength * sinVal;

        // end point (the tip)
        float x2 = cx + needleLength * cosVal;
        float y2 = cy + needleLength * sinVal;

        canvas.drawLine(x1, y1, x2, y2, needlePaint);
    }

    // convert speed value to angle in degrees for the needle
    // found this formula by trial and error
    private float speedToAngle(float speed) {
        // clamp speed to valid range first
        float clampedSpeed = speed;
        if (clampedSpeed > MAX_SPEED) {
            clampedSpeed = MAX_SPEED;
        }
        if (clampedSpeed < -MAX_SPEED) {
            clampedSpeed = -MAX_SPEED;
        }
        // map speed (-3 to 3) to angle (0 to 360)
        float angle = 180f + (clampedSpeed / MAX_SPEED) * 180f;
        return angle;
    }

    // get the label text for a given speed value
    private String getTickLabel(int metersPerSecond) {
        if (useFeet == false) {
            return String.valueOf(metersPerSecond);
        } else {
            float feetPerSecond = metersPerSecond * FT_PER_M;
            return String.format("%.0f", feetPerSecond);
        }
    }

    // helper - cosine in degrees (java math uses radians by default)
    private float cosine(float degrees) {
        double radians = Math.toRadians(degrees);
        return (float) Math.cos(radians);
    }

    // helper - sine in degrees
    private float sine(float degrees) {
        double radians = Math.toRadians(degrees);
        return (float) Math.sin(radians);
    }

    // called from MainActivity to update the speed
    public void setSpeed(float newSpeed) {
        targetSpeed = newSpeed;
        postInvalidateOnAnimation();
    }

    // called from MainActivity to switch between metric and imperial
    public void setUseFeet(boolean feet) {
        useFeet = feet;
        invalidate(); // redraw now
    }
}
