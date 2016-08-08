package com.hyunnyapp.brainycopter.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.hyunnyapp.brainycopter.ui.gl.GLSprite;

import javax.microedition.khronos.opengles.GL10;

public class AttitudeIndicator extends Sprite {
	private static final String TAG = AttitudeIndicator.class.getSimpleName();

	private static final float INTERNAL_RADIUS = 0.85f;
	private static final float YAW_ARROW_SIZE = 1.2f;
	private static final float YAW_ARROW_ANGLE = 4.5f;
	private static final float PITCH_TICK_LINE_LENGTH = 0.4f;
	private static final int PITCH_RANGE = 45;
	private static final int PITCH_TICK_SPACING = 15;
	private static final int PITCH_TICK_PADDING = 2;
	private static final float PLANE_SIZE = 0.8f;
	private static final float PLANE_BODY_SIZE = 0.2f;
	private static final float PLANE_WING_WIDTH = 5f;

	private float width;
	private float height;
	private float halfWidth;
	private float halfHeight;
	private float radiusExternal;
	private float radiusInternal;
	private RectF internalBounds;

	private Paint yawPaint;
	private Paint skyPaint;
	private Paint groundPaint;
	private Paint planePaint;
	private Paint planeFinPaint;
	private Paint planeCenterPaint;

	private Path yawPath = new Path();
	private Path groundPath = new Path();

	private float yaw, roll, pitch;
	private Paint tickPaint;

	private Resources res;
	private GLSprite sprite;
	private boolean updateTexture;

	public AttitudeIndicator(Context context, float width, float height, Align align) {
		super(align);

		res = context.getResources();

		this.width = width;
		this.height = height;

		initializeAttitudeIndicator();

		Bitmap bitmap = createBitmapToRender();
		sprite = new GLSprite(res, bitmap);
		bitmap.recycle();
	}

	private void initializeAttitudeIndicator() {
		Paint fillPaint = new Paint();
		fillPaint.setAntiAlias(true);
		fillPaint.setStyle(Style.FILL);

		yawPaint = new Paint(fillPaint);
		yawPaint.setColor(Color.LTGRAY);

		skyPaint = new Paint(fillPaint);

		groundPaint = new Paint(fillPaint);

		planePaint = new Paint(fillPaint);
		planePaint.setColor(Color.WHITE);
		planePaint.setStrokeWidth(PLANE_WING_WIDTH);
		planePaint.setStrokeCap(Cap.ROUND);
		planeCenterPaint = new Paint(planePaint);
		planeCenterPaint.setColor(Color.RED);
		planeFinPaint = new Paint(planePaint);
		planeFinPaint.setStrokeWidth(PLANE_WING_WIDTH / 2f);

		tickPaint = new Paint(fillPaint);
		tickPaint.setColor(Color.parseColor("#44ffffff"));
		tickPaint.setStrokeWidth(2);

		halfHeight = height / 2f;
		halfWidth = width / 2f;

		radiusExternal = Math.min(halfHeight, halfWidth) / YAW_ARROW_SIZE;
		radiusInternal = radiusExternal * INTERNAL_RADIUS;

		internalBounds = new RectF(-radiusInternal, -radiusInternal, radiusInternal, radiusInternal);

		skyPaint.setShader(new LinearGradient(0, -radiusInternal, 0, radiusInternal, Color
				.parseColor("#0082d6"), Color.parseColor("#2cb1e1"), TileMode.CLAMP));

		groundPaint.setShader(new LinearGradient(0, radiusInternal, 0, radiusInternal, Color
				.parseColor("#4bbba1"), Color.parseColor("#008f63"), TileMode.CLAMP));

	}

