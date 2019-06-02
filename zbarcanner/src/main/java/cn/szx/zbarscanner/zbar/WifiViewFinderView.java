package cn.szx.zbarscanner.zbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import cn.szx.zbarscanner.R;
import cn.szx.zbarscanner.base.IViewFinder;

/**
 * 覆盖在相机预览上的view，包含扫码框、扫描线、扫码框周围的阴影遮罩等
 */
public class WifiViewFinderView extends RelativeLayout implements IViewFinder {
    private Rect framingRect;//扫码框所占区域
    private float widthRatio = 0.5f;//0.86f;//扫码框宽度占view总宽度的比例
    private float heightWidthRatio = 0.889f;//0.6f;//扫码框的高宽比
    private int leftOffset = -1;//扫码框相对于左边的偏移量，若为负值，则扫码框会水平居中
    private int topOffset = -1;//(int) (150*getContext().getResources().getDisplayMetrics().density);//扫码框相对于顶部的偏移量，若为负值，则扫码框会竖直居中

    private boolean isLaserEnabled = true;//是否显示扫描线
    private static final int[] laserAlpha = {0, 64, 128, 192, 255, 192, 128, 64};
    private int laserAlphaIndex;
    private static final long animationDelay = 20l;
    private final int laserColor = Color.parseColor("#ffcc0000");

    private final int maskColor = Color.parseColor("#60000000");
    private final int borderColor = Color.parseColor("#ff3494FF");
    private final int textColor = Color.parseColor("#CCCCCC");                            //文字的颜色
    private final int borderStrokeWidth = 6;
    protected int borderLineLength = 100;

    protected Paint laserPaint;
    protected Paint maskPaint;
    protected Paint borderPaint;
    private int lineOffsetCount = 0;

    private String mScanTipContent = "放入框内，自动扫描";
    private int mScanTipFontSize = 22;
    private Paint textPaint;
    private final int textMarinTop = 50;                                           //文字距离识别框的距离

    public WifiViewFinderView(Context context) {
        super(context);
        initDraw();
        initLayout();
    }

    private void initDraw() {
        setWillNotDraw(false);//需要进行绘制

        //扫描线画笔
        laserPaint = new Paint();
        laserPaint.setColor(laserColor);
        laserPaint.setStyle(Paint.Style.FILL);

        //阴影遮罩画笔
        maskPaint = new Paint();
        maskPaint.setColor(maskColor);

        //边框画笔
        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderStrokeWidth);
        borderPaint.setAntiAlias(true);

