/*
 * This file is part of the Drawchemy project - https://code.google.com/p/drawchemy/
 *
 * Copyright (c) 2014 Pilmeyer Patrick
 *
 * Drawchemy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Drawchemy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Drawchemy.  If not, see <http://www.gnu.org/licenses/>.
 */

package draw.chemy;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import draw.chemy.creator.ACreator;
import draw.chemy.creator.IDrawingOperation;

public class DrawManager implements View.OnTouchListener {

    // Number of Operations possibles which can be cancelled
    private static final int MAX_OP = 5;
    LinkedList<IDrawingOperation> fOperations;
    LinkedList<IDrawingOperation> fUndo;

    Map<Integer, ACreator> fCreators;
    private ACreator fCurrentCreator;

    private boolean fColorSwitch;
    private float fColorVariation;
    private DrawListener fDrawListener;
    private Matrix fInputMatrix = new Matrix();

    public enum MIRROR {
        None,
        Horizontal,
        Vertical,
        Both
    }

    private final Matrix fMirrorHorizontal;
    private final Matrix fMirrorVertical;
    private final Matrix fMirrorHAndV;

    private final Paint fDitherPaint = new Paint();

    private final Bitmap fBackgroundImage;

    private final Canvas fBackgroundCanvas;
    //Default
    private int fMainColor = Color.BLACK;
    private int fSubColor = Color.WHITE;

    private Paint.Style fStyle = Paint.Style.STROKE;
    private float fStrokeWeight = 1.5f;

    private MIRROR fMirrorState = MIRROR.None;
    private boolean fGradientActive = false;

    private boolean fNewColorUsageFlag = false;

    private NewColorUsedListener fNewColorUsedListener;

    public DrawManager(int aWidth, int aHeight) {

        fBackgroundImage = Bitmap.createBitmap(aWidth, aHeight, Bitmap.Config.ARGB_8888);

        fBackgroundCanvas = new Canvas(fBackgroundImage);
        fBackgroundCanvas.drawColor(fSubColor);


        fOperations = new LinkedList<IDrawingOperation>();
        fUndo = new LinkedList<IDrawingOperation>();

        fCreators = new HashMap<Integer, ACreator>();
        fCurrentCreator = null;

        fDitherPaint.setDither(true);

        fMirrorHorizontal = new Matrix();
        fMirrorHorizontal.setScale(1, -1, 0, 0);
        fMirrorHorizontal.postTranslate(0, aHeight);

        fMirrorVertical = new Matrix();
        fMirrorVertical.setScale(-1, 1, 0, 0);
        fMirrorVertical.postTranslate(aWidth, 0);

        fMirrorHAndV = new Matrix();
        fMirrorHAndV.setScale(-1, -1, 0, 0);
        fMirrorHAndV.postTranslate(aWidth, aHeight);

    }

    public void setNewColorUsedListener(NewColorUsedListener aListener) {
        fNewColorUsedListener = aListener;
    }

    public boolean getMirrorHorizontal() {
        return fMirrorState == MIRROR.Horizontal || fMirrorState == MIRROR.Both;
    }

    public boolean getMirrorVertical() {
        return fMirrorState == MIRROR.Vertical || fMirrorState == MIRROR.Both;
    }

    public void setMirrorVertical(boolean checked) {
        switch (fMirrorState) {
            case None: {
                fMirrorState = checked ? MIRROR.Vertical : MIRROR.None;
                break;
            }
            case Horizontal: {
                fMirrorState = checked ? MIRROR.Both : MIRROR.Horizontal;
                break;
            }
            case Vertical: {
                fMirrorState = checked ? MIRROR.Vertical : MIRROR.None;
                break;
            }
            case Both: {
                fMirrorState = checked ? MIRROR.Both : MIRROR.Horizontal;
                break;
            }

        }
    }

