package com.cineai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.cineai.CineAiApp
import com.cineai.data.*
import com.cineai.worker.EnhanceWorker
import kotlinx.coroutines.flow.*
import com.cineai.engine.EnhancementProfile
import com.cineai.engine.Preset
import com.cineai.engine.ExportManager
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(app:Application):AndroidViewModel(app){
    private val repo=(app as CineAiApp).repository
    val media=repo.all.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    var selected:MediaEntity?=null; private set
    fun select(i:MediaEntity){selected=i}
    fun import(uri:Uri,onDone:(MediaEntity)->Unit) = viewModelScope.launch{onDone(repo.importUri(uri))}
    fun enhance(id:String,preset:Preset=Preset.AUTO,target:Int=3840,profile:EnhancementProfile?=null)=viewModelScope.launch{val data=workDataOf("id" to id,"preset" to preset.label,"target" to target,"profile" to profile?.json());WorkManager.getInstance(getApplication()).enqueue(OneTimeWorkRequestBuilder<EnhanceWorker>().setInputData(data).setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build()).addTag("cineai:$id").build())}
    suspend fun export(item:MediaEntity):Uri=ExportManager(getApplication()).export(java.io.File(requireNotNull(item.enhancedPath)),item.mimeType,"CINEAI_${item.displayName}")
    fun workFlow(id:String):Flow<WorkInfo?> = WorkManager.getInstance(getApplication()).getWorkInfosByTagFlow("cineai:$id").map{it.firstOrNull()}
    fun cancel(id:String){WorkManager.getInstance(getApplication()).cancelAllWorkByTag("cineai:$id")}
    fun delete(item:MediaEntity)=viewModelScope.launch{repo.delete(item)}
}