        //  提示文字
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(dp2px(mScanTipFontSize));
    }

    private int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    private void initLayout() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_view_finder, this, true);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = canvas.getWidth();
        Rect framingRect = getFramingRect();

        if (getFramingRect() == null) {
            return;
        }

        drawViewFinderMask(canvas);

        //String text = "将二维码放入框内，即可自动扫描";
        canvas.drawText(mScanTipContent, (width - textPaint.measureText(mScanTipContent)) / 2, framingRect.bottom + textMarinTop, textPaint);

        drawViewFinderBorder(canvas);

        if (isLaserEnabled) {
            drawLaser(canvas);
        }
    }

    /**
     * 绘制扫码框四周的阴影遮罩
     */
    public void drawViewFinderMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        Rect framingRect = getFramingRect();

        canvas.drawRect(0, 0, width, framingRect.top, maskPaint);//扫码框顶部阴影
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom, maskPaint);//扫码框左边阴影
        canvas.drawRect(framingRect.right, framingRect.top, width, framingRect.bottom, maskPaint);//扫码框右边阴影
        canvas.drawRect(0, framingRect.bottom, width, height, maskPaint);//扫码框底部阴影
    }

    /**
     * 绘制扫码框的边框
     */
    public void drawViewFinderBorder(Canvas canvas) {
        Rect framingRect = getFramingRect();

        // Top-left corner
        Path path = new Path();
        path.moveTo(framingRect.left, framingRect.top + borderLineLength);
        path.lineTo(framingRect.left, framingRect.top);
        path.lineTo(framingRect.left + borderLineLength, framingRect.top);
        canvas.drawPath(path, borderPaint);

        // Top-right corner
        path.moveTo(framingRect.right, framingRect.top + borderLineLength);
        path.lineTo(framingRect.right, framingRect.top);
        path.lineTo(framingRect.right - borderLineLength, framingRect.top);
        canvas.drawPath(path, borderPaint);

        // Bottom-right corner
        path.moveTo(framingRect.right, framingRect.bottom - borderLineLength);
        path.lineTo(framingRect.right, framingRect.bottom);
        path.lineTo(framingRect.right - borderLineLength, framingRect.bottom);
        canvas.drawPath(path, borderPaint);

        // Bottom-left corner
        path.moveTo(framingRect.left, framingRect.bottom - borderLineLength);
        path.lineTo(framingRect.left, framingRect.bottom);
        path.lineTo(framingRect.left + borderLineLength, framingRect.bottom);
        canvas.drawPath(path, borderPaint);
    }

    /**
     * 绘制扫描线
     */
    public void drawLaser(Canvas canvas) {
        Rect framingRect = getFramingRect();

        /*
        laserPaint.setAlpha(laserAlpha[laserAlphaIndex]);
        laserAlphaIndex = (laserAlphaIndex + 1) % laserAlpha.length;
        int middle = framingRect.height() / 2 + framingRect.top;
        canvas.drawRect(framingRect.left + 1, middle - 1, framingRect.right - 1, middle + 1, laserPaint);
        */
        //循环划线，从上到下
        if (lineOffsetCount > framingRect.bottom - framingRect.top - dp2px(10)) {
            lineOffsetCount = 0;
        } else {
            lineOffsetCount = lineOffsetCount + 4;
//            canvas.drawLine(frame.left, frame.top + lineOffsetCount, frame.right, frame.top + lineOffsetCount, linePaint);    //画一条红色的线

            laserPaint.setAlpha(laserAlpha[laserAlphaIndex]);
            laserAlphaIndex = (laserAlphaIndex + 1) % laserAlpha.length;
            int middle = framingRect.top + lineOffsetCount;
            canvas.drawRect(framingRect.left + 1, middle - 1, framingRect.right - 1, middle + 1, laserPaint);
        }

        //区域刷新
        postInvalidateDelayed(animationDelay,
                framingRect.left,
                framingRect.top,
                framingRect.right,
                framingRect.bottom);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        updateFramingRect();
    }

    /**
     * 设置framingRect的值（扫码框所占的区域）
     */
    public synchronized void updateFramingRect() {
        Point viewSize = new Point(getWidth(), getHeight());
        int width, height;
        /*
        width = (int) (getWidth() * widthRatio);
        height = (int) (heightWidthRatio * width);
        */
        width = findDesiredDimensionInRange(getWidth(),240 , 1200);
        height = findDesiredDimensionInRange(getHeight(), 240, 675);
        if(width>height){
            width = height;
        }
        // int height = width; //改成正方形
        // int value = 0;
        //if(screenResolution.x > screenResolution.y)
        //  value = screenResolution.y;
        //else value = screenResolution.x;
        //int width = 500;//value /2;
        height = width;
        height = height / 3 * 2;
        width = width / 3 * 2;
        int left, top;
        if (leftOffset < 0) {
            left = (viewSize.x - width) / 2;//水平居中
        } else {
            left = leftOffset;
        }
        if (topOffset < 0) {
            top = (viewSize.y - height) / 2;//竖直居中
        } else {
            top = topOffset;
        }
        framingRect = new Rect(left, top, left + width, top + height);
    }

    public Rect getFramingRect() {
        return framingRect;
    }

    private int dp2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    //  设置扫描文字提示内容
    public void SetTipContent(String content) {
        mScanTipContent = content;
    }

    //  设置扫描提示文字大小
    public void SetTopFontSize(int sz) {
        mScanTipFontSize = sz;
        textPaint.setTextSize(dp2px(mScanTipFontSize));
    }
}