    public void setMirrorHorizontal(boolean checked) {
        switch (fMirrorState) {
            case None: {
                fMirrorState = checked ? MIRROR.Horizontal : MIRROR.None;
                break;
            }
            case Horizontal: {
                fMirrorState = checked ? MIRROR.Horizontal : MIRROR.None;
                break;
            }
            case Vertical: {
                fMirrorState = checked ? MIRROR.Both : MIRROR.Vertical;
                break;
            }
            case Both: {
                fMirrorState = checked ? MIRROR.Both : MIRROR.Vertical;
                break;
            }

        }
    }

    public void clear() {
        synchronized (fBackgroundCanvas) {
            fBackgroundCanvas.drawColor(fSubColor | 0xff000000);
            fOperations.clear();
            fUndo.clear();
        }
        redraw();
    }

    public void undo() {
        if (fOperations.size() != 0) {
            fUndo.addFirst(fOperations.removeLast());
            redraw();
        }
    }

    public void redo() {
        if (fUndo.size() != 0) {
            fOperations.addLast(fUndo.removeFirst());
            redraw();
        }
    }

    public void setColorVariation(float aColorVariation) {
        fColorVariation = aColorVariation;
    }

    @SuppressWarnings("all")
    public float getColorVariation() {
        return fColorVariation;
    }

    public void setInputMatrix(Matrix aInputMatrix) {
        fInputMatrix = aInputMatrix;
    }

    public void setDrawListener(DrawListener aDrawListener) {
        fDrawListener = aDrawListener;
    }

    @SuppressWarnings("all")
    public DrawListener getDrawListener() {
        return fDrawListener;
    }


    public void addTool(int aKey, ACreator aDrawingTool) {
        fCreators.put(aKey, aDrawingTool);
    }

    public void setCurrentTool(int aKey) {
        if (fCreators.containsKey(aKey)) {
            fCurrentCreator = fCreators.get(aKey);
        }
    }

    public int getMainColor() {
        return fMainColor;
    }

    public int getSubColor() {
        return fSubColor;
    }

    public void setMainColor(int aColor) {
        fMainColor = aColor;
        fNewColorUsageFlag = true;
    }

    public ACreator getCurrentCreator() {
        return fCurrentCreator;
    }

    @SuppressWarnings("all")
    public void setSubColor(int aColor) {
        fSubColor = aColor;
    }

    public void switchColor() {
        int tmp = fMainColor;
        fMainColor = fSubColor;
        fSubColor = tmp;
        fNewColorUsageFlag = true;
    }


    public Paint.Style getStyle() {
        return fStyle;
    }

    public void setStyle(Paint.Style fStyle) {
        this.fStyle = fStyle;
    }

    @SuppressWarnings("all")
    public boolean isColorSwitchActivate() {
        return fColorSwitch;
    }

    public void setColorSwitchFlag(boolean checked) {
        fColorSwitch = checked;
    }

    public void setStrokeWeight(float aStrokeWeigth) {
        fStrokeWeight = aStrokeWeigth;
    }