	private Bitmap createBitmapToRender() {
		Bitmap bitmap = Bitmap.createBitmap(Math.round(width), Math.round(height), Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(0x00000000);
		bitmap.setDensity(res.getDisplayMetrics().densityDpi);
		Canvas canvas = new Canvas(bitmap);

		canvas.translate(halfWidth, halfHeight);
		drawYaw(canvas);
		drawSkyAndGround(canvas);
		drawPitchTicks(canvas);
		drawPlane(canvas);

		return bitmap;
	}

	private void drawYaw(Canvas canvas) {
		// Fill the background
		canvas.drawCircle(0, 0, radiusExternal, yawPaint);

		// Yaw Arrow
		float mathYaw = (float) Math.toRadians(180 - yaw);
		yawPath.reset();
		yawPath.moveTo(0, 0);
		radialLineTo(yawPath, mathYaw + YAW_ARROW_ANGLE, radiusExternal);
		radialLineTo(yawPath, mathYaw, radiusExternal * YAW_ARROW_SIZE);
		radialLineTo(yawPath, mathYaw - YAW_ARROW_ANGLE, radiusExternal);
		canvas.drawPath(yawPath, yawPaint);
	}

	private void radialLineTo(Path path, float angle, float radius) {
		path.lineTo((float) Math.sin(angle) * radius, (float) Math.cos(angle) * radius);
	}

	private void drawSkyAndGround(Canvas canvas) {
		// Fill with the sky
		canvas.drawCircle(0, 0, radiusInternal, skyPaint);

		// Overlay the ground
		groundPath.reset();
		float pitchProjection = (float) Math.toDegrees(Math.acos(pitch / PITCH_RANGE));
		groundPath.addArc(internalBounds, 90 - pitchProjection - roll, pitchProjection * 2);
		canvas.drawPath(groundPath, groundPaint);

	}

	private void drawPitchTicks(Canvas canvas) {
		float lineX = (float) (Math.cos(Math.toRadians(-roll)) * radiusInternal)
				* PITCH_TICK_LINE_LENGTH;
		float lineY = (float) (Math.sin(Math.toRadians(-roll)) * radiusInternal)
				* PITCH_TICK_LINE_LENGTH;
		float dx = (float) (Math.cos(Math.toRadians(-roll - 90)) * radiusInternal / PITCH_RANGE);
		float dy = (float) (Math.sin(Math.toRadians(-roll - 90)) * radiusInternal / PITCH_RANGE);
		int i = (int) ((-PITCH_RANGE + pitch + PITCH_TICK_PADDING) / PITCH_TICK_SPACING);
		int loopEnd = (int) ((PITCH_RANGE + pitch - PITCH_TICK_PADDING) / PITCH_TICK_SPACING);
		for (; i <= loopEnd; i++) {
			float degree = -pitch + PITCH_TICK_SPACING * i;
			canvas.drawLine(lineX + dx * degree, lineY + dy * degree, -lineX + dx * degree, -lineY
					+ dy * degree, tickPaint);
		}
	}

	private void drawPlane(Canvas canvas) {
		canvas.drawLine(radiusInternal * PLANE_SIZE, 0, -radiusInternal * PLANE_SIZE, 0, planePaint);
		canvas.drawLine(0, 0, 0, -radiusInternal * PLANE_SIZE * 5 / 12, planeFinPaint);
		canvas.drawCircle(0, 0, radiusInternal * PLANE_SIZE * PLANE_BODY_SIZE, planePaint);
		canvas.drawCircle(0, 0, radiusInternal * PLANE_SIZE * PLANE_BODY_SIZE / 2f,
				planeCenterPaint);
	}

	public void setAttitude(float roll, float pitch, float yaw) {
		this.roll = roll;
		this.pitch = pitch;
		this.yaw = yaw;
		Log.d(TAG, "setAttitude roll: "+this.roll+", pitch: "+this.pitch+", yaw: "+this.yaw);
	}

	@Override
	public void init(GL10 gl, int program) {
		sprite.init(gl, program);
	}

	@Override
	public void surfaceChanged(GL10 gl, int width, int height)
	{
		Log.d(TAG, "surfaceChanged width: " + width + ", height: " + height);
		super.surfaceChanged(gl, width, height);
	}

	@Override
	public void surfaceChanged(Canvas canvas)
	{
		super.surfaceChanged(canvas);
	}

	@Override
	public void draw(GL10 gl) {

		if (visible) {
			if (updateTexture) {
				Bitmap bitmap = createBitmapToRender();
				sprite.updateTexture(res, bitmap);
				Log.d(TAG, "surfaceWidth: " + surfaceWidth + ", surfaceHeight: " + surfaceHeight);
				layout(surfaceWidth, surfaceHeight);

				updateTexture = false;
				Log.d(TAG, "sprite.onDraw("+width+", "+height+")");
			}

			sprite.onDraw(gl, bounds.left, surfaceHeight - bounds.top - sprite.height);
		}
	}

	public void invalidate()
	{
		updateTexture = true;
	}

	@Override
	public void draw(Canvas canvas) {

	}

	@Override
	public boolean onTouchEvent(View v, MotionEvent event) {
		return false;
	}

	@Override
	public boolean isInitialized() {
		return sprite.isReadyToDraw();
	}

	@Override
	public void setNeedsUpdate() {

	}

	@Override
	public void setViewAndProjectionMatrices(float[] vMatrix, float[] projMatrix) {
		sprite.setViewAndProjectionMatrices(vMatrix, projMatrix);
	}

	@Override
	public int getWidth() {
		return sprite.width;
	}

	@Override
	public int getHeight() {
		return sprite.height;
	}

	@Override
	public void freeResources() {
		sprite.freeResources();
	}

}
