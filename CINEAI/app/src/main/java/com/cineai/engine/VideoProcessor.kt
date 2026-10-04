package com.cineai.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.*
import androidx.media3.transformer.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
class VideoProcessor(private val context:Context){
    suspend fun enhance(input:File, output:File, profile:EnhancementProfile, width:Int?, onProgress:(Int)->Unit):Unit=suspendCancellableCoroutine{cont->
        output.parentFile?.mkdirs(); val effects=mutableListOf<androidx.media3.common.Effect>()
        if(profile.exposure!=0f||profile.brightness!=0f)effects+=Brightness((profile.exposure+profile.brightness).coerceIn(-1f,1f))
        effects+=Contrast(profile.contrast.coerceIn(-.8f,.8f))
        effects+=RgbAdjustment.Builder().setRedScale((1f+profile.temperature*.12f).coerceIn(.7f,1.3f)).setBlueScale((1f-profile.temperature*.10f).coerceIn(.7f,1.3f)).build()
        effects+=HslAdjustment.Builder().adjustSaturation((profile.saturation+profile.vibrance)*100f).adjustLightness(profile.shadows*18f+profile.highlights*12f).build()
        if(profile.noiseReduction>.18f)effects+=GaussianBlurWithFrameOverlaid(.45f,1.06f,1.06f)
        if(profile.sharpness>.12f)effects+=GaussianBlurWithFrameOverlaid(.35f,1.10f,1.10f)
        if(width!=null)effects+=Presentation.createForHeight(width)
        val edited=EditedMediaItem.Builder(MediaItem.fromUri(input.toURI().toString())).setEffects(Effects(emptyList(),effects)).build()
        val transformerHolder=arrayOfNulls<Transformer>(1)
        val handler=Handler(Looper.getMainLooper())
        val transformer=Transformer.Builder(context).addListener(object:Transformer.Listener{
            override fun onCompleted(composition:Composition, result:ExportResult){onProgress(100);cont.resume(Unit)}
            override fun onError(composition:Composition, result:ExportResult, exception:ExportException){cont.resumeWithException(exception)}
        }).build()
        transformerHolder[0]=transformer
        handler.post{try{transformer.start(edited,output.absolutePath)}catch(t:Throwable){if(cont.isActive)cont.resumeWithException(t)}}
        cont.invokeOnCancellation{handler.post{transformer.cancel()}}
        CoroutineScope(cont.context).launch(Dispatchers.Default){val holder=ProgressHolder();while(cont.isActive){handler.post{if(cont.isActive && transformer.getProgress(holder)==Transformer.PROGRESS_STATE_AVAILABLE)onProgress(holder.progress)};delay(250)}}
    }
}
