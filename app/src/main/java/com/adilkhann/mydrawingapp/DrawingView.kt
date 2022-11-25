package com.adilkhann.mydrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context:Context, attr: AttributeSet) : View(context, attr){
    private var mDrawPath : CustomPath? = null
    private var mBitmap : Bitmap? = null
    private var canvas : Canvas? = null
    private var mDrawPaint : Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize  = 20.toFloat()
    private var mColor : Int = Color.BLACK
    private var mPath  = ArrayList<CustomPath>()
    init {
        setUpDrawing()
    }
    private fun setUpDrawing()
    {
        mDrawPath = CustomPath(mColor,mBrushSize)
        mDrawPaint = Paint()
        mDrawPaint?.strokeJoin = Paint.Join.ROUND
        mDrawPaint?.strokeCap = Paint.Cap.ROUND
        mDrawPaint?.color = mColor
        mDrawPaint?.style = Paint.Style.STROKE
        mCanvasPaint = Paint(Paint.DITHER_FLAG)


    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(mBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(mBitmap!!,0f,0f,mCanvasPaint!!)
        for(path in mPath)
        {
            mDrawPaint?.strokeWidth = path.brushThickness
            mDrawPaint?.color = path.color
            canvas?.drawPath(path,mDrawPaint!!)
        }
        if(!mDrawPath!!.isEmpty) {
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas?.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y
        when(event?.action)
        {
            MotionEvent.ACTION_DOWN ->
            {
                mDrawPath?.color = mColor
                mDrawPath?.brushThickness = mBrushSize
                mDrawPath?.reset()
                mDrawPath?.moveTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_MOVE -> {
                mDrawPath?.lineTo(touchX!!, touchY!!)
            }
            MotionEvent.ACTION_UP ->
            {    mPath.add(mDrawPath!!)
                mDrawPath = CustomPath(mColor, mBrushSize)
            }
            else -> {
                return false
            }
        }
        invalidate()
        return true

    }
     fun setSizeForBrush(size: Float)
    {
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,size,resources.displayMetrics)
        mDrawPaint?.strokeWidth = mBrushSize
    }
    fun undoView()
    {
        if(mPath.size>0)
        {
            mPath.removeAt(mPath.size-1)
        }
        invalidate()
    }
    fun setColorBrush(newColor:String)
    {
        mColor = Color.parseColor(newColor)
        mDrawPath?.color = mColor
    }
    internal inner class CustomPath(var color:Int, var brushThickness:Float):Path()
    {

    }
}