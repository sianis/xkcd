
package hu.sianis.xkcd;

import android.view.View;

public class SlideAwayAnimator {

    enum MOVE {
        UP, DOWN
    }

    private View view;

    private boolean isSlided;

    private MOVE viewMove;

    public SlideAwayAnimator(View view, MOVE viewMove) {
        this.view = view;
        this.isSlided = false;
        this.viewMove = viewMove;
    }

    public void toggle() {

        float targetY = 0;
        if (!isSlided) {
            switch (viewMove) {
                case UP:
                    targetY = view.getHeight() * -1;
                    break;

                case DOWN:
                    targetY = view.getHeight();
                    break;
            }
            isSlided = true;
        } else {
            isSlided = false;
        }
        view.animate().translationY(targetY).setDuration(250);
    }
}
