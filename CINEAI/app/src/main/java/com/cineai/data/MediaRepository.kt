package com.cineai.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MediaRepository(private val context: Context, private val dao: MediaDao) {
    val all: Flow<List<MediaEntity>> = dao.observeAll()
    suspend fun importUri(uri: android.net.Uri): MediaEntity = withContext(Dispatchers.IO) {
        val resolver=context.contentResolver
        val mime=resolver.getType(uri) ?: "application/octet-stream"
        val name=queryName(uri) ?: "media_${System.currentTimeMillis()}"
        val kind=if(mime.startsWith("video/")) MediaKind.VIDEO else MediaKind.PHOTO
        val ext=name.substringAfterLast('.', if(kind==MediaKind.VIDEO) "mp4" else "jpg")
        val root=File(context.filesDir,"media/originals").apply{mkdirs()}
        val id=UUID.randomUUID().toString()
        val out=File(root,"$id.$ext")
        resolver.openInputStream(uri).use{ input -> requireNotNull(input){"Cannot open selected media"}; out.outputStream().use{ output -> input.copyTo(output, 1024*1024) } }
        var w=0; var h=0; var duration=0L
        if(kind==MediaKind.PHOTO){ val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}; resolver.openInputStream(android.net.Uri.fromFile(out)).use{stream->android.graphics.BitmapFactory.decodeStream(stream,null,bounds)}; w=bounds.outWidth; h=bounds.outHeight }
        else { val r=MediaMetadataRetriever(); r.setDataSource(out.absolutePath); w=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?:0; h=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?:0; duration=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0; r.release() }
        val item=MediaEntity(id,out.absolutePath,null,name,mime,kind.name,w,h,duration,out.length())
        dao.upsert(item); item
    }
    private fun queryName(uri: android.net.Uri): String? = context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{c->if(c.moveToFirst())c.getString(0) else null}
    suspend fun get(id:String)=dao.get(id)
    suspend fun updateEnhanced(id:String,path:String,profile:String,preset:String)=dao.markEnhanced(id,path,ProcessingStatus.READY.name,profile,preset)
    suspend fun setStatus(id:String,status:ProcessingStatus)=dao.setStatus(id,status.name)
    suspend fun delete(item:MediaEntity){ withContext(Dispatchers.IO){ File(item.originalPath).delete(); item.enhancedPath?.let{File(it).delete()}; dao.delete(item) } }
}
