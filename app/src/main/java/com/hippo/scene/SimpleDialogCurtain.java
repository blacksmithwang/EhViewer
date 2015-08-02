/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.scene;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.annotation.NonNull;
import android.view.View;

import com.hippo.animation.ArgbEvaluator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.AnimationUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.Say;

import java.util.HashSet;
import java.util.Set;

public class SimpleDialogCurtain extends Curtain {

    private static final String TAG = SimpleDialogCurtain.class.getSimpleName();

    private static long ANIMATE_TIME = 300L;
    private static float SCALE_PERCENT = 0.75f;

    private int mStartX;
    private int mStartY;

    private AnimatorSet mAnimatorSet;

    public SimpleDialogCurtain() {
        this(0, 0);
    }

    public SimpleDialogCurtain(int startX, int startY) {
        mStartX = startX;
        mStartY = startY;
    }

    public void setStartPosition(int startX, int startY) {
        mStartX = startX;
        mStartY = startY;
    }

    @Override
    protected boolean needSpecifyPreviousScene() {
        return false;
    }

    @Override
    public void open(@NonNull final Scene enter, @NonNull final Scene exit) {
        AssertUtils.assertInstanceof("SimpleDialogCurtain should only use for SimpleDialog.", enter, SimpleDialog.class);

        // Check stage layout isLayoutRequested
        final StageLayout stageLayout = enter.getStageLayout();
        if (!stageLayout.isLayoutRequested()) {
            Say.w(TAG, "WTF? stageLayout.isLayoutRequested() == false");
            dispatchOpenFinished(enter, exit);
            return;
        }

        final SimpleDialog enterDialog = (SimpleDialog) enter;

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = enter.getBackgroundColor();
        int startBgColor = bgColor & 0xffffff;
        enter.setBackgroundColor(startBgColor);
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(enter, "backgroundColor", bgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        colorAnim.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        animatorCollection.add(colorAnim);

        final View cushion = enterDialog.getCushion();
        final View frame = enterDialog.getFrame();
        AssertUtils.assertNotNull("Cushion view must not be null.", cushion);
        AssertUtils.assertNotNull("Frame view must not be null.", frame);

        cushion.setVisibility(View.INVISIBLE);
        frame.setVisibility(View.INVISIBLE);

        stageLayout.addOnLayoutListener(new StageLayout.OnLayoutListener() {
            @Override
            public void onLayout(View view) {
                stageLayout.removeOnLayoutListener(this);

                float floatStartX;
                float floatStartY;
                int intStartX;
                int intStartY;
                if (mStartX == 0 && mStartY == 0) {
                    int[] center = new int[2];
                    enterDialog.getCenterLocation(center);
                    floatStartX = center[0];
                    floatStartY = center[1];
                    intStartX = center[0];
                    intStartY = center[1];
                } else {
                    floatStartX = mStartX;
                    floatStartY = mStartY;
                    intStartX = mStartX;
                    intStartY = mStartY;
                }

                cushion.setPivotX(0f);
                cushion.setPivotY(0f);
                PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f);
                PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 0f, 1f);
                PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", floatStartX, frame.getLeft());
                PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", floatStartY, frame.getTop());
                ObjectAnimator animCushion = ObjectAnimator.ofPropertyValuesHolder(cushion, scaleXPvh, scaleYPvh, xPvh, yPvh);
                animCushion.setDuration(ANIMATE_TIME);
                animCushion.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                animatorCollection.add(animCushion);

                PropertyValuesHolder leftPvh = PropertyValuesHolder.ofInt("drawLeft", intStartX, frame.getLeft());
                PropertyValuesHolder topPvh = PropertyValuesHolder.ofInt("drawTop", intStartY, frame.getTop());
                PropertyValuesHolder rightPvh = PropertyValuesHolder.ofInt("drawRight", intStartX, frame.getRight());
                PropertyValuesHolder bottomPvh = PropertyValuesHolder.ofInt("drawBottom", intStartY, frame.getBottom());
                ObjectAnimator animFrame = ObjectAnimator.ofPropertyValuesHolder(frame, leftPvh, topPvh, rightPvh, bottomPvh);
                animFrame.setDuration(ANIMATE_TIME);
                animFrame.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                animatorCollection.add(animFrame);

                mAnimatorSet = new AnimatorSet();
                mAnimatorSet.playTogether(animatorCollection);
                mAnimatorSet.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        cushion.setVisibility(View.VISIBLE);
                        frame.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchOpenFinished(enter, exit);
                        mAnimatorSet = null;
                    }
                });
                mAnimatorSet.start();
            }
        });
    }

    @Override
    public void close(@NonNull final Scene enter, @NonNull final Scene exit) {
        AssertUtils.assertInstanceof("SimpleDialogCurtain should only use for SimpleDialog.", exit, SimpleDialog.class);
        final SimpleDialog exitDialog = (SimpleDialog) exit;

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = exitDialog.getBackgroundColor();
        int endBgColor = bgColor & 0xffffff;
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(exit, "backgroundColor", endBgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(colorAnim);
        colorAnim.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);

        final View layout = exitDialog.getLayout();
        final View cushion = exitDialog.getCushion();
        final View frame = exitDialog.getFrame();
        AssertUtils.assertNotNull("Cushion view must not be null.", cushion);
        AssertUtils.assertNotNull("Frame view must not be null.", frame);

        // Check stage layout isLayoutRequested
        final StageLayout stageLayout = enter.getStageLayout();
        if (stageLayout.isLayoutRequested()) {
            stageLayout.addOnLayoutListener(new StageLayout.OnLayoutListener() {
                @Override
                public void onLayout(View view) {
                    stageLayout.removeOnLayoutListener(this);

                    onClose(enter, exitDialog, layout, cushion, frame, animatorCollection);
                }
            });
        } else {
            onClose(enter, exitDialog, layout, cushion, frame, animatorCollection);
        }
    }

    private void onClose(final Scene enter, final SimpleDialog exitDialog, View layout, View cushion, View frame, Set<Animator> animatorCollection) {

        layout.setAlpha(1f);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(layout, "alpha", 1f, 0f);
        alphaAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(alphaAnim);

        cushion.setPivotX(cushion.getWidth() / 2);
        cushion.setPivotY(cushion.getHeight() / 2);
        PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 1f, SCALE_PERCENT);
        PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 1f, SCALE_PERCENT);
        ObjectAnimator animCushion = ObjectAnimator.ofPropertyValuesHolder(cushion, scaleXPvh, scaleYPvh);
        animCushion.setDuration(ANIMATE_TIME);
        animCushion.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        animatorCollection.add(animCushion);

        int dx = (int) (frame.getWidth() * (1 - SCALE_PERCENT) / 2);
        int dy = (int) (frame.getHeight() * (1 - SCALE_PERCENT) / 2);
        PropertyValuesHolder leftPvh = PropertyValuesHolder.ofInt("drawLeft", frame.getLeft() + dx);
        PropertyValuesHolder topPvh = PropertyValuesHolder.ofInt("drawTop", frame.getTop() + dy);
        PropertyValuesHolder rightPvh = PropertyValuesHolder.ofInt("drawRight", frame.getRight() - dx);
        PropertyValuesHolder bottomPvh = PropertyValuesHolder.ofInt("drawBottom", frame.getBottom() - dy);
        ObjectAnimator animFrame = ObjectAnimator.ofPropertyValuesHolder(frame, leftPvh, topPvh, rightPvh, bottomPvh);
        animFrame.setDuration(ANIMATE_TIME);
        animFrame.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        animatorCollection.add(animFrame);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorCollection);
        mAnimatorSet.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchCloseFinished(enter, exitDialog);
                mAnimatorSet = null;
            }
        });
        mAnimatorSet.start();
    }

    @Override
    public void endAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
        }
    }

    @Override
    public boolean isInAnimation() {
        return mAnimatorSet != null;
    }
}
