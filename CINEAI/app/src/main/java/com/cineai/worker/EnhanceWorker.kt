package com.cineai.worker

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cineai.CineAiApp
import com.cineai.data.ProcessingStatus
import com.cineai.engine.*
import java.io.File

class EnhanceWorker(appContext:Context,params:WorkerParameters):CoroutineWorker(appContext,params){
    override suspend fun getForegroundInfo():ForegroundInfo {
        val channel="cineai_processing"
        val nm=applicationContext.getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(NotificationChannel(channel,"CINEAI processing",NotificationManager.IMPORTANCE_LOW))
        val n=NotificationCompat.Builder(applicationContext,channel).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("CINEAI processing").setContentText("Enhancing media…").setOngoing(true).setProgress(100,0,false).build()
        return ForegroundInfo(1001,n)
    }
    override suspend fun doWork():Result{
        setForeground(getForegroundInfo())
        val id=inputData.getString("id")?:return Result.failure(); val repo=(applicationContext as CineAiApp).repository; val item=repo.get(id)?:return Result.failure()
        return try{
            repo.setStatus(id,ProcessingStatus.PROCESSING)
            val dir=File(applicationContext.filesDir,"media/enhanced").apply{mkdirs()}; val out=File(dir,"$id.${if(item.kind=="PHOTO")"jpg" else "mp4"}")
            val preset=Preset.values().firstOrNull{it.label==inputData.getString("preset")}?:Preset.AUTO
            val target=inputData.getInt("target",3840)
            val custom=inputData.getString("profile")?.let{EnhancementProfile.fromJson(it)}
            if(item.kind=="PHOTO"){
                val result=PhotoProcessor().enhance(File(item.originalPath),out,preset){setProgressAsync(workDataOf("progress" to it,"stage" to if(it<20)"Analyzing scene" else if(it<95)"Enhancing image" else "Encoding photo"))}
                repo.updateEnhanced(id,out.absolutePath,result.analysis.profile.json(),preset.label)
            }else{
                VideoProcessor(applicationContext).enhance(File(item.originalPath),out,(custom ?: EnhancementProfile.fromPreset(preset)),1080){setProgressAsync(workDataOf("progress" to it,"stage" to if(it<10)"Analyzing video" else "Encoding enhanced video"))}
                repo.updateEnhanced(id,out.absolutePath,(custom ?: EnhancementProfile.fromPreset(preset)).json(),preset.label)
            }
            Result.success(workDataOf("path" to out.absolutePath))
        }catch(t:Throwable){repo.setStatus(id,ProcessingStatus.FAILED);Result.failure(workDataOf("error" to (t.message?:"Processing failed")))}
    }
}
