package com.cineai.engine

import android.graphics.*
import android.media.ExifInterface
import java.io.File
import kotlin.math.*

class PhotoProcessor {
    data class Result(val output:File,val analysis:SceneAnalyzer.Result)
    fun enhance(input:File, output:File, preset:Preset=Preset.AUTO, manual:EnhancementProfile?=null, targetLongSide:Int=3840, progress:(Int)->Unit={}):Result {
        val opts=BitmapFactory.Options().apply{inPreferredConfig=Bitmap.Config.ARGB_8888;inSampleSize=calculateSample(input,targetLongSide)}
        var bitmap=BitmapFactory.decodeFile(input.absolutePath,opts) ?: error("Unsupported or corrupt image")
        bitmap=applyExif(input,bitmap)
        val analyzer=SceneAnalyzer(); progress(8); val analysis=analyzer.analyze(bitmap); progress(20)
        val p=manual ?: if(preset==Preset.AUTO)analysis.profile else EnhancementProfile.fromPreset(preset,analysis.scene)
        val maxLong=targetLongSide
        if(max(bitmap.width,bitmap.height)!=maxLong){val s=maxLong.toFloat()/max(bitmap.width,bitmap.height); val scaled=Bitmap.createScaledBitmap(bitmap,(bitmap.width*s).roundToInt(),(bitmap.height*s).roundToInt(),true);if(scaled!==bitmap)bitmap.recycle();bitmap=scaled}
        val out=Bitmap.createBitmap(bitmap.width,bitmap.height,Bitmap.Config.ARGB_8888); val inPix=IntArray(bitmap.width*bitmap.height);val outPix=IntArray(inPix.size);bitmap.getPixels(inPix,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        val warm=p.temperature*18f; val tint=p.tint*12f; val contrast=1f+p.contrast; val exp=2.0.pow(p.exposure.toDouble()).toFloat();
        for(i in inPix.indices){ val c=inPix[i]; var r=((c shr 16) and 255)/255f;var g=((c shr 8) and 255)/255f;var b=(c and 255)/255f
            r=(r*exp);g*=exp;b*=exp
            val l=.2126f*r+.7152f*g+.0722f*b
            val shadow=(1f-l).coerceIn(0f,1f)*p.shadows; val hi=l.coerceIn(0f,1f)*p.highlights
            r += shadow+hi;g+=shadow+hi;b+=shadow+hi
            fun curve(x:Float):Float { val y=(x-.5f)*contrast+.5f; return (y + .015f*p.cinematicIntensity*(1f-y) - .01f*p.cinematicIntensity*y).coerceIn(0f,1f) }
            r=curve(r);g=curve(g);b=curve(b)
            r += warm*.01f; b -= warm*.008f; r += tint*.003f; b -= tint*.003f
            val lum=.2126f*r+.7152f*g+.0722f*b; val maxc=maxOf(r,g,b); val minc=minOf(r,g,b); val skin=(r>g && g>b && r-g>.04f && g-b>.02f && maxc-minc<.55f); val vib=1f+p.saturation+p.vibrance*(1f-abs(2f*lum-1f));r=lum+(r-lum)*vib;g=lum+(g-lum)*vib;b=lum+(b-lum)*vib; if(skin){ val skinV=(r*.5f+g*.35f+b*.15f); r=skinV+(r-skinV)*.96f; g=skinV+(g-skinV)*.98f; b=skinV+(b-skinV)*.98f }
            if(p.grain>0){ val noise=((((i*1103515245L+12345L) ushr 16) and 255L).toFloat()/255f-.5f)*p.grain*.055f; r+=noise;g+=noise;b+=noise }
            if(p.vignette>0){val x=(i%bitmap.width)/(bitmap.width-1f)*2-1;val y=(i/bitmap.width)/(bitmap.height-1f)*2-1;val v=(1f-(x*x+y*y)*.12f*p.vignette).coerceIn(.65f,1f);r*=v;g*=v;b*=v}
            outPix[i]=(0xFF shl 24) or ((r.coerceIn(0f,1f)*255).roundToInt() shl 16) or ((g.coerceIn(0f,1f)*255).roundToInt() shl 8) or (b.coerceIn(0f,1f)*255).roundToInt()
            if(i%(outPix.size/10+1)==0)progress(20+(i*60/outPix.size))
        }
        out.setPixels(outPix,0,bitmap.width,0,0,bitmap.width,bitmap.height);bitmap.recycle();output.parentFile?.mkdirs();out.compress(Bitmap.CompressFormat.JPEG,qualityFor(p),output.outputStream());out.recycle();progress(100);return Result(output,analysis)
    }
    private fun qualityFor(p:EnhancementProfile)=if(p.cinematicIntensity>.6f)95 else 97
    private fun calculateSample(f:File,target:Int):Int{val o=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(f.absolutePath,o);var s=1;while(max(o.outWidth,o.outHeight)/(s*2)>=target*1.15)s*=2;return s}
    private fun applyExif(f:File,b:Bitmap):Bitmap{val ex=try{ExifInterface(f)}catch(_:Throwable){return b};val m=Matrix();when(ex.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)){ExifInterface.ORIENTATION_ROTATE_90->m.postRotate(90f);ExifInterface.ORIENTATION_ROTATE_180->m.postRotate(180f);ExifInterface.ORIENTATION_ROTATE_270->m.postRotate(270f);ExifInterface.ORIENTATION_FLIP_HORIZONTAL->m.preScale(-1f,1f);ExifInterface.ORIENTATION_FLIP_VERTICAL->m.preScale(1f,-1f);else->return b};val r=Bitmap.createBitmap(b,0,0,b.width,b.height,m,true);if(r!==b)b.recycle();return r}
}
