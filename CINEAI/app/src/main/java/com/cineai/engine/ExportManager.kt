package com.cineai.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportManager(private val context:Context){
    suspend fun export(file:File,mime:String,displayName:String):Uri=withContext(Dispatchers.IO){
        require(file.exists()&&file.length()>0){"Output file was not created"}
        val collection=if(mime.startsWith("video/"))MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,displayName);put(MediaStore.MediaColumns.MIME_TYPE,mime);put(MediaStore.MediaColumns.RELATIVE_PATH,if(mime.startsWith("video/"))Environment.DIRECTORY_MOVIES+"/CINEAI" else Environment.DIRECTORY_PICTURES+"/CINEAI");put(MediaStore.MediaColumns.IS_PENDING,1)}
        val uri=context.contentResolver.insert(collection,values)?:error("Gallery export could not be created")
        try{context.contentResolver.openOutputStream(uri).use{out->requireNotNull(out);file.inputStream().use{it.copyTo(out)}};values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);context.contentResolver.update(uri,values,null,null);uri}catch(t:Throwable){context.contentResolver.delete(uri,null,null);throw t}
    }
}
