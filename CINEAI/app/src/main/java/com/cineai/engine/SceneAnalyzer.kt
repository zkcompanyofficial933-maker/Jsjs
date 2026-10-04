package com.cineai.engine

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.math.abs

class SceneAnalyzer {
    data class Result(val scene:Scene,val faceCount:Int,val meanLuma:Float,val profile:EnhancementProfile)
    fun analyze(bitmap:Bitmap):Result {
        val sample=Bitmap.createScaledBitmap(bitmap, minOf(bitmap.width,256), minOf(bitmap.height,256), true)
        var sum=0L; var sumR=0L; var sumG=0L; var sumB=0L; var bright=0; var dark=0; var warm=0; var green=0; var blue=0
        val px=IntArray(sample.width*sample.height); sample.getPixels(px,0,sample.width,0,0,sample.width,sample.height)
        px.forEach{c-> val r=(c shr 16) and 255; val g=(c shr 8) and 255; val b=c and 255; val y=(r*299+g*587+b*114)/1000; sum+=y; sumR+=r; sumG+=g; sumB+=b; if(y>225)bright++;if(y<45)dark++;if(r>g*1.15 && r>b*1.2)warm++;if(g>r*1.1&&g>b*1.05)green++;if(b>r*1.15&&b>g*1.05)blue++ }
        val n=px.size.coerceAtLeast(1); val mean=sum.toFloat()/n; val faceCount=try{val opts=FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build(); FaceDetection.getClient(opts).use{Tasks.await(it.process(InputImage.fromBitmap(sample,0))).size}}catch(_:Throwable){0}
        val darkRatio=dark.toFloat()/n; val warmRatio=warm.toFloat()/n
        val scene=when{faceCount>=2->Scene.GROUP;faceCount==1->Scene.PORTRAIT;mean<55||darkRatio>.42f->Scene.NIGHT;warmRatio>.22f&&mean>100->Scene.SUNSET;green.toFloat()/n>.18f->Scene.NATURE;blue.toFloat()/n>.22f->Scene.LANDSCAPE;bitmap.width>bitmap.height*1.2->Scene.LANDSCAPE;else->Scene.UNKNOWN}
        val autoTemp=((sumB.toFloat()-sumR.toFloat())/(n*255f)*0.10f).coerceIn(-.08f,.08f)
        val p=EnhancementProfile.fromPreset(Preset.AUTO,scene).copy(temperature=autoTemp)
        return Result(scene,faceCount,mean,p)
    }
}