    @SuppressWarnings("all")
    public float getStrokeWeight() {
        return fStrokeWeight;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();

        float points[] = new float[]{motionEvent.getX(), motionEvent.getY()};
        fInputMatrix.mapPoints(points);
        if (fCurrentCreator != null) {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    setPictureOp();
                    addOperation(fCurrentCreator.startDrawingOperation(points[0], points[1]));
                    if(fNewColorUsageFlag) {
                        fNewColorUsageFlag = false;
                        newColorUsed();
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    fCurrentCreator.updateDrawingOperation(points[0], points[1]);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    fCurrentCreator.endDrawingOperation();
                    break;
                }
                default:
                    return false;

            }
        }
        return true;
    }

    private void newColorUsed() {
        if(fNewColorUsedListener != null) {
            fNewColorUsedListener.newColorUsed(fMainColor);
        }
    }


    private void setPictureOp() {
        if (fUndo.size() > 0) {
            fUndo.addLast(new PictureOp(fUndo.removeLast()));
        }
    }

    public void redraw() {
        fDrawListener.redraw();
    }

    public void addOperation(IDrawingOperation op) {
        fUndo.clear();
        if (fGradientActive) {
            op = new GradientOp(op, getColor(fMainColor), getColor(fSubColor));
        }

        if (fMirrorState != MIRROR.None) {
            op = new MirrorOp(op, fMirrorState);
        }
        synchronized (fOperations) {
            fOperations.add(op);
        }
    }

    public boolean isGradientActive() {
        return fGradientActive;
    }

    public void setGradientActive(boolean fGradientActive) {
        this.fGradientActive = fGradientActive;
    }

    public void draw(Canvas aCanvas) {
        synchronized (fBackgroundCanvas) {
            while (fOperations.size() > MAX_OP) {
                IDrawingOperation op = fOperations.removeFirst();
                op.draw(fBackgroundCanvas);
            }
            aCanvas.drawBitmap(fBackgroundImage, 0.f, 0.f, fDitherPaint);
        }
        synchronized (fOperations) {
            for (IDrawingOperation op : fOperations) {
                op.draw(aCanvas);
            }
        }

    }

    public int getWidth() {
        return fBackgroundImage.getWidth();
    }

    public int getHeight() {
        return fBackgroundImage.getHeight();
    }

    public Bitmap getBitmap() {
        Bitmap result = Bitmap.createBitmap(fBackgroundImage.getWidth(), fBackgroundImage.getHeight(), Bitmap.Config.ARGB_8888);
        draw(new Canvas(result));
        return result;
    }

    public Paint getPaint() {
        Paint p = new Paint();
        p.setColor(getColor(fMainColor));
        p.setStrokeWidth(fStrokeWeight);
        p.setStyle(fStyle);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setAntiAlias(true);
        p.setPathEffect(new CornerPathEffect(7.f));
        return p;
    }

    private int getColor(int aColor) {
        if (fColorSwitch) {
            float hsv[] = new float[3];
            Color.colorToHSV(aColor, hsv);
            hsv[0] += DrawUtils.getProbability(90.f * fColorVariation);
            if (hsv[0] < 0.f) {
                hsv[0] += 360.f;
            } else if (hsv[0] > 360.f) {
                hsv[0] -= 360.f;
            }
            return Color.HSVToColor(Color.alpha(aColor), hsv);
        } else {
            return aColor;
        }
    }

    public void putBitmapAsBackground(Bitmap aBitmap) {

        float width = getWidth();
        float height = getHeight();

        float bitmapWidth = aBitmap.getWidth();
        float bitmapHeight = aBitmap.getHeight();

        Matrix matrix = new Matrix();
        float dx, dy, scale;

        if(width > height) {
            // canvas is in on paysage mode

            if(bitmapHeight > bitmapWidth) {
                // bitmap is in on portrait mode
                matrix.setRotate(-90);
                matrix.postTranslate(0, bitmapWidth);

                float temp = bitmapWidth;
                bitmapWidth = bitmapHeight;
                bitmapHeight = temp;
            }

            scale = width / bitmapWidth;
            if(scale*bitmapHeight > height) {
                scale = height / bitmapHeight;
                dx = (width - scale*bitmapWidth)/2.f;
                dy = 0.f;
            } else {
                dx = 0;
                dy = (height - scale*bitmapHeight)/2.f;
            }
        } else {
            // canvas is in on portrait mode
            if(bitmapWidth > bitmapHeight) {
                // bitmap is in on paysage mode
                matrix.setRotate(90);
                matrix.postTranslate(bitmapHeight, 0);

                float temp = bitmapWidth;
                bitmapWidth = bitmapHeight;
                bitmapHeight = temp;
            }

            scale = height / bitmapHeight;
            if(scale*bitmapWidth > bitmapWidth) {
                scale = width / bitmapWidth;
                dx = 0;
                dy = (height - scale*bitmapHeight)/2.f;
            } else {
                dx = (width - scale*bitmapWidth)/2.f;
                dy = 0.f;
            }
        }
        matrix.postScale(scale,scale);
        matrix.postTranslate(dx,dy);

        synchronized (fBackgroundCanvas) {
            fBackgroundCanvas.drawColor(fSubColor | 0xff000000);
            fOperations.clear();
            fUndo.clear();
            fBackgroundCanvas.drawBitmap(aBitmap, matrix, null);
        }
        redraw();
    }


    private class MirrorOp implements IDrawingOperation {

        private final IDrawingOperation fDelegate;
        private final MIRROR fMirrorState;

        public MirrorOp(IDrawingOperation aDelegate, MIRROR aMirrorState) {
            fDelegate = aDelegate;
            fMirrorState = aMirrorState;
        }

        @Override
        public void draw(Canvas aCanvas) {
            switch (fMirrorState) {
                case Horizontal: {
                    fDelegate.draw(aCanvas);
                    aCanvas.save();
                    aCanvas.concat(fMirrorHorizontal);
                    fDelegate.draw(aCanvas);
                    aCanvas.restore();
                    break;
                }
                case Vertical: {
                    fDelegate.draw(aCanvas);
                    aCanvas.save();
                    aCanvas.concat(fMirrorVertical);
                    fDelegate.draw(aCanvas);
                    aCanvas.restore();
                    break;
                }
                case Both: {
                    fDelegate.draw(aCanvas);

                    aCanvas.save();
                    aCanvas.concat(fMirrorVertical);
                    fDelegate.draw(aCanvas);
                    aCanvas.restore();

                    aCanvas.save();
                    aCanvas.concat(fMirrorHorizontal);
                    fDelegate.draw(aCanvas);
                    aCanvas.restore();

                    aCanvas.save();
                    aCanvas.concat(fMirrorHAndV);
                    fDelegate.draw(aCanvas);
                    aCanvas.restore();
                    break;
                }

            }
        }

        @Override
        public Paint getPaint() {
            return fDelegate.getPaint();
        }

        @Override
        public void computeBounds(RectF aBoundSFCT) {
            fDelegate.computeBounds(aBoundSFCT);
        }
    }

    private class GradientOp implements IDrawingOperation {

        private final int fMainColor;
        private final int fSubColor;
        private final IDrawingOperation fDelegate;
        private final RectF fBounds;

        public GradientOp(IDrawingOperation aDelegate, int aMainColor, int aSubColor) {
            fDelegate = aDelegate;
            fMainColor = aMainColor;
            fSubColor = aSubColor;
            fBounds = new RectF();
        }

        @Override
        public void draw(Canvas aCanvas) {
            getPaint().setShader(createShader());
            fDelegate.draw(aCanvas);
        }

        @Override
        public Paint getPaint() {
            return fDelegate.getPaint();
        }

        @Override
        public void computeBounds(RectF aBoundSFCT) {
            fDelegate.computeBounds(aBoundSFCT);
        }

        private Shader createShader() {
            computeBounds(fBounds);
            return new LinearGradient(fBounds.left, fBounds.top, fBounds.right, fBounds.bottom,
                    this.fMainColor,
                    this.fSubColor, Shader.TileMode.CLAMP);
        }
    }

    public class PictureOp implements IDrawingOperation {

        private IDrawingOperation fDelegate;
        private Picture fPicture = null;

        public PictureOp(IDrawingOperation aDelegate) {
            fDelegate = aDelegate;
        }

        @Override
        public void draw(Canvas aCanvas) {
            if (fPicture == null) {
                fPicture = new Picture();
                Canvas c = fPicture.beginRecording(getWidth(), getHeight());
                fDelegate.draw(c);
                fPicture.endRecording();
                fDelegate = null;
            }
            aCanvas.drawPicture(fPicture);
        }

        @Override
        public Paint getPaint() {
            return null;
        }

        @Override
        public void computeBounds(RectF aBoundSFCT) {
        }
    }

    public interface DrawListener {
        public void redraw();
    }

    public interface NewColorUsedListener {
        void newColorUsed(int aNewColor);
    }
}