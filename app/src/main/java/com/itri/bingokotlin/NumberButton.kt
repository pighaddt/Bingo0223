package com.itri.bingokotlin

import android.content.Context
import android.util.AttributeSet

class NumberButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleInt: Int = 0) :
    androidx.appcompat.widget.AppCompatButton(context,
        attributeSet,
        defStyleInt){
    public  var  number : Int = 0
    public var picked = false
    public var pos : Int = 0
}